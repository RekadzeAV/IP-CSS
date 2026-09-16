# Test RTSP Client Connection
# PowerShell script to test NativeRtspClient FFI bridge

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RTSP Client FFI Bridge Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if video_processing.dll exists
$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path | Split-Path -Parent
$dllPath = Join-Path $rootDir "native\video-processing\lib\windows\x64\video_processing.dll"
if (-not (Test-Path $dllPath)) {
    Write-Host "ERROR: video_processing.dll not found at: $dllPath" -ForegroundColor Red
    Write-Host "Please build the native library first:" -ForegroundColor Yellow
    Write-Host "  cd native/video-processing/build" -ForegroundColor Yellow
    Write-Host "  cmake --build . --config Release" -ForegroundColor Yellow
    exit 1
}

Write-Host "[1/4] Native library found: $dllPath" -ForegroundColor Green

# Check if MediaMTX is running
Write-Host ""
Write-Host "[2/4] Checking MediaMTX status..." -ForegroundColor Cyan
$mediamtxStatus = docker ps --filter "name=mediamtx" --format "{{.Status}}"
if ($mediamtxStatus -like "Up*") {
    Write-Host "  MediaMTX is running: $mediamtxStatus" -ForegroundColor Green
} else {
    Write-Host "  WARNING: MediaMTX is not running. Starting..." -ForegroundColor Yellow
    docker-compose up -d mediamtx | Out-Null
    Start-Sleep -Seconds 3
    $mediamtxStatus = docker ps --filter "name=mediamtx" --format "{{.Status}}"
    if ($mediamtxStatus -like "Up*") {
        Write-Host "  MediaMTX started successfully" -ForegroundColor Green
    } else {
        Write-Host "  ERROR: Failed to start MediaMTX" -ForegroundColor Red
        exit 1
    }
}

# Check if ports are accessible
Write-Host ""
Write-Host "[3/4] Testing RTSP port (8554)..." -ForegroundColor Cyan
try {
    $tcpClient = New-Object System.Net.Sockets.TcpClient("localhost", 8554)
    if ($tcpClient.Connected) {
        Write-Host "  RTSP port 8554 is accessible" -ForegroundColor Green
        $tcpClient.Close()
    } else {
        Write-Host "  WARNING: Could not connect to RTSP port" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  WARNING: Could not test RTSP port: $_" -ForegroundColor Yellow
}

# Run Gradle test
Write-Host ""
Write-Host "[4/4] Running NativeRtspClientBridgeJvmTest..." -ForegroundColor Cyan
Write-Host ""

$gradlew = Join-Path $PSScriptRoot "gradlew.bat"
$env:REQUIRE_NATIVE_RTSP_BRIDGE = "true"

& $gradlew.bat :core:network:desktopTest --tests "NativeRtspClientBridgeJvmTest" --no-daemon 2>&1 | ForEach-Object {
    if ($_ -match "PASSED|FAILED|SUCCESS|BUILD") {
        Write-Host $_ -ForegroundColor $(if ($_ -match "PASSED|SUCCESS|BUILD") { "Green" } else { "Red" })
    } elseif ($_ -match "ERROR|Exception") {
        Write-Host $_ -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Clean up environment variable
Remove-Item Env:\REQUIRE_NATIVE_RTSP_BRIDGE -ErrorAction SilentlyContinue
