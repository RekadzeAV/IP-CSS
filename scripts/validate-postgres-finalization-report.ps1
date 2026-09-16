# Validates generated PostgreSQL finalization staging report and returns
# process exit code:
# - 0 if accepted
# - 1 if not accepted or report invalid
#
# Usage:
#   .\scripts\validate-postgres-finalization-report.ps1 `
#      -ReportPath "diagnostics\postgres-finalization\staging-final-report.md"

param(
    [switch]$ShowHelp,
    [string]$ReportPath = "diagnostics\postgres-finalization\staging-final-report.md"
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Validate generated PostgreSQL finalization staging report (markdown heuristics)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\validate-postgres-finalization-report.ps1"
    Write-Host "  .\scripts\validate-postgres-finalization-report.ps1 -ReportPath diagnostics\postgres-finalization\staging-final-report.md"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -ReportPath  Repo-relative or absolute path to staging final markdown"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Report accepted (decision + smoke + critical checklist heuristics)"
    Write-Host "  1  Report missing, malformed, or not accepted"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$resolvedPath = if ([System.IO.Path]::IsPathRooted($ReportPath)) { $ReportPath } else { Join-Path $projectRoot $ReportPath }

if (-not (Test-Path $resolvedPath)) {
    Write-Host "Report not found: $resolvedPath" -ForegroundColor Red
    exit 1
}

$content = Get-Content $resolvedPath -Raw

# Must match generator output from postgres-finalization-generate-staging-report.ps1:
# Accepted: **Decision:** ☑ Accepted (100%) / ⬜ Not Accepted
# Rejected: **Decision:** ⬜ Accepted (100%) / ☑ Not Accepted
$hasDecisionLine = $content -match '\*\*Decision:\*\*'
$decisionAccepted = $content -match '\*\*Decision:\*\*\s*☑\s+Accepted\s*\(100%\)'
$decisionNotAccepted = $content -match '\*\*Decision:\*\*.*☑\s+Not\s+Accepted'
$hasUncheckedCritical = $content -match "- \[ \] Staging cutover \+ rollback rehearsal completed"
$smokeFail = $content -match "\*\*Smoke summary:\*\*\s*❌ FAIL"

if (-not $hasDecisionLine) {
    Write-Host "Decision section is missing or malformed." -ForegroundColor Red
    exit 1
}

if ($decisionNotAccepted -or $hasUncheckedCritical -or $smokeFail) {
    Write-Host "PostgreSQL finalization report validation: NOT ACCEPTED" -ForegroundColor Yellow
    if ($smokeFail) {
        Write-Host "- Smoke summary indicates FAIL." -ForegroundColor Yellow
    }
    if ($hasUncheckedCritical) {
        Write-Host "- Critical checklist item is unchecked: staging cutover + rollback rehearsal." -ForegroundColor Yellow
    }
    if ($decisionNotAccepted) {
        Write-Host "- Decision line shows Not Accepted selected." -ForegroundColor Yellow
    }
    exit 1
}

if (-not $decisionAccepted) {
    Write-Host "PostgreSQL finalization report validation: decision is not Accepted (100%)." -ForegroundColor Yellow
    exit 1
}

Write-Host "PostgreSQL finalization report validation: ACCEPTED (100%)" -ForegroundColor Green
exit 0
