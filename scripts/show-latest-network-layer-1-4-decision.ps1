param(
    [string]$AutomationDir = "diagnostics/network-layer-automation",
    [string]$PrereqDir = "diagnostics/network-layer-ci-prerequisites",
    [string]$StatusSyncDir = "diagnostics/network-layer-status-sync",
    [string]$OutputDir = "diagnostics/network-layer-decision",
    [switch]$WriteReport,
    [switch]$AsJson,
    [switch]$FailOnNoGo,
    [switch]$FailOnConditional,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Show latest aggregated decision for network layer 1.4."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\show-latest-network-layer-1-4-decision.ps1"
    Write-Host "  .\scripts\show-latest-network-layer-1-4-decision.ps1 -AsJson"
    Write-Host "  .\scripts\show-latest-network-layer-1-4-decision.ps1 -WriteReport"
    Write-Host "  .\scripts\show-latest-network-layer-1-4-decision.ps1 -FailOnNoGo -FailOnConditional"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  OK (or no strict failure flags triggered)"
    Write-Host "  1  No diagnostics found to build decision"
    Write-Host "  2  With -FailOnNoGo and NO_GO decision"
    Write-Host "  3  With -FailOnConditional and CONDITIONAL decision"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$runId = Get-Date -Format "yyyyMMdd-HHmmss"

function Resolve-ProjectPath([string]$pathValue) {
    if ([System.IO.Path]::IsPathRooted($pathValue)) {
        return $pathValue
    }
    return Join-Path $projectRoot $pathValue
}

function Get-LatestFile([string]$rootPath, [string]$filter) {
    if (-not (Test-Path $rootPath)) { return $null }
    return Get-ChildItem -Path $rootPath -Filter $filter -File -Recurse -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
}

$automationRoot = Resolve-ProjectPath $AutomationDir
$prereqRoot = Resolve-ProjectPath $PrereqDir
$statusSyncRoot = Resolve-ProjectPath $StatusSyncDir

$automationFile = Get-LatestFile $automationRoot "network-layer-automation-summary.json"
$prereqFile = Get-LatestFile $prereqRoot "network-layer-ci-prerequisites.json"
$statusSyncFile = Get-LatestFile $statusSyncRoot "network-layer-1-4-status-sync-report.json"

if (-not $automationFile -and -not $prereqFile -and -not $statusSyncFile) {
    Write-Host "No network-layer diagnostics found." -ForegroundColor Yellow
    exit 1
}

$automation = $null
$prereq = $null
$statusSync = $null

if ($automationFile) {
    $automation = Get-Content $automationFile.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
}
if ($prereqFile) {
    $prereq = Get-Content $prereqFile.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
}
if ($statusSyncFile) {
    $statusSync = Get-Content $statusSyncFile.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
}

$signals = New-Object System.Collections.Generic.List[string]
$decision = "GO"
$statusSyncConflictCount = $null
$statusSyncIgnoredSignalCount = $null

if (-not $automation) {
    $signals.Add("Missing automation summary json")
    $decision = "CONDITIONAL"
} elseif ([string]$automation.overallStatus -ne "PASS") {
    $signals.Add("Automation overallStatus is $($automation.overallStatus)")
    $decision = "NO_GO"
}

if ($prereq) {
    $prereqStatus = [string]$prereq.status
    if ($prereqStatus -eq "FAIL") {
        $signals.Add("Prerequisites status is $($prereq.status)")
        if ($decision -ne "NO_GO") { $decision = "CONDITIONAL" }
    } elseif ($prereqStatus -eq "WARN") {
        $signals.Add("Prerequisites status is WARN (local-smoke)")
    }
} else {
    $signals.Add("Missing prerequisites json")
    if ($decision -ne "NO_GO") { $decision = "CONDITIONAL" }
}

if ($statusSync) {
    $conflictCount = [int]$statusSync.conflictCount
    $ignoredSignalCount = if ($null -ne $statusSync.ignoredSignalCount) { [int]$statusSync.ignoredSignalCount } else { 0 }
    $statusSyncConflictCount = $conflictCount
    $statusSyncIgnoredSignalCount = $ignoredSignalCount
    if ($ignoredSignalCount -gt 0) {
        $signals.Add("Status sync ignoredSignalCount=$ignoredSignalCount (historical)")
    }
    if ($conflictCount -gt 0) {
        $signals.Add("Status sync conflictCount=$conflictCount")
        if ($decision -eq "GO") { $decision = "CONDITIONAL" }
    }
} else {
    $signals.Add("Missing status-sync report json")
    if ($decision -ne "NO_GO") { $decision = "CONDITIONAL" }
}

$reason = if ($signals.Count -eq 0) { "All 1.4 automation signals are green." } else { ($signals -join "; ") }

$result = [PSCustomObject]@{
    runId = $runId
    generatedAt = (Get-Date).ToString("s")
    decision = $decision
    reason = $reason
    signals = $signals.ToArray()
    artifacts = [PSCustomObject]@{
        automationSummaryJson = if ($automationFile) { $automationFile.FullName } else { $null }
        prerequisitesJson = if ($prereqFile) { $prereqFile.FullName } else { $null }
        statusSyncJson = if ($statusSyncFile) { $statusSyncFile.FullName } else { $null }
    }
    metrics = [PSCustomObject]@{
        statusSyncConflictCount = $statusSyncConflictCount
        statusSyncIgnoredSignalCount = $statusSyncIgnoredSignalCount
    }
}

if ($WriteReport) {
    $resolvedOutputRoot = Resolve-ProjectPath $OutputDir
    $resolvedRunDir = Join-Path $resolvedOutputRoot $runId
    New-Item -ItemType Directory -Path $resolvedRunDir -Force | Out-Null

    $jsonPath = Join-Path $resolvedRunDir "network-layer-1-4-decision.json"
    $mdPath = Join-Path $resolvedRunDir "network-layer-1-4-decision.md"
    $result | ConvertTo-Json -Depth 6 | Set-Content -Path $jsonPath -Encoding UTF8

    $md = @()
    $md += "# Network Layer 1.4 Decision"
    $md += ""
    $md += "- runId: $runId"
    $md += "- generatedAt: $($result.generatedAt)"
    $md += "- decision: $($result.decision)"
    $md += "- reason: $($result.reason)"
    $md += ""
    $md += "## Signals"
    $md += ""
    if ($signals.Count -eq 0) {
        $md += "- none"
    } else {
        foreach ($s in $signals) { $md += "- $s" }
    }
    $md += ""
    $md += "## Artifacts"
    $md += ""
    $md += "- automationSummaryJson: $($result.artifacts.automationSummaryJson)"
    $md += "- prerequisitesJson: $($result.artifacts.prerequisitesJson)"
    $md += "- statusSyncJson: $($result.artifacts.statusSyncJson)"
    $md += ""
    $md += "## Metrics"
    $md += ""
    $md += "- statusSyncConflictCount: $($result.metrics.statusSyncConflictCount)"
    $md += "- statusSyncIgnoredSignalCount: $($result.metrics.statusSyncIgnoredSignalCount)"
    $md | Set-Content -Path $mdPath -Encoding UTF8

    $reportPaths = [PSCustomObject]@{
        json = $jsonPath
        md = $mdPath
    }
    $result | Add-Member -NotePropertyName reportPaths -NotePropertyValue $reportPaths
}

if ($AsJson) {
    $result | ConvertTo-Json -Depth 6
} else {
    $conflictsValue = if ($null -ne $result.metrics.statusSyncConflictCount) { $result.metrics.statusSyncConflictCount } else { "<n/a>" }
    $ignoredValue = if ($null -ne $result.metrics.statusSyncIgnoredSignalCount) { $result.metrics.statusSyncIgnoredSignalCount } else { "<n/a>" }
    $automationValue = if ($result.artifacts.automationSummaryJson) { $result.artifacts.automationSummaryJson } else { "<missing>" }
    $prerequisitesValue = if ($result.artifacts.prerequisitesJson) { $result.artifacts.prerequisitesJson } else { "<missing>" }
    $statusSyncValue = if ($result.artifacts.statusSyncJson) { $result.artifacts.statusSyncJson } else { "<missing>" }

    Write-Host "Latest network layer 1.4 decision" -ForegroundColor Cyan
    Write-Host ("Decision:     {0}" -f $result.decision)
    Write-Host ("Reason:       {0}" -f $result.reason)
    Write-Host ("Conflicts:    {0}" -f $conflictsValue)
    Write-Host ("Ignored:      {0}" -f $ignoredValue)
    Write-Host ("Automation:   {0}" -f $automationValue)
    Write-Host ("Prerequisites:{0}" -f $prerequisitesValue)
    Write-Host ("Status-sync:  {0}" -f $statusSyncValue)
    if ($WriteReport) {
        Write-Host ("Report json:  {0}" -f $result.reportPaths.json)
        Write-Host ("Report md:    {0}" -f $result.reportPaths.md)
    }
}

if ($result.decision -eq "NO_GO" -and $FailOnNoGo.IsPresent) {
    exit 2
}
if ($result.decision -eq "CONDITIONAL" -and $FailOnConditional.IsPresent) {
    exit 3
}

exit 0
