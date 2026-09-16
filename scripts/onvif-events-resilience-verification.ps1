param(
    [switch]$ShowHelp,
    [string]$ApiBase = "http://localhost:8080",
    [string]$AdminUser = "admin",
    [Alias("AdminPassword")]
    [string]$AdminInput = "",
    [System.Security.SecureString]$AdminPasswordSecure = $null,
    [int]$CameraIndex = 0,
    [int]$WaitSeconds = 15,
    [switch]$EnableWebSocketCheck = $false,
    [int]$MaxEventWaitSeconds = 30,
    [int]$EventPollIntervalSeconds = 2,
    [int]$PollIntervalMs = 5000,
    [int]$PullTimeoutMs = 500,
    [int]$LatencyBufferMs = 10000,
    [string]$WebSocketUrl = "",
    [string]$RestartCommand = "",
    [int]$RestartWaitSeconds = 20,
    [string]$OutputDir = "diagnostics\onvif-events",
    [switch]$AllowApiUnavailable = $false,
    [bool]$GenerateEvidenceReport = $true
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "ONVIF Events resilience verification (baseline + optional restart + re-subscribe)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\onvif-events-resilience-verification.ps1 -AdminPassword <pwd>"
    Write-Host "  .\scripts\onvif-events-resilience-verification.ps1 -ApiBase http://localhost:8080 -AdminUser admin -EnableWebSocketCheck -AllowApiUnavailable"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -AdminPassword / -AdminPasswordSecure / env IPCSS_ADMIN_PASSWORD"
    Write-Host "  -CameraIndex  -WaitSeconds  -OutputDir  (default diagnostics\onvif-events)"
    Write-Host "  -EnableWebSocketCheck  -WebSocketUrl  -PollIntervalMs  -LatencyBufferMs  (passed through to API verification)"
    Write-Host "  -RestartCommand  -RestartWaitSeconds  Optional API restart between phases"
    Write-Host "  -AllowApiUnavailable  Soft-mode: NOT_RUN/PARTIAL paths instead of throw on baseline/API issues"
    Write-Host "  -GenerateEvidenceReport  Call generate-phase1-onvif-evidence-report.ps1 (default true)"
    Write-Host ""
    Write-Host "Steps:"
    Write-Host "  Invokes onvif-events-api-verification.ps1 twice (phase1 with -SkipDelete, phase5 full); writes phase json + summary under -OutputDir."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Completed (PASS/PARTIAL/NOT_RUN per summary when -AllowApiUnavailable)"
    Write-Host "  1  Missing admin password, missing phase artifacts, or hard failure when API required"
    exit 0
}

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

$resolvedAdminPassword = $null
if ($AdminPasswordSecure) {
    $resolvedAdminPassword = Resolve-PlainPassword $AdminPasswordSecure
}
if (-not $resolvedAdminPassword -and $AdminInput) {
    $resolvedAdminPassword = Resolve-PlainPassword $AdminInput
}
if (-not $resolvedAdminPassword) {
    $resolvedAdminPassword = $env:IPCSS_ADMIN_PASSWORD
}
if (-not $resolvedAdminPassword) {
    Write-Host "Error: Admin password required. Use -AdminPassword or set IPCSS_ADMIN_PASSWORD." -ForegroundColor Red
    exit 1
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$baseScript = Join-Path $scriptDir "onvif-events-api-verification.ps1"
$evidenceScript = Join-Path $scriptDir "generate-phase1-onvif-evidence-report.ps1"
$outputRoot = Join-Path $projectRoot $OutputDir
if (-not (Test-Path $outputRoot)) {
    New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null
}

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$phase1Json = Join-Path $outputRoot "onvif-resilience-$runId-phase1.json"
$phase2Json = Join-Path $outputRoot "onvif-resilience-$runId-phase2.json"
$summaryPath = Join-Path $outputRoot "onvif-resilience-$runId-summary.md"

Write-Host "=== ONVIF Events resilience verification ===" -ForegroundColor Cyan
Write-Host "Run id: $runId"
Write-Host "Output: $outputRoot"
Write-Host ""

function Invoke-EvidenceReport([string]$onvifStatus, [string]$noteText) {
    if (-not $GenerateEvidenceReport) { return }
    if (-not (Test-Path $evidenceScript)) {
        Write-Host "Evidence script not found: $evidenceScript" -ForegroundColor Yellow
        return
    }
    & $evidenceScript `
        -Onvif143Status $onvifStatus `
        -Unit141Status NOT_RUN `
        -UiRegressionStatus NOT_RUN `
        -Notes $noteText
}

function Write-ResilienceSummary(
    [string]$status,
    [string]$noteText,
    [string]$cameraId,
    [int]$baselineTotal,
    [int]$baselineCamera,
    [int]$restartCamera,
    [int]$resubTotal,
    [int]$resubCamera,
    [string]$phase1WebSocketStatus,
    [string]$phase1LatencyStatus,
    [string]$phase2WebSocketStatus,
    [string]$phase2LatencyStatus
) {
    $restartUsed = if ($RestartCommand) { $RestartCommand } else { "<none>" }
    $summary = @(
        "# ONVIF Events resilience verification",
        "",
        "- Run ID: $runId",
        "- API: $ApiBase",
        "- Camera index: $CameraIndex",
        "- Restart command used: $restartUsed",
        "- Status: $status",
        "",
        "## Results",
        "",
        "- Baseline pass camera id: $cameraId",
        "- Baseline /events count: $baselineTotal",
        "- Baseline /events for camera: $baselineCamera",
        "- Post-restart /events for same camera (pre-cleanup): $restartCamera",
        "- Re-subscribe pass /events total: $resubTotal",
        "- Re-subscribe pass /events for camera: $resubCamera",
        "- Baseline websocket status: $phase1WebSocketStatus",
        "- Baseline latency status: $phase1LatencyStatus",
        "- Re-subscribe websocket status: $phase2WebSocketStatus",
        "- Re-subscribe latency status: $phase2LatencyStatus",
        "",
        "## Notes",
        "",
        "- $noteText",
        "",
        "## Verdict",
        "",
        "- PASS: both baseline and re-subscribe phases completed successfully.",
        "- PARTIAL: part of scenario completed, but at least one phase failed.",
        "- NOT_RUN: API/camera was unavailable and soft mode was enabled.",
        "- FAIL: critical failure in strict mode."
    )
    $summary -join "`r`n" | Out-File -FilePath $summaryPath -Encoding UTF8
}

$onvifStatus = "PASS"
$statusNote = "Baseline and resilience cycle completed."
$cameraId = ""
$phase1EventsTotal = 0
$phase1EventsForCamera = 0
$postRestartEventsForCamera = 0
$phase2EventsTotal = 0
$phase2EventsForCamera = 0
$phase1WebSocketStatus = "NOT_RUN"
$phase1LatencyStatus = "NOT_MEASURED"
$phase2WebSocketStatus = "NOT_RUN"
$phase2LatencyStatus = "NOT_MEASURED"

Write-Host "Phase 1: baseline add/subscribe/events (keep camera)..." -ForegroundColor Yellow
try {
    & $baseScript `
        -ApiBase $ApiBase `
        -AdminUser $AdminUser `
        -AdminPassword $resolvedAdminPassword `
        -CameraIndex $CameraIndex `
        -WaitSeconds $WaitSeconds `
        -EnableWebSocketCheck:$EnableWebSocketCheck `
        -MaxEventWaitSeconds $MaxEventWaitSeconds `
        -EventPollIntervalSeconds $EventPollIntervalSeconds `
        -PollIntervalMs $PollIntervalMs `
        -PullTimeoutMs $PullTimeoutMs `
        -LatencyBufferMs $LatencyBufferMs `
        -WebSocketUrl $WebSocketUrl `
        -SkipDelete `
        -OutputJsonPath $phase1Json
} catch {
    if ($AllowApiUnavailable) {
        $onvifStatus = "NOT_RUN"
        $statusNote = "Baseline phase not executed: $($_.Exception.Message)"
        Write-Host $statusNote -ForegroundColor Yellow
        Write-ResilienceSummary -status $onvifStatus -noteText $statusNote -cameraId "<not available>" -baselineTotal 0 -baselineCamera 0 -restartCamera 0 -resubTotal 0 -resubCamera 0 -phase1WebSocketStatus $phase1WebSocketStatus -phase1LatencyStatus $phase1LatencyStatus -phase2WebSocketStatus $phase2WebSocketStatus -phase2LatencyStatus $phase2LatencyStatus
        Invoke-EvidenceReport -onvifStatus $onvifStatus -noteText $statusNote
        Write-Host "Summary: $summaryPath" -ForegroundColor Green
        exit 0
    }
    throw
}

if (-not (Test-Path $phase1Json)) {
    $message = "Phase 1 result not found: $phase1Json"
    if ($AllowApiUnavailable) {
        $onvifStatus = "NOT_RUN"
        $statusNote = $message
        Write-ResilienceSummary -status $onvifStatus -noteText $statusNote -cameraId "<not available>" -baselineTotal 0 -baselineCamera 0 -restartCamera 0 -resubTotal 0 -resubCamera 0 -phase1WebSocketStatus $phase1WebSocketStatus -phase1LatencyStatus $phase1LatencyStatus -phase2WebSocketStatus $phase2WebSocketStatus -phase2LatencyStatus $phase2LatencyStatus
        Invoke-EvidenceReport -onvifStatus $onvifStatus -noteText $statusNote
        Write-Host "Summary: $summaryPath" -ForegroundColor Green
        exit 0
    }
    Write-Host $message -ForegroundColor Red
    exit 1
}
$phase1 = Get-Content $phase1Json -Raw | ConvertFrom-Json
$cameraId = [string]$phase1.cameraId
$phase1EventsTotal = [int]$phase1.eventsTotal
$phase1EventsForCamera = [int]$phase1.eventsForCamera
$phase1WebSocketStatus = if ($phase1.websocketStatus) { [string]$phase1.websocketStatus } else { "NOT_RUN" }
$phase1LatencyStatus = if ($phase1.latencyStatus) { [string]$phase1.latencyStatus } else { "NOT_MEASURED" }
if (-not $cameraId) {
    Write-Host "Phase 1 failed: cameraId missing in json output." -ForegroundColor Red
    exit 1
}

if ($RestartCommand) {
    Write-Host "Phase 2: restart command..." -ForegroundColor Yellow
    Write-Host "  $RestartCommand" -ForegroundColor DarkGray
    Invoke-Expression $RestartCommand
    Start-Sleep -Seconds $RestartWaitSeconds
}

Write-Host "Phase 3: API availability and events endpoint after restart..." -ForegroundColor Yellow
$base = $ApiBase.TrimEnd('/')
$session = $null
$authHeaders = @{}
$loginBody = @{ username = $AdminUser; password = $resolvedAdminPassword } | ConvertTo-Json
try {
    $loginResp = Invoke-WebRequest -Uri "$base/api/v1/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -SessionVariable session -UseBasicParsing
    $setCookieValues = @($loginResp.Headers["Set-Cookie"])
    foreach ($cookie in $setCookieValues) {
        if ($cookie -match "access_token=([^;]+)") {
            $authHeaders["Authorization"] = "Bearer $($matches[1])"
            break
        }
    }
    $eventsResp = Invoke-WebRequest -Uri "$base/api/v1/events" -Method GET -WebSession $session -Headers $authHeaders -UseBasicParsing
    $eventsJson = $eventsResp.Content | ConvertFrom-Json
    $events = @()
    if ($eventsJson.data.items) { $events = @($eventsJson.data.items) }
    $eventsForCamera = @($events | Where-Object { $_.cameraId -eq $cameraId })
    $postRestartEventsForCamera = $eventsForCamera.Count
} catch {
    if ($AllowApiUnavailable) {
        $onvifStatus = "PARTIAL"
        $statusNote = "Restart/reconnect phase failed: $($_.Exception.Message)"
        Write-Host $statusNote -ForegroundColor Yellow
    } else {
        throw
    }
}

Write-Host "Phase 4: cleanup baseline camera..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri "$base/api/v1/cameras/$cameraId" -Method DELETE -WebSession $session -Headers $authHeaders -UseBasicParsing | Out-Null
    Write-Host "  deleted $cameraId" -ForegroundColor Green
} catch {
    Write-Host "  warning: delete failed for $cameraId : $_" -ForegroundColor Yellow
}

Write-Host "Phase 5: re-subscribe cycle (add/delete)..." -ForegroundColor Yellow
try {
    & $baseScript `
        -ApiBase $ApiBase `
        -AdminUser $AdminUser `
        -AdminPassword $resolvedAdminPassword `
        -CameraIndex $CameraIndex `
        -WaitSeconds $WaitSeconds `
        -EnableWebSocketCheck:$EnableWebSocketCheck `
        -MaxEventWaitSeconds $MaxEventWaitSeconds `
        -EventPollIntervalSeconds $EventPollIntervalSeconds `
        -PollIntervalMs $PollIntervalMs `
        -PullTimeoutMs $PullTimeoutMs `
        -LatencyBufferMs $LatencyBufferMs `
        -WebSocketUrl $WebSocketUrl `
        -OutputJsonPath $phase2Json
} catch {
    if ($AllowApiUnavailable) {
        $onvifStatus = "PARTIAL"
        $statusNote = "Re-subscribe phase failed: $($_.Exception.Message)"
        Write-Host $statusNote -ForegroundColor Yellow
    } else {
        throw
    }
}

if (Test-Path $phase2Json) {
    $phase2 = Get-Content $phase2Json -Raw | ConvertFrom-Json
    $phase2EventsTotal = [int]$phase2.eventsTotal
    $phase2EventsForCamera = [int]$phase2.eventsForCamera
    $phase2WebSocketStatus = if ($phase2.websocketStatus) { [string]$phase2.websocketStatus } else { "NOT_RUN" }
    $phase2LatencyStatus = if ($phase2.latencyStatus) { [string]$phase2.latencyStatus } else { "NOT_MEASURED" }
} else {
    if ($AllowApiUnavailable) {
        $onvifStatus = "PARTIAL"
        $statusNote = "Phase 5 result not found: $phase2Json"
    } else {
        Write-Host "Phase 5 result not found: $phase2Json" -ForegroundColor Red
        exit 1
    }
}

if ($onvifStatus -eq "PASS") {
    if ($EnableWebSocketCheck -and ($phase1WebSocketStatus -ne "PASS" -or $phase2WebSocketStatus -ne "PASS")) {
        $onvifStatus = "PARTIAL"
        $statusNote = "WebSocket validation not fully passed (phase1=$phase1WebSocketStatus, phase2=$phase2WebSocketStatus)."
    } elseif ($phase1LatencyStatus -eq "FAIL" -or $phase2LatencyStatus -eq "FAIL") {
        $onvifStatus = "PARTIAL"
        $statusNote = "Latency gate failed (phase1=$phase1LatencyStatus, phase2=$phase2LatencyStatus)."
    }
}

Write-ResilienceSummary `
    -status $onvifStatus `
    -noteText $statusNote `
    -cameraId $cameraId `
    -baselineTotal $phase1EventsTotal `
    -baselineCamera $phase1EventsForCamera `
    -restartCamera $postRestartEventsForCamera `
    -resubTotal $phase2EventsTotal `
    -resubCamera $phase2EventsForCamera `
    -phase1WebSocketStatus $phase1WebSocketStatus `
    -phase1LatencyStatus $phase1LatencyStatus `
    -phase2WebSocketStatus $phase2WebSocketStatus `
    -phase2LatencyStatus $phase2LatencyStatus
Invoke-EvidenceReport -onvifStatus $onvifStatus -noteText $statusNote

Write-Host ""
Write-Host "Resilience verification complete." -ForegroundColor Cyan
Write-Host "Summary: $summaryPath" -ForegroundColor Green
