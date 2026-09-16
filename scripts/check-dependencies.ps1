# Скрипт проверки зависимостей
# Usage: .\scripts\check-dependencies.ps1

param(
    [switch]$ShowHelp
)

$ErrorActionPreference = "Continue"

if ($ShowHelp) {
    Write-Host "Check native-build prerequisites (CMake, FFmpeg, C++ toolchain, optional OpenCV_DIR)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\check-dependencies.ps1"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  All required tools found"
    Write-Host "  1  Missing CMake, FFmpeg, or C++ compiler (VS 2022 or MinGW g++)"
    exit 0
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Checking dependencies for build" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$allOk = $true

# Check CMake
Write-Host "Checking CMake..." -ForegroundColor Cyan
$cmakeInstalled = Get-Command cmake -ErrorAction SilentlyContinue
if ($cmakeInstalled) {
    $cmakeVersion = (cmake --version | Select-String -Pattern "version (\d+\.\d+)" | ForEach-Object { $_.Matches[0].Groups[1].Value })
    Write-Host "  CMake: OK (version $cmakeVersion)" -ForegroundColor Green
} else {
    Write-Host "  CMake: NOT FOUND" -ForegroundColor Red
    $allOk = $false
}

# Check FFmpeg
Write-Host ""
Write-Host "Checking FFmpeg..." -ForegroundColor Cyan
$ffmpegInstalled = Get-Command ffmpeg -ErrorAction SilentlyContinue
if ($ffmpegInstalled) {
    $ffmpegVersion = (ffmpeg -version | Select-String -Pattern "ffmpeg version (\S+)" | ForEach-Object { $_.Matches[0].Groups[1].Value })
    Write-Host "  FFmpeg: OK (version $ffmpegVersion)" -ForegroundColor Green

    # Check FFMPEG_DIR
    if ($env:FFMPEG_DIR) {
        Write-Host "  FFMPEG_DIR: $env:FFMPEG_DIR" -ForegroundColor Green
    } else {
        Write-Host "  FFMPEG_DIR: NOT SET (will be auto-detected)" -ForegroundColor Yellow
    }
} else {
    Write-Host "  FFmpeg: NOT FOUND" -ForegroundColor Red
    $allOk = $false
}

# Check C++ compiler
Write-Host ""
Write-Host "Checking C++ compiler..." -ForegroundColor Cyan
$vsInstalled = $false
$vsPaths = @(
    "C:\Program Files\Microsoft Visual Studio\2022\Community\Common7\IDE\devenv.exe",
    "C:\Program Files\Microsoft Visual Studio\2022\Professional\Common7\IDE\devenv.exe",
    "C:\Program Files\Microsoft Visual Studio\2022\Enterprise\Common7\IDE\devenv.exe"
)

foreach ($vsPath in $vsPaths) {
    if (Test-Path $vsPath) {
        Write-Host "  Visual Studio 2022: OK" -ForegroundColor Green
        $vsInstalled = $true
        break
    }
}

$mingwInstalled = Get-Command g++ -ErrorAction SilentlyContinue
if ($mingwInstalled) {
    $gccVersion = (g++ --version | Select-String -Pattern "(\d+\.\d+\.\d+)" | Select-Object -First 1 | ForEach-Object { $_.Matches[0].Groups[1].Value })
    Write-Host "  MinGW g++: OK (version $gccVersion)" -ForegroundColor Green
} elseif (-not $vsInstalled) {
    Write-Host "  C++ compiler: NOT FOUND" -ForegroundColor Red
    $allOk = $false
}

# Check OpenCV (optional)
Write-Host ""
Write-Host "Checking OpenCV (optional)..." -ForegroundColor Cyan
if ($env:OpenCV_DIR) {
    Write-Host "  OpenCV: OK (found via OpenCV_DIR: $env:OpenCV_DIR)" -ForegroundColor Green
} else {
    Write-Host "  OpenCV: NOT FOUND (optional, build will continue without it)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
if ($allOk) {
    Write-Host "All required dependencies are installed!" -ForegroundColor Green
    Write-Host "Ready to build native library." -ForegroundColor Green
} else {
    Write-Host "Some required dependencies are missing!" -ForegroundColor Red
    Write-Host "Run: .\scripts\install-dependencies.ps1" -ForegroundColor Yellow
}
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

if (-not $allOk) {
    exit 1
}
exit 0
