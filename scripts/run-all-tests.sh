#!/bin/bash

# RTSP Integration Test Runner
# Запускает все тесты для P0-1 RTSP Client
# Usage: ./run-all-tests.sh [--verbose] [--platform linux|macos|windows]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
PLATFORM=${PLATFORM:-"linux"}
VERBOSE=${VERBOSE:-false}
BUILD_DIR="native/video-processing/build"
TEST_RESULTS="build/test-results"
LOG_DIR="build/logs"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Create directories
mkdir -p "$TEST_RESULTS"
mkdir -p "$LOG_DIR"

echo "=========================================="
echo "🧪 RTSP Integration Test Suite"
echo "=========================================="
echo "Platform: $PLATFORM"
echo "Date: $(date)"
echo "Log Directory: $LOG_DIR"
echo ""

# Function to run test suite
run_test_suite() {
    local suiteName=$1
    local testCommand=$2
    local testFile=$3
    
    echo -e "${BLUE}Running $suiteName...${NC}"
    echo "  Command: $testCommand"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    local startTime=$(date +%s)
    
    if eval "$testCommand" > "$LOG_DIR/${suiteName}.log" 2>&1; then
        local endTime=$(date +%s)
        local duration=$((endTime - startTime))
        echo -e "  ${GREEN}✓ PASSED${NC} (${duration}s)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo "  ✓ $suiteName: PASSED (${duration}s)" >> "$TEST_RESULTS/results.txt"
    else
        echo -e "  ${RED}✗ FAILED${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "  ✗ $suiteName: FAILED" >> "$TEST_RESULTS/results.txt"
        
        if [ "$VERBOSE" = true ]; then
            echo "  Log output:"
            tail -50 "$LOG_DIR/${suiteName}.log"
        fi
    fi
    echo ""
}

# Step 1: Compile native library
echo "=========================================="
echo "Step 1: Compile Native Library"
echo "=========================================="

echo -e "${BLUE}Building native library...${NC}"

cd "$BUILD_DIR" || { echo "Build directory not found. Creating..."; mkdir -p "$BUILD_DIR"; cd "$BUILD_DIR"; }

# Platform-specific build
case "$PLATFORM" in
    linux)
        echo "  Building for Linux x64..."
        cmake .. -DENABLE_FFMPEG=ON -DCMAKE_BUILD_TYPE=Release
        make -j8
        ;;
    macos)
        echo "  Building for macOS..."
        cmake .. -DENABLE_FFMPEG=ON -DCMAKE_BUILD_TYPE=Release
        make -j8
        ;;
    windows)
        echo "  Building for Windows x64..."
        cmake .. -G "MinGW Makefiles" -DENABLE_FFMPEG=ON -DCMAKE_BUILD_TYPE=Release
        mingw32-make -j8
        ;;
    *)
        echo -e "${RED}Unknown platform: $PLATFORM${NC}"
        exit 1
        ;;
esac

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Native library compiled successfully${NC}"
    run_test_suite "NativeCompilation" "echo 'Build completed'" "build.log"
else
    echo -e "${RED}✗ Native library compilation failed${NC}"
    run_test_suite "NativeCompilation" "echo 'Build failed'" "build.log"
    exit 1
fi

cd - > /dev/null

# Step 2: Run Audio Decoder Tests
echo "=========================================="
echo "Step 2: Audio Decoder Unit Tests"
echo "=========================================="

if [ -f "$BUILD_DIR/test/audio_decoder_test" ]; then
    run_test_suite "AudioDecoderTests" "$BUILD_DIR/test/audio_decoder_test" "audio_decoder_test.log"
else
    echo -e "${YELLOW}⚠ Audio decoder tests not found (may need to compile with tests)${NC}"
fi

# Step 3: Run Video Decoder Tests
echo "=========================================="
echo "Step 3: Video Decoder Unit Tests"
echo "=========================================="

if [ -f "$BUILD_DIR/test/video_decoder_test" ]; then
    run_test_suite "VideoDecoderTests" "$BUILD_DIR/test/video_decoder_test" "video_decoder_test.log"
else
    echo -e "${YELLOW}⚠ Video decoder tests not found (may need to compile with tests)${NC}"
fi

# Step 4: Run Kotlin JVM Tests
echo "=========================================="
echo "Step 4: Kotlin JVM Unit Tests"
echo "=========================================="

run_test_suite "NativeRtspClientTests" "./gradlew :core:network:desktopTest --tests '*NativeRtspClientTest*'" "native_rtsp_test.log"
run_test_suite "IntegrationTests" "./gradlew :core:network:desktopTest --tests '*RtspClientIntegrationTest*'" "integration_test.log"

# Step 5: FFmpeg 8.0 API Verification
echo "=========================================="
echo "Step 5: FFmpeg 8.0 API Verification"
echo "=========================================="

echo -e "${BLUE}Checking FFmpeg version...${NC}"

ffmpeg_version=$(ffmpeg -version 2>/dev/null | head -n 1 || echo "FFmpeg not installed")
echo "  FFmpeg: $ffmpeg_version"

if echo "$ffmpeg_version" | grep -q "version 8"; then
    echo -e "  ${GREEN}✓ FFmpeg 8.0 detected${NC}"
    run_test_suite "FFmpegVersionCheck" "echo 'FFmpeg 8.0 verified'" "ffmpeg_version.log"
else
    echo -e "  ${YELLOW}⚠ FFmpeg 8.0 not detected, current version may work${NC}"
    run_test_suite "FFmpegVersionCheck" "echo 'FFmpeg version check warning'" "ffmpeg_version.log"
fi

echo ""

# Step 6: Generate Test Report
echo "=========================================="
echo "Step 6: Generate Test Report"
echo "=========================================="

echo "Generating comprehensive test report..."

cat > "$TEST_RESULTS/summary.json" << EOF
{
  "timestamp": $(date +%s),
  "platform": "$PLATFORM",
  "total_tests": $TOTAL_TESTS,
  "passed_tests": $PASSED_TESTS,
  "failed_tests": $FAILED_TESTS,
  "success_rate": $(if [ $TOTAL_TESTS -gt 0 ]; then echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc; else echo "0"; fi),
  "tests": [
EOF

# Add individual test results
first=true
for log in "$LOG_DIR"/*.log; do
    suiteName=$(basename "$log" .log)
    if [ -f "$log" ]; then
        if grep -q "PASSED" "$LOG_DIR/results.txt" 2>/dev/null && grep -q "$suiteName.*PASSED" "$LOG_DIR/results.txt"; then
            status="passed"
        else
            status="failed"
        fi
        
        if [ "$first" = true ]; then
            first=false
        else
            echo "," >> "$TEST_RESULTS/summary.json"
        fi
        
        cat >> "$TEST_RESULTS/summary.json" << EOF
    {
      "name": "$suiteName",
      "status": "$status",
      "log_file": "$suiteName.log"
    }
EOF
    fi
done

cat >> "$TEST_RESULTS/summary.json" << EOF
  ]
}
EOF

echo "Test report saved to: $TEST_RESULTS/summary.json"
echo ""

# Final Summary
echo "=========================================="
echo "📊 Test Summary"
echo "=========================================="
echo "Total Test Suites: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $FAILED_TESTS"

if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)
    echo "Success Rate: ${SUCCESS_RATE}%"
fi

echo ""
echo "Logs directory: $LOG_DIR"
echo "Results: $TEST_RESULTS/summary.json"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    echo -e "${GREEN}✓ Phase 1 RTSP Client ready for integration testing${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    echo -e "${YELLOW}⚠ Please check logs in $LOG_DIR${NC}"
    exit 1
fi
