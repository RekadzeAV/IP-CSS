#!/bin/bash

# Скрипт сборки нативной библиотеки video_processing для всех платформ
# Использование: ./scripts/build-native-lib.sh [platform] [arch] [build_type]
# Примеры:
#   ./scripts/build-native-lib.sh linux x64 Release
#   ./scripts/build-native-lib.sh macos arm64 Release
#   ./scripts/build-native-lib.sh android arm64-v8a Release

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NATIVE_DIR="$PROJECT_ROOT/native/video-processing"

# Параметры по умолчанию
PLATFORM="${1:-linux}"
ARCH="${2:-x64}"
BUILD_TYPE="${3:-Release}"

echo "=========================================="
echo "Building native library: video_processing"
echo "Platform: $PLATFORM"
echo "Architecture: $ARCH"
echo "Build Type: $BUILD_TYPE"
echo "=========================================="

cd "$NATIVE_DIR"

# Создание директории сборки
BUILD_DIR="build/$PLATFORM/$ARCH"
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

# Настройка CMake в зависимости от платформы
case "$PLATFORM" in
    linux)
        cmake ../../.. \
            -DCMAKE_BUILD_TYPE="$BUILD_TYPE" \
            -DCMAKE_CXX_COMPILER="${CXX:-g++}" \
            -DENABLE_FFMPEG=ON \
            -DENABLE_OPENCV=ON
        ;;
    macos)
        cmake ../../.. \
            -DCMAKE_BUILD_TYPE="$BUILD_TYPE" \
            -DCMAKE_OSX_ARCHITECTURES="$ARCH" \
            -DCMAKE_OSX_DEPLOYMENT_TARGET="11.0" \
            -DENABLE_FFMPEG=ON \
            -DENABLE_OPENCV=ON
        ;;
    windows)
        # Для Windows используем MinGW или MSVC
        if command -v x86_64-w64-mingw32-g++ &> /dev/null; then
            cmake ../../.. \
                -G "MinGW Makefiles" \
                -DCMAKE_BUILD_TYPE="$BUILD_TYPE" \
                -DCMAKE_C_COMPILER=x86_64-w64-mingw32-gcc \
                -DCMAKE_CXX_COMPILER=x86_64-w64-mingw32-g++ \
                -DENABLE_FFMPEG=ON \
                -DENABLE_OPENCV=ON
        else
            cmake ../../.. \
                -G "Visual Studio 17 2022" \
                -A x64 \
                -DCMAKE_BUILD_TYPE="$BUILD_TYPE" \
                -DENABLE_FFMPEG=ON \
                -DENABLE_OPENCV=ON
        fi
        ;;
    android)
        if [ -z "$ANDROID_NDK" ]; then
            echo "Error: ANDROID_NDK environment variable is not set"
            exit 1
        fi

        cmake ../../.. \
            -DCMAKE_BUILD_TYPE="$BUILD_TYPE" \
            -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
            -DANDROID_ABI="$ARCH" \
            -DANDROID_PLATFORM=android-21 \
            -DENABLE_FFMPEG=ON \
            -DENABLE_OPENCV=ON
        ;;
    ios)
        cmake ../../.. \
            -DCMAKE_BUILD_TYPE="$BUILD_TYPE" \
            -DCMAKE_SYSTEM_NAME=iOS \
            -DCMAKE_OSX_ARCHITECTURES="$ARCH" \
            -DCMAKE_OSX_DEPLOYMENT_TARGET="11.0" \
            -DCMAKE_XCODE_ATTRIBUTE_ONLY_ACTIVE_ARCH=NO \
            -DENABLE_FFMPEG=ON \
            -DENABLE_OPENCV=ON
        ;;
    *)
        echo "Error: Unknown platform: $PLATFORM"
        echo "Supported platforms: linux, macos, windows, android, ios"
        exit 1
        ;;
esac

# Сборка
if [ "$PLATFORM" = "windows" ] && [ -f "video_processing.sln" ]; then
    # Visual Studio
    cmake --build . --config "$BUILD_TYPE" -j$(nproc 2>/dev/null || echo 4)
else
    # Unix-like (Linux, macOS, MinGW)
    cmake --build . --config "$BUILD_TYPE" -j$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)
fi

# Копирование библиотеки в lib директорию
LIB_OUTPUT_DIR="$PROJECT_ROOT/native/video-processing/lib/$PLATFORM/$ARCH"
mkdir -p "$LIB_OUTPUT_DIR"

case "$PLATFORM" in
    linux)
        if [ -f "libvideo_processing.so" ]; then
            cp "libvideo_processing.so" "$LIB_OUTPUT_DIR/"
            echo "✓ Library copied to: $LIB_OUTPUT_DIR/libvideo_processing.so"
        fi
        ;;
    macos)
        if [ -f "libvideo_processing.dylib" ]; then
            cp "libvideo_processing.dylib" "$LIB_OUTPUT_DIR/"
            echo "✓ Library copied to: $LIB_OUTPUT_DIR/libvideo_processing.dylib"
        fi
        ;;
    windows)
        if [ -f "video_processing.dll" ]; then
            cp "video_processing.dll" "$LIB_OUTPUT_DIR/"
            echo "✓ Library copied to: $LIB_OUTPUT_DIR/video_processing.dll"
        elif [ -f "libvideo_processing.dll" ]; then
            cp "libvideo_processing.dll" "$LIB_OUTPUT_DIR/"
            echo "✓ Library copied to: $LIB_OUTPUT_DIR/libvideo_processing.dll"
        fi
        ;;
    android)
        if [ -f "libvideo_processing.so" ]; then
            cp "libvideo_processing.so" "$LIB_OUTPUT_DIR/"
            echo "✓ Library copied to: $LIB_OUTPUT_DIR/libvideo_processing.so"
        fi
        ;;
    ios)
        if [ -f "libvideo_processing.a" ]; then
            cp "libvideo_processing.a" "$LIB_OUTPUT_DIR/"
            echo "✓ Library copied to: $LIB_OUTPUT_DIR/libvideo_processing.a"
        elif [ -f "libvideo_processing.dylib" ]; then
            cp "libvideo_processing.dylib" "$LIB_OUTPUT_DIR/"
            echo "✓ Library copied to: $LIB_OUTPUT_DIR/libvideo_processing.dylib"
        fi
        ;;
esac

echo "=========================================="
echo "Build completed successfully!"
echo "=========================================="
