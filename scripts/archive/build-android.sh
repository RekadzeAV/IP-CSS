#!/bin/bash

# Скрипт сборки для Android
# Использование: ./scripts/build-android.sh [abi] [build_type]
# Примеры:
#   ./scripts/build-android.sh arm64-v8a Release
#   ./scripts/build-android.sh armeabi-v7a Release
#   ./scripts/build-android.sh x86 Release
#   ./scripts/build-android.sh x86_64 Release

set -e

ABI="${1:-arm64-v8a}"
BUILD_TYPE="${2:-Release}"

if [ -z "$ANDROID_NDK" ]; then
    echo "Error: ANDROID_NDK environment variable is not set"
    echo "Please set it to your Android NDK path, e.g.:"
    echo "  export ANDROID_NDK=/path/to/android-ndk-r21e"
    exit 1
fi

if [ ! -d "$ANDROID_NDK" ]; then
    echo "Error: ANDROID_NDK directory does not exist: $ANDROID_NDK"
    exit 1
fi

echo "=========================================="
echo "Building for Android"
echo "ABI: $ABI"
echo "Build Type: $BUILD_TYPE"
echo "NDK: $ANDROID_NDK"
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_SCRIPT="$SCRIPT_DIR/build-native-lib.sh"

bash "$BUILD_SCRIPT" android "$ABI" "$BUILD_TYPE"

echo ""
echo "=========================================="
echo "Android build completed!"
echo "=========================================="
echo ""
echo "Library location: native/video-processing/lib/android/$ABI/libvideo_processing.so"
echo ""

