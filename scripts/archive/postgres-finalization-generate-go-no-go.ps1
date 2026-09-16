# Generates short GO/NO-GO report for PostgreSQL finalization (1.5.6)
# based on existing preflight/smoke/final staging markdown reports.
#
# Usage:
#   .\scripts\postgres-finalization-generate-go-no-go.ps1 `
#      -PreflightReport "diagnostics\postgres-finalization\preflight-report.md" `
#      -SmokeReport "diagnostics\postgres-finalization\staging-smoke-report.md" `
#      -FinalReport "diagnostics\postgres-finalization\staging-final-report.md" `
#      -OutputFile "diagnostics\postgres-finalization\go-no-go.md" `
#      -Environment "staging" `
#      -BuildCommit "<sha>" `
#      -Owner "backend-team" `
#      -Reviewer "qa-team"

param(
    [switch]$ShowHelp,
    [string]$PreflightReport = "diagnostics\postgres-finalization\preflight-report.md",
    [string]$SmokeReport = "diagnostics\postgres-finalization\staging-smoke-report.md",
    [string]$FinalReport = "diagnostics\postgres-finalization\staging-final-report.md",
    [string]$OutputFile = "diagnostics\postgres-finalization\go-no-go.md",
    [string]$Environment = "staging",
    [string]$BuildCommit = "N/A",
    [string]$Owner = "N/A",
    [string]$Reviewer = "N/A",
    [string]$RollbackEvidence = "N/A"
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Generate PostgreSQL finalization GO/NO-GO markdown from preflight/smoke/final reports"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\postgres-finalization-generate-go-no-go.ps1"
    Write-Host "  .\scripts\postgres-finalization-generate-go-no-go.ps1 -PreflightReport ... -SmokeReport ... -FinalReport ... -OutputFile ..."
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  Paths default under diagnostics\postgres-finalization\"
    Write-Host "  -RollbackEvidence  Text for rollback rehearsal evidence"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Report generated"
    Write-Host "  Non-zero  Missing inputs or generation error"
    exit 0
}

function Resolve-ProjectPath {
    param([string]$PathValue)
    $scriptDir = $PSScriptRoot
    $projectRoot = Split-Path -Parent $scriptDir
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

function Get-ContentIfExists {
    param([string]$PathValue)
    if (-not (Test-Path $PathValue)) { return $null }
    return Get-Content $PathValue -Raw
}

function Mark {
    param([bool]$Value)
    if ($Value) { return "[x]" }
    return "[ ]"
}

function Test-ContainsAny {
    param(
        [string]$Text,
        [string[]]$Patterns
    )
    if ([string]::IsNullOrWhiteSpace($Text)) { return $false }
    foreach ($p in $Patterns) {
        if ($Text -match $p) { return $true }
    }
    return $false
}

$resolvedPreflight = Resolve-ProjectPath -PathValue $PreflightReport
$resolvedSmoke = Resolve-ProjectPath -PathValue $SmokeReport
$resolvedFinal = Resolve-ProjectPath -PathValue $FinalReport
$resolvedOut = Resolve-ProjectPath -PathValue $OutputFile

$outDir = Split-Path -Parent $resolvedOut
if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

$preflightText = Get-ContentIfExists -PathValue $resolvedPreflight
$smokeText = Get-ContentIfExists -PathValue $resolvedSmoke
$finalText = Get-ContentIfExists -PathValue $resolvedFinal

$preflightPassed = Test-ContainsAny -Text $preflightText -Patterns @("Critical status:\s*PASS", "Preflight checks passed")
$smokePassed = Test-ContainsAny -Text $smokeText -Patterns @("Passed:\s*\*\*11\s*/\s*11\*\*", "Overall:\s*вњ…\s*PASS")
# Align with staging-final markdown from postgres-finalization-generate-staging-report.ps1:
# **Decision:** Accepted (100%) appears in both branches; match only **Decision:** line with leading checkmark on Accepted (see validate-postgres-finalization-report.ps1).
$finalValidated = Test-ContainsAny -Text $finalText -Patterns @('\*\*Decision:\*\*\s*в‘\s+Accepted\s*\(100%\)')
$readyOk = Test-ContainsAny -Text $smokeText -Patterns @("\|\s*GET /api/v1/health/ready\s*\|.*\|\s*вњ…\s*\|")
$dbHealthOk = Test-ContainsAny -Text $smokeText -Patterns @("\|\s*GET /api/v1/database/health \(admin\)\s*\|.*\|\s*вњ…\s*\|")
$dbMigrationsOk = Test-ContainsAny -Text $smokeText -Patterns @("\|\s*GET /api/v1/database/migrations \(admin\)\s*\|.*\|\s*вњ…\s*\|")
$dbPoolOk = Test-ContainsAny -Text $smokeText -Patterns @("\|\s*GET /api/v1/database/pool/stats \(admin\)\s*\|.*\|\s*вњ…\s*\|")
$noEmbeddedFallback = Test-ContainsAny -Text $finalText -Patterns @("No in-memory user repository fallback", "\[x\].*No in-memory")
$rollbackDone = -not [string]::IsNullOrWhiteSpace($RollbackEvidence) -and $RollbackEvidence -ne "N/A"

$goDecision = $preflightPassed -and $smokePassed -and $finalValidated -and $readyOk -and $dbHealthOk -and $dbMigrationsOk -and $dbPoolOk -and $noEmbeddedFallback -and $rollbackDone

$lines = @()
$lines += "# PostgreSQL 1.5.6 Go/No-Go"
$lines += ""
$lines += "## Meta"
$lines += ""
$lines += "- Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$lines += "- Environment: $Environment"
$lines += "- Build/Commit: $BuildCommit"
$lines += "- Owner: $Owner"
$lines += "- Reviewer: $Reviewer"
$lines += ""
$lines += "## Inputs (artifacts)"
$lines += ""
$lines += "- Preflight report: $resolvedPreflight"
$lines += "- Smoke report: $resolvedSmoke"
$lines += "- Final staging report: $resolvedFinal"
$lines += "- Rollback evidence: $RollbackEvidence"
$lines += ""
$lines += "## Gate Checklist"
$lines += ""
$lines += ('- ' + (Mark $preflightPassed) + ' Preflight passed (`postgres-finalization-preflight.ps1`)')
$lines += ('- ' + (Mark $smokePassed) + ' Smoke passed (`11/11`) on target staging')
$lines += ('- ' + (Mark $finalValidated) + ' Final report validated (`validate-postgres-finalization-report.ps1`)')
$lines += ('- ' + (Mark $readyOk) + ' `/api/v1/health/ready` returns `200`')
$lines += ('- ' + (Mark ($dbHealthOk -and $dbMigrationsOk -and $dbPoolOk)) + ' DB admin endpoints return `200`:')
$lines += ('  - ' + (Mark $dbHealthOk) + ' `/api/v1/database/health`')
$lines += ('  - ' + (Mark $dbMigrationsOk) + ' `/api/v1/database/migrations`')
$lines += ('  - ' + (Mark $dbPoolOk) + ' `/api/v1/database/pool/stats`')
$lines += ("- " + (Mark $rollbackDone) + " Rollback rehearsal completed and documented")
$lines += ("- " + (Mark $noEmbeddedFallback) + " No production fallback to embedded/in-memory path")
$lines += ""
$lines += "## Risks / Blockers"
$lines += ""
$lines += "- R1:"
$lines += "- R2:"
$lines += "- Open issues:"
$lines += ""
$lines += "## Decision"
$lines += ""
$lines += ("- " + (Mark $goDecision) + " GO")
$lines += ("- " + (Mark (-not $goDecision)) + " NO-GO")
$lines += ""
$lines += "Reason:"
$lines += ""
$lines += "Approver:"

Set-Content -Path $resolvedOut -Value ($lines -join [Environment]::NewLine) -Encoding UTF8
Write-Host "Generated GO/NO-GO report: $resolvedOut" -ForegroundColor Cyan
