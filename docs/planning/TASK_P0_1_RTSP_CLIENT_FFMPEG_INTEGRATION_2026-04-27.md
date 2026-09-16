# 🎯 Task P0-1: RTSP Client - FFmpeg Integration

**Priority:** P0 - CRITICAL  
**Owner:** Tech Lead  
**Estimated:** 3-4 weeks  
**Status:** ⚠️ **15% Complete**  
**Started:** 27 April 2026

---

## 📋 Overview

**Problem:** RTSP client has 85% C++ implementation but FFmpeg integration with Kotlin FFI is incomplete, blocking video streaming functionality.

**Impact:** **BLOCKS MVP** - No live video streaming without this task.

**Current Progress:**
- ✅ Native C++ RTSP client: 85% complete
- ✅ Kotlin wrapper structure: 100% complete  
- ⚠️ FFI bindings: 40% complete
- ❌ FFmpeg-Kotlin integration: 0% complete

---

## 🎯 Success Criteria

**MVP Definition of Done:**
1. [ ] RTSP connection to real camera succeeds
2. [ ] Video frames flow from native to Kotlin
3. [ ] H.264 decoding works
4. [ ] Connection stability > 95% over 1 hour
5. [ ] Audio decoding works (optional for MVP)

**Target Metrics:**
- Latency: < 3 seconds
- Frame rate: 20-25 FPS
- CPU usage: < 30%
- Memory: < 100MB per stream

---

## 📝 Subtasks Breakdown

### Subtask P0-1.1: FFmpeg API Compatibility Fix
**Priority:** P0 - CRITICAL  
**Estimated:** 3-5 days  
**Status:** ⚠️ Not Started

**Tasks:**
- [ ] **Day 1:** Audit FFmpeg 8.0 API changes
  - [ ] Review breaking changes from FFmpeg 7.x to 8.x
  - [ ] Identify deprecated functions in codebase
  - [ ] Create compatibility shim layer

- [ ] **Day 2:** Fix audio decoder API issues
  - [ ] Update `av_channel_layout` usage (replaces `channels`)
  - [ ] Fix `avcodec_send_packet` / `avcodec_receive_frame` pattern
  - [ ] Update swresample API for audio resampling

- [ ] **Day 3:** Fix CMake build configuration
  - [ ] Update FFmpeg pkg-config paths
  - [ ] Fix symbol visibility for FFI
  - [ ] Resolve linking errors

- [ ] **Day 4:** Test compilation on all platforms
  - [ ] Linux x86_64
  - [ ] Android ARM64
  - [ ] iOS arm64
  - [ ] Desktop x86_64

- [ ] **Day 5:** Create regression tests
  - [ ] FFmpeg API compatibility tests
  - [ ] Platform-specific build tests
  - [ ] **Acceptance:** Clean build on all platforms

**Files to Modify:**
- `native/video-processing/CMakeLists.txt`
- `native/video-processing/src/audio_decoder.cpp`
- `native/video-processing/include/audio_decoder.h`

**Acceptance:**
```bash
./gradlew :native:video-processing:assembleDebug
# Must succeed on all platforms without errors
```

---

### Subtask P0-1.2: FFmpeg-Kotlin FFI Completion
**Priority:** P0 - CRITICAL  
**Estimated:** 1-2 weeks  
**Status:** ⚠️ Not Started

**Tasks:**

- [ ] **Week 1, Day 1-2:** JNI Bridge for Video Frames
  - [ ] Create `NativeRtspClient.kt` complete implementation
  - [ ] Implement `setFrameCallback` for video frames
  - [ ] Create JavaFX `Frame` wrapper class
  - [ ] Memory management for native buffers
  - [ ] **Acceptance:** Video frame callback invoked

```kotlin
// Target interface
nativeClient.setFrameCallback(handle, RTSP_STREAM_VIDEO) { frame ->
    // frame.data contains YUV or RGB data
    // frame.width, frame.height set correctly
    // frame.timestamp in milliseconds
}
```

- [ ] **Week 1, Day 3-4:** Callback Mechanism
  - [ ] Implement thread-safe callback from native to Kotlin
  - [ ] Handle callback from RTP thread (avoid blocking)
  - [ ] Use coroutines for frame processing
  - [ ] **Acceptance:** No deadlocks or race conditions

- [ ] **Week 1, Day 5:** Memory Management
  - [ ] Allocate native buffers for frames
  - [ ] Copy to Kotlin heap safely
  - [ ] Release native buffers after use
  - [ ] **Acceptance:** No memory leaks over 24h test

**Files to Create/Modify:**
- `core/network/src/nativeMain/kotlin/NativeRtspClient.kt`
- `core/network/src/commonMain/kotlin/NativeRtspClient.kt`
- `native/video-processing/src/rtsp_client.cpp`
- `native/video-processing/include/rtsp_client.h`

**Acceptance:**
```kotlin
// Test code
val rtspClient = RtspClient(config)
rtspClient.setVideoFrameCallback { frame ->
    assertEquals(1920, frame.width)
    assertEquals(1080, frame.height)
    assertTrue(frame.data.size > 0)
}
```

---

### Subtask P0-1.3: Codec Support Implementation
**Priority:** P0 - CRITICAL  
**Estimated:** 1 week  
**Status:** ⚠️ Not Started

**Tasks:**

- [ ] **Day 1-2:** H.264 Decoding
  - [ ] Parse H.264 NAL units from RTP payload
  - [ ] Handle SPS/PPS parameters
  - [ ] Decode to YUV420 or RGB
  - [ ] **Acceptance:** Test with H.264 camera

- [ ] **Day 3:** H.265/HEVC Decoding
  - [ ] Parse H.265 NAL units
  - [ ] Handle VPS/SPS/PPS
  - [ ] Decode to YUV420 or RGB
  - [ ] **Acceptance:** Test with H.265 camera

- [ ] **Day 4:** MJPEG Decoding
  - [ ] Parse JPEG frames from RTP
  - [ ] Decode using libjpeg-turbo or FFmpeg
  - [ ] **Acceptance:** Test with MJPEG camera

- [ ] **Day 5:** Audio Codec Support
  - [ ] AAC decoding (already in code, needs fixing)
  - [ ] G.711 (PCMU/PCMA) decoding
  - [ ] Opus decoding (optional)
  - [ ] **Acceptance:** Audio plays in sync with video

**Files to Modify:**
- `native/video-processing/src/rtsp_client.cpp`
- `native/video-processing/include/rtsp_client.h`

**Acceptance:**
```bash
# Test with 3 camera models
./scripts/test-rtsp-cameras.sh --cameras hikvision,dahua,honeywell
# All must decode successfully
```

---

### Subtask P0-1.4: Integration Testing
**Priority:** P0 - CRITICAL  
**Estimated:** 1 week  
**Status:** ⚠️ Not Started

**Tasks:**

- [ ] **Day 1:** Test Environment Setup
  - [ ] Acquire 3+ camera models (Hikvision, Dahua, Axis)
  - [ ] Setup isolated test network
  - [ ] Configure network monitoring (Wireshark)
  - [ ] **Acceptance:** Network ready

- [ ] **Day 2-3:** Integration Test Suite
  - [ ] Connection stability tests
  - [ ] Codec fallback tests
  - [ ] Reconnection tests
  - [ ] **Acceptance:** 20+ test cases

- [ ] **Day 4:** Soak Testing
  - [ ] 24-hour continuous streaming
  - [ ] Monitor memory usage
  - [ ] Monitor CPU usage
  - [ ] **Acceptance:** No crashes, < 100MB memory

- [ ] **Day 5:** Performance Benchmarking
  - [ ] Measure latency
  - [ ] Measure frame rate
  - [ ] Measure CPU usage
  - [ ] **Acceptance:** Meet target metrics

**Files to Create:**
- `core/network/src/nativeTest/kotlin/RtspClientIntegrationTest.kt`
- `scripts/test-rtsp-cameras.sh`
- `scripts/rtsp-benchmark.sh`

**Acceptance:**
```kotlin
@Test
fun testH264Streaming() = runTest {
    val rtspClient = RtspClient(createConfig("rtsp://test-camera/h264"))
    rtspClient.connect()
    assertEquals(RtspClientStatus.CONNECTED, rtspClient.getStatus().value)
    
    val frame = rtspClient.getVideoFrames().first()
    assertTrue(frame.width > 0)
    assertTrue(frame.height > 0)
}
```

---

## 🔧 Technical Details

### Current Code Structure

```
native/video-processing/
├── src/
│   ├── rtsp_client.cpp          # 85% complete
│   ├── rtsp_client.h            # Header file
│   ├── audio_decoder.cpp        # Disabled (FFmpeg 8.0 issues)
│   └── audio_decoder.h          # Header file
├── include/
│   └── rtsp_client.h            # FFI declarations
└── CMakeLists.txt               # Build config

core/network/
├── src/commonMain/kotlin/
│   └── RtspClient.kt            # 100% wrapper
├── src/nativeMain/kotlin/
│   └── NativeRtspClient.kt      # 40% FFI
└── src/nativeTest/kotlin/
    └── RtspClientFfmpegDecodingTest.kt  # Tests (ignored)
```

### Key Challenges

1. **FFmpeg 8.0 API Changes:**
   - `ch_layout` replaces `channels`
   - New `swr_alloc_set_opts2` API
   - Deprecated `avcodec_decode_*` functions

2. **FFI Memory Management:**
   - Native buffers must not be freed while Kotlin holds reference
   - Frame data must be copied or managed with Foreign Memory API

3. **Thread Safety:**
   - RTP thread calls callbacks
   - Kotlin coroutines run on different threads
   - Need proper synchronization

### Dependencies

- **FFmpeg:** 8.0+ (latest stable)
- **OpenCV:** Optional (for additional processing)
- **Kotlin/Native:** 2.0.21
- **CMake:** 3.15+

---

## 🚨 Risks & Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| FFmpeg API incompatibility | Medium | High | Fallback to H.264 parser only |
| Memory leaks | Medium | High | Valgrind testing, leak detection |
| Performance issues | High | Medium | Early benchmarking, profiling |
| Camera compatibility | High | Medium | Test with multiple camera models |

### Contingency Plan

**If FFmpeg integration fails:**
1. Use HLS streaming as fallback (already implemented)
2. Implement server-side transcoding
3. Limit to cameras with HLS support only

---

## 📊 Progress Tracking

### Week 1
- [x] Task plan created
- [ ] FFmpeg API audit complete
- [ ] Audio decoder fixes
- [ ] CMake build working

### Week 2
- [ ] JNI bridge for video frames
- [ ] Callback mechanism
- [ ] Memory management
- [ ] Basic FFI tests

### Week 3
- [ ] H.264 decoding
- [ ] H.265 decoding
- [ ] MJPEG decoding
- [ ] Audio decoding

### Week 4
- [ ] Integration tests
- [ ] Soak testing
- [ ] Performance tuning
- [ ] Documentation

---

## 📚 Resources

### Documentation
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [Kotlin/Native FFI](https://kotlinlang.org/docs/native-objc-interop.html)
- [RTSP Protocol RFC 2326](https://www.ietf.org/rfc/rfc2326.txt)
- [H.264 Specification](https://www.itu.int/rec/T-REC-H.264)

### Test Cameras
- Hikvision DS-2CD2342WD-I (H.264)
- Dahua IPC-HFW2431S (H.264/H.265)
- Axis M1065-L (H.264/MJPEG)

---

## ✅ Acceptance Checklist

- [ ] FFmpeg 8.0 compiles without warnings
- [ ] Audio decoder works with AAC
- [ ] Audio decoder works with G.711
- [ ] Video decoder works with H.264
- [ ] Video decoder works with H.265
- [ ] Video decoder works with MJPEG
- [ ] FFI callbacks work correctly
- [ ] No memory leaks (24h soak test)
- [ ] Connection stability > 95%
- [ ] Latency < 3 seconds
- [ ] Frame rate 20-25 FPS
- [ ] Integration tests passing
- [ ] Documentation complete

---

**Created:** 27 April 2026  
**Owner:** Tech Lead  
**Next Review:** Daily standup  
**Blocker:** Yes - blocks entire MVP
