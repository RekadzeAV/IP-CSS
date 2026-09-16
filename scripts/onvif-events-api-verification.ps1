# Автоматическая API-проверка ONVIF Events (замена «ручной» приёмки P1-2 при наличии камеры).
# Требуется: config/test-cameras.local.json, запущенный API (ONVIF_EVENTS_ENABLED=true).
# Документация: docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md
#
# Использование:
#   .\scripts\onvif-events-api-verification.ps1 -AdminPassword "..." 
#   или: $env:IPCSS_ADMIN_PASSWORD = "..."
#
# Опционально: -CameraIndex 0, -SkipDelete, -ApiBase "http://localhost:8080"

param(
    [string]$ApiBase = "http://localhost:8080",
    [string]$AdminUser = "admin",
    [Alias("AdminPassword")]
    [string]$AdminInput = "",
    [System.Security.SecureString]$AdminPasswordSecure = $null,
    [int]$CameraIndex = 0,
    [int]$WaitSeconds = 15,
    [switch]$SkipDelete = $false,
    [string]$OutputJsonPath = "",
    [switch]$EnableWebSocketCheck = $false,
    [int]$MaxEventWaitSeconds = 30,
    [int]$EventPollIntervalSeconds = 2,
    [int]$PollIntervalMs = 5000,
    [int]$PullTimeoutMs = 500,
    [int]$LatencyBufferMs = 10000,
    [string]$WebSocketUrl = "",
    [switch]$Help = $false,
    [switch]$ShowHelp = $false
)

$ErrorActionPreference = "Stop"

function Resolve-PlainPassword([object]$value) {
    if ($null -eq $value) { return $null }
    if ($value -is [System.Security.SecureString]) {
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($value)
        try {
            return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }
    return [string]$value
}

function Write-HelpText {
    Write-Host "ONVIF Events API verification (camera + API; optional WS/latency)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\onvif-events-api-verification.ps1 -AdminPassword <pwd>"
    Write-Host "  .\scripts\onvif-events-api-verification.ps1 -ApiBase http://localhost:8080 -AdminUser admin -EnableWebSocketCheck"
    Write-Host ""
    Write-Host "Prerequisites:"
    Write-Host "  config/test-cameras.local.json, API with ONVIF_EVENTS_ENABLED=true, camera reachable."
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -AdminPassword / -AdminPasswordSecure / env IPCSS_ADMIN_PASSWORD"
    Write-Host "  -CameraIndex  -WaitSeconds  -SkipDelete  -OutputJsonPath"
    Write-Host "  -EnableWebSocketCheck  -WebSocketUrl  -MaxEventWaitSeconds  -PollIntervalMs  -PullTimeoutMs  -LatencyBufferMs"
    Write-Host ""
    Write-Host "Help switches:"
    Write-Host "  -Help  -ShowHelp  (same; print this text and exit)"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Completed successfully (or after -Help/-ShowHelp)"
    Write-Host "  Non-zero  Terminating error (missing password/config, unreachable camera, API failures)"
}

if ($Help -or $ShowHelp) {
    Write-HelpText
    exit 0
}

$resolvedAdminPassword = $null
if ($AdminPasswordSecure) {
    $resolvedAdminPassword = Resolve-PlainPassword $AdminPasswordSecure
}
if (-not $resolvedAdminPassword -and $AdminInput) {
    $resolvedAdminPassword = Resolve-PlainPassword $AdminInput
}
if (-not $resolvedAdminPassword) { $resolvedAdminPassword = $env:IPCSS_ADMIN_PASSWORD }
if (-not $resolvedAdminPassword) { throw "Admin password required." }

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$configPath = Join-Path $projectRoot "config\test-cameras.local.json"
if (-not (Test-Path $configPath)) { throw "Config not found: $configPath" }

$config = Get-Content $configPath -Raw | ConvertFrom-Json
$cameras = @($config.cameras)
if ($CameraIndex -lt 0 -or $CameraIndex -ge $cameras.Count) { throw "Invalid CameraIndex." }
$camera = $cameras[$CameraIndex]
foreach ($field in @("host", "username", "password")) {
    if (-not ($camera.PSObject.Properties.Name -contains $field) -or [string]::IsNullOrWhiteSpace([string]$camera.$field)) {
        throw "Camera profile invalid: $field"
    }
}

$hostName = [string]$camera.host
$httpPort = 80
if ($config.defaults -and $config.defaults.httpPorts -and $config.defaults.httpPorts.Count -gt 0) {
    $httpPort = [int]$config.defaults.httpPorts[0]
}
$cameraUrl = if ($httpPort -eq 80) { "http://$hostName" } else { "http://$hostName`:$httpPort" }
$camName = "Test ONVIF $hostName"
$camUser = [string]$camera.username
$camPass = [string]$camera.password

Write-Host "=== ONVIF Events automated API verification ===" -ForegroundColor Cyan
Write-Host ("API: {0}" -f $ApiBase)
Write-Host ("Camera URL: {0}" -f $cameraUrl)

Write-Host "0. Precheck camera reachability..." -ForegroundColor Yellow
$precheck = Test-NetConnection -ComputerName $hostName -Port $httpPort -WarningAction SilentlyContinue
if (-not $precheck.TcpTestSucceeded) { throw "Camera is not reachable: ${hostName}:$httpPort" }

$base = $ApiBase.TrimEnd("/")
$session = $null
$authHeaders = @{}

Write-Host "1. Logging in..." -ForegroundColor Yellow
$loginBody = @{ username = $AdminUser; password = $resolvedAdminPassword } | ConvertTo-Json
$loginResp = Invoke-WebRequest -Uri "$base/api/v1/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -SessionVariable session -UseBasicParsing
$loginJson = $loginResp.Content | ConvertFrom-Json
if (-not $loginJson.success) { throw "Login failed." }
$cookieHeader = [string]$loginResp.Headers["Set-Cookie"]
if ($cookieHeader -and $cookieHeader.Contains("access_token=")) {
    $tokenPart = ($cookieHeader -split ";")[0]
    $accessToken = $tokenPart.Replace("access_token=", "")
    if ($accessToken) {
        $authHeaders.Add("Authorization", "Bearer $accessToken")
    }
}

Write-Host "2. Adding camera..." -ForegroundColor Yellow
$addBody = @{
    name = $camName
    url = $cameraUrl
    username = $camUser
    password = $camPass
} | ConvertTo-Json
$addResp = Invoke-WebRequest -Uri "$base/api/v1/cameras" -Method POST -Body $addBody -ContentType "application/json" -WebSession $session -Headers $authHeaders -UseBasicParsing
$addJson = $addResp.Content | ConvertFrom-Json
if (-not $addJson.success -or -not $addJson.data) { throw "Add camera failed." }
$cameraId = [string]$addJson.data.id
Write-Host ("   camera id: {0}" -f $cameraId) -ForegroundColor Green

Write-Host ("3. Waiting {0}s..." -f $WaitSeconds) -ForegroundColor Yellow
Start-Sleep -Seconds $WaitSeconds

Write-Host "4. GET /api/v1/events..." -ForegroundColor Yellow
$eventsCount = 0
$eventsForCameraCount = 0
$latencyMeasured = $false
$latencyMs = -1
$latencyThresholdMs = $PollIntervalMs + $PullTimeoutMs + $LatencyBufferMs
$latencyStatus = "NOT_MEASURED"
$firstEventTimestamp = $null
$wsStatus = "NOT_RUN"
$wsEventReceived = $false
$wsEventType = $null

function Get-EventsForCamera([string]$baseUrl, $webSession, $headers, [string]$targetCameraId) {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/v1/events" -Method GET -WebSession $webSession -Headers $headers -UseBasicParsing
    $json = $resp.Content | ConvertFrom-Json
    $items = @()
    if ($json.data -and $json.data.items) { $items = @($json.data.items) }
    $forCameraItems = @($items | Where-Object { $_.cameraId -eq $targetCameraId })
    return @{
        all = $items
        forCamera = $forCameraItems
    }
}

function Wait-EventViaWebSocket([string]$apiBaseUrl, [string]$wsUrl, $webSession, $headers, [string]$targetCameraId, [int]$timeoutSeconds) {
    $result = @{
        status = "FAILED"
        eventReceived = $false
        eventType = $null
        note = ""
    }
    try {
        $wsTokenResp = Invoke-WebRequest -Uri "$apiBaseUrl/api/v1/auth/ws-token" -Method GET -WebSession $webSession -Headers $headers -UseBasicParsing
        $wsTokenJson = $wsTokenResp.Content | ConvertFrom-Json
        if (-not $wsTokenJson.success -or -not $wsTokenJson.data -or -not $wsTokenJson.data.token) {
            $result.note = "ws-token endpoint returned no token"
            return $result
        }
        $token = [string]$wsTokenJson.data.token
        $uri = [System.Uri]$wsUrl
        $ws = [System.Net.WebSockets.ClientWebSocket]::new()
        $cts = [System.Threading.CancellationTokenSource]::new()
        $cts.CancelAfter([TimeSpan]::FromSeconds($timeoutSeconds))
        $ws.ConnectAsync($uri, $cts.Token).GetAwaiter().GetResult()

        $authMsg = @{ type = "auth"; data = @{ token = $token } } | ConvertTo-Json -Compress
        $authBytes = [System.Text.Encoding]::UTF8.GetBytes($authMsg)
        $ws.SendAsync([System.ArraySegment[byte]]::new($authBytes), [System.Net.WebSockets.WebSocketMessageType]::Text, $true, $cts.Token).GetAwaiter().GetResult()

        $subMsg = @{ type = "subscribe"; data = @{ channels = @("events") } } | ConvertTo-Json -Compress
        $subBytes = [System.Text.Encoding]::UTF8.GetBytes($subMsg)
        $ws.SendAsync([System.ArraySegment[byte]]::new($subBytes), [System.Net.WebSockets.WebSocketMessageType]::Text, $true, $cts.Token).GetAwaiter().GetResult()

        $buffer = New-Object byte[] 8192
        while ($ws.State -eq [System.Net.WebSockets.WebSocketState]::Open -and -not $cts.IsCancellationRequested) {
            $seg = [System.ArraySegment[byte]]::new($buffer)
            $recv = $ws.ReceiveAsync($seg, $cts.Token).GetAwaiter().GetResult()
            if ($recv.MessageType -eq [System.Net.WebSockets.WebSocketMessageType]::Close) { break }
            if ($recv.Count -le 0) { continue }
            $txt = [System.Text.Encoding]::UTF8.GetString($buffer, 0, $recv.Count)
            $msg = $null
            try { $msg = $txt | ConvertFrom-Json } catch { continue }
            if ($msg -and $msg.channel -eq "events" -and $msg.data -and $msg.data.cameraId -eq $targetCameraId) {
                $result.status = "PASS"
                $result.eventReceived = $true
                $result.eventType = [string]$msg.type
                $result.note = "event received via websocket"
                break
            }
        }

        if ($ws.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
            $ws.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, "done", [System.Threading.CancellationToken]::None).GetAwaiter().GetResult()
        }
        $ws.Dispose()
        if (-not $result.eventReceived -and $result.note -eq "") {
            $result.status = "TIMEOUT"
            $result.note = "no matching websocket event in timeout window"
        }
        return $result
    } catch {
        $result.status = "FAILED"
        $result.note = $_.Exception.Message
        return $result
    }
}

$createdAt = Get-Date
$deadline = (Get-Date).AddSeconds($MaxEventWaitSeconds)
$latestItems = $null
do {
    try {
        $latestItems = Get-EventsForCamera -baseUrl $base -webSession $session -headers $authHeaders -targetCameraId $cameraId
        $eventsCount = @($latestItems.all).Count
        $eventsForCameraCount = @($latestItems.forCamera).Count
        if ($eventsForCameraCount -gt 0) { break }
    } catch {}
    Start-Sleep -Seconds $EventPollIntervalSeconds
} while ((Get-Date) -lt $deadline)

try {
    if (-not $latestItems) {
        $latestItems = Get-EventsForCamera -baseUrl $base -webSession $session -headers $authHeaders -targetCameraId $cameraId
        $eventsCount = @($latestItems.all).Count
        $eventsForCameraCount = @($latestItems.forCamera).Count
    }
    Write-Host ("   Total events: {0}, for camera: {1}" -f $eventsCount, $eventsForCameraCount) -ForegroundColor Green

    if ($eventsForCameraCount -gt 0) {
        $first = $latestItems.forCamera | Select-Object -First 1
        $firstEventTimestamp = $first.timestamp
        $latencyMs = [int]((Get-Date) - $createdAt).TotalMilliseconds
        $latencyMeasured = $true
        $latencyStatus = if ($latencyMs -le $latencyThresholdMs) { "PASS" } else { "FAIL" }
        Write-Host ("   Latency (measured): {0} ms (threshold: {1} ms) => {2}" -f $latencyMs, $latencyThresholdMs, $latencyStatus) -ForegroundColor $(if ($latencyStatus -eq "PASS") { "Green" } else { "Yellow" })
    } else {
        Write-Host ("   No event for camera within {0}s window; latency not measured." -f $MaxEventWaitSeconds) -ForegroundColor Yellow
    }

    if ($EnableWebSocketCheck) {
        if (-not $WebSocketUrl) {
            $wsBase = $base -replace "^http", "ws"
            $WebSocketUrl = "$wsBase/api/v1/ws"
        }
        Write-Host ("   WebSocket check against: {0}" -f $WebSocketUrl) -ForegroundColor Yellow
        $wsResult = Wait-EventViaWebSocket -apiBaseUrl $base -wsUrl $WebSocketUrl -webSession $session -headers $authHeaders -targetCameraId $cameraId -timeoutSeconds $MaxEventWaitSeconds
        $wsStatus = [string]$wsResult.status
        $wsEventReceived = [bool]$wsResult.eventReceived
        $wsEventType = $wsResult.eventType
        Write-Host ("   WebSocket status: {0}; received={1}; note={2}" -f $wsStatus, $wsEventReceived, $wsResult.note) -ForegroundColor $(if ($wsStatus -eq "PASS") { "Green" } else { "Yellow" })
    }
} catch {
    Write-Host ("   Request failed: {0}" -f $_) -ForegroundColor Yellow
}

if (-not $SkipDelete) {
    Write-Host "5. Deleting camera..." -ForegroundColor Yellow
    try {
        Invoke-WebRequest -Uri "$base/api/v1/cameras/$cameraId" -Method DELETE -WebSession $session -Headers $authHeaders -UseBasicParsing | Out-Null
        Write-Host "   OK" -ForegroundColor Green
    } catch {
        Write-Host ("   Delete failed: {0}" -f $_) -ForegroundColor Yellow
    }
} else {
    Write-Host ("5. Skipping delete. Camera id: {0}" -f $cameraId) -ForegroundColor Gray
}

if ($OutputJsonPath) {
    $outDir = Split-Path -Parent $OutputJsonPath
    if ($outDir -and -not (Test-Path $outDir)) {
        New-Item -ItemType Directory -Path $outDir -Force | Out-Null
    }
    @{
        apiBase = $base
        cameraId = $cameraId
        cameraUrl = $cameraUrl
        eventsTotal = $eventsCount
        eventsForCamera = $eventsForCameraCount
        latencyMeasured = $latencyMeasured
        latencyMs = $latencyMs
        latencyThresholdMs = $latencyThresholdMs
        latencyStatus = $latencyStatus
        firstEventTimestamp = $firstEventTimestamp
        websocketCheckEnabled = [bool]$EnableWebSocketCheck
        websocketStatus = $wsStatus
        websocketEventReceived = $wsEventReceived
        websocketEventType = $wsEventType
        skipDelete = [bool]$SkipDelete
        timestamp = [DateTime]::UtcNow.ToString("o")
    } | ConvertTo-Json -Depth 5 | Out-File -FilePath $OutputJsonPath -Encoding UTF8
    Write-Host ("Result json written: {0}" -f $OutputJsonPath) -ForegroundColor Green
}

Write-Host "Done." -ForegroundColor Cyan
