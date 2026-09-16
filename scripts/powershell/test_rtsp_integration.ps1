# RTSP Integration Test Script
# Дата: 11 June 2026
# Описание: Автоматизированное тестирование RTSP client с MediaMTX

param(
    [string]$MediaMTXContainer = "ip-camera-mediamtx",
    [string]$RTSPPath = "test",
    [int]$TestDurationSeconds = 30,
    [switch]$SkipFFmpeg,
    [switch]$Verbose
)

# Цвета для вывода
$Colors = @{
    Info     = "Cyan"
    Success  = "Green"
    Warning  = "Yellow"
    Error    = "Red"
    Header   = "Magenta"
}

function Write-Log {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    $timestamp = Get-Date -Format "HH:mm:ss"
    Write-Host "[$timestamp] $Message" -ForegroundColor $Color
}

function Test-MediaMTXRunning {
    try {
        $status = docker ps --filter "name=$MediaMTXContainer" --format "{{.Status}}"
        return $status -like "*Up*"
    }
    catch {
        return $false
    }
}

function Start-MediaMTX {
    Write-Log "Starting MediaMTX container..." $Colors Info
    docker start $MediaMTXContainer | Out-Null
    Start-Sleep -Seconds 3
    
    if (Test-MediaMTXRunning) {
        Write-Log "MediaMTX is running" $Colors Success
        return $true
    }
    else {
        Write-Log "Failed to start MediaMTX" $Colors Error
        return $false
    }
}

function Publish-TestStream {
    param(
        [int]$Duration = 30
    )
    
    Write-Log "Publishing test stream (duration: ${Duration}s)..." $Colors Info
    
    $ffmpegArgs = @(
        "-f", "lavfi"
        "-i", "testsrc=duration=${Duration}:size=1920x1080:rate=25"
        "-c:v", "libx264"
        "-preset", "ultrafast"
        "-f", "rtsp"
        "rtsp://localhost:8554/$RTSPPath"
    )
    
    $ffmpegProcess = Start-Process ffmpeg -ArgumentList $ffmpegArgs -PassThru -WindowStyle Hidden
    
    Write-Log "FFmpeg started (PID: $($ffmpegProcess.Id))" $Colors Success
    return $ffmpegProcess
}

function Wait-For-StreamReady {
    param(
        [int]$TimeoutSeconds = 10
    )
    
    Write-Log "Waiting for stream to be ready..." $Colors Info
    
    $startTime = Get-Date
    while ((Get-Date) - $startTime -lt [TimeSpan]::FromSeconds($TimeoutSeconds)) {
        $logs = docker logs $MediaMTXContainer --tail 20 2>&1 | Select-String -Pattern "ready|running"
        
        if ($logs) {
            Write-Log "Stream is ready" $Colors Success
            return $true
        }
        
        Start-Sleep -Seconds 1
    }
    
    Write-Log "Stream not ready after ${TimeoutSeconds}s" $Colors Warning
    return $false
}

function Run-IntegrationTests {
    Write-Log "Running integration tests..." $Colors Info
    
    $testCommand = ".\gradlew.bat :core:network:desktopTest --tests *NativeRtspClientLiveFrameTest* --no-daemon"
    
    if ($Verbose) {
        Invoke-Expression $testCommand
    }
    else {
        $testOutput = Invoke-Expression $testCommand 2>&1
        $testOutput | Select-String -Pattern "PASSED|FAILED|BUILD" | ForEach-Object { Write-Log $_.ToString() }
    }
    
    return $LASTEXITCODE
}

function Cleanup {
    param(
        $FFmpegProcess
    )
    
    Write-Log "Cleaning up..." $Colors Info
    
    if ($FFmpegProcess) {
        try {
            Stop-Process -Id $FFmpegProcess.Id -Force -ErrorAction SilentlyContinue
            Write-Log "FFmpeg stopped" $Colors Success
        }
        catch {
            Write-Log "Failed to stop FFmpeg: $_" $Colors Warning
        }
    }
}

function Show-TestSummary {
    param(
        [bool]$TestPassed,
        [DateTime]$StartTime,
        [DateTime]$EndTime
    )
    
    $duration = $EndTime - $StartTime
    
    Write-Log "`n=== Test Summary ===" $Colors Header
    Write-Log "Start Time: $($StartTime.ToString('HH:mm:ss'))"
    Write-Log "End Time:   $($EndTime.ToString('HH:mm:ss'))"
    Write-Log "Duration:   $($duration.TotalSeconds.ToString('F2'))s"
    
    if ($TestPassed) {
        Write-Log "Result:     PASSED" $Colors Success
    }
    else {
        Write-Log "Result:     FAILED" $Colors Error
    }
}

# ============================================
# Main Test Execution
# ============================================

$startTime = Get-Date

Write-Log "`n=== RTSP Integration Test Suite ===" $Colors Header
Write-Log "MediaMTX Container: $MediaMTXContainer"
Write-Log "RTSP Path: /$RTSPPath"
Write-Log "Test Duration: ${TestDurationSeconds}s"

# Step 1: Проверка MediaMTX
Write-Log "`n[1/5] Checking MediaMTX..." $Colors Info

if (-not (Test-MediaMTXRunning)) {
    Write-Log "MediaMTX is not running, attempting to start..." $Colors Warning
    
    if (-not (Start-MediaMTX)) {
        Write-Log "Cannot proceed without MediaMTX" $Colors Error
        exit 1
    }
}
else {
    Write-Log "MediaMTX is already running" $Colors Success
}

# Step 2: Публикация тестового видео
$ffmpegProcess = $null

if (-not $SkipFFmpeg) {
    Write-Log "`n[2/5] Publishing test stream..." $Colors Info
    $ffmpegProcess = Publish-TestStream -Duration $TestDurationSeconds
    
    # Ждем что поток опубликован
    Start-Sleep -Seconds 2
    
    if (-not (Wait-For-StreamReady -TimeoutSeconds 10)) {
        Write-Log "Stream may not be ready, continuing anyway..." $Colors Warning
    }
}
else {
    Write-Log "`n[2/5] Skipping FFmpeg (stream should already be published)" $Colors Warning
}

# Step 3: Проверка DLL
Write-Log "`n[3/5] Checking native DLL..." $Colors Info

$dllPath = "native/video-processing/lib/windows/x64/video_processing.dll"
if (Test-Path $dllPath) {
    $dllInfo = Get-Item $dllPath
    Write-Log "DLL found: $($dllInfo.LastWriteTime)" $Colors Success
}
else {
    Write-Log "DLL not found at $dllPath" $Colors Error
    Cleanup -FFmpegProcess $ffmpegProcess
    exit 1
}

# Step 4: Запуск тестов
Write-Log "`n[4/5] Running integration tests..." $Colors Info

$testResult = Run-IntegrationTests

if ($testResult -eq 0) {
    Write-Log "All tests PASSED" $Colors Success
}
else {
    Write-Log "Some tests FAILED (exit code: $testResult)" $Colors Error
}

# Step 5: Очистка
Write-Log "`n[5/5] Cleaning up..." $Colors Info
Cleanup -FFmpegProcess $ffmpegProcess

# Summary
$endTime = Get-Date
Show-TestSummary -TestPassed ($testResult -eq 0) -StartTime $startTime -EndTime $endTime

Write-Log "`n=== Test Suite Complete ===" $Colors Header

# Exit с кодом результата тестов
exit $testResult
