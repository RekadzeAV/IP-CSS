[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [System.Management.Automation.PSCredential]$Credential,
    [int]$MaxDiscoverFailures = 0,
    [int]$MaxAuthFailures = 5,
    [int]$MaxEventStatisticsFailures = 0
)

$ErrorActionPreference = "Stop"

if ($null -eq $Credential) {
    $secure = ConvertTo-SecureString "ADMIN_PASSWORD@" -AsPlainText -Force
    $Credential = New-Object System.Management.Automation.PSCredential("admin", $secure)
}

function Invoke-JsonRequest {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Uri,
        [Parameter()][Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [Parameter()]$Body
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

    $resp = Invoke-WebRequest @params
    if ([string]::IsNullOrWhiteSpace($resp.Content)) {
        return $null
    }
    return ($resp.Content | ConvertFrom-Json)
}

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

Write-Host "1) Login for metrics access..."
$loginResponse = Invoke-JsonRequest -Method "POST" -Uri "$BaseUrl/api/v1/auth/login" -Session $session -Body @{
    username = $Credential.UserName
    password = $Credential.GetNetworkCredential().Password
}
if (-not $loginResponse.success) {
    throw "Login failed: $($loginResponse.message)"
}

Write-Host "2) Read /health/metrics..."
$metricsResponse = Invoke-JsonRequest -Method "GET" -Uri "$BaseUrl/api/v1/health/metrics" -Session $session
if (-not $metricsResponse.success -or $null -eq $metricsResponse.data) {
    throw "health/metrics failed: $($metricsResponse.message)"
}

$m = $metricsResponse.data
$discoverFailures = [int64]($m.discoverFailures | ForEach-Object { $_ })
$authFailures = [int64]($m.authLoginFailure + $m.authRefreshFailure + $m.authWsTokenFailure)
$eventStatisticsFailures = [int64]($m.eventStatisticsFailure | ForEach-Object { $_ })

Write-Host ("discoverFailures={0}" -f $discoverFailures)
Write-Host ("authFailuresTotal={0}" -f $authFailures)
Write-Host ("eventStatisticsFailures={0}" -f $eventStatisticsFailures)

$errors = @()
if ($discoverFailures -gt $MaxDiscoverFailures) {
    $errors += "discoverFailures $discoverFailures > threshold $MaxDiscoverFailures"
}
if ($authFailures -gt $MaxAuthFailures) {
    $errors += "authFailuresTotal $authFailures > threshold $MaxAuthFailures"
}
if ($eventStatisticsFailures -gt $MaxEventStatisticsFailures) {
    $errors += "eventStatisticsFailures $eventStatisticsFailures > threshold $MaxEventStatisticsFailures"
}

if ($errors.Count -gt 0) {
    Write-Host ""
    Write-Host "METRICS GUARD FAIL" -ForegroundColor Red
    $errors | ForEach-Object { Write-Host ("- {0}" -f $_) }
    exit 1
}

Write-Host ""
Write-Host "METRICS GUARD PASS" -ForegroundColor Green
exit 0
