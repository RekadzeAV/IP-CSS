#!/bin/bash
# Скрипт для полной настройки тестовой среды (Linux/macOS)
# Использование: ./scripts/build-test-environment.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== Building Complete Test Environment ==="
echo ""

# Шаг 1: Проверка зависимостей
echo "Step 1: Checking dependencies..."

# Проверка CMake
if command -v cmake &> /dev/null; then
    CMAKE_VERSION=$(cmake --version | head -n 1)
    echo "✅ CMake: $CMAKE_VERSION"
else
    echo "❌ CMake not found. Please install CMake."
    exit 1
fi

# Проверка FFmpeg
FFMPEG_FOUND=false
if command -v pkg-config &> /dev/null; then
    if pkg-config --exists libavformat libavcodec libavutil 2>/dev/null; then
        echo "✅ FFmpeg found via pkg-config"
        FFMPEG_FOUND=true
    fi
fi

if [ "$FFMPEG_FOUND" = false ]; then
    if [ -n "$FFMPEG_DIR" ] && [ -f "$FFMPEG_DIR/include/libavformat/avformat.h" ]; then
        echo "✅ FFmpeg found: $FFMPEG_DIR"
        FFMPEG_FOUND=true
    else
        echo "⚠️  FFmpeg not found. Tests may not work properly."
        echo "   Install FFmpeg or set FFMPEG_DIR environment variable"
    fi
fi

echo ""

# Шаг 2: Сборка основной библиотеки
echo "Step 2: Building main library..."

NATIVE_DIR="$PROJECT_ROOT/native/video-processing"
BUILD_DIR="$NATIVE_DIR/build/linux-x64"

mkdir -p "$BUILD_DIR"

cd "$BUILD_DIR"

echo "  Configuring CMake..."

CMAKE_ARGS=(
    "../.."
    "-DCMAKE_BUILD_TYPE=Release"
    "-DENABLE_FFMPEG=ON"
    "-DENABLE_OPENCV=OFF"
)

if [ -n "$FFMPEG_DIR" ]; then
    CMAKE_ARGS+=("-DFFMPEG_DIR=$FFMPEG_DIR")
fi

if cmake "${CMAKE_ARGS[@]}" 2>&1 | grep -q "error\|Error\|ERROR"; then
    echo "  ⚠️  CMake configuration may have issues"
else
    echo "  ✅ CMake configured"

    echo "  Building..."
    if cmake --build . --config Release -j$(nproc) 2>&1 | tee build.log; then
        echo "  ✅ Library built successfully"
    else
        echo "  ⚠️  Build may have issues"
    fi
fi

cd "$PROJECT_ROOT"
echo ""

# Шаг 3: Сборка тестов
echo "Step 3: Building tests..."

TEST_DIR="$PROJECT_ROOT/native/video-processing/test"
TEST_BUILD_DIR="$TEST_DIR/build"

mkdir -p "$TEST_BUILD_DIR"

cd "$TEST_BUILD_DIR"

echo "  Configuring CMake..."

CMAKE_ARGS=(
    ".."
    "-DCMAKE_BUILD_TYPE=Release"
    "-DENABLE_FFMPEG=ON"
)

if [ -n "$FFMPEG_DIR" ]; then
    CMAKE_ARGS+=("-DFFMPEG_DIR=$FFMPEG_DIR")
fi

if cmake "${CMAKE_ARGS[@]}" 2>&1 | grep -q "error\|Error\|ERROR"; then
    echo "  ⚠️  CMake configuration may have issues"
else
    echo "  ✅ CMake configured"

    echo "  Building..."
    if cmake --build . --config Release -j$(nproc) 2>&1 | tee build.log; then
        echo "  ✅ Tests built successfully"
    else
        echo "  ⚠️  Build may have issues"
    fi
fi

cd "$PROJECT_ROOT"
echo ""

# Шаг 4: Настройка конфигурации тестов
echo "Step 4: Setting up test configuration..."

CONFIG_EXAMPLE="$TEST_DIR/test_config.json.example"
CONFIG_FILE="$TEST_DIR/test_config.json"

if [ ! -f "$CONFIG_FILE" ]; then
    if [ -f "$CONFIG_EXAMPLE" ]; then
        cp "$CONFIG_EXAMPLE" "$CONFIG_FILE"
        echo "  ✅ Config file created: $CONFIG_FILE"
        echo "     Please edit with your camera settings"
    else
        echo "  ⚠️  Config example not found"
    fi
else
    echo "  ✅ Config file already exists"
fi

echo ""

# Итоги
echo "=== Setup Complete ==="
echo ""
echo "Next steps:"
echo "  1. Edit test_config.json with your camera settings"
echo "  2. Run tests: ./scripts/run-ffmpeg-tests.sh --integration"
echo "  3. Analyze results: ./scripts/analyze-test-results.sh <results_file>"
echo ""
