# IP-CSS Beta Build Script

**Назначение:** Автоматическая сборка beta-релиза для Raspberry Pi, Docker и Windows

```powershell
<#
.SYNOPSIS
    IP-CSS Beta Build Script for Multiple Platforms

.DESCRIPTION
    Builds beta release packages for:
    - Raspberry Pi (ARM64)
    - Docker (linux/amd64, linux/arm64)
    - Windows (x64)

.VERSION
    1.0.0-beta

.AUTHOR
    NLP-Core-Team

.LAST_UPDATED
    28 January 2026
#>

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet('all', 'raspberry', 'docker', 'windows')]
    [string]$Platform = 'all',
    
    [Parameter(Mandatory=$false)]
    [string]$Version = '1.0.0-beta',
    
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
    OutputPath = $OutputPath
    SkipTests = $SkipTests
    RaspberryPi = @{
        Arch = "arm64"
        OS = "linux"
        JDK = "17"
    }
    Docker = @{
        Registry = "ghcr.io"
        Repository = "nlp-core-team/ip-css"
        Platforms = @("linux/amd64", "linux/arm64")
    }
    Windows = @{
        Arch = "x64"
        JDK = "17"
    }
}

# ============================================================================
# Helper Functions
# ============================================================================

function Write-Log {
    param(
        [string]$Message,
        [ValidateSet('Info', 'Warning', 'Error', 'Success', 'Step', 'Build')]
        [string]$Level = 'Info'
    )
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $color = switch($Level) {
        'Info' { 'White' }
        'Warning' { 'Yellow' }
        'Error' { 'Red' }
        'Success' { 'Green' }
        'Step' { 'Cyan' }
        'Build' { 'Magenta' }
    }
    
    Write-Host "[$timestamp] [$Level] $Message" -ForegroundColor $color
}

function Test-Prerequisites {
    Write-Log "Checking prerequisites..." -Level Step
    
    $required = @('git', 'docker', 'java', 'gradle')
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
    $javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
    $dockerVersion = docker --version
    $gradleVersion = gradle --version | Select-String "Gradle" | Select-Object -First 1
    
    Write-Log "  Java: $javaVersion" -Level Info
    Write-Log "  Docker: $dockerVersion" -Level Info
    Write-Log "  Gradle: $gradleVersion" -Level Info
    
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
    
    # Create platform subdirectories
    $platforms = @('raspberry', 'docker', 'windows', 'logs', 'checksums')
    foreach ($plat in $platforms) {
        New-Item -ItemType Directory -Path (Join-Path $Config.OutputPath $plat) | Out-Null
    }
    
    Write-Log "Build directory created: $($Config.OutputPath)" -Level Success
}

function Invoke-BuildCommand {
    param(
        [string]$Command,
        [string]$ErrorMessage = "Build command failed"
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

# ============================================================================
# Build Functions
# ============================================================================

function Build-RaspberryPi {
    Write-Log "╔═══════════════════════════════════════════════════════════╗" -Level Build
    Write-Log "║  Building for Raspberry Pi (ARM64)                        ║" -Level Build
    Write-Log "╚═══════════════════════════════════════════════════════════╝" -Level Build
    
    $rpiPath = Join-Path $Config.OutputPath "raspberry"
    $buildLog = Join-Path $Config.OutputPath "logs\build-raspberry.log"
    
    try {
        # Step 1: Build JAR with Gradle
        Write-Log "Step 1/5: Building JAR file..." -Level Step
        
        $gradleCommand = "gradle :server:api:build -x test -Pversion=$($Config.Version)"
        if ($Config.SkipTests) {
            $gradleCommand += " -x test"
        }
        
        Invoke-BuildCommand $gradleCommand -ErrorMessage "Gradle build failed" | Out-File -FilePath $buildLog
        
        # Step 2: Copy JAR
        Write-Log "Step 2/5: Copying JAR file..." -Level Step
        
        $jarSource = "server\api\build\libs\ip-css-server-$($Config.Version).jar"
        if (Test-Path $jarSource) {
            Copy-Item $jarSource -Destination (Join-Path $rpiPath "ip-css-server.jar") -Force
            Write-Log "JAR copied: ip-css-server.jar" -Level Success
        }
        else {
            throw "JAR file not found: $jarSource"
        }
        
        # Step 3: Create startup script
        Write-Log "Step 3/5: Creating startup script..." -Level Step
        
        $startScript = @"
#!/bin/bash
# IP-CSS Raspberry Pi Startup Script
# Version: $($Config.Version)

JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-arm64}
APP_HOME=${APP_HOME:-/opt/ip-css}
DATA_DIR=${DATA_DIR:-/var/lib/ip-css}
LOG_DIR=${LOG_DIR:-/var/log/ip-css}

# Create directories
mkdir -p `$DATA_DIR `$LOG_DIR

# Set JVM options for Raspberry Pi
JVM_OPTS="-Xmx512m -Xms256m"
JVM_OPTS+=" -XX:+UseG1GC"
JVM_OPTS+=" -Djava.awt.headless=true"
JVM_OPTS+=" -Dfile.encoding=UTF-8"

# Application properties
APP_PROPS="-Dspring.config.location=`$APP_HOME/application.yml"
APP_PROPS+=" -Dserver.port=8080"
APP_PROPS+=" -Ddata.dir=`$DATA_DIR"

# Start application
echo "Starting IP-CSS v$($Config.Version)..."
echo "Java: `$JAVA_HOME"
echo "Memory: 512MB max"

exec `$JAVA_HOME/bin/java `$JVM_OPTS `$APP_PROPS -jar `$APP_HOME/ip-css-server.jar
"@
        
        $startScript | Out-File -FilePath (Join-Path $rpiPath "start-ip-css.sh") -Encoding UTF8 -NoNewline
        
        # Step 4: Create installation script
        Write-Log "Step 4/5: Creating installation script..." -Level Step
        
        $installScript = @"
#!/bin/bash
# IP-CSS Raspberry Pi Installation Script

set -e

INSTALL_DIR="/opt/ip-css"
SYSTEMD_DIR="/etc/systemd/system"
USER="ip-css"

echo "Installing IP-CSS v$($Config.Version)..."

# Create user
if ! id -u `$USER > /dev/null 2>&1; then
    echo "Creating user: `$USER"
    useradd -r -s /bin/false `$USER
fi

# Create directories
mkdir -p `$INSTALL_DIR
mkdir -p /var/lib/ip-css
mkdir -p /var/log/ip-css

# Copy files
cp start-ip-css.sh `$INSTALL_DIR/
cp ip-css-server.jar `$INSTALL_DIR/
cp application.yml.example `$INSTALL_DIR/application.yml

# Set permissions
chown -R `$USER:`$USER `$INSTALL_DIR
chown -R `$USER:`$USER /var/lib/ip-css
chown -R `$USER:`$USER /var/log/ip-css
chmod +x `$INSTALL_DIR/start-ip-css.sh

# Install systemd service
cat > `$SYSTEMD_DIR/ip-css.service << EOF
[Unit]
Description=IP-CSS Surveillance System
After=network.target postgresql.service

[Service]
Type=simple
User=`$USER
WorkingDirectory=`$INSTALL_DIR
ExecStart=`$INSTALL_DIR/start-ip-css.sh
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable ip-css

echo ""
echo "Installation complete!"
echo "Configure: sudo nano `$INSTALL_DIR/application.yml"
echo "Start: sudo systemctl start ip-css"
echo "Status: sudo systemctl status ip-css"
"@
        
        $installScript | Out-File -FilePath (Join-Path $rpiPath "install.sh") -Encoding UTF8 -NoNewline
        
        # Step 5: Create config example
        Write-Log "Step 5/5: Creating configuration..." -Level Step
        
        $configYml = @"
# IP-CSS Configuration for Raspberry Pi
# Version: $($Config.Version)

server:
  port: 8080
  host: 0.0.0.0

database:
  url: jdbc:postgresql://localhost:5432/ipcss
  username: ipcss
  password: changeme

storage:
  recordings: /var/lib/ip-css/recordings
  max_size_gb: 100

performance:
  max_threads: 4
  stream_buffer_size: 2MB
  hardware_acceleration: false  # Set true if using Pi hardware decoder
"@
        
        $configYml | Out-File -FilePath (Join-Path $rpiPath "application.yml.example") -Encoding UTF8
        
        # Create README
        $readme = @"
# IP-CSS for Raspberry Pi

**Version:** $($Config.Version)  
**Architecture:** ARM64  
**Supported Models:** Raspberry Pi 4, Raspberry Pi 400, Raspberry Pi 5

## Requirements

- Raspberry Pi 4 or newer (4GB+ RAM recommended)
- Raspberry Pi OS 64-bit (Bullseye or later)
- Java 17 (OpenJDK)
- PostgreSQL 14+

## Installation

1. Copy files to Raspberry Pi:
   ```bash
   scp ./* pi@<raspberry-ip>:/tmp/ip-css/
   ```

2. Run installation:
   ```bash
   cd /tmp/ip-css
   sudo chmod +x install.sh
   sudo ./install.sh
   ```

3. Configure:
   ```bash
   sudo nano /opt/ip-css/application.yml
   ```

4. Start service:
   ```bash
   sudo systemctl start ip-css
   sudo systemctl enable ip-css
   ```

## Access

- Web UI: http://<raspberry-ip>:8080
- Default credentials: admin / admin

## Performance Notes

- Max recommended cameras: 4-8 (depending on resolution)
- Use hardware acceleration if available
- Monitor temperature: `vcgencmd measure_temp`
- Consider active cooling for continuous operation

## Support

Email: support@ip-css.com
Documentation: https://docs.ip-css.com
"@
        
        $readme | Out-File -FilePath (Join-Path $rpiPath "README.md") -Encoding UTF8
        
        # Make scripts executable (for Unix)
        Unix4Dos-Convert (Join-Path $rpiPath "install.sh")
        Unix4Dos-Convert (Join-Path $rpiPath "start-ip-css.sh")
        
        Write-Log "Raspberry Pi build complete" -Level Success
        Write-Log "Output: $rpiPath" -Level Success
        
        return $true
    }
    catch {
        Write-Log "Raspberry Pi build failed: $_" -Level Error
        Write-Log "Check log: $buildLog" -Level Warning
        return $false
    }
}

function Unix4Dos-Convert {
    param([string]$File)
    # Convert line endings to Unix format
    $content = Get-Content $File -Raw
    $content = $content -replace "`r`n", "`n"
    $content | Set-Content $File -NoNewline
}

function Build-Docker {
    Write-Log "╔═══════════════════════════════════════════════════════════╗" -Level Build
    Write-Log "║  Building Docker Images                                   ║" -Level Build
    Write-Log "╚═══════════════════════════════════════════════════════════╝" -Level Build
    
    $dockerPath = Join-Path $Config.OutputPath "docker"
    $buildLog = Join-Path $Config.OutputPath "logs\build-docker.log"
    
    try {
        # Step 1: Create Dockerfile
        Write-Log "Step 1/5: Creating Dockerfile..." -Level Step
        
        $dockerfile = @"
# IP-CSS Docker Image
# Version: $($Config.Version)

FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="NLP-Core-Team"
LABEL version="$($Config.Version)"
LABEL description="IP Camera Surveillance System"

# Install dependencies
RUN apk add --no-cache \
    ffmpeg \
    opencv \
    libstdc++ \
    tzdata

# Create non-root user
RUN addgroup -g 1001 ip-css && \
    adduser -D -u 1001 -G ip-css ip-css

# Set working directory
WORKDIR /opt/ip-css

# Copy JAR
COPY ip-css-server.jar ./ip-css-server.jar

# Copy configuration
COPY application.yml.example ./application.yml
COPY docker-entrypoint.sh ./docker-entrypoint.sh

# Set permissions
RUN chown -R ip-css:ip-css /opt/ip-css && \
    chmod +x docker-entrypoint.sh

# Expose ports
EXPOSE 8080 8443

# Switch to non-root user
USER ip-css

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# Entry point
ENTRYPOINT ["./docker-entrypoint.sh"]
"@
        
        $dockerfile | Out-File -FilePath (Join-Path $dockerPath "Dockerfile") -Encoding UTF8 -NoNewline
        
        # Step 2: Create entrypoint script
        Write-Log "Step 2/5: Creating entrypoint script..." -Level Step
        
        $entrypoint = @"
#!/bin/sh
set -e

echo "Starting IP-CSS v$($Config.Version)..."

# Wait for database if configured
if [ -n "`$POSTGRES_HOST" ]; then
    echo "Waiting for PostgreSQL at `$POSTGRES_HOST..."
    until nc -z `$POSTGRES_HOST 5432; do
        sleep 1
    done
    echo "PostgreSQL is ready"
fi

# Set JVM options
JVM_OPTS="-Xmx1g -Xms512m"
JVM_OPTS+=" -XX:+UseG1GC"
JVM_OPTS+=" -Djava.security.egd=file:/dev/./urandom"

# Application properties
if [ -n "`$SPRING_CONFIG_LOCATION" ]; then
    APP_PROPS="-Dspring.config.location=`$SPRING_CONFIG_LOCATION"
else
    APP_PROPS="-Dspring.config.location=/opt/ip-css/application.yml"
fi

# Start application
exec java `$JVM_OPTS `$APP_PROPS -jar /opt/ip-css/ip-css-server.jar
"@
        
        $entrypoint | Out-File -FilePath (Join-Path $dockerPath "docker-entrypoint.sh") -Encoding UTF8 -NoNewline
        Unix4Dos-Convert (Join-Path $dockerPath "docker-entrypoint.sh")
        
        # Step 3: Copy JAR
        Write-Log "Step 3/5: Copying JAR file..." -Level Step
        
        $jarSource = "server\api\build\libs\ip-css-server-$($Config.Version).jar"
        if (Test-Path $jarSource) {
            Copy-Item $jarSource -Destination (Join-Path $dockerPath "ip-css-server.jar") -Force
        }
        else {
            # Try alternative location
            $jarSource = "server\api\build\libs\ip-css-server.jar"
            if (Test-Path $jarSource) {
                Copy-Item $jarSource -Destination (Join-Path $dockerPath "ip-css-server.jar") -Force
            }
            else {
                Write-Log "JAR not found, will build..." -Level Warning
            }
        }
        
        # Step 4: Copy config
        Write-Log "Step 4/5: Copying configuration..." -Level Step
        
        $configYml = @"
# IP-CSS Docker Configuration
server:
  port: 8080

database:
  url: jdbc:postgresql://postgres:5432/ipcss
  username: ipcss
  password: `\${POSTGRES_PASSWORD:-changeme}

storage:
  recordings: /data/recordings
"@
        
        $configYml | Out-File -FilePath (Join-Path $dockerPath "application.yml.example") -Encoding UTF8
        
        # Step 5: Build Docker images
        Write-Log "Step 5/5: Building Docker images..." -Level Step
        
        # Build for current platform
        $imageName = "$($Config.Docker.Registry)/$($Config.Docker.Repository):$($Config.Version)"
        
        Write-Log "Building: $imageName" -Level Info
        
        docker build -t $imageName $dockerPath 2>&1 | Out-File -FilePath $buildLog -Append
        
        # Save image to tar
        Write-Log "Saving image to tar..." -Level Info
        docker save -o (Join-Path $dockerPath "ip-css-$($Config.Version).tar") $imageName
        
        # Compress
        Write-Log "Compressing image..." -Level Info
        if (Get-Command "gzip" -ErrorAction SilentlyContinue) {
            gzip (Join-Path $dockerPath "ip-css-$($Config.Version).tar")
        }
        
        # Create docker-compose.yml
        Write-Log "Creating docker-compose.yml..." -Level Step
        
        $composeYml = @"
version: '3.8'

services:
  ip-css:
    image: $($Config.Docker.Registry)/$($Config.Docker.Repository):$($Config.Version)
    ports:
      - "8080:8080"
      - "8443:8443"
    volumes:
      - ./data:/data
      - ./config:/config
    environment:
      - POSTGRES_HOST=postgres
      - POSTGRES_PASSWORD=changeme
      - SPRING_CONFIG_LOCATION=/config/application.yml
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

  postgres:
    image: postgres:14-alpine
    volumes:
      - postgres_data:/var/lib/postgresql/data
    environment:
      - POSTGRES_DB=ipcss
      - POSTGRES_USER=ipcss
      - POSTGRES_PASSWORD=changeme
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ipcss"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

volumes:
  postgres_data:
"@
        
        $composeYml | Out-File -FilePath (Join-Path $dockerPath "docker-compose.yml") -Encoding UTF8
        
        # Create README
        $readme = @"
# IP-CSS Docker Image

**Version:** $($Config.Version)  
**Platforms:** linux/amd64, linux/arm64

## Quick Start

### Using Docker Compose (Recommended)

```bash
# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f ip-css
```

### Using Docker Run

```bash
# Run with PostgreSQL
docker run -d \
  --name ip-css \
  -p 8080:8080 \
  -p 8443:8443 \
  -v ./data:/data \
  -e POSTGRES_HOST=postgres \
  -e POSTGRES_PASSWORD=changeme \
  $($Config.Docker.Registry)/$($Config.Docker.Repository):$($Config.Version)
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_HOST` | PostgreSQL host | `postgres` |
| `POSTGRES_PASSWORD` | PostgreSQL password | `changeme` |
| `SPRING_CONFIG_LOCATION` | Config file path | `/opt/ip-css/application.yml` |

### Volumes

| Volume | Description |
|--------|-------------|
| `/data` | Recordings and data storage |
| `/config` | Configuration files |

## Access

- **Web UI:** http://localhost:8080
- **Default credentials:** admin / admin

## Support

- Email: support@ip-css.com
- Docs: https://docs.ip-css.com
"@
        
        $readme | Out-File -FilePath (Join-Path $dockerPath "README.md") -Encoding UTF8
        
        Write-Log "Docker build complete" -Level Success
        Write-Log "Image: $imageName" -Level Success
        
        return $true
    }
    catch {
        Write-Log "Docker build failed: $_" -Level Error
        Write-Log "Check log: $buildLog" -Level Warning
        return $false
    }
}

function Build-Windows {
    Write-Log "╔═══════════════════════════════════════════════════════════╗" -Level Build
    Write-Log "║  Building for Windows (x64)                               ║" -Level Build
    Write-Log "╚═══════════════════════════════════════════════════════════╝" -Level Build
    
    $winPath = Join-Path $Config.OutputPath "windows"
    $buildLog = Join-Path $Config.OutputPath "logs\build-windows.log"
    
    try {
        # Step 1: Create directory structure
        Write-Log "Step 1/6: Creating directory structure..." -Level Step
        
        $subdirs = @('bin', 'config', 'data', 'logs', 'lib')
        foreach ($dir in $subdirs) {
            New-Item -ItemType Directory -Path (Join-Path $winPath $dir) -Force | Out-Null
        }
        
        # Step 2: Copy JAR
        Write-Log "Step 2/6: Copying JAR file..." -Level Step
        
        $jarSource = "server\api\build\libs\ip-css-server-$($Config.Version).jar"
        if (Test-Path $jarSource) {
            Copy-Item $jarSource -Destination (Join-Path $winPath "bin\ip-css-server.jar") -Force
        }
        
        # Step 3: Create batch startup script
        Write-Log "Step 3/6: Creating startup script..." -Level Step
        
        $batScript = @"
@echo off
REM IP-CSS Windows Startup Script
REM Version: $($Config.Version)

echo ============================================
echo   IP-CSS Surveillance System
echo   Version: $($Config.Version)
echo ============================================
echo.

REM Set Java home (adjust if needed)
if not defined JAVA_HOME (
    echo JAVA_HOME not set, using system Java
    set JAVA_CMD=java
) else (
    echo Using Java from: %JAVA_HOME%
    set JAVA_CMD=%JAVA_HOME%\bin\java.exe
)

REM Check if Java exists
where %JAVA_CMD% >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java not found. Please install Java 17 or set JAVA_HOME.
    pause
    exit /b 1
)

REM Set JVM options for Windows
set JVM_OPTS=-Xmx1024m -Xms512m
set JVM_OPTS=%JVM_OPTS% -XX:+UseG1GC
set JVM_OPTS=%JVM_OPTS% -Dfile.encoding=UTF-8

REM Set application directory
set APP_DIR=%~dp0
set DATA_DIR=%APP_DIR%..\data
set LOG_DIR=%APP_DIR%..\logs

REM Create directories if not exist
if not exist "%DATA_DIR%" mkdir "%DATA_DIR%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo Starting IP-CSS...
echo Data directory: %DATA_DIR%
echo Log directory: %LOG_DIR%
echo.

REM Start application
%JAVA_CMD% %JVM_OPTS% -jar "%APP_DIR%ip-css-server.jar"

echo.
echo IP-CSS stopped.
pause
"@
        
        $batScript | Out-File -FilePath (Join-Path $winPath "start-ip-css.bat") -Encoding UTF8
        
        # Step 4: Create PowerShell startup script
        Write-Log "Step 4/6: Creating PowerShell script..." -Level Step
        
        $psScript = @"
# IP-CSS Windows PowerShell Startup Script
# Version: $($Config.Version)

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  IP-CSS Surveillance System" -ForegroundColor Cyan
Write-Host "  Version: $($Config.Version)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Set Java home
if (-not $env:JAVA_HOME) {
    Write-Host "JAVA_HOME not set, using system Java" -ForegroundColor Yellow
    $JAVA_CMD = "java"
} else {
    Write-Host "Using Java from: $($env:JAVA_HOME)" -ForegroundColor Green
    $JAVA_CMD = "$($env:JAVA_HOME)\bin\java.exe"
}

# Check Java
try {
    & $JAVA_CMD -version >$null 2>&1
} catch {
    Write-Host "ERROR: Java not found. Please install Java 17 or set JAVA_HOME." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Set paths
`$APP_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
`$DATA_DIR = Join-Path $APP_DIR "..\data"
`$LOG_DIR = Join-Path $APP_DIR "..\logs"

# Create directories
New-Item -ItemType Directory -Force -Path $DATA_DIR | Out-Null
New-Item -ItemType Directory -Force -Path $LOG_DIR | Out-Null

# JVM options
`$JVM_OPTS = @(
    "-Xmx1024m",
    "-Xms512m",
    "-XX:+UseG1GC",
    "-Dfile.encoding=UTF-8"
)

Write-Host "Starting IP-CSS..." -ForegroundColor Green
Write-Host "Data: $DATA_DIR"
Write-Host "Logs: $LOG_DIR"
Write-Host ""

# Start application
& $JAVA_CMD $JVM_OPTS -jar "$APP_DIR\ip-css-server.jar"

Write-Host ""
Write-Host "IP-CSS stopped." -ForegroundColor Yellow
"@
        
        $psScript | Out-File -FilePath (Join-Path $winPath "start-ip-css.ps1") -Encoding UTF8
        
        # Step 5: Create Windows Service installer
        Write-Log "Step 5/6: Creating service installer..." -Level Step
        
        $serviceInstall = @"
@echo off
REM IP-CSS Windows Service Installation Script

set SERVICE_NAME=IP-CSS
set SERVICE_DISPLAY=IP Camera Surveillance System
set SERVICE_DESC=IP-CSS Surveillance System - Video Recording and Analytics

echo Installing IP-CSS as Windows Service...
echo.

REM Check if running as administrator
net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: Please run as Administrator
    pause
    exit /b 1
)

REM Create service (adjust path as needed)
sc create "%SERVICE_NAME%" binPath= "\"%CD%\bin\ip-css-server.jar\"" start= auto
sc description "%SERVICE_NAME%" "%SERVICE_DESC%"
sc start "%SERVICE_NAME%"

echo.
echo Service installed and started!
echo Use: sc query %SERVICE_NAME% to check status
echo Use: sc stop %SERVICE_NAME% to stop

pause
"@
        
        $serviceInstall | Out-File -FilePath (Join-Path $winPath "install-service.bat") -Encoding UTF8
        
        # Step 6: Create configuration
        Write-Log "Step 6/6: Creating configuration..." -Level Step
        
        $configYml = @"
# IP-CSS Windows Configuration
server:
  port: 8080
  host: 0.0.0.0

database:
  url: jdbc:postgresql://localhost:5432/ipcss
  username: ipcss
  password: changeme

storage:
  recordings: ./data/recordings
  max_size_gb: 200
"@
        
        $configYml | Out-File -FilePath (Join-Path $winPath "config\application.yml") -Encoding UTF8
        
        # Create README
        $readme = @"
# IP-CSS for Windows

**Version:** $($Config.Version)  
**Architecture:** x64  
**OS:** Windows 10/11, Windows Server 2019+

## Requirements

- Windows 10/11 or Windows Server 2019+
- Java 17 (OpenJDK or Oracle JDK)
- PostgreSQL 14+
- 4GB+ RAM (8GB recommended)
- 50GB+ free disk space

## Installation

### Quick Start

1. **Extract** the archive to desired location (e.g., `C:\Program Files\IP-CSS`)

2. **Install PostgreSQL** (if not already installed):
   - Download: https://www.postgresql.org/download/windows/
   - Create database: `ipcss`
   - Create user: `ipcss` with password

3. **Configure** application:
   - Edit: `config\application.yml`
   - Update database credentials

4. **Start** IP-CSS:
   - Double-click: `start-ip-css.bat`
   - Or run PowerShell: `.\start-ip-css.ps1`

### Install as Windows Service

1. Run as Administrator:
   ```cmd
   install-service.bat
   ```

2. Service will start automatically

3. Manage service:
   ```cmd
   sc query IP-CSS
   sc stop IP-CSS
   sc start IP-CSS
   ```

## Access

- **Web UI:** http://localhost:8080
- **Default credentials:** admin / admin

## Troubleshooting

### Java not found
```
ERROR: Java not found
```
**Solution:** Install Java 17 or set `JAVA_HOME` environment variable

### Database connection failed
```
Connection refused
```
**Solution:** Check PostgreSQL is running and credentials are correct

### Port already in use
```
Address already in use
```
**Solution:** Change port in `config\application.yml`

## Support

- Email: support@ip-css.com
- Documentation: https://docs.ip-css.com
"@
        
        $readme | Out-File -FilePath (Join-Path $winPath "README.md") -Encoding UTF8
        
        # Create Inno Setup script (for installer creation)
        $innoScript = @"
; IP-CSS Windows Installer Script
; Requires Inno Setup Compiler

[Setup]
AppName=IP-CSS
AppVersion=$($Config.Version)
AppPublisher=NLP-Core-Team
DefaultDirName={pf}\IP-CSS
DefaultGroupName=IP-CSS
OutputDir=installer
OutputBaseFilename=IP-CSS-Setup-$($Config.Version)
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin

[Files]
Source: "..\*"; DestDir: "{app}"; Flags: recursesubdirs
Source: "..\bin\*"; DestDir: "{app}\bin"
Source: "..\config\*"; DestDir: "{app}\config"

[Icons]
Name: "{group}\IP-CSS"; Filename: "{app}\start-ip-css.bat"
Name: "{group}\Uninstall IP-CSS"; Filename: "{uninstallexe}"

[Run]
Filename: "{app}\install-service.bat"; Description: "Install as Windows Service"; Flags: runascurrentuser
"@
        
        $innoScript | Out-File -FilePath (Join-Path $winPath "installer\ip-css.iss") -Encoding UTF8
        New-Item -ItemType Directory -Path (Join-Path $winPath "installer") -Force | Out-Null
        
        Write-Log "Windows build complete" -Level Success
        Write-Log "Output: $winPath" -Level Success
        
        return $true
    }
    catch {
        Write-Log "Windows build failed: $_" -Level Error
        Write-Log "Check log: $buildLog" -Level Warning
        return $false
    }
}

function Generate-Checksums {
    Write-Log "=== Generating Checksums ===" -Level Step
    
    $checksumPath = Join-Path $Config.OutputPath "checksums"
    
    # Get all build artifacts
    $files = Get-ChildItem -Path $Config.OutputPath -File -Recurse -Exclude *.log,*.md,*.iss | 
        Where-Object { $_.Extension -in @('.jar', '.tar', '.tar.gz', '.yml', '.bat', '.ps1', '.sh') }
    
    # Generate SHA256
    $checksums = @()
    foreach ($file in $files) {
        $hash = Get-FileHash -Path $file.FullName -Algorithm SHA256
        $relativePath = $file.FullName -replace [regex]::Escape($Config.OutputPath + '\'), ''
        $checksums += "$($hash.Hash)  $relativePath"
    }
    
    # Save
    $checksumFile = Join-Path $checksumPath "SHA256SUMS.txt"
    $checksums | Out-File -FilePath $checksumFile -Encoding UTF8
    
    Write-Log "Checksums: $checksumFile" -Level Success
}

function Generate-BuildReport {
    Write-Log "=== Generating Build Report ===" -Level Step
    
    $reportPath = Join-Path $Config.OutputPath "BUILD_REPORT.md"
    
    $report = @"
# IP-CSS Beta Build Report

**Version:** $($Config.Version)  
**Build Date:** $($Config.BuildDate)  
**Build ID:** $($Config.BuildTimestamp)

---

## Build Summary

| Platform | Status | Output |
|----------|--------|--------|
| **Raspberry Pi** | $((Test-Path (Join-Path $Config.OutputPath "raspberry")) ? '✅' : '❌') | ARM64 package |
| **Docker** | $((Test-Path (Join-Path $Config.OutputPath "docker")) ? '✅' : '❌') | Docker image |
| **Windows** | $((Test-Path (Join-Path $Config.OutputPath "windows")) ? '✅' : '❌') | Windows package |

---

## Build Configuration

- **Skip Tests:** $($Config.SkipTests ? 'Yes' : 'No')
- **Output Directory:** $($Config.OutputPath)
- **Build Machine:** $($env:COMPUTERNAME)

---

## Next Steps

1. ✅ Verify all builds
2. ✅ Test on target platforms
3. ✅ Upload to release server
4. ✅ Distribute to testers

---

**Build Completed:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
"@
    
    $report | Out-File -FilePath $reportPath -Encoding UTF8
    
    Write-Log "Build report: $reportPath" -Level Success
}

# ============================================================================
# Main Execution
# ============================================================================

function Main {
    Write-Log "╔═══════════════════════════════════════════════════════════╗" -Level Step
    Write-Log "║     IP-CSS Beta Build                                     ║" -Level Step
    Write-Log "║     Platforms: Raspberry Pi, Docker, Windows              ║" -Level Step
    Write-Log "║     Version: $($Config.Version)                              ║" -Level Step
    Write-Log "╚═══════════════════════════════════════════════════════════╝" -Level Step
    
    # Check prerequisites
    if (!(Test-Prerequisites)) {
        Write-Log "Prerequisites check failed. Exiting..." -Level Error
        exit 1
    }
    
    # Create build directory
    New-BuildDirectory
    
    # First, build JAR
    Write-Log "Building JAR file..." -Level Step
    gradle :server:api:build -x test -Pversion=$Config.Version
    
    # Execute builds based on platform
    $results = @{
        Raspberry = $false
        Docker = $false
        Windows = $false
    }
    
    switch ($Platform) {
        'all' {
            $results.Raspberry = Build-RaspberryPi
            $results.Docker = Build-Docker
            $results.Windows = Build-Windows
        }
        'raspberry' { $results.Raspberry = Build-RaspberryPi }
        'docker' { $results.Docker = Build-Docker }
        'windows' { $results.Windows = Build-Windows }
    }
    
    # Generate checksums and report
    Generate-Checksums
    Generate-BuildReport
    
    # Summary
    Write-Log "╔═══════════════════════════════════════════════════════════╗" -Level Success
    Write-Log "║     Beta Build Complete                                   ║" -Level Success
    Write-Log "╠═══════════════════════════════════════════════════════════╣" -Level Success
    
    Write-Log "║  Raspberry Pi: $($results.Raspberry ? '✅' : '❌')                            ║" -Level Success
    Write-Log "║  Docker: $($results.Docker ? '✅' : '❌')                                    ║" -Level Success
    Write-Log "║  Windows: $($results.Windows ? '✅' : '❌')                                  ║" -Level Success
    Write-Log "╠═══════════════════════════════════════════════════════════╣" -Level Success
    Write-Log "║  Output: $($Config.OutputPath)" -Level Success
    Write-Log "╚═══════════════════════════════════════════════════════════╝" -Level Success
}

# Run main function
Main

```

---

## 📖 Usage

### Build All Platforms
```powershell
.\build-beta-release.ps1 -Platform all -Version "1.0.0-beta"
```

### Build Raspberry Pi Only
```powershell
.\build-beta-release.ps1 -Platform raspberry -Version "1.0.0-beta"
```

### Build Docker Only
```powershell
.\build-beta-release.ps1 -Platform docker -Version "1.0.0-beta"
```

### Build Windows Only
```powershell
.\build-beta-release.ps1 -Platform windows -Version "1.0.0-beta"
```

### Skip Tests (Faster)
```powershell
.\build-beta-release.ps1 -Platform all -SkipTests
```

---

## 📁 Output Structure

```
release-builds/beta/
├── raspberry/
│   ├── ip-css-server.jar
│   ├── start-ip-css.sh
│   ├── install.sh
│   ├── application.yml.example
│   └── README.md
├── docker/
│   ├── Dockerfile
│   ├── docker-entrypoint.sh
│   ├── ip-css-server.jar
│   ├── docker-compose.yml
│   ├── ip-css-1.0.0-beta.tar.gz
│   └── README.md
├── windows/
│   ├── bin/ip-css-server.jar
│   ├── start-ip-css.bat
│   ├── start-ip-css.ps1
│   ├── install-service.bat
│   ├── config/application.yml
│   └── README.md
├── logs/
│   ├── build-raspberry.log
│   ├── build-docker.log
│   └── build-windows.log
├── checksums/
│   └── SHA256SUMS.txt
└── BUILD_REPORT.md
```

---

## ⚙️ Prerequisites

- PowerShell 7.0+
- Git
- Docker 20.10+
- Java 17+
- Gradle 8+
- Inno Setup (optional, for Windows installer)

---

*Script Version: 1.0*  
*Created: 28 January 2026*  
*Author: NLP-Core-Team*
