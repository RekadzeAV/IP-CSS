[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$OutputDir = "docs\reports",
    [ValidateSet("Strict", "MvpCi")]
    [string]$DecisionProfile = "Strict"
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Phase 1 consolidated Go/No-Go summary (aggregates gate artifacts)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\generate-phase1-go-no-go-summary.ps1"
    Write-Host "  .\scripts\generate-phase1-go-no-go-summary.ps1 -DecisionProfile MvpCi"
    Write-Host "  .\scripts\generate-phase1-go-no-go-summary.ps1 -OutputDir docs\reports"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -DecisionProfile Strict|MvpCi  How optional/partial signals affect final decision (default Strict)."
    Write-Host "  -OutputDir  Repo-relative folder for PHASE1_GO_NO_GO_SUMMARY_<date>.md|.json"
    Write-Host ""
    Write-Host "Inputs (latest or fixed paths under repo):"
    Write-Host "  release-build/test/video-e2e-go-no-go-report.md"
    Write-Host "  release-build/test/security-mvp-readiness-report.md"
    Write-Host "  release-build/test/recording-ws-gate-wrapper-*.json"
    Write-Host "  diagnostics/onvif-events/phase1-onvif-acceptance-*.json"
    Write-Host "  diagnostics/platform-smoke/android|desktop/*-smoke-*.json"
    Write-Host "  diagnostics/platform-smoke/w4-mvp-platform-gate-*.json (context only)"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Final decision GO"
    Write-Host "  2  Final decision NO_GO"
    Write-Host "  3  Final decision CONDITIONAL"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$resolvedOutputDir = Join-Path $projectRoot $OutputDir
if (-not (Test-Path $resolvedOutputDir)) {
    New-Item -ItemType Directory -Path $resolvedOutputDir -Force | Out-Null
}

function Get-LatestFile([string]$dir, [string]$filter) {
    if (-not (Test-Path $dir)) { return $null }
    return Get-ChildItem -Path $dir -File -Filter $filter -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
}

function Read-StatusFromMarkdown([string]$path, [string]$pattern, [string]$default = "NOT_RUN") {
    if (-not (Test-Path $path)) { return $default }
    $content = Get-Content -Path $path -Raw
    $m = [regex]::Match($content, $pattern, "IgnoreCase")
    if ($m.Success) { return $m.Groups[1].Value.Trim() }
    return $default
}

$videoReport = Join-Path $projectRoot "release-build\test\video-e2e-go-no-go-report.md"
$securityReport = Join-Path $projectRoot "release-build\test\security-mvp-readiness-report.md"
$onvifJson = Get-LatestFile (Join-Path $projectRoot "diagnostics\onvif-events") "phase1-onvif-acceptance-*.json"
$androidJson = Get-LatestFile (Join-Path $projectRoot "diagnostics\platform-smoke\android") "android-video-background-smoke-*.json"
$desktopJson = Get-LatestFile (Join-Path $projectRoot "diagnostics\platform-smoke\desktop") "desktop-video-event-smoke-*.json"
$w4Json = Get-LatestFile (Join-Path $projectRoot "diagnostics\platform-smoke") "w4-mvp-platform-gate-*.json"
$recordingWsGateJson = Get-LatestFile (Join-Path $projectRoot "release-build\test") "recording-ws-gate-wrapper-*.json"

$videoDecision = Read-StatusFromMarkdown $videoReport "Release decision \(profile-aware\):\s*\*\*([A-Z\-]+)\*\*" "NOT_RUN"
$securityDecision = Read-StatusFromMarkdown $securityReport "Decision:\s*\*\*([A-Z\-]+)\*\*" "NOT_RUN"
$onvifDecision = "NOT_RUN"
$androidDecision = "NOT_RUN"
$desktopDecision = "NOT_RUN"
$w4Overall = "NOT_FOUND"
$w4Obj = $null
$recordingWsGateOverall = "NOT_FOUND"
$recordingWsGateObj = $null
$onvifObj = $null

if ($onvifJson) {
    $onvifObj = Get-Content -Path $onvifJson.FullName -Raw | ConvertFrom-Json
    $onvifDecision = [string]$onvifObj.decision.goNoGo
}
if ($androidJson) {
    $androidObj = Get-Content -Path $androidJson.FullName -Raw | ConvertFrom-Json
    $androidDecision = [string]$androidObj.statuses.overall
}
if ($desktopJson) {
    $desktopObj = Get-Content -Path $desktopJson.FullName -Raw | ConvertFrom-Json
    $desktopDecision = [string]$desktopObj.statuses.overall
}
if ($w4Json) {
    $w4Obj = Get-Content -Path $w4Json.FullName -Raw | ConvertFrom-Json
    $rawW4 = [string]$w4Obj.overall
    if ([string]::IsNullOrWhiteSpace($rawW4)) {
        $w4Overall = "UNKNOWN"
    } else {
        $w4Overall = $rawW4
    }
}
if ($recordingWsGateJson) {
    $recordingWsGateObj = Get-Content -Path $recordingWsGateJson.FullName -Raw | ConvertFrom-Json
    $rawWsGate = [string]$recordingWsGateObj.overall
    if ([string]::IsNullOrWhiteSpace($rawWsGate)) {
        $recordingWsGateOverall = "UNKNOWN"
    } else {
        $recordingWsGateOverall = $rawWsGate
    }
}

function Test-RecordingWsGateSoftState([string]$state) {
    return $state -eq "NOT_FOUND" -or $state -eq "UNKNOWN" -or $state -eq "NOT_RUN" -or $state -eq "PARTIAL"
}

$finalDecision = "GO"
if ($videoDecision -eq "NO-GO" -or $securityDecision -eq "NO-GO" -or $onvifDecision -eq "NO_GO") {
    $finalDecision = "NO_GO"
} elseif ($recordingWsGateOverall -eq "FAIL") {
    $finalDecision = "NO_GO"
} elseif ($videoDecision -eq "NOT_RUN" -or $securityDecision -eq "NOT_RUN") {
    $finalDecision = "CONDITIONAL"
} elseif ($DecisionProfile -eq "Strict") {
    if (
        $onvifDecision -eq "CONDITIONAL" -or
        $onvifDecision -eq "NOT_RUN" -or
        $androidDecision -eq "PARTIAL" -or
        $desktopDecision -eq "PARTIAL" -or
        (Test-RecordingWsGateSoftState $recordingWsGateOverall)
    ) {
        $finalDecision = "CONDITIONAL"
    }
} else {
    # MvpCi: treat optional hardware smoke as non-blocking; allow ONVIF CONDITIONAL when policy waives camera evidence.
    $onvifConditionalWaived = $false
    if ($null -ne $onvifObj -and $onvifDecision -eq "CONDITIONAL") {
        try {
            $requireOnvif = [bool]$onvifObj.policy.requireOnvifEvidence
        } catch {
            $requireOnvif = $true
        }
        if (-not $requireOnvif) { $onvifConditionalWaived = $true }
    }
    if (
        $onvifDecision -eq "NOT_RUN" -or
        ($onvifDecision -eq "CONDITIONAL" -and -not $onvifConditionalWaived) -or
        ($recordingWsGateOverall -eq "NOT_FOUND")
    ) {
        $finalDecision = "CONDITIONAL"
    }
}

$dateTag = Get-Date -Format "yyyy-MM-dd"
$summaryPath = Join-Path $resolvedOutputDir ("PHASE1_GO_NO_GO_SUMMARY_{0}.md" -f $dateTag)
$jsonPath = Join-Path $resolvedOutputDir ("PHASE1_GO_NO_GO_SUMMARY_{0}.json" -f $dateTag)

$lines = @(
    "# Phase 1 Go/No-Go Summary",
    "",
    "- Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
    "- Decision profile: **$DecisionProfile**",
    "- Final decision: **$finalDecision**",
    "",
    "| Gate | Status | Evidence |",
    "|---|---|---|",
    "| Video profile-aware release | $videoDecision | $videoReport |",
    "| Security MVP readiness | $securityDecision | $securityReport |",
    "| ONVIF acceptance | $onvifDecision | $(if ($onvifJson) { $onvifJson.FullName } else { 'NOT_FOUND' }) |",
    "| Android video/background smoke | $androidDecision | $(if ($androidJson) { $androidJson.FullName } else { 'NOT_FOUND' }) |",
    "| Desktop video/event smoke | $desktopDecision | $(if ($desktopJson) { $desktopJson.FullName } else { 'NOT_FOUND' }) |",
    "| Recording WS gate wrapper | $recordingWsGateOverall | $(if ($recordingWsGateJson) { $recordingWsGateJson.FullName } else { 'NOT_FOUND' }) |",
    "| W4 platform gate (context) | $w4Overall | $(if ($w4Json) { $w4Json.FullName } else { 'NOT_FOUND' }) |"
)
$lines -join [Environment]::NewLine | Set-Content -Path $summaryPath -Encoding UTF8

$json = [ordered]@{
    generatedAtUtc = [DateTime]::UtcNow.ToString("o")
    decisionProfile = $DecisionProfile
    finalDecision = $finalDecision
    gates = @{
        videoProfileAware = @{
            status = $videoDecision
            evidence = $videoReport
        }
        securityMvp = @{
            status = $securityDecision
            evidence = $securityReport
        }
        onvif = @{
            status = $onvifDecision
            evidence = if ($onvifJson) { $onvifJson.FullName } else { $null }
        }
        androidSmoke = @{
            status = $androidDecision
            evidence = if ($androidJson) { $androidJson.FullName } else { $null }
        }
        desktopSmoke = @{
            status = $desktopDecision
            evidence = if ($desktopJson) { $desktopJson.FullName } else { $null }
        }
        recordingWsGate = @{
            status = $recordingWsGateOverall
            evidence = if ($recordingWsGateJson) { $recordingWsGateJson.FullName } else { $null }
            wsLifecycleStatus = if ($recordingWsGateObj) { $recordingWsGateObj.wsLifecycle.status } else { $null }
            wsLifecycleExitCode = if ($recordingWsGateObj) { $recordingWsGateObj.wsLifecycle.exitCode } else { $null }
            videoGateStatus = if ($recordingWsGateObj) { $recordingWsGateObj.videoGate.status } else { $null }
            videoGateExitCode = if ($recordingWsGateObj) { $recordingWsGateObj.videoGate.exitCode } else { $null }
        }
        w4PlatformGate = @{
            status = $w4Overall
            evidence = if ($w4Json) { $w4Json.FullName } else { $null }
            runProfile = if ($w4Obj) { $w4Obj.runProfile } else { $null }
            full = if ($w4Obj) { $w4Obj.full } else { $null }
            platformSmokeCompileOnlyGate = if ($w4Obj) { $w4Obj.platformSmokeCompileOnlyGate } else { $null }
            androidLocalFrameAnalytics = if ($w4Obj) { $w4Obj.androidLocalFrameAnalytics } else { $null }
        }
    }
}
$json | ConvertTo-Json -Depth 8 | Set-Content -Path $jsonPath -Encoding UTF8

Write-Host ("Summary report: {0}" -f $summaryPath) -ForegroundColor Green
Write-Host ("Summary json: {0}" -f $jsonPath) -ForegroundColor Green
Write-Host ("Final decision: {0}" -f $finalDecision) -ForegroundColor Cyan

if ($finalDecision -eq "NO_GO") { exit 2 }
if ($finalDecision -eq "CONDITIONAL") { exit 3 }
exit 0
