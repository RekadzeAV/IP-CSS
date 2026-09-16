# 🧪 IP-CSS Testing Guide

**Comprehensive testing documentation for Phase 1 RTSP Client**

**Date:** 27 April 2026  
**Version:** 1.0.0

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Test Infrastructure](#test-infrastructure)
3. [Running Tests](#running-tests)
4. [Test Suites](#test-suites)
5. [Integration Testing](#integration-testing)
6. [Performance Testing](#performance-testing)
7. [Soak Testing](#soak-testing)
8. [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

This guide covers all aspects of testing the IP-CSS RTSP Client implementation.

### Test Coverage

| Component | Unit Tests | Integration Tests | Total |
|-----------|-----------|-------------------|-------|
| Audio Decoder | 16 | 0 | 16 |
| Video Decoder | 17 | 0 | 17 |
| RTSP Client | 16 | 15 | 31 |
| **Total** | **49** | **15** | **64** |

### Test Goals

- ✅ Verify codec functionality
- ✅ Test error handling
- ✅ Validate memory management
- ✅ Ensure cross-platform compatibility
- ⏳ Real camera integration
- ⏳ Performance validation

---

## 🔧 Test Infrastructure

### Test Scripts

All test scripts are located in `scripts/` directory:

```bash
scripts/
├── run-all-tests.sh           # Master test runner
├── test-rtsp-cameras.sh       # Camera integration tests
├── benchmark-stream.sh        # Performance benchmarking
└── soak-test.sh               # Long-duration stability tests
```

### Test Directories

```
native/video-processing/
├── test/
│   ├── audio_decoder_test.cpp    # Audio decoder unit tests
│   └── video_decoder_test.cpp    # Video decoder unit tests

core/network/
├── src/jvmTest/kotlin/
│   ├── NativeRtspClientTest.kt   # Client unit tests
│   └── RtspClientIntegrationTest.kt  # Integration tests
```

---

## ▶️ Running Tests

### Quick Start

```bash
# Run all tests
./scripts/run-all-tests.sh

# Run with verbose output
./scripts/run-all-tests.sh --verbose

# Run for specific platform
./scripts/run-all-tests.sh --platform linux
```

### Native Library Tests

#### Build with Tests

```bash
cd native/video-processing
mkdir build && cd build
cmake .. -DENABLE_FFMPEG=ON -DBUILD_TESTS=ON
cmake --build .
```

#### Run C++ Unit Tests

```bash
# Audio decoder tests
./test/audio_decoder_test

# Video decoder tests
./test/video_decoder_test

# All C++ tests
ctest
```

#### Run Specific Test

```bash
# GTest pattern matching
./test/audio_decoder_test --gtest_filter="*AAC*"
./test/video_decoder_test --gtest_filter="*H264*"
```

### Kotlin JVM Tests

```bash
# All network tests
./gradlew :core:network:desktopTest

# Specific test class
./gradlew :core:network:desktopTest --tests "*NativeRtspClientTest*"

# Specific test method
./gradlew :core:network:desktopTest --tests "*NativeRtspClientTest.testCreateClient*"
```

### Android Tests

```bash
# Run on connected device
./gradlew :core:network:connectedAndroidTest

# Run on emulator
./gradlew :core:network:testDebugUnitTest
```

---

## 📚 Test Suites

### 1. Audio Decoder Tests

**File:** `native/video-processing/test/audio_decoder_test.cpp`  
**Count:** 16 tests

#### Test Categories

**AAC Decoder (3 tests):**
- `TestAACDecoderInit` - Initialize AAC decoder
- `TestAACDecoderExtradata` - Handle AAC extradata
- `TestAACDecoderDecoding` - Decode AAC packets

**G.711 Decoder (3 tests):**
- `TestG711PCMU` - G.711 μ-law decoding
- `TestG711PCMA` - G.711 A-law decoding
- `TestG711DecoderInit` - G.711 initialization

**Audio Resampling (3 tests):**
- `TestResamplerInit` - Initialize resampler
- `TestResampler48to441` - Resample 48kHz to 44.1kHz
- `TestResamplerMultiChannel` - Multi-channel resampling

**Error Handling (4 tests):**
- `TestInvalidConfig` - Invalid configuration handling
- `TestNullData` - Null data handling
- `TestInvalidSamples` - Invalid sample count
- `TestDecoderLifecycle` - Proper resource cleanup

**Memory Management (3 tests):**
- `TestMemoryLeaks` - Check for memory leaks
- `TestBufferManagement` - Buffer allocation/free
- `TestDecoderDestroy` - Decoder destruction

---

### 2. Video Decoder Tests

**File:** `native/video-processing/test/video_decoder_test.cpp`  
**Count:** 17 tests

#### Test Categories

**Decoder Creation (3 tests):**
- `CreateH264Decoder` - Create H.264 decoder
- `CreateH265Decoder` - Create H.265 decoder
- `CreateMJPEGDecoder` - Create MJPEG decoder

**Decoder Lifecycle (2 tests):**
- `DestroyDecoder` - Proper cleanup
- `MultipleDecodings` - Multiple decode cycles

**Callback Testing (1 test):**
- `SetCallback` - Frame callback mechanism

**NAL Unit Processing (2 tests):**
- `NALUnitExtraction` - Extract NAL units
- `H264SPSPPS` - SPS/PPS handling

**Edge Cases (6 tests):**
- `DecodeInvalidData` - Invalid data handling
- `DecodeEmptyData` - Empty data handling
- `ReleaseFrameNull` - Null frame release
- `GetInfoNullParams` - Null parameter handling
- `LargeData` - Large data handling
- `Timestamps` - Timestamp handling

**Performance (1 test):**
- `PerformanceTest` - Decode performance

**MJPEG (1 test):**
- `MJPEGDecode` - MJPEG decoding

---

### 3. NativeRtspClient Tests

**File:** `core/network/src/jvmTest/kotlin/.../NativeRtspClientTest.kt`  
**Count:** 16 tests

#### Test Categories

**Client Lifecycle (3 tests):**
- `testCreateClient` - Create client instance
- `testDestroyClient` - Destroy client properly
- `testMultipleClients` - Multiple client instances

**Status Management (1 test):**
- `testStatusTransitions` - State machine transitions

**Stream Enumeration (3 tests):**
- `testGetStreams` - Enumerate available streams
- `testGetStreamById` - Get specific stream
- `testStreamInfo` - Stream metadata

**Callback Registration (2 tests):**
- `testSetFrameCallback` - Set frame callback
- `testSetStatusCallback` - Set status callback

**Connection Testing (1 test):**
- `testConnectivity` - Connection status

**Error Handling (4 tests):**
- `testConnectionFailure` - Handle connection failure
- `testNullCallbacks` - Null callback handling
- `testInvalidURL` - Invalid URL handling
- `testTimeoutHandling` - Timeout handling

**Library Loading (1 test):**
- `testLibraryLoaded` - Verify native library loaded

---

### 4. Integration Tests

**File:** `core/network/src/jvmTest/kotlin/.../RtspClientIntegrationTest.kt`  
**Count:** 15 tests

#### Test Categories

**Client Creation (1 test):**
- `testClientCreation` - Create and configure client

**Status Flow (2 tests):**
- `testStatusFlow` - DISCONNECTED → CONNECTED → STREAMING
- `testStatusFlowWithFailure` - Failure recovery flow

**Connection Testing (3 tests):**
- `testConnectWithCredentials` - Connect with auth
- `testConnectWithoutCredentials` - Connect without auth
- `testReconnect` - Reconnection after disconnect

**Disconnection (1 test):**
- `testDisconnect` - Clean disconnection

**Stream Access (2 tests):**
- `testStreamAccess` - Access streams after connect
- `testStreamUpdate` - Stream list updates

**Frame Flows (2 tests):**
- `testFrameFlow` - Frame delivery verification
- `testFrameTimestamps` - Frame timestamp accuracy

**Callback Testing (1 test):**
- `testCallbackInvocations` - Callback invocation count

**Diagnostics (1 test):**
- `testDiagnostics` - Diagnostics collection

**Reconnect (1 test):**
- `testReconnectWithFailure` - Reconnect after failure

**Codec Detection (1 test):**
- `testCodecDetection` - Auto-detect codec

---

## 🎥 Integration Testing

### Test Camera Setup

#### Required Cameras

1. **Hikvision DS-2CD2342WD-I**
   - RTSP: `rtsp://admin:pass@192.168.1.101:554/Streaming/Channels/101`
   - Codec: H.264
   - Audio: G.711 PCMU

2. **Dahua IPC-HFW2431S**
   - RTSP: `rtsp://admin:pass@192.168.1.102:554/cam/realmonitor?channel=1&substream=0`
   - Codec: H.264/H.265
   - Audio: G.711 PCMA

3. **Axis M1065-L**
   - RTSP: `rtsp://admin:pass@192.168.1.103:554/axis-media/media.amp?videocodec=h264`
   - Codec: H.264/MJPEG
   - Audio: None

### Running Camera Tests

```bash
# Test single camera
./scripts/test-rtsp-cameras.sh --camera hikvision --duration 60

# Test all cameras
./scripts/test-rtsp-cameras.sh --all --duration 60

# Test with specific codec
./scripts/test-rtsp-cameras.sh --camera dahua --codec h265 --duration 60

# Verbose output
./scripts/test-rtsp-cameras.sh --camera hikvision --verbose
```

### Expected Results

| Camera | Codec | Connection Time | Frame Rate | Status |
|--------|-------|----------------|------------|--------|
| Hikvision | H.264 | < 5s | 25 FPS | ✅ |
| Dahua | H.264 | < 5s | 20 FPS | ✅ |
| Dahua | H.265 | < 5s | 20 FPS | ✅ |
| Axis | H.264 | < 5s | 30 FPS | ✅ |
| Axis | MJPEG | < 5s | 30 FPS | ✅ |

---

## 📊 Performance Testing

### Benchmark Script

```bash
./scripts/benchmark-stream.sh \
  --camera hikvision \
  --duration 60 \
  --resolution 1920x1080 \
  --output build/benchmark.json
```

### Metrics Collected

| Metric | Target | Measurement |
|--------|--------|-------------|
| Latency | < 3s | RTT from capture to display |
| Frame Rate | 20-25 FPS | Frames decoded per second |
| CPU Usage | < 30% | Process CPU utilization |
| Memory | < 100MB | Resident set size |
| Frame Drops | < 1% | Dropped frames / total frames |

### Performance Report

Results are saved to `build/benchmark.json`:

```json
{
  "timestamp": 1714234567,
  "camera": "hikvision",
  "codec": "h264",
  "resolution": "1920x1080",
  "duration": 60,
  "metrics": {
    "avgFrameRate": 24.5,
    "minLatency": 1200,
    "maxLatency": 1800,
    "avgLatency": 1450,
    "cpuUsage": 22.3,
    "memoryUsage": 78.5,
    "frameDrops": 2,
    "totalFrames": 1470
  }
}
```

---

## 🔋 Soak Testing

### 24-Hour Stability Test

```bash
./scripts/soak-test.sh \
  --camera hikvision \
  --duration 86400 \
  --monitor-interval 60 \
  --output build/soak-test.json
```

### Monitoring

Every 60 seconds, the test collects:

- Memory usage
- CPU usage
- Frame count
- Error count
- Reconnection events
- Network statistics

### Success Criteria

- No crashes
- No memory leaks (< 5% growth over 24h)
- < 1% frame drops
- < 5 reconnections
- CPU usage < 30%

---

## 🐛 Troubleshooting

### Test Failures

**Problem:** Tests fail to compile

**Solution:**
```bash
# Clean build
./build.sh --clean

# Verify FFmpeg
ffmpeg -version

# Check CMake
cmake --version
```

**Problem:** Native library not found

**Solution:**
```bash
# Rebuild library
cd native/video-processing
./build.sh

# Verify library exists
ls -la build/*.so  # Linux
ls -la build/*.dylib  # macOS
ls -la build/*.dll  # Windows
```

**Problem:** Camera connection timeout

**Solution:**
```bash
# Check network connectivity
ping 192.168.1.101

# Verify RTSP URL
ffprobe rtsp://192.168.1.101:554/stream

# Check firewall
sudo ufw status
```

### Memory Leaks

**Problem:** Memory grows during soak test

**Solution:**
```bash
# Run with Valgrind
valgrind --leak-check=full ./test/video_decoder_test

# Check for leaks
grep "definitely lost" valgrind.log
```

---

## 📈 Test Coverage Report

### Current Coverage

| File | Statements | Branches | Functions |
|------|-----------|----------|-----------|
| audio_decoder.cpp | 78% | 72% | 85% |
| video_decoder.cpp | 82% | 75% | 88% |
| rtsp_client.cpp | 75% | 68% | 80% |
| **Total** | **78%** | **72%** | **84%** |

### Coverage Goals

- Statements: > 80%
- Branches: > 75%
- Functions: > 85%

---

## ✅ Test Checklist

### Pre-Release Checklist

- [ ] All unit tests pass (49/49)
- [ ] All integration tests pass (15/15)
- [ ] Performance benchmarks meet targets
- [ ] 24h soak test completed
- [ ] 3 camera models tested
- [ ] No memory leaks detected
- [ ] Code coverage > 80%
- [ ] All platforms tested

### Daily Testing

- [ ] Run all unit tests
- [ ] Test with at least 1 camera
- [ ] Check memory usage
- [ ] Review test logs

---

**Guide Version:** 1.0.0  
**Last Updated:** 27 April 2026  
**Maintainer:** IP-CSS Team
