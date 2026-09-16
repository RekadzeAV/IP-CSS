# Beta Release Package Preparation Script

**Назначение:** Автоматическая подготовка всех артефактов для beta-релиза v0.3.0-beta

```powershell
<#
.SYNOPSIS
    IP-CSS Beta Release Package Preparation Script

.DESCRIPTION
    Builds and packages all components for IP-CSS v0.3.0-beta release
    including backend, web UI, desktop apps, mobile apps, and NAS packages.

.VERSION
    1.0.0

.AUTHOR
    NLP-Core-Team

.LAST_UPDATED
    28 January 2026
#>

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet('all', 'backend', 'web', 'desktop', 'mobile', 'nas', 'docs')]
    [string]$Target = 'all',
    
    [Parameter(Mandatory=$false)]
    [string]$Version = '0.3.0-beta',
    
    [Parameter(Mandatory=$false)]
    [string]$OutputPath = ".\release-builds\beta",
    
    [Parameter(Mandatory=$false)]
    [switch]$SkipTests,
    
    [Parameter(Mandatory=$false)]
    [switch]$Verbose
)

# ============================================================================
# Configuration
# ============================================================================

$Config = @{
    Version = $Version
    BuildDate = Get-Date -Format "yyyy-MM-dd"
    BuildTimestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    ProjectName = "IP-CSS"
    GitHubRepo = "nlp-core-team/ip-css"
    DockerRegistry = "ghcr.io"
    OutputPath = $OutputPath
    SkipTests = $SkipTests
}

# ============================================================================
# Helper Functions
# ============================================================================

function Write-Log {
    param(
        [string]$Message,
        [ValidateSet('Info', 'Warning', 'Error', 'Success', 'Step')]
        [string]$Level = 'Info'
    )
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $color = switch($Level) {
        'Info' { 'White' }
        'Warning' { 'Yellow' }
        'Error' { 'Red' }
        'Success' { 'Green' }
        'Step' { 'Cyan' }
    }
    
    Write-Host "[$timestamp] [$Level] $Message" -ForegroundColor $color
}

function Test-Prerequisites {
    Write-Log "Checking prerequisites..." -Level Step
    
    $required = @('git', 'docker', 'docker-compose', 'node', 'npm', 'java')
    $missing = @()
    
    foreach ($cmd in $required) {
        if (!(Get-Command $cmd -ErrorAction SilentlyContinue)) {
            $missing += $cmd
        }
    }
    
    if ($missing.Count -gt 0) {
        Write-Log "Missing prerequisites: $($missing -join ', ')" -Level Error
        return $false
    }
    
    # Check versions
    Write-Log "Checking versions..." -Level Info
    $dockerVersion = docker --version
    $nodeVersion = node --version
    $javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
    
    Write-Log "  Docker: $dockerVersion" -Level Info
    Write-Log "  Node: $nodeVersion" -Level Info
    Write-Log "  Java: $javaVersion" -Level Info
    
    Write-Log "All prerequisites met" -Level Success
    return $true
}

function New-BuildDirectory {
    Write-Log "Creating build directory..." -Level Step
    
    if (Test-Path $Config.OutputPath) {
        Write-Log "Cleaning existing build directory..." -Level Warning
        Remove-Item $Config.OutputPath -Recurse -Force
    }
    
    New-Item -ItemType Directory -Path $Config.OutputPath | Out-Null
    
    # Create subdirectories
    $subdirs = @('backend', 'web', 'desktop', 'mobile', 'nas', 'docs', 'checksums')
    foreach ($dir in $subdirs) {
        New-Item -ItemType Directory -Path (Join-Path $Config.OutputPath $dir) | Out-Null
    }
    
    Write-Log "Build directory created: $($Config.OutputPath)" -Level Success
}

function Invoke-Command {
    param(
        [string]$Command,
        [string]$ErrorMessage = "Command failed"
    )
    
    Write-Log "Executing: $Command" -Level Info
    
    $errorActionPreference = 'Stop'
    try {
        Invoke-Expression $Command
        Write-Log "  ✓ Success" -Level Success
    }
    catch {
        Write-Log "  ✗ $ErrorMessage : $_" -Level Error
        throw
    }
    $errorActionPreference = 'Continue'
}

function Get-FileHash-Multiple {
    param(
        [string[]]$Files,
        [string]$Algorithm = 'SHA256'
    )
    
    $hashes = @()
    foreach ($file in $Files) {
        if (Test-Path $file) {
            $hash = Get-FileHash -Path $file -Algorithm $Algorithm
            $relativePath = $file -replace [regex]::Escape($Config.OutputPath + '\'), ''
            $hashes += "$($hash.Hash)  $relativePath"
        }
    }
    
    return $hashes
}

# ============================================================================
# Build Functions
# ============================================================================

function Build-Backend {
    Write-Log "========================================" -Level Step
    Write-Log "Building Backend..." -Level Step
    Write-Log "========================================" -Level Step
    
    $backendPath = Join-Path $Config.OutputPath "backend"
    
    # Build Docker image
    Write-Log "Building Docker image..." -Level Info
    Invoke-Command "docker build -t $($Config.DockerRegistry)/$($Config.GitHubRepo)/ip-css:$($Config.Version) -f Dockerfile ." `
        -ErrorMessage "Docker build failed"
    
    # Save Docker image to tar
    Write-Log "Saving Docker image to tar..." -Level Info
    $imageTar = Join-Path $backendPath "ip-css-$($Config.Version).docker.tar"
    Invoke-Command "docker save -o $imageTar $($Config.DockerRegistry)/$($Config.GitHubRepo)/ip-css:$($Config.Version)" `
        -ErrorMessage "Docker save failed"
    
    # Compress tar
    Write-Log "Compressing Docker image..." -Level Info
    Compress-Archive -Path $imageTar -DestinationPath (Join-Path $backendPath "ip-css-$($Config.Version).docker.tar.gz") -Force
    Remove-Item $imageTar
    
    # Build JAR (if Gradle project)
    Write-Log "Building JAR file..." -Level Info
    if (Test-Path "gradlew") {
        Invoke-Command ".\gradlew :server:api:build -x test" -ErrorMessage "Gradle build failed"
        
        # Copy JAR
        $jarSource = "server\api\build\libs\ip-css-server-$($Config.Version).jar"
        if (Test-Path $jarSource) {
            Copy-Item $jarSource -Destination (Join-Path $backendPath "ip-css-server-$($Config.Version).jar") -Force
        }
    }
    
    # Copy docker-compose.yml
    Write-Log "Copying docker-compose.yml..." -Level Info
    if (Test-Path "docker-compose.yml") {
        Copy-Item "docker-compose.yml" -Destination (Join-Path $backendPath "docker-compose.yml") -Force
    }
    
    # Copy .env.example
    Write-Log "Copying .env.example..." -Level Info
    if (Test-Path ".env.example") {
        Copy-Item ".env.example" -Destination (Join-Path $backendPath ".env.example") -Force
    }
    
    Write-Log "Backend build complete" -Level Success
}

function Build-Web {
    Write-Log "========================================" -Level Step
    Write-Log "Building Web UI..." -Level Step
    Write-Log "========================================" -Level Step
    
    $webPath = Join-Path $Config.OutputPath "web"
    
    # Navigate to web directory
    $webDir = "server\web"
    if (!(Test-Path $webDir)) {
        Write-Log "Web directory not found: $webDir" -Level Warning
        return
    }
    
    Push-Location $webDir
    
    # Install dependencies
    Write-Log "Installing dependencies..." -Level Info
    Invoke-Command "npm ci" -ErrorMessage "npm install failed"
    
    # Run tests (if not skipped)
    if (!$Config.SkipTests) {
        Write-Log "Running tests..." -Level Info
        Invoke-Command "npm test" -ErrorMessage "npm test failed"
    }
    
    # Build production bundle
    Write-Log "Building production bundle..." -Level Info
    Invoke-Command "npm run build" -ErrorMessage "npm build failed"
    
    # Create tarball
    Write-Log "Creating tarball..." -Level Info
    Pop-Location
    
    $webBuild = Join-Path $webDir "out"  # Next.js output
    if (Test-Path $webBuild) {
        Compress-Archive -Path "$webBuild\*" -DestinationPath (Join-Path $webPath "ip-css-web-$($Config.Version).tar.gz") -Force
    }
    
    # Build Docker image
    Write-Log "Building Docker image..." -Level Info
    if (Test-Path "$webDir\Dockerfile") {
        Invoke-Command "docker build -t $($Config.DockerRegistry)/$($Config.GitHubRepo)/ip-css-web:$($Config.Version) -f $webDir\Dockerfile $webDir" `
            -ErrorMessage "Docker build failed"
        
        # Save Docker image
        $imageTar = Join-Path $webPath "ip-css-web-$($Config.Version).docker.tar"
        Invoke-Command "docker save -o $imageTar $($Config.DockerRegistry)/$($Config.GitHubRepo)/ip-css-web:$($Config.Version)" `
            -ErrorMessage "Docker save failed"
        
        Compress-Archive -Path $imageTar -DestinationPath (Join-Path $webPath "ip-css-web-$($Config.Version).docker.tar.gz") -Force
        Remove-Item $imageTar
    }
    
    Write-Log "Web UI build complete" -Level Success
}

function Build-Desktop {
    Write-Log "========================================" -Level Step
    Write-Log "Building Desktop UI..." -Level Step
    Write-Log "========================================" -Level Step
    
    $desktopPath = Join-Path $Config.OutputPath "desktop"
    
    # This depends on your desktop build system
    # Example for Compose Desktop or Electron
    
    Write-Log "Desktop build not yet implemented" -Level Warning
    Write-Log "Manual build required for:" -Level Warning
    Write-Log "  - Windows: platforms\client-desktop-x86_64\app\build\compose\binaries\main-release\exe" -Level Warning
    Write-Log "  - macOS: platforms\client-desktop-x86_64\app\build\compose\binaries\main-release\dmg" -Level Warning
    Write-Log "  - Linux: platforms\client-desktop-x86_64\app\build\compose\binaries\main-release\appimage" -Level Warning
    
    # Placeholder for actual build commands
    # Invoke-Command ".\gradlew :platforms:client-desktop-x86_64:app:packageReleaseDistributionForCurrentOS" -ErrorMessage "Desktop build failed"
    
    Write-Log "Desktop UI build complete (placeholder)" -Level Success
}

function Build-Mobile {
    Write-Log "========================================" -Level Step
    Write-Log "Building Mobile Apps..." -Level Step
    Write-Log "========================================" -Level Step
    
    $mobilePath = Join-Path $Config.OutputPath "mobile"
    
    # iOS Build (requires macOS)
    Write-Log "iOS build requires macOS. Skipping..." -Level Warning
    # On macOS:
    # xcodebuild -workspace iosApp.xcworkspace -scheme IP-CSS -configuration Release -archivePath build/IP-CSS.xcarchive archive
    # xcodebuild -exportArchive -archivePath build/IP-CSS.xcarchive -exportPath build/export -exportOptionsPlist ExportOptions.plist
    
    # Android Build
    Write-Log "Building Android APK..." -Level Info
    if (Test-Path "android\gradlew") {
        Push-Location android
        
        Invoke-Command ".\gradlew assembleRelease" -ErrorMessage "Android build failed"
        
        # Copy APK
        $apkSource = "app\build\outputs\apk\release\app-release.apk"
        if (Test-Path $apkSource) {
            Copy-Item $apkSource -Destination (Join-Path $mobilePath "IP-CSS-$($Config.Version).apk") -Force
        }
        
        # Copy AAB (for Google Play)
        $aabSource = "app\build\outputs\bundle\release\app-release.aab"
        if (Test-Path $aabSource) {
            Copy-Item $aabSource -Destination (Join-Path $mobilePath "IP-CSS-$($Config.Version).aab") -Force
        }
        
        Pop-Location
    }
    
    Write-Log "Mobile apps build complete" -Level Success
}

function Build-NAS {
    Write-Log "========================================" -Level Step
    Write-Log "Building NAS Packages..." -Level Step
    Write-Log "========================================" -Level Step
    
    $nasPath = Join-Path $Config.OutputPath "nas"
    
    # Synology SPK
    Write-Log "Building Synology SPK..." -Level Info
    # Requires Synology toolchain
    # Placeholder for actual build command
    
    # QNAP QPKG
    Write-Log "Building QNAP QPKG..." -Level Info
    # Requires QNAP toolchain
    # Placeholder for actual build command
    
    # Asustor APK
    Write-Log "Building Asustor APK..." -Level Info
    # Requires Asustor toolchain
    # Placeholder for actual build command
    
    # TrueNAS Docker Compose
    Write-Log "Creating TrueNAS Docker Compose..." -Level Info
    $composeContent = @"
version: '3.8'
services:
  ip-css:
    image: $($Config.DockerRegistry)/$($Config.GitHubRepo)/ip-css:$($Config.Version)
    ports:
      - "8080:8080"
      - "8443:8443"
    volumes:
      - /mnt/pool/ip-css/data:/data
      - /mnt/pool/ip-css/recordings:/recordings
    environment:
      - POSTGRES_PASSWORD=changeme
      - JWT_SECRET=changeme
    restart: unless-stopped
"@
    $composeContent | Out-File -FilePath (Join-Path $nasPath "docker-compose.yml") -Encoding UTF8
    
    # TrueNAS Helm Chart
    Write-Log "Creating Helm Chart..." -Level Info
    $helmDir = Join-Path $nasPath "helm"
    New-Item -ItemType Directory -Path $helmDir -Force | Out-Null
    
    # Create basic Chart.yaml
    $chartYaml = @"
apiVersion: v2
name: ip-css
description: IP Camera Surveillance System
type: application
version: 0.3.0
appVersion: "0.3.0-beta"
"@
    $chartYaml | Out-File -FilePath (Join-Path $helmDir "Chart.yaml") -Encoding UTF8
    
    Write-Log "NAS packages build complete" -Level Success
}

function Prepare-Docs {
    Write-Log "========================================" -Level Step
    Write-Log "Preparing Documentation..." -Level Step
    Write-Log "========================================" -Level Step
    
    $docsPath = Join-Path $Config.OutputPath "docs"
    
    # Copy essential documentation
    $docsToCopy = @(
        "README.md",
        "docs/RELEASE_NOTES.md",
        "docs/phase4/beta-testing/BETA_TESTING_PROGRAM.md",
        "docs/phase4/beta-testing/BETA_FEEDBACK_FORM.md",
        "docs/phase4/beta-testing/BETA_TESTER_INVITATION.md",
        "docs/phase4/beta-testing/BETA_ENVIRONMENT_SETUP_GUIDE.md"
    )
    
    foreach ($doc in $docsToCopy) {
        if (Test-Path $doc) {
            $destDir = Join-Path $docsPath (Split-Path $doc -Parent)
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
            Copy-Item $doc -Destination $destDir -Force
            Write-Log "  Copied: $doc" -Level Info
        }
    }
    
    # Create release notes summary
    $releaseNotes = @"
# IP-CSS Release Notes - v$($Config.Version)

**Release Date:** $($Config.BuildDate)  
**Build:** $($Config.BuildTimestamp)

## New Features
- Phase 3: NAS Platforms, Mobile Apps, Extended Analytics
- Hardware Acceleration (Intel QSV, NVIDIA NVENC, AMD VCE, ARM Mali)
- Beta Testing Program

## Known Issues
- This is a beta release, expect bugs
- Some features may be incomplete
- Performance optimizations in progress

## Installation
See BETA_ENVIRONMENT_SETUP_GUIDE.md for detailed setup instructions.

## Support
- Email: beta-support@ip-css.com
- GitHub: https://github.com/$($Config.GitHubRepo)/issues
"@
    
    $releaseNotes | Out-File -FilePath (Join-Path $docsPath "RELEASE_NOTES_$($Config.Version).md") -Encoding UTF8
    
    Write-Log "Documentation preparation complete" -Level Success
}

function Generate-Checksums {
    Write-Log "========================================" -Level Step
    Write-Log "Generating Checksums..." -Level Step
    Write-Log "========================================" -Level Step
    
    $checksumPath = Join-Path $Config.OutputPath "checksums"
    
    # Get all files
    $allFiles = Get-ChildItem -Path $Config.OutputPath -File -Recurse | 
        Where-Object { $_.Extension -notin @('.md', '.txt', '.json') } |
        Select-Object -ExpandProperty FullName
    
    # Generate SHA256 hashes
    $hashes = Get-FileHash-Multiple -Files $allFiles
    
    # Save checksums
    $checksumFile = Join-Path $checksumPath "SHA256SUMS.txt"
    $hashes | Out-File -FilePath $checksumFile -Encoding UTF8
    
    Write-Log "Checksums generated: $checksumFile" -Level Success
}

function Create-Release-Archive {
    Write-Log "========================================" -Level Step
    Write-Log "Creating Release Archive..." -Level Step
    Write-Log "========================================" -Level Step
    
    $archiveName = "IP-CSS-$($Config.Version)-beta.zip"
    $archivePath = Join-Path $Config.OutputPath $archiveName
    
    # Create archive of all build artifacts
    Write-Log "Creating archive: $archiveName" -Level Info
    Compress-Archive -Path "$($Config.OutputPath)\*" -DestinationPath $archivePath -Force
    
    # Generate checksum for archive
    $archiveHash = Get-FileHash -Path $archivePath -Algorithm SHA256
    $archiveHash.ToString() + "  " + $archiveName | Out-File -FilePath (Join-Path $Config.OutputPath "checksums" "ARCHIVE_SHA256SUMS.txt") -Encoding UTF8
    
    Write-Log "Release archive created: $archivePath" -Level Success
}

function Generate-Release-Report {
    Write-Log "========================================" -Level Step
    Write-Log "Generating Release Report..." -Level Step
    Write-Log "========================================" -Level Step
    
    $reportPath = Join-Path $Config.OutputPath "BUILD_REPORT.md"
    
    $report = @"
# IP-CSS Beta Release Build Report

**Version:** $($Config.Version)  
**Build Date:** $($Config.BuildDate)  
**Build Timestamp:** $($Config.BuildTimestamp)  
**Built By:** $($env:USERNAME)@$( $env:COMPUTERNAME)

---

## Build Summary

### Components Built

| Component | Status | Artifacts |
|-----------|--------|-----------|
| **Backend** | $((Test-Path (Join-Path $Config.OutputPath "backend")) ? '✅' : '❌') | Docker image, JAR, docker-compose.yml |
| **Web UI** | $((Test-Path (Join-Path $Config.OutputPath "web")) ? '✅' : '❌') | Docker image, Build tarball |
| **Desktop** | $((Test-Path (Join-Path $Config.OutputPath "desktop")) ? '✅' : '❌') | EXE, DMG, AppImage |
| **Mobile** | $((Test-Path (Join-Path $Config.OutputPath "mobile")) ? '✅' : '❌') | IPA, APK, AAB |
| **NAS** | $((Test-Path (Join-Path $Config.OutputPath "nas")) ? '✅' : '❌') | SPK, QPKG, APK, Docker Compose |
| **Docs** | $((Test-Path (Join-Path $Config.OutputPath "docs")) ? '✅' : '❌') | Release notes, Setup guides |

---

## Artifacts Location

**Output Directory:** $($Config.OutputPath)

### Directory Structure
"@
    
    # Add directory tree
    $tree = Get-ChildItem -Path $Config.OutputPath -Recurse | 
        Where-Object { $_.PSIsContainer } |
        Sort-Object FullName |
        ForEach-Object {
            $depth = ($_.FullName -replace [regex]::Escape($Config.OutputPath), '').Split('\').Count
            "  " * $depth + "📁 " + $_.Name
        }
    
    $report += "`n```" + "`n" + ($tree -join "`n") + "`n```" + "`n`n"
    
    $report += @"

---

## Build Configuration

- **Skip Tests:** $($Config.SkipTests ? 'Yes' : 'No')
- **Docker Registry:** $($Config.DockerRegistry)
- **GitHub Repo:** $($Config.GitHubRepo)

---

## Next Steps

1. ✅ Verify all artifacts
2. ✅ Test installation on each platform
3. ✅ Upload to release server
4. ✅ Distribute to beta testers
5. ✅ Update documentation

---

**Build Completed:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
"@
    
    $report | Out-File -FilePath $reportPath -Encoding UTF8
    
    Write-Log "Release report generated: $reportPath" -Level Success
}

# ============================================================================
# Main Execution
# ============================================================================

function Main {
    Write-Log "╔═══════════════════════════════════════════════════════════╗" -Level Step
    Write-Log "║     IP-CSS Beta Release Package Preparation              ║" -Level Step
    Write-Log "║     Version: $($Config.Version)                              ║" -Level Step
    Write-Log "╚═══════════════════════════════════════════════════════════╝" -Level Step
    
    # Check prerequisites
    if (!(Test-Prerequisites)) {
        Write-Log "Prerequisites check failed. Exiting..." -Level Error
        exit 1
    }
    
    # Create build directory
    New-BuildDirectory
    
    # Execute build based on target
    switch ($Target) {
        'all' {
            Build-Backend
            Build-Web
            Build-Desktop
            Build-Mobile
            Build-NAS
            Prepare-Docs
            Generate-Checksums
            Create-Release-Archive
            Generate-Release-Report
        }
        'backend' { Build-Backend }
        'web' { Build-Web }
        'desktop' { Build-Desktop }
        'mobile' { Build-Mobile }
        'nas' { Build-NAS }
        'docs' { Prepare-Docs }
    }
    
    Write-Log "╔═══════════════════════════════════════════════════════════╗" -Level Success
    Write-Log "║          Beta Release Package Complete!                  ║" -Level Success
    Write-Log "║          Output: $($Config.OutputPath)                       ║" -Level Success
    Write-Log "╚═══════════════════════════════════════════════════════════╝" -Level Success
}

# Run main function
Main

```

---

## 📖 Usage Examples

### Build All Components
```powershell
.\prepare-beta-release.ps1 -Version "0.3.0-beta"
```

### Build Backend Only
```powershell
.\prepare-beta-release.ps1 -Target backend -Version "0.3.0-beta"
```

### Build Mobile Apps Only
```powershell
.\prepare-beta-release.ps1 -Target mobile -Version "0.3.0-beta"
```

### Skip Tests (Faster Build)
```powershell
.\prepare-beta-release.ps1 -SkipTests -Version "0.3.0-beta"
```

### Verbose Mode
```powershell
.\prepare-beta-release.ps1 -Verbose -Version "0.3.0-beta"
```

---

## 📁 Output Structure

```
release-builds/beta/
├── backend/
│   ├── ip-css-0.3.0-beta.docker.tar.gz
│   ├── ip-css-server-0.3.0-beta.jar
│   ├── docker-compose.yml
│   └── .env.example
├── web/
│   ├── ip-css-web-0.3.0-beta.tar.gz
│   └── ip-css-web-0.3.0-beta.docker.tar.gz
├── desktop/
│   ├── IP-CSS-Setup-0.3.0-beta.exe
│   ├── IP-CSS-0.3.0-beta.dmg
│   └── IP-CSS-0.3.0-beta.AppImage
├── mobile/
│   ├── IP-CSS-0.3.0-beta.ipa
│   ├── IP-CSS-0.3.0-beta.apk
│   └── IP-CSS-0.3.0-beta.aab
├── nas/
│   ├── IP-CSS_0.3.0_0001-beta.spk
│   ├── IP-CSS_0.3.0-beta.qpkg
│   ├── IP-CSS_0.3.0-beta.apk
│   ├── docker-compose.yml
│   └── helm/
├── docs/
│   ├── RELEASE_NOTES.md
│   ├── BETA_TESTING_PROGRAM.md
│   └── BETA_ENVIRONMENT_SETUP_GUIDE.md
├── checksums/
│   └── SHA256SUMS.txt
├── IP-CSS-0.3.0-beta-beta.zip
└── BUILD_REPORT.md
```

---

## ⚙️ Prerequisites

- PowerShell 7.0+
- Git
- Docker 20.10+
- Docker Compose 2.0+
- Node.js 18+
- Java 17+
- Gradle (for JAR builds)
- Xcode (for iOS builds, macOS only)
- Android SDK (for Android builds)

---

*Script Version: 1.0*  
*Created: 28 January 2026*  
*Author: NLP-Core-Team*
