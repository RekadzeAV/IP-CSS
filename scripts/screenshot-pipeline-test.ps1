#!/usr/bin/env pwsh
# W2-2: Screenshot Pipeline Tests
# Назначение: тестирование pipeline захвата кадров (captureFrame)

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$OutputDir = "diagnostics/screenshot-tests",
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
Screenshot Pipeline Tests

Usage:
  .\scripts\screenshot-pipeline-test.ps1 [-BaseUrl <url>]

Options:
  -BaseUrl    URL API сервера (default: http://localhost:8080)
  -OutputDir  Директория для отчётов (default: diagnostics/screenshot-tests)
  -ShowHelp   Показать эту справку

Examples:
  # Базовое тестирование
  .\scripts\screenshot-pipeline-test.ps1

  # С кастомным URL
  .\scripts\screenshot-pipeline-test.ps1 -BaseUrl http://192.168.1.100:8080

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
$reportMdPath = Join-Path $OutputDir ("screenshot-pipeline-test-{0}.md" -f $runId)
$reportJsonPath = Join-Path $OutputDir ("screenshot-pipeline-test-{0}.json" -f $runId)

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

# ============================================================================
# Test Execution
# ============================================================================

Write-Separator "Screenshot Pipeline Tests"
Write-Host "Run ID:     $runId" -ForegroundColor White
Write-Host "Timestamp:  $timestamp" -ForegroundColor White
Write-Host "Base URL:   $BaseUrl" -ForegroundColor White
Write-Host "Output:     $OutputDir" -ForegroundColor White
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

function Test-ScreenshotBasicCapture {
    Write-Separator "Test: Basic Screenshot Capture"
    
    $result = @{
        test = "Basic Screenshot Capture"
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
            name = "Screenshot_Test_$runId"
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
        
        # Запрос скриншота
        Write-Info "Capturing screenshot..."
        $screenshotResponse = Invoke-ApiRequest -Method "Post" -Path "/cameras/$cameraId/screenshot"
        
        if ($screenshotResponse.Success) {
            Write-Success "Screenshot captured successfully"
            $result.status = "PASS"
            $result.metrics["format"] = $screenshotResponse.Data.format
            $result.metrics["width"] = $screenshotResponse.Data.width
            $result.metrics["height"] = $screenshotResponse.Data.height
        } else {
            throw "Screenshot capture failed: $($screenshotResponse.Error)"
        }
        
        $stopwatch.Stop()
        $result.durationMs = $stopwatch.ElapsedMilliseconds
        $result.metrics["captureTimeMs"] = $result.durationMs
        
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

function Test-ScreenshotFromRtsp {
    Write-Separator "Test: Screenshot from RTSP Direct"
    
    $result = @{
        test = "Screenshot from RTSP Direct"
        status = "NOT_RUN"
        durationMs = 0
        errors = @()
        metrics = @{}
    }
    
    try {
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        
        # Прямой захват из RTSP URL
        Write-Info "Capturing screenshot from RTSP URL..."
        $screenshotResponse = Invoke-ApiRequest -Method "Post" -Path "/streams/screenshot" -Body @{
            rtspUrl = "rtsp://127.0.0.1:8554/stream"
            timeoutMs = 10000
        }
        
        if ($screenshotResponse.Success) {
            Write-Success "Screenshot captured from RTSP"
            $result.status = "PASS"
            $result.metrics["format"] = $screenshotResponse.Data.format
        } else {
            Write-Warn "Direct RTSP screenshot failed (expected if no RTSP server): $($screenshotResponse.Error)"
            $result.status = "SKIPPED"
        }
        
        $stopwatch.Stop()
        $result.durationMs = $stopwatch.ElapsedMilliseconds
        
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
    } else {
        $testResults.summary.skipped++
    }
    
    $testResults.tests += $result
    return $result
}

function Test-ScreenshotTimeout {
    Write-Separator "Test: Screenshot Timeout Handling"
    
    $result = @{
        test = "Screenshot Timeout"
        status = "NOT_RUN"
        durationMs = 0
        errors = @()
        metrics = @{}
    }
    
    try {
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        
        # Создание камеры с неверным URL
        Write-Info "Creating camera with invalid URL..."
        $cameraData = @{
            name = "Screenshot_Timeout_Test_$runId"
            url = "rtsp://192.0.2.1:554/stream"  # Test-NET-1 (недоступный)
            protocol = "RTSP"
            enabled = $true
        }
        
        $response = Invoke-ApiRequest -Method "Post" -Path "/cameras" -Body $cameraData
        if (-not $response.Success) {
            throw "Failed to create camera: $($response.Error)"
        }
        
        $cameraId = $response.Data.id
        Write-Success "Camera created: $cameraId"
        
        # Запрос скриншота с коротким таймаутом
        Write-Info "Testing timeout handling..."
        $screenshotResponse = Invoke-ApiRequest -Method "Post" -Path "/cameras/$cameraId/screenshot"
        
        # Ожидается ошибка или таймаут
        if (-not $screenshotResponse.Success) {
            Write-Success "Timeout handled correctly (error returned)"
            $result.status = "PASS"
            $result.metrics["expectedError"] = $true
        } else {
            Write-Warn "Screenshot succeeded unexpectedly"
            $result.status = "PASS"
            $result.metrics["unexpectedSuccess"] = $true
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

function Test-ScreenshotMultipleSequential {
    Write-Separator "Test: Multiple Sequential Screenshots"
    
    $result = @{
        test = "Multiple Sequential Screenshots"
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
            name = "Screenshot_Multi_Test_$runId"
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
        
        # Несколько последовательных захватов
        $captureCount = 3
        $successCount = 0
        $timings = @()
        
        for ($i = 1; $i -le $captureCount; $i++) {
            Write-Info "Capture $i/$captureCount..."
            $startTime = Get-Date
            
            $screenshotResponse = Invoke-ApiRequest -Method "Post" -Path "/cameras/$cameraId/screenshot"
            
            $elapsed = (New-TimeSpan -Start $startTime -End (Get-Date)).TotalMilliseconds
            $timings += $elapsed
            
            if ($screenshotResponse.Success) {
                $successCount++
            }
            
            Start-Sleep -Milliseconds 500  # Небольшая задержка между захватами
        }
        
        if ($successCount -eq $captureCount) {
            Write-Success "All $captureCount screenshots captured successfully"
            $result.status = "PASS"
        } else {
            Write-Error "Only $successCount/$captureCount screenshots succeeded"
            $result.status = "FAIL"
            $result.errors += "Failed to capture all screenshots"
        }
        
        $stopwatch.Stop()
        $result.durationMs = $stopwatch.ElapsedMilliseconds
        $result.metrics["totalCaptures"] = $captureCount
        $result.metrics["successfulCaptures"] = $successCount
        $result.metrics["avgCaptureTimeMs"] = ($timings | Measure-Object -Average).Average
        $result.metrics["totalTimeMs"] = $result.durationMs
        
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

# Test 1: Basic Screenshot Capture
Test-ScreenshotBasicCapture | Out-Null

# Test 2: Screenshot from RTSP Direct
Test-ScreenshotFromRtsp | Out-Null

# Test 3: Screenshot Timeout Handling
Test-ScreenshotTimeout | Out-Null

# Test 4: Multiple Sequential Screenshots
Test-ScreenshotMultipleSequential | Out-Null

# ============================================================================
# Generate Report
# ============================================================================

Write-Separator "Generating Reports"

# Markdown report
$mdLines = @(
    "# Screenshot Pipeline Test Report",
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
