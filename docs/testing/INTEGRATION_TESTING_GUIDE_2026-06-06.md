# 🎥 IP-CSS Integration Testing Guide

**Purpose:** Comprehensive guide for RTSP camera integration testing  
**Week:** 2 (Days 3-4)  
**Status:** Ready for Execution

---

## 📋 Overview

This guide covers integration testing with real IP cameras to validate the RTSP client implementation.

### Test Objectives

1. ✅ Verify connectivity with multiple camera models
2. ✅ Validate video stream decoding
3. ✅ Test audio stream handling
4. ✅ Measure performance metrics
5. ✅ Validate reconnection logic
6. ✅ Ensure stability over extended periods

---

## 🎥 Test Cameras

### Camera 1: Hikvision DS-2CD2342WD-I

**Specifications:**
- Resolution: 2688x1520 (4MP)
- Codec: H.264/H.265
- FPS: 25
- Audio: G.711 PCMU
- RTSP Port: 554

**RTSP URLs:**
```
Main Stream: rtsp://admin:password123@192.168.1.101:554/Streaming/Channels/101
Sub Stream:  rtsp://admin:password123@192.168.1.101:554/Streaming/Channels/102
```

**Test Scenarios:**
- H.264 Main Profile
- High Resolution (4MP)
- G.711 Audio
- Digest Authentication

---

### Camera 2: Dahua IPC-HFW2431S

**Specifications:**
- Resolution: 2688x1520 (4MP)
- Codec: H.264/H.265
- FPS: 20
- Audio: G.711 PCMA
- RTSP Port: 554

**RTSP URLs:**
```
Main Stream: rtsp://admin:password123@192.168.1.102:554/cam/realmonitor?channel=1&substream=0
Sub Stream:  rtsp://admin:password123@192.168.1.102:554/cam/realmonitor?channel=1&substream=1
```

**Test Scenarios:**
- H.264 Baseline Profile
- H.265 Main Profile
- G.711 A-law Audio
- Basic Authentication

---

### Camera 3: Axis M1065-L

**Specifications:**
- Resolution: 1920x1080 (Full HD)
- Codec: H.264/MJPEG
- FPS: 30
- Audio: Not supported
- RTSP Port: 554

**RTSP URLs:**
```
H.264 Stream: rtsp://admin:password123@192.168.1.103:554/axis-media/media.amp?videocodec=h264
MJPEG Stream: rtsp://admin:password123@192.168.1.103:554/axis-media/media.amp?videocodec=mjpeg
```

**Test Scenarios:**
- H.264 High Profile
- MJPEG Progressive
- No Audio
- Advanced Authentication

---

## 🧪 Test Suite

### 1. Connection Tests

**Objective:** Verify basic connectivity

**Test Cases:**
1. **Connect with credentials**
   - Connect to camera with valid username/password
   - Expected: Connection established in < 5 seconds
   
2. **Connect without credentials**
   - Connect to camera without authentication
   - Expected: Connection fails or succeeds (depends on camera config)
   
3. **Connect with invalid credentials**
   - Connect with wrong password
   - Expected: Authentication failure

4. **Connect to invalid URL**
   - Connect to non-existent camera
   - Expected: Timeout after 10 seconds

---

### 2. Stream Tests

**Objective:** Validate stream access and quality

**Test Cases:**
1. **Main stream access**
   - Access primary high-resolution stream
   - Expected: Stream available, correct resolution
   
2. **Sub stream access**
   - Access secondary low-resolution stream
   - Expected: Stream available, lower resolution
   
3. **Codec detection**
   - Auto-detect video codec
   - Expected: Correct codec identified (H.264/H.265/MJPEG)
   
4. **Audio stream access**
   - Access audio stream (if available)
   - Expected: Audio codec detected (G.711)

---

### 3. Decoding Tests

**Objective:** Validate video/audio decoding

**Test Cases:**
1. **H.264 decoding**
   - Decode H.264 video stream
   - Expected: Frames decoded correctly, no artifacts
   
2. **H.265 decoding**
   - Decode H.265 video stream
   - Expected: Frames decoded correctly
   
3. **MJPEG decoding**
   - Decode MJPEG progressive stream
   - Expected: Frames decoded correctly
   
4. **G.711 audio decoding**
   - Decode G.711 PCMU/PCMA audio
   - Expected: Audio samples decoded correctly

---

### 4. Performance Tests

**Objective:** Measure performance metrics

**Test Cases:**
1. **Frame rate measurement**
   - Measure actual frames per second
   - Expected: 20-30 FPS (depends on camera)
   
2. **Latency measurement**
   - Measure end-to-end latency
   - Expected: < 3 seconds
   
3. **CPU usage**
   - Monitor CPU utilization
   - Expected: < 30% per stream
   
4. **Memory usage**
   - Monitor memory consumption
   - Expected: < 100MB per stream

---

### 5. Stability Tests

**Objective:** Validate long-term stability

**Test Cases:**
1. **1-hour continuous streaming**
   - Stream continuously for 1 hour
   - Expected: No crashes, no memory leaks
   
2. **Reconnection test (10 cycles)**
   - Disconnect and reconnect 10 times
   - Expected: All reconnections successful
   
3. **Network interruption**
   - Simulate network interruption for 30 seconds
   - Expected: Automatic reconnection

---

## 📊 Test Commands

### Basic Connection Test

```bash
# Test single camera
./scripts/test-rtsp-cameras.sh --cameras hikvision --duration 60

# Verbose output
./scripts/test-rtsp-cameras.sh --cameras hikvision --verbose

# Specific codec
./scripts/test-rtsp-cameras.sh --cameras dahua --codec h265 --duration 120
```

### Full Test Suite

```bash
# Test all cameras
./scripts/test-rtsp-cameras.sh --cameras hikvision,dahua,axis --duration 3600

# Generate results
./scripts/test-rtsp-cameras.sh --cameras all --duration 3600 --verbose
```

### Soak Test (24 hours)

```bash
# Long-duration test
./scripts/test-rtsp-cameras.sh --cameras hikvision --duration 86400

# Monitor resources
watch -n 60 'ps aux | grep ffmpeg'
```

---

## 📈 Metrics Collection

### Connection Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Connection Time | < 5s | RTT from request to establish |
| Authentication Time | < 2s | Time for auth handshake |
| Stream Setup Time | < 3s | Time to start receiving frames |
| Reconnection Time | < 5s | Time to reconnect after failure |

### Performance Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Frame Rate | 20-30 FPS | Frames decoded per second |
| Latency | < 3s | RTT from capture to display |
| CPU Usage | < 30% | Process CPU utilization |
| Memory Usage | < 100MB | Resident set size |
| Frame Drops | < 1% | Dropped frames / total frames |

### Quality Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Video Quality | No artifacts | Visual inspection |
| Audio Sync | < 200ms offset | Audio/video alignment |
| Packet Loss | < 0.1% | Lost RTP packets |
| Jitter | < 50ms | Frame timing variance |

---

## 🛠️ Test Environment Setup

### Hardware Requirements

- 3 IP cameras (Hikvision, Dahua, Axis)
- Network switch (Gigabit recommended)
- Test PC (Windows/Linux/macOS)
- Monitor for visual verification

### Software Requirements

- FFmpeg 8.0+ (for stream probing)
- Wireshark (for network analysis)
- Git (for version control)
- Gradle 8.9 (for building)

### Network Configuration

```
Test Network: 192.168.1.0/24
Camera 1 (Hikvision): 192.168.1.101
Camera 2 (Dahua): 192.168.1.102
Camera 3 (Axis): 192.168.1.103
Test PC: 192.168.1.100
```

---

## 📝 Test Execution Workflow

### Phase 1: Setup (30 minutes)

1. **Configure cameras**
   - Set static IP addresses
   - Enable RTSP streams
   - Configure authentication
   
2. **Verify connectivity**
   - Ping each camera
   - Test RTSP URLs with ffprobe
   
3. **Setup test environment**
   - Install dependencies
   - Build native library
   - Configure test scripts

---

### Phase 2: Connection Tests (1 hour)

1. **Basic connectivity**
   - Test each camera connection
   - Verify authentication
   
2. **Stream access**
   - Access main/sub streams
   - Verify stream metadata
   
3. **Codec detection**
   - Auto-detect codecs
   - Verify codec parameters

---

### Phase 3: Decoding Tests (2 hours)

1. **Video decoding**
   - H.264 stream decoding
   - H.265 stream decoding
   - MJPEG stream decoding
   
2. **Audio decoding**
   - G.711 PCMU decoding
   - G.711 PCMA decoding
   - Audio/video sync

---

### Phase 4: Performance Tests (2 hours)

1. **Frame rate benchmarking**
   - Measure FPS for each camera
   - Compare with camera specs
   
2. **Latency measurement**
   - Measure end-to-end latency
   - Analyze bottlenecks
   
3. **Resource monitoring**
   - CPU usage tracking
   - Memory usage tracking
   - Network bandwidth

---

### Phase 5: Stability Tests (24 hours)

1. **Soak testing**
   - 24-hour continuous streaming
   - Monitor for memory leaks
   
2. **Reconnection testing**
   - 10 reconnection cycles
   - Verify recovery logic
   
3. **Network interruption**
   - Simulate network failures
   - Verify automatic recovery

---

## 📊 Results Reporting

### Test Results Template

```json
{
  "timestamp": 1717728000,
  "camera": "hikvision",
  "codec": "H.264",
  "resolution": "2688x1520",
  "duration": 3600,
  "metrics": {
    "connectionTime": 3.2,
    "frameRate": 24.5,
    "latency": 1.8,
    "cpuUsage": 22.3,
    "memoryUsage": 78.5,
    "frameDrops": 2,
    "totalFrames": 88200,
    "reconnections": 0,
    "errors": 0
  },
  "status": "passed"
}
```

### Success Criteria

| Criteria | Target | Pass/Fail |
|----------|--------|-----------|
| Connection Success | > 95% | Pass |
| Frame Rate | 20-30 FPS | Pass |
| Latency | < 3s | Pass |
| CPU Usage | < 30% | Pass |
| Memory Usage | < 100MB | Pass |
| Frame Drops | < 1% | Pass |
| Reconnections | 100% success | Pass |

---

## 🐛 Troubleshooting

### Issue: Connection Timeout

**Symptoms:**
- Connection fails after 10 seconds
- No response from camera

**Solutions:**
1. Verify camera IP address
2. Check network connectivity (ping)
3. Verify RTSP port (554) is open
4. Check firewall settings

---

### Issue: Authentication Failure

**Symptoms:**
- 401 Unauthorized error
- Connection rejected

**Solutions:**
1. Verify username/password
2. Check authentication type (Basic/Digest)
3. Verify camera authentication settings
4. Try without authentication

---

### Issue: Stream Not Available

**Symptoms:**
- Stream URL returns error
- No video frames received

**Solutions:**
1. Verify RTSP URL format
2. Check camera stream configuration
3. Try different stream (main/sub)
4. Use ffprobe to verify stream

---

### Issue: High CPU Usage

**Symptoms:**
- CPU usage > 50%
- System slowdown

**Solutions:**
1. Reduce resolution
2. Use hardware acceleration
3. Optimize decoding parameters
4. Profile code for bottlenecks

---

## ✅ Test Checklist

### Pre-Test
- [ ] Cameras configured and powered
- [ ] Network connectivity verified
- [ ] FFmpeg installed (8.0+)
- [ ] Native library compiled
- [ ] Test scripts ready

### During Test
- [ ] Connection tests passed
- [ ] Stream access verified
- [ ] Decoding working correctly
- [ ] Performance within targets
- [ ] No crashes or errors

### Post-Test
- [ ] Results documented
- [ ] Metrics collected
- [ ] Issues logged
- [ ] Report generated

---

**Guide Version:** 1.0.0  
**Last Updated:** June 6, 2026  
**Owner:** IP-CSS Team
