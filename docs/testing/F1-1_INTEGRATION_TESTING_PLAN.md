# F1-1 Integration Testing Plan

**Date:** 31 May 2026  
**Phase:** 6 - Integration Testing  
**Status:** 🟡 Pending (after unit tests)

---

## Overview

Integration testing plan for RTSP client with native FFmpeg integration. Tests will verify real-world functionality with IP cameras and streaming scenarios.

---

## Test Categories

### 1. Connection Tests

#### 1.1 Basic Connection
- [ ] Connect to RTSP server
- [ ] Verify session establishment
- [ ] Test disconnect/cleanup
- [ ] Test reconnection after failure

#### 1.2 Network Conditions
- [ ] Test with stable connection
- [ ] Test with intermittent connection
- [ ] Test with high latency (100ms+)
- [ ] Test with packet loss (1-5%)

### 2. Codec Tests

#### 2.1 H.264 Video
- [ ] Decode H.264 stream
- [ ] Verify frame integrity
- [ ] Test with different profiles (Baseline, Main, High)
- [ ] Test with different resolutions (720p, 1080p, 4K)

#### 2.2 H.265/HEVC Video
- [ ] Decode H.265 stream
- [ ] Verify frame integrity
- [ ] Test with 10-bit color
- [ ] Test with HDR

#### 2.3 Audio Codecs
- [ ] Decode AAC audio
- [ ] Decode MP3 audio
- [ ] Test audio/video sync
- [ ] Test mono/stereo channels

### 3. Performance Tests

#### 3.1 Latency
- [ ] Measure end-to-end latency
- [ ] Target: < 500ms
- [ ] Test with buffer sizes: 256KB, 512KB, 1MB, 2MB

#### 3.2 Throughput
- [ ] Measure FPS stability
- [ ] Target: 30-60 FPS sustained
- [ ] Test with high bitrate streams (>10 Mbps)

#### 3.3 Resource Usage
- [ ] Monitor CPU usage
- [ ] Target: < 30% per stream
- [ ] Monitor memory usage
- [ ] Target: < 100MB per stream
- [ ] Test with multiple concurrent streams

### 4. Error Handling Tests

#### 4.1 Network Errors
- [ ] Handle connection timeout
- [ ] Handle DNS resolution failure
- [ ] Handle server unavailable
- [ ] Test automatic reconnection

#### 4.2 Stream Errors
- [ ] Handle corrupted frames
- [ ] Handle missing I-frames
- [ ] Handle bitrate changes
- [ ] Handle resolution changes

#### 4.3 Resource Errors
- [ ] Handle memory exhaustion
- [ ] Handle decoder initialization failure
- [ ] Handle cleanup on exception

### 5. Multi-Camera Tests

#### 5.1 Concurrent Streams
- [ ] Test 2 concurrent streams
- [ ] Test 4 concurrent streams
- [ ] Test 8 concurrent streams
- [ ] Verify no resource leaks

#### 5.2 Stream Management
- [ ] Start/stop individual streams
- [ ] Pause/resume streams
- [ ] Seek within stream
- [ ] Handle stream priority

---

## Test Environment

### Hardware Requirements

- **Camera(s):** At least 1 real IP camera
  - Support H.264 and/or H.265
  - RTSP stream output
  - Optional: Audio support

- **Network:**
  - Gigabit Ethernet
  - Stable connection (< 1% packet loss)
  - Latency < 50ms

### Software Requirements

- **FFmpeg:** 8.1.1
- **RTSP Client:** Native library (video_processing.dll)
- **Test Framework:** JUnit 5 (Kotlin)
- **Mock Server:** FFmpeg or similar for mock streams

### Test Cameras

| Camera | Model | Codec | Resolution | Audio | Status |
|--------|-------|-------|------------|-------|--------|
| Cam 1 | TODO | H.264 | 1080p | AAC | ⏳ Pending |
| Cam 2 | TODO | H.265 | 4K | AAC | ⏳ Pending |

---

## Test Scripts

### Mock Stream Tests

```powershell
# Start mock RTSP server with test stream
.\scripts\start-mock-rtsp.ps1 -Stream h264

# Run integration tests
.\scripts\test-integration-rtsp.ps1 -TestMode mock
```

### Real Camera Tests

```powershell
# Configure camera URL
$env:RTSP_CAMERA_URL = "rtsp://user:pass@192.168.1.100:554/stream1"

# Run real camera tests
.\scripts\test-integration-rtsp.ps1 -TestMode real
```

---

## Success Criteria

### Functional

- ✅ All connection tests pass
- ✅ All codec tests pass
- ✅ Error handling works correctly
- ✅ Multi-camera support stable

### Performance

- ✅ Latency < 500ms (target: 200ms)
- ✅ FPS stable at 30+
- ✅ CPU usage < 30% per stream
- ✅ Memory usage < 100MB per stream
- ✅ No memory leaks after 1 hour

### Reliability

- ✅ 99.9% uptime in 24-hour test
- ✅ Automatic reconnection works
- ✅ Graceful degradation on errors
- ✅ No crashes in stress tests

---

## Test Execution Schedule

| Phase | Duration | Status |
|-------|----------|--------|
| Unit Tests | 1-2 hours | 🟡 In progress |
| Mock Integration Tests | 2-3 hours | ⏳ Pending |
| Real Camera Tests | 1-2 days | ⏳ Pending |
| Performance Tests | 1 day | ⏳ Pending |
| Stress Tests | 24 hours | ⏳ Pending |
| **Total** | **3-5 days** | **In progress** |

---

## Test Results Template

```markdown
## Test Run: [Date]

### Connection Tests
- [ ] Basic Connection: ✅ / ❌
- [ ] Network Conditions: ✅ / ❌

### Codec Tests
- [ ] H.264: ✅ / ❌
- [ ] H.265: ✅ / ❌
- [ ] Audio: ✅ / ❌

### Performance
- Latency: [value] ms
- FPS: [value]
- CPU: [value]%
- Memory: [value] MB

### Issues
- [Issue 1]
- [Issue 2]

### Conclusion
[Summary and recommendations]
```

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| No real camera available | High | Use mock streams, request camera loan |
| Network instability | Medium | Test on wired connection, use network emulator |
| FFmpeg compatibility | Low | Fixed version 8.1.1, tested locally |
| Memory leaks | High | Run stress tests, use memory profiler |
| Performance issues | Medium | Profile early, optimize hot paths |

---

## Next Steps

1. ✅ Complete unit tests (in progress)
2. ⏳ Set up test environment
3. ⏳ Run mock stream tests
4. ⏳ Acquire real camera(s)
5. ⏳ Run integration tests
6. ⏳ Performance optimization (if needed)
7. ⏳ Final validation

---

**Prepared by:** Koda (AI Assistant)  
**Date:** 31 May 2026  
**Status:** Pending unit test completion
