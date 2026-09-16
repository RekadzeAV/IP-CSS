# Long-run smoke for 1.8 runtime stability (HLS/RTSP path).
# Periodically checks health + optional HLS/stream URLs and writes a markdown report.
#
# Usage:
#   .\scripts\video-runtime-longrun-smoke.ps1 `
#     -BaseUrl "http://localhost:8080" `
#     -Username "admin" `
#     -Password "<pwd>" `
#     -DurationMinutes 30 `
#     -IntervalSec 30 `
#     -HealthPath "/api/v1/health/ready" `
#     -PlaylistUrls @("http://localhost:8080/api/v1/hls/cam-1/playlist.m3u8")

[Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSAvoidUsingPlainTextForPassword', '')]
param(
    [switch]$ShowHelp,
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "admin",
    [int]$DurationMinutes = 30,
    [int]$IntervalSec = 30,
    [string]$HealthPath = "/api/v1/health/ready",
    [string[]]$PlaylistUrls = @(),
    [string[]]$CameraIds = @(),
    [switch]$AutoDiscoverCameraIds,
    [switch]$AutoStartStreams,
    [switch]$AttemptLogin,
    [string]$FallbackCameraConfigPath = "config\test-cameras.local.json",
    [string]$OutputDir = "diagnostics\video-longrun",
    [switch]$RequireLogin
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Video runtime long-run smoke (health + optional HLS playlists over time)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\video-runtime-longrun-smoke.ps1 -BaseUrl http://localhost:8080 -Username admin -Password <pwd> -PlaylistUrls http://localhost:8080/api/v1/hls/cam-1/playlist.m3u8"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -DurationMinutes  -IntervalSec  Polling window and cadence"
    Write-Host "  -HealthPath  Path or full URL for readiness probe (default /api/v1/health/ready)"
    Write-Host "  -CameraIds  Camera IDs for /api/v1/cameras/{id}/stream/status and RTSP diagnostics snapshots"
    Write-Host "  -AutoDiscoverCameraIds  Load camera IDs from /api/v1/cameras when -CameraIds is empty"
    Write-Host "  -AutoStartStreams  Start streams for resolved camera IDs before checks"
    Write-Host "  -AttemptLogin  Try login even when -RequireLogin is not set"
    Write-Host "  -FallbackCameraConfigPath  Local camera config fallback for camera IDs"
    Write-Host "  -OutputDir  Repo-relative folder for video-longrun-report-*.md"
    Write-Host "  -RequireLogin  Fail if login does not return HTTP 200"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  All checks passed"
    Write-Host "  1  One or more checks failed"
    exit 0
}

function Resolve-ProjectPath {
    param([string]$PathValue)
    $scriptDir = $PSScriptRoot
    $projectRoot = Split-Path -Parent $scriptDir
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

function Get-CameraIdsFromConfig {
    param([string]$ConfigPath)
    if (-not (Test-Path -LiteralPath $ConfigPath)) { return @() }
    try {
        $cfg = Get-Content -LiteralPath $ConfigPath -Raw | ConvertFrom-Json
        if ($null -eq $cfg -or $null -eq $cfg.cameras) { return @() }
        return @($cfg.cameras | ForEach-Object { $_.id } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)
    } catch {
        return @()
    }
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Url,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session = $null,
        [object]$Body = $null,
        [int]$TimeoutSec = 15
    )
    $params = @{
        Method = $Method
        Uri = $Url
        TimeoutSec = $TimeoutSec
        ErrorAction = "Stop"
    }
    if ($null -ne $Session) {
        $params["WebSession"] = $Session
    }
    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = ($Body | ConvertTo-Json -Depth 8)
    }

    if ($Method -eq "GET" -and $null -eq $Body -and $null -eq $Session) {
        $tmpPath = [System.IO.Path]::GetTempFileName()
        try {
            $code = & curl.exe --max-time $TimeoutSec -s -o $tmpPath -w "%{http_code}" $Url
            $content = ""
            if (Test-Path -LiteralPath $tmpPath) {
                $content = Get-Content -LiteralPath $tmpPath -Raw -ErrorAction SilentlyContinue
            }
            $statusCode = 0
            [void][int]::TryParse($code, [ref]$statusCode)
            return [PSCustomObject]@{
                Ok = ($statusCode -ge 200 -and $statusCode -lt 400)
                StatusCode = $statusCode
                Content = $content
                ErrorMessage = $(if ($statusCode -eq 0) { "curl request failed" } else { "" })
            }
        } finally {
            Remove-Item -LiteralPath $tmpPath -Force -ErrorAction SilentlyContinue
        }
    }

    try {
        $resp = Invoke-WebRequest @params
        return [PSCustomObject]@{
            Ok = $true
            StatusCode = [int]$resp.StatusCode
            Content = $resp.Content
            ErrorMessage = ""
        }
    } catch {
        $err = $_.Exception
        $statusCode = 0
        try {
            if ($null -ne $err -and $null -ne $err.Response -and $null -ne $err.Response.StatusCode) {
                $statusCode = [int]$err.Response.StatusCode
            }
        } catch { }
        $errorMessage = ""
        if ($null -ne $err) {
            if (-not [string]::IsNullOrWhiteSpace($err.Message)) {
                $errorMessage = $err.Message
            } else {
                $errorMessage = $err.ToString()
            }
        } else {
            $errorMessage = "Unknown request error"
        }
        return [PSCustomObject]@{
            Ok = $false
            StatusCode = $statusCode
            Content = ""
            ErrorMessage = $errorMessage
        }
    }
}

function ConvertFrom-JsonSafe {
    param([string]$Raw)
    if ([string]::IsNullOrWhiteSpace($Raw)) { return $null }
    try {
        return ($Raw | ConvertFrom-Json)
    } catch {
        return $null
    }
}

function Convert-ResponseContentToText {
    param([object]$Content)
    if ($null -eq $Content) { return "" }
    if ($Content -is [string]) { return $Content }
    if ($Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Content)
    }
    return $Content.ToString()
}

$resolvedOutputDir = Resolve-ProjectPath -PathValue $OutputDir
if (-not (Test-Path $resolvedOutputDir)) {
    New-Item -ItemType Directory -Path $resolvedOutputDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportPath = Join-Path $resolvedOutputDir "video-longrun-report-$timestamp.md"
$base = $BaseUrl.TrimEnd("/")
$healthUrl = if ($HealthPath.StartsWith("http")) { $HealthPath } else { "$base$HealthPath" }

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$isAuthenticated = $false
$resolvedCameraIds = @($CameraIds)
$startedStreams = New-Object System.Collections.Generic.List[string]
$autoDiscoverEnabled = $AutoDiscoverCameraIds.IsPresent -or ($CameraIds.Count -eq 0)
$autoStartEnabled = $AutoStartStreams.IsPresent
$fallbackCameraConfigAbs = Resolve-ProjectPath -PathValue $FallbackCameraConfigPath

Write-Host "Long-run smoke started" -ForegroundColor Cyan
Write-Host "Base URL: $base" -ForegroundColor Gray
Write-Host "Duration: $DurationMinutes min, interval: $IntervalSec sec" -ForegroundColor Gray

# Login can be required or attempted best-effort.
if ($RequireLogin -or $AttemptLogin -or $autoDiscoverEnabled -or $autoStartEnabled) {
    $login = Invoke-Api -Method "POST" -Url "$base/api/v1/auth/login" -Session $session -Body @{
        username = $Username
        password = $Password
    }
    if ($login.StatusCode -eq 200) {
        $isAuthenticated = $true
    } elseif ($RequireLogin) {
        throw "Login failed: HTTP $($login.StatusCode) $($login.ErrorMessage)"
    } else {
        Write-Host "Login attempt failed (continuing): HTTP $($login.StatusCode) $($login.ErrorMessage)" -ForegroundColor Yellow
    }
}

if ($autoDiscoverEnabled -and $resolvedCameraIds.Count -eq 0 -and $isAuthenticated) {
    $camerasResp = Invoke-Api -Method "GET" -Url "$base/api/v1/cameras" -Session $session
    if ($camerasResp.StatusCode -eq 200) {
        $camerasJson = ConvertFrom-JsonSafe -Raw $camerasResp.Content
        $cameraData = $null
        if ($null -ne $camerasJson -and $null -ne $camerasJson.data) {
            $cameraData = $camerasJson.data
        }
        if ($null -ne $cameraData) {
            $resolvedCameraIds = @($cameraData | ForEach-Object { $_.id } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)
        }
    }
}

if ($autoDiscoverEnabled -and $resolvedCameraIds.Count -eq 0) {
    $resolvedCameraIds = Get-CameraIdsFromConfig -ConfigPath $fallbackCameraConfigAbs
}

if ($autoStartEnabled -and $resolvedCameraIds.Count -gt 0 -and $isAuthenticated) {
    foreach ($cameraId in $resolvedCameraIds) {
        $startResp = Invoke-Api -Method "POST" -Url "$base/api/v1/cameras/$cameraId/stream/start" -Session $session
        if ($startResp.StatusCode -eq 200) {
            $startedStreams.Add($cameraId) | Out-Null
        }
    }
}

$start = Get-Date
$end = $start.AddMinutes($DurationMinutes)
$checks = New-Object System.Collections.Generic.List[object]
$rtspDiagSamples = New-Object System.Collections.Generic.List[object]
$iteration = 0

while ((Get-Date) -lt $end) {
    $iteration += 1
    $ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

    $health = Invoke-Api -Method "GET" -Url $healthUrl
    $healthPass = $health.StatusCode -eq 200
    $checks.Add([PSCustomObject]@{
        Time = $ts
        Check = "health"
        Target = $healthUrl
        StatusCode = $health.StatusCode
        Passed = $healthPass
        Error = $health.ErrorMessage
    })

    foreach ($u in $PlaylistUrls) {
        $pl = if ($isAuthenticated) {
            Invoke-Api -Method "GET" -Url $u -Session $session
        } else {
            Invoke-Api -Method "GET" -Url $u
        }
        $playlistText = Convert-ResponseContentToText -Content $pl.Content
        $isM3u8 = $playlistText -match "#EXTM3U"
        $playlistPass = $pl.StatusCode -eq 200 -and $isM3u8
        $checks.Add([PSCustomObject]@{
            Time = $ts
            Check = "playlist"
            Target = $u
            StatusCode = $pl.StatusCode
            Passed = $playlistPass
            Error = $pl.ErrorMessage
        })
    }

    foreach ($cameraId in $resolvedCameraIds) {
        $statusUrl = "$base/api/v1/cameras/$cameraId/stream/status"
        $streamStatus = if ($isAuthenticated) {
            Invoke-Api -Method "GET" -Url $statusUrl -Session $session
        } else {
            Invoke-Api -Method "GET" -Url $statusUrl
        }
        $streamJson = ConvertFrom-JsonSafe -Raw $streamStatus.Content
        $streamData = $null
        if ($null -ne $streamJson -and $null -ne $streamJson.data) {
            $streamData = $streamJson.data
        }
        $active = $false
        if ($null -ne $streamData -and $null -ne $streamData.active) {
            $active = [bool]$streamData.active
        }
        $isAuthLimited = (-not $isAuthenticated) -and ($streamStatus.StatusCode -eq 401 -or $streamStatus.StatusCode -eq 403)
        $statusPass = ($streamStatus.StatusCode -eq 200) -or $isAuthLimited
        $statusError = if ($isAuthLimited) { "Auth required (stream status skipped without credentials)" } else { $streamStatus.ErrorMessage }
        $checks.Add([PSCustomObject]@{
            Time = $ts
            Check = "stream_status"
            Target = $statusUrl
            StatusCode = $streamStatus.StatusCode
            Passed = $statusPass
            Error = $statusError
        })
        if ($statusPass -and $active -and $null -ne $streamData.rtspDiagnostics) {
            $diag = $streamData.rtspDiagnostics
            $rtspDiagSamples.Add([PSCustomObject]@{
                Time = $ts
                CameraId = $cameraId
                ConnectAttempts = [long]$diag.connectAttempts
                ConnectSuccesses = [long]$diag.connectSuccesses
                ConnectFailures = [long]$diag.connectFailures
                ReconnectAttempts = [long]$diag.reconnectAttempts
                ReconnectSuccesses = [long]$diag.reconnectSuccesses
                ReconnectFailures = [long]$diag.reconnectFailures
                ConsecutiveFailures = [long]$diag.consecutiveFailures
                LastError = [string]$diag.lastError
            }) | Out-Null
        }
    }

    # Keep auth session alive for longer runs.
    if ($isAuthenticated -and ($iteration % 10) -eq 0) {
        [void](Invoke-Api -Method "POST" -Url "$base/api/v1/auth/refresh" -Session $session -Body @{})
    }

    Start-Sleep -Seconds $IntervalSec
}

$total = $checks.Count
$passed = @($checks | Where-Object { $_.Passed }).Count
$failed = $total - $passed
$overallPass = $failed -eq 0

$lines = @()
$lines += "# Video Runtime Long-Run Smoke Report"
$lines += ""
$lines += "- Generated at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$lines += "- Base URL: $base"
$lines += "- Duration minutes: $DurationMinutes"
$lines += "- Interval sec: $IntervalSec"
$lines += "- Health URL: $healthUrl"
$lines += "- Playlist targets: " + ($(if ($PlaylistUrls.Count -gt 0) { ($PlaylistUrls -join ", ") } else { "none" }))
$lines += "- Stream status targets: " + ($(if ($resolvedCameraIds.Count -gt 0) { ($resolvedCameraIds | ForEach-Object { "$base/api/v1/cameras/$_/stream/status" }) -join ", " } else { "none" }))
$lines += "- Auto discovered camera IDs: " + ($(if ($autoDiscoverEnabled) { if ($resolvedCameraIds.Count -gt 0) { $resolvedCameraIds -join ", " } else { "none" } } else { "disabled" }))
$lines += "- Fallback camera config: " + $fallbackCameraConfigAbs
$lines += "- Auto started streams: " + ($(if ($startedStreams.Count -gt 0) { ($startedStreams -join ", ") } else { "none" }))
$lines += "- Passed checks: **$passed / $total**"
$lines += "- Overall: " + ($(if ($overallPass) { "PASS" } else { "FAIL" }))
$lines += ""
$lines += "| Time | Check | Target | HTTP | Status | Error |"
$lines += "|---|---|---|---|---|---|"
foreach ($c in $checks) {
    $st = if ($c.Passed) { "OK" } else { "FAIL" }
    $err = if ([string]::IsNullOrWhiteSpace($c.Error)) { "" } else { $c.Error.Replace("|", "/") }
    $lines += "| $($c.Time) | $($c.Check) | $($c.Target) | $($c.StatusCode) | $st | $err |"
}

if ($rtspDiagSamples.Count -gt 0) {
    $lines += ""
    $lines += "## RTSP Diagnostics Snapshot"
    $lines += ""
    $lines += "| Camera | Last Sample | Reconnect Attempts | Reconnect Successes | Reconnect Failures | Max Consecutive Failures | Last Error |"
    $lines += "|---|---|---|---|---|---|---|"
    foreach ($cam in ($rtspDiagSamples | Select-Object -ExpandProperty CameraId -Unique)) {
        $camSamples = @($rtspDiagSamples | Where-Object { $_.CameraId -eq $cam })
        $last = $camSamples | Select-Object -Last 1
        $maxConsecutive = ($camSamples | Measure-Object -Property ConsecutiveFailures -Maximum).Maximum
        $lastError = if ([string]::IsNullOrWhiteSpace($last.LastError)) { "" } else { $last.LastError.Replace("|", "/") }
        $lines += "| $cam | $($last.Time) | $($last.ReconnectAttempts) | $($last.ReconnectSuccesses) | $($last.ReconnectFailures) | $maxConsecutive | $lastError |"
    }
}

Set-Content -Path $reportPath -Value ($lines -join [Environment]::NewLine) -Encoding UTF8

if ($startedStreams.Count -gt 0 -and $isAuthenticated) {
    foreach ($cameraId in $startedStreams) {
        [void](Invoke-Api -Method "POST" -Url "$base/api/v1/cameras/$cameraId/stream/stop" -Session $session)
    }
}

Write-Host "Long-run smoke report: $reportPath" -ForegroundColor Cyan
if (-not $overallPass) {
    Write-Host "Long-run smoke FAILED" -ForegroundColor Red
    exit 1
}

Write-Host "Long-run smoke PASSED" -ForegroundColor Green
exit 0
