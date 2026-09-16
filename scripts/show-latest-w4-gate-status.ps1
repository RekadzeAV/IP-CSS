[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$ReportDir = "diagnostics\platform-smoke",
    [switch]$AsJson,
    [switch]$FailOnPartial,
    [switch]$FailOnFail
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Show latest W4 gate status (reads newest w4-mvp-platform-gate-*.json)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\show-latest-w4-gate-status.ps1"
    Write-Host "  .\scripts\show-latest-w4-gate-status.ps1 -AsJson"
    Write-Host "  .\scripts\show-latest-w4-gate-status.ps1 -FailOnPartial"
    Write-Host "  .\scripts\show-latest-w4-gate-status.ps1 -FailOnFail"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -ReportDir  Root-relative folder containing W4 json reports (default diagnostics\platform-smoke)."
    Write-Host "  -AsJson  Print a small JSON summary to stdout."
    Write-Host "  -FailOnFail  Exit 2 when overall status is FAIL."
    Write-Host "  -FailOnPartial  Exit 3 when overall status is PARTIAL."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  OK (or policy flags not triggered)"
    Write-Host "  1  Report directory or latest json missing"
    Write-Host "  2  With -FailOnFail when overall is FAIL"
    Write-Host "  3  With -FailOnPartial when overall is PARTIAL"
    exit 0
}
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$resolvedReportDir = Join-Path $projectRoot $ReportDir

if (-not (Test-Path $resolvedReportDir)) {
    Write-Host "W4 report directory not found: $resolvedReportDir" -ForegroundColor Red
    exit 1
}

$latestJson = Get-ChildItem -Path $resolvedReportDir -File -Filter "w4-mvp-platform-gate-*.json" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTimeUtc -Descending |
    Select-Object -First 1

if ($null -eq $latestJson) {
    Write-Host "No W4 gate json reports found in: $resolvedReportDir" -ForegroundColor Red
    exit 1
}

$data = Get-Content -Path $latestJson.FullName -Raw | ConvertFrom-Json
$overall = [string]$data.overall
$steps = @()
if ($null -ne $data.steps) {
    $steps = @($data.steps)
}
$failedSteps = @($steps | Where-Object { $_.status -eq "FAIL" })
$partialSteps = @($steps | Where-Object { $_.status -eq "PARTIAL" })

if ($AsJson) {
    $result = [PSCustomObject]@{
        report = $latestJson.FullName
        runId = $data.runId
        runProfile = $data.runProfile
        full = $data.full
        platformSmokeCompileOnlyGate = $data.platformSmokeCompileOnlyGate
        androidLocalFrameAnalytics = $data.androidLocalFrameAnalytics
        overall = $overall
        failedSteps = @($failedSteps | ForEach-Object { $_.step })
        partialSteps = @($partialSteps | ForEach-Object { $_.step })
    }
    $result | ConvertTo-Json -Depth 6 | Out-Host
} else {
    Write-Host ("W4 latest report: {0}" -f $latestJson.FullName) -ForegroundColor Green
    Write-Host ("Run ID: {0}" -f $data.runId) -ForegroundColor Cyan
    if ($null -ne $data.runProfile) {
        Write-Host ("Run profile: {0}" -f $data.runProfile) -ForegroundColor DarkGray
    }
    if ($null -ne $data.full) {
        Write-Host ("Full: {0}" -f $data.full) -ForegroundColor DarkGray
    }
    if ($null -ne $data.platformSmokeCompileOnlyGate) {
        Write-Host ("Platform smoke compile-only gate: {0}" -f $data.platformSmokeCompileOnlyGate) -ForegroundColor DarkGray
    }
    if ($null -ne $data.androidLocalFrameAnalytics) {
        Write-Host ("Android local frame analytics: {0}" -f $data.androidLocalFrameAnalytics) -ForegroundColor DarkGray
    }
    Write-Host ("Overall: {0}" -f $overall) -ForegroundColor Cyan

    if ($failedSteps.Count -gt 0) {
        Write-Host "Failed steps:" -ForegroundColor Red
        foreach ($s in $failedSteps) {
            Write-Host ("- {0}: {1}" -f $s.step, $s.details) -ForegroundColor Red
        }
    }
    if ($partialSteps.Count -gt 0) {
        Write-Host "Partial steps:" -ForegroundColor Yellow
        foreach ($s in $partialSteps) {
            Write-Host ("- {0}: {1}" -f $s.step, $s.details) -ForegroundColor Yellow
        }
    }
    if ($failedSteps.Count -eq 0 -and $partialSteps.Count -eq 0) {
        Write-Host "All steps PASS." -ForegroundColor Green
    }
}

if ($FailOnFail -and $overall -eq "FAIL") { exit 2 }
if ($FailOnPartial -and $overall -eq "PARTIAL") { exit 3 }
exit 0
