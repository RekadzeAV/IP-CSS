# 🎯 Week 2 Integration Testing Plan

**Date:** 27 April 2026  
**Week:** 2 (May 4 - May 10)  
**Owner:** Tech Lead  
**Status:** 🟡 **PLANNING PHASE**  
**Target:** 90-95% completion

---

## 📋 Overview

**Objective:** Complete RTSP Client integration testing with real cameras and achieve MVP readiness.

**Current Progress:** P0-1 at 70%  
**Target:** 90-95% by end of Week 2

**Key Activities:**
1. Integration testing with 3+ camera models
2. 24-hour soak testing
3. Performance benchmarking
4. iOS platform support
5. Bug fixes and polish

---

## 🎯 Success Criteria

### MVP Definition of Done
- [x] FFmpeg 8.0 API compatible
- [x] FFI implementation complete
- [x] Video codecs (H.264/H.265/MJPEG) working
- [x] Audio codecs (AAC/G.711) working
- [x] 60+ unit tests passing
- [ ] Real camera testing complete
- [ ] 24h soak test passed
- [ ] Performance targets met
- [ ] iOS support working

### Performance Targets
| Metric | Target | Current |
|--------|--------|---------|
| Latency | < 3 seconds | N/A |
| Frame Rate | 20-25 FPS | N/A |
| CPU Usage | < 30% | N/A |
| Memory | < 100MB/stream | N/A |
| Connection Success | > 95% | N/A |

---

## 📅 Week 2 Schedule

### Day 1 (May 4) - Build & Unit Tests
**Focus:** Compile and verify all components

**Tasks:**
1. ⏳ Compile native library for all platforms
   - [ ] Linux x64
   - [ ] macOS x64/arm64
   - [ ] Windows x64
2. ⏳ Run all 60+ unit tests
   - [ ] Audio decoder tests (16)
   - [ ] Video decoder tests (17)
   - [ ] NativeRtspClient tests (16)
   - [ ] Integration tests (15)
3. ⏳ Fix any compilation errors
4. ⏳ Verify FFmpeg 8.0 API

**Deliverables:**
- Compiled libraries for 3 platforms
- Test results report
- Compilation fixes applied

**Time:** 8 hours

---

### Day 2 (May 5) - Camera Test Setup
**Focus:** Test environment preparation

**Tasks:**
1. ⏳ Acquire test cameras
   - [ ] Hikvision DS-2CD2342WD-I (H.264)
   - [ ] Dahua IPC-HFW2431S (H.264/H.265)
   - [ ] Axis M1065-L (H.264/MJPEG)
2. ⏳ Setup test network
   - [ ] Isolated VLAN
   - [ ] Network monitoring (Wireshark)
   - [ ] RTSP test server
3. ⏳ Configure cameras
   - [ ] RTSP streams enabled
   - [ ] Authentication configured
   - [ ] Multiple resolutions tested
4. ⏳ Prepare test scripts

**Deliverables:**
- Test network ready
- 3 cameras configured
- Test environment documented

**Time:** 8 hours

---

### Day 3 (May 6) - Integration Testing I
**Focus:** Basic connectivity tests

**Tasks:**
1. ⏳ Connection tests
   - [ ] Hikvision H.264 stream
   - [ ] Dahua H.264 stream
   - [ ] Dahua H.265 stream
   - [ ] Axis H.264 stream
   - [ ] Axis MJPEG stream
2. ⏳ Authentication tests
   - [ ] Basic auth
   - [ ] Digest auth
   - [ ] No auth
3. ⏳ Stream quality tests
   - [ ] 1920x1080 @ 25fps
   - [ ] 1280x720 @ 25fps
   - [ ] 640x480 @ 25fps
4. ⏳ Log connection metrics

**Deliverables:**
- Connection test report
- Camera compatibility matrix
- Authentication working

**Time:** 8 hours

---

### Day 4 (May 7) - Integration Testing II
**Focus:** Performance and stability

**Tasks:**
1. ⏳ Performance benchmarking
   - [ ] Frame rate measurement
   - [ ] Latency measurement
   - [ ] CPU usage monitoring
   - [ ] Memory usage monitoring
2. ⏳ Stability tests
   - [ ] 1-hour continuous streaming
   - [ ] Reconnection tests (10 cycles)
   - [ ] Network interruption tests
3. ⏳ Audio tests
   - [ ] AAC audio stream
   - [ ] G.711 PCMU stream
   - [ ] G.711 PCMA stream
   - [ ] Audio/video sync
4. ⏳ Collect metrics

**Deliverables:**
- Performance benchmark report
- Stability test results
- Audio sync verification

**Time:** 8 hours

---

### Day 5 (May 8) - Soak Testing
**Focus:** Long-duration stability

**Tasks:**
1. ⏳ 24-hour soak test
   - [ ] Hikvision camera (primary)
   - [ ] Continuous streaming
   - [ ] Monitor memory leaks
   - [ ] Monitor CPU usage
   - [ ] Log all events
2. ⏳ Overnight monitoring
   - [ ] Set up alerts
   - [ ] Automated health checks
   - [ ] Frame drop monitoring
3. ⏳ Analysis
   - [ ] Memory usage trend
   - [ ] Connection stability
   - [ ] Frame quality over time

**Deliverables:**
- 24h soak test report
- Memory leak analysis
- Stability metrics

**Time:** 16 hours (overnight)

---

### Day 6 (May 9) - iOS Support
**Focus:** Apple platform integration

**Tasks:**
1. ⏳ cinterop generation
   - [ ] Generate bindings for iOS arm64
   - [ ] Generate bindings for iOS simulator
   - [ ] Configure Xcode project
2. ⏳ iOS implementation
   - [ ] NativeRtspClient.ios.kt
   - [ ] Platform-specific handling
   - [ ] Memory management
3. ⏳ Testing
   - [ ] iOS simulator tests
   - [ ] Physical device tests (iPhone/iPad)
   - [ ] Test with camera
4. ⏳ Bug fixes

**Deliverables:**
- iOS platform support
- iOS test results
- Platform-specific fixes

**Time:** 8 hours

---

### Day 7 (May 10) - Polish & Documentation
**Focus:** Final cleanup

**Tasks:**
1. ⏳ Bug fixes
   - [ ] Address issues from testing
   - [ ] Performance optimizations
   - [ ] Edge case handling
2. ⏳ Documentation
   - [ ] Update API documentation
   - [ ] Performance benchmarks
   - [ ] Known issues list
   - [ ] Deployment guide updates
3. ⏳ Final review
   - [ ] Code review
   - [ ] Test coverage review
   - [ ] Documentation completeness
4. ⏳ Prepare beta release

**Deliverables:**
- Updated documentation
- Bug fix report
- Beta release candidate

**Time:** 8 hours

---

## 🎥 Test Cameras

### Camera 1: Hikvision DS-2CD2342WD-I
**Specifications:**
- Resolution: 2688x1520 (4MP)
- Codec: H.264/H.265
- FPS: 25
- Audio: G.711 PCMU

**RTSP URLs:**
```
Main Stream: rtsp://admin:password@192.168.1.101:554/Streaming/Channels/101
Sub Stream: rtsp://admin:password@192.168.1.101:554/Streaming/Channels/102
```

**Test Scenarios:**
- H.264 main profile
- High resolution (4MP)
- G.711 audio
- Digest authentication

---

### Camera 2: Dahua IPC-HFW2431S
**Specifications:**
- Resolution: 2688x1520 (4MP)
- Codec: H.264/H.265
- FPS: 20
- Audio: G.711 PCMA

**RTSP URLs:**
```
Main Stream: rtsp://admin:password@192.168.1.102:554/cam/realmonitor?channel=1&substream=0
Sub Stream: rtsp://admin:password@192.168.1.102:554/cam/realmonitor?channel=1&substream=1
```

**Test Scenarios:**
- H.264 baseline profile
- H.265 main profile
- G.711 A-law audio
- Basic authentication

---

### Camera 3: Axis M1065-L
**Specifications:**
- Resolution: 1920x1080
- Codec: H.264/MJPEG
- FPS: 30
- Audio: Not supported

**RTSP URLs:**
```
H.264 Stream: rtsp://admin:password@192.168.1.103:554/axis-media/media.amp?videocodec=h264
MJPEG Stream: rtsp://admin:password@192.168.1.103:554/axis-media/media.amp?videocodec=mjpeg
```

**Test Scenarios:**
- H.264 high profile
- MJPEG progressive
- No audio
- Advanced authentication

---

## 📊 Test Scripts

### Connection Test Script
```bash
./scripts/test-camera-connection.sh \
  --camera hikvision \
  --url "rtsp://192.168.1.101:554/Streaming/Channels/101" \
  --username admin \
  --password password123 \
  --timeout 10000
```

**Expected:** Connection established in < 5 seconds

---

### Performance Benchmark Script
```bash
./scripts/benchmark-stream.sh \
  --camera dahua \
  --duration 60 \
  --resolution 1920x1080 \
  --output build/benchmark.json
```

**Metrics:**
- Frame rate
- Latency
- CPU usage
- Memory usage

---

### Soak Test Script
```bash
./scripts/soak-test.sh \
  --camera hikvision \
  --duration 86400 \
  --monitor-interval 60 \
  --output build/soak-test.json
```

**Monitoring:**
- Memory every 60s
- CPU every 60s
- Frame drops
- Reconnection events

---

## 📈 Metrics Collection

### Connection Metrics
- Connection time
- Authentication time
- Stream setup time
- Reconnection time

### Performance Metrics
- Frame rate (FPS)
- Frame drop rate
- Latency (RTT)
- CPU usage (%)
- Memory usage (MB)

### Quality Metrics
- Video quality score
- Audio sync offset
- Packet loss rate
- Jitter (ms)

### Stability Metrics
- Uptime percentage
- Crash count
- Error count
- Reconnection count

---

## 🚨 Risk Assessment

### High Risk Items

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Camera incompatibility | Medium | High | Test multiple models |
| Memory leaks | Medium | High | Soak testing + Valgrind |
| Performance issues | High | Medium | Early benchmarking |
| Audio sync problems | Medium | Medium | Audio timestamp testing |

### Contingency Plans

**If H.265 fails:**
1. Fallback to H.264
2. Document limitation
3. Post-MVP fix

**If audio fails:**
1. Video-only mode
2. Audio as optional feature
3. Post-MVP enhancement

**If performance poor:**
1. Optimize decoding
2. Reduce resolution
3. Hardware acceleration (Post-MVP)

---

## ✅ Acceptance Checklist

### Integration Testing
- [ ] 3 camera models tested
- [ ] H.264 streaming working
- [ ] H.265 streaming working
- [ ] MJPEG streaming working
- [ ] Audio streaming working
- [ ] Authentication working
- [ ] Reconnection working

### Performance
- [ ] Latency < 3 seconds
- [ ] Frame rate > 20 FPS
- [ ] CPU usage < 30%
- [ ] Memory < 100MB/stream
- [ ] No memory leaks (24h)

### Stability
- [ ] 24h soak test passed
- [ ] Connection stability > 95%
- [ ] No crashes
- [ ] Reconnection works

### Platform Support
- [ ] Linux x64 working
- [ ] macOS x64/arm64 working
- [ ] Windows x64 working
- [ ] Android working
- [ ] iOS working (optional)

### Documentation
- [ ] API documentation complete
- [ ] Deployment guide updated
- [ ] Test results documented
- [ ] Known issues listed

---

## 📎 Resources

### Required Hardware
- 3 IP cameras (Hikvision, Dahua, Axis)
- Test network switch
- Monitoring PC with Wireshark
- iOS device for testing

### Required Software
- FFmpeg 8.0+
- CMake 3.15+
- Wireshark (network monitoring)
- Valgrind (memory testing)
- Xcode (iOS testing)

### Test Scripts
- `scripts/run-all-tests.sh`
- `scripts/test-rtsp-cameras.sh`
- `scripts/benchmark-stream.sh`
- `scripts/soak-test.sh`

---

## 📞 Team & Responsibilities

**Tech Lead:** Overall coordination, integration testing  
**Backend Dev:** Camera setup, network configuration  
**Frontend Dev:** Performance monitoring, metrics collection  
**QA Engineer:** Test execution, bug reporting  
**DevOps:** Build automation, CI/CD integration

---

**Plan Created:** 27 April 2026  
**Week Duration:** 7 days (May 4-10)  
**Target Completion:** 90-95%  
**Next Review:** Daily standup (09:00)
