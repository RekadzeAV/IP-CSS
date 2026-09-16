# Beta Environment Setup Guide

**Версия:** 0.3.0-beta  
**Статус:** 🟡 **PREPARING**  
**Audience:** Beta Testers, DevOps Team

---

## 🎯 Overview

This guide provides step-by-step instructions for setting up the IP-CSS beta testing environment.

**Estimated Time:** 30-60 minutes  
**Difficulty:** Intermediate

---

## 📋 Prerequisites

### Hardware Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **CPU** | 4 cores | 8+ cores |
| **RAM** | 8 GB | 16+ GB |
| **Storage** | 50 GB free | 100+ GB SSD |
| **Network** | 100 Mbps | 1 Gbps |
| **IP Cameras** | 1 | 3-5 |

### Software Requirements

| Platform | Version | Required |
|----------|---------|----------|
| **Docker** | 20.10+ | ✅ (Backend) |
| **Docker Compose** | 2.0+ | ✅ (Backend) |
| **Node.js** | 18+ | ⏸️ (Web UI dev only) |
| **Java** | 17+ | ⏸️ (Desktop only) |
| **iOS** | 14+ | ✅ (iPhone/iPad) |
| **Android** | 10+ | ✅ (Phone/Tablet) |

### Network Requirements

- Open ports: 8080 (HTTP), 8443 (HTTPS), 554 (RTSP)
- Static IP or DNS name (recommended)
- Internet access (for updates, telemetry)
- Local network access (for camera discovery)

---

## 🖥️ Backend Setup

### Option 1: Docker (Recommended)

**Step 1: Create Directory**
```bash
mkdir -p ~/ip-css-beta
cd ~/ip-css-beta
```

**Step 2: Download Docker Compose**
```bash
# Download docker-compose.yml
curl -O https://releases.ip-css.com/0.3.0-beta/docker-compose.yml

# Download .env example
curl -O https://releases.ip-css.com/0.3.0-beta/.env.example
cp .env.example .env
```

**Step 3: Configure Environment**
```bash
# Edit .env file
nano .env

# Required variables:
IP_CSS_VERSION=0.3.0-beta
POSTGRES_PASSWORD=<your-password>
JWT_SECRET=<your-secret-key>
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=<your-admin-password>
```

**Step 4: Start Services**
```bash
# Pull images
docker-compose pull

# Start services
docker-compose up -d

# Check status
docker-compose ps
```

**Step 5: Verify Installation**
```bash
# Check logs
docker-compose logs -f api

# Access web UI
# Open browser: http://localhost:8080

# Default credentials:
# Username: admin
# Password: (set in .env)
```

---

### Option 2: JAR File (Alternative)

**Step 1: Download JAR**
```bash
# Download from releases
curl -O https://releases.ip-css.com/0.3.0-beta/ip-css-server-0.3.0-beta.jar
```

**Step 2: Install PostgreSQL**
```bash
# Ubuntu/Debian
sudo apt-get install postgresql postgresql-contrib

# Create database
sudo -u postgres psql
CREATE DATABASE ipcss;
CREATE USER ipcss WITH PASSWORD 'your-password';
GRANT ALL PRIVILEGES ON DATABASE ipcss TO ipcss;
\q
```

**Step 3: Run Application**
```bash
# Set environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ipcss
export SPRING_DATASOURCE_USERNAME=ipcss
export SPRING_DATASOURCE_PASSWORD=your-password
export JWT_SECRET=your-secret-key

# Run JAR
java -jar ip-css-server-0.3.0-beta.jar
```

---

## 🌐 Web UI Setup

### Option 1: Docker (Recommended)

**Step 1: Add to Docker Compose**
```yaml
# Add to existing docker-compose.yml
services:
  web:
    image: ghcr.io/nlp-core-team/ip-css-web:0.3.0-beta
    ports:
      - "3000:3000"
    environment:
      - NEXT_PUBLIC_API_URL=http://localhost:8080
    depends_on:
      - api
```

**Step 2: Start Web UI**
```bash
docker-compose up -d web
```

**Step 3: Access Web UI**
```
URL: http://localhost:3000
Login: Use backend credentials
```

---

### Option 2: Manual Build (Development)

**Step 1: Clone Repository**
```bash
git clone https://github.com/nlp-core-team/ip-css.git
cd ip-css/server/web
```

**Step 2: Install Dependencies**
```bash
npm install
```

**Step 3: Configure Environment**
```bash
cp .env.example .env.local
# Edit .env.local
NEXT_PUBLIC_API_URL=http://localhost:8080
```

**Step 4: Build and Run**
```bash
# Development
npm run dev

# Production
npm run build
npm start
```

---

## 💻 Desktop UI Setup

### Windows

**Step 1: Download Installer**
```powershell
# Download from releases
Invoke-WebRequest -Uri "https://releases.ip-css.com/0.3.0-beta/IP-CSS-Setup-0.3.0-beta.exe" -OutFile "IP-CSS-Setup.exe"
```

**Step 2: Install**
```powershell
# Run installer
.\IP-CSS-Setup.exe

# Follow installation wizard
# Choose installation directory
# Create desktop shortcut (optional)
```

**Step 3: Launch**
```powershell
# Launch from desktop shortcut
# Or: Start Menu → IP-CSS

# Configure API URL: http://localhost:8080
# Login with credentials
```

---

### macOS

**Step 1: Download DMG**
```bash
# Download from releases
curl -O https://releases.ip-css.com/0.3.0-beta/IP-CSS-0.3.0-beta.dmg
```

**Step 2: Install**
```bash
# Open DMG
open IP-CSS-0.3.0-beta.dmg

# Drag to Applications folder
# Launch from Applications
```

**Step 3: First Launch**
```bash
# macOS may block unsigned app
# Go to: System Preferences → Security & Privacy
# Click "Open Anyway"

# Configure API URL: http://localhost:8080
# Login with credentials
```

---

### Linux

**Step 1: Download AppImage**
```bash
# Download from releases
wget https://releases.ip-css.com/0.3.0-beta/IP-CSS-0.3.0-beta.AppImage

# Make executable
chmod +x IP-CSS-0.3.0-beta.AppImage
```

**Step 2: Run**
```bash
# Run AppImage
./IP-CSS-0.3.0-beta.AppImage

# Or install to system
./IP-CSS-0.3.0-beta.AppImage --appimage-install
```

---

## 📱 Mobile Apps Setup

### iOS (TestFlight)

**Step 1: Install TestFlight**
```
1. Open App Store on iPhone/iPad
2. Search for "TestFlight"
3. Install TestFlight app
```

**Step 2: Accept Invitation**
```
1. Open email invitation from TestFlight
2. Tap "View in TestFlight"
3. Tap "Accept" button
```

**Step 3: Install IP-CSS**
```
1. Open TestFlight app
2. Find "IP-CSS" in available apps
3. Tap "Install"
4. Wait for installation to complete
```

**Step 4: Launch and Configure**
```
1. Open IP-CSS app
2. Configure server URL: http://<your-server-ip>:8080
3. Login with credentials
4. Enable notifications (optional)
```

---

### Android (Internal Testing)

**Option 1: Google Play Internal Testing**

**Step 1: Accept Invitation**
```
1. Open email invitation
2. Tap "Become a tester" link
3. Open Google Play Store
```

**Step 2: Install**
```
1. Search for "IP-CSS Beta"
2. Tap "Install"
3. Wait for installation
```

**Option 2: Direct APK Download**

**Step 1: Download APK**
```bash
# Download from releases
curl -O https://releases.ip-css.com/0.3.0-beta/IP-CSS-0.3.0-beta.apk
```

**Step 2: Install**
```bash
# Enable "Install from unknown sources"
# Settings → Security → Unknown sources (enable)

# Install APK
adb install IP-CSS-0.3.0-beta.apk

# Or: Open APK on device and tap install
```

**Step 3: Launch and Configure**
```
1. Open IP-CSS app
2. Configure server URL
3. Login with credentials
```

---

## 🖥️ NAS Platforms Setup

### Synology DSM

**Step 1: Download SPK**
```bash
# Download from releases
wget https://releases.ip-css.com/0.3.0-beta/IP-CSS_0.3.0_0001-beta.spk
```

**Step 2: Install via Package Center**
```
1. Open Synology DSM
2. Go to: Package Center
3. Click: Manual Install
4. Select SPK file
5. Follow installation wizard
6. Configure settings:
   - Port: 8080
   - Storage location: /volume1/ip-css
7. Click: Done
```

**Step 3: Launch**
```
1. Open Main Menu
2. Find: IP-CSS
3. Click to launch
4. Configure admin credentials
```

---

### QNAP QTS

**Step 1: Download QPKG**
```bash
# Download from releases
wget https://releases.ip-css.com/0.3.0-beta/IP-CSS_0.3.0-beta.qpkg
```

**Step 2: Install via App Center**
```
1. Open QNAP QTS
2. Go to: App Center
3. Click: Install Manually
4. Select QPKG file
5. Follow installation wizard
6. Configure settings
7. Click: Install
```

**Step 3: Launch**
```
1. Open Main Menu
2. Find: IP-CSS
3. Click to launch
4. Complete setup wizard
```

---

### Asustor ADM

**Step 1: Download APK**
```bash
# Download from releases
wget https://releases.ip-css.com/0.3.0-beta/IP-CSS_0.3.0-beta.apk
```

**Step 2: Install via App Central**
```
1. Open Asustor ADM
2. Go to: App Central
3. Click: Manual Installation
4. Select APK file
5. Follow installation wizard
6. Click: Install
```

**Step 3: Launch**
```
1. Open Apps
2. Find: IP-CSS
3. Click to launch
4. Complete setup
```

---

### TrueNAS SCALE

**Step 1: Download Docker Compose**
```bash
# Download from releases
wget https://releases.ip-css.com/0.3.0-beta/docker-compose-truenas.yml
```

**Step 2: Deploy via Apps**
```
1. Open TrueNAS SCALE UI
2. Go to: Apps
3. Click: Discover Apps
4. Search: IP-CSS (if available)
   OR
   Click: Manage Catalogs → Add Custom
5. Configure:
   - Application Name: ip-css
   - Version: 0.3.0-beta
   - Storage: /mnt/pool/ip-css
   - Network: Host mode
6. Click: Install
```

**Step 3: Verify**
```
1. Check Apps → ip-css
2. Status should be: Running
3. Click: Web Interface
4. Complete setup
```

---

## 🔧 Configuration

### Basic Configuration

**Step 1: Access Admin Panel**
```
URL: http://localhost:8080/admin
Login: admin / <your-password>
```

**Step 2: Configure System Settings**
```
1. Go to: Settings → System
2. Configure:
   - System name: IP-CSS Beta
   - Timezone: Your timezone
   - Language: English
3. Click: Save
```

**Step 3: Configure Storage**
```
1. Go to: Settings → Storage
2. Configure:
   - Recording path: /data/recordings
   - Max storage: 80% (or custom)
   - Retention: 30 days (or custom)
3. Click: Save
```

---

### Camera Configuration

**Step 1: Add Camera Manually**
```
1. Go to: Cameras → Add Camera
2. Fill in:
   - Name: Camera 1
   - RTSP URL: rtsp://<camera-ip>:554/stream
   - Username: <camera-username>
   - Password: <camera-password>
   - Brand: <select brand>
   - Model: <select model>
3. Click: Test Connection
4. Click: Save
```

**Step 2: Add Camera via ONVIF Discovery**
```
1. Go to: Cameras → Discover Cameras
2. Click: Scan Network
3. Wait for discovery (30-60 seconds)
4. Select discovered cameras
5. Enter credentials
6. Click: Add Selected
```

---

### Recording Configuration

**Step 1: Configure Recording Schedule**
```
1. Go to: Cameras → [Select Camera] → Recording
2. Enable: Recording
3. Configure schedule:
   - Mode: Continuous / Motion / Event
   - Schedule: 24/7 or custom hours
4. Click: Save
```

**Step 2: Configure Motion Detection**
```
1. Go to: Analytics → Motion Detection
2. Enable: Motion Detection
3. Configure:
   - Sensitivity: Medium
   - Regions: Full frame (or custom)
   - Min object size: 5%
4. Click: Save
```

---

## ✅ Verification Checklist

### Backend Verification

- [ ] Docker containers running (`docker-compose ps`)
- [ ] API accessible (http://localhost:8080/api/health)
- [ ] Database connected (check logs)
- [ ] WebSocket working (check browser console)
- [ ] No critical errors in logs

### Web UI Verification

- [ ] Web UI accessible (http://localhost:3000)
- [ ] Login works
- [ ] Dashboard loads
- [ ] Camera list displays
- [ ] Live view works

### Desktop UI Verification

- [ ] Application launches
- [ ] Can connect to server
- [ ] Login works
- [ ] Live view works
- [ ] No crashes

### Mobile Apps Verification

- [ ] App installs successfully
- [ ] Can connect to server
- [ ] Login works
- [ ] Live view works
- [ ] Push notifications work (if enabled)

### NAS Verification

- [ ] Package installs successfully
- [ ] Service starts automatically
- [ ] Web interface accessible
- [ ] Cameras can be added
- [ ] Recording works

---

## 🐛 Troubleshooting

### Common Issues

**Issue 1: Docker containers won't start**
```bash
# Check logs
docker-compose logs api

# Common solutions:
# 1. Check port conflicts
netstat -tulpn | grep 8080

# 2. Check disk space
df -h

# 3. Restart Docker
sudo systemctl restart docker
```

**Issue 2: Cannot access web UI**
```bash
# Check if service is running
docker-compose ps

# Check firewall
sudo ufw status
sudo ufw allow 8080/tcp
sudo ufw allow 3000/tcp

# Check network
ping localhost
curl http://localhost:8080/api/health
```

**Issue 3: Camera connection fails**
```
1. Verify camera IP and credentials
2. Check network connectivity (ping camera)
3. Verify RTSP URL format
4. Check firewall rules
5. Try ONVIF discovery instead
```

**Issue 4: Mobile app can't connect**
```
1. Ensure server is accessible from mobile network
2. Use server IP, not localhost
3. Check firewall allows external connections
4. Verify port forwarding (if remote)
```

---

## 📞 Support

**Beta Testing Support:**
- **Email:** beta-support@ip-css.com
- **Discord:** [Invite Link]
- **GitHub Issues:** https://github.com/nlp-core-team/ip-css/issues
- **Documentation:** https://docs.ip-css.com

**Emergency Contact:**
- **Critical Issues:** beta-emergency@ip-css.com
- **Response Time:** 24-48 hours

---

## 📝 Next Steps

After completing setup:

1. ✅ Verify all components are working
2. ✅ Add at least 1 camera
3. ✅ Test live view
4. ✅ Configure recording
5. ✅ Start Week 1 testing scenarios
6. ✅ Fill out weekly feedback form

---

**Guide Version:** 1.0  
**Created:** 28 January 2026  
**Last Updated:** 28 January 2026  
**Status:** 🟡 **PREPARING**  
**Owner:** DevOps Team
