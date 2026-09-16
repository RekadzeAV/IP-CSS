# 🍓 Raspberry Pi 4 Jenkins CI/CD Setup

## 📋 Overview

This directory contains scripts to set up Jenkins CI/CD on Raspberry Pi 4 for building IP-CSS project with automatic artifact deployment to NAS.

## 🔐 Credentials

**SMB/CIFS Credentials (from `credentials/.env.nas`):**
- **User:** `VSCode`
- **Password:** `$QN~snl0` (base64 encoded in scripts as `JFFOfnNubDA=`)
- **NAS Host:** `192.168.10.40`

## 📁 Directory Structure

```
scripts/
├── rpi-mount-smb.sh              # Manual SMB mount script
├── rpi-setup-fstab.sh            # Configure auto-mount in /etc/fstab
├── rpi-install-jenkins.sh        # Install Jenkins only
├── rpi-jenkins-init.groovy       # Jenkins initialization script
├── rpi-setup-jenkins.sh          # ⭐ COMPLETE SETUP (recommended)
├── Jenkinsfile-*                 # Build pipeline definitions
└── README-RPI-JENKINS.md         # This file
```

## 🚀 Quick Start

### Prerequisites

- Raspberry Pi 4 (4GB) with Raspberry Pi OS (64-bit)
- Network access to `192.168.10.40`
- SSH access to Raspberry Pi as user `andrey`
- Project source code available

### Installation Steps

#### Option 1: Complete Setup (Recommended)

Run the unified setup script on Raspberry Pi:

```bash
# Copy scripts to Raspberry Pi
scp -r scripts/ andrey@192.168.10.46:/home/andrey/

# SSH to Raspberry Pi
ssh andrey@192.168.10.46

# Run setup script
cd ~/scripts
chmod +x rpi-setup-jenkins.sh
./rpi-setup-jenkins.sh
```

This script will:
1. ✅ Install required packages:
   - **apt-transport-https, ca-certificates, gnupg, lsb-release** - system utilities
   - **cifs-utils, keyutils** - for SMB/CIFS mounting
   - **openjdk-17-jdk** - Java runtime for Jenkins
   - **git, curl, wget** - version control and downloads
   - **unzip, zip** - archive tools
   - **build-essential** - compilation tools (gcc, make, etc.)
   - **python3, python3-pip** - Python runtime
   - **docker.io** - Docker engine
   - **jenkins** - CI/CD server (from official repository)
   - **gradle 8.5** - Kotlin/Java build tool
   - **node.js 20.x** - JavaScript runtime
2. ✅ Configure SMB mounts in `/etc/fstab`
3. ✅ Mount NAS shares
4. ✅ Install and start Jenkins
5. ✅ Configure firewall (UFW)
6. ✅ Initialize Jenkins jobs and pipelines
7. ✅ Copy Jenkinsfiles to project

#### Option 2: Manual Step-by-Step

If you prefer manual control:

```bash
# 1. Mount SMB shares
./rpi-mount-smb.sh

# 2. Configure auto-mount
./rpi-setup-fstab.sh

# 3. Install Jenkins
./rpi-install-jenkins.sh

# 4. Initialize Jenkins (via web UI or CLI)
sudo cp rpi-jenkins-init.groovy /var/lib/jenkins/init.groovy.d/
sudo systemctl restart jenkins
```

## 🔧 Configuration

### SMB Mount Points

| Share | Mount Point | Purpose |
|-------|-------------|---------|
| `\\192.168.10.40\AI-prodgect` | `/mnt/nas/AI-prodgect` | Project source code |
| `\\192.168.10.40\AI-prodgect-2` | `/mnt/nas/AI-prodgect-2` | Build artifacts |

### Build Artifacts Structure

```
\\192.168.10.40\AI-prodgect-2\release\
├── desktop-linux\
│   ├── test\
│   └── prod\
├── desktop-windows\
│   ├── test\
│   └── prod\
├── desktop-macos\
│   ├── test\
│   └── prod\
├── android\
│   ├── test\
│   └── prod\
├── server\
│   ├── test\
│   └── prod\
└── native-libs\
    ├── test\
    └── prod\
```

## 🏗️ Build Types

The following build pipelines are configured:

| Job Name | Description | Artifacts |
|----------|-------------|-----------|
| `IP-CSS-desktop-linux` | Desktop Linux (x64) | JAR, ZIP |
| `IP-CSS-desktop-windows` | Desktop Windows (x64) | JAR, EXE |
| `IP-CSS-desktop-macos` | Desktop macOS (x64/ARM) | JAR, DMG |
| `IP-CSS-android` | Android (ARM64) | APK, AAB |
| `IP-CSS-server` | Server (Linux x64) | JAR, ZIP |
| `IP-CSS-native-libs` | Native libraries | SO, DYLIB, DLL |

## 📝 Jenkins Pipeline Parameters

Each build job accepts the following parameter:

- **DEPLOY_TARGET**: `test` (default) or `prod`
  - `test` → deploys to `\\192.168.10.40\AI-prodgect-2\release\{build-type}\test\`
  - `prod` → deploys to `\\192.168.10.40\AI-prodgect-2\release\{build-type}\prod\`

## 🔍 Verification

### Check SMB Mounts

```bash
# List mounted shares
mount | grep /mnt/nas

# Check mount points
df -h /mnt/nas/AI-prodgect
df -h /mnt/nas/AI-prodgect-2

# Test access
ls -la /mnt/nas/AI-prodgect
ls -la /mnt/nas/AI-prodgect-2/release
```

### Check Jenkins

```bash
# Check Jenkins status
sudo systemctl status jenkins

# View Jenkins logs
sudo journalctl -u jenkins -f

# Check Jenkins is running
curl http://localhost:8080
```

### Check Mounts on Boot

```bash
# Reboot Raspberry Pi
sudo reboot

# After reboot, verify mounts
mount | grep /mnt/nas
```

## 🛠️ Troubleshooting

### SMB Mount Issues

```bash
# Check credentials file
cat /home/andrey/.smbcredentials

# Test manual mount
sudo mount -t cifs //192.168.10.40/AI-prodgect /mnt/nas/AI-prodgect \
    -o credentials=/home/andrey/.smbcredentials,iocharset=utf8

# Check mount errors
dmesg | grep CIFS
```

### Jenkins Issues

```bash
# Restart Jenkins
sudo systemctl restart jenkins

# Check Jenkins logs
sudo journalctl -u jenkins -f

# Get initial password
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

### Permission Issues

```bash
# Fix mount permissions
sudo chown -R andrey:andrey /mnt/nas
sudo chmod -R 775 /mnt/nas
```

## 🔄 Updating Jenkins Configuration

### Re-run Initialization

```bash
# Copy init script
sudo cp rpi-jenkins-init.groovy /var/lib/jenkins/init.groovy.d/ipcss-init.groovy
sudo chown jenkins:jenkins /var/lib/jenkins/init.groovy.d/ipcss-init.groovy

# Restart Jenkins
sudo systemctl restart jenkins

# Wait 30 seconds for initialization
sleep 30
```

### Update Jenkinsfiles

```bash
# Copy updated Jenkinsfiles to project
cp Jenkinsfile-* /mnt/nas/AI-prodgect/
```

## 📊 Monitoring

### Jenkins Web UI

Access Jenkins at: `http://192.168.10.46:8080`

Default credentials:
- Username: (created during setup)
- Password: (initial password shown in setup output)

### Build Monitoring

```bash
# View build logs
sudo tail -f /var/lib/jenkins/jobs/IP-CSS-desktop-linux/builds/latest/log

# List all jobs
sudo ls -la /var/lib/jenkins/jobs/
```

## 🔒 Security Notes

1. **Credentials Storage:**
   - SMB credentials stored in `/home/andrey/.smbcredentials` (mode 600)
   - Jenkins credentials stored in `/var/lib/jenkins/credentials.xml`
   - Never commit credentials to Git

2. **Firewall:**
   - Port 8080 (Jenkins) - accessible from local network
   - Port 22 (SSH) - accessible from local network
   - Consider restricting access via `ufw` if needed

3. **NAS Access:**
   - SMB shares mounted with `noperm` option
   - Files owned by `andrey:andrey`
   - Permissions set to 775

## 📚 Additional Resources

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [Raspberry Pi OS](https://www.raspberrypi.com/software/)
- [CIFS/SMB Mounting](https://wiki.ubuntu.com/MountWindowsSharesPermanently)

## 🆘 Support

If you encounter issues:

1. Check logs: `sudo journalctl -u jenkins -f`
2. Verify mounts: `mount | grep /mnt/nas`
3. Test SMB connectivity: `smbclient //192.168.10.40/AI-prodgect -U VSCode`
4. Check disk space: `df -h /mnt/nas`

---

**Last Updated:** 2026-07-08  
**Author:** IP-CSS DevOps Team