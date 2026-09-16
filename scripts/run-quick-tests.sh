#!/bin/bash

# Quick RTSP Tests Runner
# Runs only JVM desktop tests without native compilation
# Usage: ./run-quick-tests.sh [--verbose]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "=========================================="
echo "IP-CSS Quick Test Runner"
echo "=========================================="
echo ""
echo "Running JVM desktop tests only (skipping native compilation)"
echo ""

# Check if native library exists
LIB_PATH="native/video-processing/lib/windows/x64/video_processing.dll"
if [ ! -f "$LIB_PATH" ]; then
    echo -e "${YELLOW}⚠ Native library not found at: $LIB_PATH${NC}"
    echo -e "${YELLOW}Building native library first...${NC}"
    ./native/video-processing/build.ps1
fi

# Gradle command with optimization flags
GRADLE_CMD="./gradlew :core:network:desktopTest \
    -Dipcss.skipNativeTargets=true \
    --parallel \
    --no-daemon \
    --max-workers=4"

if [ "$1" == "--verbose" ]; then
    GRADLE_CMD="$GRADLE_CMD --info --stacktrace"
fi

echo -e "${BLUE}Executing: $GRADLE_CMD${NC}"
echo ""

# Run tests
eval $GRADLE_CMD

# Check results
if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo -e "${GREEN}✓ All tests passed!${NC}"
    echo "=========================================="
    exit 0
else
    echo ""
    echo "=========================================="
    echo -e "${RED}✗ Some tests failed${NC}"
    echo "=========================================="
    exit 1
fi
