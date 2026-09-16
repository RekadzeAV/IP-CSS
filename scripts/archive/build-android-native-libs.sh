#!/bin/bash

# Скрипт для компиляции нативных библиотек для Android
# Использование: ./scripts/build-android-native-libs.sh [arch]
# Архитектуры: armeabi-v7a, arm64-v8a, x86, x86_64, all
#
# Требования:
# - Android NDK (установлен и NDK_HOME или ANDROID_NDK_HOME установлен)
# - CMake 3.15+
# - FFmpeg для Android (опционально)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NATIVE_DIR="$PROJECT_ROOT/native"
BUILD_DIR="$NATIVE_DIR/build/android"

ARCH=${1:-"all"}

echo "🔨 Building native libraries for Android"
echo "Architecture: $ARCH"
echo "Native directory: $NATIVE_DIR"
echo "Build directory: $BUILD_DIR"

# Определение NDK пути
if [ -z "$ANDROID_NDK_HOME" ] && [ -z "$NDK_HOME" ]; then
    # Попытка найти NDK в стандартных местах
    if [ -d "$HOME/Android/Sdk/ndk" ]; then
        NDK_PATH=$(find "$HOME/Android/Sdk/ndk" -maxdepth 1 -type d | sort -V | tail -1)
        export ANDROID_NDK_HOME="$NDK_PATH"
        echo "Found NDK at: $NDK_PATH"
    else
        echo "❌ Android NDK not found. Please set ANDROID_NDK_HOME or NDK_HOME"
        echo "   Or install NDK via Android Studio SDK Manager"
        exit 1
    fi
else
    NDK_PATH="${ANDROID_NDK_HOME:-$NDK_HOME}"
    echo "Using NDK at: $NDK_PATH"
fi

# Проверка CMake
if ! command -v cmake &> /dev/null; then
    echo "❌ CMake is not installed. Please install CMake."
    exit 1
fi

# Функция для сборки для конкретной архитектуры
build_for_arch() {
    local arch=$1
    local abi=$2
    local toolchain=$3

    echo ""
    echo "📦 Building for Android $arch ($abi)..."

    local build_dir="$BUILD_DIR/$arch"
    mkdir -p "$build_dir"

    cd "$build_dir"

    # Настройка toolchain
    local toolchain_file="$NDK_PATH/build/cmake/android.toolchain.cmake"
    if [ ! -f "$toolchain_file" ]; then
        echo "❌ Android toolchain file not found: $toolchain_file"
        echo "   Please check your NDK installation"
        exit 1
    fi

    # CMake конфигурация для Android
    cmake "$NATIVE_DIR" \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_TOOLCHAIN_FILE="$toolchain_file" \
        -DANDROID_ABI="$abi" \
        -DANDROID_PLATFORM=android-21 \
        -DANDROID_STL=c++_shared \
        -DENABLE_FFMPEG=OFF \
        -DENABLE_OPENCV=OFF \
        -DENABLE_TENSORFLOW=OFF \
        -DCMAKE_INSTALL_PREFIX="$build_dir/install"

    cmake --build . --config Release -j$(nproc)

    # Копируем библиотеки в lib директории
    local lib_ext="so"
    local lib_prefix="lib"

    # video-processing
    mkdir -p "$NATIVE_DIR/video-processing/lib/android/$arch"
    if [ -f "$build_dir/video-processing/${lib_prefix}video_processing.${lib_ext}" ]; then
        cp "$build_dir/video-processing/${lib_prefix}video_processing.${lib_ext}" \
           "$NATIVE_DIR/video-processing/lib/android/$arch/" 2>/dev/null || true
        echo "  ✅ Copied video_processing.${lib_ext}"
    fi

    # analytics
    mkdir -p "$NATIVE_DIR/analytics/lib/android/$arch"
    if [ -f "$build_dir/analytics/${lib_prefix}analytics.${lib_ext}" ]; then
        cp "$build_dir/analytics/${lib_prefix}analytics.${lib_ext}" \
           "$NATIVE_DIR/analytics/lib/android/$arch/" 2>/dev/null || true
        echo "  ✅ Copied analytics.${lib_ext}"
    fi

    # codecs
    mkdir -p "$NATIVE_DIR/codecs/lib/android/$arch"
    if [ -f "$build_dir/codecs/${lib_prefix}codecs.${lib_ext}" ]; then
        cp "$build_dir/codecs/${lib_prefix}codecs.${lib_ext}" \
           "$NATIVE_DIR/codecs/lib/android/$arch/" 2>/dev/null || true
        echo "  ✅ Copied codecs.${lib_ext}"
    fi

    echo "✅ Android $arch build completed"
}

# Создание директорий
mkdir -p "$BUILD_DIR"

# Сборка
case "$ARCH" in
    armeabi-v7a)
        build_for_arch "armeabi-v7a" "armeabi-v7a" "arm-linux-androideabi"
        ;;
    arm64-v8a)
        build_for_arch "arm64-v8a" "arm64-v8a" "aarch64-linux-android"
        ;;
    x86)
        build_for_arch "x86" "x86" "i686-linux-android"
        ;;
    x86_64)
        build_for_arch "x86_64" "x86_64" "x86_64-linux-android"
        ;;
    all)
        build_for_arch "armeabi-v7a" "armeabi-v7a" "arm-linux-androideabi"
        build_for_arch "arm64-v8a" "arm64-v8a" "aarch64-linux-android"
        build_for_arch "x86" "x86" "i686-linux-android"
        build_for_arch "x86_64" "x86_64" "x86_64-linux-android"
        ;;
    *)
        echo "❌ Unknown architecture: $ARCH"
        echo "Usage: $0 [armeabi-v7a|arm64-v8a|x86|x86_64|all]"
        exit 1
        ;;
esac

echo ""
echo "✨ Android build completed successfully!"



