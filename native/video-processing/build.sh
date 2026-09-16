#!/bin/bash

# Cross-platform build script for video_processing library
# Usage: ./build.sh [--all] [--platform linux|macos|windows|arm64] [--clean]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
BUILD_ALL=false
PLATFORM=${PLATFORM:-"auto"}
CLEAN=false
VERBOSE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --all)
            BUILD_ALL=true
            shift
            ;;
        --platform)
            PLATFORM="$2"
            shift 2
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Detect platform
if [ "$PLATFORM" = "auto" ]; then
    OS=$(uname -s | tr '[:upper:]' '[:lower:]')
    ARCH=$(uname -m)
    
    case "$OS" in
        linux)
            PLATFORM="linux"
            ;;
        darwin)
            PLATFORM="macos"
            ;;
        msys|mingw|cygwin)
            PLATFORM="windows"
            ;;
        *)
            echo -e "${RED}Unknown OS: $OS${NC}"
            exit 1
            ;;
    esac
    
    case "$ARCH" in
        x86_64|amd64)
            ARCH="x64"
            ;;
        aarch64|arm64)
            ARCH="arm64"
            ;;
    esac
    
    echo -e "${BLUE}Detected platform: $PLATFORM $ARCH${NC}"
fi

# Build directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_DIR="$PROJECT_ROOT/build"
OUTPUT_DIR="$PROJECT_ROOT/lib"

# Create output directories
mkdir -p "$OUTPUT_DIR/$PLATFORM"

# Function to build for specific platform
build_platform() {
    local plat=$1
    local arch=$2
    local preset="$plat-$arch-release"
    
    echo -e "${BLUE}Building for $plat $arch...${NC}"
    
    local plat_build_dir="$BUILD_DIR/$plat-$arch"
    mkdir -p "$plat_build_dir"
    cd "$plat_build_dir"
    
    # Clean if requested
    if [ "$CLEAN" = true ]; then
        echo "  Cleaning build directory..."
        rm -rf *
    fi
    
    # Configure
    echo "  Configuring with CMake..."
    if cmake --preset="$preset" 2>&1; then
        :
    else
        # Fallback to manual configuration
        echo "  Using manual configuration..."
        if [ "$plat" = "windows" ]; then
            cmake -G "MinGW Makefiles" \
                  -DENABLE_FFMPEG=ON \
                  -DCMAKE_BUILD_TYPE=Release \
                  -DCMAKE_INSTALL_PREFIX="$OUTPUT_DIR/$plat/$arch" \
                  ..
        else
            cmake -G "Unix Makefiles" \
                  -DENABLE_FFMPEG=ON \
                  -DCMAKE_BUILD_TYPE=Release \
                  -DCMAKE_INSTALL_PREFIX="$OUTPUT_DIR/$plat/$arch" \
                  ..
        fi
    fi
    
    # Build
    echo "  Building..."
    if [ "$VERBOSE" = true ]; then
        cmake --build . --config Release -j8
    else
        cmake --build . --config Release -j8 > /dev/null 2>&1
    fi
    
    # Install
    echo "  Installing..."
    cmake --install . --config Release
    
    # Copy library to output directory
    echo "  Copying library..."
    local lib_name=""
    case "$plat" in
        linux)
            lib_name="libvideo_processing.so"
            ;;
        macos)
            lib_name="libvideo_processing.dylib"
            ;;
        windows)
            lib_name="video_processing.dll"
            ;;
    esac
    
    if [ -f "$plat_build_dir/$lib_name" ]; then
        mkdir -p "$OUTPUT_DIR/$plat/$arch"
        cp "$plat_build_dir/$lib_name" "$OUTPUT_DIR/$plat/$arch/"
        echo -e "  ${GREEN}✓ Library copied to: $OUTPUT_DIR/$plat/$arch/$lib_name${NC}"
    fi
    
    cd "$PROJECT_ROOT"
    echo ""
}

# Main build logic
echo "=========================================="
echo "🔧 Video Processing Library Build"
echo "=========================================="
echo "Platform: $PLATFORM"
echo "Clean: $CLEAN"
echo "Verbose: $VERBOSE"
echo ""

if [ "$BUILD_ALL" = true ]; then
    echo -e "${BLUE}Building for all platforms...${NC}"
    echo ""
    
    # Linux
    if [ "$(uname -s)" = "Linux" ]; then
        build_platform "linux" "x64"
        build_platform "linux" "arm64"
    else
        echo -e "${YELLOW}⚠ Linux build skipped (not on Linux)${NC}"
    fi
    
    # macOS
    if [ "$(uname -s)" = "Darwin" ]; then
        build_platform "macos" "x64"
        build_platform "macos" "arm64"
    else
        echo -e "${YELLOW}⚠ macOS build skipped (not on macOS)${NC}"
    fi
    
    # Windows
    if [[ "$(uname -s)" == *MINGW* ]] || [[ "$(uname -s)" == *MSYS* ]]; then
        build_platform "windows" "x64"
    else
        echo -e "${YELLOW}⚠ Windows build skipped (not on Windows)${NC}"
    fi
else
    # Build for specified platform
    case "$PLATFORM" in
        linux)
            build_platform "linux" "$ARCH"
            ;;
        macos)
            build_platform "macos" "$ARCH"
            ;;
        windows)
            build_platform "windows" "x64"
            ;;
        *)
            echo -e "${RED}Unknown platform: $PLATFORM${NC}"
            exit 1
            ;;
    esac
fi

# Summary
echo "=========================================="
echo "📊 Build Summary"
echo "=========================================="
echo "Output directory: $OUTPUT_DIR"
echo ""
echo "Libraries built:"
find "$OUTPUT_DIR" -name "*.so" -o -name "*.dylib" -o -name "*.dll" 2>/dev/null | while read lib; do
    echo "  - $lib"
done
echo ""
echo -e "${GREEN}✓ Build completed successfully${NC}"
