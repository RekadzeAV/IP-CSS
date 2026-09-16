# 🚀 IP-CSS Week 2 - Quick Start Guide

**Week:** 2 (May 4-10, 2026)  
**Goal:** Complete integration testing and reach 95%  
**Status:** 🟢 **READY TO START**

---

## 📋 Quick Reference

### Today's Target (Day 1 - May 4)
- Compile native library
- Run all 64 tests
- Fix any issues
- **Target Progress:** 70% → 75%

---

## ⚡ Quick Start Commands

### 1. Compile Native Library (2 hours)

**Linux/macOS:**
```bash
cd native/video-processing
./build.sh
```

**Windows:**
```powershell
cd native/video-processing
.\build.ps1
```

**Expected Output:**
```
✓ Library copied to: lib/linux/x64/libvideo_processing.so
✓ Build completed successfully
```

---

### 2. Run All Tests (1-2 hours)

**Master Test Runner:**
```bash
./scripts/run-all-tests.sh
```

**Verbose Mode:**
```bash
./scripts/run-all-tests.sh --verbose
```

**Platform-Specific:**
```bash
./scripts/run-all-tests.sh --platform linux
```

---

### 3. Individual Test Suites

**C++ Unit Tests:**
```bash
cd native/video-processing/build
ctest --output-on-failure
```

**Kotlin JVM Tests:**
```bash
./gradlew :core:network:desktopTest --tests "*NativeRtspClientTest*"
./gradlew :core:network:desktopTest --tests "*RtspClientIntegrationTest*"
```

---

### 4. Verify FFmpeg 8.0

```bash
# Check version
ffmpeg -version

# Check libraries
ldd native/video-processing/build/libvideo_processing.so | grep av
```

**Expected Output:**
```
libavformat.so.60 => ...
libavcodec.so.60 => ...
libavutil.so.58 => ...
libswscale.so.7 => ...
libswresample.so.4 => ...
```

---

## 📊 Test Expectations

### Audio Decoder Tests (16 tests)
```
✓ TestAACDecoderInit
✓ TestAACDecoderExtradata
✓ TestAACDecoderDecoding
✓ TestG711PCMU
✓ TestG711PCMA
✓ TestG711DecoderInit
✓ TestResamplerInit
✓ TestResampler48to441
✓ TestResamplerMultiChannel
✓ TestInvalidConfig
✓ TestNullData
✓ TestInvalidSamples
✓ TestDecoderLifecycle
✓ TestMemoryLeaks
✓ TestBufferManagement
✓ TestDecoderDestroy
```

### Video Decoder Tests (17 tests)
```
✓ CreateH264Decoder
✓ CreateH265Decoder
✓ CreateMJPEGDecoder
✓ DestroyDecoder
✓ MultipleDecodings
✓ SetCallback
✓ NALUnitExtraction
✓ H264SPSPPS
✓ DecodeInvalidData
✓ DecodeEmptyData
✓ ReleaseFrameNull
✓ GetInfoNullParams
✓ LargeData
✓ Timestamps
✓ PerformanceTest
✓ MJPEGDecode
```

### NativeRtspClient Tests (16 tests)
```
✓ testCreateClient
✓ testDestroyClient
✓ testMultipleClients
✓ testStatusTransitions
✓ testGetStreams
✓ testGetStreamById
✓ testStreamInfo
✓ testSetFrameCallback
✓ testSetStatusCallback
✓ testConnectivity
✓ testConnectionFailure
✓ testNullCallbacks
✓ testInvalidURL
✓ testTimeoutHandling
✓ testLibraryLoaded
```

### Integration Tests (15 tests)
```
✓ testClientCreation
✓ testStatusFlow
✓ testStatusFlowWithFailure
✓ testConnectWithCredentials
✓ testConnectWithoutCredentials
✓ testReconnect
✓ testDisconnect
✓ testStreamAccess
✓ testStreamUpdate
✓ testFrameFlow
✓ testFrameTimestamps
✓ testCallbackInvocations
✓ testDiagnostics
✓ testReconnectWithFailure
✓ testCodecDetection
```

**Total:** 64/64 tests expected to pass

---

## 🔧 Troubleshooting

### Build Fails - FFmpeg Not Found

**Ubuntu/Debian:**
```bash
sudo apt-get install -y libavformat-dev libavcodec-dev libswscale-dev libswresample-dev
```

**macOS:**
```bash
brew install ffmpeg
```

**Windows:**
```powershell
vcpkg install ffmpeg:x64-windows
```

---

### Test Segmentation Fault

```bash
# Run with debugger
gdb --args ./test/audio_decoder_test

# Or use valgrind
valgrind --leak-check=full ./test/audio_decoder_test
```

---

### JNI Library Not Found

**Linux:**
```bash
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$(pwd)/native/video-processing/build
```

**macOS:**
```bash
export DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:$(pwd)/native/video-processing/build
```

**Windows:**
```powershell
$env:PATH = "$(pwd)\native\video-processing\build;" + $env:PATH
```

---

## 📅 Week 2 Schedule

| Day | Date | Focus | Target | Status |
|-----|------|-------|--------|--------|
| **Day 1** | May 4 | Build & Tests | 75% | 🟡 STARTING |
| Day 2 | May 5 | Camera Setup | 75% | ⚪ PENDING |
| Day 3 | May 6 | Integration I | 80% | ⚪ PENDING |
| Day 4 | May 7 | Integration II | 85% | ⚪ PENDING |
| Day 5 | May 8 | Soak Test (24h) | 85% | ⚪ PENDING |
| Day 6 | May 9 | iOS Support | 90% | ⚪ PENDING |
| Day 7 | May 10 | Polish & Beta | 95% | ⚪ PENDING |

---

## 📁 Important Files

### Documentation
- `docs/planning/WEEK_2_INTEGRATION_TESTING_PLAN_2026-04-27.md` - Full plan
- `docs/planning/WEEK_2_DAY_1_TESTING_KICKOFF_2026-05-04.md` - Day 1 details
- `docs/testing/TESTING_GUIDE_2026-04-27.md` - Testing reference

### Scripts
- `scripts/run-all-tests.sh` - Master test runner
- `scripts/test-rtsp-cameras.sh` - Camera testing

### Build
- `native/video-processing/build.sh` - Linux/macOS build
- `native/video-processing/build.ps1` - Windows build

---

## ✅ Day 1 Checklist

**Morning (09:00-12:00):**
- [ ] Compile native library
- [ ] Verify library symbols
- [ ] Run audio decoder tests (16)
- [ ] Run video decoder tests (17)

**Afternoon (13:00-17:00):**
- [ ] Run Kotlin JVM tests (31)
- [ ] Verify FFmpeg 8.0 API
- [ ] Generate test report
- [ ] Fix any issues

**Evening (17:00-18:00):**
- [ ] Document results
- [ ] Update status report
- [ ] Prepare for Day 2

---

## 🎯 Success Criteria

### Day 1 (May 4)
- [ ] Native library compiled
- [ ] 64/64 tests pass
- [ ] No compilation errors
- [ ] FFmpeg 8.0 verified
- [ ] Test report generated

### Week 2 Target
- [ ] Real camera testing complete
- [ ] 24h soak test passed
- [ ] Performance targets met
- [ ] iOS support working
- [ ] 95% progress achieved

---

## 📞 Support & Contacts

**Documentation:**
- All docs in `docs/` directory
- Testing guide: `docs/testing/TESTING_GUIDE_2026-04-27.md`

**Scripts:**
- Test runner: `scripts/run-all-tests.sh`
- Camera testing: `scripts/test-rtsp-cameras.sh`

**Build:**
- Linux/macOS: `native/video-processing/build.sh`
- Windows: `native/video-processing/build.ps1`

**Team:**
- Tech Lead: [Contact]
- DevOps: [Contact]
- QA Team: [Contact]

---

## 🚨 Escalation

**Critical Issues:**
1. Build failures → Check FFmpeg installation
2. Test failures → Check logs, run with debugger
3. Platform issues → Verify toolchain

**Blockers:**
- FFmpeg version incompatibility
- Missing dependencies
- Platform-specific issues

**Contact Tech Lead if:**
- Build fails after 3 attempts
- More than 5 tests fail
- Critical bug discovered

---

**Quick Start Created:** April 27, 2026  
**Week 2 Start:** May 4, 2026  
**Status:** 🟢 READY TO START
