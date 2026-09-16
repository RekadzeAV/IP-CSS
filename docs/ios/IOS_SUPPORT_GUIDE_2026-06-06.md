# 📱 IP-CSS iOS Support Guide

**Purpose:** Build and integrate native video processing library for iOS  
**Last Updated:** June 6, 2026  
**Status:** 🟡 **IN PREPARATION**

---

## 📋 Overview

This guide covers iOS platform support for the IP-CSS video processing library.

### Supported iOS Architectures

1. **iOS Arm64** (Physical devices)
2. **iOS x64** (Simulator on Intel Mac)
3. **iOS Simulator Arm64** (Simulator on Apple Silicon Mac)

### Minimum Requirements

- **iOS Version:** 13.0+
- **Xcode:** 14.0+
- **macOS:** 12.0+ (Monterey)
- **CMake:** 3.15+
- **FFmpeg:** 8.0+

---

## 🛠️ Prerequisites

### Required Software

1. **Xcode** (from App Store)
   ```bash
   xcode-select --install
   ```

2. **CMake** (via Homebrew)
   ```bash
   brew install cmake
   ```

3. **FFmpeg for iOS** (pre-built or build from source)

4. **Git**
   ```bash
   xcode-select --install
   ```

---

## 🔧 Build Instructions

### Option 1: Using CMake Presets (Recommended)

**Configure and build for iOS Arm64:**
```bash
cd native/video-processing
cmake --preset ios-arm64
cmake --build --preset ios-arm64
```

**For iOS Simulator x64:**
```bash
cmake --preset ios-x64
cmake --build --preset ios-x64
```

**For iOS Simulator Arm64:**
```bash
cmake --preset ios-simulator-arm64
cmake --build --preset ios-simulator-arm64
```

---

### Option 2: Using Build Scripts

**Bash (macOS/Linux):**
```bash
# iOS Device (Arm64)
./build-ios.sh arm64 Release

# iOS Simulator (x64)
./build-ios.sh x64 Release

# iOS Simulator (Arm64)
./build-ios.sh simulator-arm64 Release
```

**PowerShell (macOS/Windows):**
```powershell
# iOS Device
.\build-ios.ps1 -Arch arm64 -BuildType Release

# iOS Simulator
.\build-ios.ps1 -Arch x64 -BuildType Release
```

---

### Option 3: Manual CMake Commands

**iOS Arm64 (Device):**
```bash
mkdir -p build/ios/arm64 && cd build/ios/arm64

cmake ../.. \
  -G Xcode \
  -DCMAKE_SYSTEM_NAME=iOS \
  -DCMAKE_OSX_DEPLOYMENT_TARGET=13.0 \
  -DCMAKE_OSX_ARCHITECTURES=arm64 \
  -DCMAKE_OSX_SYSROOT=/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS.sdk \
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_SHARED_LIBS=ON \
  -DENABLE_H264=ON \
  -DENABLE_H265=ON \
  -DENABLE_MJPEG=ON \
  -DENABLE_AAC=ON \
  -DENABLE_G711=ON

cmake --build . --config Release -j$(sysctl -n hw.ncpu)
cmake --install . --config Release
```

**iOS Simulator x64:**
```bash
mkdir -p build/ios/x64 && cd build/ios/x64

cmake ../.. \
  -G Xcode \
  -DCMAKE_SYSTEM_NAME=iOS \
  -DCMAKE_OSX_DEPLOYMENT_TARGET=13.0 \
  -DCMAKE_OSX_ARCHITECTURES=x86_64 \
  -DCMAKE_OSX_SYSROOT=/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator.sdk \
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_SHARED_LIBS=ON \
  -DENABLE_H264=ON \
  -DENABLE_H265=ON \
  -DENABLE_MJPEG=ON \
  -DENABLE_AAC=ON \
  -DENABLE_G711=ON

cmake --build . --config Release -j$(sysctl -n hw.ncpu)
cmake --install . --config Release
```

---

## 📦 Output

### Library Location

After build, the library will be located at:

- **iOS Arm64:** `native/video-processing/lib/ios/arm64/libvideo_processing.dylib`
- **iOS x64:** `native/video-processing/lib/ios/x64/libvideo_processing.dylib`
- **iOS Simulator Arm64:** `native/video-processing/lib/ios/simulator-arm64/libvideo_processing.dylib`

### Library Contents

```
lib/ios/arm64/
├── libvideo_processing.dylib  # Dynamic library
├── include/
│   ├── video_decoder.h
│   ├── audio_decoder.h
│   └── rtsp_client.h
└── cmake/
    └── video_processingConfig.cmake
```

---

## 🔗 Kotlin/Native Integration

### Build Configuration

iOS targets are already configured in `core/network/build.gradle.kts`:

```kotlin
kotlin {
    iosArm64 {
        compilations.getByName("main") {
            cinterops {
                val rtspClient by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                    packageName("com.company.ipcamera.core.network.rtsp")
                    compilerOpts("-I${project.rootDir}/native/video-processing/include")
                    includeDirs("${project.rootDir}/native/video-processing/include")
                    linkerOpts("-L${project.rootDir}/native/video-processing/lib/ios/arm64 -lvideo_processing")
                }
            }
        }
    }
    
    iosX64 {
        // Similar configuration for x64
    }
    
    iosSimulatorArm64 {
        // Similar configuration for simulator arm64
    }
}
```

### Source Sets

```kotlin
sourceSets {
    val iosMain by creating {
        dependsOn(commonMain)
        dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
    
    val iosX64Main by getting {
        dependsOn(iosMain)
    }
    
    val iosArm64Main by getting {
        dependsOn(iosMain)
    }
    
    val iosSimulatorArm64Main by getting {
        dependsOn(iosMain)
    }
}
```

---

## 🧪 Testing on iOS

### iOS Simulator

1. **Build for simulator:**
   ```bash
   ./build-ios.sh x64 Release
   ```

2. **Run Gradle tasks:**
   ```bash
   ./gradlew :core:network:compileKotlinIosX64
   ./gradlew :core:network:iosX64Test
   ```

### Physical Device

1. **Build for device:**
   ```bash
   ./build-ios.sh arm64 Release
   ```

2. **Configure Xcode project** (if using native iOS app)
3. **Sign and deploy** to device

---

## 📋 Troubleshooting

### Issue 1: Xcode Not Found

**Symptoms:**
```
CMake Error: Generator does not support platform: Xcode
```

**Solution:**
```bash
# Install Xcode command line tools
xcode-select --install

# Verify Xcode path
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

---

### Issue 2: iOS SDK Not Found

**Symptoms:**
```
fatal error: 'Foundation/Foundation.h' file not found
```

**Solution:**
```bash
# Find iOS SDK
xcrun --sdk iphoneos --show-sdk-path

# Update CMAKE_OSX_SYSROOT accordingly
```

---

### Issue 3: Architecture Mismatch

**Symptoms:**
```
building for iOS Simulator, but linking in object file built for iOS
```

**Solution:**
- Ensure you're building for the correct architecture
- Use `x64` for Intel Mac simulator
- Use `simulator-arm64` for Apple Silicon Mac simulator
- Use `arm64` for physical devices

---

### Issue 4: FFmpeg Not Found

**Symptoms:**
```
fatal error: 'libavcodec/avcodec.h' file not found
```

**Solution:**
```bash
# Install FFmpeg for iOS
# Option 1: Use pre-built binaries
# Option 2: Build FFmpeg for iOS
cd ffmpeg
./configure --target-os=iphone --disable-asm --enable-cross-compile
make
make install
```

---

## 🎯 Build Automation

### Gradle Tasks

**Build all iOS targets:**
```bash
./gradlew :core:network:compileKotlinIosArm64
./gradlew :core:network:compileKotlinIosX64
./gradlew :core:network:compileKotlinIosSimulatorArm64
```

**Build and test:**
```bash
./gradlew :core:network:iosArm64Test
./gradlew :core:network:iosX64Test
./gradlew :core:network:iosSimulatorArm64Test
```

**Build XCFramework:**
```bash
./gradlew :core:network:linkXCFramework
```

---

## 📊 Build Performance

### Expected Build Times

| Target | Clean Build | Incremental |
|--------|-------------|-------------|
| iOS Arm64 | 15-20 min | 5-8 min |
| iOS x64 | 15-20 min | 5-8 min |
| iOS Simulator Arm64 | 15-20 min | 5-8 min |

### Optimization Tips

1. **Use incremental builds:**
   ```bash
   cmake --build . --config Release
   ```

2. **Parallel compilation:**
   ```bash
   cmake --build . --config Release -j$(sysctl -n hw.ncpu)
   ```

3. **Clean build only when necessary:**
   ```bash
   rm -rf build/ios && mkdir -p build/ios
   ```

---

## 🚀 Next Steps

### Week 2 Day 6 (June 11)

**Tasks:**
1. ✅ Build iOS libraries (completed in preparation)
2. ⏳ Test on iOS Simulator
3. ⏳ Test on Physical Device
4. ⏳ Create XCFramework
5. ⏳ Integrate with iOS app

**Deliverables:**
- iOS Arm64 library
- iOS x64 library
- iOS Simulator Arm64 library
- XCFramework for distribution
- Integration tests

---

## 📁 Files Created

1. `native/video-processing/CMakeIOSPresets.json` - CMake presets for iOS
2. `native/video-processing/build-ios.sh` - Bash build script
3. `native/video-processing/build-ios.ps1` - PowerShell build script
4. `docs/ios/IOS_SUPPORT_GUIDE_2026-06-06.md` - This guide

---

**Guide Created:** June 6, 2026  
**Status:** 🟡 **Preparation Complete - Ready for Testing**  
**Next:** Week 2 Day 6 - Actual iOS Testing
