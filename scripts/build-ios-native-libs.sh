#!/bin/bash

# Скрипт для компиляции нативных библиотек для iOS
# Использование: ./scripts/build-ios-native-libs.sh [arch]
# Архитектуры: arm64, x64, simulator-arm64, all
#
# Требования:
# - macOS с Xcode и Command Line Tools
# - CMake 3.15+
# - iOS SDK (обычно устанавливается с Xcode)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NATIVE_DIR="$PROJECT_ROOT/native"
BUILD_DIR="$NATIVE_DIR/build/ios"

ARCH=${1:-"all"}

echo "🔨 Building native libraries for iOS"
echo "Architecture: $ARCH"
echo "Native directory: $NATIVE_DIR"
echo "Build directory: $BUILD_DIR"

# Проверка что мы на macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
    echo "❌ iOS builds can only be done on macOS"
    exit 1
fi

# Проверка Xcode
if ! command -v xcodebuild &> /dev/null; then
    echo "❌ Xcode is not installed. Please install Xcode from App Store"
    exit 1
fi

# Проверка CMake
if ! command -v cmake &> /dev/null; then
    echo "❌ CMake is not installed. Please install CMake."
    exit 1
fi

# Получение iOS SDK пути
IOS_SDK_PATH=$(xcrun --sdk iphoneos --show-sdk-path 2>/dev/null || echo "")
if [ -z "$IOS_SDK_PATH" ]; then
    echo "❌ iOS SDK not found. Please install Xcode Command Line Tools:"
    echo "   xcode-select --install"
    exit 1
fi
echo "Using iOS SDK: $IOS_SDK_PATH"

# Получение версии SDK
IOS_SDK_VERSION=$(xcrun --sdk iphoneos --show-sdk-version 2>/dev/null || echo "15.0")
echo "iOS SDK Version: $IOS_SDK_VERSION"

# Функция для сборки для конкретной архитектуры
build_for_arch() {
    local arch=$1
    local platform=$2
    local sdk=$3
    local min_version=$4

    echo ""
    echo "📦 Building for iOS $arch ($platform)..."

    local build_dir="$BUILD_DIR/$arch"
    mkdir -p "$build_dir"

    cd "$build_dir"

    # Определение toolchain
    local toolchain_file="$NATIVE_DIR/cmake/ios.toolchain.cmake"

    # Если кастомный toolchain не найден, используем стандартный подход
    if [ ! -f "$toolchain_file" ]; then
        # Используем встроенный CMake iOS support
        cmake "$NATIVE_DIR" \
            -DCMAKE_BUILD_TYPE=Release \
            -DCMAKE_SYSTEM_NAME=iOS \
            -DCMAKE_OSX_DEPLOYMENT_TARGET="$min_version" \
            -DCMAKE_OSX_ARCHITECTURES="$arch" \
            -DCMAKE_OSX_SYSROOT="$sdk" \
            -DCMAKE_C_COMPILER="$(xcrun --sdk $platform --find clang)" \
            -DCMAKE_CXX_COMPILER="$(xcrun --sdk $platform --find clang++)" \
            -DCMAKE_C_FLAGS="-arch $arch -miphoneos-version-min=$min_version" \
            -DCMAKE_CXX_FLAGS="-arch $arch -miphoneos-version-min=$min_version" \
            -DENABLE_FFMPEG=OFF \
            -DENABLE_OPENCV=OFF \
            -DENABLE_TENSORFLOW=OFF \
            -DCMAKE_INSTALL_PREFIX="$build_dir/install"
    else
        cmake "$NATIVE_DIR" \
            -DCMAKE_BUILD_TYPE=Release \
            -DCMAKE_TOOLCHAIN_FILE="$toolchain_file" \
            -DPLATFORM="$platform" \
            -DCMAKE_OSX_DEPLOYMENT_TARGET="$min_version" \
            -DENABLE_FFMPEG=OFF \
            -DENABLE_OPENCV=OFF \
            -DENABLE_TENSORFLOW=OFF \
            -DCMAKE_INSTALL_PREFIX="$build_dir/install"
    fi

    cmake --build . --config Release -j$(sysctl -n hw.ncpu)

    # Копируем библиотеки в lib директории
    local lib_ext="a"  # iOS использует статические библиотеки (.a)
    local lib_prefix="lib"

    # Определяем путь для iOS
    local ios_lib_path="ios"
    if [ "$platform" = "iphonesimulator" ]; then
        if [ "$arch" = "arm64" ]; then
            ios_lib_path="ios/simulator-arm64"
        else
            ios_lib_path="ios/x64"
        fi
    else
        ios_lib_path="ios/arm64"
    fi

    # video-processing
    mkdir -p "$NATIVE_DIR/video-processing/lib/$ios_lib_path"
    if [ -f "$build_dir/video-processing/${lib_prefix}video_processing.${lib_ext}" ]; then
        cp "$build_dir/video-processing/${lib_prefix}video_processing.${lib_ext}" \
           "$NATIVE_DIR/video-processing/lib/$ios_lib_path/" 2>/dev/null || true
        echo "  ✅ Copied video_processing.${lib_ext}"
    fi

    # analytics
    mkdir -p "$NATIVE_DIR/analytics/lib/$ios_lib_path"
    if [ -f "$build_dir/analytics/${lib_prefix}analytics.${lib_ext}" ]; then
        cp "$build_dir/analytics/${lib_prefix}analytics.${lib_ext}" \
           "$NATIVE_DIR/analytics/lib/$ios_lib_path/" 2>/dev/null || true
        echo "  ✅ Copied analytics.${lib_ext}"
    fi

    # codecs
    mkdir -p "$NATIVE_DIR/codecs/lib/$ios_lib_path"
    if [ -f "$build_dir/codecs/${lib_prefix}codecs.${lib_ext}" ]; then
        cp "$build_dir/codecs/${lib_prefix}codecs.${lib_ext}" \
           "$NATIVE_DIR/codecs/lib/$ios_lib_path/" 2>/dev/null || true
        echo "  ✅ Copied codecs.${lib_ext}"
    fi

    echo "✅ iOS $arch build completed"
}

# Создание директорий
mkdir -p "$BUILD_DIR"

# Сборка
case "$ARCH" in
    arm64)
        build_for_arch "arm64" "iphoneos" "$IOS_SDK_PATH" "11.0"
        ;;
    x64)
        build_for_arch "x86_64" "iphonesimulator" "$(xcrun --sdk iphonesimulator --show-sdk-path)" "11.0"
        ;;
    simulator-arm64)
        build_for_arch "arm64" "iphonesimulator" "$(xcrun --sdk iphonesimulator --show-sdk-path)" "11.0"
        ;;
    all)
        build_for_arch "arm64" "iphoneos" "$IOS_SDK_PATH" "11.0"
        build_for_arch "x86_64" "iphonesimulator" "$(xcrun --sdk iphonesimulator --show-sdk-path)" "11.0"
        build_for_arch "arm64" "iphonesimulator" "$(xcrun --sdk iphonesimulator --show-sdk-path)" "11.0"
        ;;
    *)
        echo "❌ Unknown architecture: $ARCH"
        echo "Usage: $0 [arm64|x64|simulator-arm64|all]"
        exit 1
        ;;
esac

echo ""
echo "✨ iOS build completed successfully!"

