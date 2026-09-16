# Local API restart hook for ONVIF resilience (-RestartCommand) and similar automation.
# Set IPCSS_RESTART_API_COMMAND or pass -Command with a PowerShell expression that restarts your API and returns.

[Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSAvoidUsingInvokeExpression', '')]
param(
    [switch]$ShowHelp,
    [string]$Command = "",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$gradlew = Join-Path $projectRoot "gradlew.bat"

if ($ShowHelp) {
    Write-Host "Restart local API (operator-defined command)"
    Write-Host ""
    Write-Host "Usage (from repo root; configure restart for your environment):"
    Write-Host '  $env:IPCSS_RESTART_API_COMMAND = "& .\gradlew.bat :server:api:run"'
    Write-Host "  .\scripts\restart-api-local.ps1"
    Write-Host '  .\scripts\restart-api-local.ps1 -Command "& .\gradlew.bat :server:api:run"'
    Write-Host "  .\scripts\restart-api-local.ps1 -DryRun"
    Write-Host ("  gradlew path on this machine: {0}" -f $gradlew)
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Command <string>  PowerShell expression to execute (overrides IPCSS_RESTART_API_COMMAND)."
    Write-Host "  -DryRun  Print the resolved command and exit 0 without executing."
    Write-Host ""
    Write-Host "Environment:"
    Write-Host "  IPCSS_RESTART_API_COMMAND  Default command when -Command is omitted."
    Write-Host ""
    Write-Host "Notes:"
    Write-Host "  - Typical local run: .\gradlew.bat :server:api:run (see README.md). Your restart may need stop + start."
    Write-Host "  - Used by onvif-events-resilience-verification.ps1 -RestartCommand .\scripts\restart-api-local.ps1"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  -ShowHelp, -DryRun, or command completed (last exit code from Invoke-Expression)"
    Write-Host "  1  No command configured (set env or pass -Command)"
    exit 0
}

$resolved = if (-not [string]::IsNullOrWhiteSpace($Command)) {
    $Command
} else {
    $env:IPCSS_RESTART_API_COMMAND
}

if ([string]::IsNullOrWhiteSpace($resolved)) {
    Write-Host "restart-api-local.ps1: no restart command." -ForegroundColor Red
    Write-Host "Set IPCSS_RESTART_API_COMMAND or pass -Command. Use -ShowHelp for examples." -ForegroundColor Yellow
    exit 1
}

if ($DryRun) {
    Write-Host "DryRun (would execute):" -ForegroundColor Cyan
    Write-Host $resolved
    exit 0
}

Write-Host "==> restart-api-local: executing configured command" -ForegroundColor Cyan
Invoke-Expression $resolved
exit $LASTEXITCODE
