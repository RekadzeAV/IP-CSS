[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$Date = (Get-Date -Format "yyyy-MM-dd"),
    [string]$AcceptanceProfilePath = "config\video-e2e-acceptance-profile.local.json",
    [switch]$RecalculateGate,
    [switch]$SkipReadinessCheck
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Finalize NAS field validation status and optionally recalculate video gate."
    Write-Host "Usage:"
    Write-Host "  .\scripts\nas-field-finalize.ps1 -Date 2026-04-27 -RecalculateGate"
    Write-Host ""
    Write-Host "What it does:"
    Write-Host "  0) Runs scripts/nas-field-readiness-check.ps1 (can be skipped)"
    Write-Host "  1) Runs scripts/nas-field-aggregate.ps1"
    Write-Host "  2) Optionally runs scripts/video-e2e-go-no-go.ps1"
    Write-Host "  3) Prints final field/program decisions from aggregator"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Field validation is GO/CONDITIONAL GO and final decision is GO"
    Write-Host "  1  Final decision is NO-GO"
    exit 0
}

function Resolve-ProjectPath {
    param([string]$PathValue)
    $projectRoot = Split-Path -Parent $PSScriptRoot
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

$aggregateScript = Resolve-ProjectPath -PathValue "scripts\nas-field-aggregate.ps1"
$readinessScript = Resolve-ProjectPath -PathValue "scripts\nas-field-readiness-check.ps1"
$goNoGoScript = Resolve-ProjectPath -PathValue "scripts\video-e2e-go-no-go.ps1"
$aggPath = Resolve-ProjectPath -PathValue "docs/reports/NAS_FIELD_AGGREGATOR_$Date.md"
$profilePath = Resolve-ProjectPath -PathValue $AcceptanceProfilePath

if (-not (Test-Path -LiteralPath $aggregateScript)) {
    throw "Aggregate script not found: $aggregateScript"
}

if (-not $SkipReadinessCheck) {
    if (-not (Test-Path -LiteralPath $readinessScript)) {
        throw "Readiness check script not found: $readinessScript"
    }
    & $readinessScript -Date $Date
    $readinessExitCode = $LASTEXITCODE
    if ($readinessExitCode -ne 0) {
        Write-Host "Readiness check failed. Use -SkipReadinessCheck only for intentional partial/fallback finalize." -ForegroundColor Yellow
        exit $readinessExitCode
    }
}

& $aggregateScript -Date $Date

if ($RecalculateGate) {
    if (-not (Test-Path -LiteralPath $goNoGoScript)) {
        throw "Go/No-Go script not found: $goNoGoScript"
    }
    if (-not (Test-Path -LiteralPath $profilePath)) {
        throw "Acceptance profile not found: $profilePath"
    }
    & $goNoGoScript -AcceptanceProfilePath $profilePath
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        Write-Host "video-e2e-go-no-go returned non-zero (strict/profile policy may still be acceptable)." -ForegroundColor Yellow
    }
}

if (-not (Test-Path -LiteralPath $aggPath)) {
    throw "Aggregator not found: $aggPath"
}

$agg = Get-Content -LiteralPath $aggPath -Raw
$mField = [regex]::Match($agg, "(?m)^- Field validation:\s*(.+?)\s*$")
$mFinal = [regex]::Match($agg, "(?m)^- Final decision:\s*(.+?)\s*$")
$fieldValidation = if ($mField.Success) { $mField.Groups[1].Value.Trim() } else { "UNKNOWN" }
$finalDecision = if ($mFinal.Success) { $mFinal.Groups[1].Value.Trim() } else { "UNKNOWN" }

Write-Host "Field validation: $fieldValidation" -ForegroundColor Cyan
Write-Host "Final decision: $finalDecision" -ForegroundColor Cyan

if ($finalDecision -match "^GO$") {
    exit 0
}
exit 1

