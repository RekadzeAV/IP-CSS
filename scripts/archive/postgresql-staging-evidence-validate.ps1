[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$ReportPath = "docs/reports/POSTGRESQL_FINALIZATION_STAGING_REPORT_2026-04-27.md",
    [switch]$AllowInProgress
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Validate PostgreSQL staging evidence report completeness."
    Write-Host "Usage:"
    Write-Host "  .\scripts\postgresql-staging-evidence-validate.ps1 -ReportPath <path>"
    Write-Host "  .\scripts\postgresql-staging-evidence-validate.ps1 -ReportPath <path> -AllowInProgress"
    Write-Host "Exit codes: 0=valid, 2=incomplete/invalid"
    exit 0
}

function Resolve-ProjectPath {
    param([string]$PathValue)
    $projectRoot = Split-Path -Parent $PSScriptRoot
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

$reportAbs = Resolve-ProjectPath -PathValue $ReportPath
if (-not (Test-Path -LiteralPath $reportAbs)) {
    Write-Host "PostgreSQL evidence report not found: $reportAbs" -ForegroundColor Red
    exit 2
}

$text = Get-Content -LiteralPath $reportAbs -Raw
$issues = New-Object System.Collections.Generic.List[string]

# Basic structural checks
foreach ($requiredHeading in @(
    "## Preconditions",
    "## Cutover Execution Log",
    "## Rollback Rehearsal",
    "## DB Smoke Evidence After Cutover/Rollback",
    "## Decision for 1.5.6"
)) {
    if ($text -notmatch [regex]::Escape($requiredHeading)) {
        $issues.Add("Missing section: $requiredHeading")
    }
}

# Hard blockers for acceptance
if ($text -match "TODO") {
    $issues.Add("Report contains TODO placeholders.")
}
if ($text -match "\|\s*\[\s\]\s*\|") {
    $issues.Add("One or more table status cells are unchecked ([ ]).")
}

$uncheckedMandatory = [regex]::Matches($text, "^- \[ \] ", [System.Text.RegularExpressions.RegexOptions]::Multiline).Count
if ($uncheckedMandatory -gt 0) {
    $issues.Add("Mandatory checklist contains unchecked items ($uncheckedMandatory).")
}

if ($text -notmatch "\*\*Decision:\*\*\s*\[[xX]\]\s*Accepted") {
    $issues.Add("Final decision is not marked as Accepted.")
}
if ($text -notmatch "\*\*Approver:\*\*\s*(?!TODO).+") {
    $issues.Add("Approver is empty or still TODO.")
}

if ($issues.Count -gt 0) {
    if ($AllowInProgress) {
        Write-Host "PostgreSQL staging evidence validation: IN PROGRESS" -ForegroundColor Yellow
        foreach ($issue in $issues) {
            Write-Host " - $issue"
        }
        exit 0
    }
    Write-Host "PostgreSQL staging evidence validation: INVALID" -ForegroundColor Yellow
    foreach ($issue in $issues) {
        Write-Host " - $issue"
    }
    exit 2
}

Write-Host "PostgreSQL staging evidence validation: VALID" -ForegroundColor Green
exit 0
