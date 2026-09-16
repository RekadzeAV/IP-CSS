# Debug build: BuildConfig LOCAL_FRAME_ANALYTICS (see android/app/build.gradle.kts).
[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$OutputDir = "diagnostics\platform-smoke\android",
    [switch]$RunInstallIfDevicePresent,
    [switch]$CompileOnly,
    [switch]$LocalFrameAnalytics
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$gradlew = Join-Path $projectRoot "gradlew.bat"
$resolvedOutputDir = Join-Path $projectRoot $OutputDir

if ($ShowHelp) {
    Write-Host "Android video/background smoke (compile/assemble; optional install)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\android-video-background-smoke.ps1"
    Write-Host "  .\scripts\android-video-background-smoke.ps1 -CompileOnly"
    Write-Host "  .\scripts\android-video-background-smoke.ps1 -RunInstallIfDevicePresent"
    Write-Host "  .\scripts\android-video-background-smoke.ps1 -LocalFrameAnalytics"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -OutputDir  Repo-relative folder for android-video-background-smoke-<run-id>.md|.json"
    Write-Host "  -RunInstallIfDevicePresent  Run installDebug when adb sees a device"
    Write-Host "  -CompileOnly  PASS when compile+assemble succeed; missing device does not yield PARTIAL"
    Write-Host "  -LocalFrameAnalytics  Gradle -Pipcss.localFrameAnalytics=true (debug local-frame analytics; см. android/app/build.gradle.kts)"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Overall PASS"
    Write-Host "  1  Overall FAIL"
    Write-Host "  3  Overall PARTIAL (e.g. no adb/device when not -CompileOnly, or install failed when not -CompileOnly)"
    exit 0
}

if (-not (Test-Path $gradlew)) {
    Write-Host "gradlew.bat not found: $gradlew" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $resolvedOutputDir)) {
    New-Item -ItemType Directory -Path $resolvedOutputDir -Force | Out-Null
}

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryJson = Join-Path $resolvedOutputDir ("android-video-background-smoke-{0}.json" -f $runId)
$summaryMd = Join-Path $resolvedOutputDir ("android-video-background-smoke-{0}.md" -f $runId)

$gradleLocalAnalyticsArgs = @()
if ($LocalFrameAnalytics) {
    $gradleLocalAnalyticsArgs += "-Pipcss.localFrameAnalytics=true"
}

function Invoke-GradleTask([string]$task, [string[]]$ExtraGradleArgs = @()) {
    $allArgs = @()
    $allArgs += $ExtraGradleArgs
    $allArgs += $task
    $allArgs += "--no-daemon"
    $display = ($ExtraGradleArgs + $task) -join " "
    Write-Host "==> gradlew $display" -ForegroundColor Cyan
    & $gradlew @allArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle task failed: $task (exit $LASTEXITCODE)"
    }
}

function Get-AdbDeviceCount {
    $candidates = New-Object System.Collections.Generic.List[string]
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($null -ne $adb) {
        if (-not [string]::IsNullOrWhiteSpace($adb.Path)) { $candidates.Add($adb.Path) | Out-Null }
        if (-not [string]::IsNullOrWhiteSpace($adb.Source)) { $candidates.Add($adb.Source) | Out-Null }
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) {
        $candidates.Add((Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe")) | Out-Null
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
        $candidates.Add((Join-Path $env:ANDROID_HOME "platform-tools\adb.exe")) | Out-Null
    }
    $candidates.Add((Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe")) | Out-Null
    $exe = $candidates | Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and (Test-Path $_) } | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($exe)) { return -1 }

    & $exe start-server *> $null
    $output = @()
    try {
        $output = & $exe devices 2>$null
    } catch {
        return -1
    }
    $count = 0
    foreach ($line in $output) {
        if ($line -match "^\S+\s+device$") {
            $count++
        }
    }
    return $count
}

$compileStatus = "PASS"
# Skipped until assembleDebug actually runs (avoid PASS in reports when compile failed first).
$assembleStatus = "NOT_RUN"
$deviceStatus = "NOT_RUN"
$installStatus = "NOT_RUN"
$overall = "PASS"
$notes = New-Object System.Collections.Generic.List[string]

try {
    Invoke-GradleTask ":android:app:compileDebugKotlin" -ExtraGradleArgs $gradleLocalAnalyticsArgs
} catch {
    $compileStatus = "FAIL"
    $overall = "FAIL"
    $notes.Add($_.Exception.Message) | Out-Null
}

if ($overall -eq "PASS") {
    try {
        Invoke-GradleTask ":android:app:assembleDebug" -ExtraGradleArgs $gradleLocalAnalyticsArgs
        $assembleStatus = "PASS"
    } catch {
        $assembleStatus = "FAIL"
        $overall = "FAIL"
        $notes.Add($_.Exception.Message) | Out-Null
    }
}

$deviceCount = Get-AdbDeviceCount
if ($deviceCount -lt 0) {
    $deviceStatus = "NOT_RUN"
    $notes.Add("adb not found in PATH; skipped device smoke.") | Out-Null
} elseif ($deviceCount -eq 0) {
    $deviceStatus = "NOT_RUN"
    $notes.Add("No connected Android devices/emulators; install smoke skipped.") | Out-Null
} else {
    $deviceStatus = "PASS"
    if ($RunInstallIfDevicePresent) {
        try {
            Invoke-GradleTask ":android:app:installDebug" -ExtraGradleArgs $gradleLocalAnalyticsArgs
            $installStatus = "PASS"
        } catch {
            $installStatus = "FAIL"
            if ((-not $CompileOnly) -and ($overall -ne "FAIL")) { $overall = "PARTIAL" }
            $notes.Add($_.Exception.Message) | Out-Null
        }
    }
}

if (-not $CompileOnly) {
    if ($overall -ne "FAIL" -and ($deviceStatus -eq "NOT_RUN" -or $installStatus -eq "FAIL")) {
        $overall = "PARTIAL"
    }
} elseif ($overall -eq "PASS") {
    $notes.Add("CompileOnly: overall reflects compile+assemble only (device/install not required for PASS).") | Out-Null
}

$summary = [ordered]@{
    runId = $runId
    timestampUtc = [DateTime]::UtcNow.ToString("o")
    localFrameAnalyticsGradleProperty = [bool]$LocalFrameAnalytics
    statuses = @{
        overall = $overall
        compileDebugKotlin = $compileStatus
        assembleDebug = $assembleStatus
        deviceCheck = $deviceStatus
        installDebug = $installStatus
    }
    metrics = @{
        adbDeviceCount = $deviceCount
    }
    notes = @($notes)
}

$summary | ConvertTo-Json -Depth 6 | Set-Content -Path $summaryJson -Encoding UTF8

$adbDeviceCountMd = if ($deviceCount -lt 0) { "n/a (adb unavailable)" } else { [string]$deviceCount }

$localFrameAnalyticsMd = if ($LocalFrameAnalytics) { "yes (-Pipcss.localFrameAnalytics=true)" } else { "no" }
$lines = @(
    "# Android Video/Background Smoke Report",
    "",
    "- Run ID: $runId",
    "- Local frame analytics build flag: **$localFrameAnalyticsMd**",
    "- Overall: **$overall**",
    "- compileDebugKotlin: **$compileStatus**",
    "- assembleDebug: **$assembleStatus**",
    "- deviceCheck: **$deviceStatus**",
    "- installDebug: **$installStatus**",
    "- adb devices: **$adbDeviceCountMd**",
    "",
    "## Notes"
)
if ($notes.Count -eq 0) {
    $lines += "- none"
} else {
    foreach ($n in $notes) { $lines += "- $n" }
}

$lines -join [Environment]::NewLine | Set-Content -Path $summaryMd -Encoding UTF8

Write-Host ("Android smoke summary: {0}" -f $summaryMd) -ForegroundColor Green
Write-Host ("Android smoke json: {0}" -f $summaryJson) -ForegroundColor Green

if ($overall -eq "FAIL") { exit 1 }
if ($overall -eq "PARTIAL") { exit 3 }
exit 0
