param(
    [string]$CanonicalPath = "docs/status/PROJECT_STATUS_PHASES.md",
    [string]$OutputStatusPath = "docs/status/NETWORK_LAYER_1_4_STATUS_SYNC.md",
    [string]$ReportDir = "diagnostics/network-layer-status-sync",
    [switch]$FailOnLegacyConflicts,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Synchronize and audit 1.4 network-layer status."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\sync-network-layer-1-4-status.ps1 -ShowHelp"
    Write-Host "  .\scripts\sync-network-layer-1-4-status.ps1 [-CanonicalPath <path>] [-OutputStatusPath <path>] [-ReportDir <dir>] [-FailOnLegacyConflicts]"
    Write-Host ""
    Write-Host "Behavior:"
    Write-Host "  - Uses 1.4 table in PROJECT_STATUS_PHASES.md as canonical source"
    Write-Host "  - Generates synced status file in docs/status"
    Write-Host "  - Scans known legacy docs for outdated conflict signals"
    Write-Host "  - Writes machine-readable json report in diagnostics"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0 - sync completed (or only warnings)"
    Write-Host "  2 - legacy conflicts detected with -FailOnLegacyConflicts"
    exit 0
}

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$runId = Get-Date -Format "yyyyMMdd-HHmmss"

function Resolve-ProjectPath([string]$pathValue) {
    if ([System.IO.Path]::IsPathRooted($pathValue)) {
        return $pathValue
    }
    return Join-Path $projectRoot $pathValue
}

function Add-ConflictSignalIfCurrent {
    param(
        [System.Collections.Generic.List[string]]$signals,
        [System.Collections.Generic.List[string]]$ignoredSignals,
        [string[]]$lines,
        [string]$pattern,
        [string]$message,
        [string]$ignoreReason
    )
    for ($i = 0; $i -lt $lines.Length; $i++) {
        $line = $lines[$i]
        if ($line -notmatch $pattern) {
            continue
        }

        # Ignore historical notes explicitly marked as audit snapshots.
        $windowStart = [Math]::Max(0, $i - 2)
        $windowEnd = [Math]::Min($lines.Length - 1, $i + 2)
        $window = ($lines[$windowStart..$windowEnd] -join " ")
        if ($window -match 'историческ|первичн(ого|ый)\s+аудит|было\s+оценено|было\s+отмечено') {
            $ignoredSignals.Add("$message (ignored: $ignoreReason)")
            continue
        }

        $signals.Add($message)
        return
    }
}

$canonicalAbs = Resolve-ProjectPath $CanonicalPath
$outputStatusAbs = Resolve-ProjectPath $OutputStatusPath
$reportRootAbs = Resolve-ProjectPath $ReportDir
$reportRunDir = Join-Path $reportRootAbs $runId
New-Item -ItemType Directory -Path $reportRunDir -Force | Out-Null

if (-not (Test-Path $canonicalAbs)) {
    throw "Canonical status file not found: $canonicalAbs"
}

$canonicalLines = Get-Content -Path $canonicalAbs -Encoding UTF8
$tableRows = New-Object System.Collections.Generic.List[object]
foreach ($line in $canonicalLines) {
    $trimmed = $line.Trim()
    if ($trimmed -match '^\|\s*(1\.4\.[0-9]+)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|$') {
        $tableRows.Add([PSCustomObject]@{
            id = $Matches[1].Trim()
            task = $Matches[2].Trim()
            status = $Matches[3].Trim()
            note = $Matches[4].Trim()
        })
    }
}

if ($tableRows.Count -eq 0) {
    throw "Could not parse 1.4 table rows in canonical file: $canonicalAbs"
}

$legacyTargets = @(
    "core/network/NETWORK_LAYER_IMPLEMENTATION_PROGRESS.md",
    "docs/planning/NETWORK_LAYER_ANALYSIS_AND_PLAN.md"
)

$legacyScan = New-Object System.Collections.Generic.List[object]
$conflicts = New-Object System.Collections.Generic.List[object]
$ignoredSignalCount = 0
foreach ($legacyRel in $legacyTargets) {
    $legacyAbs = Resolve-ProjectPath $legacyRel
    if (-not (Test-Path $legacyAbs)) {
        $legacyScan.Add([PSCustomObject]@{
            path = $legacyRel
            exists = $false
            conflictSignals = @()
        })
        continue
    }

    $content = Get-Content -Path $legacyAbs -Raw -Encoding UTF8
    $lines = Get-Content -Path $legacyAbs -Encoding UTF8
    $signals = New-Object System.Collections.Generic.List[string]
    $ignoredSignals = New-Object System.Collections.Generic.List[string]

    Add-ConflictSignalIfCurrent -signals $signals -ignoredSignals $ignoredSignals -lines $lines -pattern 'ONVIF Event service \(0%\)' -message "Found 'ONVIF Event service (0%)'" -ignoreReason "historical audit context"
    Add-ConflictSignalIfCurrent -signals $signals -ignoredSignals $ignoredSignals -lines $lines -pattern 'ONVIF Analytics service \(0%\)' -message "Found 'ONVIF Analytics service (0%)'" -ignoreReason "historical audit context"
    Add-ConflictSignalIfCurrent -signals $signals -ignoredSignals $ignoredSignals -lines $lines -pattern 'ONVIF Imaging service \(0%\)' -message "Found 'ONVIF Imaging service (0%)'" -ignoreReason "historical audit context"
    if ($content -match 'Текущий статус:\s*~85%') {
        $signals.Add("Found stale overall status '~85%'")
    }

    $legacyScan.Add([PSCustomObject]@{
        path = $legacyRel
        exists = $true
        conflictSignals = $signals.ToArray()
        ignoredSignals = $ignoredSignals.ToArray()
    })
    $ignoredSignalCount += $ignoredSignals.Count

    if ($signals.Count -gt 0) {
        $conflicts.Add([PSCustomObject]@{
            path = $legacyRel
            conflictSignals = $signals.ToArray()
        })
    }
}

$canonicalSnapshot = @()
$canonicalSnapshot += "# 1.4 Network Layer Status Sync"
$canonicalSnapshot += ""
$canonicalSnapshot += "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$canonicalSnapshot += "Canonical source: $CanonicalPath"
$canonicalSnapshot += ""
$canonicalSnapshot += "## Canonical 1.4 Rows"
$canonicalSnapshot += ""
$canonicalSnapshot += "| ID | Task | Status | Note |"
$canonicalSnapshot += "|----|------|--------|------|"
foreach ($row in $tableRows) {
    $canonicalSnapshot += "| $($row.id) | $($row.task) | $($row.status) | $($row.note) |"
}
$canonicalSnapshot += ""
$canonicalSnapshot += "## Legacy Conflict Audit"
$canonicalSnapshot += ""
if ($conflicts.Count -eq 0) {
    $canonicalSnapshot += "- No legacy conflict signals detected."
} else {
    foreach ($item in $conflicts) {
        $canonicalSnapshot += ('- "{0}": {1}' -f $item.path, ($item.conflictSignals -join '; '))
    }
}
$canonicalSnapshot += ""
$canonicalSnapshot += "## Ignored Legacy Signals"
$canonicalSnapshot += ""
$canonicalSnapshot += "- ignoredSignalCount: $ignoredSignalCount"
$canonicalSnapshot += ""
$ignoredAny = $false
foreach ($item in $legacyScan) {
    if ($item.exists -and $item.ignoredSignals -and $item.ignoredSignals.Count -gt 0) {
        $ignoredAny = $true
        $canonicalSnapshot += ('- "{0}": {1}' -f $item.path, ($item.ignoredSignals -join '; '))
    }
}
if (-not $ignoredAny) {
    $canonicalSnapshot += "- none"
}
$canonicalSnapshot += ""
$canonicalSnapshot += "## Policy"
$canonicalSnapshot += ""
$canonicalSnapshot += "- Canonical source for 1.4 is `docs/status/PROJECT_STATUS_PHASES.md`."
$canonicalSnapshot += "- Legacy planning/progress docs are informational and can diverge."

$canonicalSnapshot | Set-Content -Path $outputStatusAbs -Encoding UTF8

$report = [PSCustomObject]@{
    runId = $runId
    generatedAt = (Get-Date).ToString("s")
    canonicalPath = $CanonicalPath
    outputStatusPath = $OutputStatusPath
    canonicalRows = $tableRows.ToArray()
    legacyScan = $legacyScan.ToArray()
    conflictsDetected = $conflicts.ToArray()
    conflictCount = $conflicts.Count
    ignoredSignalCount = $ignoredSignalCount
}

$reportJsonPath = Join-Path $reportRunDir "network-layer-1-4-status-sync-report.json"
$report | ConvertTo-Json -Depth 8 | Set-Content -Path $reportJsonPath -Encoding UTF8

Write-Host "Updated status snapshot: $outputStatusAbs" -ForegroundColor Green
Write-Host "Sync report JSON: $reportJsonPath" -ForegroundColor DarkCyan
if ($conflicts.Count -gt 0) {
    Write-Host "Legacy conflict signals detected: $($conflicts.Count)" -ForegroundColor Yellow
    if ($FailOnLegacyConflicts) {
        exit 2
    }
}

exit 0
