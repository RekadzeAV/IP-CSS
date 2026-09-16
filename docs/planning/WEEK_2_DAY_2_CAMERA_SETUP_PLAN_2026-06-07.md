# 🎥 IP-CSS Week 2 Day 2 - Camera Setup Plan

**Date:** June 7, 2026 (TBD)  
**Focus:** Test Environment Preparation  
**Target Progress:** 72% → 75%

---

## 📋 Objectives

1. Acquire test cameras (or simulate)
2. Setup test network environment
3. Configure camera RTSP streams
4. Verify connectivity
5. Prepare test scripts

---

## 🎥 Camera Requirements

### Required Cameras (3 models)

**1. Hikvision DS-2CD2342WD-I**
- Resolution: 2688x1520 (4MP)
- Codec: H.264/H.265
- Audio: G.711 PCMU
- RTSP: `rtsp://admin:password@192.168.1.101:554/Streaming/Channels/101`

**2. Dahua IPC-HFW2431S**
- Resolution: 2688x1520 (4MP)
- Codec: H.264/H.265
- Audio: G.711 PCMA
- RTSP: `rtsp://admin:password@192.168.1.102:554/cam/realmonitor?channel=1&substream=0`

**3. Axis M1065-L**
- Resolution: 1920x1080
- Codec: H.264/MJPEG
- Audio: None
- RTSP: `rtsp://admin:password@192.168.1.103:554/axis-media/media.amp?videocodec=h264`

---

## 🔧 Setup Options

### Option A: Physical Cameras (Recommended)

**Requirements:**
- 3 IP cameras (Hikvision, Dahua, Axis)
- Gigabit network switch
- Power over Ethernet (PoE) injector or switch
- Router with static IP assignment

**Setup Time:** 2-4 hours

**Steps:**
1. Unbox and configure cameras
2. Assign static IP addresses
3. Configure RTSP streams
4. Set authentication credentials
5. Verify connectivity with ffprobe
6. Run basic tests

---

### Option B: Virtual Cameras (Fallback)

**Use FFmpeg to simulate RTSP streams:**

**Requirements:**
- Sample video files (H.264, H.265, MJPEG)
- FFmpeg 8.0+
- Network bridge or localhost

**Setup Time:** 30-60 minutes

**Steps:**
1. Prepare test video files
2. Create RTSP server with FFmpeg
3. Configure virtual camera URLs
4. Verify streams accessible
5. Run tests against virtual cameras

---

### Option C: Public RTSP Servers (Alternative)

**Use publicly available test cameras:**

**Examples:**
- `rtsp://wowzaec2demo.streamlock.net:1935/vod:1000/mp4:sample.mp4`
- `rtsp://media-1-us-east-1-1.cloud.acuitybrands.com/live/h264`

**Pros:**
- No hardware required
- Immediate setup

**Cons:**
- Network latency
- Unreliable availability
- Limited codec support

---

## 📝 Setup Checklist

### Hardware Setup
- [ ] Cameras acquired (physical or virtual)
- [ ] Network equipment ready (switch, cables)
- [ ] Power supply configured
- [ ] Network connectivity verified

### Network Configuration
- [ ] Static IP addresses assigned
- [ ] Subnet mask configured (255.255.255.0)
- [ ] Gateway configured
- [ ] Firewall rules updated (RTSP port 554)

### Camera Configuration
- [ ] RTSP streams enabled
- [ ] Authentication configured
- [ ] Resolution set to expected values
- [ ] Frame rate configured
- [ ] Audio enabled (if applicable)

### Software Setup
- [ ] FFmpeg installed (8.0+)
- [ ] ffprobe available
- [ ] Test scripts ready
- [ ] Monitoring tools configured

---

## 🔍 Verification Steps

### 1. Network Connectivity

```bash
# Ping each camera
ping 192.168.1.101  # Hikvision
ping 192.168.1.102  # Dahua
ping 192.168.1.103  # Axis
```

**Expected:** All cameras respond to ping

---

### 2. RTSP Stream Verification

```bash
# Test Hikvision stream
ffprobe -rtsp_transport tcp -i "rtsp://admin:password@192.168.1.101:554/Streaming/Channels/101"

# Test Dahua stream
ffprobe -rtsp_transport tcp -i "rtsp://admin:password@192.168.1.102:554/cam/realmonitor?channel=1&substream=0"

# Test Axis stream
ffprobe -rtsp_transport tcp -i "rtsp://admin:password@192.168.1.103:554/axis-media/media.amp?videocodec=h264"
```

**Expected Output:**
```
Input #0, rtsp, from 'rtsp://...':
  Duration: N/A, start: 0.000000, bitrate: N/A
  Stream #0:0: Video: h264 (High), yuv420p(progressive), 2688x1520
  Stream #0:1: Audio: aac, 48000 Hz, stereo
```

---

### 3. Stream Quality Check

```bash
# Stream for 10 seconds and analyze
ffprobe -rtsp_transport tcp -i "rtsp://admin:password@192.168.1.101:554/Streaming/Channels/101" \
  -v error -show_entries stream=codec_name,width,height,r_frame_rate,duration \
  -of json
```

**Expected:**
- Correct codec (h264/h265)
- Expected resolution
- Valid frame rate (20-30 FPS)

---

## 📊 Test Environment Configurations

### Configuration 1: Physical Network

```
Network: 192.168.1.0/24

Device          IP Address        Purpose
-----------     ------------      -----------
Hikvision       192.168.1.101     Main camera
Dahua           192.168.1.102     Multi-codec camera
Axis            192.168.1.103     MJPEG support
Test PC         192.168.1.100     Running tests
Switch          192.168.1.1       Network hub
```

---

### Configuration 2: Virtual Network (Docker)

```dockerfile
# docker-compose.yml for virtual cameras
version: '3.8'
services:
  rtsp-server:
    image: aler9/rtsp-simple-server
    ports:
      - "8554:8554"
      - "8000:8000"
  
  test-client:
    build: .
    depends_on:
      - rtsp-server
    environment:
      - RTSP_URL=rtsp://rtsp-server:8554/test
```

---

## 🛠️ Setup Scripts

### Camera Configuration Script

```bash
#!/bin/bash
# setup-test-cameras.sh

# Configure cameras with static IPs
configure_camera() {
    local name=$1
    local ip=$2
    local username=$3
    local password=$4
    
    echo "Configuring $name at $ip..."
    
    # Use camera API or web interface
    # curl -u $username:$password http://$ip/api/settings \
    #   -d '{"network":{"ip":"'$ip'"}}'
}

configure_camera "hikvision" "192.168.1.101" "admin" "password123"
configure_camera "dahua" "192.168.1.102" "admin" "password123"
configure_camera "axis" "192.168.1.103" "admin" "password123"
```

---

### Connectivity Test Script

```bash
#!/bin/bash
# verify-connectivity.sh

echo "Testing camera connectivity..."

for camera in hikvision dahua axis; do
    case $camera in
        hikvision) IP="192.168.1.101" ;;
        dahua) IP="192.168.1.102" ;;
        axis) IP="192.168.1.103" ;;
    esac
    
    echo -n "$camera: "
    if ping -c 1 $IP > /dev/null 2>&1; then
        echo "✅ Ping OK"
    else
        echo "❌ Ping FAILED"
    fi
    
    # Test RTSP port
    if timeout 5 bash -c "cat < /dev/null > /dev/tcp/$IP/554" 2>/dev/null; then
        echo "✅ RTSP Port 554 OPEN"
    else
        echo "❌ RTSP Port 554 CLOSED"
    fi
done
```

---

## 📈 Success Criteria

### Day 2 Completion

- [ ] Test environment ready (physical or virtual)
- [ ] All 3 cameras configured
- [ ] RTSP streams verified with ffprobe
- [ ] Network connectivity confirmed
- [ ] Test scripts executable
- [ ] Basic connection test passed

---

## ⏱️ Timeline

| Time | Activity | Duration |
|------|----------|----------|
| 09:00-09:30 | Choose setup option | 30 min |
| 09:30-10:30 | Acquire/configure cameras | 60 min |
| 10:30-11:00 | Network setup | 30 min |
| 11:00-11:30 | Camera configuration | 30 min |
| 11:30-12:00 | Verification testing | 30 min |
| 13:00-14:00 | Script testing | 60 min |
| 14:00-15:00 | Documentation | 60 min |

**Total:** ~5 hours

---

## 🚨 Potential Issues

### Issue 1: Cameras Not Available

**Solution:**
- Use virtual cameras with FFmpeg
- Use public RTSP servers
- Borrow cameras from colleagues

---

### Issue 2: Network Configuration Problems

**Solution:**
- Use localhost testing
- Configure port forwarding
- Use Docker network

---

### Issue 3: Authentication Issues

**Solution:**
- Reset camera passwords
- Try without authentication
- Check authentication type (Basic/Digest)

---

## 📁 Deliverables

### Expected Outputs

1. **Configured test environment**
   - Physical or virtual cameras
   - Network topology documented
   
2. **Verification report**
   - Connectivity test results
   - Stream quality metrics
   
3. **Test configuration files**
   - camera-config.json
   - network-setup.md
   
4. **Documentation**
   - Setup guide
   - Troubleshooting guide

---

## 📞 Preparation Checklist

### Before Day 2

- [ ] Review camera specifications
- [ ] Decide on setup option (physical/virtual/public)
- [ ] Prepare test video files (if virtual)
- [ ] Download FFmpeg (if needed)
- [ ] Review test scripts
- [ ] Prepare network equipment

### During Day 2

- [ ] Execute setup plan
- [ ] Document any issues
- [ ] Capture verification results
- [ ] Update documentation

---

**Plan Created:** June 6, 2026  
**Target Execution:** June 7, 2026  
**Status:** Ready for execution
