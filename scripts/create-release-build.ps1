# Create Release Build Script
# Скрипт для создания Release Build IP-CSS RTSP Client

param(
    [string]$Version = "1.0.0",
    [string]$Platform = "x64",
    [switch]$CreateInstaller,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
Create Release Build for IP-CSS RTSP Client

Usage:
  .\scripts\create-release-build.ps1
  .\scripts\create-release-build.ps1 -Version 1.0.0
  .\scripts\create-release-build.ps1 -Platform x64
  .\scripts\create-release-build.ps1 -CreateInstaller

Options:
  -Version        Version number (default: 1.0.0)
  -Platform       Build platform: x64, x86, arm64 (default: x64)
  -CreateInstaller Create installer (default: false)
  -ShowHelp       Show this help message

Examples:
  # Create release build
  .\scripts\create-release-build.ps1

  # Create release build with specific version
  .\scripts\create-release-build.ps1 -Version 1.0.1

  # Create release build with installer
  .\scripts\create-release-build.ps1 -CreateInstaller
"@
    exit 0
}

# ============================================================================
# Setup
# ============================================================================

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$buildDir = Join-Path $projectRoot "build-release"
$releaseDir = Join-Path $projectRoot "release-build"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "IP-CSS RTSP Client Release Build" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Version: $Version" -ForegroundColor White
Write-Host "Platform: $Platform" -ForegroundColor White
Write-Host "Build Dir: $buildDir" -ForegroundColor Gray
Write-Host "Release Dir: $releaseDir" -ForegroundColor Gray
Write-Host ""

# ============================================================================
# Clean Previous Builds
# ============================================================================

Write-Host "Cleaning previous builds..." -ForegroundColor Cyan

if (Test-Path $buildDir) {
    Remove-Item -Path $buildDir -Recurse -Force
    Write-Host "  Removed: $buildDir" -ForegroundColor Gray
}

if (Test-Path $releaseDir) {
    Remove-Item -Path $releaseDir -Recurse -Force
    Write-Host "  Removed: $releaseDir" -ForegroundColor Gray
}

New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null

# ============================================================================
# Configure Build
# ============================================================================

Write-Host ""
Write-Host "Configuring build..." -ForegroundColor Cyan

$generator = "Visual Studio 17 2022"
$arch = "x64"

if ($Platform -eq "x86") {
    $arch = "Win32"
} elseif ($Platform -eq "arm64") {
    $arch = "ARM64"
}

$cmakeArgs = @(
    "-B", $buildDir,
    "-G", $generator,
    "-A", $arch,
    "-DCMAKE_BUILD_TYPE=Release",
    "-DENABLE_FFMPEG=ON",
    "-DVERSION=$Version"
)

Write-Host "  CMake args: $($cmakeArgs -join ' ')" -ForegroundColor Gray

# ============================================================================
# Build
# ============================================================================

Write-Host ""
Write-Host "Building..." -ForegroundColor Cyan

$buildArgs = @(
    "--build", $buildDir,
    "--config", "Release",
    "--parallel"
)

try {
    cmake $cmakeArgs
    cmake $buildArgs
    Write-Host "  Build successful!" -ForegroundColor Green
} catch {
    Write-Host "  Build failed!" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

# ============================================================================
# Copy Artifacts
# ============================================================================

Write-Host ""
Write-Host "Copying artifacts..." -ForegroundColor Cyan

$artifactDir = Join-Path $buildDir "Release"
$releaseBinDir = Join-Path $releaseDir "bin"
$releaseLibDir = Join-Path $releaseDir "lib"
$releaseIncludeDir = Join-Path $releaseDir "include"

New-Item -ItemType Directory -Path $releaseBinDir -Force | Out-Null
New-Item -ItemType Directory -Path $releaseLibDir -Force | Out-Null
New-Item -ItemType Directory -Path $releaseIncludeDir -Force | Out-Null

# Copy DLL
$dllFile = Join-Path $artifactDir "video_processing.dll"
if (Test-Path $dllFile) {
    Copy-Item -Path $dllFile -Destination $releaseBinDir -Force
    Write-Host "  Copied: video_processing.dll" -ForegroundColor Gray
}

# Copy LIB
$libFile = Join-Path $artifactDir "video_processing.lib"
if (Test-Path $libFile) {
    Copy-Item -Path $libFile -Destination $releaseLibDir -Force
    Write-Host "  Copied: video_processing.lib" -ForegroundColor Gray
}

# Copy headers
$includeDir = Join-Path $projectRoot "native/video-processing/src"
if (Test-Path $includeDir) {
    Get-ChildItem -Path $includeDir -Filter "*.h" | Copy-Item -Destination $releaseIncludeDir -Force
    Write-Host "  Copied: Header files" -ForegroundColor Gray
}

# Copy PDB (debug symbols)
$pdbFile = Join-Path $artifactDir "video_processing.pdb"
if (Test-Path $pdbFile) {
    Copy-Item -Path $pdbFile -Destination $releaseBinDir -Force
    Write-Host "  Copied: video_processing.pdb" -ForegroundColor Gray
}

# ============================================================================
# Create Version Info
# ============================================================================

Write-Host ""
Write-Host "Creating version info..." -ForegroundColor Cyan

$versionInfoPath = Join-Path $releaseDir "version.txt"
$versionInfo = @"
IP-CSS RTSP Client
Version: $Version
Build Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
Platform: $Platform
Build Type: Release
"@

Set-Content -Path $versionInfoPath -Value $versionInfo -Encoding UTF8
Write-Host "  Created: version.txt" -ForegroundColor Gray

# ============================================================================
# Create Package Info
# ============================================================================

$packageInfoPath = Join-Path $releaseDir "package.json"
$packageInfo = @{
    name = "ipcamera-rtsp-client"
    version = $Version
    description = "High-performance RTSP client library for IP cameras"
    platform = $Platform
    buildType = "Release"
    buildDate = (Get-Date -Format 'yyyy-MM-dd')
    files = @(
        "bin/video_processing.dll",
        "lib/video_processing.lib",
        "include/*.h",
        "version.txt"
    )
    codecs = @{
        video = @("H.264", "H.265", "MJPEG")
        audio = @("AAC", "PCMU", "PCMA")
    }
    features = @(
        "Hardware Decoding (DXVA2)",
        "AV Synchronization (<50ms)",
        "Auto Reconnect",
        "Frame Pool Optimization"
    )
}

$packageInfo | ConvertTo-Json -Depth 5 | Set-Content -Path $packageInfoPath -Encoding UTF8
Write-Host "  Created: package.json" -ForegroundColor Gray

# ============================================================================
# Create Installer (Optional)
# ============================================================================

if ($CreateInstaller) {
    Write-Host ""
    Write-Host "Creating installer..." -ForegroundColor Cyan
    
    $installerName = "IPCamera_RTSP_Client_${Version}_Setup.exe"
    $installerPath = Join-Path $releaseDir $installerName
    
    # Note: This is a placeholder - actual installer creation would use WiX, InnoSetup, etc.
    Write-Host "  Installer creation not implemented yet" -ForegroundColor Yellow
    Write-Host "  To create installer, use WiX Toolset or InnoSetup" -ForegroundColor Yellow
}

# ============================================================================
# Create Checksum
# ============================================================================

Write-Host ""
Write-Host "Creating checksum..." -ForegroundColor Cyan

$checksumFile = Join-Path $releaseDir "checksums.sha256"
$files = Get-ChildItem -Path $releaseDir -Recurse -File | 
    Where-Object { $_.Extension -in @(".dll", ".lib", ".h", ".txt", ".json") }

$checksums = @()
foreach ($file in $files) {
    $relativePath = $file.FullName.Replace((Resolve-Path $releaseDir).Path + "\", "")
    $hash = Get-FileHash -Path $file.FullName -Algorithm SHA256
    $checksums += "$($hash.Hash)  $relativePath"
}

$checksums | Set-Content -Path $checksumFile -Encoding UTF8
Write-Host "  Created: checksums.sha256" -ForegroundColor Gray

# ============================================================================
# Summary
# ============================================================================

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Release Build Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Version: $Version" -ForegroundColor White
Write-Host "Platform: $Platform" -ForegroundColor White
Write-Host "Release Dir: $releaseDir" -ForegroundColor White
Write-Host ""

# List files
Write-Host "Files:" -ForegroundColor Cyan
Get-ChildItem -Path $releaseDir -Recurse -File | ForEach-Object {
    $relativePath = $_.FullName.Replace((Resolve-Path $releaseDir).Path + "\", "")
    $size = $_.Length / 1KB
    Write-Host "  $relativePath ($([math]::Round($size, 2)) KB)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Test the build: .\scripts\test-connection.ps1" -ForegroundColor White
Write-Host "  2. Create NuGet package (if needed)" -ForegroundColor White
Write-Host "  3. Create installer (optional)" -ForegroundColor White
Write-Host "  4. Upload to release server" -ForegroundColor White
Write-Host ""
