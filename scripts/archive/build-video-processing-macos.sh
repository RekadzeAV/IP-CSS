#!/bin/bash

# Скрипт для сборки нативной библиотеки video_processing для macOS
# Использование: ./scripts/build-video-processing-macos.sh [x64|arm64]

set -e

ARCH="${1:-arm64}"  # По умолчанию arm64 для Apple Silicon

if [ "$ARCH" != "x64" ] && [ "$ARCH" != "arm64" ]; then
    echo "ERROR: Invalid architecture. Use 'x64' or 'arm64'"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NATIVE_DIR="$PROJECT_ROOT/native/video-processing"
BUILD_DIR="$NATIVE_DIR/build/macos-$ARCH"
LIB_DIR="$NATIVE_DIR/lib/macos/$ARCH"

echo "=== Building video_processing library for macOS $ARCH ==="
echo ""

# Проверка зависимостей
echo "Checking dependencies..."

if ! command -v cmake &> /dev/null; then
    echo "ERROR: CMake is not installed"
    echo "Install with: brew install cmake"
    exit 1
fi

# Проверка FFmpeg
if ! brew list ffmpeg &> /dev/null 2>&1; then
    echo "WARNING: FFmpeg not found via Homebrew"
    if [ -z "$FFMPEG_DIR" ]; then
        echo "ERROR: FFmpeg not found. Please install FFmpeg:"
        echo "  brew install ffmpeg"
        echo "Or set FFMPEG_DIR environment variable"
        exit 1
    fi
else
    echo "FFmpeg found via Homebrew"
    export PKG_CONFIG_PATH="$(brew --prefix ffmpeg)/lib/pkgconfig:$PKG_CONFIG_PATH"
fi

echo "Dependencies OK"
echo ""

# Создание директорий
mkdir -p "$BUILD_DIR"
mkdir -p "$LIB_DIR"

# Переход в директорию сборки
cd "$BUILD_DIR"

# Конфигурация CMake
echo "Configuring CMake for macOS $ARCH..."

# Подготовка аргументов CMake
CMAKE_ARGS=(
    "$NATIVE_DIR"
    "-DCMAKE_BUILD_TYPE=Release"
    "-DENABLE_FFMPEG=ON"
    "-DENABLE_OPENCV=OFF"
    "-DCMAKE_OSX_ARCHITECTURES=$ARCH"
    "-DCMAKE_INSTALL_PREFIX=$LIB_DIR"
)

# Добавляем JNI если доступен
if [ -n "$JAVA_HOME" ]; then
    CMAKE_ARGS+=("-DJAVA_HOME=$JAVA_HOME")
    echo "Enabling Desktop JNI support"
fi

cmake "${CMAKE_ARGS[@]}"

# Сборка
echo ""
echo "Building library..."
cmake --build . --config Release -j$(sysctl -n hw.ncpu)

# Копирование библиотеки
echo ""
echo "Copying library..."
if [ -f "$BUILD_DIR/libvideo_processing.dylib" ]; then
    cp "$BUILD_DIR/libvideo_processing.dylib" "$LIB_DIR/"
    echo "Library copied to: $LIB_DIR/libvideo_processing.dylib"
elif [ -f "$BUILD_DIR/bin/libvideo_processing.dylib" ]; then
    cp "$BUILD_DIR/bin/libvideo_processing.dylib" "$LIB_DIR/"
    echo "Library copied to: $LIB_DIR/libvideo_processing.dylib"
else
    echo "ERROR: Library not found in build directory"
    exit 1
fi

# Проверка экспорта символов
echo ""
echo "Checking exported symbols..."
if command -v nm &> /dev/null; then
    nm -gU "$LIB_DIR/libvideo_processing.dylib" | grep -E "video_decoder|T " | head -10
fi

echo ""
echo "=== Build Complete ==="
echo "Library location: $LIB_DIR/libvideo_processing.dylib"
