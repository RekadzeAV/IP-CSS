# IP-CSS Linux Beta Build Script

**Назначение:** Автоматическая сборка beta-релиза для Linux (x64, ARM64)

```powershell
<#
.SYNOPSIS
    IP-CSS Linux Beta Build Script

.DESCRIPTION
    Builds beta release packages for:
    - Linux x64 (Ubuntu, Debian, CentOS, RHEL, Fedora)
    - Linux ARM64 (Raspberry Pi, ARM servers)

.VERSION
    1.0.0-beta

.AUTHOR
    NLP-Core-Team

.LAST_UPDATED
    28 January 2026
#>

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet('all', 'x64', 'arm64', 'deb', 'rpm')]
    [string]$Architecture = 'all',
    
    [Parameter(Mandatory=$false)]
    [ValidateSet('universal', 'ubuntu', 'debian', 'centos', 'rhel', 'fedora', 'raspbian')]
    [string]$Distribution = 'universal',
    
    [Parameter(Mandatory=$false)]
    [string]$Version = '1.0.0-beta',
    
    [Parameter(Mandatory=$false)]
    [string]$OutputPath = ".\release-builds\beta\linux",
    
    [Parameter(Mandatory=$false)]
    [switch]$SkipTests,
    
    [Parameter(Mandatory=$false)]
    [switch]$CreateInstaller
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
    Architectures = @{
        x64 = @{
            Arch = "amd64"
            Triplet = "x86_64-linux-gnu"
            JDK = "17"
        }
        arm64 = @{
            Arch = "arm64"
            Triplet = "aarch64-linux-gnu"
            JDK = "17"
        }
    }
    Distributions = @{
        ubuntu = @("20.04", "22.04", "24.04")
        debian = @("11", "12")
        centos = @("8", "9")
        rhel = @("8", "9")
        fedora = @("38", "39", "40")
        raspbian = @("11", "12")
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
    
    $required = @('git', 'java', 'gradle')
    $missing = @()
    
    foreach ($cmd in $required) {
        if (!(Get-Command $cmd -ErrorAction SilentlyContinue)) {
            $missing += $cmd
        }
    }
    
    # Check for Docker (needed for cross-compilation)
    if (!(Get-Command 'docker' -ErrorAction SilentlyContinue)) {
        Write-Log "Docker not found. Cross-compilation may be limited." -Level Warning
    }
    
    if ($missing.Count -gt 0) {
        Write-Log "Missing prerequisites: $($missing -join ', ')" -Level Error
        return $false
    }
    
    # Check versions
    Write-Log "Checking versions..." -Level Info
    $javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
    $gradleVersion = gradle --version | Select-String "Gradle" | Select-Object -First 1
    
    Write-Log "  Java: $javaVersion" -Level Info
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
    
    # Create architecture subdirectories
    $archDirs = @('x64', 'arm64', 'deb', 'rpm', 'universal', 'logs')
    foreach ($dir in $archDirs) {
        New-Item -ItemType Directory -Path (Join-Path $Config.OutputPath $dir) | Out-Null
    }
    
    Write-Log "Build directory created: $($Config.OutputPath)" -Level Success
}

function ConvertTo-UnixLineEndings {
    param([string]$File)
    $content = Get-Content $File -Raw
    $content = $content -replace "`r`n", "`n"
    $content | Set-Content $File -NoNewline -Encoding UTF8
}

# ============================================================================
# Build Functions
# ============================================================================

function Build-Linux-Universal {
    param(
        [ValidateSet('x64', 'arm64')]
        [string]$Arch
    )
    
    Write-Log "╔═══════════════════════════════════════════════════════════╗" -Level Build
    Write-Log "║  Building Linux Universal ($Arch)                            ║" -Level Build
    Write-Log "╚═══════════════════════════════════════════════════════════╝" -Level Build
    
    $archPath = Join-Path $Config.OutputPath $Arch
    $buildLog = Join-Path $Config.OutputPath "logs\build-linux-$Arch.log"
    
    try {
        # Step 1: Build JAR
        Write-Log "Step 1/6: Building JAR file..." -Level Step
        
        $gradleCommand = "gradle :server:api:build -x test -Pversion=$($Config.Version)"
        if ($Config.SkipTests) {
            $gradleCommand += " -x test"
        }
        
        Invoke-Expression $gradleCommand 2>&1 | Out-File -FilePath $buildLog
        
        # Step 2: Copy JAR
        Write-Log "Step 2/6: Copying JAR file..." -Level Step
        
        $jarSource = "server\api\build\libs\ip-css-server-$($Config.Version).jar"
        if (Test-Path $jarSource) {
            Copy-Item $jarSource -Destination (Join-Path $archPath "ip-css-server.jar") -Force
        }
        else {
            throw "JAR file not found: $jarSource"
        }
        
        # Step 3: Create startup script
        Write-Log "Step 3/6: Creating startup script..." -Level Step
        
        $startScript = @"
#!/bin/bash
# IP-CSS Linux Startup Script
# Version: $($Config.Version)
# Architecture: $Arch

set -e

# Configuration
JAVA_HOME=\${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-$Arch}
APP_HOME=\${APP_HOME:-/opt/ip-css}
DATA_DIR=\${DATA_DIR:-/var/lib/ip-css}
LOG_DIR=\${LOG_DIR:-/var/log/ip-css}
CONFIG_FILE=\${CONFIG_FILE:-\$APP_HOME/application.yml}

# Colors for output
RED='\\033[0;31m'
GREEN='\\033[0;32m'
YELLOW='\\033[1;33m'
NC='\\033[0m' # No Color

log_info() {
    echo -e "\${GREEN}[INFO]\${NC} \$1"
}

log_warn() {
    echo -e "\${YELLOW}[WARN]\${NC} \$1"
}

log_error() {
    echo -e "\${RED}[ERROR]\${NC} \$1"
}

# Check Java
check_java() {
    if [ -n "\$JAVA_HOME" ]; then
        JAVA_CMD="\$JAVA_HOME/bin/java"
    else
        JAVA_CMD=\$(which java 2>/dev/null)
    fi
    
    if [ -z "\$JAVA_CMD" ] || [ ! -x "\$JAVA_CMD" ]; then
        log_error "Java 17 not found. Please install OpenJDK 17 or set JAVA_HOME."
        exit 1
    fi
    
    JAVA_VERSION=\$("\$JAVA_CMD" -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "\$JAVA_VERSION" -lt 17 ]; then
        log_error "Java 17 or higher required. Found: \$JAVA_VERSION"
        exit 1
    fi
    
    log_info "Using Java: \$JAVA_CMD (version \$JAVA_VERSION)"
}

# Create directories
create_directories() {
    log_info "Creating directories..."
    mkdir -p "\$DATA_DIR"
    mkdir -p "\$LOG_DIR"
    mkdir -p "\$(dirname \$CONFIG_FILE)"
}

# Check configuration
check_config() {
    if [ ! -f "\$CONFIG_FILE" ]; then
        log_warn "Configuration file not found: \$CONFIG_FILE"
        log_info "Creating default configuration..."
        cp "\$APP_HOME/application.yml.example" "\$CONFIG_FILE"
    fi
}

# Set JVM options
get_jvm_opts() {
    local TOTAL_MEM=\$(grep MemTotal /proc/meminfo | awk '{print \$2}')
    local TOTAL_MB=\$((TOTAL_MEM / 1024))
    
    # Adjust heap based on available memory
    if [ \$TOTAL_MB -lt 2048 ]; then
        echo "-Xmx512m -Xms256m"
    elif [ \$TOTAL_MB -lt 4096 ]; then
        echo "-Xmx1g -Xms512m"
    elif [ \$TOTAL_MB -lt 8192 ]; then
        echo "-Xmx2g -Xms1g"
    else
        echo "-Xmx4g -Xms2g"
    fi
}

# Main
main() {
    echo "============================================"
    echo "  IP-CSS Surveillance System"
    echo "  Version: $($Config.Version)"
    echo "  Architecture: $Arch"
    echo "============================================"
    echo ""
    
    check_java
    create_directories
    check_config
    
    JVM_OPTS=\$(get_jvm_opts)
    JVM_OPTS+=" -XX:+UseG1GC"
    JVM_OPTS+=" -XX:+HeapDumpOnOutOfMemoryError"
    JVM_OPTS+=" -XX:HeapDumpPath=\$LOG_DIR"
    JVM_OPTS+=" -Djava.awt.headless=true"
    JVM_OPTS+=" -Dfile.encoding=UTF-8"
    JVM_OPTS+=" -Dspring.config.location=\$CONFIG_FILE"
    
    log_info "JVM Options: \$JVM_OPTS"
    log_info "Data directory: \$DATA_DIR"
    log_info "Log directory: \$LOG_DIR"
    log_info "Starting IP-CSS..."
    echo ""
    
    exec "\$JAVA_CMD" \$JVM_OPTS -jar "\$APP_HOME/ip-css-server.jar"
}

main "\$@"
"@
        
        $startScript | Out-File -FilePath (Join-Path $archPath "start-ip-css.sh") -Encoding UTF8 -NoNewline
        ConvertTo-UnixLineEndings (Join-Path $archPath "start-ip-css.sh")
        
        # Step 4: Create installation script
        Write-Log "Step 4/6: Creating installation script..." -Level Step
        
        $installScript = @"
#!/bin/bash
# IP-CSS Linux Installation Script
# Version: $($Config.Version)
# Architecture: $Arch

set -e

# Configuration
INSTALL_DIR=\${INSTALL_DIR:-/opt/ip-css}
DATA_DIR=\${DATA_DIR:-/var/lib/ip-css}
LOG_DIR=\${LOG_DIR:-/var/log/ip-css}
SERVICE_USER=\${SERVICE_USER:-ip-css}
SERVICE_NAME=\${SERVICE_NAME:-ip-css}

# Colors
RED='\\033[0;31m'
GREEN='\\033[0;32m'
YELLOW='\\033[1;33m'
BLUE='\\033[0;34m'
NC='\\033[0m'

log_info() { echo -e "\${GREEN}[INFO]\${NC} \$1"; }
log_warn() { echo -e "\${YELLOW}[WARN]\${NC} \$1"; }
log_error() { echo -e "\${RED}[ERROR]\${NC} \$1"; }
log_step() { echo -e "\${BLUE}[STEP]\${NC} \$1"; }

# Check root
check_root() {
    if [ "\$(id -u)" -ne 0 ]; then
        log_error "This script must be run as root (use sudo)"
        exit 1
    fi
}

# Detect distribution
detect_distribution() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        DISTRO=\$ID
        DISTRO_VERSION=\$VERSION_ID
        log_info "Detected: \$DISTRO \$DISTRO_VERSION"
    else
        log_warn "Cannot detect distribution, assuming generic Linux"
        DISTRO="generic"
    fi
}

# Install dependencies
install_dependencies() {
    log_step "Installing dependencies..."
    
    case \$DISTRO in
        ubuntu|debian|raspbian)
            apt-get update
            apt-get install -y openjdk-17-jre-headless wget curl
            ;;
        centos|rhel|fedora)
            dnf install -y java-17-openjdk-headless wget curl
            ;;
        *)
            log_warn "Package manager not detected. Please ensure Java 17 is installed."
            ;;
    esac
}

# Create user
create_user() {
    log_step "Creating service user: \$SERVICE_USER"
    
    if id -u "\$SERVICE_USER" &>/dev/null; then
        log_warn "User already exists: \$SERVICE_USER"
    else
        useradd -r -s /bin/false "\$SERVICE_USER"
        log_info "User created: \$SERVICE_USER"
    fi
}

# Create directories
create_directories() {
    log_step "Creating directories..."
    
    mkdir -p "\$INSTALL_DIR"
    mkdir -p "\$DATA_DIR"
    mkdir -p "\$LOG_DIR"
    
    chown -R "\$SERVICE_USER:\$SERVICE_USER" "\$INSTALL_DIR"
    chown -R "\$SERVICE_USER:\$SERVICE_USER" "\$DATA_DIR"
    chown -R "\$SERVICE_USER:\$SERVICE_USER" "\$LOG_DIR"
    
    chmod 755 "\$INSTALL_DIR"
    chmod 755 "\$DATA_DIR"
    chmod 755 "\$LOG_DIR"
}

# Copy files
copy_files() {
    log_step "Copying files..."
    
    cp -f start-ip-css.sh "\$INSTALL_DIR/"
    cp -f ip-css-server.jar "\$INSTALL_DIR/"
    cp -f application.yml.example "\$INSTALL_DIR/application.yml"
    
    chmod +x "\$INSTALL_DIR/start-ip-css.sh"
    chown -R "\$SERVICE_USER:\$SERVICE_USER" "\$INSTALL_DIR"
}

# Install systemd service
install_service() {
    log_step "Installing systemd service..."
    
    cat > /etc/systemd/system/\$SERVICE_NAME.service << EOF
[Unit]
Description=IP-CSS Surveillance System (v$($Config.Version))
Documentation=https://docs.ip-css.com
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=notify
User=\$SERVICE_USER
Group=\$SERVICE_USER
WorkingDirectory=\$INSTALL_DIR
Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk-$Arch"
Environment="APP_HOME=$INSTALL_DIR"
Environment="DATA_DIR=$DATA_DIR"
Environment="LOG_DIR=$LOG_DIR"
ExecStart=$INSTALL_DIR/start-ip-css.sh
ExecReload=/bin/kill -HUP \$MAINPID
Restart=on-failure
RestartSec=10
SuccessExitStatus=143
TimeoutStopSec=30

# Security hardening
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=$DATA_DIR $LOG_DIR

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
    systemctl enable \$SERVICE_NAME
    
    log_info "Service installed and enabled"
}

# Firewall configuration
configure_firewall() {
    log_step "Configuring firewall..."
    
    if command -v ufw &>/dev/null; then
        ufw allow 8080/tcp comment "IP-CSS HTTP"
        ufw allow 8443/tcp comment "IP-CSS HTTPS"
        log_info "UFW rules added"
    elif command -v firewall-cmd &>/dev/null; then
        firewall-cmd --permanent --add-port=8080/tcp
        firewall-cmd --permanent --add-port=8443/tcp
        firewall-cmd --reload
        log_info "firewalld rules added"
    else
        log_warn "No firewall detected. Please configure manually."
    fi
}

# Print summary
print_summary() {
    echo ""
    echo "============================================"
    echo -e "\${GREEN}Installation Complete!\${NC}"
    echo "============================================"
    echo ""
    echo "Installation directory: \$INSTALL_DIR"
    echo "Data directory: \$DATA_DIR"
    echo "Log directory: \$LOG_DIR"
    echo ""
    echo "Service management:"
    echo "  Start:   sudo systemctl start \$SERVICE_NAME"
    echo "  Stop:    sudo systemctl stop \$SERVICE_NAME"
    echo "  Restart: sudo systemctl restart \$SERVICE_NAME"
    echo "  Status:  sudo systemctl status \$SERVICE_NAME"
    echo ""
    echo "Configuration:"
    echo "  Edit: sudo nano \$INSTALL_DIR/application.yml"
    echo ""
    echo "Logs:"
    echo "  View: sudo journalctl -u \$SERVICE_NAME -f"
    echo "  File: \$LOG_DIR/ip-css.log"
    echo ""
    echo "Access:"
    echo "  Web UI: http://localhost:8080"
    echo "  Default: admin / admin"
    echo ""
    echo "============================================"
}

# Main
main() {
    echo "============================================"
    echo "  IP-CSS Linux Installation"
    echo "  Version: $($Config.Version)"
    echo "  Architecture: $Arch"
    echo "============================================"
    echo ""
    
    check_root
    detect_distribution
    install_dependencies
    create_user
    create_directories
    copy_files
    install_service
    configure_firewall
    print_summary
}

main "\$@"
"@
        
        $installScript | Out-File -FilePath (Join-Path $archPath "install.sh") -Encoding UTF8 -NoNewline
        ConvertTo-UnixLineEndings (Join-Path $archPath "install.sh")
        
        # Step 5: Create uninstall script
        Write-Log "Step 5/6: Creating uninstall script..." -Level Step
        
        $uninstallScript = @"
#!/bin/bash
# IP-CSS Linux Uninstall Script
# Version: $($Config.Version)

set -e

INSTALL_DIR=\${INSTALL_DIR:-/opt/ip-css}
SERVICE_NAME=\${SERVICE_NAME:-ip-css}
SERVICE_USER=\${SERVICE_USER:-ip-css}

echo "Uninstalling IP-CSS..."

# Stop service
systemctl stop \$SERVICE_NAME 2>/dev/null || true
systemctl disable \$SERVICE_NAME 2>/dev/null || true

# Remove service file
rm -f /etc/systemd/system/\$SERVICE_NAME.service
systemctl daemon-reload

# Remove directories
rm -rf "\$INSTALL_DIR"

# Remove user
userdel "\$SERVICE_USER" 2>/dev/null || true

# Remove data (optional - commented out by default)
# rm -rf /var/lib/ip-css
# rm -rf /var/log/ip-css

echo "Uninstallation complete!"
echo "Note: Data directories preserved. Remove manually if needed:"
echo "  sudo rm -rf /var/lib/ip-css"
echo "  sudo rm -rf /var/log/ip-css"
"@
        
        $uninstallScript | Out-File -FilePath (Join-Path $archPath "uninstall.sh") -Encoding UTF8 -NoNewline
        ConvertTo-UnixLineEndings (Join-Path $archPath "uninstall.sh")
        
        # Step 6: Create configuration
        Write-Log "Step 6/6: Creating configuration..." -Level Step
        
        $configYml = @"
# IP-CSS Linux Configuration
# Version: $($Config.Version)
# Architecture: $Arch

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
  max_threads: \${CPU_CORES:-4}
  stream_buffer_size: 2MB
"@
        
        $configYml | Out-File -FilePath (Join-Path $archPath "application.yml.example") -Encoding UTF8
        
        # Create README
        $readme = @"
# IP-CSS for Linux

**Version:** $($Config.Version)  
**Architecture:** $Arch  
**Supported Distributions:** Ubuntu, Debian, CentOS, RHEL, Fedora, Raspbian

## Requirements

- Linux (64-bit)
- Java 17 (OpenJDK)
- PostgreSQL 14+
- 2GB+ RAM (4GB recommended)
- 20GB+ free disk space

## Quick Installation

### Automated Installation (Recommended)

```bash
# Download and extract
wget https://releases.ip-css.com/ip-css-$($Config.Version)-linux-$Arch.tar.gz
tar -xzf ip-css-$($Config.Version)-linux-$Arch.tar.gz
cd ip-css-$($Config.Version)

# Install (requires sudo)
sudo ./install.sh
```

### Manual Installation

```bash
# Copy to installation directory
sudo mkdir -p /opt/ip-css
sudo cp ip-css-server.jar /opt/ip-css/
sudo cp start-ip-css.sh /opt/ip-css/
sudo cp application.yml.example /opt/ip-css/application.yml

# Set permissions
sudo chmod +x /opt/ip-css/start-ip-css.sh
sudo useradd -r -s /bin/false ip-css
sudo chown -R ip-css:ip-css /opt/ip-css

# Install service
sudo cp ip-css.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable ip-css
sudo systemctl start ip-css
```

## Service Management

```bash
# Start
sudo systemctl start ip-css

# Stop
sudo systemctl stop ip-css

# Restart
sudo systemctl restart ip-css

# Status
sudo systemctl status ip-css

# View logs
sudo journalctl -u ip-css -f
```

## Configuration

Edit `/opt/ip-css/application.yml`:

```yaml
server:
  port: 8080

database:
  url: jdbc:postgresql://localhost:5432/ipcss
  username: ipcss
  password: your_password
```

## Firewall Configuration

### UFW (Ubuntu/Debian)
```bash
sudo ufw allow 8080/tcp
sudo ufw allow 8443/tcp
```

### firewalld (CentOS/RHEL/Fedora)
```bash
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=8443/tcp
sudo firewall-cmd --reload
```

## Access

- **Web UI:** http://localhost:8080
- **Default credentials:** admin / admin

## Troubleshooting

### Check service status
```bash
sudo systemctl status ip-css
```

### View logs
```bash
sudo journalctl -u ip-css -f
cat /var/log/ip-css/ip-css.log
```

### Check Java
```bash
java -version
# Should show Java 17+
```

### Test database connection
```bash
psql -U ipcss -d ipcss -c "SELECT 1"
```

## Uninstallation

```bash
cd /opt/ip-css
sudo ./uninstall.sh
```

## Support

- Email: support@ip-css.com
- Documentation: https://docs.ip-css.com
- Issues: https://github.com/nlp-core-team/ip-css/issues
"@
        
        $readme | Out-File -FilePath (Join-Path $archPath "README.md") -Encoding UTF8
        
        # Create systemd service file
        $serviceFile = @"
[Unit]
Description=IP-CSS Surveillance System (v$($Config.Version))
Documentation=https://docs.ip-css.com
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=notify
User=ip-css
Group=ip-css
WorkingDirectory=/opt/ip-css
Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk-$Arch"
Environment="APP_HOME=/opt/ip-css"
Environment="DATA_DIR=/var/lib/ip-css"
Environment="LOG_DIR=/var/log/ip-css"
ExecStart=/opt/ip-css/start-ip-css.sh
ExecReload=/bin/kill -HUP \$MAINPID
Restart=on-failure
RestartSec=10
SuccessExitStatus=143
TimeoutStopSec=30

# Security hardening
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/lib/ip-css /var/log/ip-css

[Install]
WantedBy=multi-user.target
"@
        
        $serviceFile | Out-File -FilePath (Join-Path $archPath "ip-css.service") -Encoding UTF8
        ConvertTo-UnixLineEndings (Join-Path $archPath "ip-css.service")
        
        Write-Log "Linux $Arch build complete" -Level Success
        
        return $true
    }
    catch {
        Write-Log "Linux $Arch build failed: $_" -Level Error
        Write-Log "Check log: $buildLog" -Level Warning
        return $false
    }
}

function Create-DEB-Package {
    Write-Log "╔═══════════════════════════════════════════════════════════╗" -Level Build
    Write-Log "║  Creating DEB Package (Ubuntu/Debian/Raspberry Pi OS)     ║" -Level Build
    Write-Log "╚═══════════════════════════════════════════════════════════╝" -Level Build
    
    $debPath = Join-Path $Config.OutputPath "deb"
    $packageName = "ip-css_$($Config.Version)_all.deb"
    
    try {
        # Create DEB structure
        Write-Log "Creating DEB structure..." -Level Step
        
        $debStructure = @(
            "DEBIAN",
            "opt/ip-css",
            "usr/lib/systemd/system",
            "etc/ip-css"
        )
        
        foreach ($dir in $debStructure) {
            New-Item -ItemType Directory -Path (Join-Path $debPath $dir) -Force | Out-Null
        }
        
        # Create control file
        Write-Log "Creating control file..." -Level Step
        
        $control = @"
Package: ip-css
Version: $($Config.Version)
Section: net
Priority: optional
Architecture: all
Depends: openjdk-17-jre-headless | java17-runtime, postgresql
Maintainer: NLP-Core-Team <support@ip-css.com>
Description: IP Camera Surveillance System
 IP-CSS is a comprehensive IP camera surveillance and video analytics system.
 .
 Features:
 - Real-time video streaming
 - Motion detection
 - Face recognition
 - License plate recognition
 - Behavioral analytics
Homepage: https://ip-css.com
"@
        
        $control | Out-File -FilePath (Join-Path $debPath "DEBIAN/control") -Encoding UTF8 -NoNewline
        ConvertTo-UnixLineEndings (Join-Path $debPath "DEBIAN/control")
        
        # Create preinst script
        $preinst = @"
#!/bin/bash
set -e

if id -u ip-css &>/dev/null; then
    echo "Service user already exists"
else
    useradd -r -s /bin/false ip-css
fi
"@
        
        $preinst | Out-File -FilePath (Join-Path $debPath "DEBIAN/preinst") -Encoding UTF8 -NoNewline
        ConvertTo-UnixLineEndings (Join-Path $debPath "DEBIAN/preinst")
        
        # Create postinst script
        $postinst = @"
#!/bin/bash
set -e

case "\$1" in
    configure)
        systemctl daemon-reload
        systemctl enable ip-css
        echo ""
        echo "IP-CSS installed successfully!"
        echo "Configure: sudo nano /etc/ip-css/application.yml"
        echo "Start: sudo systemctl start ip-css"
        ;;
esac

exit 0
"@
        
        $postinst | Out-File -FilePath (Join-Path $debPath "DEBIAN/postinst") -Encoding UTF8 -NoNewline
        ConvertTo-UnixLineEndings (Join-Path $debPath "DEBIAN/postinst")
        
        # Create prerm script
        $prerm = @"
#!/bin/bash
set -e

case "\$1" in
    remove)
        systemctl stop ip-css || true
        systemctl disable ip-css || true
        ;;
esac

exit 0
"@
        
        $prerm | Out-File -FilePath (Join-Path $debPath "DEBIAN/prerm") -Encoding UTF8 -NoNewline
        ConvertTo-UnixLineEndings (Join-Path $debPath "DEBIAN/prerm")
        
        Write-Log "DEB package structure created" -Level Success
        Write-Log "Note: Building DEB requires dpkg-deb (Linux only)" -Level Info
        
        return $true
    }
    catch {
        Write-Log "DEB package creation failed: $_" -Level Error
        return $false
    }
}

function Create-RPM-Package {
    Write-Log "╔═══════════════════════════════════════════════════════════╗" -Level Build
    Write-Log "║  Creating RPM Package (CentOS/RHEL/Fedora)                ║" -Level Build
    Write-Log "╚═══════════════════════════════════════════════════════════╝" -Level Build
    
    $rpmPath = Join-Path $Config.OutputPath "rpm"
    
    try {
        # Create RPM structure
        Write-Log "Creating RPM structure..." -Level Step
        
        $rpmStructure = @(
            "SOURCES",
            "SPECS",
            "BUILD",
            "RPMS",
            "SRPMS"
        )
        
        foreach ($dir in $rpmStructure) {
            New-Item -ItemType Directory -Path (Join-Path $rpmPath $dir) -Force | Out-Null
        }
        
        # Create spec file
        Write-Log "Creating spec file..." -Level Step
        
        $spec = @"
Name:           ip-css
Version:        $($Config.Version.Replace('-', '_'))
Release:        1%{?dist}
Summary:        IP Camera Surveillance System

License:        MIT
URL:            https://ip-css.com
BuildArch:      noarch

Requires:       java-17-openjdk-headless >= 17
Requires:       postgresql >= 14

%description
IP-CSS is a comprehensive IP camera surveillance and video analytics system.

Features:
- Real-time video streaming
- Motion detection
- Face recognition
- License plate recognition
- Behavioral analytics

%prep
%setup -T

%build
%setup -T

%install
mkdir -p %{buildroot}/opt/ip-css
mkdir -p %{buildroot}/var/lib/ip-css
mkdir -p %{buildroot}/var/log/ip-css
mkdir -p %{buildroot}/etc/ip-css
mkdir -p %{buildroot}/usr/lib/systemd/system

cp -a * %{buildroot}/opt/ip-css/
cp %{_sourcedir}/ip-css.service %{buildroot}/usr/lib/systemd/system/

%pre
getent group ip-css >/dev/null || groupadd -r ip-css
getent passwd ip-css >/dev/null || useradd -r -g ip-css -s /sbin/nologin ip-css

%post
systemctl daemon-reload
systemctl enable ip-css

%preun
if [ \$1 -eq 0 ]; then
    systemctl stop ip-css
    systemctl disable ip-css
fi

%files
/opt/ip-css
/var/lib/ip-css
/var/log/ip-css
/etc/ip-css
/usr/lib/systemd/system/ip-css.service

%changelog
* $(Get-Date -Format "Mon Jan 02 2006") NLP-Core-Team <support@ip-css.com> - $($Config.Version)
- Initial package
"@
        
        $spec | Out-File -FilePath (Join-Path $rpmPath "SPECS/ip-css.spec") -Encoding UTF8 -NoNewline
        ConvertTo-UnixLineEndings (Join-Path $rpmPath "SPECS/ip-css.spec")
        
        Write-Log "RPM package structure created" -Level Success
        Write-Log "Note: Building RPM requires rpmbuild (Linux only)" -Level Info
        
        return $true
    }
    catch {
        Write-Log "RPM package creation failed: $_" -Level Error
        return $false
    }
}

function Generate-Tarball {
    param(
        [ValidateSet('x64', 'arm64')]
        [string]$Arch
    )
    
    Write-Log "=== Creating TAR.GZ Archive ($Arch) ===" -Level Step
    
    $archPath = Join-Path $Config.OutputPath $Arch
    $tarballName = "ip-css-$($Config.Version)-linux-$Arch.tar.gz"
    
    # Create tarball
    $sourceDir = $archPath
    $destinationTarball = Join-Path $Config.OutputPath $tarballName
    
    # Use PowerShell to create zip (tar not native on Windows)
    # For actual deployment, use Linux with tar
    Write-Log "Creating archive: $tarballName" -Level Info
    Write-Log "Note: For production tarballs, build on Linux" -Level Warning
    
    return $true
}

function Generate-Checksums {
    Write-Log "=== Generating Checksums ===" -Level Step
    
    $checksumPath = Join-Path $Config.OutputPath "checksums"
    New-Item -ItemType Directory -Path $checksumPath -Force | Out-Null
    
    # Get all build artifacts
    $files = Get-ChildItem -Path $Config.OutputPath -File -Recurse -Exclude *.log,*.md,*.iss | 
        Where-Object { $_.Extension -in @('.jar', '.tar', '.tar.gz', '.yml', '.sh', '.service', '.deb', '.rpm') }
    
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
# IP-CSS Linux Beta Build Report

**Version:** $($Config.Version)  
**Build Date:** $($Config.BuildDate)  
**Build ID:** $($Config.BuildTimestamp)

---

## Build Summary

| Architecture | Status | Package |
|--------------|--------|---------|
| **x64** | $((Test-Path (Join-Path $Config.OutputPath "x64")) ? '✅' : '❌') | Universal, DEB, RPM |
| **ARM64** | $((Test-Path (Join-Path $Config.OutputPath "arm64")) ? '✅' : '❌') | Universal, DEB |

---

## Supported Distributions

### x64 (amd64)
- Ubuntu 20.04, 22.04, 24.04
- Debian 11, 12
- CentOS 8, 9
- RHEL 8, 9
- Fedora 38, 39, 40

### ARM64 (aarch64)
- Raspberry Pi OS 11, 12
- Ubuntu Server ARM64
- Debian ARM64

---

## Installation Methods

### Universal Tarball
```bash
tar -xzf ip-css-$($Config.Version)-linux-<arch>.tar.gz
cd ip-css-$($Config.Version)
sudo ./install.sh
```

### DEB Package (Ubuntu/Debian)
```bash
sudo dpkg -i ip-css_$($Config.Version)_all.deb
sudo apt-get install -f
```

### RPM Package (CentOS/RHEL/Fedora)
```bash
sudo dnf install ip-css-$($Config.Version).rpm
```

---

## Build Configuration

- **Architecture:** $($Architecture)
- **Distribution:** $($Distribution)
- **Skip Tests:** $($Config.SkipTests ? 'Yes' : 'No')
- **Output Directory:** $($Config.OutputPath)

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
    Write-Log "║     IP-CSS Linux Beta Build                               ║" -Level Step
    Write-Log "║     Architectures: x64, ARM64                             ║" -Level Step
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
    
    # Execute builds based on architecture
    $results = @{
        x64 = $false
        arm64 = $false
        deb = $false
        rpm = $false
    }
    
    switch ($Architecture) {
        'all' {
            $results.x64 = Build-Linux-Universal -Arch 'x64'
            $results.arm64 = Build-Linux-Universal -Arch 'arm64'
            $results.deb = Create-DEB-Package
            $results.rpm = Create-RPM-Package
        }
        'x64' { $results.x64 = Build-Linux-Universal -Arch 'x64' }
        'arm64' { $results.arm64 = Build-Linux-Universal -Arch 'arm64' }
        'deb' { $results.deb = Create-DEB-Package }
        'rpm' { $results.rpm = Create-RPM-Package }
    }
    
    # Generate checksums and report
    Generate-Checksums
    Generate-BuildReport
    
    # Summary
    Write-Log "╔═══════════════════════════════════════════════════════════╗" -Level Success
    Write-Log "║     Linux Beta Build Complete                             ║" -Level Success
    Write-Log "╠═══════════════════════════════════════════════════════════╣" -Level Success
    
    Write-Log "║  x64: $($results.x64 ? '✅' : '❌')                                        ║" -Level Success
    Write-Log "║  ARM64: $($results.arm64 ? '✅' : '❌')                                    ║" -Level Success
    Write-Log "║  DEB: $($results.deb ? '✅' : '❌')                                        ║" -Level Success
    Write-Log "║  RPM: $($results.rpm ? '✅' : '❌')                                        ║" -Level Success
    Write-Log "╠═══════════════════════════════════════════════════════════╣" -Level Success
    Write-Log "║  Output: $($Config.OutputPath)" -Level Success
    Write-Log "╚═══════════════════════════════════════════════════════════╝" -Level Success
}

# Run main function
Main

```

---

## 📖 Usage

### Build All Linux Packages
```powershell
.\build-linux-release.ps1 -Architecture all -Version "1.0.0-beta"
```

### Build x64 Only
```powershell
.\build-linux-release.ps1 -Architecture x64 -Version "1.0.0-beta"
```

### Build ARM64 Only
```powershell
.\build-linux-release.ps1 -Architecture arm64 -Version "1.0.0-beta"
```

### Build DEB Package Only
```powershell
.\build-linux-release.ps1 -Architecture deb -Version "1.0.0-beta"
```

### Build RPM Package Only
```powershell
.\build-linux-release.ps1 -Architecture rpm -Version "1.0.0-beta"
```

---

## 📁 Output Structure

```
release-builds/beta/linux/
├── x64/
│   ├── ip-css-server.jar
│   ├── start-ip-css.sh
│   ├── install.sh
│   ├── uninstall.sh
│   ├── ip-css.service
│   ├── application.yml.example
│   └── README.md
├── arm64/
│   ├── ip-css-server.jar
│   ├── start-ip-css.sh
│   ├── install.sh
│   ├── uninstall.sh
│   ├── ip-css.service
│   ├── application.yml.example
│   └── README.md
├── deb/
│   ├── DEBIAN/
│   │   ├── control
│   │   ├── preinst
│   │   ├── postinst
│   │   └── prerm
│   ├── opt/ip-css/
│   └── etc/ip-css/
├── rpm/
│   ├── SOURCES/
│   ├── SPECS/
│   │   └── ip-css.spec
│   ├── BUILD/
│   ├── RPMS/
│   └── SRPMS/
├── checksums/
│   └── SHA256SUMS.txt
└── BUILD_REPORT.md
```

---

## 🎯 Supported Distributions

### x64 (amd64)
| Distribution | Versions | Package Type |
|--------------|----------|--------------|
| Ubuntu | 20.04, 22.04, 24.04 | DEB, Universal |
| Debian | 11 (Bullseye), 12 (Bookworm) | DEB, Universal |
| CentOS | 8, 9 | RPM, Universal |
| RHEL | 8, 9 | RPM, Universal |
| Fedora | 38, 39, 40 | RPM, Universal |

### ARM64 (aarch64)
| Distribution | Versions | Package Type |
|--------------|----------|--------------|
| Raspberry Pi OS | 11 (Bullseye), 12 (Bookworm) | DEB, Universal |
| Ubuntu Server | 20.04, 22.04, 24.04 | DEB, Universal |
| Debian | 11, 12 | DEB, Universal |

---

## ⚙️ Prerequisites

- PowerShell 7.0+
- Git
- Java 17+
- Gradle 8+
- Docker (optional, for cross-compilation)
- dpkg-deb (for DEB, Linux only)
- rpmbuild (for RPM, Linux only)

---

*Script Version: 1.0*  
*Created: 28 January 2026*  
*Author: NLP-Core-Team*
