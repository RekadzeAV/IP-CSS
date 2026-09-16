[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$ApiBase = "http://localhost:8080",
    [string]$AdminUser = "admin",
    [Alias("AdminPassword")]
    [string]$AdminInput = "",
    [System.Security.SecureString]$AdminPasswordSecure = $null,
    [string]$CameraId = "",
    [int]$EventTimeoutSeconds = 15,
    [string]$EvidenceReportDir = "diagnostics\recording-ws-acceptance",
    [string]$WsLogDir = "diagnostics\recording-ws-acceptance",
    [string]$Notes = ""
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

if ($ShowHelp) {
    Write-Host "Run recording WS lifecycle acceptance (semi-automated)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-recording-ws-lifecycle-acceptance.ps1 -CameraId cam-1 -AdminPassword <pwd>"
    Write-Host "  .\scripts\run-recording-ws-lifecycle-acceptance.ps1 -ApiBase http://localhost:8080 -CameraId cam-1 -EventTimeoutSeconds 20"
    Write-Host ""
    Write-Host "Behavior:"
    Write-Host "  1) Login to API"
    Write-Host "  2) Obtain WS token and subscribe to 'recordings'"
    Write-Host "  3) Trigger start/pause/resume/stop endpoints"
    Write-Host "  4) Wait for recording_* events from WebSocket"
    Write-Host "  5) Generate evidence via recording-ws-lifecycle-acceptance-evidence.ps1"
    Write-Host ""
    Write-Host "Output:"
    Write-Host "  WS log: diagnostics/recording-ws-acceptance/recording-ws-frames-<run-id>.log"
    Write-Host "  Evidence: diagnostics/recording-ws-acceptance/recording-ws-lifecycle-acceptance-<run-id>.md|json"
    exit 0
}

if ([string]::IsNullOrWhiteSpace($CameraId)) {
    throw "CameraId is required. Use -CameraId <id>."
}

$resolvedAdminPassword = $null
if ($AdminPasswordSecure) {
    $resolvedAdminPassword = Resolve-PlainPassword $AdminPasswordSecure
}
if (-not $resolvedAdminPassword -and $AdminInput) {
    $resolvedAdminPassword = Resolve-PlainPassword $AdminInput
}
if (-not $resolvedAdminPassword) { $resolvedAdminPassword = $env:IPCSS_ADMIN_PASSWORD }
if (-not $resolvedAdminPassword) { throw "Admin password required (-AdminPassword or IPCSS_ADMIN_PASSWORD)." }

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$evidenceScript = Join-Path $scriptDir "recording-ws-lifecycle-acceptance-evidence.ps1"
if (-not (Test-Path -LiteralPath $evidenceScript)) {
    throw "Evidence script not found: $evidenceScript"
}

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$resolvedWsLogDir = Join-Path $projectRoot $WsLogDir
if (-not (Test-Path -LiteralPath $resolvedWsLogDir)) {
    New-Item -ItemType Directory -Path $resolvedWsLogDir -Force | Out-Null
}
$wsLogPath = Join-Path $resolvedWsLogDir ("recording-ws-frames-{0}.log" -f $runId)

$base = $ApiBase.TrimEnd("/")
$wsUrl = ($base -replace "^http", "ws") + "/api/v1/ws"

function Write-WsLogLine {
    param([string]$line)
    Add-Content -LiteralPath $wsLogPath -Value $line -Encoding UTF8
}

function Invoke-ApiJson {
    param(
        [string]$Uri,
        [string]$Method,
        [string]$Body = "",
        $WebSession,
        [hashtable]$Headers
    )
    if ([string]::IsNullOrWhiteSpace($Body)) {
        $resp = Invoke-WebRequest -Uri $Uri -Method $Method -WebSession $WebSession -Headers $Headers -UseBasicParsing
    } else {
        $resp = Invoke-WebRequest -Uri $Uri -Method $Method -Body $Body -ContentType "application/json" -WebSession $WebSession -Headers $Headers -UseBasicParsing
    }
    if ([string]::IsNullOrWhiteSpace($resp.Content)) {
        return $null
    }
    return ($resp.Content | ConvertFrom-Json)
}

function Receive-ExpectedRecordingEvent {
    param(
        [System.Net.WebSockets.ClientWebSocket]$Socket,
        [System.Threading.CancellationToken]$Token,
        [string]$ExpectedType,
        [int]$TimeoutSeconds
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline -and $Socket.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
        $buffer = New-Object byte[] 16384
        $seg = [System.ArraySegment[byte]]::new($buffer)
        $recv = $Socket.ReceiveAsync($seg, $Token).GetAwaiter().GetResult()
        if ($recv.MessageType -eq [System.Net.WebSockets.WebSocketMessageType]::Close) {
            Write-WsLogLine ("[{0}] ws-close-frame" -f (Get-Date).ToString("o"))
            break
        }
        if ($recv.Count -le 0) { continue }

        $txt = [System.Text.Encoding]::UTF8.GetString($buffer, 0, $recv.Count)
        Write-WsLogLine ("[{0}] {1}" -f (Get-Date).ToString("o"), $txt)
        $msg = $null
        try { $msg = $txt | ConvertFrom-Json } catch { continue }
        if ($msg -and $msg.channel -eq "recordings" -and $msg.type -eq $ExpectedType) {
            return @{ ok = $true; message = $msg }
        }
    }
    return @{ ok = $false; message = $null }
}

$session = $null
$authHeaders = @{}
$recordingId = ""
$startedState = "NOT_RUN"
$pausedState = "NOT_RUN"
$resumedState = "NOT_RUN"
$stoppedState = "NOT_RUN"
$isolationState = "NOT_RUN"
$socket = $null
$cts = $null

try {
    Write-Host "1) Login..." -ForegroundColor Yellow
    $loginBody = @{ username = $AdminUser; password = $resolvedAdminPassword } | ConvertTo-Json
    $loginResp = Invoke-WebRequest -Uri "$base/api/v1/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -SessionVariable session -UseBasicParsing
    $loginJson = $loginResp.Content | ConvertFrom-Json
    if (-not $loginJson.success) { throw "Login failed." }

    $cookieHeader = [string]$loginResp.Headers["Set-Cookie"]
    if ($cookieHeader -and $cookieHeader.Contains("access_token=")) {
        $tokenPart = ($cookieHeader -split ";")[0]
        $accessToken = $tokenPart.Replace("access_token=", "")
        if ($accessToken) {
            $authHeaders["Authorization"] = "Bearer $accessToken"
        }
    }

    Write-Host "2) Obtain ws-token and connect WebSocket..." -ForegroundColor Yellow
    $wsTokenJson = Invoke-ApiJson -Uri "$base/api/v1/auth/ws-token" -Method GET -WebSession $session -Headers $authHeaders
    if (-not $wsTokenJson.success -or -not $wsTokenJson.data -or -not $wsTokenJson.data.token) {
        throw "ws-token endpoint returned no token."
    }
    $wsToken = [string]$wsTokenJson.data.token

    $socket = [System.Net.WebSockets.ClientWebSocket]::new()
    $cts = [System.Threading.CancellationTokenSource]::new()
    $cts.CancelAfter([TimeSpan]::FromSeconds([Math]::Max(60, $EventTimeoutSeconds * 6)))
    $socket.ConnectAsync([System.Uri]$wsUrl, $cts.Token).GetAwaiter().GetResult()

    $authMsg = @{ type = "auth"; data = @{ token = $wsToken } } | ConvertTo-Json -Compress
    $authBytes = [System.Text.Encoding]::UTF8.GetBytes($authMsg)
    $socket.SendAsync([System.ArraySegment[byte]]::new($authBytes), [System.Net.WebSockets.WebSocketMessageType]::Text, $true, $cts.Token).GetAwaiter().GetResult()
    Write-WsLogLine ("[{0}] >>> {1}" -f (Get-Date).ToString("o"), $authMsg)

    $subMsg = @{ type = "subscribe"; data = @{ channels = @("recordings") } } | ConvertTo-Json -Compress
    $subBytes = [System.Text.Encoding]::UTF8.GetBytes($subMsg)
    $socket.SendAsync([System.ArraySegment[byte]]::new($subBytes), [System.Net.WebSockets.WebSocketMessageType]::Text, $true, $cts.Token).GetAwaiter().GetResult()
    Write-WsLogLine ("[{0}] >>> {1}" -f (Get-Date).ToString("o"), $subMsg)

    Write-Host "3) Trigger recording start..." -ForegroundColor Yellow
    $startBody = @{ cameraId = $CameraId; format = "mp4"; quality = "high" } | ConvertTo-Json
    $startJson = Invoke-ApiJson -Uri "$base/api/v1/recordings/start" -Method POST -Body $startBody -WebSession $session -Headers $authHeaders
    if (-not $startJson.success -or -not $startJson.data -or -not $startJson.data.recordingId) {
        throw "start recording failed for cameraId=$CameraId"
    }
    $recordingId = [string]$startJson.data.recordingId
    $startEvent = Receive-ExpectedRecordingEvent -Socket $socket -Token $cts.Token -ExpectedType "recording_started" -TimeoutSeconds $EventTimeoutSeconds
    $startedState = if ($startEvent.ok) { "PASS" } else { "FAIL" }

    Write-Host "4) Trigger recording pause..." -ForegroundColor Yellow
    $pauseJson = Invoke-ApiJson -Uri "$base/api/v1/recordings/pause/$CameraId" -Method POST -WebSession $session -Headers $authHeaders
    if (-not $pauseJson.success) {
        throw "pause recording failed for cameraId=$CameraId"
    }
    $pauseEvent = Receive-ExpectedRecordingEvent -Socket $socket -Token $cts.Token -ExpectedType "recording_paused" -TimeoutSeconds $EventTimeoutSeconds
    $pausedState = if ($pauseEvent.ok) { "PASS" } else { "FAIL" }

    Write-Host "5) Trigger recording resume..." -ForegroundColor Yellow
    $resumeJson = Invoke-ApiJson -Uri "$base/api/v1/recordings/resume/$CameraId" -Method POST -WebSession $session -Headers $authHeaders
    if (-not $resumeJson.success) {
        throw "resume recording failed for cameraId=$CameraId"
    }
    $resumeEvent = Receive-ExpectedRecordingEvent -Socket $socket -Token $cts.Token -ExpectedType "recording_resumed" -TimeoutSeconds $EventTimeoutSeconds
    $resumedState = if ($resumeEvent.ok) { "PASS" } else { "FAIL" }

    Write-Host "6) Trigger recording stop..." -ForegroundColor Yellow
    $stopJson = Invoke-ApiJson -Uri "$base/api/v1/recordings/stop/$CameraId" -Method POST -WebSession $session -Headers $authHeaders
    if (-not $stopJson.success) {
        throw "stop recording failed for cameraId=$CameraId"
    }
    $stopEvent = Receive-ExpectedRecordingEvent -Socket $socket -Token $cts.Token -ExpectedType "recording_stopped" -TimeoutSeconds $EventTimeoutSeconds
    $stoppedState = if ($stopEvent.ok) { "PASS" } else { "FAIL" }

    # Isolation is not directly asserted in this single-session runner.
    $isolationState = "NOT_RUN"
}
finally {
    if ($socket -and $socket.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
        try {
            $socket.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, "done", [System.Threading.CancellationToken]::None).GetAwaiter().GetResult()
        } catch {}
    }
    if ($socket) { $socket.Dispose() }
    if ($cts) { $cts.Dispose() }
}

Write-Host "7) Generate evidence..." -ForegroundColor Yellow
$resolvedEvidenceReportDir = Join-Path $projectRoot $EvidenceReportDir
$evidenceArgs = @(
    "-EnvironmentName", "semi-auto",
    "-CameraId", $CameraId,
    "-RecordingId", $recordingId,
    "-StartedEvent", $startedState,
    "-PausedEvent", $pausedState,
    "-ResumedEvent", $resumedState,
    "-StoppedEvent", $stoppedState,
    "-ChannelIsolation", $isolationState,
    "-WsLogPath", $wsLogPath,
    "-ReportDir", $resolvedEvidenceReportDir,
    "-Notes", $Notes
)

& $evidenceScript @evidenceArgs
$evidenceExit = $LASTEXITCODE
if ($evidenceExit -ne 0) {
    Write-Host "Evidence marked FAIL." -ForegroundColor Red
    exit $evidenceExit
}

Write-Host "Done." -ForegroundColor Green
exit 0
