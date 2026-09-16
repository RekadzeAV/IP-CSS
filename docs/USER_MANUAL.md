# IP-CSS User Manual

**Version:** 0.3.0-beta
**Date:** 19.07.2026
**Product:** IP Camera Surveillance System

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Installation](#2-installation)
3. [Quick Start](#3-quick-start)
4. [Configuration](#4-configuration)
5. [Camera Management](#5-camera-management)
6. [Viewing Feeds](#6-viewing-feeds)
7. [Recording & Playback](#7-recording--playback)
8. [Events & Alerts](#8-events--alerts)
9. [Analytics](#9-analytics)
10. [Cloud & Sync](#10-cloud--sync)
11. [Cluster Setup](#11-cluster-setup)
12. [Security](#12-security)
13. [Troubleshooting](#13-troubleshooting)
14. [FAQ](#14-faq)

---

## 1. Introduction

IP-CSS is an open-source IP Camera Surveillance System designed for:
- Home and small business security
- Enterprise-grade multi-site deployments
- NAS appliances (Synology, QNAP, Asustor, TrueNAS)
- Cloud-connected surveillance networks

### Key Features

| Feature | Description |
|---------|-------------|
| **Multi-protocol camera support** | ONVIF Profile S/T, RTSP, HTTP(S) |
| **HLS streaming** | Low-latency video via HLS |
| **Motion detection** | AI-powered motion detection |
| **Object detection** | YOLO-based person/vehicle/object detection |
| **Face recognition** | FaceNet/ArcFace integration |
| **ANPR** | License plate recognition |
| **Cloud sync** | S3-compatible (AWS, MinIO, Backblaze, GCS) |
| **Cluster mode** | Redis Cluster, NGINX LB, multi-master replication |
| **NAS support** | Docker, Synology SPK, QNAP QPKG, Asustor APK |
| **Mobile apps** | iOS (SwiftUI) and Android (Jetpack Compose) |

---

## 2. Installation

### 2.1 Docker (Recommended)

```bash
# Pull and run
$ docker run -d --name ipcss \
    -p 8080:8080 \
    -p 8443:8443 \
    -v /path/to/config:/app/config \
    -v /path/to/data:/app/data \
    ghcr.io/rekadzeav/ip-css:latest
```

### 2.2 Docker Compose

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ipcss
      POSTGRES_USER: ipcss
      POSTGRES_PASSWORD: changeme
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data

  ipcss-server:
    image: ghcr.io/rekadzeav/ip-css:latest
    ports:
      - "8080:8080"
      - "8443:8443"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/ipcss
      DATABASE_USER: ipcss
      DATABASE_PASSWORD: changeme
      REDIS_HOST: redis
    volumes:
      - ./config:/app/config
      - ./data:/app/data
      - ./recordings:/app/recordings
    depends_on:
      - postgres
      - redis

volumes:
  postgres_data:
  redis_data:
```

### 2.3 NAS Installation

**Synology:**
1. Download `ipcss-*.spk` from Releases
2. Open Package Center → Manual Install
3. Select the SPK file and install
4. Access via `http://nas-ip:8080`

**QNAP:**
1. Download `ipcss-*.qpkg` from Releases
2. Open App Center → Install Manually
3. Select the QPKG file and install
4. Access via `http://qnap-ip:8080`

**TrueNAS SCALE:**
1. Go to Apps → Launch Docker Image
2. Set image to `ghcr.io/rekadzeav/ip-css:latest`
3. Configure volumes and ports
4. Launch the app

### 2.4 From Source

```bash
$ git clone https://github.com/RekadzeAV/IP-CSS.git
$ cd IP-CSS
$ ./gradlew :server:api:shadowJar
$ java -jar server/api/build/libs/server-api-*.jar
```

---

## 3. Quick Start

### 3.1 First Run

1. Open `http://localhost:8080`
2. Create admin account
3. Go to **Settings** → **Server** and configure:
   - Recording directory
   - Retention period
   - Notification channels

### 3.2 Add First Camera

1. Click **Add Camera** (+)
2. Enter camera details:
   - **Name:** Front Door
   - **URL:** `rtsp://192.168.1.100:554/stream1`
   - **Username:** admin
   - **Password:** ****
3. Click **Test Connection**
4. Click **Save**

### 3.3 Auto-Discovery

For ONVIF cameras on the same subnet:
1. Go to **Cameras** → **Discover**
2. Wait 5-10 seconds for WS-Discovery to scan
3. Select cameras to add
4. Enter credentials if needed

---

## 4. Configuration

### 4.1 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/ipcss` | PostgreSQL connection |
| `DATABASE_USER` | `ipcss` | Database user |
| `DATABASE_PASSWORD` | — | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | — | Redis password |
| `API_GLOBAL_RATE_LIMIT` | `300` | Requests per minute |
| `CLUSTER_ENABLED` | `false` | Enable cluster mode |
| `CLUSTER_COMPRESSION_ENABLED` | `false` | Enable GZIP compression |
| `LB_TYPE` | `none` | Load balancer type |
| `S3_ENABLED` | `false` | Enable cloud sync |
| `S3_ENDPOINT` | — | S3 endpoint URL |
| `S3_REGION` | `us-east-1` | S3 region |
| `S3_BUCKET` | — | S3 bucket name |
| `SAML_ENABLED` | `false` | Enable SAML SSO |
| `ATP_ENABLED` | `false` | Enable threat protection |
| `AI_ENABLED` | `false` | Enable AI analytics |

### 4.2 Configuration Files

| File | Description |
|------|-------------|
| `config/cameras.json` | Camera definitions |
| `config/postgresql.env` | Database credentials |
| `config/nginx/nginx-load-balancer.conf` | NGINX LB config |
| `config/mediamtx/mediamtx.yml` | MediaMTX streaming |

---

## 5. Camera Management

### 5.1 Supported Protocols

| Protocol | Port | Description |
|----------|------|-------------|
| **RTSP** | 554 | Real-Time Streaming Protocol |
| **ONVIF** | 80/443 | SOAP-based camera control |
| **HTTP(S)** | 80/443 | MJPEG snapshots |
| **WS-Discovery** | 3702/1900 | Automatic camera discovery |

### 5.2 PTZ Control

Supported PTZ commands:
- UP / DOWN / LEFT / RIGHT
- ZOOM_IN / ZOOM_OUT
- Preset positions
- Absolute/Relative positioning

### 5.3 Camera Groups

Organize cameras into groups for:
- Bulk operations
- Custom layouts
- Shared access

---

## 6. Viewing Feeds

### 6.1 Live View

Access live feeds via:
- **Web UI:** `http://server:8080`
- **HLS:** `http://server:8080/api/v1/stream/hls/{cameraId}/playlist.m3u8`
- **Snapshot:** `http://server:8080/api/v1/cameras/{id}/snapshot`

### 6.2 Multi-Camera Layout

Supported layouts:
- 1×1 (single camera)
- 2×2 (quad view)
- 3×3 (nine cameras)
- Custom grid

### 6.3 WebRTC (Low Latency)

For sub-second latency:
1. Enable WebRTC in Settings
2. Use WebRTC-compatible client
3. Connect via `ws://server:8080/api/v1/ws`

---

## 7. Recording & Playback

### 7.1 Recording Modes

| Mode | Description |
|------|-------------|
| **Continuous** | Always recording |
| **Motion-triggered** | Record when motion detected |
| **Schedule-based** | Record on a timer |
| **Event-triggered** | Record on specific events |

### 7.2 Playback Controls

- Timeline view
- Seek by timestamp
- Speed control (0.5×, 1×, 2×, 4×, 8×, 16×)
- Export to MP4/MKV/AVI
- Download recordings

### 7.3 Storage Management

- Configurable retention (1-365 days)
- Auto-delete old recordings
- Cloud upload to S3
- Storage quota alerts

---

## 8. Events & Alerts

### 8.1 Event Types

| Event | Description |
|-------|-------------|
| `motion` | Motion detected |
| `object_detected` | Person/vehicle/animal detected |
| `face_detected` | Known face recognized |
| `anpr` | License plate read |
| `camera_offline` | Camera disconnected |
| `recording_started` | Recording began |
| `camera_online` | Camera reconnected |

### 8.2 Notification Channels

- **Push notifications** (iOS/Android)
- **Email** (SMTP)
- **Telegram** (Bot API)
- **Webhook** (Custom URL)

### 8.3 Alert Rules

Create custom rules:
```json
{
    "name": "Backdoor Alert",
    "camera": "backdoor-camera",
    "events": ["motion"],
    "time_range": {
        "from": "22:00",
        "to": "06:00"
    },
    "actions": ["push", "email", "record"]
}
```

---

## 9. Analytics

### 9.1 Motion Detection

- Sensitivity adjustment (1-100)
- Region selection (mask out areas)
- Object size filter
- Time schedule

### 9.2 Object Detection (YOLO)

Supported objects:
- Person
- Vehicle (car, bus, truck, motorcycle)
- Animal (dog, cat, bird)
- Package

### 9.3 Face Recognition

- Enroll faces via snapshots
- Known/unknown person detection
- GDPR-compliant data storage
- Match confidence threshold

### 9.4 ANPR (License Plate Recognition)

- Multi-country support
- Blacklist/whitelist
- Export to CSV
- Real-time alerts

---

## 10. Cloud & Sync

### 10.1 S3 Cloud Storage

Supported providers:
- **AWS S3** — Amazon Web Services
- **MinIO** — Self-hosted S3-compatible
- **Backblaze B2** — S3-compatible API
- **Google Cloud Storage** — GCS S3 API

Configuration:
```env
S3_ENABLED=true
S3_ENDPOINT=https://s3.amazonaws.com
S3_REGION=us-east-1
S3_BUCKET=ipcss-recordings
S3_ACCESS_KEY=your-access-key
S3_SECRET_KEY=your-secret-key
S3_AUTO_CREATE_BUCKET=true
```

### 10.2 Multi-Node Sync

For multi-server deployments:
1. Enable cluster mode (`CLUSTER_ENABLED=true`)
2. Configure Redis for metadata sync
3. Configure NGINX LB for load balancing
4. Nodes auto-discover each other via heartbeat

---

## 11. Cluster Setup

### 11.1 Architecture

```
         ┌─────────────────────────────┐
         │     NGINX Load Balancer     │
         │  (SSL, Session Affinity)    │
         └────────────┬────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
   ┌────▼───┐   ┌────▼───┐   ┌────▼───┐
   │ Node 1 │◄─►│ Node 2 │◄─►│ Node 3 │
   └────┬───┘   └────┬───┘   └────┬───┘
        │             │             │
   ┌────▼────────────▼────────────▼───┐
   │         Redis Cluster           │
   └──────────────────────────────────┘
   ┌──────────────────────────────────┐
   │      PostgreSQL (Primary/Replica)│
   └──────────────────────────────────┘
```

### 11.2 Quick Cluster Setup

```bash
# Start with docker-compose
$ docker-compose -f docker-compose.yml -f docker-compose.cluster.yml up -d

# Scale nodes
$ docker-compose up -d --scale ipcss-server=3
```

---

## 12. Security

### 12.1 Authentication Methods

| Method | Description |
|--------|-------------|
| **Local** | Username/password with bcrypt |
| **JWT** | Token-based authentication |
| **OAuth2/OIDC** | Google, GitHub, Azure AD |
| **SAML 2.0** | Enterprise SSO (Okta, ADFS) |
| **LDAP** | Active Directory integration |
| **Kerberos** | Windows domain auth |

### 12.2 Security Recommendations

1. **Always use HTTPS** (TLS 1.2+)
2. Enable rate limiting
3. Use strong passwords (12+ chars)
4. Enable 2FA for admin accounts
5. Regular security audits
6. Keep system updated

### 12.3 Threat Protection

- Brute-force detection
- IP auto-blocking
- Rate limiting (configurable)
- Anomaly detection
- Security audit log

---

## 13. Troubleshooting

### 13.1 Common Issues

| Issue | Solution |
|-------|----------|
| Camera shows offline | Check RTSP URL, credentials, firewall |
| No video feed | Verify codec support (H.264) |
| High CPU usage | Disable AI analytics, reduce resolution |
| Storage full | Increase retention limit, add S3 cloud |
| WS-Discovery empty | Enable multicast on router (239.255.255.250) |
| Digest auth fails | Verify username/password, check ONVIF profile |

### 13.2 Logs

```bash
# Docker logs
$ docker logs ipcss-server

# System logs
$ tail -f /var/log/ipcss/server.log

# API logs
$ tail -f /var/log/nginx/ipcss-access.log
```

### 13.3 Diagnostic Commands

```bash
# Health check
$ curl http://localhost:8080/api/v1/health

# Cluster status
$ curl http://localhost:8080/api/v1/cluster/health

# Camera test
$ curl -X POST http://localhost:8080/api/v1/cameras/{id}/test

# Redis check
$ redis-cli ping

# PostgreSQL check
$ psql -U ipcss -d ipcss -c "SELECT 1"
```

---

## 14. FAQ

**Q: How many cameras can I add?**
A: Unlimited. Performance depends on hardware. Typically 16-32 cameras on a mid-range NAS.

**Q: What is the video latency?**
A: HLS: 2-5 seconds. WebRTC: <1 second. RTSP (direct): ~200ms.

**Q: Can I access from outside my network?**
A: Yes, via HTTPS port forwarding or VPN. Configure firewall rules accordingly.

**Q: How much storage do I need?**
A: ~1-5 GB per camera per day (depends on resolution, fps, compression).

**Q: Does it support H.265/HEVC?**
A: Yes, via FFmpeg transcoding. Direct H.265 passthrough supported.

**Q: Can I migrate from another NVR?**
A: Yes, via ONVIF migration tools. Contact support for custom migration.

**Q: Is there a mobile app?**
A: Yes, iOS (SwiftUI) and Android (Jetpack Compose) apps available.

**Q: How often are updates released?**
A: Security patches: within 48 hours. Feature releases: quarterly.

---

## Support

- **GitHub Issues:** https://github.com/RekadzeAV/IP-CSS/issues
- **Documentation:** https://github.com/RekadzeAV/IP-CSS/blob/main/docs/
- **API Reference:** http://localhost:8080/api/v1/docs (when server running)

---

*IP-CSS v0.3.0-beta — Open Source IP Camera Surveillance System*
