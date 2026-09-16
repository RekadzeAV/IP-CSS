param(
    [switch]$SkipGradle,
    [switch]$StrictRuntimeMatrix,
    [switch]$CiProfile,
    [switch]$Help,
    [switch]$ShowHelp,
    [string]$ReportJson
)

$ErrorActionPreference = "Stop"

if ($Help -or $ShowHelp) {
    Write-Host "KMP Phase 1 verifier (PowerShell wrapper over verify-kmp-phase1.py)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\ci\verify-kmp-phase1.ps1"
    Write-Host "  .\scripts\ci\verify-kmp-phase1.ps1 -SkipGradle"
    Write-Host "  .\scripts\ci\verify-kmp-phase1.ps1 -StrictRuntimeMatrix"
    Write-Host "  .\scripts\ci\verify-kmp-phase1.ps1 -CiProfile"
    Write-Host "  .\scripts\ci\verify-kmp-phase1.ps1 -CiProfile -ReportJson diagnostics\kmp\verify-report.json"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -SkipGradle           Run only Python checks (no Gradle)"
    Write-Host "  -StrictRuntimeMatrix  Treat enabled empty playlistUrls as errors"
    Write-Host "  -CiProfile            CI-equivalent mode (skip gradle + strict runtime matrix)"
    Write-Host "  -ReportJson <path>    Pass --report-json to Python verifier"
    Write-Host ""
    Write-Host "Help switches:"
    Write-Host "  -Help  -ShowHelp  (same; print this text and exit)"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Python verifier succeeded"
    Write-Host "  Non-zero  Python verifier failed (propagated)"
    exit 0
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$scriptPath = Join-Path $repoRoot "scripts\ci\verify-kmp-phase1.py"

if (-not (Test-Path $scriptPath)) {
    throw "Script not found: $scriptPath"
}

$pythonArgs = @($scriptPath)
if ($SkipGradle) {
    $pythonArgs += "--skip-gradle"
}
if ($StrictRuntimeMatrix) {
    $pythonArgs += "--strict-runtime-matrix"
}
if ($CiProfile) {
    $pythonArgs += "--ci-profile"
}
if (-not [string]::IsNullOrWhiteSpace($ReportJson)) {
    $pythonArgs += "--report-json"
    $pythonArgs += $ReportJson
}

Write-Host "Running KMP Phase 1 verifier..."
Write-Host "Repository: $repoRoot"

& python @pythonArgs
if ($LASTEXITCODE -ne 0) {
    throw "KMP Phase 1 verification failed with exit code $LASTEXITCODE"
}

Write-Host "KMP Phase 1 verification completed successfully."
