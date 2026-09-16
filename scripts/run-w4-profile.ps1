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
    [switch]$AndroidLocalFrameAnalytics
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Run W4 gate by profile (wrapper around w4-mvp-platform-and-gate.ps1)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-w4-profile.ps1 -RunProfile local"
    Write-Host "  .\scripts\run-w4-profile.ps1 -RunProfile strict"
    Write-Host "  .\scripts\run-w4-profile.ps1 -RunProfile local -AndroidLocalFrameAnalytics"
    Write-Host "  .\scripts\run-w4-profile.ps1 -RunProfile staging -IncludeDesktopSmoke -DesktopSmokeBaseUrl http://localhost:8080 -DesktopSmokePlaylistUrls http://localhost:8080/api/v1/hls/cam-1/playlist.m3u8"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -RunProfile local|staging|strict"
    Write-Host "  -IncludeDesktopSmoke  -DesktopSmokeBaseUrl  -DesktopSmokeUsername  -DesktopSmokePassword  -DesktopSmokePlaylistUrls"
    Write-Host "  -NoPlatformSmokeCompileOnlyGate  Forwarded to w4-mvp-platform-and-gate.ps1"
    Write-Host "  -AndroidLocalFrameAnalytics  Forwarded to w4-mvp-platform-and-gate.ps1 (Gradle -P + Android smoke)"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  Same as .\scripts\w4-mvp-platform-and-gate.ps1 (0 / 1 / 3)."
    Write-Host ""
    Write-Host "Output:"
    Write-Host "  Prints paths to latest w4-mvp-platform-gate-*.md|.json under diagnostics/platform-smoke."
    exit 0
}
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$w4Script = Join-Path $scriptDir "w4-mvp-platform-and-gate.ps1"
$reportDir = Join-Path $projectRoot "diagnostics\platform-smoke"

if (-not (Test-Path $w4Script)) {
    Write-Host "w4 script not found: $w4Script" -ForegroundColor Red
    exit 1
}

$w4Args = @{
    RunProfile = $RunProfile
}

if ($IncludeDesktopSmoke) {
    $w4Args["IncludeDesktopSmoke"] = $true
}
if (-not [string]::IsNullOrWhiteSpace($DesktopSmokeBaseUrl)) {
    $w4Args["DesktopSmokeBaseUrl"] = $DesktopSmokeBaseUrl
}
if (-not [string]::IsNullOrWhiteSpace($DesktopSmokeUsername)) {
    $w4Args["DesktopSmokeUsername"] = $DesktopSmokeUsername
}
if (-not [string]::IsNullOrWhiteSpace($DesktopSmokePassword)) {
    $w4Args["DesktopSmokePassword"] = $DesktopSmokePassword
}
if ($DesktopSmokePlaylistUrls.Count -gt 0) {
    $w4Args["DesktopSmokePlaylistUrls"] = $DesktopSmokePlaylistUrls
}
if ($NoPlatformSmokeCompileOnlyGate) {
    $w4Args["NoPlatformSmokeCompileOnlyGate"] = $true
}
if ($AndroidLocalFrameAnalytics) {
    $w4Args["AndroidLocalFrameAnalytics"] = $true
}

Write-Host ("==> Running W4 profile: {0}" -f $RunProfile) -ForegroundColor Cyan
& $w4Script @w4Args
$exitCode = $LASTEXITCODE

$latestMd = $null
$latestJson = $null
if (Test-Path $reportDir) {
    $latestMd = Get-ChildItem -Path $reportDir -File -Filter "w4-mvp-platform-gate-*.md" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
    $latestJson = Get-ChildItem -Path $reportDir -File -Filter "w4-mvp-platform-gate-*.json" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
}

Write-Host ""
Write-Host ("W4 profile completed with exit code: {0}" -f $exitCode) -ForegroundColor Yellow
if ($latestMd) {
    Write-Host ("Latest W4 report: {0}" -f $latestMd.FullName) -ForegroundColor Green
}
if ($latestJson) {
    Write-Host ("Latest W4 json: {0}" -f $latestJson.FullName) -ForegroundColor Green
}

exit $exitCode
