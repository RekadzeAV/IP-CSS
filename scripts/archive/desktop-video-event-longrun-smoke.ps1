[Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSAvoidUsingPlainTextForPassword', '')]
[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$OutputDir = "diagnostics\platform-smoke\desktop",
    [string]$BaseUrl = "",
    [string]$Username = "admin",
    [string]$Password = "",
    [int]$DurationMinutes = 20,
    [string[]]$PlaylistUrls = @(),
    [switch]$CompileAndTestOnly
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$gradlew = Join-Path $projectRoot "gradlew.bat"
$longRunScript = Join-Path $scriptDir "video-runtime-longrun-smoke.ps1"
$resolvedOutputDir = Join-Path $projectRoot $OutputDir

if ($ShowHelp) {
    Write-Host "Desktop video/event smoke (compile + tests; optional long-run)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\desktop-video-event-longrun-smoke.ps1"
    Write-Host "  .\scripts\desktop-video-event-longrun-smoke.ps1 -CompileAndTestOnly"
    Write-Host "  .\scripts\desktop-video-event-longrun-smoke.ps1 -BaseUrl http://localhost:8080 -Username admin -Password <pwd> -PlaylistUrls http://localhost:8080/api/v1/hls/cam-1/playlist.m3u8"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -OutputDir  Repo-relative folder for desktop-video-event-smoke-<run-id>.md|.json"
    Write-Host "  -BaseUrl -Username -Password -PlaylistUrls -DurationMinutes  Passed to video-runtime-longrun-smoke.ps1 when URL set"
    Write-Host "  -CompileAndTestOnly  PASS when compile+JVM tests succeed; skipped long-run does not yield PARTIAL"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Overall PASS"
    Write-Host "  1  Overall FAIL"
    Write-Host "  3  Overall PARTIAL when not -CompileAndTestOnly (e.g. long-run skipped, or child script exit 3)"
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
$summaryJson = Join-Path $resolvedOutputDir ("desktop-video-event-smoke-{0}.json" -f $runId)
$summaryMd = Join-Path $resolvedOutputDir ("desktop-video-event-smoke-{0}.md" -f $runId)
$sharedClasspathSnapshotDir = Join-Path $projectRoot "shared\build\kotlin\compileKotlinDesktop\classpath-snapshot"

function Invoke-GradleTask([string[]]$tasks) {
    $maxAttempts = 2
    for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
        Write-Host ("==> gradlew {0} (attempt {1}/{2})" -f ($tasks -join " "), $attempt, $maxAttempts) -ForegroundColor Cyan
        & $gradlew @tasks --no-daemon
        if ($LASTEXITCODE -eq 0) {
            return
        }

        if ($attempt -lt $maxAttempts) {
            Write-Host "Gradle task failed. Stopping daemons and retrying..." -ForegroundColor Yellow
            & $gradlew --stop | Out-Host
            if (Test-Path $sharedClasspathSnapshotDir) {
                Remove-Item -Path $sharedClasspathSnapshotDir -Recurse -Force -ErrorAction SilentlyContinue
            }
        }
    }

    throw "Gradle tasks failed: $($tasks -join ' ') after $maxAttempts attempts"
}

$desktopCompileStatus = "PASS"
# Skipped until desktop JVM tests actually run (avoid PASS when compile failed first).
$desktopTestStatus = "NOT_RUN"
$runtimeLongRunStatus = "NOT_RUN"
$overall = "PASS"
$notes = New-Object System.Collections.Generic.List[string]

try {
    if (Test-Path $sharedClasspathSnapshotDir) {
        # Workaround for intermittent Gradle unreadable classpath-snapshot state on Windows.
        Remove-Item -Path $sharedClasspathSnapshotDir -Recurse -Force -ErrorAction SilentlyContinue
    }
    Invoke-GradleTask @(":platforms:client-desktop-x86_64:app:compileKotlin")
} catch {
    $desktopCompileStatus = "FAIL"
    $overall = "FAIL"
    $notes.Add($_.Exception.Message) | Out-Null
}

if ($overall -eq "PASS") {
    try {
        Invoke-GradleTask @(":shared:desktopTest", ":core:network:desktopTest")
        $desktopTestStatus = "PASS"
    } catch {
        $desktopTestStatus = "FAIL"
        $overall = "FAIL"
        $notes.Add($_.Exception.Message) | Out-Null
    }
}

if ($overall -eq "PASS" -and $BaseUrl -and $PlaylistUrls.Count -gt 0 -and (Test-Path $longRunScript)) {
    try {
        Write-Host "==> video runtime long-run smoke (desktop evidence)" -ForegroundColor Cyan
        & $longRunScript `
            -BaseUrl $BaseUrl `
            -Username $Username `
            -Password $Password `
            -DurationMinutes $DurationMinutes `
            -PlaylistUrls $PlaylistUrls
        if ($LASTEXITCODE -eq 0) {
            $runtimeLongRunStatus = "PASS"
        } elseif ($LASTEXITCODE -eq 3) {
            $runtimeLongRunStatus = "PARTIAL"
            if ((-not $CompileAndTestOnly) -and ($overall -ne "FAIL")) { $overall = "PARTIAL" }
        } else {
            $runtimeLongRunStatus = "FAIL"
            $overall = "FAIL"
        }
    } catch {
        $runtimeLongRunStatus = "FAIL"
        $overall = "FAIL"
        $notes.Add($_.Exception.Message) | Out-Null
    }
} else {
    $notes.Add("Long-run runtime smoke not executed (missing BaseUrl/PlaylistUrls or script).") | Out-Null
}

if (-not $CompileAndTestOnly) {
    if ($overall -ne "FAIL" -and $runtimeLongRunStatus -eq "NOT_RUN") {
        $overall = "PARTIAL"
    }
} elseif ($overall -eq "PASS") {
    $notes.Add("CompileAndTestOnly: overall reflects compile+desktop module tests only (runtime long-run not required for PASS).") | Out-Null
}

$summary = [ordered]@{
    runId = $runId
    timestampUtc = [DateTime]::UtcNow.ToString("o")
    statuses = @{
        overall = $overall
        compileDesktop = $desktopCompileStatus
        desktopTests = $desktopTestStatus
        runtimeLongRun = $runtimeLongRunStatus
    }
    notes = @($notes)
}
$summary | ConvertTo-Json -Depth 6 | Set-Content -Path $summaryJson -Encoding UTF8

$lines = @(
    "# Desktop Video/Event Long-run Smoke Report",
    "",
    "- Run ID: $runId",
    "- Overall: **$overall**",
    "- compileDesktop: **$desktopCompileStatus**",
    "- desktopTests: **$desktopTestStatus**",
    "- runtimeLongRun: **$runtimeLongRunStatus**",
    "",
    "## Notes"
)
if ($notes.Count -eq 0) {
    $lines += "- none"
} else {
    foreach ($n in $notes) { $lines += "- $n" }
}
$lines -join [Environment]::NewLine | Set-Content -Path $summaryMd -Encoding UTF8

Write-Host ("Desktop smoke summary: {0}" -f $summaryMd) -ForegroundColor Green
Write-Host ("Desktop smoke json: {0}" -f $summaryJson) -ForegroundColor Green

if ($overall -eq "FAIL") { exit 1 }
if ($overall -eq "PARTIAL") { exit 3 }
exit 0
