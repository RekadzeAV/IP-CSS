#!/bin/bash

# Скрипт для компиляции нативной библиотеки video_processing
# Использование: ./scripts/build-native-lib.sh [platform]
# Платформы: linux, macos, all

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NATIVE_DIR="$PROJECT_ROOT/native/video-processing"
BUILD_DIR="$NATIVE_DIR/build"

PLATFORM=${1:-"all"}

echo "🔨 Building native video_processing library"
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
        -DCMAKE_INSTALL_PREFIX="$LINUX_BUILD/install"
    
    cmake --build . --config Release
    
    # Копируем библиотеку в lib директорию
    LIB_DIR="$NATIVE_DIR/lib/linux"
    mkdir -p "$LIB_DIR"
    cp libvideo_processing.so "$LIB_DIR/" 2>/dev/null || true
    
    echo "✅ Linux build completed"
    echo "Library: $LIB_DIR/libvideo_processing.so"
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
        -DCMAKE_OSX_ARCHITECTURES="x86_64;arm64"
    
    cmake --build . --config Release
    
    # Копируем библиотеку в lib директорию
    LIB_DIR="$NATIVE_DIR/lib/macos"
    mkdir -p "$LIB_DIR"
    cp libvideo_processing.dylib "$LIB_DIR/" 2>/dev/null || cp "$MACOS_BUILD/libvideo_processing.dylib" "$LIB_DIR/" 2>/dev/null || true
    
    echo "✅ macOS build completed"
    echo "Library: $LIB_DIR/libvideo_processing.dylib"
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
    if ! pkg-config --exists libavformat libavcodec libavutil libswscale; then
        echo "⚠️  FFmpeg libraries not found via pkg-config"
        echo "   Install FFmpeg development libraries:"
        echo "   - Ubuntu/Debian: sudo apt-get install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev"
        echo "   - macOS: brew install ffmpeg"
        echo "   Continuing anyway (library may not work without FFmpeg)..."
    else
        echo "✅ FFmpeg found"
    fi
    
    # Проверка OpenCV (опционально)
    if ! pkg-config --exists opencv4; then
        echo "⚠️  OpenCV not found (optional, will be disabled if not found)"
    else
        echo "✅ OpenCV found"
    fi
}

# Создание директорий
mkdir -p "$BUILD_DIR"
mkdir -p "$NATIVE_DIR/lib/linux"
mkdir -p "$NATIVE_DIR/lib/macos"

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
    all)
        # Определяем текущую ОС
        if [[ "$OSTYPE" == "linux-gnu"* ]]; then
            build_linux
        elif [[ "$OSTYPE" == "darwin"* ]]; then
            build_macos
        else
            echo "❌ Unsupported platform: $OSTYPE"
            exit 1
        fi
        ;;
    *)
        echo "❌ Unknown platform: $PLATFORM"
        echo "Usage: $0 [linux|macos|all]"
        exit 1
        ;;
esac

echo ""
echo "✨ Build completed successfully!"

