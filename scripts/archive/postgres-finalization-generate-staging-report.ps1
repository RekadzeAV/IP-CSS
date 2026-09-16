# Generates a filled staging acceptance report for PostgreSQL finalization (1.5.6)
# based on smoke-test markdown output.
#
# Usage:
#   .\scripts\postgres-finalization-generate-staging-report.ps1 `
#      -SmokeReport "diagnostics\postgres-finalization\staging-smoke-report.md" `
#      -OutputFile "diagnostics\postgres-finalization\staging-final-report.md" `
#      -Environment "staging" `
#      -BuildCommit "<sha>" `
#      -Owner "backend-team" `
#      -Reviewer "qa-team"

param(
    [switch]$ShowHelp,
    [string]$SmokeReport = "diagnostics\postgres-finalization\staging-smoke-report.md",
    [string]$OutputFile = "diagnostics\postgres-finalization\staging-final-report.md",
    [string]$Environment = "staging",
    [string]$BuildCommit = "N/A",
    [string]$Owner = "N/A",
    [string]$Reviewer = "N/A"
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Generate PostgreSQL finalization staging acceptance markdown from smoke report"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\postgres-finalization-generate-staging-report.ps1 -SmokeReport diagnostics\postgres-finalization\staging-smoke-report.md -OutputFile diagnostics\postgres-finalization\staging-final-report.md"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Environment  -BuildCommit  -Owner  -Reviewer  Metadata embedded in the report"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Report generated"
    Write-Host "  Non-zero  Smoke report missing or generation error"
    exit 0
}

function Resolve-ProjectPath {
    param([string]$PathValue)
    $scriptDir = $PSScriptRoot
    $projectRoot = Split-Path -Parent $scriptDir
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

function Get-SmokeStatus {
    param(
        [string]$Content,
        [string]$CheckName
    )
    $pattern = "\|\s*$([regex]::Escape($CheckName))\s*\|.*\|\s*(вњ…|вќЊ)\s*\|"
    $m = [regex]::Match($Content, $pattern)
    if ($m.Success) { return $m.Groups[1].Value }
    return "вќЊ"
}

$resolvedSmoke = Resolve-ProjectPath -PathValue $SmokeReport
$resolvedOut = Resolve-ProjectPath -PathValue $OutputFile
$outputDir = Split-Path -Parent $resolvedOut
if (-not (Test-Path $outputDir)) { New-Item -ItemType Directory -Path $outputDir -Force | Out-Null }

if (-not (Test-Path $resolvedSmoke)) {
    throw "Smoke report not found: $resolvedSmoke"
}

$smokeContent = Get-Content $resolvedSmoke -Raw
$date = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

function Get-CheckboxMark {
    param([bool]$Value)
    if ($Value) { return "[x]" }
    return "[ ]"
}

$envProduction = [string]::Equals($env:ENVIRONMENT, "production", [System.StringComparison]::OrdinalIgnoreCase)
$nodeProduction = [string]::Equals($env:NODE_ENV, "production", [System.StringComparison]::OrdinalIgnoreCase)
$dbModePostgres = [string]::Equals($env:DB_MODE, "postgres", [System.StringComparison]::OrdinalIgnoreCase)
$databaseUrlSet = -not [string]::IsNullOrWhiteSpace($env:DATABASE_URL)
$databaseUserSet = -not [string]::IsNullOrWhiteSpace($env:DATABASE_USER)
$databasePasswordSet = -not [string]::IsNullOrWhiteSpace($env:DATABASE_PASSWORD) -or -not [string]::IsNullOrWhiteSpace($env:DB_PASSWORD)
$flywayEnabled = [string]::Equals($env:ENABLE_FLYWAY, "true", [System.StringComparison]::OrdinalIgnoreCase)

$statusMap = @{
    "Login" = (Get-SmokeStatus -Content $smokeContent -CheckName "POST /api/v1/auth/login")
    "Refresh" = (Get-SmokeStatus -Content $smokeContent -CheckName "POST /api/v1/auth/refresh")
    "Logout" = (Get-SmokeStatus -Content $smokeContent -CheckName "POST /api/v1/auth/logout")
    "Cameras" = (Get-SmokeStatus -Content $smokeContent -CheckName "GET /api/v1/cameras (auth)")
    "Events" = (Get-SmokeStatus -Content $smokeContent -CheckName "GET /api/v1/events (auth)")
    "Recordings" = (Get-SmokeStatus -Content $smokeContent -CheckName "GET /api/v1/recordings (auth)")
    "Health" = (Get-SmokeStatus -Content $smokeContent -CheckName "GET /api/v1/health")
    "Ready" = (Get-SmokeStatus -Content $smokeContent -CheckName "GET /api/v1/health/ready")
    "DbHealth" = (Get-SmokeStatus -Content $smokeContent -CheckName "GET /api/v1/database/health (admin)")
    "DbMigrations" = (Get-SmokeStatus -Content $smokeContent -CheckName "GET /api/v1/database/migrations (admin)")
    "DbPool" = (Get-SmokeStatus -Content $smokeContent -CheckName "GET /api/v1/database/pool/stats (admin)")
}

$allSmokePassed = ($statusMap.Values | Where-Object { $_ -eq "вќЊ" }).Count -eq 0
$smokeDecision = if ($allSmokePassed) { "вњ… PASS" } else { "вќЊ FAIL" }
$finalDecision = if ($allSmokePassed) { "в‘ Accepted (100%) / в¬њ Not Accepted" } else { "в¬њ Accepted (100%) / в‘ Not Accepted" }

$lines = @()
$lines += "# PostgreSQL Finalization Staging Report"
$lines += ""
$lines += "## Meta"
$lines += ""
$lines += "- Date: $date"
$lines += "- Environment: $Environment"
$lines += "- Build/Commit: $BuildCommit"
$lines += "- Owner: $Owner"
$lines += "- Reviewer: $Reviewer"
$lines += ""
$lines += "## Preconditions"
$lines += ""
$lines += ('- ' + (Get-CheckboxMark $envProduction) + ' `ENVIRONMENT=production`')
$lines += ('- ' + (Get-CheckboxMark $nodeProduction) + ' `NODE_ENV=production`')
$lines += ('- ' + (Get-CheckboxMark $dbModePostgres) + ' `DB_MODE=postgres`')
$lines += ('- ' + (Get-CheckboxMark $databaseUrlSet) + ' `DATABASE_URL` set')
$lines += ('- ' + (Get-CheckboxMark $databaseUserSet) + ' `DATABASE_USER` set')
$lines += ('- ' + (Get-CheckboxMark $databasePasswordSet) + ' `DATABASE_PASSWORD` (or `DB_PASSWORD`) set')
$lines += ('- ' + (Get-CheckboxMark $flywayEnabled) + ' `ENABLE_FLYWAY=true`')
$lines += "- [ ] Backup created before cutover"
$lines += ""
$lines += "## Cutover Execution Log"
$lines += ""
$lines += "| Step | Command / Action | Expected Result | Actual Result | Evidence | Status |"
$lines += "|---|---|---|---|---|---|"
$lines += "| 1 | Deploy new build | Service starts | Manual validation required |  | в¬њ |"
$lines += "| 2 | Startup preflight | No fail-fast errors | Manual validation required |  | в¬њ |"
$lines += "| 3 | Flyway migrations | Applied successfully / up-to-date | Manual validation required |  | в¬њ |"
$lines += "| 4 | `/api/v1/health` | `status=OK/DEGRADED` with valid checks | See smoke report | $resolvedSmoke | $($statusMap["Health"]) |"
$lines += "| 5 | `/api/v1/health/ready` | `READY` | See smoke report | $resolvedSmoke | $($statusMap["Ready"]) |"
$lines += "| 6 | `/api/v1/database/health` (admin) | DB healthy | See smoke report | $resolvedSmoke | $($statusMap["DbHealth"]) |"
$lines += "| 7 | `/api/v1/database/migrations` (admin) | Up-to-date | See smoke report | $resolvedSmoke | $($statusMap["DbMigrations"]) |"
$lines += "| 8 | `/api/v1/database/pool/stats` (admin) | Pool healthy | See smoke report | $resolvedSmoke | $($statusMap["DbPool"]) |"
$lines += ""
$lines += "## Functional Smoke"
$lines += ""
$lines += "| Scenario | Expected | Actual | Evidence | Status |"
$lines += "|---|---|---|---|---|"
$lines += "| Login | 200, auth cookies set | See smoke report | $resolvedSmoke | $($statusMap["Login"]) |"
$lines += "| Refresh | 200, token rotation works | See smoke report | $resolvedSmoke | $($statusMap["Refresh"]) |"
$lines += "| Logout | 200, refresh token revoked | See smoke report | $resolvedSmoke | $($statusMap["Logout"]) |"
$lines += "| GET `/api/v1/cameras` | 200 for authorized role | See smoke report | $resolvedSmoke | $($statusMap["Cameras"]) |"
$lines += "| GET `/api/v1/events` | 200 for authorized role | See smoke report | $resolvedSmoke | $($statusMap["Events"]) |"
$lines += "| GET `/api/v1/recordings` | 200 for authorized role | See smoke report | $resolvedSmoke | $($statusMap["Recordings"]) |"
$lines += ""
$lines += "**Smoke summary:** $smokeDecision"
$lines += ""
$lines += "## Rollback Rehearsal"
$lines += ""
$lines += "| Step | Action | Expected | Actual | Evidence | Status |"
$lines += "|---|---|---|---|---|---|"
$lines += "| 1 | Stop new deployment | Service stopped | Manual validation required |  | в¬њ |"
$lines += "| 2 | Restore previous build | Previous version starts | Manual validation required |  | в¬њ |"
$lines += "| 3 | Restore DB backup (if required) | DB restored | Manual validation required |  | в¬њ |"
$lines += "| 4 | Post-rollback health/smoke | System operational | Manual validation required |  | в¬њ |"
$lines += ""
$lines += "## Risks / Findings"
$lines += ""
$lines += "- Risk 1: Ensure manual DB admin endpoints are validated before acceptance."
$lines += "- Risk 2: Ensure rollback rehearsal is completed with evidence."
$lines += "- Open issues:"
$lines += ""
$lines += "## Final Acceptance for 1.5.6"
$lines += ""
$lines += "- [ ] Production startup blocked without PostgreSQL config (manual / policy attestation)"
$lines += "- [ ] No in-memory user repository fallback in production (manual / policy attestation)"
$lines += "- [ ] Safe admin bootstrap policy validated (manual / policy attestation)"
$lines += ('- ' + (Get-CheckboxMark (($statusMap["Ready"] -eq "вњ…") -and ($statusMap["DbHealth"] -eq "вњ…"))) + ' Readiness + DB health smoke checks passed (from smoke report)')
$lines += "- [ ] CI gate `PostgreSQL Finalization Gate` is green (manual / CI attestation)"
$lines += "- [ ] Staging cutover + rollback rehearsal completed"
$lines += ""
$lines += "**Decision:** $finalDecision"
$lines += "**Approver:**"

Set-Content -Path $resolvedOut -Value ($lines -join [Environment]::NewLine) -Encoding UTF8
Write-Host "Generated staging final report: $resolvedOut" -ForegroundColor Cyan
