# Build video-processing library for iOS (PowerShell)
# Usage: .\build-ios.ps1 [-Arch arm64|x64|simulator-arm64] [-BuildType Release|Debug]

param(
    [ValidateSet("arm64", "x64", "simulator-arm64")]
    [string]$Arch = "arm64",
    
    [ValidateSet("Release", "Debug")]
    [string]$BuildType = "Release"
)

# Colors
function Write-Color {
    param([string]$Text, [string]$Color)
    $original = $Host.UI.RawUI.ForegroundColor
    $Host.UI.RawUI.ForegroundColor = $Color
    Write-Host $Text
    $Host.UI.RawUI.ForegroundColor = $original
}

$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BuildDir = Join-Path $ProjectDir "build\ios\$Arch"
$OutputDir = Join-Path $ProjectDir "lib\ios\$Arch"

Write-Color "==========================================" -Color Cyan
Write-Color "iOS Build Script (PowerShell)" -Color Cyan
Write-Color "==========================================" -Color Cyan
Write-Host ""
Write-Host "Architecture: $Arch"
Write-Host "Build Type: $BuildType"
Write-Host "Build Directory: $BuildDir"
Write-Host "Output Directory: $OutputDir"
Write-Host ""

# Determine platform
switch ($Arch) {
    "arm64" {
        $Platform = "iPhoneOS"
        $SdkPath = "/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS.sdk"
    }
    "x64" {
        $Platform = "iPhoneSimulator"
        $SdkPath = "/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator.sdk"
    }
    "simulator-arm64" {
        $Platform = "iPhoneSimulator"
        $SdkPath = "/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator.sdk"
    }
}

Write-Host "Platform: $Platform"
Write-Host "SDK Path: $SdkPath"
Write-Host ""

# Create directories
New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

# Configure CMake
Write-Color "Configuring CMake..." -Color Blue
Set-Location $BuildDir

cmake .. `
    -G Xcode `
    -DCMAKE_SYSTEM_NAME=iOS `
    -DCMAKE_OSX_DEPLOYMENT_TARGET=13.0 `
    -DCMAKE_OSX_ARCHITECTURES=$Arch `
    -DCMAKE_OSX_SYSROOT=$SdkPath `
    -DCMAKE_BUILD_TYPE=$BuildType `
    -DBUILD_SHARED_LIBS=ON `
    -DBUILD_STATIC_LIBS=OFF `
    -DENABLE_H264=ON `
    -DENABLE_H265=ON `
    -DENABLE_MJPEG=ON `
    -DENABLE_AAC=ON `
    -DENABLE_G711=ON `
    -DCMAKE_INSTALL_PREFIX=$OutputDir

# Build
Write-Color "Building library..." -Color Blue
cmake --build . --config $BuildType

# Install
Write-Color "Installing library..." -Color Blue
cmake --install . --config $BuildType

# Verify output
Write-Host ""
Write-Color "==========================================" -Color Cyan
Write-Color "Build Results" -Color Cyan
Write-Color "==========================================" -Color Cyan

$LibPath = Join-Path $OutputDir "libvideo_processing.dylib"

if (Test-Path $LibPath) {
    $LibSize = (Get-Item $LibPath).Length
    Write-Color "✓ Library created: libvideo_processing.dylib" -Color Green
    Write-Host "  Size: $LibSize bytes"
    Write-Host "  Location: $OutputDir"
    
    Write-Host ""
    Write-Host "File Info:"
    file $LibPath
} else {
    Write-Color "✗ Library not found at: $LibPath" -Color Red
    exit 1
}

Write-Host ""
Write-Color "✓ iOS build completed successfully!" -Color Green
Write-Host ""
