param(
    [switch]$ShowHelp,
    [string]$DiagnosticsDir = "diagnostics\onvif-events",
    [switch]$AsJson = $false,
    [switch]$FailOnNoGo,
    [switch]$FailOnConditional
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Show latest Phase 1 ONVIF gate decision (newest phase1-onvif-acceptance-*.json)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\show-latest-phase1-onvif-decision.ps1"
    Write-Host "  .\scripts\show-latest-phase1-onvif-decision.ps1 -AsJson"
    Write-Host "  .\scripts\show-latest-phase1-onvif-decision.ps1 -FailOnNoGo -FailOnConditional"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -DiagnosticsDir  Root-relative folder (default diagnostics\onvif-events)."
    Write-Host "  -AsJson  Extended JSON to stdout."
    Write-Host "  -FailOnNoGo  Exit 2 when decision is NO_GO."
    Write-Host "  -FailOnConditional  Exit 3 when decision is CONDITIONAL."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  OK (or policy flags not triggered)"
    Write-Host "  1  Diagnostics dir missing or no acceptance json found"
    Write-Host "  2  With -FailOnNoGo"
    Write-Host "  3  With -FailOnConditional"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$diagRoot = Join-Path $projectRoot $DiagnosticsDir

if (-not (Test-Path $diagRoot)) {
    Write-Host "Diagnostics directory not found: $diagRoot" -ForegroundColor Yellow
    exit 1
}

$latest = Get-ChildItem -Path $diagRoot -Filter "phase1-onvif-acceptance-*.json" -File |
    Sort-Object LastWriteTimeUtc -Descending |
    Select-Object -First 1

if (-not $latest) {
    Write-Host "No phase1 ONVIF acceptance summary json found in: $diagRoot" -ForegroundColor Yellow
    exit 1
}

$data = Get-Content $latest.FullName -Raw | ConvertFrom-Json
if ($null -eq $data.statuses -or $null -eq $data.decision -or [string]::IsNullOrWhiteSpace([string]$data.decision.goNoGo)) {
    Write-Host "Acceptance json is missing statuses and/or decision.goNoGo: $($latest.FullName)" -ForegroundColor Yellow
    exit 1
}
$phase1Ws = "NOT_RUN"
$phase1Latency = "NOT_MEASURED"
$phase2Ws = "NOT_RUN"
$phase2Latency = "NOT_MEASURED"
$phase1Path = $null
$phase2Path = $null
if ($data.diagnostics) {
    if ($data.diagnostics.websocket.phase1) { $phase1Ws = [string]$data.diagnostics.websocket.phase1 }
    if ($data.diagnostics.websocket.phase2) { $phase2Ws = [string]$data.diagnostics.websocket.phase2 }
    if ($data.diagnostics.latency.phase1) { $phase1Latency = [string]$data.diagnostics.latency.phase1 }
    if ($data.diagnostics.latency.phase2) { $phase2Latency = [string]$data.diagnostics.latency.phase2 }
    if ($data.diagnostics.onvifPhaseJson.phase1) { $phase1Path = [string]$data.diagnostics.onvifPhaseJson.phase1 }
    if ($data.diagnostics.onvifPhaseJson.phase2) { $phase2Path = [string]$data.diagnostics.onvifPhaseJson.phase2 }
}

if (-not $phase1Path) {
    $latestOnvifPhase1 = Get-ChildItem -Path $diagRoot -Filter "*phase1.json" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
    if ($latestOnvifPhase1) { $phase1Path = $latestOnvifPhase1.FullName }
}
if (-not $phase2Path) {
    $latestOnvifPhase2 = Get-ChildItem -Path $diagRoot -Filter "*phase2.json" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
    if ($latestOnvifPhase2) { $phase2Path = $latestOnvifPhase2.FullName }
}

if ($phase1Path -and (Test-Path $phase1Path)) {
    try {
        $p1 = Get-Content $phase1Path -Raw | ConvertFrom-Json
        if ($p1.websocketStatus) { $phase1Ws = [string]$p1.websocketStatus }
        if ($p1.latencyStatus) { $phase1Latency = [string]$p1.latencyStatus }
    } catch {}
}
if ($phase2Path -and (Test-Path $phase2Path)) {
    try {
        $p2 = Get-Content $phase2Path -Raw | ConvertFrom-Json
        if ($p2.websocketStatus) { $phase2Ws = [string]$p2.websocketStatus }
        if ($p2.latencyStatus) { $phase2Latency = [string]$p2.latencyStatus }
    } catch {}
}

if ($AsJson) {
    $extended = [ordered]@{
        runId = $data.runId
        timestampUtc = $data.timestampUtc
        apiBase = $data.apiBase
        statuses = $data.statuses
        decision = $data.decision
        policy = $data.policy
        artifacts = $data.artifacts
        notes = $data.notes
        preflight = $data.preflight
        diagnostics = @{
            websocket = @{
                phase1 = $phase1Ws
                phase2 = $phase2Ws
            }
            latency = @{
                phase1 = $phase1Latency
                phase2 = $phase2Latency
            }
            onvifPhaseJson = @{
                phase1 = $phase1Path
                phase2 = $phase2Path
            }
            sourceJson = $(if ($data.artifacts.acceptanceJson) { [string]$data.artifacts.acceptanceJson } else { $latest.FullName })
        }
    }
    $extended | ConvertTo-Json -Depth 10
} else {
    Write-Host "Latest phase1 ONVIF decision" -ForegroundColor Cyan
    Write-Host ("Run ID:            {0}" -f $data.runId)
    Write-Host ("Timestamp (UTC):   {0}" -f $data.timestampUtc)
    Write-Host ("API base:          {0}" -f $data.apiBase)
    Write-Host ("Overall status:    {0}" -f $data.statuses.overall)
    Write-Host ("1.4.3 ONVIF:       {0}" -f $data.statuses.onvif143)
    Write-Host ("1.4.1 unit:        {0}" -f $data.statuses.unit141)
    Write-Host ("UI regression:     {0}" -f $data.statuses.uiRegression)
    Write-Host ("Gate decision:     {0}" -f $data.decision.goNoGo)
    Write-Host ("Decision reason:   {0}" -f $data.decision.reason)
    Write-Host ("Require ONVIF ev.: {0}" -f $(if ($data.policy) { $data.policy.requireOnvifEvidence } else { $false }))
    Write-Host ("WS status p1/p2:   {0}/{1}" -f $phase1Ws, $phase2Ws)
    Write-Host ("Latency p1/p2:     {0}/{1}" -f $phase1Latency, $phase2Latency)
    Write-Host ("Evidence report:   {0}" -f $data.artifacts.evidenceReport)
    Write-Host ("ONVIF summary:     {0}" -f $data.artifacts.latestOnvifSummary)
    Write-Host ("ONVIF p1 json:     {0}" -f $(if ($phase1Path) { $phase1Path } else { "<not found>" }))
    Write-Host ("ONVIF p2 json:     {0}" -f $(if ($phase2Path) { $phase2Path } else { "<not found>" }))
    Write-Host ("Source json:       {0}" -f $latest.FullName)
}

if ($data.decision.goNoGo -eq "NO_GO" -and $FailOnNoGo.IsPresent) {
    Write-Host "Exit 2: NO_GO decision." -ForegroundColor Red
    exit 2
}
if ($data.decision.goNoGo -eq "CONDITIONAL" -and $FailOnConditional.IsPresent) {
    Write-Host "Exit 3: CONDITIONAL decision." -ForegroundColor Yellow
    exit 3
}
