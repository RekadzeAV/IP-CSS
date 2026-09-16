[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$ReportDate = (Get-Date -Format "yyyy-MM-dd"),
    [string]$Environment = "staging",
    [string]$BuildRef = "TBD",
    [string]$Owner = "TBD",
    [string]$Reviewer = "TBD",
    [string]$OutputDir = "docs/reports",
    [switch]$Force
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Generate PostgreSQL staging evidence report scaffold"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\postgresql-staging-evidence-pack.ps1"
    Write-Host "  .\scripts\postgresql-staging-evidence-pack.ps1 -ReportDate 2026-04-27 -BuildRef abc123 -Owner qa-oncall -Reviewer techlead"
    Write-Host "  .\scripts\postgresql-staging-evidence-pack.ps1 -Force"
    Write-Host ""
    Write-Host "Output:"
    Write-Host "  docs/reports/POSTGRESQL_FINALIZATION_STAGING_REPORT_<date>.md"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$resolvedOutputDir = Join-Path $projectRoot $OutputDir

if (-not (Test-Path $resolvedOutputDir)) {
    New-Item -ItemType Directory -Path $resolvedOutputDir -Force | Out-Null
}

$reportFileName = "POSTGRESQL_FINALIZATION_STAGING_REPORT_{0}.md" -f $ReportDate
$reportPath = Join-Path $resolvedOutputDir $reportFileName

if ((Test-Path $reportPath) -and (-not $Force)) {
    throw "Report already exists: $reportPath (use -Force to overwrite)"
}

$generatedAtUtc = [DateTime]::UtcNow.ToString("yyyy-MM-dd HH:mm:ss 'UTC'")

$content = @(
    "# PostgreSQL Finalization Staging Report - $ReportDate",
    "",
    "Generated: $generatedAtUtc",
    "",
    "## Meta",
    "",
    "- Date: $ReportDate",
    "- Environment: $Environment",
    "- Build/Commit: $BuildRef",
    "- Owner: $Owner",
    "- Reviewer: $Reviewer",
    "",
    "## Preconditions",
    "",
    '- [ ] `ENVIRONMENT=production`',
    '- [ ] `NODE_ENV=production`',
    '- [ ] `DB_MODE=postgres`',
    '- [ ] `DATABASE_URL` set',
    '- [ ] `DATABASE_USER` set',
    '- [ ] `DATABASE_PASSWORD` or `DB_PASSWORD` set',
    '- [ ] `ENABLE_FLYWAY=true`',
    '- [ ] Backup created before cutover (snapshot or `pg_dump`)',
    "",
    "## Cutover Execution Log",
    "",
    "| Step | Action | Expected | Actual | Evidence | Status |",
    "|---|---|---|---|---|---|",
    "| 1 | Deploy staging build | Service starts | TODO | TODO | [ ] |",
    "| 2 | Startup preflight | No DB fail-fast errors | TODO | TODO | [ ] |",
    "| 3 | Flyway migrate | Up-to-date schema | TODO | TODO | [ ] |",
    '| 4 | GET `/api/v1/health/ready` | READY + database OK | TODO | TODO | [ ] |',
    "| 5 | DB smoke (camera/event/recording basic flow) | CRUD smoke passes | TODO | TODO | [ ] |",
    "",
    "## Rollback Rehearsal",
    "",
    "| Step | Action | Expected | Actual | Evidence | Status |",
    "|---|---|---|---|---|---|",
    "| 1 | Stop current build | Service stopped | TODO | TODO | [ ] |",
    "| 2 | Restore previous build/env | Previous version starts | TODO | TODO | [ ] |",
    "| 3 | Restore DB backup if required | DB restored to checkpoint | TODO | TODO | [ ] |",
    "| 4 | Post-rollback health check | READY + basic read flow | TODO | TODO | [ ] |",
    "",
    "## DB Smoke Evidence After Cutover/Rollback",
    "",
    "| Check | Cutover | Rollback | Evidence |",
    "|---|---|---|---|",
    '| `/api/v1/health` | TODO | TODO | TODO |',
    '| `/api/v1/health/ready` | TODO | TODO | TODO |',
    "| One camera read/list | TODO | TODO | TODO |",
    "| One event read/list | TODO | TODO | TODO |",
    "| One recording read/list | TODO | TODO | TODO |",
    "",
    "## Risks / Findings",
    "",
    "- Risk 1: TODO",
    "- Risk 2: TODO",
    "- Open issues: TODO",
    "",
    "## Decision for 1.5.6",
    "",
    "- [ ] Staging cutover completed",
    "- [ ] Rollback rehearsal completed",
    "- [ ] DB smoke evidence captured for cutover and rollback",
    "- [ ] No critical blockers remain",
    "",
    "**Decision:** [ ] Accepted / [ ] Not Accepted",
    "**Approver:** TODO"
)

$content -join [Environment]::NewLine | Set-Content -Path $reportPath -Encoding UTF8
Write-Host ("Generated report scaffold: {0}" -f $reportPath) -ForegroundColor Green
