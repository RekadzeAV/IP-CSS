param(
  [int]$Keep = 30,
  [switch]$ApplyDeletion,
  [switch]$ShowHelp
)

if ($ShowHelp) {
  Write-Host "Cleanup old web interface automation artifacts."
  Write-Host ""
  Write-Host "Usage:"
  Write-Host "  .\scripts\cleanup-web-interface-automation-artifacts.ps1 -ShowHelp"
  Write-Host "  .\scripts\cleanup-web-interface-automation-artifacts.ps1"
  Write-Host "  .\scripts\cleanup-web-interface-automation-artifacts.ps1 -Keep 50"
  Write-Host "  .\scripts\cleanup-web-interface-automation-artifacts.ps1 -Keep 20 -ApplyDeletion"
  Write-Host ""
  Write-Host "Behavior:"
  Write-Host "  - Keeps newest N automation run/report/summary test artifacts."
  Write-Host "  - Dry-run by default (no deletion)."
  Write-Host "  - Uses -ApplyDeletion to actually delete old files."
  exit 0
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($Keep -lt 1) {
  throw "Keep must be >= 1"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$statusDir = Join-Path $repoRoot "docs/status"
if (-not (Test-Path -Path $statusDir)) {
  throw "Status directory not found: $statusDir"
}

$patterns = @(
  "WEB_INTERFACE_AUTOMATION_RUN_TEST*.md",
  "WEB_INTERFACE_AUTOMATION_SUMMARY_TEST*.json",
  "WEB_INTERFACE_AUTOMATION_STATUS_TEST*.md",
  "WEB_INTERFACE_AUTOMATION_HISTORY_TEST*.md"
)

$allCandidates = @()
foreach ($pattern in $patterns) {
  $items = Get-ChildItem -Path $statusDir -Filter $pattern -File -ErrorAction SilentlyContinue
  if ($null -ne $items) {
    $allCandidates += $items
  }
}

$uniqueCandidates = $allCandidates | Sort-Object FullName -Unique
$ordered = $uniqueCandidates | Sort-Object LastWriteTime -Descending

if ($ordered.Count -le $Keep) {
  Write-Host "No cleanup needed. Found $($ordered.Count) files; keep=$Keep."
  exit 0
}

$toDelete = @($ordered | Select-Object -Skip $Keep)

Write-Host "Cleanup plan:"
Write-Host "  Total candidates: $($ordered.Count)"
Write-Host "  Keep newest: $Keep"
Write-Host "  Delete old: $($toDelete.Count)"
Write-Host ""

foreach ($item in $toDelete) {
  Write-Host "  - $($item.FullName)"
}

if (-not $ApplyDeletion) {
  Write-Host ""
  Write-Host "Dry-run only. Use -ApplyDeletion to remove listed files."
  exit 0
}

foreach ($item in $toDelete) {
  Remove-Item -Path $item.FullName -Force
}

Write-Host ""
Write-Host "Deletion completed. Removed $($toDelete.Count) files."
