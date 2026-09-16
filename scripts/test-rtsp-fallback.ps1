# Script for testing RTSP with fallback mode
# Launches Desktop application and opens test scenarios

param(
    [switch]$SkipBuild,
    [switch]$Headless,
    [string]$TestCameraUrl = "rtsp://192.168.1.100:554/stream"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing RTSP with fallback mode" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check if we are in the project root
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "Error: Script must be run from project root" -ForegroundColor Red
    exit 1
}

# Build application (if not skipped)
if (-not $SkipBuild) {
    Write-Host "`n[1/3] Building application..." -ForegroundColor Yellow
    $buildResult = .\gradlew.bat :platforms:client-desktop-x86_64:app:assemble --no-daemon 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed!" -ForegroundColor Red
        exit 1
    }
    Write-Host "Build successful!" -ForegroundColor Green
}

# Launch application
Write-Host "`n[2/3] Launching Desktop application..." -ForegroundColor Yellow

if ($Headless) {
    Write-Host "Headless mode - application will not open" -ForegroundColor Yellow
    Write-Host "Check logs in console" -ForegroundColor Yellow
    
    # Launch in background for compilation check
    $job = Start-Job -ScriptBlock {
        cd $using:PSScriptRoot
        .\gradlew.bat :platforms:client-desktop-x86_64:app:run --no-daemon 2>&1
    }
    
    Start-Sleep -Seconds 15
    
    if ($job.State -eq "Running") {
        Write-Host "Application launched (background mode)" -ForegroundColor Green
        Write-Host "To view logs: Get-Job -Id $($job.Id) | Receive-Job" -ForegroundColor Cyan
        Write-Host "To stop: Stop-Job -Id $($job.Id); Remove-Job -Id $($job.Id)" -ForegroundColor Cyan
    }
} else {
    Write-Host "Launching GUI application..." -ForegroundColor Yellow
    .\gradlew.bat :platforms:client-desktop-x86_64:app:run --no-daemon
}

# Test camera check
Write-Host "`n[3/3] Testing recommendations..." -ForegroundColor Yellow

Write-Host @"

Test Scenarios:

1. Add test camera:
   - Open Desktop application
   - Go to "Cameras" section
   - Press "+" (Add camera)
   - Enter:
     * Name: Test Camera (Fallback)
     * URL: $TestCameraUrl
     * Login: admin (optional)
     * Password: password (optional)
   - Save

2. Check fallback video:
   - Open Live View
   - Select added camera
   - Expect:
     * Status "CONNECTING" -> "CONNECTED" -> "PLAYING"
     * Display of mock video (green/blue test pattern)
     * FPS ~25 (in telemetry)

3. Check controls:
   - Play/Pause buttons
   - Stop button
   - Screenshot button
   - Reconnect button

4. Check multi-camera:
   - Add 2-4 test cameras
   - Switch GridLayout (1, 4, 9 cameras)
   - Check stream priorities

5. Check errors:
   - Disable fallback:
     private const val ALLOW_FALLBACK_MODE = false
   - Try connecting to non-existent camera
   - Expect ERROR status with message

"@

Write-Host "`nTesting completed!" -ForegroundColor Green
Write-Host "To run again: .\scripts\test-rtsp-fallback.ps1" -ForegroundColor Cyan
