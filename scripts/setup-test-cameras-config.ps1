# Create repo-root test-cameras-config.json (template for camera integration tests).
# Usage: .\scripts\setup-test-cameras-config.ps1

param(
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Write test-cameras-config.json at repository root (JSON template; may prompt before overwrite)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\setup-test-cameras-config.ps1 -ShowHelp"
    Write-Host "  .\scripts\setup-test-cameras-config.ps1"
    Write-Host ""
    Write-Host "Output:"
    Write-Host "  <repo>\test-cameras-config.json"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Created, overwritten, or cancelled at prompt"
    Write-Host "  Non-zero  Write error"
    exit 0
}

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$ConfigFile = Join-Path $ProjectRoot "test-cameras-config.json"

Write-Host "=== Test Cameras Configuration Setup ===" -ForegroundColor Cyan
Write-Host ""

# Создание конфигурационного файла
$config = @{
    cameras = @(
        @{
            id = "camera-1"
            name = "Test Camera 1"
            url = ""
            username = ""
            password = ""
            codec = "H264"
            resolution = "1920x1080"
            enabled = $false
        },
        @{
            id = "camera-2"
            name = "Test Camera 2"
            url = ""
            username = ""
            password = ""
            codec = "H264"
            resolution = "1280x720"
            enabled = $false
        },
        @{
            id = "camera-3"
            name = "Test Camera 3"
            url = ""
            username = ""
            password = ""
            codec = "H265"
            resolution = "1920x1080"
            enabled = $false
        }
    )
    testSettings = @{
        durationSeconds = 60
        collectMetrics = $true
        metricsInterval = 10
        testProfiles = @("Baseline", "Main", "High")
    }
} | ConvertTo-Json -Depth 10

if (Test-Path $ConfigFile) {
    Write-Host "Configuration file already exists: $ConfigFile" -ForegroundColor Yellow
    $overwrite = Read-Host "Overwrite? (y/N)"
    if ($overwrite -ne "y" -and $overwrite -ne "Y") {
        Write-Host "Cancelled." -ForegroundColor Gray
        exit 0
    }
}

Set-Content -Path $ConfigFile -Value $config

Write-Host "[OK] Configuration file created: $ConfigFile" -ForegroundColor Green
Write-Host ""
Write-Host "Please edit the configuration file and add your camera URLs:" -ForegroundColor Yellow
Write-Host "  1. Open: $ConfigFile" -ForegroundColor White
Write-Host "  2. Fill in camera URLs, usernames, and passwords" -ForegroundColor White
Write-Host "  3. Set enabled = true for cameras you want to test" -ForegroundColor White
Write-Host ""
Write-Host "Example:" -ForegroundColor Cyan
Write-Host '  "url": "rtsp://192.168.1.100:554/stream1"' -ForegroundColor Gray
Write-Host '  "username": "admin"' -ForegroundColor Gray
Write-Host '  "password": "password"' -ForegroundColor Gray
