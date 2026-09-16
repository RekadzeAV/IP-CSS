# Integration test runner for RTSP client
# Requires: Docker (for MediaMTX) and FFmpeg

Write-Host "=== RTSP Integration Test Runner ===" -ForegroundColor Cyan

# Check prerequisites
Write-Host "`n[1/4] Checking prerequisites..." -ForegroundColor Yellow

# Check Docker
if (!(docker ps 2>$null)) {
    Write-Host "ERROR: Docker not running or not installed" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Docker running" -ForegroundColor Green

# Check FFmpeg
if (!(Get-Command ffmpeg -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: FFmpeg not installed" -ForegroundColor Red
    exit 1
}
Write-Host "✓ FFmpeg installed" -ForegroundColor Green

# Check Java
if (!(Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: Java not installed" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Java installed" -ForegroundColor Green

# Start MediaMTX
Write-Host "`n[2/4] Starting MediaMTX..." -ForegroundColor Yellow

$mediamtxRunning = docker ps --filter "name=mediamtx" --format "{{.Names}}" | Select-String "mediamtx"
if (!$mediamtxRunning) {
    docker-compose up -d mediamtx 2>$null
    Start-Sleep -Seconds 3
}
Write-Host "✓ MediaMTX started" -ForegroundColor Green

# Start FFmpeg test stream
Write-Host "`n[3/4] Starting FFmpeg test stream..." -ForegroundColor Yellow

# Stop any existing test stream
Get-Process ffmpeg -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

Start-Process -FilePath "ffmpeg" -ArgumentList @("-re", "-f", "lavfi", "-i", "testsrc=duration=120:size=1920x1080:rate=30", "-f", "lavfi", "-i", "sine=frequency=440:duration=120", "-c:v", "libx264", "-preset", "ultrafast", "-tune", "zerolatency", "-c:a", "aac", "-f", "rtsp", "rtsp://localhost:8554/test") -NoNewWindow -PassThru | Out-Null

Start-Sleep -Seconds 2
Write-Host "✓ FFmpeg test stream started" -ForegroundColor Green

# Run Gradle tests
Write-Host "`n[4/4] Running integration tests..." -ForegroundColor Yellow

$env:REQUIRE_NATIVE_RTSP_BRIDGE = "true"
.\gradlew.bat :core:network:desktopTest --tests "*NativeRtspClientStreamIntegrationTest*" --no-daemon 2>&1 | Select-String -Pattern "PASSED|FAILED|SUCCESS|BUILD|tests completed"

$testResult = $LASTEXITCODE

# Cleanup
Write-Host "`n[Cleanup] Stopping test stream..." -ForegroundColor Yellow
Get-Process ffmpeg -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

Write-Host "✓ Cleanup complete" -ForegroundColor Green

if ($testResult -eq 0) {
    Write-Host "`n=== ALL TESTS PASSED ===" -ForegroundColor Green
} else {
    Write-Host "`n=== SOME TESTS FAILED ===" -ForegroundColor Red
}

exit $testResult
