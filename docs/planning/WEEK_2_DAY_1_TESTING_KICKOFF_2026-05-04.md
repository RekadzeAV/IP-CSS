# 🎯 IP-CSS Week 2 - Integration Testing Kickoff

**Week:** 2 (May 4-10, 2026)  
**Day:** 1 (May 4, 2026)  
**Owner:** Tech Lead  
**Status:** 🟡 **STARTING INTEGRATION TESTING**  
**Target Progress:** 70% → 75%

---

## 📊 Current Status

**Previous Day (Day 1):** ✅ Complete
- 15 documents created (12,000+ lines)
- 64 tests created
- Build infrastructure complete
- FFI implementation 100%
- Codec support 100%

**Current Progress:** P0-1 at 70%  
**Today's Target:** 75%

---

## 🎯 Today's Goals (Day 1 - May 4)

### Primary Objectives

1. ✅ Compile native library for current platform
2. ✅ Run all 64 unit tests
3. ✅ Fix any compilation/test errors
4. ✅ Verify FFmpeg 8.0 API compatibility
5. ✅ Generate test coverage report

### Success Criteria

- [ ] Native library compiled successfully
- [ ] 60+ unit tests passing
- [ ] No critical compilation errors
- [ ] FFmpeg 8.0 API verified
- [ ] Test report generated

---

## ▶️ Execution Plan

### Step 1: Compile Native Library (2 hours)

```bash
cd native/video-processing

# Option A: Use build script (recommended)
./build.sh

# Option B: Use CMake presets
mkdir build && cd build
cmake --preset=linux-x64  # or macos-intel, windows-x64
cmake --build --preset=linux-x64-release

# Option C: Manual build
mkdir build && cd build
cmake .. -DENABLE_FFMPEG=ON -DCMAKE_BUILD_TYPE=Release
make -j8
```

**Expected Output:**
```
libvideo_processing.so    # Linux
libvideo_processing.dylib # macOS
video_processing.dll      # Windows
```

**Verification:**
```bash
# Check library symbols
nm -D build/libvideo_processing.so | grep rtsp_client
# Should show: rtsp_client_create, rtsp_client_connect, etc.
```

---

### Step 2: Run C++ Unit Tests (1 hour)

```bash
cd native/video-processing/build

# Run audio decoder tests
ctest -R audio_decoder_test --verbose

# Run video decoder tests
ctest -R video_decoder_test --verbose

# Run all tests
ctest --output-on-failure
```

**Expected Results:**
```
Test project build
  Start 1: audio_decoder_test
1/2 Test #1: audio_decoder_test .........   Passed    X sec
  Start 2: video_decoder_test
2/2 Test #2: video_decoder_test .........   Passed    Y sec

100% tests passed, 0 tests failed out of 2
```

---

### Step 3: Run Kotlin JVM Tests (1 hour)

```bash
cd /workspace/IP-CSS

# Run NativeRtspClient tests
./gradlew :core:network:desktopTest --tests "*NativeRtspClientTest*"

# Run integration tests
./gradlew :core:network:desktopTest --tests "*RtspClientIntegrationTest*"

# Run all network tests
./gradlew :core:network:desktopTest
```

**Expected Results:**
```
> Task :core:network:desktopTest

NativeRtspClientTest > testCreateClient PASSED
NativeRtspClientTest > testDestroyClient PASSED
...
16 tests passed

RtspClientIntegrationTest > testClientCreation PASSED
RtspClientIntegrationTest > testStatusFlow PASSED
...
15 tests passed

BUILD SUCCESSFUL
```

---

### Step 4: Verify FFmpeg 8.0 API (30 minutes)

```bash
# Check FFmpeg version
ffmpeg -version

# Verify required libraries
ldd native/video-processing/build/libvideo_processing.so | grep av

# Expected output:
# libavformat.so.60 => ...
# libavcodec.so.60 => ...
# libavutil.so.58 => ...
# libswscale.so.7 => ...
# libswresample.so.4 => ...
```

**API Verification Checklist:**
- [ ] AVCodecContext.ch_layout (FFmpeg 8.0)
- [ ] swr_alloc_set_opts2 (FFmpeg 8.0)
- [ ] avcodec_send_packet/receive_frame pattern
- [ ] Proper resource cleanup

---

### Step 5: Generate Test Report (30 minutes)

```bash
# Run master test runner
./scripts/run-all-tests.sh --verbose

# Generate coverage report (if lcov available)
lcov --capture --directory native/video-processing/build --output-file coverage.info
genhtml coverage.info --output-directory coverage_report
```

**Test Report Template:**

```markdown
# Test Results - May 4, 2026

## Audio Decoder Tests
- Total: 16
- Passed: 16
- Failed: 0
- Status: ✅ PASS

## Video Decoder Tests
- Total: 17
- Passed: 17
- Failed: 0
- Status: ✅ PASS

## NativeRtspClient Tests
- Total: 16
- Passed: 16
- Failed: 0
- Status: ✅ PASS

## Integration Tests
- Total: 15
- Passed: 15
- Failed: 0
- Status: ✅ PASS

## Overall
- Total: 64
- Passed: 64
- Failed: 0
- Success Rate: 100%
```

---

## 🔧 Troubleshooting Guide

### Issue: FFmpeg Not Found

```bash
# Ubuntu/Debian
sudo apt-get install libavformat-dev libavcodec-dev libswscale-dev libswresample-dev

# macOS
brew install ffmpeg

# Verify installation
pkg-config --modversion libavformat
```

### Issue: Build Fails on Windows

```powershell
# Install MinGW
choco install mingw

# Set environment variables
$env:PATH = "C:\msys64\mingw64\bin;" + $env:PATH

# Rebuild
cd native/video-processing
.\build.ps1
```

### Issue: Test Segmentation Fault

```bash
# Run with debugger
gdb --args ./test/audio_decoder_test

# Or use valgrind for memory analysis
valgrind --leak-check=full ./test/audio_decoder_test
```

### Issue: JNI Library Not Found

```bash
# Verify library location
ls -la native/video-processing/build/*.so

# Set library path
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$(pwd)/native/video-processing/build

# Or copy to JVM library path
cp native/video-processing/build/libvideo_processing.so /usr/lib/
```

---

## 📊 Metrics to Collect

### Build Metrics
- Build time
- Library size
- Number of symbols exported

### Test Metrics
- Total tests run
- Tests passed/failed
- Test execution time
- Code coverage percentage

### Performance Metrics
- FFmpeg version
- CPU usage during tests
- Memory usage during tests

---

## 📝 Daily Log Template

**Date:** May 4, 2026  
**Start Time:** 09:00  
**End Time:** 17:00

| Time | Activity | Status | Notes |
|------|----------|--------|-------|
| 09:00-11:00 | Compile native library | ⏳ | |
| 11:00-12:00 | C++ unit tests | ⏳ | |
| 12:00-13:00 | Lunch | - | |
| 13:00-14:00 | JVM tests | ⏳ | |
| 14:00-15:00 | FFmpeg verification | ⏳ | |
| 15:00-16:00 | Test report generation | ⏳ | |
| 16:00-17:00 | Bug fixes (if needed) | ⏳ | |

---

## ✅ Today's Checklist

### Build
- [ ] Native library compiled
- [ ] Library symbols verified
- [ ] No compilation warnings
- [ ] Library size acceptable (< 5MB)

### Tests
- [ ] Audio decoder tests: 16/16 pass
- [ ] Video decoder tests: 17/17 pass
- [ ] NativeRtspClient tests: 16/16 pass
- [ ] Integration tests: 15/15 pass
- [ ] All 64 tests pass

### Verification
- [ ] FFmpeg 8.0 API confirmed
- [ ] No memory leaks detected
- [ ] No segmentation faults
- [ ] All platforms build successfully

### Documentation
- [ ] Test results documented
- [ ] Build logs saved
- [ ] Issues logged
- [ ] Progress updated

---

## 🚀 Commands Quick Reference

```bash
# Full test suite
./scripts/run-all-tests.sh

# Specific test suite
./gradlew :core:network:desktopTest --tests "*NativeRtspClientTest*"

# C++ tests only
cd native/video-processing/build && ctest

# Build only
./build.sh

# Clean build
./build.sh --clean

# Verbose output
./scripts/run-all-tests.sh --verbose

# Platform-specific
./scripts/run-all-tests.sh --platform linux
```

---

## 📞 Escalation Points

**Critical Issues:**
- Build failures: Check FFmpeg installation, CMake configuration
- Test failures: Check logs, run with debugger
- Platform issues: Verify toolchain, dependencies

**Blockers:**
- FFmpeg version incompatibility
- Missing dependencies
- Platform-specific issues

**Contact:**
- Tech Lead: [Contact]
- DevOps: [Contact]
- QA Team: [Contact]

---

**Plan Created:** May 4, 2026  
**Target Completion:** 17:00  
**Expected Progress:** 70% → 75%
