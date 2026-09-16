# Script to download and configure pre-built FFmpeg for MinGW
# Usage: .\scripts\download-ffmpeg-mingw.ps1

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "FFmpeg MinGW Download Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$downloadUrl = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"
$downloadPath = "$env:TEMP\ffmpeg-master-latest-win64-gcc.zip"
$extractPath = "C:\ffmpeg-mingw"

# Step 1: Check if already installed
Write-Host "Step 1: Checking existing installation..." -ForegroundColor Cyan
if (Test-Path "$extractPath\include\libavcodec\avcodec.h") {
    Write-Host "FFmpeg already installed at: $extractPath" -ForegroundColor Green
    Write-Host "Skipping download..." -ForegroundColor Gray
} else {
    # Step 2: Download FFmpeg
    Write-Host ""
    Write-Host "Step 2: Downloading FFmpeg..." -ForegroundColor Cyan
    Write-Host "URL: $downloadUrl" -ForegroundColor Gray
    Write-Host "Destination: $downloadPath" -ForegroundColor Gray
    
    try {
        Invoke-WebRequest -Uri $downloadUrl -OutFile $downloadPath -UseBasicParsing
        Write-Host "Download complete!" -ForegroundColor Green
    } catch {
        Write-Host "Download failed: $_" -ForegroundColor Red
        Write-Host "Please download manually from: $downloadUrl" -ForegroundColor Yellow
        exit 1
    }
    
    # Step 3: Extract
    Write-Host ""
    Write-Host "Step 3: Extracting FFmpeg..." -ForegroundColor Cyan
    Write-Host "Extracting to: $extractPath" -ForegroundColor Gray
    
    try {
        if (Test-Path $extractPath) {
            Remove-Item -Path $extractPath -Recurse -Force
        }
        New-Item -ItemType Directory -Path $extractPath -Force | Out-Null
        Expand-Archive -Path $downloadPath -DestinationPath $extractPath -Force
        
        # Move contents from nested folder if needed
        $nestedFolder = Get-ChildItem $extractPath -Directory | Select-Object -First 1
        if ($nestedFolder -and $nestedFolder.Name -like "ffmpeg-*") {
            Get-ChildItem "$($nestedFolder.FullName)\*" | Move-Item -Destination $extractPath -Force
            Remove-Item -Path $nestedFolder.FullName -Recurse -Force
        }
        
        Write-Host "Extraction complete!" -ForegroundColor Green
    } catch {
        Write-Host "Extraction failed: $_" -ForegroundColor Red
        exit 1
    }
    
    # Clean up download
    Remove-Item -Path $downloadPath -Force
}

# Step 4: Verify installation
Write-Host ""
Write-Host "Step 4: Verifying installation..." -ForegroundColor Cyan

$requiredPaths = @(
    "$extractPath\include\libavcodec\avcodec.h",
    "$extractPath\include\libavformat\avformat.h",
    "$extractPath\include\libavutil\avutil.h",
    "$extractPath\lib\libavcodec.a",
    "$extractPath\lib\libavformat.a",
    "$extractPath\lib\libavutil.a"
)

$allPresent = $true
foreach ($path in $requiredPaths) {
    if (Test-Path $path) {
        Write-Host "  вњ… $(Split-Path $path -Leaf)" -ForegroundColor Green
    } else {
        Write-Host "  вќЊ $(Split-Path $path -Leaf)" -ForegroundColor Red
        $allPresent = $false
    }
}

if (-not $allPresent) {
    Write-Host ""
    Write-Host "Some files are missing. Please check the extraction." -ForegroundColor Red
    exit 1
}

# Step 5: Configure CMake
Write-Host ""
Write-Host "Step 5: Configuring CMake..." -ForegroundColor Cyan

cd native/video-processing

# Clean build directory
if (Test-Path build) {
    Remove-Item -Path build -Recurse -Force
    Write-Host "Cleaned build directory" -ForegroundColor Gray
}

# Configure
Write-Host "Running CMake configuration..." -ForegroundColor Gray
$cmakeCommand = @"
cmake -B build/windows/mingw -G "MinGW Makefiles" -DCMAKE_BUILD_TYPE=Release `
  -DCMAKE_PREFIX_PATH="$extractPath" `
  -DCMAKE_FIND_ROOT_PATH="$extractPath"
"@

Invoke-Expression $cmakeCommand

if ($LASTEXITCODE -eq 0) {
    Write-Host "CMake configuration successful!" -ForegroundColor Green
} else {
    Write-Host "CMake configuration failed!" -ForegroundColor Red
    exit 1
}

# Step 6: Build
Write-Host ""
Write-Host "Step 6: Building native library..." -ForegroundColor Cyan
Write-Host "This may take 1-2 hours..." -ForegroundColor Yellow

$buildCommand = "cmake --build build/windows/mingw --config Release"
Invoke-Expression $buildCommand

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful!" -ForegroundColor Green
} else {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# Step 7: Copy library
Write-Host ""
Write-Host "Step 7: Copying library..." -ForegroundColor Cyan

$libDir = "native/video-processing/lib/windows/x64"
if (-not (Test-Path $libDir)) {
    New-Item -ItemType Directory -Path $libDir -Force | Out-Null
}

$libFile = "build/windows/mingw/bin/video_processing.dll"
if (Test-Path $libFile) {
    Copy-Item $libFile "$libDir\" -Force
    Write-Host "Library copied to: $libDir\video_processing.dll" -ForegroundColor Green
} else {
    Write-Host "Library not found at: $libFile" -ForegroundColor Red
    exit 1
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "FFmpeg location: $extractPath" -ForegroundColor Gray
Write-Host "Library location: $libDir\video_processing.dll" -ForegroundColor Gray
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Run tests: ./gradlew :core:network:jvmTest --tests '*RtspClient*'" -ForegroundColor Gray
Write-Host "2. Test with real cameras" -ForegroundColor Gray
Write-Host ""
