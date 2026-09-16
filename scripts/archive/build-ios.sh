#!/bin/bash

# Скрипт сборки для iOS
# Использование: ./scripts/build-ios.sh [arch] [build_type]
# Примеры:
#   ./scripts/build-ios.sh arm64 Release
#   ./scripts/build-ios.sh x64 Release  # iOS Simulator
#   ./scripts/build-ios.sh simulator-arm64 Release  # iOS Simulator ARM64

set -e

ARCH="${1:-arm64}"
BUILD_TYPE="${2:-Release}"

# Проверка наличия Xcode
if ! command -v xcodebuild &> /dev/null; then
    echo "Error: Xcode is not installed or xcodebuild is not in PATH"
    exit 1
fi

echo "=========================================="
echo "Building for iOS"
echo "Architecture: $ARCH"
echo "Build Type: $BUILD_TYPE"
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_SCRIPT="$SCRIPT_DIR/build-native-lib.sh"

bash "$BUILD_SCRIPT" ios "$ARCH" "$BUILD_TYPE"

echo ""
echo "=========================================="
echo "iOS build completed!"
echo "=========================================="
echo ""
echo "Library location: native/video-processing/lib/ios/$ARCH/libvideo_processing.a"
echo ""

