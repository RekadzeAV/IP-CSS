[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [System.Management.Automation.PSCredential]$Credential
)

$ErrorActionPreference = "Stop"

function Invoke-JsonRequest {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Uri,
        [Parameter()][Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [Parameter()]$Body,
        [Parameter()][hashtable]$Headers = @{}
    )

    $params = @{
        Method = $Method
        Uri = $Uri
        UseBasicParsing = $true
    }
    if ($null -ne $Session) {
        $params["WebSession"] = $Session
    }
    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = ($Body | ConvertTo-Json -Compress)
    }
    if ($Headers.Count -gt 0) {
        $params["Headers"] = $Headers
    }

    $resp = Invoke-WebRequest @params
    if ([string]::IsNullOrWhiteSpace($resp.Content)) {
        return $null
    }
    return ($resp.Content | ConvertFrom-Json)
}

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
if ($null -eq $Credential) {
    $secure = ConvertTo-SecureString "ADMIN_PASSWORD@" -AsPlainText -Force
    $Credential = New-Object System.Management.Automation.PSCredential("admin", $secure)
}
$plainPassword = $Credential.GetNetworkCredential().Password

Write-Host "1) Login..."
$loginResponse = Invoke-JsonRequest -Method "POST" -Uri "$BaseUrl/api/v1/auth/login" -Session $session -Body @{
    username = $Credential.UserName
    password = $plainPassword
}
if (-not $loginResponse.success) {
    throw "Login failed: $($loginResponse.message)"
}

Write-Host "2) Refresh token..."
$refreshResponse = Invoke-JsonRequest -Method "POST" -Uri "$BaseUrl/api/v1/auth/refresh" -Session $session -Body @{}
if (-not $refreshResponse.success) {
    throw "Refresh failed: $($refreshResponse.message)"
}

Write-Host "3) Fetch ws-token..."
$wsTokenResponse = Invoke-JsonRequest -Method "GET" -Uri "$BaseUrl/api/v1/auth/ws-token" -Session $session
if (-not $wsTokenResponse.success -or [string]::IsNullOrWhiteSpace($wsTokenResponse.data.token)) {
    throw "ws-token failed: $($wsTokenResponse.message)"
}
$wsToken = $wsTokenResponse.data.token

Write-Host "4) Check events statistics..."
$statsResponse = Invoke-JsonRequest -Method "GET" -Uri "$BaseUrl/api/v1/events/statistics" -Session $session
if (-not $statsResponse.success) {
    throw "events/statistics failed: $($statsResponse.message)"
}
if ($null -eq $statsResponse.data -or $null -eq $statsResponse.data.total) {
    throw "events/statistics returned invalid payload"
}

Write-Host "5) Discover cameras..."
$discoverResponse = Invoke-JsonRequest -Method "GET" -Uri "$BaseUrl/api/v1/cameras/discover?refresh=true" -Headers @{
    Authorization = "Bearer $wsToken"
}
if (-not $discoverResponse.success) {
    throw "discover failed: $($discoverResponse.message)"
}

$count = 0
if ($null -ne $discoverResponse.data) {
    $count = @($discoverResponse.data).Count
}

Write-Host ""
Write-Host "SMOKE PASS" -ForegroundColor Green
Write-Host ("discover.count = {0}" -f $count)
Write-Host ("events.total = {0}" -f $statsResponse.data.total)
Write-Host ("ws-token.length = {0}" -f $wsToken.Length)
