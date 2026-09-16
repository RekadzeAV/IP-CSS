#!/usr/bin/env pwsh
# W2-1: HLS Pipeline Runtime Stability Tests
# Назначение: тестирование стабильности HLS pipeline (long-run, reconnect, cleanup)

param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$LongRunDurationSeconds = 120,
    [switch]$FullTest,
    [switch]$SkipCleanupTest,
    [switch]$SkipReconnectTest,
    [string]$OutputDir = "diagnostics/hls-tests",
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
HLS Pipeline Runtime Stability Tests

Usage:
  .\scripts\hls-runtime-stability-test.ps1 [-FullTest] [-LongRunDurationSeconds <sec>]

Options:
  -BaseUrl                   URL API сервера (default: http://localhost:8080)
  -LongRunDurationSeconds    Длительность long-run теста в секундах (default: 120)
  -FullTest                  Запустить все тесты (включая cleanup и reconnect)
  -SkipCleanupTest           Пропустить тест cleanup процессов
  -SkipReconnectTest         Пропустить тест reconnect
  -OutputDir                 Директория для отчётов (default: diagnostics/hls-tests)
  -ShowHelp                  Показать эту справку

Examples:
  # Базовый тест (краткий)
  .\scripts\hls-runtime-stability-test.ps1

  # Полный тест с длительным запуском
  .\scripts\hls-runtime-stability-test.ps1 -FullTest -LongRunDurationSeconds 600

  # Только cleanup тест
  .\scripts\hls-runtime-stability-test.ps1 -SkipReconnectTest

"@
    exit 0
}

# ============================================================================
# Setup
# ============================================================================

$ErrorActionPreference = "Stop"
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

# Resolve paths
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$OutputDir = Join-Path $projectRoot $OutputDir

if (!(Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

# Output files
$reportMdPath = Join-Path $OutputDir ("hls-runtime-stability-test-{0}.md" -f $runId)
$reportJsonPath = Join-Path $OutputDir ("hls-runtime-stability-test-{0}.json" -f $runId)

# ============================================================================
# Helper Functions
# ============================================================================

function Write-Success {
    param([string]$Message)
    Write-Host "  ✓ $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "  ✗ $Message" -ForegroundColor Red
}

function Write-Warn {
    param([string]$Message)
    Write-Host "  ⚠ $Message" -ForegroundColor Yellow
}

function Write-Info {
    param([string]$Message)
    Write-Host "  ℹ $Message" -ForegroundColor Cyan
}

function Write-Separator {
    param([string]$Title = "")
    if ($Title) {
        Write-Host ""
        Write-Host ("=" * 80) -ForegroundColor DarkGray
        Write-Host "  $Title" -ForegroundColor DarkGray
        Write-Host ("=" * 80) -ForegroundColor DarkGray
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host ("-" * 80) -ForegroundColor DarkGray
        Write-Host ""
    }
}

function Invoke-ApiRequest {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )
    
    $uri = "$BaseUrl/api/v1$Path"
    
    try {
        $headers["Content-Type"] = "application/json"
        $params = @{
            Method = $Method
            Uri = $uri
            Headers = $Headers
            UseBasicParsing = $true
        }
        
        if ($Body) {
            $params["Body"] = $Body | ConvertTo-Json -Depth 10
        }
        
        $response = Invoke-RestMethod @params
        return @{
            Success = $true
            Data = $response
            StatusCode = 200
        }
    } catch {
        return @{
            Success = $false
            Error = $_.Exception.Message
            StatusCode = $_.Exception.Response.StatusCode.value__
        }
    }
}

function Get-FfmpegProcesses {
    # Получение списка процессов FFmpeg
    $processes = Get-Process | Where-Object { $_.ProcessName -like "*ffmpeg*" }
    return $processes
}

function Get-HlsDirectories {
    # Получение списка HLS директорий
    $hlsBase = "data/recordings/hls"
    if (Test-Path $hlsBase) {
        return Get-ChildItem -Path $hlsBase -Directory
    }
    return @()
}

# ============================================================================
# Test Execution
# ============================================================================

Write-Separator "HLS Pipeline Runtime Stability Tests"
Write-Host "Run ID:     $runId" -ForegroundColor White
Write-Host "Timestamp:  $timestamp" -ForegroundColor White
Write-Host "Base URL:   $BaseUrl" -ForegroundColor White
Write-Host "Output:     $OutputDir" -ForegroundColor White
Write-Host "Full Test:  $FullTest" -ForegroundColor White
Write-Host ""

$testResults = @{
    runId = $runId
    timestamp = $timestamp
    tests = @()
    summary = @{
        total = 0
        passed = 0
        failed = 0
        skipped = 0
    }
}

function Test-HlsStreamStartup {
    Write-Separator "Test: HLS Stream Startup"
    
    $result = @{
        test = "HLS Stream Startup"
        status = "NOT_RUN"
        durationMs = 0
        errors = @()
        metrics = @{}
    }
    
    try {
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        
        # Создание тестовой камеры
        Write-Info "Creating test camera..."
        $cameraData = @{
            name = "HLS_Test_Camera_$runId"
            url = "rtsp://127.0.0.1:8554/stream"
            protocol = "RTSP"
            enabled = $true
        }
        
        $response = Invoke-ApiRequest -Method "Post" -Path "/cameras" -Body $cameraData
        if (-not $response.Success) {
            throw "Failed to create camera: $($response.Error)"
        }
        
        $cameraId = $response.Data.id
        Write-Success "Camera created: $cameraId"
        
        # Запуск HLS потока
        Write-Info "Starting HLS stream..."
        $hlsResponse = Invoke-ApiRequest -Method "Post" -Path "/streams/$cameraId/hls/start"
        
        if (-not $hlsResponse.Success) {
            throw "Failed to start HLS: $($hlsResponse.Error)"
        }
        
        # Ожидание создания плейлиста
        Write-Info "Waiting for HLS playlist..."
        Start-Sleep -Seconds 5
        
        # Проверка плейлиста
        $playlistResponse = Invoke-ApiRequest -Method "Get" -Path "/streams/$cameraId/hls/playlist.m3u8"
        
        if ($playlistResponse.Success) {
            Write-Success "HLS playlist created successfully"
            $result.status = "PASS"
            $result.metrics["playlistSize"] = $playlistResponse.Data.Length
        } else {
            throw "HLS playlist not found: $($playlistResponse.Error)"
        }
        
        $stopwatch.Stop()
        $result.durationMs = $stopwatch.ElapsedMilliseconds
        $result.metrics["cameraId"] = $cameraId
        
        # Очистка
        Write-Info "Cleaning up..."
        Invoke-ApiRequest -Method "Delete" -Path "/cameras/$cameraId"
        
    } catch {
        $stopwatch.Stop()
        $result.status = "FAIL"
        $result.errors += $_.Exception.Message
        Write-Error "Test failed: $($_.Exception.Message)"
    }
    
    $testResults.summary.total++
    if ($result.status -eq "PASS") {
        $testResults.summary.passed++
    } elseif ($result.status -eq "FAIL") {
        $testResults.summary.failed++
    }
    
    $testResults.tests += $result
    return $result
}

function Test-HlsLongRun {
    Write-Separator "Test: HLS Long-Run Stability"
    
    $result = @{
        test = "HLS Long-Run"
        status = "NOT_RUN"
        durationMs = 0
        errors = @()
        metrics = @{}
    }
    
    try {
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        $durationSec = $LongRunDurationSeconds
        
        Write-Info "Starting long-run test for $durationSec seconds..."
        
        # Создание камеры и запуск HLS
        $cameraData = @{
            name = "HLS_LongRun_Test_$runId"
            url = "rtsp://127.0.0.1:8554/stream"
            protocol = "RTSP"
            enabled = $true
        }
        
        $response = Invoke-ApiRequest -Method "Post" -Path "/cameras" -Body $cameraData
        if (-not $response.Success) {
            throw "Failed to create camera: $($response.Error)"
        }
        
        $cameraId = $response.Data.id
        Write-Success "Camera created: $cameraId"
        
        # Запуск HLS
        $hlsResponse = Invoke-ApiRequest -Method "Post" -Path "/streams/$cameraId/hls/start"
        if (-not $hlsResponse.Success) {
            throw "Failed to start HLS: $($hlsResponse.Error)"
        }
        
        Write-Success "HLS stream started"
        
        # Мониторинг процесса
        $checkInterval = 10
        $ffmpegProcessesBefore = (Get-FfmpegProcesses).Count
        $issues = @()
        
        while ((New-TimeSpan -Start (Get-Date).AddSeconds(-$durationSec) -End (Get-Date)).TotalSeconds -lt $durationSec) {
            # Проверка процесса FFmpeg
            $ffmpegProcesses = Get-FfmpegProcesses
            $currentProcesses = $ffmpegProcesses.Count
            
            # Проверка плейлиста
            $playlistResponse = Invoke-ApiRequest -Method "Get" -Path "/streams/$cameraId/hls/playlist.m3u8"
            if (-not $playlistResponse.Success) {
                $issues += "Playlist not accessible at $(Get-Date -Format 'HH:mm:ss')"
            }
            
            # Прогресс
            $elapsed = (New-TimeSpan -Start (Get-Date).AddSeconds(-$durationSec) -End (Get-Date)).TotalSeconds
            $progress = [math]::Round(($elapsed / $durationSec) * 100, 1)
            Write-Progress -Activity "Long-run test" -Status "Monitoring..." -PercentComplete $progress
            
            Start-Sleep -Seconds $checkInterval
        }
        
        $stopwatch.Stop()
        $result.durationMs = $stopwatch.ElapsedMilliseconds
        
        if ($issues.Count -eq 0) {
            Write-Success "Long-run test completed successfully ($durationSec seconds)"
            $result.status = "PASS"
        } else {
            Write-Error "Long-run test failed with $($issues.Count) issues"
            $result.status = "FAIL"
            $result.errors += $issues
        }
        
        $result.metrics["durationSec"] = $durationSec
        $result.metrics["ffmpegProcessesBefore"] = $ffmpegProcessesBefore
        $result.metrics["ffmpegProcessesAfter"] = (Get-FfmpegProcesses).Count
        $result.metrics["issues"] = $issues.Count
        
        # Очистка
        Write-Info "Cleaning up..."
        Invoke-ApiRequest -Method "Delete" -Path "/cameras/$cameraId"
        
    } catch {
        $stopwatch.Stop()
        $result.status = "FAIL"
        $result.errors += $_.Exception.Message
        Write-Error "Test failed: $($_.Exception.Message)"
    }
    
    $testResults.summary.total++
    if ($result.status -eq "PASS") {
        $testResults.summary.passed++
    } elseif ($result.status -eq "FAIL") {
        $testResults.summary.failed++
    }
    
    $testResults.tests += $result
    return $result
}

function Test-HlsCleanup {
    if ($SkipCleanupTest) {
        Write-Separator "Test: HLS Cleanup (SKIPPED)"
        $result = @{
            test = "HLS Cleanup"
            status = "SKIPPED"
            durationMs = 0
            errors = @()
            metrics = @{}
        }
        $testResults.summary.total++
        $testResults.summary.skipped++
        $testResults.tests += $result
        return $result
    }
    
    Write-Separator "Test: HLS Cleanup on Stop"
    
    $result = @{
        test = "HLS Cleanup"
        status = "NOT_RUN"
        durationMs = 0
        errors = @()
        metrics = @{}
    }
    
    try {
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        
        # Создание камеры и запуск HLS
        $cameraData = @{
            name = "HLS_Cleanup_Test_$runId"
            url = "rtsp://127.0.0.1:8554/stream"
            protocol = "RTSP"
            enabled = $true
        }
        
        $response = Invoke-ApiRequest -Method "Post" -Path "/cameras" -Body $cameraData
        if (-not $response.Success) {
            throw "Failed to create camera: $($response.Error)"
        }
        
        $cameraId = $response.Data.id
        Write-Success "Camera created: $cameraId"
        
        # Запуск HLS
        $hlsResponse = Invoke-ApiRequest -Method "Post" -Path "/streams/$cameraId/hls/start"
        if (-not $hlsResponse.Success) {
            throw "Failed to start HLS: $($hlsResponse.Error)"
        }
        
        Write-Success "HLS stream started"
        Start-Sleep -Seconds 5
        
        # Подсчёт процессов и файлов до остановки
        $ffmpegProcessesBefore = (Get-FfmpegProcesses).Count
        $hlsDirsBefore = (Get-HlsDirectories).Count
        Write-Info "FFmpeg processes before stop: $ffmpegProcessesBefore"
        Write-Info "HLS directories before stop: $hlsDirsBefore"
        
        # Остановка HLS
        Write-Info "Stopping HLS stream..."
        $stopResponse = Invoke-ApiRequest -Method "Post" -Path "/streams/$cameraId/hls/stop"
        
        if (-not $stopResponse.Success) {
            throw "Failed to stop HLS: $($stopResponse.Error)"
        }
        
        Write-Success "HLS stream stopped"
        
        # Ожидание cleanup
        Start-Sleep -Seconds 5
        
        # Проверка cleanup
        $ffmpegProcessesAfter = (Get-FfmpegProcesses).Count
        $hlsDirsAfter = (Get-HlsDirectories).Count
        Write-Info "FFmpeg processes after stop: $ffmpegProcessesAfter"
        Write-Info "HLS directories after stop: $hlsDirsAfter"
        
        # Проверка: процессы должны быть остановлены
        if ($ffmpegProcessesAfter -le $ffmpegProcessesBefore) {
            Write-Success "FFmpeg processes cleaned up"
            $result.metrics["processesCleaned"] = $true
        } else {
            Write-Warn "Some FFmpeg processes may still be running"
            $result.metrics["processesCleaned"] = $false
            $result.errors += "FFmpeg processes not fully cleaned up"
        }
        
        $stopwatch.Stop()
        $result.durationMs = $stopwatch.ElapsedMilliseconds
        $result.status = "PASS"
        
        # Очистка камеры
        Write-Info "Cleaning up camera..."
        Invoke-ApiRequest -Method "Delete" -Path "/cameras/$cameraId"
        
    } catch {
        $stopwatch.Stop()
        $result.status = "FAIL"
        $result.errors += $_.Exception.Message
        Write-Error "Test failed: $($_.Exception.Message)"
    }
    
    $testResults.summary.total++
    if ($result.status -eq "PASS") {
        $testResults.summary.passed++
    } elseif ($result.status -eq "FAIL") {
        $testResults.summary.failed++
    }
    
    $testResults.tests += $result
    return $result
}

function Test-HlsReconnect {
    if ($SkipReconnectTest -or -not $FullTest) {
        Write-Separator "Test: HLS Reconnect (SKIPPED)"
        $result = @{
            test = "HLS Reconnect"
            status = "SKIPPED"
            durationMs = 0
            errors = @()
            metrics = @{}
        }
        $testResults.summary.total++
        $testResults.summary.skipped++
        $testResults.tests += $result
        return $result
    }
    
    Write-Separator "Test: HLS Reconnect on Stream Interruption"
    
    $result = @{
        test = "HLS Reconnect"
        status = "NOT_RUN"
        durationMs = 0
        errors = @()
        metrics = @{}
    }
    
    try {
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        
        # Создание камеры и запуск HLS
        $cameraData = @{
            name = "HLS_Reconnect_Test_$runId"
            url = "rtsp://127.0.0.1:8554/stream"
            protocol = "RTSP"
            enabled = $true
        }
        
        $response = Invoke-ApiRequest -Method "Post" -Path "/cameras" -Body $cameraData
        if (-not $response.Success) {
            throw "Failed to create camera: $($response.Error)"
        }
        
        $cameraId = $response.Data.id
        Write-Success "Camera created: $cameraId"
        
        # Запуск HLS
        $hlsResponse = Invoke-ApiRequest -Method "Post" -Path "/streams/$cameraId/hls/start"
        if (-not $hlsResponse.Success) {
            throw "Failed to start HLS: $($hlsResponse.Error)"
        }
        
        Write-Success "HLS stream started"
        Start-Sleep -Seconds 3
        
        # Имитация разрыва (остановка и старт)
        Write-Info "Simulating stream interruption..."
        $stopResponse = Invoke-ApiRequest -Method "Post" -Path "/streams/$cameraId/hls/stop"
        Start-Sleep -Seconds 2
        
        # Перезапуск
        Write-Info "Restarting HLS stream..."
        $restartResponse = Invoke-ApiRequest -Method "Post" -Path "/streams/$cameraId/hls/start"
        
        if ($restartResponse.Success) {
            Write-Success "HLS stream reconnected successfully"
            $result.status = "PASS"
        } else {
            Write-Error "Failed to reconnect HLS: $($restartResponse.Error)"
            $result.status = "FAIL"
            $result.errors += "Reconnect failed: $($restartResponse.Error)"
        }
        
        $stopwatch.Stop()
        $result.durationMs = $stopwatch.ElapsedMilliseconds
        
        # Очистка
        Write-Info "Cleaning up..."
        Invoke-ApiRequest -Method "Delete" -Path "/cameras/$cameraId"
        
    } catch {
        $stopwatch.Stop()
        $result.status = "FAIL"
        $result.errors += $_.Exception.Message
        Write-Error "Test failed: $($_.Exception.Message)"
    }
    
    $testResults.summary.total++
    if ($result.status -eq "PASS") {
        $testResults.summary.passed++
    } elseif ($result.status -eq "FAIL") {
        $testResults.summary.failed++
    }
    
    $testResults.tests += $result
    return $result
}

# ============================================================================
# Run Tests
# ============================================================================

# Test 1: HLS Stream Startup
Test-HlsStreamStartup | Out-Null

# Test 2: HLS Long-Run
Test-HlsLongRun | Out-Null

# Test 3: HLS Cleanup (if not skipped)
if (-not $SkipCleanupTest) {
    Test-HlsCleanup | Out-Null
}

# Test 4: HLS Reconnect (if full test)
if ($FullTest -and -not $SkipReconnectTest) {
    Test-HlsReconnect | Out-Null
}

# ============================================================================
# Generate Report
# ============================================================================

Write-Separator "Generating Reports"

# Markdown report
$mdLines = @(
    "# HLS Pipeline Runtime Stability Test Report",
    "",
    "- **Run ID:** $runId",
    "- **Timestamp:** $timestamp",
    "- **Base URL:** $BaseUrl",
    "",
    "## Summary",
    "",
    "| Metric | Value |",
    "|--------|-------|",
    "| Total Tests | $($testResults.summary.total) |",
    "| Passed | $($testResults.summary.passed) |",
    "| Failed | $($testResults.summary.failed) |",
    "| Skipped | $($testResults.summary.skipped) |",
    "| Success Rate | $(if ($testResults.summary.total -gt 0) { '{0:P1}' -f ($testResults.summary.passed / $testResults.summary.total) } else { 'N/A' }) |",
    "",
    "## Test Results",
    ""
)

foreach ($test in $testResults.tests) {
    $mdLines += "### $($test.test)"
    $mdLines += ""
    $mdLines += "| Property | Value |"
    $mdLines += "|----------|-------|"
    $mdLines += "| Status | **$($test.status)** |"
    $mdLines += "| Duration | $($test.durationMs) ms |"
    if ($test.errors.Count -gt 0) {
        $mdLines += "| Errors | $($test.errors -join "; ") |"
    }
    $mdLines += ""
}

$mdContent = $mdLines -join "`n"
Set-Content -Path $reportMdPath -Value $mdContent -Encoding UTF8
Write-Success "Markdown report: $reportMdPath"

# JSON report
$testResults | ConvertTo-Json -Depth 10 | Set-Content -Path $reportJsonPath -Encoding UTF8
Write-Success "JSON report: $reportJsonPath"

# ============================================================================
# Final Summary
# ============================================================================

Write-Separator "Final Summary"
Write-Host "Total Tests:   $($testResults.summary.total)" -ForegroundColor White
Write-Host "Passed:        $($testResults.summary.passed)" -ForegroundColor Green
Write-Host "Failed:        $($testResults.summary.failed)" -ForegroundColor $(if ($testResults.summary.failed -gt 0) { "Red" } else { "Green" })
Write-Host "Skipped:       $($testResults.summary.skipped)" -ForegroundColor Yellow
Write-Host ""
Write-Host "Reports:" -ForegroundColor Cyan
Write-Host "  Markdown: $reportMdPath" -ForegroundColor Gray
Write-Host "  JSON:     $reportJsonPath" -ForegroundColor Gray
Write-Host ""

if ($testResults.summary.failed -gt 0) {
    Write-Error "Some tests failed!"
    exit 1
} else {
    Write-Success "All tests passed!"
    exit 0
}
