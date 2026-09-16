#!/bin/bash

# Скрипт для сборки нативной библиотеки video_processing через Docker
# Использование: ./scripts/build-video-processing-docker.sh [linux|macos]

set -e

PLATFORM="${1:-linux}"

if [ "$PLATFORM" != "linux" ] && [ "$PLATFORM" != "macos" ]; then
    echo "ERROR: Invalid platform. Use 'linux' or 'macos'"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NATIVE_DIR="$PROJECT_ROOT/native/video-processing"

echo "=== Building video_processing library via Docker ($PLATFORM) ==="
echo ""

# Проверка Docker
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed"
    echo "Install Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# Создание Dockerfile для сборки
if [ "$PLATFORM" == "linux" ]; then
    cat > "$NATIVE_DIR/Dockerfile.build" << 'EOF'
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

# Установка зависимостей
RUN apt-get update && apt-get install -y \
    cmake \
    build-essential \
    pkg-config \
    libavcodec-dev \
    libavformat-dev \
    libavutil-dev \
    libswscale-dev \
    libswresample-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build
COPY . .

# Сборка
RUN mkdir -p build/linux-x64 && \
    cd build/linux-x64 && \
    cmake ../.. \
        -DCMAKE_BUILD_TYPE=Release \
        -DENABLE_FFMPEG=ON \
        -DENABLE_OPENCV=OFF && \
    cmake --build . --config Release -j$(nproc)

# Копирование результата
RUN mkdir -p /output && \
    cp build/linux-x64/libvideo_processing.so /output/ 2>/dev/null || \
    cp build/linux-x64/bin/libvideo_processing.so /output/ 2>/dev/null || \
    echo "Library not found, check build directory"
EOF

    echo "Building Linux library via Docker..."
    docker build -f "$NATIVE_DIR/Dockerfile.build" -t video-processing-builder:linux "$NATIVE_DIR"

    # Копирование результата
    mkdir -p "$NATIVE_DIR/lib/linux/x64"
    docker create --name temp-container video-processing-builder:linux
    docker cp temp-container:/output/libvideo_processing.so "$NATIVE_DIR/lib/linux/x64/" 2>/dev/null || echo "Library not found in container"
    docker rm temp-container

    echo ""
    echo "=== Build Complete ==="
    if [ -f "$NATIVE_DIR/lib/linux/x64/libvideo_processing.so" ]; then
        echo "Library location: $NATIVE_DIR/lib/linux/x64/libvideo_processing.so"
    else
        echo "WARNING: Library not found. Check Docker build logs."
    fi

elif [ "$PLATFORM" == "macos" ]; then
    echo "ERROR: macOS build via Docker requires macOS host or cross-compilation setup"
    echo "For macOS, please use: ./scripts/build-video-processing-macos.sh"
    echo ""
    echo "Alternative: Use GitHub Actions or CI/CD for macOS builds"
    exit 1
fi
