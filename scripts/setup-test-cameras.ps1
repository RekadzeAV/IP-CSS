# Copy native/video-processing/test/test_config.json.example to test_config.json for FFmpeg/native camera tests.
# Usage: .\scripts\setup-test-cameras.ps1

param(
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Bootstrap native test_config.json from test_config.json.example (video-processing tests)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\setup-test-cameras.ps1 -ShowHelp"
    Write-Host "  .\scripts\setup-test-cameras.ps1"
    Write-Host ""
    Write-Host "Paths:"
    Write-Host "  Example: native\video-processing\test\test_config.json.example"
    Write-Host "  Target:  native\video-processing\test\test_config.json"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Created or cancelled at overwrite prompt"
    Write-Host "  1  Example file missing"
    exit 0
}

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$ConfigExample = Join-Path $ProjectRoot "native\video-processing\test\test_config.json.example"
$ConfigFile = Join-Path $ProjectRoot "native\video-processing\test\test_config.json"

Write-Host "=== Test Camera Setup ===" -ForegroundColor Cyan
Write-Host ""

# Проверка существования примера конфигурации
if (-not (Test-Path $ConfigExample)) {
    Write-Host "[FAIL] Example config not found: $ConfigExample" -ForegroundColor Red
    exit 1
}

# Проверка существования конфигурации
if (Test-Path $ConfigFile) {
    Write-Host "[WARN] Config file already exists: $ConfigFile" -ForegroundColor Yellow
    $overwrite = Read-Host "Overwrite? (y/N)"
    if ($overwrite -ne "y" -and $overwrite -ne "Y") {
        Write-Host "Aborted" -ForegroundColor Yellow
        exit 0
    }
}

Write-Host "Creating test configuration..." -ForegroundColor Cyan
Write-Host ""

# Копирование примера
Copy-Item $ConfigExample $ConfigFile -Force
Write-Host "[OK] Config file created: $ConfigFile" -ForegroundColor Green
Write-Host ""

Write-Host "=== Configuration Guide ===" -ForegroundColor Yellow
Write-Host ""
Write-Host "Please edit the config file with your camera settings:" -ForegroundColor White
Write-Host "  $ConfigFile" -ForegroundColor Gray
Write-Host ""
Write-Host "Required fields for each camera:" -ForegroundColor Cyan
Write-Host "  - name: Camera identifier" -ForegroundColor Gray
Write-Host "  - url: RTSP URL (rtsp://ip:port/path)" -ForegroundColor Gray
Write-Host "  - username: Username (optional, leave empty if not needed)" -ForegroundColor Gray
Write-Host "  - password: Password (optional)" -ForegroundColor Gray
Write-Host "  - codec: H264 or H265" -ForegroundColor Gray
Write-Host "  - resolution: WIDTHxHEIGHT (e.g., 1920x1080)" -ForegroundColor Gray
Write-Host "  - fps: Frame rate" -ForegroundColor Gray
Write-Host ""

Write-Host "Example camera entry:" -ForegroundColor Cyan
Write-Host @"
{
  "name": "Test Camera 1",
  "url": "rtsp://192.168.1.100:554/stream",
  "username": "admin",
  "password": "password",
  "codec": "H264",
  "resolution": "1920x1080",
  "fps": 25
}
"@ -ForegroundColor Gray

Write-Host ""
Write-Host "After editing, run tests with:" -ForegroundColor Yellow
Write-Host "  .\scripts\run-ffmpeg-tests.ps1 --integration" -ForegroundColor White
Write-Host ""

# Открытие файла в редакторе (опционально)
$openEditor = Read-Host "Open config file in default editor? (Y/n)"
if ($openEditor -ne "n" -and $openEditor -ne "N") {
    notepad $ConfigFile
}
