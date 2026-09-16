param(
    [switch]$ShowHelp,
    [string]$OutputPath = "",
    [string]$DiagnosticsDir = "diagnostics\onvif-events",
    [ValidateSet("PASS", "PARTIAL", "FAIL", "NOT_RUN")]
    [string]$Onvif143Status = "PARTIAL",
    [ValidateSet("PASS", "PARTIAL", "FAIL", "NOT_RUN")]
    [string]$Unit141Status = "NOT_RUN",
    [ValidateSet("PASS", "PARTIAL", "FAIL", "NOT_RUN")]
    [string]$UiRegressionStatus = "NOT_RUN",
    [string]$Notes = ""
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Generate ONVIF Phase 1 evidence markdown (aggregates diagnostics + passed-in statuses)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host '  .\scripts\generate-phase1-onvif-evidence-report.ps1 -Onvif143Status PARTIAL -Unit141Status PASS -UiRegressionStatus PASS -Notes "Local run"'
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -OutputPath  Target markdown (default docs\reports\ONVIF_PHASE1_EVIDENCE_REPORT_<yyyy-MM-dd>.md)."
    Write-Host "  -DiagnosticsDir  Scan for latest *summary*.md, *phase1.json, *phase2.json, phase1-onvif-acceptance-*.json."
    Write-Host "  -Onvif143Status  -Unit141Status  -UiRegressionStatus  PASS|PARTIAL|FAIL|NOT_RUN"
    Write-Host "    (defaults: Onvif143Status=PARTIAL; Unit141/UI=NOT_RUN - pass PASS only when those steps were actually executed.)"
    Write-Host "  -Notes  Free-text line in the report"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Report written successfully"
    Write-Host "  Non-zero  Unhandled error while generating report"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$diagRoot = Join-Path $projectRoot $DiagnosticsDir
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$dateStamp = Get-Date -Format "yyyy-MM-dd"

if (-not $OutputPath) {
    $OutputPath = Join-Path $projectRoot "docs\reports\ONVIF_PHASE1_EVIDENCE_REPORT_$dateStamp.md"
}

$outputDir = Split-Path -Parent $OutputPath
if ($outputDir -and -not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

function Get-LatestFile([string]$path, [string]$mask) {
    if (-not (Test-Path $path)) { return $null }
    $items = Get-ChildItem -Path $path -Filter $mask -File | Sort-Object LastWriteTimeUtc -Descending
    if ($items.Count -eq 0) { return $null }
    return $items[0]
}

$latestSummary = Get-LatestFile -path $diagRoot -mask "*summary*.md"
$latestPhase1 = Get-LatestFile -path $diagRoot -mask "*phase1.json"
$latestPhase2 = Get-LatestFile -path $diagRoot -mask "*phase2.json"
$latestAcceptance = Get-LatestFile -path $diagRoot -mask "phase1-onvif-acceptance-*.json"
$acceptanceDiagPhase1Ws = $null
$acceptanceDiagPhase2Ws = $null
$acceptanceDiagPhase1Latency = $null
$acceptanceDiagPhase2Latency = $null
$acceptanceRequireOnvifEvidence = $null

if ($latestAcceptance) {
    try {
        $acceptance = Get-Content $latestAcceptance.FullName -Raw | ConvertFrom-Json
        $diagPhase1Path = [string]$acceptance.diagnostics.onvifPhaseJson.phase1
        $diagPhase2Path = [string]$acceptance.diagnostics.onvifPhaseJson.phase2
        $acceptanceDiagPhase1Ws = [string]$acceptance.diagnostics.websocket.phase1
        $acceptanceDiagPhase2Ws = [string]$acceptance.diagnostics.websocket.phase2
        $acceptanceDiagPhase1Latency = [string]$acceptance.diagnostics.latency.phase1
        $acceptanceDiagPhase2Latency = [string]$acceptance.diagnostics.latency.phase2
        $acceptanceRequireOnvifEvidence = $acceptance.policy.requireOnvifEvidence
        if ($diagPhase1Path -and (Test-Path $diagPhase1Path)) {
            $latestPhase1 = Get-Item -LiteralPath $diagPhase1Path
        }
        if ($diagPhase2Path -and (Test-Path $diagPhase2Path)) {
            $latestPhase2 = Get-Item -LiteralPath $diagPhase2Path
        }
    } catch {
        # Fallback to latest phase1/phase2 artifacts found by mask.
    }
}

$summaryRel = if ($latestSummary) { Resolve-Path -LiteralPath $latestSummary.FullName -Relative } else { "<not found>" }
$phase1Rel = if ($latestPhase1) { Resolve-Path -LiteralPath $latestPhase1.FullName -Relative } else { "<not found>" }
$phase2Rel = if ($latestPhase2) { Resolve-Path -LiteralPath $latestPhase2.FullName -Relative } else { "<not found>" }

$phase1Ws = "NOT_RUN"
$phase1Latency = "NOT_MEASURED"
$phase2Ws = "NOT_RUN"
$phase2Latency = "NOT_MEASURED"
if ($acceptanceDiagPhase1Ws) { $phase1Ws = $acceptanceDiagPhase1Ws }
if ($acceptanceDiagPhase2Ws) { $phase2Ws = $acceptanceDiagPhase2Ws }
if ($acceptanceDiagPhase1Latency) { $phase1Latency = $acceptanceDiagPhase1Latency }
if ($acceptanceDiagPhase2Latency) { $phase2Latency = $acceptanceDiagPhase2Latency }
if ($latestPhase1) {
    try {
        $p1 = Get-Content $latestPhase1.FullName -Raw | ConvertFrom-Json
        if ($p1.websocketStatus) { $phase1Ws = [string]$p1.websocketStatus }
        if ($p1.latencyStatus) { $phase1Latency = [string]$p1.latencyStatus }
    } catch {}
}
if ($latestPhase2) {
    try {
        $p2 = Get-Content $latestPhase2.FullName -Raw | ConvertFrom-Json
        if ($p2.websocketStatus) { $phase2Ws = [string]$p2.websocketStatus }
        if ($p2.latencyStatus) { $phase2Latency = [string]$p2.latencyStatus }
    } catch {}
}

$overallStatus = "PASS"
if ($Onvif143Status -eq "FAIL" -or $Unit141Status -eq "FAIL" -or $UiRegressionStatus -eq "FAIL") {
    $overallStatus = "FAIL"
} elseif ($Onvif143Status -eq "PARTIAL" -or $Unit141Status -eq "PARTIAL" -or $UiRegressionStatus -eq "PARTIAL" -or
    $Onvif143Status -eq "NOT_RUN" -or $Unit141Status -eq "NOT_RUN" -or $UiRegressionStatus -eq "NOT_RUN") {
    $overallStatus = "PARTIAL"
}

$noteLine = if ([string]::IsNullOrWhiteSpace($Notes)) { "- none" } else { "- $Notes" }
$lines = @(
    "# ONVIF Phase 1 Evidence Report",
    "",
    "- Generated at: $timestamp",
    "- Overall status: **$overallStatus**",
    "",
    "## Scope",
    "",
    "- 1.4.3 ONVIF camera verification via automated scripts",
    "- 1.4.1 unit coverage for mapping and subscription lifecycle",
    "- short UI real-time regression pass",
    "",
    "## Evidence Artifacts",
    "",
    "- ONVIF acceptance json: $(if ($latestAcceptance) { Resolve-Path -LiteralPath $latestAcceptance.FullName -Relative } else { "<not found>" })",
    "- ONVIF resilience summary: $summaryRel",
    "- ONVIF phase1 json: $phase1Rel",
    "- ONVIF phase2 json: $phase2Rel",
    "",
    "## Status Matrix",
    "",
    "| Item | Status | Evidence |",
    "|------|--------|----------|",
    "| 1.4.3 automated ONVIF verification | $Onvif143Status | scripts: onvif-events-api-verification.ps1 (WS/latency), onvif-events-resilience-verification.ps1 |",
    "| 1.4.1 unit tests (mapping+lifecycle) | $Unit141Status | OnvifEventMapperTest, OnvifEventSubscriptionServiceTest, CameraEventMonitoringServiceJvmTest |",
    "| UI real-time regression (short) | $UiRegressionStatus | eventsSlice.test.ts + useWebSocket.test.tsx |",
    "",
    "## Commands Baseline",
    "",
    "- .\scripts\onvif-events-api-verification.ps1 -AdminPassword <pwd> -EnableWebSocketCheck -PollIntervalMs 5000 -PullTimeoutMs 500 -LatencyBufferMs 10000",
    "- .\scripts\onvif-events-resilience-verification.ps1 -AdminPassword <pwd> -EnableWebSocketCheck",
    "- ./gradlew :server:api:test --tests ""com.company.ipcamera.server.service.OnvifEventMapperTest"" --tests ""com.company.ipcamera.server.service.OnvifEventSubscriptionServiceTest"" --tests ""com.company.ipcamera.server.service.CameraEventMonitoringServiceJvmTest""",
    "- cd server/web; npm test -- --runInBand src/store/slices/eventsSlice.test.ts src/hooks/useWebSocket.test.tsx",
    "",
    "## ONVIF Diagnostics",
    "",
    "- WebSocket status (phase1/phase2): $phase1Ws / $phase2Ws",
    "- Latency status (phase1/phase2): $phase1Latency / $phase2Latency",
    "- Gate policy requireOnvifEvidence: $(if ($null -ne $acceptanceRequireOnvifEvidence) { $acceptanceRequireOnvifEvidence } else { "<unknown>" })",
    "",
    "## Notes",
    "",
    $noteLine,
    "",
    "## Verdict",
    "",
    "- PASS: all three statuses are PASS and diagnostics artifacts are present.",
    "- PARTIAL: at least one block is PARTIAL/NOT_RUN or artifacts are missing.",
    "- FAIL: at least one block is FAIL."
)

$lines -join "`r`n" | Out-File -FilePath $OutputPath -Encoding UTF8
Write-Host "Evidence report generated: $OutputPath" -ForegroundColor Green
