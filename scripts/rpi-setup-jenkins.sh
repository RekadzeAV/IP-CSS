#!/bin/bash
# Complete Jenkins Setup Script for Raspberry Pi 4
# This script performs the complete setup:
# 1. Mounts NAS shares
# 2. Configures fstab for auto-mount
# 3. Installs Jenkins
# 4. Initializes Jenkins jobs
# Date: 2026-07-08

set -e

echo "=========================================="
echo "  Raspberry Pi 4 Jenkins Setup"
echo "=========================================="
echo ""

# Configuration
NAS_HOST="192.168.10.40"
SHARE_PROJECT="AI-prodgect"
SHARE_BUILDS="AI-prodgect-2"
MOUNT_BASE="/mnt/nas"
SMB_USER="VSCode"
# Store password - use base64 to avoid any shell expansion issues with $ character
SMB_PASSWORD_BASE64='JFFOfnNubDA='

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper function
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check if running on Raspberry Pi
if ! grep -q "Raspberry Pi" /proc/device-tree/model 2>/dev/null; then
    print_warning "This script is designed for Raspberry Pi"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Step 1: Install required packages
echo ""
echo "📦 Step 1/5: Installing required packages..."
sudo apt-get update -qq
sudo apt-get install -y -qq \
    apt-transport-https \
    ca-certificates \
    gnupg \
    lsb-release \
    cifs-utils \
    keyutils \
    git \
    curl \
    wget \
    unzip \
    zip \
    build-essential \
    python3 \
    python3-pip \
    docker.io

print_status "Required packages installed"

# Step 2: Configure SMB mounts
echo ""
echo "🔗 Step 2/5: Configuring SMB mounts..."

# Create mount points
sudo mkdir -p "${MOUNT_BASE}/${SHARE_PROJECT}"
sudo mkdir -p "${MOUNT_BASE}/${SHARE_BUILDS}/release"

# Decode password from base64 to avoid shell expansion issues
SMB_PASSWORD=$(echo "$SMB_PASSWORD_BASE64" | base64 -d)

# Test SMB connectivity first before configuring fstab
echo "🔍 Testing SMB connectivity..."
if ! smbclient "//${NAS_HOST}/${SHARE_PROJECT}" -U "${SMB_USER}%${SMB_PASSWORD}" -c "ls" >/dev/null 2>&1; then
    print_warning "SMB connectivity test failed, but continuing..."
    echo "   This might be due to firewall or NAS configuration"
fi

# Create credentials file using Python - write password as-is (no escaping needed)
CREDENTIALS_FILE="/home/andrey/.smbcredentials"
TMP_PYTHON_SCRIPT="/tmp/create_creds.py"
cat > "$TMP_PYTHON_SCRIPT" <<'PYEOF'
import sys
with open(sys.argv[1], 'w') as f:
    f.write('username=' + sys.argv[2] + '\n')
    f.write('password=' + sys.argv[3] + '\n')
    f.write('domain=WORKGROUP\n')
PYEOF
python3 "$TMP_PYTHON_SCRIPT" "$CREDENTIALS_FILE" "$SMB_USER" "$SMB_PASSWORD"
rm -f "$TMP_PYTHON_SCRIPT"
sudo chmod 600 "$CREDENTIALS_FILE"
sudo chown andrey:andrey "$CREDENTIALS_FILE"

# Backup and configure fstab
sudo cp /etc/fstab /etc/fstab.backup.$(date +%Y%m%d_%H%M%S)

# Remove old entries
sudo sed -i "\|//${NAS_HOST}/${SHARE_PROJECT}|d" /etc/fstab
sudo sed -i "\|//${NAS_HOST}/${SHARE_BUILDS}|d" /etc/fstab

# Add new entries using credentials file
echo "" | sudo tee -a /etc/fstab
echo "# NAS SMB Shares - Added $(date +%Y-%m-%d)" | sudo tee -a /etc/fstab
echo "//${NAS_HOST}/${SHARE_PROJECT} ${MOUNT_BASE}/${SHARE_PROJECT} cifs credentials=${CREDENTIALS_FILE},iocharset=utf8,file_mode=0775,dir_mode=0775,noperm,sec=ntlmssp,vers=3.0,nounix 0 0" | sudo tee -a /etc/fstab
echo "//${NAS_HOST}/${SHARE_BUILDS} ${MOUNT_BASE}/${SHARE_BUILDS} cifs credentials=${CREDENTIALS_FILE},iocharset=utf8,file_mode=0775,dir_mode=0775,noperm,sec=ntlmssp,vers=3.0,nounix 0 0" | sudo tee -a /etc/fstab

# Test mounts
echo "🧪 Testing fstab configuration..."
if sudo mount -a 2>&1; then
    print_status "fstab configured and mounts tested"
    
    # Verify mounts are actually accessible
    if mountpoint -q "${MOUNT_BASE}/${SHARE_PROJECT}" && mountpoint -q "${MOUNT_BASE}/${SHARE_BUILDS}"; then
        print_status "Both shares mounted successfully"
    else
        print_warning "fstab valid but mounts not active, trying manual mount..."
        sudo mount "${MOUNT_BASE}/${SHARE_PROJECT}" 2>&1 || true
        sudo mount "${MOUNT_BASE}/${SHARE_BUILDS}" 2>&1 || true
    fi
else
    print_error "fstab configuration failed"
    echo ""
    echo "🔍 Debugging information:"
    echo "  1. Check credentials file:"
    echo "     cat ${CREDENTIALS_FILE}"
    echo ""
    echo "  2. Test manual mount with verbose output:"
    echo "     sudo mount -t cifs //${NAS_HOST}/${SHARE_PROJECT} ${MOUNT_BASE}/${SHARE_PROJECT} \\"
    echo "       -o credentials=${CREDENTIALS_FILE},sec=ntlmssp,vers=3.0 -o vers=2.1 -o vers=1.0"
    echo ""
    echo "  3. Check kernel messages:"
    echo "     dmesg | tail -30"
    echo ""
    echo "  4. Test SMB connectivity:"
    echo "     smbclient //${NAS_HOST}/${SHARE_PROJECT} -U ${SMB_USER}"
    echo ""
    echo "  5. Check if NAS is reachable:"
    echo "     ping -c 3 ${NAS_HOST}"
    echo ""
    exit 1
fi

# Set permissions
sudo chown -R andrey:andrey "${MOUNT_BASE}/${SHARE_PROJECT}"
sudo chown -R andrey:andrey "${MOUNT_BASE}/${SHARE_BUILDS}"
sudo chmod -R 775 "${MOUNT_BASE}/${SHARE_PROJECT}"
sudo chmod -R 775 "${MOUNT_BASE}/${SHARE_BUILDS}"

print_status "SMB mounts configured"

# Step 3: Install Java
echo ""
echo "☕ Step 3/5: Installing Java 17..."
sudo apt-get install -y -qq openjdk-17-jdk
print_status "Java 17 installed"

# Step 4: Install Jenkins
echo ""
echo "📥 Step 4/5: Installing Jenkins..."

# Add Jenkins repository
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
    /usr/share/keyrings/jenkins-keyring.asc > /dev/null

echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
    https://pkg.jenkins.io/debian-stable binary/" | sudo tee \
    /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt-get update -qq
sudo apt-get install -y -qq jenkins

# Start Jenkins
sudo systemctl enable jenkins
sudo systemctl start jenkins

# Wait for Jenkins to start
echo "⏳ Waiting for Jenkins to start..."
sleep 30

if sudo systemctl is-active --quiet jenkins; then
    print_status "Jenkins installed and running"
else
    print_error "Jenkins failed to start"
    exit 1
fi

# Install Gradle
echo "📦 Installing Gradle..."
if ! command -v gradle &> /dev/null; then
    GRADLE_VERSION=8.5
    wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip
    sudo unzip -q /tmp/gradle.zip -d /opt/gradle
    sudo ln -s /opt/gradle/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle
    rm /tmp/gradle.zip
    print_status "Gradle ${GRADLE_VERSION} installed"
fi

# Install Node.js
echo "📦 Installing Node.js..."
if ! command -v node &> /dev/null; then
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
    sudo apt-get install -y -qq nodejs
    print_status "Node.js $(node --version) installed"
fi

# Add jenkins user to docker group
sudo usermod -aG docker jenkins

# Configure firewall
sudo ufw allow 8080/tcp --force
sudo ufw allow 22/tcp --force
sudo ufw --force enable

print_status "Jenkins installation completed"

# Step 5: Initialize Jenkins
echo ""
echo "⚙️  Step 5/5: Initializing Jenkins configuration..."

# Wait a bit more for Jenkins to be fully ready
sleep 10

# Copy initialization script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
INIT_SCRIPT="${SCRIPT_DIR}/rpi-jenkins-init.groovy"

if [ -f "$INIT_SCRIPT" ]; then
    # Copy init script to Jenkins
    sudo cp "$INIT_SCRIPT" /var/lib/jenkins/init.groovy.d/ipcss-init.groovy
    sudo chown jenkins:jenkins /var/lib/jenkins/init.groovy.d/ipcss-init.groovy
    
    # Restart Jenkins to apply initialization
    sudo systemctl restart jenkins
    echo "⏳ Waiting for Jenkins to initialize..."
    sleep 30
    
    print_status "Jenkins initialization script deployed"
else
    print_warning "Initialization script not found: $INIT_SCRIPT"
    print_warning "You'll need to configure Jenkins manually"
fi

# Copy Jenkinsfiles to project directory
echo ""
echo "📄 Copying Jenkinsfiles to project directory..."
PROJECT_DIR="${MOUNT_BASE}/${SHARE_PROJECT}/IP-CSS"
if [ -d "$PROJECT_DIR" ]; then
    cp "${SCRIPT_DIR}/Jenkinsfile-"* "$PROJECT_DIR/" 2>/dev/null || true
    print_status "Jenkinsfiles copied to project"
else
    print_warning "Project directory not found: $PROJECT_DIR"
    print_warning "Copy Jenkinsfiles manually after mounting"
fi

# Summary
echo ""
echo "=========================================="
echo "  Setup Completed!"
echo "=========================================="
echo ""
echo "📋 Summary:"
echo "  • SMB mounts configured in /etc/fstab"
echo "  • Mount points:"
echo "    - Project: ${MOUNT_BASE}/${SHARE_PROJECT}"
echo "    - Builds:  ${MOUNT_BASE}/${SHARE_BUILDS}"
echo "  • Jenkins installed and running"
echo ""
echo "🔐 Jenkins Access:"
echo "  URL: http://$(hostname -I | awk '{print $1}'):8080"
echo ""
echo "🔑 Initial Admin Password:"
if [ -f /var/lib/jenkins/secrets/initialAdminPassword ]; then
    sudo cat /var/lib/jenkins/secrets/initialAdminPassword
else
    echo "   Check Jenkins logs: sudo journalctl -u jenkins"
fi
echo ""
echo "📝 Next Steps:"
echo "  1. Open Jenkins in browser"
echo "  2. Install suggested plugins"
echo "  3. Create admin user"
echo "  4. Configure Git repository"
echo "  5. Test build pipelines"
echo ""
echo "📁 Build Artifacts Location:"
echo "  ${MOUNT_BASE}/${SHARE_BUILDS}/release/{build-type}/{test|prod}"
echo ""
echo "🔧 Useful Commands:"
echo "  • Check Jenkins status: sudo systemctl status jenkins"
echo "  • View Jenkins logs: sudo journalctl -u jenkins -f"
echo "  • Restart Jenkins: sudo systemctl restart jenkins"
echo "  • Check mounts: mount | grep ${MOUNT_BASE}"
echo ""