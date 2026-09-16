[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$Date = (Get-Date -Format "yyyy-MM-dd"),
    [string]$AcceptanceProfilePath = "config\video-e2e-acceptance-profile.local.json",
    [switch]$SkipReadinessCheck
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Run full NAS field finalization pipeline in one command."
    Write-Host "Usage:"
    Write-Host "  .\scripts\nas-field-full-finalize.ps1 -Date 2026-04-27"
    Write-Host ""
    Write-Host "Pipeline:"
    Write-Host "  1) scripts/nas-field-finalize.ps1 -RecalculateGate"
    Write-Host "  2) scripts/sync-release-status-docs.ps1"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Final decision is GO and docs sync completed"
    Write-Host "  1  Final decision is NO-GO"
    Write-Host "  2  Readiness check failed (if not skipped)"
    exit 0
}

function Resolve-ProjectPath {
    param([string]$PathValue)
    $projectRoot = Split-Path -Parent $PSScriptRoot
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

$finalizeScript = Resolve-ProjectPath -PathValue "scripts\nas-field-finalize.ps1"
$syncDocsScript = Resolve-ProjectPath -PathValue "scripts\sync-release-status-docs.ps1"

if (-not (Test-Path -LiteralPath $finalizeScript)) {
    throw "Finalize script not found: $finalizeScript"
}
if (-not (Test-Path -LiteralPath $syncDocsScript)) {
    throw "Docs sync script not found: $syncDocsScript"
}

if ($SkipReadinessCheck) {
    & $finalizeScript -Date $Date -AcceptanceProfilePath $AcceptanceProfilePath -RecalculateGate -SkipReadinessCheck
} else {
    & $finalizeScript -Date $Date -AcceptanceProfilePath $AcceptanceProfilePath -RecalculateGate
}
$finalizeExitCode = $LASTEXITCODE

if ($finalizeExitCode -eq 2) {
    Write-Host "Readiness is not complete; docs sync skipped to avoid locking in placeholder field status." -ForegroundColor Yellow
    exit 2
}

& $syncDocsScript -Date $Date
$syncExitCode = $LASTEXITCODE
if ($syncExitCode -ne 0) {
    throw "Docs sync failed with exit code $syncExitCode"
}

if ($finalizeExitCode -eq 0) {
    Write-Host "NAS full finalization pipeline completed successfully." -ForegroundColor Green
    exit 0
}

Write-Host "Finalization completed with non-GO decision (exit code $finalizeExitCode); docs were synchronized." -ForegroundColor Yellow
exit $finalizeExitCode

