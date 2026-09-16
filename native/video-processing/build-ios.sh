#!/bin/bash

# Build video-processing library for iOS
# Usage: ./build-ios.sh [arm64|x64|simulator-arm64] [BuildType]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
ARCH=${1:-"arm64"}
BUILD_TYPE=${2:-"Release"}
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build/ios/$ARCH"
OUTPUT_DIR="$PROJECT_DIR/lib/ios/$ARCH"

echo "=========================================="
echo "iOS Build Script"
echo "=========================================="
echo "Architecture: $ARCH"
echo "Build Type: $BUILD_TYPE"
echo "Build Directory: $BUILD_DIR"
echo "Output Directory: $OUTPUT_DIR"
echo ""

# Determine platform
case $ARCH in
    arm64)
        PLATFORM="iPhoneOS"
        SDK_PATH="/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS.sdk"
        ;;
    x64|simulator-arm64)
        PLATFORM="iPhoneSimulator"
        SDK_PATH="/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator.sdk"
        ;;
    *)
        echo -e "${RED}✗ Invalid architecture: $ARCH${NC}"
        echo "Valid architectures: arm64, x64, simulator-arm64"
        exit 1
        ;;
esac

echo "Platform: $PLATFORM"
echo "SDK Path: $SDK_PATH"
echo ""

# Create build directory
mkdir -p "$BUILD_DIR"
mkdir -p "$OUTPUT_DIR"

# Configure CMake
echo -e "${BLUE}Configuring CMake...${NC}"
cd "$BUILD_DIR"

cmake .. \
    -G Xcode \
    -DCMAKE_SYSTEM_NAME=iOS \
    -DCMAKE_OSX_DEPLOYMENT_TARGET=13.0 \
    -DCMAKE_OSX_ARCHITECTURES="$ARCH" \
    -DCMAKE_OSX_SYSROOT="$SDK_PATH" \
    -DCMAKE_BUILD_TYPE="$BUILD_TYPE" \
    -DBUILD_SHARED_LIBS=ON \
    -DBUILD_STATIC_LIBS=OFF \
    -DENABLE_H264=ON \
    -DENABLE_H265=ON \
    -DENABLE_MJPEG=ON \
    -DENABLE_AAC=ON \
    -DENABLE_G711=ON \
    -DCMAKE_INSTALL_PREFIX="$OUTPUT_DIR"

# Build
echo -e "${BLUE}Building library...${NC}"
cmake --build . --config "$BUILD_TYPE" -j$(sysctl -n hw.ncpu)

# Install
echo -e "${BLUE}Installing library...${NC}"
cmake --install . --config "$BUILD_TYPE"

# Verify output
echo ""
echo "=========================================="
echo "Build Results"
echo "=========================================="

if [ -f "$OUTPUT_DIR/libvideo_processing.dylib" ]; then
    LIB_SIZE=$(stat -f%z "$OUTPUT_DIR/libvideo_processing.dylib")
    echo -e "${GREEN}✓ Library created: libvideo_processing.dylib${NC}"
    echo "  Size: $LIB_SIZE bytes"
    echo "  Location: $OUTPUT_DIR"
    
    # Show file info
    echo ""
    echo "File Info:"
    file "$OUTPUT_DIR/libvideo_processing.dylib"
else
    echo -e "${RED}✗ Library not found at: $OUTPUT_DIR/libvideo_processing.dylib${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ iOS build completed successfully!${NC}"
echo ""
