#!/bin/bash
# Install Jenkins on Raspberry Pi 4 (ARM64)
# Date: 2026-07-08

set -e

echo "=== Jenkins Installation for Raspberry Pi 4 (ARM64) ==="
echo ""

# Check if running on Raspberry Pi
if ! grep -q "Raspberry Pi" /proc/device-tree/model 2>/dev/null; then
    echo "⚠️  Warning: This script is designed for Raspberry Pi"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Update system
echo "📦 Updating system packages..."
sudo apt-get update -qq
sudo apt-get upgrade -y -qq

# Install Java (Jenkins requires Java 17 or 21)
echo "☕ Installing Java 17..."
sudo apt-get install -y -qq openjdk-17-jdk

# Verify Java installation
java -version
echo "   ✓ Java installed"

# Add Jenkins repository
echo "🔑 Adding Jenkins repository..."
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
    /usr/share/keyrings/jenkins-keyring.asc > /dev/null

echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
    https://pkg.jenkins.io/debian-stable binary/" | sudo tee \
    /etc/apt/sources.list.d/jenkins.list > /dev/null

# Install Jenkins
echo "📥 Installing Jenkins..."
sudo apt-get update -qq
sudo apt-get install -y -qq jenkins

# Start and enable Jenkins
echo "▶️  Starting Jenkins service..."
sudo systemctl enable jenkins
sudo systemctl start jenkins

# Wait for Jenkins to start
echo "⏳ Waiting for Jenkins to start..."
sleep 30

# Check Jenkins status
if sudo systemctl is-active --quiet jenkins; then
    echo "   ✓ Jenkins is running"
else
    echo "   ✗ Jenkins failed to start"
    sudo systemctl status jenkins
    exit 1
fi

# Install additional tools
echo "🛠️  Installing additional build tools..."
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

print_status "Additional build tools installed"

# Add jenkins user to docker group
echo "🐳 Adding jenkins user to docker group..."
sudo usermod -aG docker jenkins

# Install Gradle (for Kotlin/Java builds)
echo "📦 Installing Gradle..."
if ! command -v gradle &> /dev/null; then
    GRADLE_VERSION=8.5
    wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip
    sudo unzip -q /tmp/gradle.zip -d /opt/gradle
    sudo ln -s /opt/gradle/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle
    rm /tmp/gradle.zip
    echo "   ✓ Gradle ${GRADLE_VERSION} installed"
fi

# Install Node.js (for frontend builds)
echo "📦 Installing Node.js..."
if ! command -v node &> /dev/null; then
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
    sudo apt-get install -y -qq nodejs
    echo "   ✓ Node.js $(node --version) installed"
fi

# Configure firewall
echo "🔥 Configuring firewall..."
sudo ufw allow 8080/tcp  # Jenkins web interface
sudo ufw allow 22/tcp     # SSH
sudo ufw --force enable

# Get Jenkins initial password
echo ""
echo "🔐 Jenkins initial admin password:"
if [ -f /var/lib/jenkins/secrets/initialAdminPassword ]; then
    sudo cat /var/lib/jenkins/secrets/initialAdminPassword
else
    echo "   Password file not found, check Jenkins logs"
fi

echo ""
echo "✅ Jenkins installation completed!"
echo ""
echo "Access Jenkins at: http://$(hostname -I | awk '{print $1}'):8080"
echo ""
echo "Next steps:"
echo "1. Open Jenkins in browser"
echo "2. Use the initial admin password shown above"
echo "3. Install suggested plugins"
echo "4. Create admin user"
echo "5. Configure Jenkins pipeline"