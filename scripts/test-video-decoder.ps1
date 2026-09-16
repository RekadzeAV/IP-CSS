# Run VideoDecoder JVM tests (unit; integration if TEST_CAMERA_* URLs are set in the environment).
# Prerequisite: dot-source .\scripts\setup-test-environment.ps1 or set env vars manually.

param(
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Run Gradle VideoDecoderPerformanceTest; optionally VideoDecoderIntegrationTest when camera URLs are set."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\test-video-decoder.ps1 -ShowHelp"
    Write-Host "  . .\scripts\setup-test-environment.ps1; .\scripts\test-video-decoder.ps1"
    Write-Host ""
    Write-Host "Environment (examples):"
    Write-Host "  `$env:TEST_CAMERA_H264_URL='rtsp://host:554/stream'"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  Non-zero  Propagated from gradlew.bat"
    exit 0
}

Write-Host "=== VideoDecoder Testing Script ===" -ForegroundColor Cyan
Write-Host ""

# Проверка переменных окружения
$hasCameraUrl = $env:TEST_CAMERA_H264_URL -or $env:TEST_CAMERA_H265_URL -or $env:TEST_CAMERA_MJPEG_URL

if (-not $hasCameraUrl) {
    Write-Host "WARNING: No camera URLs configured in environment variables" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To run tests, set the following environment variables:" -ForegroundColor Yellow
    Write-Host "  `$env:TEST_CAMERA_H264_URL='rtsp://camera-ip:554/stream'" -ForegroundColor Yellow
    Write-Host "  `$env:TEST_CAMERA_H265_URL='rtsp://camera-ip:554/stream'" -ForegroundColor Yellow
    Write-Host "  `$env:TEST_CAMERA_MJPEG_URL='rtsp://camera-ip:554/stream'" -ForegroundColor Yellow
    Write-Host "  `$env:TEST_CAMERA_USERNAME='admin' (optional)" -ForegroundColor Yellow
    Write-Host "  `$env:TEST_CAMERA_PASSWORD='password' (optional)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Running unit tests only..." -ForegroundColor Yellow
    Write-Host ""
}

# Запуск unit тестов
Write-Host "Running VideoDecoder unit tests..." -ForegroundColor Green
& .\gradlew.bat :core:network:jvmTest --tests "com.company.ipcamera.core.network.video.VideoDecoderPerformanceTest"

# Запуск integration тестов (если настроены камеры)
if ($hasCameraUrl) {
    Write-Host ""
    Write-Host "Running VideoDecoder integration tests with real cameras..." -ForegroundColor Green
    & .\gradlew.bat :core:network:jvmTest --tests "com.company.ipcamera.core.network.video.VideoDecoderIntegrationTest"
}

Write-Host ""
Write-Host "=== Testing Complete ===" -ForegroundColor Cyan
