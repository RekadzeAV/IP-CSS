#!/bin/bash

# Скрипт для компиляции всех нативных библиотек
# Использование: ./scripts/build-all-native-libs.sh [platform]
# Платформы: linux, macos, windows, android, ios, all

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NATIVE_DIR="$PROJECT_ROOT/native"
BUILD_DIR="$NATIVE_DIR/build"

PLATFORM=${1:-"all"}

echo "🔨 Building all native libraries"
echo "Platform: $PLATFORM"
echo "Native directory: $NATIVE_DIR"
echo "Build directory: $BUILD_DIR"

# Функция для сборки Linux
build_linux() {
    echo ""
    echo "📦 Building for Linux..."

    LINUX_BUILD="$BUILD_DIR/linux"
    mkdir -p "$LINUX_BUILD"

    cd "$LINUX_BUILD"
    cmake "$NATIVE_DIR" \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_INSTALL_PREFIX="$LINUX_BUILD/install" \
        -DENABLE_FFMPEG=ON \
        -DENABLE_OPENCV=ON

    cmake --build . --config Release

    # Копируем библиотеки в lib директории
    mkdir -p "$NATIVE_DIR/video-processing/lib/linux"
    mkdir -p "$NATIVE_DIR/analytics/lib/linux"
    mkdir -p "$NATIVE_DIR/codecs/lib/linux"

    cp "$LINUX_BUILD/video-processing/libvideo_processing.so" "$NATIVE_DIR/video-processing/lib/linux/" 2>/dev/null || true
    cp "$LINUX_BUILD/analytics/libanalytics.so" "$NATIVE_DIR/analytics/lib/linux/" 2>/dev/null || true
    cp "$LINUX_BUILD/codecs/libcodecs.so" "$NATIVE_DIR/codecs/lib/linux/" 2>/dev/null || true

    echo "✅ Linux build completed"
}

# Функция для сборки macOS
build_macos() {
    echo ""
    echo "📦 Building for macOS..."

    MACOS_BUILD="$BUILD_DIR/macos"
    mkdir -p "$MACOS_BUILD"

    cd "$MACOS_BUILD"
    cmake "$NATIVE_DIR" \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_INSTALL_PREFIX="$MACOS_BUILD/install" \
        -DCMAKE_OSX_ARCHITECTURES="x86_64;arm64" \
        -DENABLE_FFMPEG=ON \
        -DENABLE_OPENCV=ON

    cmake --build . --config Release

    # Копируем библиотеки в lib директории
    mkdir -p "$NATIVE_DIR/video-processing/lib/macos"
    mkdir -p "$NATIVE_DIR/analytics/lib/macos"
    mkdir -p "$NATIVE_DIR/codecs/lib/macos"

    cp "$MACOS_BUILD/video-processing/libvideo_processing.dylib" "$NATIVE_DIR/video-processing/lib/macos/" 2>/dev/null || true
    cp "$MACOS_BUILD/analytics/libanalytics.dylib" "$NATIVE_DIR/analytics/lib/macos/" 2>/dev/null || true
    cp "$MACOS_BUILD/codecs/libcodecs.dylib" "$NATIVE_DIR/codecs/lib/macos/" 2>/dev/null || true

    echo "✅ macOS build completed"
}

# Функция для сборки Windows
build_windows() {
    echo ""
    echo "📦 Building for Windows..."

    WINDOWS_BUILD="$BUILD_DIR/windows"
    mkdir -p "$WINDOWS_BUILD"

    cd "$WINDOWS_BUILD"
    cmake "$NATIVE_DIR" \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_INSTALL_PREFIX="$WINDOWS_BUILD/install" \
        -DENABLE_FFMPEG=ON \
        -DENABLE_OPENCV=ON \
        -G "MinGW Makefiles"

    cmake --build . --config Release

    # Копируем библиотеки в lib директории
    mkdir -p "$NATIVE_DIR/video-processing/lib/windows"
    mkdir -p "$NATIVE_DIR/analytics/lib/windows"
    mkdir -p "$NATIVE_DIR/codecs/lib/windows"

    cp "$WINDOWS_BUILD/video-processing/libvideo_processing.dll" "$NATIVE_DIR/video-processing/lib/windows/" 2>/dev/null || true
    cp "$WINDOWS_BUILD/analytics/libanalytics.dll" "$NATIVE_DIR/analytics/lib/windows/" 2>/dev/null || true
    cp "$WINDOWS_BUILD/codecs/libcodecs.dll" "$NATIVE_DIR/codecs/lib/windows/" 2>/dev/null || true

    echo "✅ Windows build completed"
}

# Проверка зависимостей
check_dependencies() {
    echo ""
    echo "🔍 Checking dependencies..."

    # Проверка CMake
    if ! command -v cmake &> /dev/null; then
        echo "❌ CMake is not installed. Please install CMake."
        exit 1
    fi

    # Проверка FFmpeg
    if ! pkg-config --exists libavformat libavcodec libavutil libswscale 2>/dev/null; then
        echo "⚠️  FFmpeg libraries not found via pkg-config"
        echo "   Install FFmpeg development libraries:"
        echo "   - Ubuntu/Debian: sudo apt-get install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev"
        echo "   - macOS: brew install ffmpeg"
        echo "   - Windows: Download from https://ffmpeg.org/download.html"
        echo "   Continuing anyway (library may not work without FFmpeg)..."
    else
        echo "✅ FFmpeg found"
    fi

    # Проверка OpenCV (опционально)
    if ! pkg-config --exists opencv4 2>/dev/null; then
        echo "⚠️  OpenCV not found (optional, will be disabled if not found)"
    else
        echo "✅ OpenCV found"
    fi
}

# Создание директорий
mkdir -p "$BUILD_DIR"

# Проверка зависимостей
check_dependencies

# Сборка
case "$PLATFORM" in
    linux)
        build_linux
        ;;
    macos)
        build_macos
        ;;
    windows)
        build_windows
        ;;
    all)
        # Определяем текущую ОС
        if [[ "$OSTYPE" == "linux-gnu"* ]]; then
            build_linux
        elif [[ "$OSTYPE" == "darwin"* ]]; then
            build_macos
        elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
            build_windows
        else
            echo "❌ Unsupported platform: $OSTYPE"
            exit 1
        fi
        ;;
    android)
        if [ -z "$ANDROID_NDK_HOME" ] && [ -z "$NDK_HOME" ]; then
            echo "❌ Android NDK not found. Please set ANDROID_NDK_HOME or NDK_HOME"
            echo "   Or run: ./scripts/build-android-native-libs.sh all"
            exit 1
        fi
        "$SCRIPT_DIR/build-android-native-libs.sh" all
        ;;
    ios)
        if [[ "$OSTYPE" != "darwin"* ]]; then
            echo "❌ iOS builds can only be done on macOS"
            echo "   Or run: ./scripts/build-ios-native-libs.sh all"
            exit 1
        fi
        "$SCRIPT_DIR/build-ios-native-libs.sh" all
        ;;
    *)
        echo "❌ Unknown platform: $PLATFORM"
        echo "Usage: $0 [linux|macos|windows|android|ios|all]"
        exit 1
        ;;
esac

echo ""
echo "✨ Build completed successfully!"

