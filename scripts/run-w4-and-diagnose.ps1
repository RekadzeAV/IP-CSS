[Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSAvoidUsingPlainTextForPassword', '')]
[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [ValidateSet("local", "staging", "strict")]
    [string]$RunProfile = "local",
    [switch]$IncludeDesktopSmoke,
    [string]$DesktopSmokeBaseUrl = "",
    [string]$DesktopSmokeUsername = "admin",
    [string]$DesktopSmokePassword = "",
    [string[]]$DesktopSmokePlaylistUrls = @(),
    [switch]$NoPlatformSmokeCompileOnlyGate,
    [switch]$AndroidLocalFrameAnalytics,
    [switch]$AsJsonDiagnosis
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Run W4 profile then diagnose latest report (one command)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-w4-and-diagnose.ps1 -RunProfile local"
    Write-Host "  .\scripts\run-w4-and-diagnose.ps1 -RunProfile strict"
    Write-Host "  .\scripts\run-w4-and-diagnose.ps1 -RunProfile staging -AsJsonDiagnosis"
    Write-Host "  .\scripts\run-w4-and-diagnose.ps1 -RunProfile local -AndroidLocalFrameAnalytics"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  Same passthrough as run-w4-profile.ps1: -IncludeDesktopSmoke, -DesktopSmoke*, -NoPlatformSmokeCompileOnlyGate, -AndroidLocalFrameAnalytics."
    Write-Host "  -AsJsonDiagnosis  Forward -AsJson to show-latest-w4-gate-status.ps1."
    Write-Host ""
    Write-Host "Steps:"
    Write-Host "  1) scripts/run-w4-profile.ps1"
    Write-Host "  2) scripts/show-latest-w4-gate-status.ps1 (with CI-oriented flags when run exited 1 or 3)"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  Propagates W4 run exit code; may return diagnosis exit 2/3 when run failed with matching policy."
    exit 0
}
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runScript = Join-Path $scriptDir "run-w4-profile.ps1"
$diagnoseScript = Join-Path $scriptDir "show-latest-w4-gate-status.ps1"

if (-not (Test-Path $runScript)) {
    Write-Host "run-w4-profile.ps1 not found: $runScript" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $diagnoseScript)) {
    Write-Host "show-latest-w4-gate-status.ps1 not found: $diagnoseScript" -ForegroundColor Red
    exit 1
}

$runArgs = @{
    RunProfile = $RunProfile
}
if ($IncludeDesktopSmoke) {
    $runArgs["IncludeDesktopSmoke"] = $true
}
if (-not [string]::IsNullOrWhiteSpace($DesktopSmokeBaseUrl)) {
    $runArgs["DesktopSmokeBaseUrl"] = $DesktopSmokeBaseUrl
}
if (-not [string]::IsNullOrWhiteSpace($DesktopSmokeUsername)) {
    $runArgs["DesktopSmokeUsername"] = $DesktopSmokeUsername
}
if (-not [string]::IsNullOrWhiteSpace($DesktopSmokePassword)) {
    $runArgs["DesktopSmokePassword"] = $DesktopSmokePassword
}
if ($DesktopSmokePlaylistUrls.Count -gt 0) {
    $runArgs["DesktopSmokePlaylistUrls"] = $DesktopSmokePlaylistUrls
}
if ($NoPlatformSmokeCompileOnlyGate) {
    $runArgs["NoPlatformSmokeCompileOnlyGate"] = $true
}
if ($AndroidLocalFrameAnalytics) {
    $runArgs["AndroidLocalFrameAnalytics"] = $true
}

Write-Host ("==> W4 run+diagnose profile: {0}" -f $RunProfile) -ForegroundColor Cyan
& $runScript @runArgs
$runExitCode = $LASTEXITCODE

Write-Host ""
Write-Host "==> Diagnosis of latest W4 run" -ForegroundColor Cyan

$diagnoseArgs = @{}
if ($AsJsonDiagnosis) {
    $diagnoseArgs["AsJson"] = $true
}
if ($runExitCode -eq 1) {
    $diagnoseArgs["FailOnFail"] = $true
} elseif ($runExitCode -eq 3) {
    $diagnoseArgs["FailOnPartial"] = $true
}

& $diagnoseScript @diagnoseArgs
$diagnoseExitCode = $LASTEXITCODE

if ($runExitCode -ne 0) {
    Write-Host ("Final status: W4 run failed with exit code {0}" -f $runExitCode) -ForegroundColor Red
    exit $runExitCode
}

if ($diagnoseExitCode -ne 0) {
    Write-Host ("Final status: diagnose returned non-zero exit code {0}" -f $diagnoseExitCode) -ForegroundColor Yellow
    exit $diagnoseExitCode
}

Write-Host "Final status: SUCCESS" -ForegroundColor Green
exit 0
