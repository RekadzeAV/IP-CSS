# Build OpenCV from source for Windows x64 with MSVC
# This script builds OpenCV 4.x with FFmpeg support

$ErrorActionPreference = "Stop"

Write-Host "=== Building OpenCV from Source ===" -ForegroundColor Cyan
Write-Host "Start time: $(Get-Date)" -ForegroundColor Yellow

# Configuration
$OPENCV_SOURCE = "C:\OpenCV"
$OPENCV_BUILD = "C:\OpenCV\build\vs2022"
$OPENCV_INSTALL = "C:\OpenCV\install"
$GENERATOR = "Visual Studio 17 2022"
$PLATFORM = "x64"
$CONFIGURATION = "Release"
$SAFE_JOBS = 4  # Based on available memory

# Create build directory
if (Test-Path $OPENCV_BUILD) {
    Write-Host "Removing existing build directory..." -ForegroundColor Yellow
    Remove-Item -Path $OPENCV_BUILD -Recurse -Force
}
New-Item -ItemType Directory -Path $OPENCV_BUILD | Out-Null

# Set up MSVC environment
$vcVarsPath = "C:\Program Files\Microsoft Visual Studio\18\Community\VC\Auxiliary\Build\vcvarsall.bat"
if (-not (Test-Path $vcVarsPath)) {
    Write-Host "ERROR: vcvarsall.bat not found at $vcVarsPath" -ForegroundColor Red
    exit 1
}

Write-Host "Using MSVC from: $vcVarsPath" -ForegroundColor Green

# Find FFmpeg
$FFMPEG_PATH = "C:\Users\Rekad\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\bin"
$FFMPEG_LIB_PATH = "C:\Users\Rekad\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\lib"
$FFMPEG_INC_PATH = "C:\Users\Rekad\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\include"

if (Test-Path "$FFMPEG_INC_PATH") {
    Write-Host "Found FFmpeg at: $FFMPEG_PATH" -ForegroundColor Green
    $FFMPEG_INCLUDE = "$FFMPEG_INC_PATH"
    $FFMPEG_LIB = "$FFMPEG_LIB_PATH"
    $USE_FFMPEG = $true
} else {
    Write-Host "WARNING: FFmpeg headers not found, building without FFmpeg support" -ForegroundColor Yellow
    $FFMPEG_INCLUDE = ""
    $FFMPEG_LIB = ""
    $USE_FFMPEG = $false
}

# CMake configuration
Write-Host "`n=== Configuring OpenCV with CMake ===" -ForegroundColor Cyan
Set-Location $OPENCV_BUILD

$cmakeArgs = @(
    $OPENCV_SOURCE,
    "-G", "`"$GENERATOR`"",
    "-A", $PLATFORM,
    "-DCMAKE_BUILD_TYPE=Release",
    "-DCMAKE_INSTALL_PREFIX=$OPENCV_INSTALL",
    "-DBUILD_SHARED_LIBS=ON",
    "-DBUILD_EXAMPLES=OFF",
    "-DBUILD_TESTS=OFF",
    "-DBUILD_PERF_TESTS=OFF",
    "-DBUILD_DOCS=OFF",
    "-DWITH_IPP=OFF",
    "-DWITH_TBB=OFF",
    "-DWITH_OPENMP=ON",
    "-DWITH_V4L=OFF",
    "-DWITH_DIRECTX=ON",
    "-DWITH_D3D11=ON",
    "-DWITH_D3D12=ON",
    "-DWITH_MSMF=ON",
    "-DCMAKE_POLICY_DEFAULT_CMP0091=NEW"
)

# Add FFmpeg support if found
if ($USE_FFMPEG) {
    $cmakeArgs += "-DWITH_FFMPEG=ON"
    $cmakeArgs += "-DFFMPEG_INCLUDE_DIR=$FFMPEG_INCLUDE"
    $cmakeArgs += "-DFFMPEG_LIBRARY_DIR=$FFMPEG_LIB"
} else {
    $cmakeArgs += "-DWITH_FFMPEG=OFF"
}

Write-Host "Running CMake with arguments:" -ForegroundColor Yellow
$cmakeArgs | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }

# Run CMake
$cmakeResult = Start-Process "cmake.exe" -ArgumentList $cmakeArgs -Wait -NoNewWindow -PassThru
if ($cmakeResult.ExitCode -ne 0) {
    Write-Host "CMake configuration failed!" -ForegroundColor Red
    Get-Content "CMakeCache.txt" | Select-String "Error" | Select-Object -First 10
    exit 1
}

Write-Host "CMake configuration completed successfully" -ForegroundColor Green

# Build OpenCV
Write-Host "`n=== Building OpenCV (this will take 20-40 minutes) ===" -ForegroundColor Cyan
Write-Host "Configuration: $CONFIGURATION, Jobs: $SAFE_JOBS" -ForegroundColor Yellow

# Run MSBuild via cmake
$buildResult = Start-Process "cmake.exe" -ArgumentList "--build", ".", "--config", $CONFIGURATION, "-j", $SAFE_JOBS, "--target", "install" -Wait -NoNewWindow -PassThru

if ($buildResult.ExitCode -ne 0) {
    Write-Host "OpenCV build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "OpenCV build completed successfully" -ForegroundColor Green

# Verify installation
Write-Host "`n=== Verifying Installation ===" -ForegroundColor Cyan
if (Test-Path "$OPENCV_INSTALL\lib") {
    $libFiles = Get-ChildItem "$OPENCV_INSTALL\lib" -Filter "*.lib" | Select-Object -First 5
    Write-Host "Libraries installed to: $OPENCV_INSTALL\lib" -ForegroundColor Green
    $libFiles | ForEach-Object { Write-Host "  - $($_.Name)" -ForegroundColor Gray }
}

if (Test-Path "$OPENCV_INSTALL\include\opencv2") {
    Write-Host "Headers installed to: $OPENCV_INSTALL\include" -ForegroundColor Green
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "OpenCV Build Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Source: $OPENCV_SOURCE" -ForegroundColor White
Write-Host "Build dir: $OPENCV_BUILD" -ForegroundColor White
Write-Host "Install: $OPENCV_INSTALL" -ForegroundColor White
Write-Host "FFmpeg: $(if ($FFMPEG_INCLUDE) { 'Enabled' } else { 'Disabled' })" -ForegroundColor White
Write-Host "End time: $(Get-Date)" -ForegroundColor Yellow
Write-Host "========================================`n" -ForegroundColor Cyan

exit 0
Установи Desktop development with C++