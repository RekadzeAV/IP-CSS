# One-command suite for PostgreSQL finalization staging checks (1.5.6)
# Runs:
# 1) postgres-finalization-staging-smoke.ps1
# 2) postgres-finalization-generate-staging-report.ps1
#
# Usage:
#   .\scripts\run-postgres-finalization-suite.ps1 `
#      -BaseUrl "http://localhost:8080" `
#      -Username "admin" `
#      -Password "<admin-password>" `
#      -Environment "staging" `
#      -BuildCommit "<commit-sha>" `
#      -Owner "backend-team" `
#      -Reviewer "qa-team"

[Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSAvoidUsingPlainTextForPassword', '')]
param(
    [switch]$ShowHelp,
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "$env:ADMIN_PASSWORD",
    [string]$Environment = "staging",
    [string]$BuildCommit = "N/A",
    [string]$Owner = "N/A",
    [string]$Reviewer = "N/A",
    [string]$OutputDir = "diagnostics\postgres-finalization",
    [switch]$AutoStartRuntime,
    [int]$RuntimeStartTimeoutSec = 180,
    [switch]$ValidateReport,
    [switch]$SkipPreflight,
    [switch]$GenerateGoNoGo,
    [switch]$FailOnNoGo,
    [string]$RollbackEvidence = "N/A"
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "PostgreSQL finalization suite (smoke + staging report + optional preflight/validate/go-no-go)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-postgres-finalization-suite.ps1 -BaseUrl http://localhost:8080 -Username admin -Password <pwd> -Environment staging -BuildCommit <sha> -Owner team -Reviewer qa"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -OutputDir  Timestamped smoke/final reports under this folder"
    Write-Host "  -AutoStartRuntime  Run start-postgres-finalization-staging.ps1 first"
    Write-Host "  -ValidateReport  Run validate-postgres-finalization-report.ps1 on final report"
    Write-Host "  -SkipPreflight  Skip postgres-finalization-preflight.ps1"
    Write-Host "  -GenerateGoNoGo  Run postgres-finalization-generate-go-no-go.ps1"
    Write-Host "  -FailOnNoGo  Exit 1 when GO/NO-GO markdown is not GO"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Suite completed"
    Write-Host "  1  With -FailOnNoGo when decision is not GO"
    Write-Host "  Non-zero  Preflight/runtime start/smoke/validate failure (fail-fast)"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$resolvedOutDir = if ([System.IO.Path]::IsPathRooted($OutputDir)) { $OutputDir } else { Join-Path $projectRoot $OutputDir }

if (-not (Test-Path $resolvedOutDir)) {
    New-Item -ItemType Directory -Path $resolvedOutDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$smokeReportPath = Join-Path $resolvedOutDir "staging-smoke-report-$timestamp.md"
$finalReportPath = Join-Path $resolvedOutDir "staging-final-report-$timestamp.md"

$smokeScript = Join-Path $scriptDir "postgres-finalization-staging-smoke.ps1"
$generateScript = Join-Path $scriptDir "postgres-finalization-generate-staging-report.ps1"
$startRuntimeScript = Join-Path $scriptDir "start-postgres-finalization-staging.ps1"
$validateReportScript = Join-Path $scriptDir "validate-postgres-finalization-report.ps1"
$preflightScript = Join-Path $scriptDir "postgres-finalization-preflight.ps1"
$goNoGoScript = Join-Path $scriptDir "postgres-finalization-generate-go-no-go.ps1"

if (-not (Test-Path $smokeScript)) { throw "Smoke script not found: $smokeScript" }
if (-not (Test-Path $generateScript)) { throw "Report generator script not found: $generateScript" }
if ($AutoStartRuntime -and -not (Test-Path $startRuntimeScript)) {
    throw "Runtime starter script not found: $startRuntimeScript"
}
if ($ValidateReport -and -not (Test-Path $validateReportScript)) {
    throw "Report validation script not found: $validateReportScript"
}
if (-not $SkipPreflight -and -not (Test-Path $preflightScript)) {
    throw "Preflight script not found: $preflightScript"
}
if ($GenerateGoNoGo -and -not (Test-Path $goNoGoScript)) {
    throw "GO/NO-GO generator script not found: $goNoGoScript"
}
if ($FailOnNoGo -and -not $GenerateGoNoGo) {
    throw "FailOnNoGo requires -GenerateGoNoGo."
}

Write-Host "=== PostgreSQL Finalization Suite ===" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl" -ForegroundColor Gray
Write-Host "Output directory: $resolvedOutDir" -ForegroundColor Gray
Write-Host ""

if (-not $SkipPreflight) {
    $preflightPath = Join-Path $resolvedOutDir "preflight-$timestamp.md"
    Write-Host "[pre] Running preflight checks..." -ForegroundColor Cyan
    & $preflightScript -BaseUrl $BaseUrl -OutputFile $preflightPath
    if ($LASTEXITCODE -ne 0) {
        throw "postgres-finalization-preflight.ps1 failed with exit code $LASTEXITCODE"
    }
    Write-Host "Preflight report: $preflightPath" -ForegroundColor Gray
    Write-Host ""
}

$totalSteps = if ($ValidateReport) { 3 } else { 2 }
$startStep = 1
if ($AutoStartRuntime) {
    Write-Host "[0/$totalSteps] Ensuring staging runtime is up..." -ForegroundColor Cyan
    & $startRuntimeScript -BaseUrl $BaseUrl -TimeoutSec $RuntimeStartTimeoutSec
    if ($LASTEXITCODE -ne 0) {
        throw "start-postgres-finalization-staging.ps1 failed with exit code $LASTEXITCODE"
    }
    Write-Host ""
}

Write-Host "[$startStep/$totalSteps] Running staging smoke..." -ForegroundColor Cyan
& $smokeScript `
    -BaseUrl $BaseUrl `
    -Username $Username `
    -Password $Password `
    -OutputFile $smokeReportPath
if ($LASTEXITCODE -ne 0) {
    throw "postgres-finalization-staging-smoke.ps1 failed with exit code $LASTEXITCODE"
}

Write-Host ""
Write-Host "[$($startStep + 1)/$totalSteps] Generating staging final report..." -ForegroundColor Cyan
& $generateScript `
    -SmokeReport $smokeReportPath `
    -OutputFile $finalReportPath `
    -Environment $Environment `
    -BuildCommit $BuildCommit `
    -Owner $Owner `
    -Reviewer $Reviewer
if ($LASTEXITCODE -ne 0) {
    throw "postgres-finalization-generate-staging-report.ps1 failed with exit code $LASTEXITCODE"
}

if ($ValidateReport) {
    Write-Host ""
    Write-Host "[$($startStep + 2)/$totalSteps] Validating final report..." -ForegroundColor Cyan
    & $validateReportScript -ReportPath $finalReportPath
    if ($LASTEXITCODE -ne 0) {
        throw "validate-postgres-finalization-report.ps1 failed with exit code $LASTEXITCODE"
    }
}

if ($GenerateGoNoGo) {
    Write-Host ""
    Write-Host "[go/no-go] Generating short decision report..." -ForegroundColor Cyan
    $preflightForGoNoGo = if (-not $SkipPreflight) {
        Join-Path $resolvedOutDir "preflight-$timestamp.md"
    } else {
        Join-Path $resolvedOutDir "preflight-report.md"
    }
    $goNoGoReportPath = Join-Path $resolvedOutDir "go-no-go-$timestamp.md"
    & $goNoGoScript `
        -PreflightReport $preflightForGoNoGo `
        -SmokeReport $smokeReportPath `
        -FinalReport $finalReportPath `
        -OutputFile $goNoGoReportPath `
        -Environment $Environment `
        -BuildCommit $BuildCommit `
        -Owner $Owner `
        -Reviewer $Reviewer `
        -RollbackEvidence $RollbackEvidence
    if ($LASTEXITCODE -ne 0) {
        throw "postgres-finalization-generate-go-no-go.ps1 failed with exit code $LASTEXITCODE"
    }

    if ($FailOnNoGo) {
        $goNoGoContent = Get-Content $goNoGoReportPath -Raw
        $isGo = $goNoGoContent -match "(?m)^- \[x\] GO\s*$"
        if (-not $isGo) {
            Write-Host "GO/NO-GO decision is NO-GO. Failing suite because -FailOnNoGo is enabled." -ForegroundColor Red
            exit 1
        }
        Write-Host "GO/NO-GO decision is GO." -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Suite completed." -ForegroundColor Green
Write-Host "Smoke report: $smokeReportPath" -ForegroundColor Green
Write-Host "Final report: $finalReportPath" -ForegroundColor Green
if ($GenerateGoNoGo) {
    Write-Host "GO/NO-GO report: $(Join-Path $resolvedOutDir "go-no-go-$timestamp.md")" -ForegroundColor Green
}
