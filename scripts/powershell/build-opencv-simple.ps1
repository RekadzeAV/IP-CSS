# Build OpenCV using vcvarsall.bat to set up MSVC environment
$ErrorActionPreference = "Stop"

Write-Host "=== Building OpenCV ===" -ForegroundColor Cyan

# Set up MSVC environment
$vcVarsPath = "C:\Program Files\Microsoft Visual Studio\18\Community\VC\Auxiliary\Build\vcvarsall.bat"
if (-not (Test-Path $vcVarsPath)) {
    Write-Host "ERROR: vcvarsall.bat not found" -ForegroundColor Red
    exit 1
}

Write-Host "Using vcvarsall.bat: $vcVarsPath" -ForegroundColor Green

# Create build directory
$BUILD_DIR = "C:\OpenCV\build\vs2022"
if (Test-Path $BUILD_DIR) {
    Remove-Item -Path $BUILD_DIR -Recurse -Force
}
New-Item -ItemType Directory -Path $BUILD_DIR | Out-Null

# FFmpeg paths
$FFMPEG_INC = "C:\Users\Rekad\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\include"
$FFMPEG_LIB = "C:\Users\Rekad\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\lib"

Write-Host "`n=== Step 1: CMake Configuration ===" -ForegroundColor Cyan

# Run cmake from within VS environment
$cmakeCmd = @"
call "`"$vcVarsPath`"" x64 && `
cmake -S "C:\OpenCV" -B "$BUILD_DIR" -G "Visual Studio 17 2022" -A x64 `
  -DCMAKE_BUILD_TYPE=Release `
  -DCMAKE_INSTALL_PREFIX="C:\OpenCV\install" `
  -DBUILD_SHARED_LIBS=ON `
  -DBUILD_EXAMPLES=OFF `
  -DBUILD_TESTS=OFF `
  -DBUILD_PERF_TESTS=OFF `
  -DWITH_FFMPEG=ON `
  -DFFMPEG_INCLUDE_DIR="$FFMPEG_INC" `
  -DFFMPEG_LIBRARY_DIR="$FFMPEG_LIB"
"@

Invoke-Expression $cmakeCmd

if ($LASTEXITCODE -ne 0) {
    Write-Host "CMake configuration failed!" -ForegroundColor Red
    exit 1
}

Write-Host "CMake configuration completed successfully" -ForegroundColor Green

Write-Host "`n=== Step 2: Building OpenCV (20-40 minutes) ===" -ForegroundColor Cyan
Write-Host "This will compile all OpenCV modules. You can monitor progress in Task Manager." -ForegroundColor Yellow

# Build with MSBuild
$buildCmd = @"
call "`"$vcVarsPath`"" x64 && `
cmake --build "$BUILD_DIR" --config Release -j4 --target install
"@

Invoke-Expression $buildCmd

if ($LASTEXITCODE -ne 0) {
    Write-Host "OpenCV build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== OpenCV Build Completed Successfully ===" -ForegroundColor Green
Write-Host "Installation directory: C:\OpenCV\install" -ForegroundColor Cyan
Write-Host "Libraries: C:\OpenCV\install\lib" -ForegroundColor Cyan
Write-Host "Headers: C:\OpenCV\install\include" -ForegroundColor Cyan

exit 0
