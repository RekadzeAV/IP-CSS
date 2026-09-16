param(
    [switch]$SkipDesktopTests,
    [switch]$SkipStatusSync,
    [switch]$RunRtspIntegration = $false,
    [switch]$RunWebSocketLive = $false,
    [switch]$RunNetworkSmoke = $false,
    [string]$CameraConfigPath = "config\test-cameras.local.json",
    [string]$ReportDir = "diagnostics\network-layer-automation",
    [int]$RtspSoakDurationSec = 60,
    [int]$RtspSoakMaxConnectFailures = 0,
    [int]$RtspSoakMaxConsecutiveFailures = 0,
    [int]$RtspSoakMaxReconnectFailures = 0,
    [int]$RtspSoakMaxFrameSilenceMs = 15000,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Run automated 1.4 network-layer checks (desktop-first)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-network-layer-automation.ps1 -ShowHelp"
    Write-Host "  .\scripts\run-network-layer-automation.ps1 [-SkipDesktopTests] [-SkipStatusSync] [-RunRtspIntegration] [-RunWebSocketLive] [-RunNetworkSmoke] [-CameraConfigPath <path>] [-ReportDir <dir>] [-RtspSoakDurationSec N] [-RtspSoakMaxConnectFailures N] [-RtspSoakMaxConsecutiveFailures N] [-RtspSoakMaxReconnectFailures N] [-RtspSoakMaxFrameSilenceMs N]"
    Write-Host ""
    Write-Host "Flags:"
    Write-Host "  -SkipDesktopTests  Skip stable desktop test suite for core:network"
    Write-Host "  -SkipStatusSync    Skip 1.4 status synchronization/audit step"
    Write-Host "  -RunRtspIntegration Enable opt-in RTSP integration tests via env"
    Write-Host "  -RunWebSocketLive  Enable opt-in live WebSocket integration test via env"
    Write-Host "  -RunNetworkSmoke   Run scripts/network-layer-smoke-test.ps1"
    Write-Host "  -CameraConfigPath  Path to camera config for smoke script"
    Write-Host "  -ReportDir         Output dir for json/md summary"
    Write-Host "  -RtspSoakDurationSec            RTSP soak duration threshold input"
    Write-Host "  -RtspSoakMaxConnectFailures     Max allowed connect failures"
    Write-Host "  -RtspSoakMaxConsecutiveFailures Max allowed consecutive failures"
    Write-Host "  -RtspSoakMaxReconnectFailures   Max allowed reconnect failures"
    Write-Host "  -RtspSoakMaxFrameSilenceMs      Max allowed frame silence during soak"
    Write-Host ""
    Write-Host "Env used by opt-in tests:"
    Write-Host "  ENABLE_RTSP_INTEGRATION_TESTS=true"
    Write-Host "  TEST_RTSP_URL=rtsp://..."
    Write-Host "  ENABLE_WEBSOCKET_LIVE_TESTS=true"
    Write-Host "  TEST_WEBSOCKET_URL=wss://..."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0 - all selected steps passed"
    Write-Host "  1 - at least one selected step failed"
    exit 0
}

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$resolvedReportRoot = if ([System.IO.Path]::IsPathRooted($ReportDir)) { $ReportDir } else { Join-Path $projectRoot $ReportDir }
$resolvedRunDir = Join-Path $resolvedReportRoot $runId
New-Item -ItemType Directory -Path $resolvedRunDir -Force | Out-Null
Write-Host "Report dir: $resolvedRunDir" -ForegroundColor DarkCyan

if ($RunRtspIntegration) {
    $env:ENABLE_RTSP_INTEGRATION_TESTS = "true"
    $env:RTSP_SOAK_DURATION_SEC = [string]$RtspSoakDurationSec
    $env:RTSP_SOAK_MAX_CONNECT_FAILURES = [string]$RtspSoakMaxConnectFailures
    $env:RTSP_SOAK_MAX_CONSECUTIVE_FAILURES = [string]$RtspSoakMaxConsecutiveFailures
    $env:RTSP_SOAK_MAX_RECONNECT_FAILURES = [string]$RtspSoakMaxReconnectFailures
    $env:RTSP_SOAK_MAX_FRAME_SILENCE_MS = [string]$RtspSoakMaxFrameSilenceMs
}
if ($RunWebSocketLive) {
    $env:ENABLE_WEBSOCKET_LIVE_TESTS = "true"
}

if ($RunRtspIntegration -and [string]::IsNullOrWhiteSpace($env:TEST_RTSP_URL)) {
    throw "RunRtspIntegration requires TEST_RTSP_URL"
}
if ($RunWebSocketLive -and [string]::IsNullOrWhiteSpace($env:TEST_WEBSOCKET_URL)) {
    throw "RunWebSocketLive requires TEST_WEBSOCKET_URL"
}

$failures = New-Object System.Collections.Generic.List[string]
$stepResults = New-Object System.Collections.Generic.List[object]

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    Write-Host ""
    Write-Host "=== $Name ===" -ForegroundColor Cyan
    $startedAt = Get-Date
    try {
        & $Action
        Write-Host "[PASS] $Name" -ForegroundColor Green
        $stepResults.Add([PSCustomObject]@{
            name = $Name
            status = "PASS"
            startedAt = $startedAt.ToString("s")
            finishedAt = (Get-Date).ToString("s")
            error = ""
        })
    } catch {
        $errorMessage = $_.Exception.Message
        Write-Host "[FAIL] $Name :: $errorMessage" -ForegroundColor Red
        $script:failures.Add($Name)
        $stepResults.Add([PSCustomObject]@{
            name = $Name
            status = "FAIL"
            startedAt = $startedAt.ToString("s")
            finishedAt = (Get-Date).ToString("s")
            error = $errorMessage
        })
    }
}

if (-not $SkipDesktopTests) {
    Invoke-Step -Name "core:network desktopTest" -Action {
        Push-Location $projectRoot
        try {
            & .\gradlew :core:network:desktopTest
            if ($LASTEXITCODE -ne 0) {
                throw "gradlew returned exit code $LASTEXITCODE"
            }
        } finally {
            Pop-Location
        }
    }
}

if (-not $SkipStatusSync) {
    Invoke-Step -Name "network-layer 1.4 status sync" -Action {
        Push-Location $projectRoot
        try {
            & .\scripts\sync-network-layer-1-4-status.ps1
            if ($LASTEXITCODE -ne 0) {
                throw "sync-network-layer-1-4-status returned exit code $LASTEXITCODE"
            }
        } finally {
            Pop-Location
        }
    }
}

if ($RunNetworkSmoke) {
    Invoke-Step -Name "network-layer smoke script" -Action {
        Push-Location $projectRoot
        try {
            & .\scripts\network-layer-smoke-test.ps1 -ConfigPath $CameraConfigPath
            if ($LASTEXITCODE -ne 0) {
                throw "network-layer-smoke-test returned exit code $LASTEXITCODE"
            }
        } finally {
            Pop-Location
        }
    }
}

Write-Host ""
$overallStatus = if ($failures.Count -gt 0) { "FAIL" } else { "PASS" }
$summary = [PSCustomObject]@{
    runId = $runId
    generatedAt = (Get-Date).ToString("s")
    overallStatus = $overallStatus
    options = [PSCustomObject]@{
        skipDesktopTests = [bool]$SkipDesktopTests
        skipStatusSync = [bool]$SkipStatusSync
        runRtspIntegration = [bool]$RunRtspIntegration
        runWebSocketLive = [bool]$RunWebSocketLive
        runNetworkSmoke = [bool]$RunNetworkSmoke
        cameraConfigPath = $CameraConfigPath
        reportDir = $resolvedRunDir
        rtspThresholds = [PSCustomObject]@{
            soakDurationSec = $RtspSoakDurationSec
            maxConnectFailures = $RtspSoakMaxConnectFailures
            maxConsecutiveFailures = $RtspSoakMaxConsecutiveFailures
            maxReconnectFailures = $RtspSoakMaxReconnectFailures
            maxFrameSilenceMs = $RtspSoakMaxFrameSilenceMs
        }
    }
    environment = [PSCustomObject]@{
        enableRtspIntegrationTests = $env:ENABLE_RTSP_INTEGRATION_TESTS
        testRtspUrlPresent = -not [string]::IsNullOrWhiteSpace($env:TEST_RTSP_URL)
        enableWebSocketLiveTests = $env:ENABLE_WEBSOCKET_LIVE_TESTS
        testWebSocketUrlPresent = -not [string]::IsNullOrWhiteSpace($env:TEST_WEBSOCKET_URL)
    }
    steps = $stepResults.ToArray()
    failedSteps = $failures.ToArray()
}

$summaryJsonPath = Join-Path $resolvedRunDir "network-layer-automation-summary.json"
$summaryMdPath = Join-Path $resolvedRunDir "network-layer-automation-summary.md"
$summary | ConvertTo-Json -Depth 8 | Set-Content -Path $summaryJsonPath -Encoding UTF8

$md = @()
$md += "# Network Layer Automation Summary"
$md += ""
$md += "- runId: $runId"
$md += "- generatedAt: $($summary.generatedAt)"
$md += "- overallStatus: $overallStatus"
$md += ""
$md += "## Selected Options"
$md += ""
$md += "- skipDesktopTests: $([bool]$SkipDesktopTests)"
$md += "- skipStatusSync: $([bool]$SkipStatusSync)"
$md += "- runRtspIntegration: $([bool]$RunRtspIntegration)"
$md += "- runWebSocketLive: $([bool]$RunWebSocketLive)"
$md += "- runNetworkSmoke: $([bool]$RunNetworkSmoke)"
$md += "- cameraConfigPath: $CameraConfigPath"
$md += "- rtspThresholds: duration=$RtspSoakDurationSec, connectFail<=$RtspSoakMaxConnectFailures, consecutiveFail<=$RtspSoakMaxConsecutiveFailures, reconnectFail<=$RtspSoakMaxReconnectFailures, frameSilenceMs<=$RtspSoakMaxFrameSilenceMs"
$md += ""
$md += "## Steps"
$md += ""
foreach ($step in $stepResults) {
    $line = "- [$($step.status)] $($step.name)"
    if ($step.error) {
        $line = "$line :: $($step.error)"
    }
    $md += $line
}
$md += ""
$md += "## Artifacts"
$md += ""
$md += "- json: $summaryJsonPath"
$md += "- md: $summaryMdPath"
$md | Set-Content -Path $summaryMdPath -Encoding UTF8

Write-Host "Summary JSON: $summaryJsonPath" -ForegroundColor DarkCyan
Write-Host "Summary MD:   $summaryMdPath" -ForegroundColor DarkCyan

if ($failures.Count -gt 0) {
    Write-Host "Completed with failures:" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

Write-Host "All selected network-layer automation steps passed." -ForegroundColor Green
exit 0
