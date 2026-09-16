[CmdletBinding()]
param(
    [string]$ReportsDir = "docs/reports",
    [string]$OutputDir = "release-build/test/server-docker-gate",
    [switch]$IncludeTimestampSubdir,
    [int]$KeepExportReports = 20,
    [int]$KeepExportRuns = 20
)

$ErrorActionPreference = "Stop"

function Resolve-ProjectPath {
    param([Parameter(Mandatory = $true)][string]$PathValue)
    $projectRoot = Split-Path -Parent $PSScriptRoot
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

function Get-LatestReportByPattern {
    param(
        [Parameter(Mandatory = $true)][string]$Dir,
        [Parameter(Mandatory = $true)][string]$Pattern
    )
    return Get-ChildItem -Path $Dir -Filter $Pattern -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

$reportsDirAbs = Resolve-ProjectPath -PathValue $ReportsDir
$outputDirAbs = Resolve-ProjectPath -PathValue $OutputDir
$baseOutputDirAbs = $outputDirAbs
if ($IncludeTimestampSubdir) {
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $outputDirAbs = Join-Path $outputDirAbs $stamp
}
if ($KeepExportReports -lt 1) { $KeepExportReports = 1 }
if ($KeepExportRuns -lt 1) { $KeepExportRuns = 1 }

if (-not (Test-Path -LiteralPath $reportsDirAbs)) {
    throw "Reports directory not found: $reportsDirAbs"
}

New-Item -ItemType Directory -Path $outputDirAbs -Force | Out-Null

$copyCandidates = @()

$gateLatestMd = Get-LatestReportByPattern -Dir $reportsDirAbs -Pattern "DOCKER_PRE_RELEASE_GATE_REPORT_*.md"
$gateLatestJson = Get-LatestReportByPattern -Dir $reportsDirAbs -Pattern "DOCKER_PRE_RELEASE_GATE_REPORT_*.json"
$lastMd = Join-Path $reportsDirAbs "DOCKER_PRE_RELEASE_GATE_LAST.md"
$lastJson = Join-Path $reportsDirAbs "DOCKER_PRE_RELEASE_GATE_LAST.json"
$closureDoc = Join-Path $reportsDirAbs "DOCKER_SERVER_TASK_CLOSURE_2026-04-27.md"
$runbookDoc = Join-Path $reportsDirAbs "DOCKER_WEB_MANUAL_RUNBOOK_2026-04-27.md"

if ($null -ne $gateLatestMd) { $copyCandidates += $gateLatestMd.FullName }
if ($null -ne $gateLatestJson) { $copyCandidates += $gateLatestJson.FullName }
if (Test-Path -LiteralPath $lastMd) { $copyCandidates += $lastMd }
if (Test-Path -LiteralPath $lastJson) { $copyCandidates += $lastJson }
if (Test-Path -LiteralPath $closureDoc) { $copyCandidates += $closureDoc }
if (Test-Path -LiteralPath $runbookDoc) { $copyCandidates += $runbookDoc }

if ($copyCandidates.Count -eq 0) {
    throw "No Docker gate artifacts found to export in: $reportsDirAbs"
}

$copied = @()
foreach ($src in $copyCandidates | Select-Object -Unique) {
    $dest = Join-Path $outputDirAbs ([System.IO.Path]::GetFileName($src))
    Copy-Item -LiteralPath $src -Destination $dest -Force
    $copied += $dest
}

$manifestPath = Join-Path $outputDirAbs "DOCKER_GATE_EXPORT_MANIFEST.md"
$generatedAt = Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz"
$manifestLines = @(
    "# Docker Gate Export Manifest",
    "",
    "- Generated at: $generatedAt",
    "- Source reports dir: $reportsDirAbs",
    "- Export dir: $outputDirAbs",
    "",
    "## Files",
    ""
)

foreach ($file in $copied) {
    $manifestLines += "- $([System.IO.Path]::GetFileName($file))"
}

$manifestLines -join [Environment]::NewLine | Set-Content -Path $manifestPath -Encoding UTF8

# Stable machine-readable status pointer for CI dashboards.
$latestGateMd = Get-LatestReportByPattern -Dir $outputDirAbs -Pattern "DOCKER_PRE_RELEASE_GATE_REPORT_*.md"
$latestGateJson = Get-LatestReportByPattern -Dir $outputDirAbs -Pattern "DOCKER_PRE_RELEASE_GATE_REPORT_*.json"
$lastSummaryMdInExport = Join-Path $outputDirAbs "DOCKER_PRE_RELEASE_GATE_LAST.md"
$lastSummaryJsonInExport = Join-Path $outputDirAbs "DOCKER_PRE_RELEASE_GATE_LAST.json"

$statusPayload = [pscustomobject]@{
    generatedAt = $generatedAt
    exportDir = $outputDirAbs
    manifest = [System.IO.Path]::GetFileName($manifestPath)
    latestGateReportMarkdown = if ($null -ne $latestGateMd) { $latestGateMd.Name } else { "" }
    latestGateReportJson = if ($null -ne $latestGateJson) { $latestGateJson.Name } else { "" }
    lastSummaryMarkdown = if (Test-Path -LiteralPath $lastSummaryMdInExport) { [System.IO.Path]::GetFileName($lastSummaryMdInExport) } else { "" }
    lastSummaryJson = if (Test-Path -LiteralPath $lastSummaryJsonInExport) { [System.IO.Path]::GetFileName($lastSummaryJsonInExport) } else { "" }
    files = $copied | ForEach-Object { [System.IO.Path]::GetFileName($_) }
}

$statusJsonPath = Join-Path $outputDirAbs "server-docker-gate-status.json"
$statusPayload | ConvertTo-Json -Depth 8 | Set-Content -Path $statusJsonPath -Encoding UTF8

$statusMdPath = Join-Path $outputDirAbs "server-docker-gate-status.md"
$statusLines = @(
    "# Server Docker Gate Status",
    "",
    "- Generated at: $generatedAt",
    "- Export dir: $outputDirAbs",
    "- Latest gate report (md): $($statusPayload.latestGateReportMarkdown)",
    "- Latest gate report (json): $($statusPayload.latestGateReportJson)",
    "- Last summary (md): $($statusPayload.lastSummaryMarkdown)",
    "- Last summary (json): $($statusPayload.lastSummaryJson)",
    "- Manifest: $([System.IO.Path]::GetFileName($manifestPath))",
    "",
    "Use `server-docker-gate-status.json` as the stable CI input."
)
$statusLines -join [Environment]::NewLine | Set-Content -Path $statusMdPath -Encoding UTF8

# Rotation for flat export mode: keep only newest N gate reports.
if (-not $IncludeTimestampSubdir) {
    $oldMd = Get-ChildItem -Path $outputDirAbs -Filter "DOCKER_PRE_RELEASE_GATE_REPORT_*.md" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending
    if ($oldMd.Count -gt $KeepExportReports) {
        $oldMd | Select-Object -Skip $KeepExportReports | Remove-Item -Force
    }

    $oldJson = Get-ChildItem -Path $outputDirAbs -Filter "DOCKER_PRE_RELEASE_GATE_REPORT_*.json" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending
    if ($oldJson.Count -gt $KeepExportReports) {
        $oldJson | Select-Object -Skip $KeepExportReports | Remove-Item -Force
    }
}

# Rotation for timestamped export mode: keep only newest N run directories.
if ($IncludeTimestampSubdir -and (Test-Path -LiteralPath $baseOutputDirAbs)) {
    $runDirs = Get-ChildItem -Path $baseOutputDirAbs -Directory -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending
    if ($runDirs.Count -gt $KeepExportRuns) {
        $runDirs | Select-Object -Skip $KeepExportRuns | Remove-Item -Recurse -Force
    }
}

Write-Host "Artifacts exported to: $outputDirAbs" -ForegroundColor Green
Write-Host "Manifest: $manifestPath" -ForegroundColor Cyan
Write-Host "Status JSON: $statusJsonPath" -ForegroundColor Cyan
exit 0
