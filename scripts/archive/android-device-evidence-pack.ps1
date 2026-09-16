[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$OutputDir = "diagnostics\platform-smoke\android",
    [switch]$LocalFrameAnalytics
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir

if ($ShowHelp) {
    Write-Host "Android device evidence pack (background + permissions)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\android-device-evidence-pack.ps1"
    Write-Host "  .\scripts\android-device-evidence-pack.ps1 -LocalFrameAnalytics"
    Write-Host ""
    Write-Host "Steps:"
    Write-Host "  1) android-video-background-smoke.ps1 -RunInstallIfDevicePresent"
    Write-Host "  2) android-permissions-revoke-recover-smoke.ps1 -FailIfNoDevice"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Both steps PASS"
    Write-Host "  1  Any step FAIL/PARTIAL"
    exit 0
}

$backgroundScript = Join-Path $scriptDir "android-video-background-smoke.ps1"
$permissionsScript = Join-Path $scriptDir "android-permissions-revoke-recover-smoke.ps1"

if (-not (Test-Path $backgroundScript)) { throw "Missing script: $backgroundScript" }
if (-not (Test-Path $permissionsScript)) { throw "Missing script: $permissionsScript" }

$bgArgs = @{
    OutputDir = $OutputDir
    RunInstallIfDevicePresent = $true
}
if ($LocalFrameAnalytics) {
    $bgArgs["LocalFrameAnalytics"] = $true
}

Write-Host "=== Step 1/2: Android background/device smoke ===" -ForegroundColor Cyan
& $backgroundScript @bgArgs
$bgExit = $LASTEXITCODE

Write-Host "=== Step 2/2: Android permissions revoke/recover smoke ===" -ForegroundColor Cyan
& $permissionsScript -OutputDir $OutputDir -FailIfNoDevice
$permExit = $LASTEXITCODE

if ($bgExit -eq 0 -and $permExit -eq 0) {
    Write-Host "Android device evidence pack: PASS" -ForegroundColor Green
    exit 0
}

Write-Host ("Android device evidence pack: FAIL (background={0}, permissions={1})" -f $bgExit, $permExit) -ForegroundColor Red
exit 1

