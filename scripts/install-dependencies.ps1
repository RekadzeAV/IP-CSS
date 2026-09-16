# Скрипт установки зависимостей для Windows
# Usage: .\scripts\install-dependencies.ps1

param(
    [switch]$SkipFFmpeg,
    [switch]$SkipOpenCV,
    [switch]$SkipCMake,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Install Windows build prerequisites (Chocolatey: CMake, FFmpeg, OpenCV tooling as configured)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\install-dependencies.ps1 -ShowHelp"
    Write-Host "  .\scripts\install-dependencies.ps1 [-SkipFFmpeg] [-SkipOpenCV] [-SkipCMake]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -SkipFFmpeg   Skip FFmpeg install check"
    Write-Host "  -SkipOpenCV  Skip OpenCV-related steps"
    Write-Host "  -SkipCMake   Skip CMake install check"
    Write-Host ""
    Write-Host "Notes:"
    Write-Host "  Chocolatey installs require Administrator. Script exits 1 if admin is missing when Choco install is needed."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Script finished (review output; some steps may print manual install hints)"
    Write-Host "  1  Hard failure (e.g. no admin when Chocolatey must be installed)"
    exit 0
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Installing dependencies for build" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Check admin rights
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

# Check Chocolatey
$chocoInstalled = Get-Command choco -ErrorAction SilentlyContinue

if (-not $chocoInstalled) {
    Write-Host "Chocolatey not installed. Installing Chocolatey..." -ForegroundColor Yellow

    if ($isAdmin) {
        Set-ExecutionPolicy Bypass -Scope Process -Force
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
        iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    } else {
        Write-Host "Administrator rights required for Chocolatey installation" -ForegroundColor Red
        Write-Host "Run PowerShell as Administrator or install Chocolatey manually" -ForegroundColor Yellow
        exit 1
    }
}

# Install CMake
if (-not $SkipCMake) {
    Write-Host ""
    Write-Host "Checking CMake..." -ForegroundColor Cyan
    $cmakeInstalled = Get-Command cmake -ErrorAction SilentlyContinue

    if (-not $cmakeInstalled) {
        Write-Host "CMake not found. Installing via Chocolatey..." -ForegroundColor Yellow
        if ($isAdmin) {
            choco install cmake -y
            $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        } else {
            Write-Host "Administrator rights required for CMake installation" -ForegroundColor Red
            Write-Host "Install manually: choco install cmake -y" -ForegroundColor Yellow
        }
    } else {
        $cmakeVersion = (cmake --version | Select-String -Pattern "version (\d+\.\d+)" | ForEach-Object { $_.Matches[0].Groups[1].Value })
        Write-Host "CMake already installed (version $cmakeVersion)" -ForegroundColor Green
    }
} else {
    Write-Host "Skipping CMake installation" -ForegroundColor Yellow
}

# Install FFmpeg
if (-not $SkipFFmpeg) {
    Write-Host ""
    Write-Host "Checking FFmpeg..." -ForegroundColor Cyan
    $ffmpegInstalled = Get-Command ffmpeg -ErrorAction SilentlyContinue

    if (-not $ffmpegInstalled) {
        Write-Host "FFmpeg not found. Installing via Chocolatey..." -ForegroundColor Yellow
        if ($isAdmin) {
            choco install ffmpeg -y
            $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        } else {
            Write-Host "Administrator rights required for FFmpeg installation" -ForegroundColor Red
            Write-Host "Install manually: choco install ffmpeg -y" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "Or download from https://ffmpeg.org/download.html and extract to C:\ffmpeg" -ForegroundColor Yellow
            Write-Host "Then set environment variable:" -ForegroundColor Yellow
            Write-Host '  $env:FFMPEG_DIR = "C:\ffmpeg"' -ForegroundColor Yellow
        }
    } else {
        $ffmpegVersion = (ffmpeg -version | Select-String -Pattern "ffmpeg version (\S+)" | ForEach-Object { $_.Matches[0].Groups[1].Value })
        Write-Host "FFmpeg already installed (version $ffmpegVersion)" -ForegroundColor Green

        if (-not $env:FFMPEG_DIR) {
            $ffmpegPath = (Get-Command ffmpeg).Source
            $ffmpegDir = Split-Path (Split-Path $ffmpegPath -Parent) -Parent
            Write-Host "Setting FFMPEG_DIR environment variable: $ffmpegDir" -ForegroundColor Yellow
            [System.Environment]::SetEnvironmentVariable("FFMPEG_DIR", $ffmpegDir, "User")
            $env:FFMPEG_DIR = $ffmpegDir
        }
    }
} else {
    Write-Host "Skipping FFmpeg installation" -ForegroundColor Yellow
}

# Check OpenCV (optional)
if (-not $SkipOpenCV) {
    Write-Host ""
    Write-Host "Checking OpenCV..." -ForegroundColor Cyan

    if ($env:OpenCV_DIR) {
        Write-Host "OpenCV found via environment variable: $env:OpenCV_DIR" -ForegroundColor Green
    } else {
        Write-Host "OpenCV not found. OpenCV is optional for build." -ForegroundColor Yellow
        Write-Host "To install OpenCV:" -ForegroundColor Yellow
        Write-Host "  1. Download from https://opencv.org/releases/" -ForegroundColor Yellow
        Write-Host "  2. Extract to C:\opencv" -ForegroundColor Yellow
        Write-Host "  3. Set environment variable:" -ForegroundColor Yellow
        Write-Host '     $env:OpenCV_DIR = "C:\opencv\build"' -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Or install via vcpkg:" -ForegroundColor Yellow
        Write-Host "  vcpkg install opencv:x64-windows" -ForegroundColor Yellow
    }
} else {
    Write-Host "Skipping OpenCV check" -ForegroundColor Yellow
}

# Check C++ compiler
Write-Host ""
Write-Host "Checking C++ compiler..." -ForegroundColor Cyan

# Check Visual Studio
$vsInstalled = $false
$vsPaths = @(
    "C:\Program Files\Microsoft Visual Studio\2022\Community\Common7\IDE\devenv.exe",
    "C:\Program Files\Microsoft Visual Studio\2022\Professional\Common7\IDE\devenv.exe",
    "C:\Program Files\Microsoft Visual Studio\2022\Enterprise\Common7\IDE\devenv.exe"
)

foreach ($vsPath in $vsPaths) {
    if (Test-Path $vsPath) {
        Write-Host "Visual Studio 2022 found" -ForegroundColor Green
        $vsInstalled = $true
        break
    }
}

# Check MinGW
$mingwInstalled = Get-Command g++ -ErrorAction SilentlyContinue
if ($mingwInstalled) {
    Write-Host "MinGW found" -ForegroundColor Green
} elseif (-not $vsInstalled) {
    Write-Host "C++ compiler not found!" -ForegroundColor Red
    Write-Host "Install one of the following:" -ForegroundColor Yellow
    Write-Host "  1. Visual Studio 2022 (recommended): https://visualstudio.microsoft.com/" -ForegroundColor Yellow
    Write-Host "     Make sure 'Desktop development with C++' component is installed" -ForegroundColor Yellow
    Write-Host "  2. MinGW-w64: choco install mingw -y" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Check completed!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Make sure all dependencies are installed" -ForegroundColor White
Write-Host "  2. Restart terminal to apply environment variables" -ForegroundColor White
Write-Host "  3. Build native library:" -ForegroundColor White
Write-Host "     .\scripts\build-native-lib.ps1 x64 Release" -ForegroundColor Yellow
Write-Host ""
