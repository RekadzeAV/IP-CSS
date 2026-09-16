# Operator User Guide

## Overview

This guide provides step-by-step instructions for operating the IP-CSS camera management system.

**Target Audience:** System operators, security personnel, administrators  
**Version:** 1.0  
**Last Updated:** 2026-05-17

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Authentication](#authentication)
3. [Camera Management](#camera-management)
4. [Recording Management](#recording-management)
5. [Event Monitoring](#event-monitoring)
6. [Settings Configuration](#settings-configuration)
7. [Troubleshooting](#troubleshooting)

---

## Getting Started

### System Requirements

- **Operating System:** Windows 10+, macOS 10.15+, Linux (Ubuntu 20.04+), Android 7.0+
- **RAM:** 4GB minimum, 8GB recommended
- **Storage:** 10GB free space for recordings
- **Network:** Stable internet connection (10 Mbps+)

### First Launch

1. **Install the application**
   - Download installer from official source
   - Run installer and follow prompts
   - Launch application

2. **Initial setup**
   - Application will detect cameras on your network
   - You can add cameras manually or auto-discover

---

## Authentication

### Login

1. **Open the application**
2. **Enter credentials:**
   - Username: Your assigned username
   - Password: Your password
3. **Click "Login"**

**Success:** You'll be redirected to the main dashboard  
**Failure:** Error message will explain the issue

### Account Security

- **Password Requirements:**
  - Minimum 8 characters
  - At least 1 uppercase letter
  - At least 1 number
  - At least 1 special character

- **Failed Attempts:**
  - 5 failed attempts = 30-minute lockout
  - Contact administrator to unlock

### Logout

- Click profile icon → "Logout"
- Or close the application (auto-logout after 30 minutes of inactivity)

---

## Camera Management

### Adding a Camera

#### Method 1: Auto-Discovery

1. Go to **Cameras** → **Add Camera**
2. Click **"Auto-discover"**
3. Wait for scan to complete (30-60 seconds)
4. Select camera from list
5. Enter camera credentials (username/password)
6. Click **"Add"**

#### Method 2: Manual Addition

1. Go to **Cameras** → **Add Camera**
2. Click **"Manual Add"**
3. Fill in camera details:
   - **Name:** Friendly name (e.g., "Front Door")
   - **RTSP URL:** Camera stream URL (e.g., `rtsp://192.168.1.100:554/stream`)
   - **Username:** Camera admin username
   - **Password:** Camera admin password
   - **Resolution:** 1920x1080, 1280x720, etc.
   - **FPS:** 30, 24, 15, etc.
4. Click **"Test Connection"**
5. If successful, click **"Add"**

### Testing Camera Connection

1. Select camera from list
2. Click **"Test"** button
3. Wait for test results:
   - ✅ **Success:** Stream URL and resolution displayed
   - ❌ **Failure:** Error message shown

### Viewing Camera Feed

1. Go to **Cameras**
2. Select camera from list
3. Live feed will start automatically
4. Use controls:
   - **Play/Pause:** Toggle live stream
   - **Fullscreen:** Expand to full screen
   - **Snapshot:** Take screenshot
   - **PTZ Controls:** Pan/Tilt/Zoom (if supported)

### PTZ Control

1. Select PTZ-enabled camera
2. Open **PTZ Controls** panel
3. Use controls:
   - **Direction buttons:** Pan/Tilt
   - **Zoom +/-:** Zoom in/out
   - **Speed slider:** Control movement speed
   - **Presets:** Jump to preset positions

### Editing Camera

1. Select camera
2. Click **"Edit"** button
3. Modify settings
4. Click **"Save"**

### Deleting Camera

1. Select camera
2. Click **"Delete"** button
3. Confirm deletion
4. Camera and all recordings will be removed

---

## Recording Management

### Starting Recording

#### Manual Recording

1. Select camera
2. Click **"Record"** button (red circle)
3. Recording indicator will appear
4. Click again to stop

#### Scheduled Recording

1. Go to **Settings** → **Recording**
2. Enable **"Scheduled Recording"**
3. Set schedule:
   - **Days:** Select days of week
   - **Time Range:** Start and end times
4. Click **"Save"**

### Viewing Recordings

1. Go to **Recordings** tab
2. Select camera from filter
3. Browse recordings by date/time
4. Click recording to play

### Recording Controls

- **Play:** Play recording
- **Pause:** Pause playback
- **Seek:** Drag timeline to position
- **Speed:** 0.5x, 1x, 2x, 4x
- **Snapshot:** Take screenshot from recording

### Deleting Recordings

1. Select recording(s)
2. Click **"Delete"** button
3. Confirm deletion
4. **Warning:** This action is irreversible

### Exporting Recordings

1. Select recording
2. Click **"Export"** button
3. Choose format:
   - **MP4:** Universal compatibility
   - **MKV:** High quality
4. Choose destination folder
5. Click **"Export"**
6. Wait for export to complete

---

## Event Monitoring

### Event Types

- **Motion Detection:** Movement detected in camera view
- **Face Detection:** Human face detected
- **Object Detection:** Specific objects detected (person, vehicle, animal)
- **License Plate:** Vehicle license plate recognized

### Viewing Events

1. Go to **Events** tab
2. Filter by:
   - **Camera:** Select specific camera
   - **Type:** Motion, Face, Object, License Plate
   - **Date Range:** Custom date range
   - **Status:** Acknowledged/Unacknowledged
3. Browse event list

### Event Details

Click on event to view:
- **Timestamp:** When event occurred
- **Camera:** Which camera detected it
- **Type:** Event type
- **Snapshot:** Image from event
- **Details:** Additional information

### Acknowledging Events

1. Select event(s)
2. Click **"Acknowledge"** button
3. Optionally add comment
4. Event marked as acknowledged

### Motion Detection Zones

1. Go to **Settings** → **Motion Detection**
2. Select camera
3. Draw detection zones on camera view
4. Set sensitivity (1-10)
5. Click **"Save"**

### Face Recognition

1. Enroll faces:
   - Go to **Faces** → **Add Face**
   - Upload face photo
   - Enter person name
   - Click **"Enroll"**
2. System will detect and match faces in real-time

### License Plate Recognition

1. Configure camera region:
   - Go to **Settings** → **License Plates**
   - Select country/region
   - Set recognition parameters
2. System will automatically detect and log plates

---

## Settings Configuration

### General Settings

- **Application Language:** Choose interface language
- **Theme:** Light/Dark mode
- **Startup:** Auto-start on system boot
- **Notifications:** Enable/disable system notifications

### Video Settings

- **Quality:**
  - Low (480p) — Saves bandwidth
  - Medium (720p) — Balanced
  - High (1080p) — Best quality
  - Ultra (4K) — Maximum quality (if supported)

- **FPS:**
  - 15 FPS — Low bandwidth
  - 24 FPS — Standard
  - 30 FPS — Smooth motion
  - 60 FPS — Ultra smooth (if supported)

- **Bitrate:**
  - Auto — Automatic adjustment
  - 2000 kbps — Low
  - 4000 kbps — Medium
  - 8000 kbps — High

### Network Settings

- **Server URL:** API server address
- **Port:** Server port (default: 8080)
- **Proxy:** Configure proxy settings
- **SSL/TLS:** Enable secure connections

### Storage Settings

- **Recording Location:** Choose storage folder
- **Max Storage:** Limit recording size
- **Retention:** How long to keep recordings
- **Cleanup:** Auto-delete old recordings

### User Management

1. Go to **Settings** → **Users**
2. **Add User:**
   - Click **"Add User"**
   - Enter username, email, password
   - Assign role (Admin, Operator, Viewer)
   - Set permissions
   - Click **"Save"**
3. **Edit User:**
   - Select user
   - Click **"Edit"**
   - Modify settings
   - Click **"Save"**
4. **Delete User:**
   - Select user
   - Click **"Delete"**
   - Confirm deletion

### Roles and Permissions

| Role | Permissions |
|------|-------------|
| **Admin** | Full access to all features |
| **Operator** | Can view cameras, manage recordings |
| **Viewer** | View-only access |

---

## Troubleshooting

### Common Issues

#### Camera Connection Failed

**Symptoms:**
- Error: "Cannot connect to camera"
- Black screen in live feed

**Solutions:**
1. Check camera is powered on
2. Verify network connectivity
3. Check RTSP URL is correct
4. Verify username/password
5. Ensure camera firmware is up-to-date

#### Recording Not Starting

**Symptoms:**
- Record button does nothing
- No recording indicator

**Solutions:**
1. Check storage space (minimum 10% free)
2. Verify recording permissions
3. Check camera supports recording
4. Restart application

#### Motion Detection Not Working

**Symptoms:**
- No motion events triggered
- False positives

**Solutions:**
1. Check motion detection is enabled
2. Adjust sensitivity settings
3. Verify detection zones are drawn
4. Clean camera lens
5. Check lighting conditions

#### Slow Performance

**Symptoms:**
- Laggy video playback
- Application freezing

**Solutions:**
1. Reduce video quality
2. Close other applications
3. Increase RAM allocation
4. Check network bandwidth
5. Restart application

#### Login Failed

**Symptoms:**
- Error: "Invalid credentials"
- Account locked

**Solutions:**
1. Verify username/password
2. Check Caps Lock is off
3. Reset password if needed
4. Wait for lockout period (30 min)
5. Contact administrator

### Getting Help

**Documentation:**
- Read this user guide
- Check online documentation
- Review FAQ section

**Support:**
- Email: support@ipcamera.com
- Phone: +1-800-IP-CAM-SUPPORT
- Hours: Mon-Fri 9AM-6PM EST

**System Logs:**
1. Go to **Settings** → **About**
2. Click **"Export Logs"**
3. Send logs to support team

---

## Appendix

### Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+N` | Add new camera |
| `Ctrl+R` | Start/stop recording |
| `Ctrl+E` | Export recording |
| `Ctrl+F` | Search events |
| `Ctrl+L` | Toggle fullscreen |
| `Esc` | Cancel/Close |

### Camera Compatibility

**Supported Brands:**
- Hikvision
- Dahua
- Axis
- Bosch
- Hanwha
- Uniview
- Generic RTSP cameras

**Protocols:**
- RTSP (Real-Time Streaming Protocol)
- ONVIF Profile S/G
- HTTP/HTTPS

### Security Features

- **Encryption:** AES-256 for stored data
- **Password Hashing:** bcrypt/PBKDF2
- **Certificate Pinning:** HTTPS connections
- **Brute Force Protection:** Account lockout
- **Audit Logging:** All actions logged

---

**End of User Guide**

For technical documentation, see:
- [Security Module API](../api/SECURITY_MODULE_API.md)
- [UI Bridge API](../api/UI_BRIDGE_API.md)
- [Developer Guide](DEVELOPER_GUIDE.md)
