# Build script for NAS packages (PowerShell version)
# Usage: .\scripts\build-nas-package.ps1 -PackageType synology -Arch x86_64 -Version Alfa-0.1.1

param(
    [Parameter(Mandatory=$false)]
    [string]$PackageType = "synology",

    [Parameter(Mandatory=$false)]
    [string]$Arch = "x86_64",

    [Parameter(Mandatory=$false)]
    [string]$Version = "Alfa-0.1.1",

    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Build NAS installer (Synology SPK) from platforms/*; may run Gradle and npm; uses TEMP build dir."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\build-nas-package.ps1 -ShowHelp"
    Write-Host "  .\scripts\build-nas-package.ps1 [-PackageType synology] [-Arch x86_64|amd64|arm64|aarch64] [-Version <semver-or-label>]"
    Write-Host ""
    Write-Host "Defaults:"
    Write-Host "  PackageType synology, Arch x86_64, Version Alfa-0.1.1 or from gradle.properties when unchanged"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Package build finished"
    Write-Host "  1  Unsupported arch, Gradle/npm failure, or packaging error"
    exit 0
}

$PROJECT_ROOT = Split-Path -Parent $PSScriptRoot
Set-Location $PROJECT_ROOT

Write-Host "Building NAS package: $PackageType for $Arch" -ForegroundColor Green

# Determine platform directory
if ($Arch -eq "x86_64" -or $Arch -eq "amd64") {
    $PLATFORM_DIR = Join-Path $PROJECT_ROOT "platforms\nas-x86_64"
} elseif (
    $Arch -eq "arm64" -or
    $Arch -eq "aarch64" -or
    $Arch -eq "armv7" -or
    $Arch -eq "arm" -or
    $Arch -eq "rtd1296" -or
    $Arch -eq "rtd"
) {
    $PLATFORM_DIR = Join-Path $PROJECT_ROOT "platforms\nas-arm"
} else {
    Write-Host "Error: Unsupported architecture: $Arch" -ForegroundColor Red
    exit 1
}

# Build the server and web components first
Write-Host "Building server components..." -ForegroundColor Yellow

# Build API server
$jarFiles = Get-ChildItem -Path "server\api\build\libs" -Filter "*.jar" -ErrorAction SilentlyContinue
if ($jarFiles) {
    Write-Host "API server JAR already exists, skipping build" -ForegroundColor Green
} else {
    Write-Host "API server JAR not found, building..." -ForegroundColor Yellow
    $gradlewPath = Join-Path $PROJECT_ROOT "gradlew.bat"
    if (Test-Path $gradlewPath) {
        & $gradlewPath :server:api:assemble --no-daemon
    } else {
        Write-Host "Warning: gradlew.bat not found. Skipping API server build." -ForegroundColor Yellow
        Write-Host "You may need to build the API server manually or fix dependency issues." -ForegroundColor Yellow
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error: Failed to build API server" -ForegroundColor Red
        Write-Host "Note: You may need to fix dependency issues first" -ForegroundColor Yellow
        exit 1
    }
}

# Build web interface
if (Test-Path "server\web\.next") {
    Write-Host "Web interface already built, skipping build" -ForegroundColor Green
} else {
    Write-Host "Web interface not found, building..." -ForegroundColor Yellow
    Push-Location "server\web"
    if (-not (Test-Path "node_modules")) {
        Write-Host "Installing npm dependencies..." -ForegroundColor Yellow
        npm install
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Warning: npm install failed. Web interface will not be included." -ForegroundColor Yellow
            Pop-Location
        } else {
            npm run build
            if ($LASTEXITCODE -ne 0) {
                Write-Host "Warning: npm build failed. Web interface will not be included." -ForegroundColor Yellow
            }
            Pop-Location
        }
    } else {
        npm run build
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Warning: npm build failed. Web interface will not be included." -ForegroundColor Yellow
        }
        Pop-Location
    }
}

# Create temporary build directory
$BUILD_DIR = Join-Path $env:TEMP "ip-css-nas-build-$PID"
New-Item -ItemType Directory -Path $BUILD_DIR -Force | Out-Null

function Build-SynologyPackage {
    Write-Host "Building Synology SPK package..." -ForegroundColor Green

    $PACKAGE_DIR = Join-Path $PLATFORM_DIR "packages\synology"
    $OUTPUT_FILE = Join-Path $PROJECT_ROOT "build\ip-css-$Version-synology-$Arch.spk"

    # Read version from gradle.properties if not provided or default
    if ($Version -eq "Alfa-0.1.1") {
        $gradleProps = Join-Path $PROJECT_ROOT "gradle.properties"
        if (Test-Path $gradleProps) {
            $gradleVersion = (Get-Content $gradleProps | Select-String "^version=").Line -replace "version=", ""
            if ($gradleVersion) {
                $Version = $gradleVersion.Trim()
                Write-Host "Using version from gradle.properties: $Version" -ForegroundColor Green
            }
        }
    }

    # Check if icons exist
    $icon72 = Join-Path $PACKAGE_DIR "icons\PACKAGE_ICON.PNG"
    $icon256 = Join-Path $PACKAGE_DIR "icons\PACKAGE_ICON_256.PNG"
    $HAS_ICONS = (Test-Path $icon72) -and (Test-Path $icon256)

    if (-not $HAS_ICONS) {
        Write-Host "Warning: Package icons not found. Package will be created without icons." -ForegroundColor Yellow
        Write-Host "Expected files:" -ForegroundColor Yellow
        Write-Host "  - $icon72" -ForegroundColor Yellow
        Write-Host "  - $icon256" -ForegroundColor Yellow
    }

    # Create package structure
    $packageDir = Join-Path $BUILD_DIR "package"
    New-Item -ItemType Directory -Path (Join-Path $packageDir "bin") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $packageDir "lib") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $packageDir "web") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $packageDir "conf") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $packageDir "scripts") -Force | Out-Null

    # Copy package files
    $binDir = Join-Path $PACKAGE_DIR "package\bin"
    if (Test-Path $binDir) {
        Copy-Item -Path "$binDir\*" -Destination (Join-Path $packageDir "bin") -Recurse -Force
    }

    # Copy API server JAR
    $jarFiles = Get-ChildItem -Path "server\api\build\libs" -Filter "*.jar" -ErrorAction SilentlyContinue
    if ($jarFiles) {
        Copy-Item -Path $jarFiles[0].FullName -Destination (Join-Path $packageDir "lib\server.jar") -Force
        Write-Host "API server JAR included" -ForegroundColor Green
    } else {
        Write-Host "Error: API server JAR not found. Build it first with: .\gradlew.bat :server:api:assemble" -ForegroundColor Red
        Remove-Item -Path $BUILD_DIR -Recurse -Force
        exit 1
    }

    # Copy web interface (optional)
    if (Test-Path "server\web\.next") {
        Copy-Item -Path "server\web\.next" -Destination (Join-Path $packageDir "web\dist") -Recurse -Force
        Write-Host "Web interface included" -ForegroundColor Green
    } else {
        Write-Host "Warning: Web interface not found, package will include only API server" -ForegroundColor Yellow
        New-Item -ItemType Directory -Path (Join-Path $packageDir "web\dist") -Force | Out-Null
    }

    # Copy scripts
    $scriptsDir = Join-Path $PACKAGE_DIR "scripts"
    if (Test-Path $scriptsDir) {
        Copy-Item -Path "$scriptsDir\*" -Destination (Join-Path $packageDir "scripts") -Force
    }

    # Ensure all scripts are executable (for Unix systems)
    Get-ChildItem -Path $packageDir -Filter "*.sh" -Recurse | ForEach-Object {
        # On Windows, this doesn't do anything, but it's here for completeness
    }

    # Copy icons if they exist
    if ($HAS_ICONS) {
        $iconsDir = Join-Path $BUILD_DIR "icons"
        New-Item -ItemType Directory -Path $iconsDir -Force | Out-Null
        Copy-Item -Path $icon72 -Destination (Join-Path $iconsDir "PACKAGE_ICON.PNG") -Force
        Copy-Item -Path $icon256 -Destination (Join-Path $iconsDir "PACKAGE_ICON_256.PNG") -Force
        Write-Host "Package icons included" -ForegroundColor Green
    }

    # Copy and update INFO file
    $infoFile = Join-Path $PACKAGE_DIR "INFO"
    $buildInfoFile = Join-Path $BUILD_DIR "INFO"
    Copy-Item -Path $infoFile -Destination $buildInfoFile -Force
    (Get-Content $buildInfoFile) -replace 'version=".*"', "version=`"$Version`"" | Set-Content $buildInfoFile
    (Get-Content $buildInfoFile) -replace 'arch=".*"', "arch=`"$Arch`"" | Set-Content $buildInfoFile

    # Create package.tgz using tar (if available) or 7zip
    Push-Location $BUILD_DIR
    try {
        # Try using tar (available in Windows 10+)
        & tar -czf package.tgz -C package .
        if ($LASTEXITCODE -ne 0) {
            throw "tar failed"
        }
    } catch {
        Write-Host "Error: tar not available. Please install tar or use WSL/Git Bash." -ForegroundColor Red
        Remove-Item -Path $BUILD_DIR -Recurse -Force
        exit 1
    }
    Pop-Location

    # Create SPK file
    $outputDir = Split-Path -Parent $OUTPUT_FILE
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

    Push-Location $BUILD_DIR
    try {
        if ($HAS_ICONS -and (Test-Path "icons")) {
            & tar -czf $OUTPUT_FILE INFO package.tgz -C $BUILD_DIR icons
            Write-Host "SPK package created with icons: $OUTPUT_FILE" -ForegroundColor Green
        } else {
            Write-Host "Warning: Creating SPK package without icons" -ForegroundColor Yellow
            & tar -czf $OUTPUT_FILE INFO package.tgz
            Write-Host "SPK package created: $OUTPUT_FILE" -ForegroundColor Green
        }
    } catch {
        Write-Host "Error: Failed to create SPK package" -ForegroundColor Red
        Remove-Item -Path $BUILD_DIR -Recurse -Force
        exit 1
    }
    Pop-Location

    # Cleanup
    Remove-Item -Path $BUILD_DIR -Recurse -Force
}

function Build-QnapPackage {
    Write-Host "Building QNAP QPKG package..." -ForegroundColor Green

    $PACKAGE_DIR = Join-Path $PLATFORM_DIR "packages\qnap"
    $OUTPUT_FILE = Join-Path $PROJECT_ROOT "build\ip-css-$Version-qnap-$Arch.qpkg"

    $qnapArch = switch ($Arch) {
        "arm64" { "arm_64" }
        "aarch64" { "arm_64" }
        "armv7" { "arm_32" }
        "arm" { "arm_32" }
        "x86_64" { "x86_64" }
        "amd64" { "x86_64" }
        default { $Arch }
    }

    $packageDir = Join-Path $BUILD_DIR "package"
    New-Item -ItemType Directory -Path $packageDir -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $packageDir "lib") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $packageDir "web") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $packageDir "scripts") -Force | Out-Null

    foreach ($script in @("init.sh", "start.sh", "stop.sh", "uninstall.sh", "service.sh")) {
        $scriptPath = Join-Path $PACKAGE_DIR "scripts\$script"
        if (Test-Path $scriptPath) {
            Copy-Item -Path $scriptPath -Destination (Join-Path $packageDir $script) -Force
        }
    }

    $detectScript = Join-Path $PACKAGE_DIR "scripts\detect-nas-paths.sh"
    if (Test-Path $detectScript) {
        Copy-Item -Path $detectScript -Destination (Join-Path $packageDir "scripts\detect-nas-paths.sh") -Force
    }

    $jarFiles = Get-ChildItem -Path "server\api\build\libs" -Filter "*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "-sources|-javadoc" }
    if ($jarFiles) {
        Copy-Item -Path $jarFiles[0].FullName -Destination (Join-Path $packageDir "lib\server.jar") -Force
        Write-Host "API server JAR included" -ForegroundColor Green
    } else {
        Write-Host "Error: API server JAR not found. Build it first with: .\gradlew.bat :server:api:assemble" -ForegroundColor Red
        Remove-Item -Path $BUILD_DIR -Recurse -Force
        exit 1
    }

    if (Test-Path "server\web\.next") {
        Copy-Item -Path "server\web\.next" -Destination (Join-Path $packageDir "web\dist") -Recurse -Force
        Write-Host "Web interface included" -ForegroundColor Green
    } else {
        New-Item -ItemType Directory -Path (Join-Path $packageDir "web\dist") -Force | Out-Null
    }

    $qpkgInfoSrc = Join-Path $PACKAGE_DIR "QPKG.INFO"
    $qpkgInfoDst = Join-Path $BUILD_DIR "QPKG.INFO"
    Copy-Item -Path $qpkgInfoSrc -Destination $qpkgInfoDst -Force
    (Get-Content $qpkgInfoDst) -replace '<Version>.*</Version>', "<Version>$Version</Version>" | Set-Content $qpkgInfoDst
    (Get-Content $qpkgInfoDst) -replace '<Architecture>.*</Architecture>', "<Architecture>$qnapArch</Architecture>" | Set-Content $qpkgInfoDst

    Push-Location $BUILD_DIR
    try {
        & tar -czf package.tgz -C package .
        if ($LASTEXITCODE -ne 0) { throw "tar failed" }
        $outputDir = Split-Path -Parent $OUTPUT_FILE
        New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
        & tar -czf $OUTPUT_FILE QPKG.INFO package.tgz
        if ($LASTEXITCODE -ne 0) { throw "tar failed" }
        Write-Host "QPKG package created: $OUTPUT_FILE" -ForegroundColor Green
    } catch {
        Write-Host "Error: Failed to create QPKG package" -ForegroundColor Red
        Remove-Item -Path $BUILD_DIR -Recurse -Force
        exit 1
    } finally {
        Pop-Location
    }

    Remove-Item -Path $BUILD_DIR -Recurse -Force
}

function Build-AsustorPackage {
    Write-Host "Building Asustor APK package..." -ForegroundColor Green

    $PACKAGE_DIR = Join-Path $PLATFORM_DIR "packages\asustor"
    $OUTPUT_FILE = Join-Path $PROJECT_ROOT "build\ip-css-$Version-asustor-$Arch.apk"

    $apkgArch = switch ($Arch) {
        "arm64" { "armv8" }
        "aarch64" { "armv8" }
        "armv7" { "armv7" }
        "arm" { "armv7" }
        "rtd1296" { "rtd1296" }
        "rtd" { "rtd1296" }
        "x86_64" { "x86_64" }
        "amd64" { "x86_64" }
        default { $Arch }
    }

    $packageDir = Join-Path $BUILD_DIR "package"
    New-Item -ItemType Directory -Path (Join-Path $packageDir "bin") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $packageDir "lib") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $packageDir "web") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $packageDir "scripts") -Force | Out-Null

    $binDir = Join-Path $PACKAGE_DIR "package\bin"
    if (Test-Path $binDir) {
        Copy-Item -Path "$binDir\*" -Destination (Join-Path $packageDir "bin") -Recurse -Force
    }

    $jarFiles = Get-ChildItem -Path "server\api\build\libs" -Filter "*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "-sources|-javadoc" }
    if ($jarFiles) {
        Copy-Item -Path $jarFiles[0].FullName -Destination (Join-Path $packageDir "lib\server.jar") -Force
        Write-Host "API server JAR included" -ForegroundColor Green
    } else {
        Write-Host "Error: API server JAR not found. Build it first with: .\gradlew.bat :server:api:assemble" -ForegroundColor Red
        Remove-Item -Path $BUILD_DIR -Recurse -Force
        exit 1
    }

    if (Test-Path "server\web\.next") {
        Copy-Item -Path "server\web\.next" -Destination (Join-Path $packageDir "web\dist") -Recurse -Force
    } else {
        New-Item -ItemType Directory -Path (Join-Path $packageDir "web\dist") -Force | Out-Null
    }

    foreach ($s in @("preinst", "postinst", "preuninst", "postuninst")) {
        $sPath = Join-Path $PACKAGE_DIR "scripts\$s"
        if (Test-Path $sPath) {
            Copy-Item -Path $sPath -Destination (Join-Path $packageDir "scripts\$s") -Force
        }
    }

    $infoSrc = Join-Path $PACKAGE_DIR "INFO"
    $infoDst = Join-Path $BUILD_DIR "INFO"
    Copy-Item -Path $infoSrc -Destination $infoDst -Force
    (Get-Content $infoDst) -replace 'version=".*"', "version=`"$Version`"" | Set-Content $infoDst
    (Get-Content $infoDst) -replace 'architecture=".*"', "architecture=`"$apkgArch`"" | Set-Content $infoDst

    Push-Location $BUILD_DIR
    try {
        & tar -czf package.tgz -C package .
        if ($LASTEXITCODE -ne 0) { throw "tar failed" }

        $apkFiles = @("INFO", "package.tgz")
        foreach ($s in @("preinst", "postinst", "preuninst", "postuninst")) {
            $src = Join-Path $PACKAGE_DIR "scripts\$s"
            if (Test-Path $src) {
                Copy-Item -Path $src -Destination (Join-Path $BUILD_DIR $s) -Force
                $apkFiles += $s
            }
        }

        $outputDir = Split-Path -Parent $OUTPUT_FILE
        New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
        & tar -czf $OUTPUT_FILE @apkFiles
        if ($LASTEXITCODE -ne 0) { throw "tar failed" }
        Write-Host "APK package created: $OUTPUT_FILE" -ForegroundColor Green
    } catch {
        Write-Host "Error: Failed to create Asustor package" -ForegroundColor Red
        Remove-Item -Path $BUILD_DIR -Recurse -Force
        exit 1
    } finally {
        Pop-Location
    }

    Remove-Item -Path $BUILD_DIR -Recurse -Force
}

function Build-TruenasPackage {
    Write-Host "Building TrueNAS package..." -ForegroundColor Green

    $PACKAGE_DIR = Join-Path $PLATFORM_DIR "packages\truenas"
    $OUTPUT_DIR = Join-Path $PROJECT_ROOT "build\truenas-$Version"
    New-Item -ItemType Directory -Path $OUTPUT_DIR -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $OUTPUT_DIR "kubernetes") -Force | Out-Null

    $dockerComposeSrc = Join-Path $PACKAGE_DIR "docker-compose.yml"
    $dockerComposeDst = Join-Path $OUTPUT_DIR "docker-compose.yml"
    Copy-Item -Path $dockerComposeSrc -Destination $dockerComposeDst -Force
    (Get-Content $dockerComposeDst) -replace ":Alfa-0.0.1", ":$Version" | Set-Content $dockerComposeDst

    $k8sDir = Join-Path $PACKAGE_DIR "kubernetes"
    if (Test-Path $k8sDir) {
        Get-ChildItem -Path $k8sDir -Filter "*.yaml" | ForEach-Object {
            $dst = Join-Path $OUTPUT_DIR "kubernetes\$($_.Name)"
            Copy-Item -Path $_.FullName -Destination $dst -Force
            (Get-Content $dst) -replace ":Alfa-0.0.1", ":$Version" | Set-Content $dst
        }
    }

    $coreDir = Join-Path $PACKAGE_DIR "core"
    if (Test-Path $coreDir) {
        New-Item -ItemType Directory -Path (Join-Path $OUTPUT_DIR "core") -Force | Out-Null
        Copy-Item -Path "$coreDir\*" -Destination (Join-Path $OUTPUT_DIR "core") -Recurse -Force
    }

    $readmePath = Join-Path $OUTPUT_DIR "README.md"
    @"
# IP-CSS for TrueNAS

## TrueNAS CORE (FreeBSD Jail)
See `core/README.md` and `core/setup-jail.sh`.

## TrueNAS SCALE (Docker)
1. Copy `docker-compose.yml`
2. Run `docker-compose up -d`

## TrueNAS SCALE (Kubernetes)
Run `kubectl apply -f kubernetes/`

Version: $Version
"@ | Set-Content -Path $readmePath

    Write-Host "TrueNAS package created in: $OUTPUT_DIR" -ForegroundColor Green
    Remove-Item -Path $BUILD_DIR -Recurse -Force
}

# Execute build function based on package type
switch ($PackageType) {
    "synology" {
        Build-SynologyPackage
    }
    "qnap" {
        Build-QnapPackage
    }
    "asustor" {
        Build-AsustorPackage
    }
    "truenas" {
        Build-TruenasPackage
    }
    default {
        Write-Host "Error: Unknown package type: $PackageType" -ForegroundColor Red
        Write-Host "Supported types: synology, qnap, asustor, truenas" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "Build completed successfully!" -ForegroundColor Green
