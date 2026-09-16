# IP-CSS Phase 1 - CI/CD Pipeline Configuration

**Purpose:** Automated build, test, and deployment pipeline  
**Status:** Ready for Week 2 Integration  
**Last Updated:** 27 April 2026

---

## 📋 Overview

This document describes the CI/CD pipeline configuration for IP-CSS Phase 1.

### Pipeline Stages

1. **Build** - Compile native library for all platforms
2. **Test** - Run unit and integration tests
3. **Coverage** - Generate code coverage reports
4. **Deploy** - Publish artifacts

---

## 🔄 GitHub Actions Workflow

### File: `.github/workflows/ci.yml`

```yaml
name: IP-CSS CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

env:
  CMAKE_VERSION: '3.22'
  FFMPEG_VERSION: '8.0'

jobs:
  build-linux:
    name: Build Linux x64
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Install Dependencies
        run: |
          sudo apt-get update
          sudo apt-get install -y \
            cmake \
            g++ \
            libavformat-dev \
            libavcodec-dev \
            libavutil-dev \
            libswscale-dev \
            libswresample-dev
      
      - name: Configure CMake
        run: |
          cd native/video-processing
          cmake -B build -DENABLE_FFMPEG=ON -DCMAKE_BUILD_TYPE=Release
      
      - name: Build
        run: |
          cd native/video-processing
          cmake --build build -j4
      
      - name: Upload Artifact
        uses: actions/upload-artifact@v3
        with:
          name: libvideo_processing-linux
          path: native/video-processing/build/libvideo_processing.so

  build-macos:
    name: Build macOS
    runs-on: macos-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Install Dependencies
        run: |
          brew install cmake ffmpeg
      
      - name: Configure CMake
        run: |
          cd native/video-processing
          cmake -B build -DENABLE_FFMPEG=ON -DCMAKE_BUILD_TYPE=Release
      
      - name: Build
        run: |
          cd native/video-processing
          cmake --build build -j4
      
      - name: Upload Artifact
        uses: actions/upload-artifact@v3
        with:
          name: libvideo_processing-macos
          path: native/video-processing/build/libvideo_processing.dylib

  build-windows:
    name: Build Windows x64
    runs-on: windows-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Install FFmpeg
        run: |
          vcpkg install ffmpeg:x64-windows
      
      - name: Configure CMake
        run: |
          cd native/video-processing
          cmake -B build -G "Visual Studio 17 2022" -A x64 -DENABLE_FFMPEG=ON
      
      - name: Build
        run: |
          cd native/video-processing
          cmake --build build --config Release
      
      - name: Upload Artifact
        uses: actions/upload-artifact@v3
        with:
          name: libvideo_processing-windows
          path: native/video-processing/build/Release/video_processing.dll

  test-native:
    name: Native Tests
    runs-on: ubuntu-latest
    needs: build-linux
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Download Artifact
        uses: actions/download-artifact@v3
        with:
          name: libvideo_processing-linux
          path: native/video-processing/build/
      
      - name: Install Test Dependencies
        run: |
          sudo apt-get install -y \
            libgtest-dev \
            lcov
      
      - name: Configure with Tests
        run: |
          cd native/video-processing
          cmake -B build -DENABLE_FFMPEG=ON -DBUILD_TESTS=ON
      
      - name: Build Tests
        run: |
          cd native/video-processing
          cmake --build build --target audio_decoder_test video_decoder_test
      
      - name: Run Tests
        run: |
          cd native/video-processing/build
          ctest --output-on-failure
      
      - name: Generate Coverage
        run: |
          cd native/video-processing
          lcov --capture --directory build --output-file coverage.info
          lcov --remove coverage.info '/usr/*' --output-file coverage.info
      
      - name: Upload Coverage
        uses: actions/upload-artifact@v3
        with:
          name: coverage-report
          path: native/video-processing/coverage.info

  test-kotlin:
    name: Kotlin JVM Tests
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'
      
      - name: Setup Gradle
        uses: gradle/gradle-build-action@v2
      
      - name: Run Tests
        run: |
          ./gradlew :core:network:desktopTest --tests "*NativeRtspClientTest*"
          ./gradlew :core:network:desktopTest --tests "*RtspClientIntegrationTest*"
      
      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-results
          path: |
            **/build/test-results/
            **/build/reports/tests/

  integration-test:
    name: Integration Tests
    runs-on: ubuntu-latest
    needs: [test-native, test-kotlin]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Download Native Library
        uses: actions/download-artifact@v3
        with:
          name: libvideo_processing-linux
          path: native/video-processing/build/
      
      - name: Setup Test Environment
        run: |
          chmod +x scripts/*.sh
      
      - name: Run Integration Tests
        run: |
          ./scripts/run-all-tests.sh --verbose
      
      - name: Upload Results
        uses: actions/upload-artifact@v3
        with:
          name: integration-results
          path: build/test-results/

  deploy:
    name: Deploy Release
    runs-on: ubuntu-latest
    needs: [build-linux, build-macos, build-windows, integration-test]
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Download All Artifacts
        uses: actions/download-artifact@v3
      
      - name: Create Release Package
        run: |
          mkdir -p release
          cp -r libvideo_processing-* release/
          cp scripts/*.sh release/
          cp docs/reports/*.md release/docs/
      
      - name: Upload Release
        uses: softprops/action-gh-release@v1
        with:
          files: release/*
          generate_release_notes: true
```

---

## 📊 Code Coverage Configuration

### File: `.codecov.yml`

```yaml
codecov:
  require_ci_to_pass: yes

coverage:
  precision: 2
  round: down
  range: "70...100"

  status:
    project:
      default:
        target: 80%
        threshold: 1%
    patch:
      default:
        target: 70%
        threshold: 1%

parsers:
  gcov:
    branch_detection:
      conditional: yes
      loop: yes
      method: no
      macro: no

comment:
  layout: "reach,diff,flags,tree"
  behavior: default
  require_changes: no
```

---

## 🐳 Docker Configuration

### File: `Dockerfile.test`

```dockerfile
FROM ubuntu:22.04

# Avoid interactive prompts
ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies
RUN apt-get update && apt-get install -y \
    cmake \
    g++ \
    make \
    pkg-config \
    libavformat-dev \
    libavcodec-dev \
    libavutil-dev \
    libswscale-dev \
    libswresample-dev \
    libgtest-dev \
    lcov \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /workspace

# Copy source code
COPY . /workspace

# Build
RUN cd native/video-processing && \
    mkdir build && cd build && \
    cmake .. -DENABLE_FFMPEG=ON -DBUILD_TESTS=ON && \
    make -j$(nproc)

# Run tests
RUN cd native/video-processing/build && ctest --output-on-failure

# Generate coverage
RUN cd native/video-processing && \
    lcov --capture --directory build --output-file coverage.info && \
    lcov --remove coverage.info '/usr/*' --output-file coverage.info && \
    genhtml coverage.info --output-directory coverage_report

CMD ["bash"]
```

### File: `.dockerignore`

```
build/
lib/
*.o
*.so
*.dylib
*.dll
*.class
build/
.gradle/
*.log
```

---

## 📝 Makefile for Local Development

### File: `native/video-processing/Makefile`

```makefile
.PHONY: all build test clean install coverage debug release help

BUILD_DIR = build
INSTALL_DIR = lib

# Compiler settings
CXX = g++
CXXFLAGS = -std=c++17 -Wall -Wextra -O2
DEBUG_FLAGS = -g -O0 -DDEBUG
RELEASE_FLAGS = -O3 -DNDEBUG

# FFmpeg flags
FFMPEG_CFLAGS := $(shell pkg-config --cflags libavformat libavcodec libavutil libswscale libswresample 2>/dev/null || echo "")
FFMPEG_LIBS := $(shell pkg-config --libs libavformat libavcodec libavutil libswscale libswresample 2>/dev/null || echo "-lavformat -lavcodec -lavutil -lswscale -lswresample")

all: release

release: $(BUILD_DIR)/Release
	@echo "Building release version..."
	cd $(BUILD_DIR)/Release && cmake --build . -j$(nproc)

debug: $(BUILD_DIR)/Debug
	@echo "Building debug version..."
	cd $(BUILD_DIR)/Debug && cmake --build . -j$(nproc)

$(BUILD_DIR)/Release:
	@mkdir -p $@
	cd $@ && cmake .. -DCMAKE_BUILD_TYPE=Release -DENABLE_FFMPEG=ON

$(BUILD_DIR)/Debug:
	@mkdir -p $@
	cd $@ && cmake .. -DCMAKE_BUILD_TYPE=Debug -DENABLE_FFMPEG=ON

test: release
	@echo "Running tests..."
	cd $(BUILD_DIR)/Release && ctest --output-on-failure

coverage: release
	@echo "Generating coverage report..."
	cd $(BUILD_DIR)/Release && cmake .. -DCMAKE_BUILD_TYPE=Debug -DCMAKE_CXX_FLAGS="--coverage"
	cd $(BUILD_DIR)/Release && make -j$(nproc)
	cd $(BUILD_DIR)/Release && ctest
	lcov --capture --directory . --output-file coverage.info
	lcov --remove coverage.info '/usr/*' --output-file coverage.info
	genhtml coverage.info --output-directory coverage_report
	@echo "Coverage report: coverage_report/index.html"

install: release
	@echo "Installing library..."
	cd $(BUILD_DIR)/Release && cmake --install . --prefix $(INSTALL_DIR)

clean:
	@echo "Cleaning build directory..."
	rm -rf $(BUILD_DIR)
	rm -rf $(INSTALL_DIR)
	rm -rf coverage_report
	rm -f coverage.info

help:
	@echo "Available targets:"
	@echo "  all       - Build release version (default)"
	@echo "  release   - Build release version"
	@echo "  debug     - Build debug version"
	@echo "  test      - Run all tests"
	@echo "  coverage  - Generate coverage report"
	@echo "  install   - Install library to lib/"
	@echo "  clean     - Clean build artifacts"
	@echo "  help      - Show this help message"
```

---

## 🔧 Pre-commit Hooks

### File: `.git/hooks/pre-commit`

```bash
#!/bin/bash

# Pre-commit hook for code quality checks

echo "Running pre-commit checks..."

# Check for large files
if git diff --cached --name-only | grep -E '\.(so|dll|dylib|jar)$'; then
    echo "Error: Binary files should not be committed"
    exit 1
fi

# Check for TODO comments
if git diff --cached -U0 | grep -E '^\+.*TODO|^\+.*FIXME'; then
    echo "Warning: TODO/FIXME comments detected"
fi

# Run linter (if available)
if command -v clang-format &> /dev/null; then
    echo "Running clang-format check..."
    git diff --cached --name-only | grep -E '\.(cpp|h)$' | xargs -I {} clang-format -n {}
    if [ $? -ne 0 ]; then
        echo "Error: Code formatting issues detected. Run: clang-format -i <file>"
        exit 1
    fi
fi

echo "Pre-commit checks passed"
exit 0
```

---

## 📊 Quality Gates

### Code Quality Checklist

**Build Quality:**
- [ ] No compilation warnings
- [ ] All platforms build successfully
- [ ] Build time < 10 minutes
- [ ] Binary size < 10MB

**Test Quality:**
- [ ] All tests pass
- [ ] Code coverage > 80%
- [ ] No test flakiness
- [ ] Test execution time < 5 minutes

**Code Quality:**
- [ ] Passes clang-format
- [ ] No TODO/FIXME in critical paths
- [ ] Documentation complete
- [ ] No security vulnerabilities

---

## 🚀 Quick Start Commands

### Local Development

```bash
# Build
make all

# Run tests
make test

# Generate coverage
make coverage

# Install
make install

# Clean
make clean
```

### CI/CD Manual Trigger

```bash
# Trigger GitHub Actions manually
gh workflow run ci.yml

# Download artifacts
gh run download --name libvideo_processing-linux
```

### Docker Testing

```bash
# Build test image
docker build -f Dockerfile.test -t ipcss-test .

# Run tests in container
docker run --rm ipcss-test

# Generate coverage
docker run --rm -v $(pwd):/workspace ipcss-test make coverage
```

---

**Configuration Version:** 1.0.0  
**Last Updated:** 27 April 2026  
**Status:** Ready for Week 2
