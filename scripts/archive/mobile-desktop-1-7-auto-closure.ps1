[Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSAvoidUsingPlainTextForPassword', '')]
[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$OutputDir = "diagnostics\platform-smoke\closure-1-7",
    [bool]$CompileOnly = $true,
    [bool]$LocalFrameAnalytics = $true,
    [string]$DesktopBaseUrl = "",
    [string]$DesktopUsername = "admin",
    [string]$DesktopPassword = "",
    [int]$DesktopDurationMinutes = 20,
    [string[]]$DesktopPlaylistUrls = @()
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$resolvedOutputDir = Join-Path $projectRoot $OutputDir

$androidSmokeScript = Join-Path $scriptDir "android-video-background-smoke.ps1"
$androidPermScript = Join-Path $scriptDir "android-permissions-revoke-recover-smoke.ps1"
$desktopSmokeScript = Join-Path $scriptDir "desktop-video-event-longrun-smoke.ps1"

if ($ShowHelp) {
    Write-Host "1.7 mobile/desktop auto-closure orchestrator"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\mobile-desktop-1-7-auto-closure.ps1"
    Write-Host "  .\scripts\mobile-desktop-1-7-auto-closure.ps1 -CompileOnly"
    Write-Host "  .\scripts\mobile-desktop-1-7-auto-closure.ps1 -DesktopBaseUrl http://localhost:8080 -DesktopPlaylistUrls http://localhost:8080/api/v1/hls/cam-1/playlist.m3u8"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -OutputDir                 Repo-relative report folder"
    Write-Host "  -CompileOnly               Use compile-only mode for Android/Desktop smoke"
    Write-Host "  -LocalFrameAnalytics       Pass local-frame analytics flag to Android smoke"
    Write-Host "  -DesktopBaseUrl            Base URL for runtime desktop long-run smoke"
    Write-Host "  -DesktopUsername           Desktop runtime smoke username"
    Write-Host "  -DesktopPassword           Desktop runtime smoke password"
    Write-Host "  -DesktopDurationMinutes    Runtime long-run duration in minutes"
    Write-Host "  -DesktopPlaylistUrls       HLS playlists for desktop runtime long-run"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  PASS"
    Write-Host "  1  FAIL"
    Write-Host "  3  PARTIAL (expected when no devices/runtime streams)"
    exit 0
}

if (-not (Test-Path $resolvedOutputDir)) {
    New-Item -ItemType Directory -Path $resolvedOutputDir -Force | Out-Null
}

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Command
    )
    try {
        $null = & $Command
        return [PSCustomObject]@{ name = $Name; exitCode = $LASTEXITCODE; status = $(if ($LASTEXITCODE -eq 0) { "PASS" } elseif ($LASTEXITCODE -eq 3) { "PARTIAL" } else { "FAIL" }) }
    } catch {
        return [PSCustomObject]@{ name = $Name; exitCode = 1; status = "FAIL"; error = $_.Exception.Message }
    }
}

$androidSmokeSplat = @{}
if ($CompileOnly) { $androidSmokeSplat.CompileOnly = $true }
if ($LocalFrameAnalytics) { $androidSmokeSplat.LocalFrameAnalytics = $true }

$desktopSmokeSplat = @{}
if ($CompileOnly) {
    $desktopSmokeSplat.CompileAndTestOnly = $true
} elseif ($DesktopBaseUrl -and $DesktopPlaylistUrls.Count -gt 0) {
    $desktopSmokeSplat.BaseUrl = $DesktopBaseUrl
    $desktopSmokeSplat.Username = $DesktopUsername
    $desktopSmokeSplat.Password = $DesktopPassword
    $desktopSmokeSplat.DurationMinutes = $DesktopDurationMinutes
    $desktopSmokeSplat.PlaylistUrls = $DesktopPlaylistUrls
} else {
    # Keep behavior explicit when runtime inputs are missing.
    $desktopSmokeSplat.CompileAndTestOnly = $true
}

$results = @()
$results += Invoke-Step -Name "android_video_background_smoke" -Command { & $androidSmokeScript @androidSmokeSplat }
$results += Invoke-Step -Name "android_permissions_revoke_recover_smoke" -Command { & $androidPermScript }
$results += Invoke-Step -Name "desktop_video_event_longrun_smoke" -Command { & $desktopSmokeScript @desktopSmokeSplat }

$overall = "PASS"
if ($results.status -contains "FAIL") {
    $overall = "FAIL"
} elseif ($results.status -contains "PARTIAL") {
    $overall = "PARTIAL"
}

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$jsonPath = Join-Path $resolvedOutputDir ("mobile-desktop-1-7-auto-closure-{0}.json" -f $runId)
$mdPath = Join-Path $resolvedOutputDir ("mobile-desktop-1-7-auto-closure-{0}.md" -f $runId)

$payload = [ordered]@{
    runId = $runId
    timestampUtc = [DateTime]::UtcNow.ToString("o")
    overall = $overall
    compileOnly = [bool]$CompileOnly
    localFrameAnalytics = [bool]$LocalFrameAnalytics
    results = $results
}

$payload | ConvertTo-Json -Depth 6 | Set-Content -Path $jsonPath -Encoding UTF8

$lines = @(
    "# Mobile/Desktop 1.7 Auto-Closure Report",
    "",
    "- Run ID: $runId",
    "- Overall: **$overall**",
    "- CompileOnly: **$CompileOnly**",
    "- LocalFrameAnalytics: **$LocalFrameAnalytics**",
    "",
    "## Step Results",
    "",
    "| Step | Status | Exit Code |",
    "|---|---|---:|"
)

foreach ($r in $results) {
    $lines += "| $($r.name) | $($r.status) | $($r.exitCode) |"
}

$lines -join [Environment]::NewLine | Set-Content -Path $mdPath -Encoding UTF8

Write-Host ("1.7 auto-closure summary: {0}" -f $mdPath) -ForegroundColor Green
Write-Host ("1.7 auto-closure json: {0}" -f $jsonPath) -ForegroundColor Green

if ($overall -eq "FAIL") { exit 1 }
if ($overall -eq "PARTIAL") { exit 3 }
exit 0
