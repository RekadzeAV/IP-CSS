#!/bin/bash

# Скрипт для сборки библиотеки на всех поддерживаемых платформах
# Использование: ./scripts/build-all-platforms.sh [build_type]

set -e

BUILD_TYPE="${1:-Release}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_SCRIPT="$SCRIPT_DIR/build-native-lib.sh"

echo "=========================================="
echo "Building for all platforms"
echo "Build Type: $BUILD_TYPE"
echo "=========================================="

# Определение текущей платформы
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    CURRENT_PLATFORM="linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    CURRENT_PLATFORM="macos"
elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    CURRENT_PLATFORM="windows"
else
    echo "Unknown platform: $OSTYPE"
    exit 1
fi

# Сборка для текущей платформы
echo ""
echo "Building for $CURRENT_PLATFORM..."
echo ""

if [ "$CURRENT_PLATFORM" = "linux" ]; then
    # Linux x64
    bash "$BUILD_SCRIPT" linux x64 "$BUILD_TYPE"

elif [ "$CURRENT_PLATFORM" = "macos" ]; then
    # macOS x64 (Intel)
    if [ "$(uname -m)" = "x86_64" ]; then
        bash "$BUILD_SCRIPT" macos x64 "$BUILD_TYPE"
    fi

    # macOS arm64 (Apple Silicon)
    if [ "$(uname -m)" = "arm64" ]; then
        bash "$BUILD_SCRIPT" macos arm64 "$BUILD_TYPE"
    fi

    # Универсальная сборка (если нужно)
    # bash "$BUILD_SCRIPT" macos "x64;arm64" "$BUILD_TYPE"

elif [ "$CURRENT_PLATFORM" = "windows" ]; then
    # Windows x64
    powershell -ExecutionPolicy Bypass -File "$SCRIPT_DIR/build-native-lib.ps1" x64 "$BUILD_TYPE"
fi

echo ""
echo "=========================================="
echo "Build for current platform completed!"
echo "=========================================="
echo ""
echo "To build for other platforms:"
echo "  Android:  bash $BUILD_SCRIPT android arm64-v8a $BUILD_TYPE"
echo "  iOS:      bash $BUILD_SCRIPT ios arm64 $BUILD_TYPE"
echo ""
