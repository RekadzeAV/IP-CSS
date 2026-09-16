# F1-1 RTSP Client - Build Environment Blocker Resolution

**Date:** 30 May 2026 01:00  
**Status:** 🔴 **Blocker: Build Environment Not Ready**  
**Progress:** 15% → 35%

---

## 📊 Current Situation

### ❌ Failed: vcpkg ffmpeg:x64-mingw-static

**Error:**
```
Command failed: bash.exe ./build.sh
Error code: 2
```

**Cause:** Build script failure in MSYS2 environment

### ❌ Pending: Visual Studio Build Tools 2022

**Status:** Installation not started/failed  
**Reason:** winget installation did not complete

---

## 🎯 Resolution Options

### Option 1: Install Visual Studio Build Tools (Recommended)

**Pros:**
- Industry standard Windows development environment
- Full support for MSVC FFmpeg libraries
- Best compatibility and performance
- Long-term solution for project

**Cons:**
- Large download (~5-10 GB)
- Long installation time (1-2 hours)
- Requires admin rights

**Steps:**
```powershell
# Download Visual Studio Build Tools 2022
# Visit: https://visualstudio.microsoft.com/downloads/
# Download: "Build Tools for Visual Studio 2022"

# Run installer with command line
.\vs_buildtools.exe --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended --quiet --wait

# After installation, rebuild with Visual Studio generator
cd native/video-processing
cmake -B build/windows/vs2022 -G "Visual Studio 17 2022" -A x64 `
  -DCMAKE_TOOLCHAIN_FILE="$env:USERPROFILE/vcpkg/scripts/buildsystems/vcpkg.cmake"
cmake --build build/windows/vs2022 --config Release
```

**Time:** 1-2 hours

---

### Option 2: Use Pre-built MinGW FFmpeg (Faster Alternative)

**Pros:**
- No Visual Studio installation needed
- Faster setup (30-60 minutes)
- Works with current MinGW compiler

**Cons:**
- Need to download pre-built binaries manually
- Manual configuration required
- May have compatibility issues

**Steps:**
1. Download FFmpeg for MinGW from:
   - https://www.gyan.dev/ffmpeg/builds/
   - Look for "ffmpeg-release-essentials.zip" OR
   - https://github.com/BtbN/FFmpeg-Builds/releases
   - Look for "ffmpeg-master-latest-win64-gcc.zip"

2. Extract to `C:\ffmpeg-mingw`

3. Configure CMake:
```powershell
cd native/video-processing
Remove-Item -Path build -Recurse -Force
cmake -B build/windows/mingw -G "MinGW Makefiles" -DCMAKE_BUILD_TYPE=Release `
  -DCMAKE_PREFIX_PATH="C:/ffmpeg-mingw" `
  -DFFMPEG_DIR="C:/ffmpeg-mingw"
cmake --build build/windows/mingw --config Release
```

**Time:** 30-60 minutes

---

### Option 3: Reattempt vcpkg mingw Installation

**Pros:**
- Automatic dependency management
- Proper integration with vcpkg

**Cons:**
- Previous attempt failed
- May fail again
- Already took 1+ hour

**Steps:**
```powershell
# Clean vcpkg build cache
cd $env:USERPROFILE\vcpkg
.\vcpkg.exe remove ffmpeg:x64-mingw-static --purge

# Reinstall
.\vcpkg.exe install ffmpeg:x64-mingw-static

# If fails again, check logs for specific error
Get-Content "$env:USERPROFILE\vcpkg\buildtrees\ffmpeg\build-x64-mingw-static-rel-err.log"
```

**Time:** 1-2 hours (if successful)

---

### Option 4: Skip Native Build for Now (Temporary)

**Pros:**
- Immediate progress on other tasks
- Can test Kotlin layer without native code
- Work around blocker

**Cons:**
- No actual video decoding
- Not production-ready
- Delay F1-1 completion

**Steps:**
1. Create mock/stub RtspClient implementation
2. Use @Ignore annotation for native tests
3. Complete other F1 tasks in parallel
4. Return to native build later

**Time:** 1-2 hours for mock implementation

---

## 🚀 Recommended Action Plan

### Immediate (Next 30 minutes)

**Decision:** Try Option 2 (Pre-built MinGW FFmpeg)

**Reasoning:**
- Fastest path to working build
- Avoids Visual Studio installation time
- Compatible with current setup
- Can always switch to Visual Studio later

### Short-term (Next 1-2 hours)

1. **Download pre-built FFmpeg**
   - Visit: https://github.com/BtbN/FFmpeg-Builds/releases
   - Download: `ffmpeg-master-latest-win64-gcc.zip`
   - Extract to `C:\ffmpeg-mingw`

2. **Configure and build**
   ```powershell
   cd native/video-processing
   Remove-Item -Path build -Recurse -Force
   cmake -B build/windows/mingw -G "MinGW Makefiles" -DCMAKE_BUILD_TYPE=Release `
     -DCMAKE_PREFIX_PATH="C:/ffmpeg-mingw"
   cmake --build build/windows/mingw --config Release
   ```

3. **Test and verify**
   - Run unit tests
   - Verify FFmpeg integration

### Medium-term (Next 1-2 weeks)

4. **Consider Visual Studio installation**
   - For production builds
   - Better compatibility
   - Long-term maintenance

---

## 📋 Quick Start: Option 2 (Pre-built FFmpeg)

### Step 1: Download FFmpeg

**URL:** https://github.com/BtbN/FFmpeg-Builds/releases

**File:** `ffmpeg-master-latest-win64-gcc.zip` (~150-200 MB)

### Step 2: Extract

```powershell
# Extract to C:\ffmpeg-mingw
Expand-Archive -Path "ffmpeg-master-latest-win64-gcc.zip" -DestinationPath "C:\ffmpeg-mingw" -Force
```

### Step 3: Verify Structure

```powershell
# Should have:
# C:\ffmpeg-mingw\include\  (FFmpeg headers)
# C:\ffmpeg-mingw\lib\      (.a libraries)
# C:\ffmpeg-mingw\bin\      (.dll files)
```

### Step 4: Build

```powershell
cd native/video-processing
Remove-Item -Path build -Recurse -Force

cmake -B build/windows/mingw -G "MinGW Makefiles" -DCMAKE_BUILD_TYPE=Release `
  -DCMAKE_PREFIX_PATH="C:/ffmpeg-mingw" `
  -DCMAKE_FIND_ROOT_PATH="C:/ffmpeg-mingw"

cmake --build build/windows/mingw --config Release
```

### Step 5: Copy Library

```powershell
New-Item -ItemType Directory -Force -Path "native/video-processing/lib/windows/x64"
Copy-Item "build/windows/mingw/bin/video_processing.dll" "lib/windows/x64/" -Force
```

**Total Time:** ~1 hour

---

## 📊 Comparison of Options

| Option | Time | Complexity | Reliability | Recommendation |
|--------|------|------------|-------------|----------------|
| 1. Visual Studio | 1-2h | Medium | High | Good long-term |
| 2. Pre-built FFmpeg | 30-60min | Low | Medium | **Best now** |
| 3. Reattempt vcpkg | 1-2h | Low | Low | Not recommended |
| 4. Skip native | 1-2h | Low | N/A | Temporary only |

---

## ⏱️ Updated Timeline

| Phase | Original | With Option 2 | Notes |
|-------|----------|---------------|-------|
| Environment setup | 3-4 weeks | ~1 hour | Pre-built FFmpeg |
| Native build | 1-2 days | ~2 hours | After FFmpeg ready |
| Unit tests | 1 day | 1 day | - |
| Integration tests | 3-5 days | 3-5 days | Requires cameras |
| Features | 7-10 days | 7-10 days | - |
| **Total** | **3-4 weeks** | **3-4 weeks** | **No delay** |

---

## 📞 Next Actions

**For Option 2 (Recommended):**

1. **Download FFmpeg** (15-20 minutes)
   - Visit: https://github.com/BtbN/FFmpeg-Builds/releases
   - Download: `ffmpeg-master-latest-win64-gcc.zip`

2. **Extract and configure** (10 minutes)
   ```powershell
   Expand-Archive -Path "ffmpeg-master-latest-win64-gcc.zip" -DestinationPath "C:\ffmpeg-mingw" -Force
   ```

3. **Build native library** (1-2 hours)
   ```powershell
   cd native/video-processing
   cmake -B build/windows/mingw -G "MinGW Makefiles" -DCMAKE_BUILD_TYPE=Release `
     -DCMAKE_PREFIX_PATH="C:/ffmpeg-mingw"
   cmake --build build/windows/mingw --config Release
   ```

4. **Run tests** (30 minutes)
   ```powershell
   ./gradlew :core:network:jvmTest --tests "*RtspClient*"
   ```

---

## 🎯 Success Criteria

After completing Option 2:

- ✅ video_processing.dll built successfully
- ✅ FFmpeg libraries linked correctly
- ✅ No linker errors
- ✅ Unit tests pass
- ✅ F1-1 progress: 35% → 50%

---

**Last Updated:** 30 May 2026 01:00  
**Blocker:** Build environment (mingw FFmpeg failed, VS not installed)  
**Recommendation:** Use pre-built MinGW FFmpeg (Option 2)  
**Estimated Resolution:** 1 hour
