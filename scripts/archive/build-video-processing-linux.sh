#!/bin/bash

# Скрипт для сборки нативной библиотеки video_processing для Linux x64
# Использование: ./scripts/build-video-processing-linux.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NATIVE_DIR="$PROJECT_ROOT/native/video-processing"
BUILD_DIR="$NATIVE_DIR/build/linux-x64"
LIB_DIR="$NATIVE_DIR/lib/linux/x64"

echo "=== Building video_processing library for Linux x64 ==="
echo ""

# Проверка зависимостей
echo "Checking dependencies..."

if ! command -v cmake &> /dev/null; then
    echo "ERROR: CMake is not installed"
    echo "Install with: sudo apt-get install cmake"
    exit 1
fi

if ! command -v pkg-config &> /dev/null; then
    echo "ERROR: pkg-config is not installed"
    echo "Install with: sudo apt-get install pkg-config"
    exit 1
fi

# Проверка FFmpeg
if ! pkg-config --exists libavcodec libavformat libavutil libswscale; then
    echo "WARNING: FFmpeg development libraries not found via pkg-config"
    echo "Trying to find FFmpeg manually..."

    if [ -z "$FFMPEG_DIR" ]; then
        echo "ERROR: FFmpeg not found. Please install FFmpeg development libraries:"
        echo "  sudo apt-get install libavcodec-dev libavformat-dev libavutil-dev libswscale-dev libswresample-dev"
        echo "Or set FFMPEG_DIR environment variable"
        exit 1
    fi
fi

echo "Dependencies OK"
echo ""

# Создание директорий
mkdir -p "$BUILD_DIR"
mkdir -p "$LIB_DIR"

# Переход в директорию сборки
cd "$BUILD_DIR"

# Конфигурация CMake
echo "Configuring CMake..."

# Подготовка аргументов CMake
CMAKE_ARGS=(
    "$NATIVE_DIR"
    "-DCMAKE_BUILD_TYPE=Release"
    "-DENABLE_FFMPEG=ON"
    "-DENABLE_OPENCV=OFF"
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
cmake --build . --config Release -j$(nproc)

# Копирование библиотеки
echo ""
echo "Copying library..."
if [ -f "$BUILD_DIR/libvideo_processing.so" ]; then
    cp "$BUILD_DIR/libvideo_processing.so" "$LIB_DIR/"
    echo "Library copied to: $LIB_DIR/libvideo_processing.so"
elif [ -f "$BUILD_DIR/bin/libvideo_processing.so" ]; then
    cp "$BUILD_DIR/bin/libvideo_processing.so" "$LIB_DIR/"
    echo "Library copied to: $LIB_DIR/libvideo_processing.so"
else
    echo "ERROR: Library not found in build directory"
    exit 1
fi

# Проверка экспорта символов
echo ""
echo "Checking exported symbols..."
if command -v nm &> /dev/null; then
    nm -D "$LIB_DIR/libvideo_processing.so" | grep -E "video_decoder|T " | head -10
fi

echo ""
echo "=== Build Complete ==="
echo "Library location: $LIB_DIR/libvideo_processing.so"
