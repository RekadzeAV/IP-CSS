# Platform matrix wrapper for 1.8 long-run smoke.
# Runs multiple scenarios (desktop/android/self-hosted) and generates a summary report.
#
# Usage:
#   .\scripts\video-runtime-platform-matrix.ps1 `
#     -ConfigPath "config\video-runtime-matrix.local.json" `
#     -OutputDir "diagnostics\video-longrun-matrix"

[Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSAvoidUsingPlainTextForPassword', '')]
param(
    [switch]$ShowHelp,
    [string]$ConfigPath = "config\video-runtime-matrix.local.json",
    [string]$OutputDir = "diagnostics\video-longrun-matrix",
    [switch]$StopOnFirstFailure
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Video runtime platform matrix (multiple long-run scenarios + summary)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\video-runtime-platform-matrix.ps1 -ConfigPath config\video-runtime-matrix.local.json -OutputDir diagnostics\video-longrun-matrix"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -ConfigPath  JSON matrix (copy from config/video-runtime-matrix.example.json)."
    Write-Host "  -OutputDir  Root output folder; each run creates <timestamp>/ with per-scenario dirs."
    Write-Host "  Scenario fields: name/platform/enabled, playlistUrls[], cameraIds[], autoDiscoverCameraIds, autoStartStreams, attemptLogin, durationMinutes, intervalSec"
    Write-Host "  -StopOnFirstFailure  Abort remaining scenarios after first long-run failure."
    Write-Host ""
    Write-Host "Steps:"
    Write-Host "  Invokes scripts/video-runtime-longrun-smoke.ps1 per enabled scenario; writes video-runtime-matrix-summary.md under the run directory."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  All scenarios passed"
    Write-Host "  1  One or more scenarios failed (or config/script missing)"
    exit 0
}

function Resolve-ProjectPath {
    param([string]$PathValue)
    $scriptDir = $PSScriptRoot
    $projectRoot = Split-Path -Parent $scriptDir
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

function ConvertTo-IntOrDefault {
    param([object]$Value, [int]$Default)
    $tmp = 0
    if ($null -ne $Value -and [int]::TryParse($Value.ToString(), [ref]$tmp)) {
        return $tmp
    }
    return $Default
}

function Get-OptionalPropertyValue {
    param(
        [object]$Object,
        [string]$PropertyName
    )
    if ($null -eq $Object) { return $null }
    $prop = $Object.PSObject.Properties[$PropertyName]
    if ($null -eq $prop) { return $null }
    return $prop.Value
}

$resolvedConfig = Resolve-ProjectPath -PathValue $ConfigPath
$resolvedOutputRoot = Resolve-ProjectPath -PathValue $OutputDir
$longRunScript = Resolve-ProjectPath -PathValue "scripts\video-runtime-longrun-smoke.ps1"

if (-not (Test-Path $resolvedConfig)) {
    throw "Config not found: $resolvedConfig. Create it from config\video-runtime-matrix.example.json"
}
if (-not (Test-Path $longRunScript)) {
    throw "Script not found: $longRunScript"
}

$cfg = Get-Content $resolvedConfig -Raw | ConvertFrom-Json
$scenarios = @($cfg.scenarios | Where-Object { $_.enabled -ne $false })
if ($scenarios.Count -eq 0) {
    throw "No enabled scenarios in config: $resolvedConfig"
}

if (-not (Test-Path $resolvedOutputRoot)) {
    New-Item -ItemType Directory -Path $resolvedOutputRoot -Force | Out-Null
}

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$runDir = Join-Path $resolvedOutputRoot $runId
New-Item -ItemType Directory -Path $runDir -Force | Out-Null

$defaults = $cfg.defaults
$results = @()

foreach ($s in $scenarios) {
    $name = if ($s.name) { [string]$s.name } else { "scenario" }
    $safeName = ($name -replace "[^0-9A-Za-z\-_\.]", "_")
    $scenarioDir = Join-Path $runDir $safeName
    New-Item -ItemType Directory -Path $scenarioDir -Force | Out-Null

    $scenarioBaseUrl = Get-OptionalPropertyValue -Object $s -PropertyName "baseUrl"
    $scenarioUsername = Get-OptionalPropertyValue -Object $s -PropertyName "username"
    $scenarioPassword = Get-OptionalPropertyValue -Object $s -PropertyName "password"
    $scenarioDuration = Get-OptionalPropertyValue -Object $s -PropertyName "durationMinutes"
    $scenarioInterval = Get-OptionalPropertyValue -Object $s -PropertyName "intervalSec"
    $scenarioHealthPath = Get-OptionalPropertyValue -Object $s -PropertyName "healthPath"

    $defaultsBaseUrl = Get-OptionalPropertyValue -Object $defaults -PropertyName "baseUrl"
    $defaultsUsername = Get-OptionalPropertyValue -Object $defaults -PropertyName "username"
    $defaultsPassword = Get-OptionalPropertyValue -Object $defaults -PropertyName "password"
    $defaultsDuration = Get-OptionalPropertyValue -Object $defaults -PropertyName "durationMinutes"
    $defaultsInterval = Get-OptionalPropertyValue -Object $defaults -PropertyName "intervalSec"
    $defaultsHealthPath = Get-OptionalPropertyValue -Object $defaults -PropertyName "healthPath"

    $baseUrl = if ($scenarioBaseUrl) { [string]$scenarioBaseUrl } elseif ($defaultsBaseUrl) { [string]$defaultsBaseUrl } else { "http://localhost:8080" }
    $username = if ($scenarioUsername) { [string]$scenarioUsername } elseif ($defaultsUsername) { [string]$defaultsUsername } else { "admin" }
    $password = if ($scenarioPassword) { [string]$scenarioPassword } elseif ($defaultsPassword) { [string]$defaultsPassword } else { "admin" }
    $duration = ConvertTo-IntOrDefault -Value $scenarioDuration -Default (ConvertTo-IntOrDefault -Value $defaultsDuration -Default 30)
    $interval = ConvertTo-IntOrDefault -Value $scenarioInterval -Default (ConvertTo-IntOrDefault -Value $defaultsInterval -Default 30)
    $healthPath = if ($scenarioHealthPath) { [string]$scenarioHealthPath } elseif ($defaultsHealthPath) { [string]$defaultsHealthPath } else { "/api/v1/health/ready" }
    $scenarioPlaylistUrls = Get-OptionalPropertyValue -Object $s -PropertyName "playlistUrls"
    $playlistUrls = @()
    if ($null -ne $scenarioPlaylistUrls) { $playlistUrls = @($scenarioPlaylistUrls) }
    $scenarioCameraIds = Get-OptionalPropertyValue -Object $s -PropertyName "cameraIds"
    $cameraIds = @()
    if ($null -ne $scenarioCameraIds) { $cameraIds = @($scenarioCameraIds) }
    $autoDiscoverCameraIds = [bool](Get-OptionalPropertyValue -Object $s -PropertyName "autoDiscoverCameraIds")
    $autoStartStreams = [bool](Get-OptionalPropertyValue -Object $s -PropertyName "autoStartStreams")
    $attemptLogin = [bool](Get-OptionalPropertyValue -Object $s -PropertyName "attemptLogin")

    Write-Host ""
    Write-Host "=== Scenario: $name ===" -ForegroundColor Cyan
    Write-Host "BaseUrl=$baseUrl Duration=$duration Interval=$interval" -ForegroundColor Gray

    $code = 1
    try {
        $invokeArgs = @{
            BaseUrl = $baseUrl
            Username = $username
            Password = $password
            DurationMinutes = $duration
            IntervalSec = $interval
            HealthPath = $healthPath
            PlaylistUrls = $playlistUrls
            CameraIds = $cameraIds
            AutoDiscoverCameraIds = $autoDiscoverCameraIds
            AutoStartStreams = $autoStartStreams
            AttemptLogin = $attemptLogin
            OutputDir = $scenarioDir
        }
        & $longRunScript @invokeArgs
        $code = $LASTEXITCODE
    } catch {
        Write-Host "Scenario failed with exception: $($_.Exception.Message)" -ForegroundColor Red
        $code = 1
    }
    $latestReport = Get-ChildItem -Path $scenarioDir -Filter "video-longrun-report-*.md" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    $platform = if ($s.platform) { [string]$s.platform } else { "n/a" }
    $reportPath = if ($latestReport) { $latestReport.FullName } else { "" }
    $results += [PSCustomObject]@{
        Name = $name
        Platform = $platform
        BaseUrl = $baseUrl
        ExitCode = $code
        Passed = ($code -eq 0)
        Report = $reportPath
    }

    if ($code -ne 0 -and $StopOnFirstFailure) {
        break
    }
}

$passed = @($results | Where-Object { $_.Passed }).Count
$total = $results.Count
$failed = $total - $passed
$overallPass = $failed -eq 0
$overallLabel = if ($overallPass) { "PASS" } else { "FAIL" }

$summaryPath = Join-Path $runDir "video-runtime-matrix-summary.md"
$lines = @()
$lines += "# Video Runtime Platform Matrix Summary"
$lines += ""
$lines += "- Generated at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$lines += "- Config: $resolvedConfig"
$lines += "- Run dir: $runDir"
$lines += "- Passed scenarios: **$passed / $total**"
$lines += "- Overall: $overallLabel"
$lines += ""
$lines += "| Scenario | Platform | Base URL | Exit | Status | Report |"
$lines += "|---|---|---|---|---|---|"
foreach ($r in $results) {
    $st = if ($r.Passed) { "OK" } else { "FAIL" }
    $rep = if ([string]::IsNullOrWhiteSpace($r.Report)) { "" } else { $r.Report.Replace("|", "/") }
    $lines += "| $($r.Name) | $($r.Platform) | $($r.BaseUrl) | $($r.ExitCode) | $st | $rep |"
}

Set-Content -Path $summaryPath -Value ($lines -join [Environment]::NewLine) -Encoding UTF8
Write-Host ""
Write-Host "Matrix summary: $summaryPath" -ForegroundColor Cyan

if (-not $overallPass) {
    exit 1
}
exit 0
