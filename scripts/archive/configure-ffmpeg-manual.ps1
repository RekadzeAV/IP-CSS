# Script to configure FFmpeg paths for manual installation
# Usage: .\scripts\configure-ffmpeg-manual.ps1

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Manual FFmpeg Configuration Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# FFmpeg paths for different installations
$ffmpegPaths = @{
    "WinGet" = "C:\Users\Rekad\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build"
    "Chocolatey" = "C:\ProgramData\chocolatey\lib\ffmpeg\tools\ffmpeg\bin"
    "Manual" = "C:\ffmpeg"
}

Write-Host "Available FFmpeg installations:" -ForegroundColor Cyan
$ffmpegPaths.Keys | ForEach-Object {
    $path = $ffmpegPaths[$_]
    $exists = Test-Path $path
    $status = if ($exists) { "[OK]" } else { "[NOT FOUND]" }
    Write-Host "  $_ : $status" -ForegroundColor $(if($exists){"Green"}else{"Yellow"})
    Write-Host "       $path" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Note: The WinGet version has only runtime binaries (no headers/libs)" -ForegroundColor Yellow
Write-Host "For development, you need FFmpeg with headers and import libraries." -ForegroundColor Yellow
Write-Host ""

# Check if vcpkg is available
$vcpkgPath = "$env:USERPROFILE\vcpkg"
if (Test-Path $vcpkgPath) {
    Write-Host "vcpkg is available at: $vcpkgPath" -ForegroundColor Green
    Write-Host "Recommendation: Use vcpkg for FFmpeg development libraries" -ForegroundColor Cyan
    Write-Host "Command: cd $vcpkgPath; .\vcpkg.exe install ffmpeg:x64-windows" -ForegroundColor Gray
} else {
    Write-Host "vcpkg not found. Consider installing it:" -ForegroundColor Yellow
    Write-Host "git clone https://github.com/microsoft/vcpkg.git $env:USERPROFILE\vcpkg" -ForegroundColor Gray
    Write-Host ".\$env:USERPROFILE\vcpkg\bootstrap-vcpkg.bat" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Configuration Options" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Option 1: Wait for vcpkg installation (recommended)" -ForegroundColor Cyan
Write-Host "  - Highest compatibility" -ForegroundColor Gray
Write-Host "  - Automatic dependency management" -ForegroundColor Gray
Write-Host "  - Time: 20-40 minutes for initial build" -ForegroundColor Gray
Write-Host ""
Write-Host "Option 2: Download FFmpeg development build" -ForegroundColor Cyan
Write-Host "  - Visit: https://www.gyan.dev/ffmpeg/builds/" -ForegroundColor Gray
Write-Host "  - Download: ffmpeg-8.1.1-full_build.zip (with headers)" -ForegroundColor Gray
Write-Host "  - Extract to: C:\ffmpeg" -ForegroundColor Gray
Write-Host ""
Write-Host "Option 3: Use vcpkg with binary caching (faster)" -ForegroundColor Cyan
Write-Host "  - vcpkg provides pre-built binaries" -ForegroundColor Gray
Write-Host "  - Command: .\vcpkg.exe install ffmpeg:x64-windows --x-binarysource=clear" -ForegroundColor Gray
Write-Host ""
