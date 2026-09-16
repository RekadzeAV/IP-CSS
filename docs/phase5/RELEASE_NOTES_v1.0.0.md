# IP-CSS v1.0.0 Release Notes

**Release Date:** 28 January 2026  
**Version:** 1.0.0  
**Type:** Production (GA)  
**Support:** Long-term support (LTS) until January 2028

---

## 🎉 Welcome to IP-CSS v1.0.0

We are excited to announce the general availability of **IP-CSS v1.0.0** - the first production release of our comprehensive IP camera surveillance and video analytics system.

This release marks the transition from beta to production-ready software, with enterprise-grade features, security, and reliability.

---

## ✨ What's New

### 🏗️ Core Platform

#### Backend Services
- **RESTful API** - Complete OpenAPI 3.0 specification
- **WebSocket Support** - Real-time event streaming
- **gRPC Services** - High-performance inter-service communication
- **Event-Driven Architecture** - Kafka-based event bus

#### Database
- **PostgreSQL** - Primary data storage
- **Redis** - Caching and session management
- **Flyway Migrations** - Automated schema management
- **Connection Pooling** - HikariCP optimization

#### Security
- **JWT Authentication** - Token-based auth with refresh
- **RBAC** - 4-level role-based access control
- **BCrypt** - Secure password hashing
- **2FA Support** - TOTP-based two-factor authentication
- **HTTPS Enforcement** - TLS 1.2/1.3

---

### 📹 Video Management

#### Camera Integration
- **ONVIF Support** - Profile S, G, T
- **RTSP/RTSPS** - Secure stream transport
- **Auto-Discovery** - Network camera detection
- **Multi-Protocol** - RTSP, HTTP, HLS support

#### Stream Processing
- **Hardware Acceleration** - QuickSync, NVENC, VCE, Mali
- **Adaptive Bitrate** - Dynamic quality adjustment
- **Low Latency** - <500ms end-to-end
- **Multi-Stream** - Simultaneous recording and viewing

#### Recording
- **Continuous Recording** - 24/7 capture
- **Motion Detection** - AI-powered motion analysis
- **Event Recording** - Trigger-based capture
- **Smart Retention** - Configurable retention policies

---

### 🧠 Analytics & AI

#### Video Analytics
- **Motion Detection** - Background subtraction, optical flow
- **Object Detection** - YOLO, SSD, Faster R-CNN
- **Object Tracking** - DeepSORT, ByteTrack
- **Activity Recognition** - Temporal action localization

#### Face Recognition
- **Face Detection** - MTCNN, RetinaFace
- **Face Embedding** - FaceNet, ArcFace
- **Face Matching** - 1:N identification
- **Liveness Detection** - Anti-spoofing protection

#### ANPR (License Plate Recognition)
- **Plate Detection** - YOLO-based detection
- **Character Recognition** - CRNN, LPRNet
- **Multi-Country** - EU, US, Asia formats
- **Watchlist Alerts** - Real-time matching

#### Behavioral Analytics
- **People Counting** - Entry/exit counting
- **Crowd Detection** - Density analysis
- **Loitering Detection** - Time-based alerts
- **Fall Detection** - Pose estimation

---

### 🖥️ User Interface

#### Web Application
- **React 18** - Modern UI framework
- **TypeScript** - Type-safe development
- **Material-UI** - Consistent design system
- **Responsive Design** - Desktop, tablet, mobile

#### Features
- **Live View** - Multi-camera grid (1/4/9/16/25)
- **Playback** - Timeline-based video review
- **PTZ Control** - Camera pan/tilt/zoom
- **Event Timeline** - Filterable event browser
- **Analytics Dashboard** - Real-time metrics
- **Map View** - Camera geolocation

#### Mobile Apps
- **iOS** - SwiftUI, iOS 15+
- **Android** - Jetpack Compose, Android 10+
- **Push Notifications** - Real-time alerts
- **Offline Mode** - Cached data access

---

### 🌐 Platform Support

#### Operating Systems
- **Linux** - Ubuntu, Debian, CentOS, RHEL, Fedora
- **Windows** - 10, 11, Server 2019+
- **Raspberry Pi** - Pi 4, 400, 5 (ARM64)
- **Docker** - Multi-architecture containers

#### NAS Platforms
- **Synology DSM** - 7.0+
- **QNAP QTS** - 5.0+
- **Asustor ADM** - 4.0+
- **TrueNAS SCALE** - 22.02+

#### Hardware Encoders
- **Intel QuickSync** - 6th gen+
- **NVIDIA NVENC** - GTX 10 series+
- **AMD VCE** - RX 400 series+
- **ARM Mali** - G52+

---

## 📦 Installation

### Quick Start

#### Docker (Recommended)
```bash
docker-compose up -d
```

#### Ubuntu/Debian
```bash
wget https://releases.ip-css.com/ip-css_1.0.0_all.deb
sudo dpkg -i ip-css_1.0.0_all.deb
sudo systemctl start ip-css
```

#### CentOS/RHEL/Fedora
```bash
sudo dnf install ip-css-1.0.0.rpm
sudo systemctl start ip-css
```

#### Windows
```powershell
# Download and run installer
IP-CSS-Setup-1.0.0.exe
```

#### Raspberry Pi
```bash
wget https://releases.ip-css.com/ip-css-1.0.0-linux-arm64.tar.gz
tar -xzf ip-css-1.0.0-linux-arm64.tar.gz
cd ip-css-1.0.0
sudo ./install.sh
```

### System Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **CPU** | 4 cores | 8+ cores |
| **RAM** | 4 GB | 8+ GB |
| **Storage** | 50 GB | 500+ GB (SSD) |
| **Network** | 1 Gbps | 10 Gbps |
| **GPU** | Optional | NVIDIA GTX 1650+ |

---

## 🔧 Configuration

### Basic Configuration

```yaml
# /etc/ip-css/application.yml

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

security:
  jwt_expiry_minutes: 15
  require_2fa: false
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `IP_CSS_PORT` | HTTP port | 8080 |
| `IP_CSS_DB_URL` | Database URL | jdbc:postgresql://localhost:5432/ipcss |
| `IP_CSS_DB_USER` | Database user | ipcss |
| `IP_CSS_DB_PASSWORD` | Database password | changeme |
| `IP_CSS_STORAGE_PATH` | Recording storage | /var/lib/ip-css/recordings |
| `IP_CSS_JWT_SECRET` | JWT signing key | (auto-generated) |

---

## 🆙 Upgrade Guide

### From v0.3.0-beta

```bash
# Stop current version
sudo systemctl stop ip-css

# Backup data
sudo cp -r /var/lib/ip-css /var/lib/ip-css.backup

# Install new version
# DEB
sudo dpkg -i ip-css_1.0.0_all.deb

# RPM
sudo dnf install ip-css-1.0.0.rpm

# Start new version
sudo systemctl start ip-css

# Verify
sudo systemctl status ip-css
```

### Database Migration

Database migrations run automatically on startup. Manual migration:

```bash
curl -X POST http://localhost:8080/api/admin/migrate
```

---

## 🐛 Bug Fixes

### Since v0.3.0-beta

| ID | Issue | Severity | Status |
|----|-------|----------|--------|
| BUG-001 | Memory leak in stream processor | High | ✅ Fixed |
| BUG-002 | JWT token not expiring | High | ✅ Fixed |
| BUG-003 | ONVIF discovery timeout | Medium | ✅ Fixed |
| BUG-004 | Playback stuttering | Medium | ✅ Fixed |
| BUG-005 | Mobile app crash on rotation | Low | ✅ Fixed |
| BUG-006 | Incorrect timestamp in logs | Low | ✅ Fixed |
| BUG-007 | CSS styling issues in Safari | Low | ✅ Fixed |
| BUG-008 | WebSocket reconnection delay | Low | ✅ Fixed |

---

## ⚠️ Known Issues

### High Priority

| ID | Issue | Workaround | Planned Fix |
|----|-------|------------|-------------|
| None | No critical issues | N/A | N/A |

### Medium Priority

| ID | Issue | Workaround | Planned Fix |
|----|-------|------------|-------------|
| REL-002 | RTSPS requires camera support | Use RTSP with authentication | v1.1.0 |
| REL-003 | No OIDC in v1.0.0 | Use basic OAuth2 | v1.1.0 |

### Low Priority

| ID | Issue | Workaround | Planned Fix |
|----|-------|------------|-------------|
| REL-004 | Limited macOS support | Use Docker on macOS | Community |
| REL-005 | No bug bounty program | Report via security@ip-css.com | Q3 2026 |

---

## 🔒 Security

### Security Improvements

- ✅ BCrypt password hashing (cost factor 12)
- ✅ JWT with short expiry (15 minutes)
- ✅ Rate limiting on all endpoints
- ✅ Security headers (HSTS, CSP, X-Frame-Options)
- ✅ Input validation and sanitization
- ✅ SQL injection prevention
- ✅ XSS protection

### Security Audit

- **Overall Score:** 92/100 ✅ PASS
- **Critical Issues:** 0
- **High Issues:** 0 (2 resolved)
- **Medium Issues:** 2 (accepted risks)

### Reporting Vulnerabilities

Email: security@ip-css.com  
Response Time: 48 hours  
Bug Bounty: Planned Q3 2026

---

## 📊 Performance

### Benchmarks

| Metric | Value | Conditions |
|--------|-------|------------|
| **API Response Time** | <100ms | p95, 100 concurrent users |
| **Stream Latency** | <500ms | End-to-end |
| **Recording Write Speed** | 500 MB/s | SSD storage |
| **Face Recognition** | <200ms | Per face, GPU |
| **ANPR** | <150ms | Per plate, GPU |
| **Max Cameras (1080p)** | 32 | With GPU |
| **Max Cameras (4K)** | 8 | With GPU |

### Resource Usage

| Component | Idle | Typical | Peak |
|-----------|------|---------|------|
| **Memory** | 512 MB | 1-2 GB | 4 GB |
| **CPU** | 5% | 20-40% | 80% |
| **Disk I/O** | Low | Medium | High |
| **Network** | Low | Medium | High |

---

## 🆘 Support

### Getting Help

- **Documentation:** https://docs.ip-css.com
- **Issue Tracker:** https://github.com/nlp-core-team/ip-css/issues
- **Community Forum:** https://forum.ip-css.com
- **Email Support:** support@ip-css.com

### Support Levels

| Level | Response Time | Channels |
|-------|---------------|----------|
| **Community** | Best effort | Forum, GitHub Issues |
| **Standard** | 48 hours | Email, Documentation |
| **Enterprise** | 4 hours | Email, Phone, Slack |

---

## 📅 Release Schedule

### Upcoming Releases

| Version | Date | Type | Highlights |
|---------|------|------|------------|
| **v1.0.1** | Feb 2026 | Patch | Bug fixes, security updates |
| **v1.0.2** | Mar 2026 | Patch | Performance improvements |
| **v1.1.0** | Apr 2026 | Minor | OIDC, enhanced analytics |
| **v1.2.0** | Jun 2026 | Minor | Cloud integration |
| **v2.0.0** | Jan 2027 | Major | Microservices architecture |

### End of Life

- **v1.0.0 EOL:** January 2028
- **Security Updates:** Until January 2029

---

## 👥 Contributors

### Core Team

- **NLP-Core-Team** - Development
- **Security Team** - Security audit
- **QA Team** - Testing
- **Documentation Team** - User guides

### Community Contributors

Thank you to all community members who contributed through bug reports, feature requests, and feedback.

---

## 📄 License

**IP-CSS v1.0.0** is released under the MIT License.

```
Copyright (c) 2026 NLP-Core-Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🎉 Thank You

Thank you for choosing IP-CSS! We are committed to providing the best IP camera surveillance and video analytics platform.

**Happy Monitoring!** 🎥🔒

---

**IP-CSS Team**  
**NLP-Core-Team**  
**January 2026**
