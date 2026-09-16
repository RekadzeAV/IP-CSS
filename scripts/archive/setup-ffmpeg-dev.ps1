# Script to install FFmpeg development libraries via vcpkg
# Usage: .\scripts\setup-ffmpeg-dev.ps1

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "FFmpeg Development Setup Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check if vcpkg is installed
$vcpkgPath = "$env:USERPROFILE\vcpkg"

if (-not (Test-Path $vcpkgPath)) {
    Write-Host "vcpkg not found. Installing vcpkg..." -ForegroundColor Yellow
    
    $vcpkgParent = Split-Path $vcpkgPath -Parent
    if (-not (Test-Path $vcpkgParent)) {
        New-Item -ItemType Directory -Path $vcpkgParent -Force | Out-Null
    }
    
    git clone https://github.com/microsoft/vcpkg.git $vcpkgPath
    Set-Location $vcpkgPath
    .\bootstrap-vcpkg.bat
    Set-Location -
} else {
    Write-Host "vcpkg found at: $vcpkgPath" -ForegroundColor Green
}

# Step 2: Install FFmpeg with all required components
Write-Host ""
Write-Host "Installing FFmpeg development libraries..." -ForegroundColor Cyan

Set-Location $vcpkgPath

# Install ffmpeg for x64-windows
Write-Host "Installing ffmpeg:x64-windows..." -ForegroundColor Gray
.\vcpkg.exe install ffmpeg:x64-windows

# Verify installation
Write-Host ""
Write-Host "Verifying FFmpeg installation..." -ForegroundColor Cyan
$ffmpegStatus = .\vcpkg.exe list ffmpeg*

if ($ffmpegStatus -match "ffmpeg:x64-windows\s*\[ok\]") {
    Write-Host "FFmpeg installed successfully!" -ForegroundColor Green
    
    # Get installation path
    $installedPackages = .\vcpkg.exe list ffmpeg:x64-windows
    $ffmpegPath = "$vcpkgPath\installed\x64-windows"
    
    Write-Host ""
    Write-Host "FFmpeg installation path: $ffmpegPath" -ForegroundColor Gray
    Write-Host "Include dir: $ffmpegPath\include" -ForegroundColor Gray
    Write-Host "Lib dir: $ffmpegPath\lib" -ForegroundColor Gray
    Write-Host ""
    
    # Create environment variable
    Write-Host "Setting FFMPEG_DIR environment variable..." -ForegroundColor Cyan
    [Environment]::SetEnvironmentVariable("FFMPEG_DIR", $ffmpegPath, "User")
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Installation Complete!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. Restart your terminal to apply environment variables" -ForegroundColor Yellow
    Write-Host "2. Run: \$env:FFMPEG_DIR = '$ffmpegPath'" -ForegroundColor Yellow
    Write-Host "3. Build native library: cmake -B build/windows/x64 ..." -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host "FFmpeg installation may have failed. Please check the output above." -ForegroundColor Red
    exit 1
}

Set-Location -
