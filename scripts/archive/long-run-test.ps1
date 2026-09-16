# Long-Run Stability Test Script
# РўРµСЃС‚РёСЂРѕРІР°РЅРёРµ СЃС‚Р°Р±РёР»СЊРЅРѕСЃС‚Рё RTSP РєР»РёРµРЅС‚Р° РЅР° РїСЂРѕС‚СЏР¶РµРЅРёРё 2+ С‡Р°СЃРѕРІ
#
# РСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ:
#   .\scripts\long-run-test.ps1 -DurationSeconds 7200
#   .\scripts\long-run-test.ps1 -DurationSeconds 3600 -CameraName "Emulator_AAC"
#   .\scripts\long-run-test.ps1 -QuickTest (15 РјРёРЅСѓС‚)

param(
    [int]$DurationSeconds = 7200,
    [string]$CameraName = "Emulator_AAC",
    [string]$OutputDir = "diagnostics\long-run-tests",
    [switch]$QuickTest,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
Long-Run Stability Test

Usage:
  .\scripts\long-run-test.ps1 -DurationSeconds 7200
  .\scripts\long-run-test.ps1 -QuickTest

Options:
  -DurationSeconds    Test duration in seconds (default: 7200 = 2 hours)
  -CameraName         Camera to test (default: Emulator_AAC)
  -OutputDir          Output directory for test results
  -QuickTest          Quick test (15 minutes)
  -ShowHelp           Show this help message

Examples:
  # 2-hour test
  .\scripts\long-run-test.ps1 -DurationSeconds 7200

  # Quick test (15 minutes)
  .\scripts\long-run-test.ps1 -QuickTest
"@
    exit 0
}

# ============================================================================
# Setup
# ============================================================================

$ErrorActionPreference = "Stop"
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

if ($QuickTest) {
    $DurationSeconds = 900  # 15 minutes
    Write-Host "Quick test mode: 15 minutes" -ForegroundColor Cyan
}

# Resolve paths
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$OutputDir = Join-Path $projectRoot $OutputDir

if (!(Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

# Output files
$reportMdPath = Join-Path $OutputDir ("long-run-test-{0}.md" -f $runId)
$reportJsonPath = Join-Path $OutputDir ("long-run-test-{0}.json" -f $runId)
$metricsCsvPath = Join-Path $OutputDir ("long-run-test-{0}.csv" -f $runId)

# ============================================================================
# Helper Functions
# ============================================================================

function Write-Success {
    param([string]$Message)
    Write-Host "  вњ“ $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "  вњ— $Message" -ForegroundColor Red
}

function Write-Warn {
    param([string]$Message)
    Write-Host "  вљ  $Message" -ForegroundColor Yellow
}

function Write-Info {
    param([string]$Message)
    Write-Host "  в„№ $Message" -ForegroundColor Cyan
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

function Get-MemoryUsage {
    # РџРѕР»СѓС‡РµРЅРёРµ РёСЃРїРѕР»СЊР·РѕРІР°РЅРёСЏ РїР°РјСЏС‚Рё РґР»СЏ РїСЂРѕС†РµСЃСЃР°
    try {
        $process = Get-Process -Name "java" -ErrorAction SilentlyContinue
        if ($process) {
            return [PSCustomObject]@{
                WorkingSetMB = [math]::Round($process.WorkingSet64 / 1MB, 2)
                PrivateMemoryMB = [math]::Round($process.PrivateMemorySize64 / 1MB, 2)
                Handles = $process.Handles
                Threads = $process.Threads
            }
        }
    } catch {
        # Р’РѕР·РІСЂР°С‰Р°РµРј РЅСѓР»Рё РµСЃР»Рё РїСЂРѕС†РµСЃСЃ РЅРµ РЅР°Р№РґРµРЅ
    }
    return [PSCustomObject]@{
        WorkingSetMB = 0
        PrivateMemoryMB = 0
        Handles = 0
        Threads = 0
    }
}

function Test-RTSPConnection {
    param([string]$Url, [int]$TimeoutMs = 5000)
    
    try {
        # РџСЂРѕСЃС‚РѕР№ С‚РµСЃС‚ РїРѕРґРєР»СЋС‡РµРЅРёСЏ С‡РµСЂРµР· ffprobe (РµСЃР»Рё РґРѕСЃС‚СѓРїРµРЅ)
        $probePath = "ffprobe.exe"
        if (Test-Path $probePath) {
            $startTime = Get-Date
            $result = & $probePath -timeout $TimeoutMs -i $Url -loglevel error -show_entries stream=codec_type -of csv=p=0 2>&1
            
            if ($LASTEXITCODE -eq 0) {
                return [PSCustomObject]@{
                    Success = $true
                    DurationMs = (New-TimeSpan -Start $startTime -End (Get-Date)).TotalMilliseconds
                    Streams = $result
                }
            }
        }
        
        # Fallback: С‚РµСЃС‚ С‡РµСЂРµР· TCP
        $tcpTest = Test-NetConnection -ComputerName "127.0.0.1" -Port 8554 -WarningAction SilentlyContinue
        return [PSCustomObject]@{
            Success = $tcpTest.TcpTestSucceeded
            DurationMs = 0
            Streams = "N/A"
        }
    } catch {
        return [PSCustomObject]@{
            Success = $false
            DurationMs = 0
            Streams = "ERROR: $($_.Exception.Message)"
        }
    }
}

# ============================================================================
# Test Execution
# ============================================================================

Write-Separator "Long-Run Stability Test"
Write-Host "Run ID:       $runId" -ForegroundColor White
Write-Host "Timestamp:    $timestamp" -ForegroundColor White
Write-Host "Duration:     $DurationSeconds seconds ($([math]::Round($DurationSeconds / 60, 1)) minutes)" -ForegroundColor White
Write-Host "Camera:       $CameraName" -ForegroundColor White
Write-Host "Output Dir:   $OutputDir" -ForegroundColor White
Write-Separator

# Check if RTSP server is running
Write-Info "Checking RTSP server..."
$serverRunning = Test-NetConnection -ComputerName "127.0.0.1" -Port 8554 -WarningAction SilentlyContinue
if (-not $serverRunning.TcpTestSucceeded) {
    Write-Warn "RTSP server not detected on port 8554"
    Write-Info "Please start the RTSP emulator:"
    Write-Host "  python scripts\rtsp-audio-test-server.py --audio-codec aac --port 8554" -ForegroundColor Yellow
    Write-Info "Continuing with connection tests anyway..."
}

# Initialize metrics collection
$metrics = @()
$startTime = Get-Date
$endTime = $startTime.AddSeconds($DurationSeconds)
$iteration = 0
$connectionSuccesses = 0
$connectionFailures = 0
$memorySnapshots = @()

Write-Info "Starting long-run test..."
Write-Info "Press Ctrl+C to stop early"

try {
    while ((Get-Date) -lt $endTime) {
        $iteration++
        $currentTimestamp = Get-Date
        $elapsedSeconds = (New-TimeSpan -Start $startTime -End $currentTimestamp).TotalSeconds
        $progressPercent = [math]::Round(($elapsedSeconds / $DurationSeconds) * 100, 2)
        
        # Every 60 seconds, collect detailed metrics
        if ($iteration % 60 -eq 0) {
            Write-Info "Progress: $([math]::Round($progressPercent, 2))% ($([math]::Round($elapsedSeconds, 0)) / $DurationSeconds seconds)"
            
            # Memory snapshot
            $memory = Get-MemoryUsage
            $memorySnapshots += [PSCustomObject]@{
                Timestamp = $currentTimestamp.ToString("HH:mm:ss")
                ElapsedSeconds = $elapsedSeconds
                WorkingSetMB = $memory.WorkingSetMB
                PrivateMemoryMB = $memory.PrivateMemoryMB
                Handles = $memory.Handles
                Threads = $memory.Threads
            }
            
            # Connection test
            $connectionTest = Test-RTSPConnection -Url "rtsp://127.0.0.1:8554/stream/"
            if ($connectionTest.Success) {
                $connectionSuccesses++
            } else {
                $connectionFailures++
            }
            
            # Record metrics
            $metrics += [PSCustomObject]@{
                Iteration = $iteration
                Timestamp = $currentTimestamp.ToString("HH:mm:ss")
                ElapsedSeconds = $elapsedSeconds
                ProgressPercent = $progressPercent
                ConnectionSuccess = $connectionTest.Success
                ConnectionDurationMs = $connectionTest.DurationMs
                MemoryWorkSetMB = $memory.WorkingSetMB
                MemoryPrivateMB = $memory.PrivateMemoryMB
                MemoryHandles = $memory.Handles
                MemoryThreads = $memory.Threads
            }
            
            # Save intermediate results
            $metrics | Export-Csv -Path $metricsCsvPath -NoTypeInformation -Append -Force
        }
        
        # Progress indicator every 10 seconds
        if ($iteration % 10 -eq 0) {
            Write-Host "." -NoNewline -ForegroundColor Gray
        }
        
        # Wait 1 second before next iteration
        Start-Sleep -Seconds 1
    }
    
    Write-Success "Test completed successfully!"
} catch {
    Write-Warn "Test interrupted: $($_.Exception.Message)"
}

# ============================================================================
# Generate Report
# ============================================================================

Write-Separator "Generating Report"

$endTime = Get-Date
$totalDuration = (New-TimeSpan -Start $startTime -End $endTime).TotalSeconds

# Calculate summary statistics
$totalIterations = $metrics.Count
$successfulConnections = ($metrics | Where-Object { $_.ConnectionSuccess }).Count
$failedConnections = ($metrics | Where-Object { -not $_.ConnectionSuccess }).Count
$avgMemoryMB = if ($metrics.Count -gt 0) { ($metrics.MemoryWorkSetMB | Measure-Object -Average).Average } else { 0 }
$maxMemoryMB = if ($metrics.Count -gt 0) { ($metrics.MemoryWorkSetMB | Measure-Object -Maximum).Maximum } else { 0 }
$minMemoryMB = if ($metrics.Count -gt 0) { ($metrics.MemoryWorkSetMB | Measure-Object -Minimum).Minimum } else { 0 }
$connectionSuccessRate = if ($totalIterations -gt 0) { [math]::Round(($successfulConnections / $totalIterations) * 100, 2) } else { 0 }

# Check for memory leaks
$memoryLeakDetected = $false
$memoryLeakTrend = "Stable"
if ($memorySnapshots.Count -ge 10) {
    $firstFive = $memorySnapshots | Select-Object -First 5
    $lastFive = $memorySnapshots | Select-Object -Last 5
    $avgFirst = ($firstFive.WorkingSetMB | Measure-Object -Average).Average
    $avgLast = ($lastFive.WorkingSetMB | Measure-Object -Average).Average
    $memoryIncrease = $avgLast - $avgFirst
    $memoryIncreasePercent = ($memoryIncrease / $avgFirst) * 100
    
    if ($memoryIncreasePercent -gt 10) {
        $memoryLeakDetected = $true
        $memoryLeakTrend = "Increasing (+$([math]::Round($memoryIncreasePercent, 2))%)"
    } elseif ($memoryIncreasePercent -gt 5) {
        $memoryLeakTrend = "Slightly Increasing (+$([math]::Round($memoryIncreasePercent, 2))%)"
    }
}

# Markdown report
$mdLines = @(
    "# Long-Run Stability Test Report",
    "",
    "- **Run ID:** $runId",
    "- **Timestamp:** $timestamp",
    "- **Duration:** $([math]::Round($totalDuration, 0)) seconds ($([math]::Round($totalDuration / 60, 1)) minutes)",
    "- **Camera:** $CameraName",
    "",
    "## Summary",
    "",
    "| Metric | Value |",
    "|--------|-------|",
    "| Total Iterations | $totalIterations |",
    "| Successful Connections | $successfulConnections |",
    "| Failed Connections | $failedConnections |",
    "| Connection Success Rate | $connectionSuccessRate% |",
    "| Average Memory (MB) | $([math]::Round($avgMemoryMB, 2)) |",
    "| Max Memory (MB) | $([math]::Round($maxMemoryMB, 2)) |",
    "| Min Memory (MB) | $([math]::Round($minMemoryMB, 2)) |",
    "| Memory Trend | $memoryLeakTrend |",
    "| Memory Leak Detected | $(if ($memoryLeakDetected) { 'вљ пёЏ Yes' } else { 'вњ… No' }) |",
    "",
    "## Memory Snapshots",
    "",
    "| Timestamp | Elapsed (s) | Memory (MB) | Handles | Threads |",
    "|-----------|-------------|-------------|---------|---------|"
)

foreach ($snapshot in $memorySnapshots | Select-Object -First 20) {
    $mdLines += "| $($snapshot.Timestamp) | $([math]::Round($snapshot.ElapsedSeconds, 0)) | $($snapshot.WorkingSetMB) | $($snapshot.Handles) | $($snapshot.Threads) |"
}

if ($memorySnapshots.Count -gt 20) {
    $mdLines += "| ... | ... | ... | ... | ... |"
}

$mdLines += @(
    "",
    "## Detailed Metrics",
    "",
    "See CSV file for full metrics: $($metricsCsvPath)",
    "",
    "## Conclusion",
    ""
)

if ($memoryLeakDetected) {
    $mdLines += "**вљ пёЏ Memory leak detected!** Memory increased by $([math]::Round($memoryIncreasePercent, 2))% during the test."
} else {
    $mdLines += "**вњ… No memory leaks detected.** Memory usage remained stable throughout the test."
}

$mdLines += ""
$mdLines += "**Connection stability:** $([math]::Round($connectionSuccessRate, 2))% success rate over $totalIterations iterations."

$mdContent = $mdLines -join "`n"
Set-Content -Path $reportMdPath -Value $mdContent -Encoding UTF8
Write-Success "Markdown report: $reportMdPath"

# JSON report
$testResults = @{
    runId = $runId
    timestamp = $timestamp
    durationSeconds = $totalDuration
    camera = $CameraName
    summary = @{
        totalIterations = $totalIterations
        successfulConnections = $successfulConnections
        failedConnections = $failedConnections
        connectionSuccessRate = $connectionSuccessRate
        avgMemoryMB = $avgMemoryMB
        maxMemoryMB = $maxMemoryMB
        minMemoryMB = $minMemoryMB
        memoryLeakDetected = $memoryLeakDetected
        memoryTrend = $memoryLeakTrend
    }
    metrics = $metrics
    memorySnapshots = $memorySnapshots
}

$testResults | ConvertTo-Json -Depth 10 | Set-Content -Path $reportJsonPath -Encoding UTF8
Write-Success "JSON report: $reportJsonPath"

# CSV metrics
$metrics | Export-Csv -Path $metricsCsvPath -NoTypeInformation -Force
Write-Success "CSV metrics: $metricsCsvPath"

# ============================================================================
# Final Summary
# ============================================================================

Write-Separator "Final Summary"
Write-Host "Duration:        $([math]::Round($totalDuration, 0)) seconds ($([math]::Round($totalDuration / 60, 1)) minutes)" -ForegroundColor White
Write-Host "Iterations:      $totalIterations" -ForegroundColor White
Write-Host "Success Rate:    $connectionSuccessRate%" -ForegroundColor $(if ($connectionSuccessRate -ge 95) { "Green" } else { "Yellow" })
Write-Host "Avg Memory:      $([math]::Round($avgMemoryMB, 2)) MB" -ForegroundColor White
Write-Host "Max Memory:      $([math]::Round($maxMemoryMB, 2)) MB" -ForegroundColor White
Write-Host "Memory Trend:    $memoryLeakTrend" -ForegroundColor $(if ($memoryLeakDetected) { "Red" } else { "Green" })
Write-Host ""
Write-Host "Reports:" -ForegroundColor Cyan
Write-Host "  Markdown: $reportMdPath" -ForegroundColor Gray
Write-Host "  JSON:     $reportJsonPath" -ForegroundColor Gray
Write-Host "  CSV:      $metricsCsvPath" -ForegroundColor Gray
Write-Host ""

if ($memoryLeakDetected) {
    Write-Warn "Memory leak detected! Review the report for details."
    exit 1
} elseif ($connectionSuccessRate -lt 95) {
    Write-Warn "Connection success rate below 95%! Review the report for details."
    exit 1
} else {
    Write-Success "Long-run test passed!"
    exit 0
}
