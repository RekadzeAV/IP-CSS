param(
  [string]$SummaryJsonPath = "",
  [string]$ChecklistPath = "",
  [string]$OutputPath = "",
  [switch]$ShowHelp
)

if ($ShowHelp) {
  Write-Host "Publish current web interface automation status markdown."
  Write-Host ""
  Write-Host "Usage:"
  Write-Host "  .\scripts\publish-web-interface-automation-status.ps1"
  Write-Host "  .\scripts\publish-web-interface-automation-status.ps1 -SummaryJsonPath <path> -ChecklistPath <path> -OutputPath <path>"
  exit 0
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$statusDir = Join-Path $repoRoot "docs/status"

if ([string]::IsNullOrWhiteSpace($ChecklistPath)) { $ChecklistPath = Join-Path $statusDir "WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md" }
if ([string]::IsNullOrWhiteSpace($OutputPath)) { $OutputPath = Join-Path $statusDir "WEB_INTERFACE_AUTOMATION_STATUS.md" }

if ([string]::IsNullOrWhiteSpace($SummaryJsonPath)) {
  $candidates = Get-ChildItem -Path $statusDir -Filter "WEB_INTERFACE_AUTOMATION_SUMMARY*.json" -File | Sort-Object LastWriteTime -Descending
  if ($null -eq $candidates -or $candidates.Count -eq 0) {
    throw "No summary JSON files found in $statusDir"
  }
  $SummaryJsonPath = $candidates[0].FullName
}

if (-not (Test-Path -Path $SummaryJsonPath)) { throw "Summary JSON not found: $SummaryJsonPath" }
if (-not (Test-Path -Path $ChecklistPath)) { throw "Checklist not found: $ChecklistPath" }

$summary = Get-Content -Path $SummaryJsonPath -Raw | ConvertFrom-Json
$checklist = Get-Content -Path $ChecklistPath -Raw

function Get-ChecklistLine([string]$Text, [string]$Id) {
  $match = [regex]::Match($Text, "(?m)^- .*" + [regex]::Escape($Id) + ".*$")
  if ($match.Success) { return ($match.Value -replace "^-+\s*", "").Trim() }
  return "N/A"
}

$lines = @(
  "# Web Interface 1.6 Automation Status",
  "",
  "- Generated at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
  "- Summary source: $SummaryJsonPath",
  "- Checklist source: $ChecklistPath",
  "",
  "## Automation Summary",
  "",
  "- Run status: $($summary.runStatus)",
  "- Error code: $($summary.errorCode)",
  "- Exit code: $($summary.exitCode)",
  "- Recommendation: $($summary.recommendation)",
  "- Overall: $($summary.overall)",
  "- F2: $($summary.F2)",
  "- C4: $($summary.C4)",
  "- F2 validation summary: $($summary.f2ValidationSummary)",
  "- Markdown report: $($summary.outputReport)",
  "",
  "## Acceptance Snapshot",
  "",
  "- F2 checklist line: $(Get-ChecklistLine -Text $checklist -Id 'F2')",
  "- C4 checklist line: $(Get-ChecklistLine -Text $checklist -Id 'C4')"
)

[System.IO.File]::WriteAllText($OutputPath, (($lines -join "`r`n") + "`r`n"), [System.Text.Encoding]::UTF8)
Write-Host "Published automation status: $OutputPath"
