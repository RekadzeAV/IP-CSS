# Video Processing Library - Windows Build Script
# Usage: .\build.ps1 [-All] [-Clean] [-Verbose]

[CmdletBinding()]
param(
    [switch]$All,
    [switch]$Clean,
    [switch]$Verbose
)

# Colors
function Write-Color {
    param([string]$Text, [string]$Color)
    Write-Host $Text -ForegroundColor $Color
}

function Write-Green { Write-Color -Text $_ -Color Green }
function Write-Red { Write-Color -Text $_ -Color Red }
function Write-Yellow { Write-Color -Text $_ -Color Yellow }
function Write-Blue { Write-Color -Text $_ -Color Blue }

# Configuration
$ErrorActionPreference = "Stop"
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$PROJECT_ROOT = Split-Path -Parent $SCRIPT_DIR
$BUILD_DIR = Join-Path $PROJECT_ROOT "build"
$OUTPUT_DIR = Join-Path $PROJECT_ROOT "lib\windows"

# Parse arguments
if ($PSBoundParameters.ContainsKey('Verbose')) {
    $VerbosePreference = 'Continue'
}

Write-Green "=========================================="
Write-Green "🔧 Video Processing Library Build (Windows)"
Write-Green "=========================================="
Write-Blue "Clean: $Clean"
Write-Blue "Build All: $All"
Write-Blue ""

# Check prerequisites
Write-Blue "Checking prerequisites..."

# Check CMake
if (!(Get-Command cmake -ErrorAction SilentlyContinue)) {
    Write-Red "✗ CMake not found. Please install CMake 3.15+"
    exit 1
}
Write-Green "✓ CMake found: $(cmake --version | Select-Object -First 1)"

# Check MinGW
if (!(Get-Command mingw32-make -ErrorAction SilentlyContinue)) {
    Write-Red "✗ MinGW not found. Please install MinGW-w64"
    exit 1
}
Write-Green "✓ MinGW found"

# Check FFmpeg
$ffmpegVersion = ffmpeg -version 2>&1 | Select-Object -First 1
if ($ffmpegVersion) {
    Write-Green "✓ FFmpeg found: $ffmpegVersion"
} else {
    Write-Yellow "⚠ FFmpeg not found in PATH"
}

# Create directories
if (!(Test-Path $BUILD_DIR)) {
    New-Item -ItemType Directory -Path $BUILD_DIR | Out-Null
}
if (!(Test-Path $OUTPUT_DIR)) {
    New-Item -ItemType Directory -Path $OUTPUT_DIR | Out-Null
}

# Function to build for specific configuration
function Build-Platform {
    param(
        [string]$Platform,
        [string]$Arch,
        [string]$Preset
    )
    
    Write-Blue "Building for $Platform $Arch..."
    
    $platBuildDir = Join-Path $BUILD_DIR "$Platform-$Arch"
    if (!(Test-Path $platBuildDir)) {
        New-Item -ItemType Directory -Path $platBuildDir | Out-Null
    }
    
    Set-Location $platBuildDir
    
    # Clean if requested
    if ($Clean) {
        Write-Blue "  Cleaning build directory..."
        Remove-Item -Path * -Recurse -Force -ErrorAction SilentlyContinue
    }
    
    # Configure
    Write-Blue "  Configuring with CMake..."
    try {
        cmake --preset=$Preset 2>&1 | Out-String
    } catch {
        Write-Yellow "  Preset failed, using manual configuration..."
        
        cmake -G "MinGW Makefiles" `
              -DENABLE_FFMPEG=ON `
              -DCMAKE_BUILD_TYPE=Release `
              -DCMAKE_INSTALL_PREFIX="$OUTPUT_DIR\$Arch" `
              .. 2>&1 | Out-String
    }
    
    # Build
    Write-Blue "  Building..."
    if ($Verbose) {
        cmake --build . --config Release -j8
    } else {
        cmake --build . --config Release -j8 | Out-String
    }
    
    # Install
    Write-Blue "  Installing..."
    cmake --install . --config Release 2>&1 | Out-String
    
    # Copy library to output directory
    Write-Blue "  Copying library..."
    $libName = "video_processing.dll"
    $libPath = Join-Path $platBuildDir $libName
    
    if (Test-Path $libPath) {
        Copy-Item $libPath "$OUTPUT_DIR\$Arch\$libName" -Force
        Write-Green "  ✓ Library copied to: $OUTPUT_DIR\$Arch\$libName"
    } else {
        Write-Red "  ✗ Library not found at $libPath"
    }
    
    Set-Location $PROJECT_ROOT
    Write-Blue ""
}

# Main build logic
if ($All) {
    Write-Blue "Building for all platforms..."
    Write-Blue ""
    
    # Windows x64
    Build-Platform -Platform "windows" -Arch "x64" -Preset "windows-x64-release"
} else {
    # Build for current platform (Windows x64)
    Write-Blue "Building for Windows x64..."
    Write-Blue ""
    
    Build-Platform -Platform "windows" -Arch "x64" -Preset "windows-x64-release"
}

# Summary
Write-Green "=========================================="
Write-Green "📊 Build Summary"
Write-Green "=========================================="
Write-Blue "Output directory: $OUTPUT_DIR"
Write-Blue ""
Write-Blue "Libraries built:"
Get-ChildItem -Path $OUTPUT_DIR -Recurse -Filter "*.dll" | ForEach-Object {
    Write-Blue "  - $($_.FullName)"
}
Write-Blue ""
Write-Green "✓ Build completed successfully"
