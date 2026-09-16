#!/bin/bash

# Скрипт для сборки нативной библиотеки video_processing
# Использование: ./scripts/build-video-processing-lib.sh [platform] [architecture]
# Платформы: linux, macos, windows, all
# Архитектуры: x64, arm64 (для Linux/macOS)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NATIVE_DIR="$PROJECT_ROOT/native/video-processing"
BUILD_DIR="$NATIVE_DIR/build"

PLATFORM=${1:-"all"}
ARCH=${2:-"x64"}

echo "🔨 Building native video_processing library"
echo "Platform: $PLATFORM"
echo "Architecture: $ARCH"
echo "Native directory: $NATIVE_DIR"
echo "Build directory: $BUILD_DIR"

# Функция для проверки зависимостей
check_dependencies() {
    echo ""
    echo "🔍 Checking dependencies..."

    # Проверка CMake
    if ! command -v cmake &> /dev/null; then
        echo "❌ CMake is not installed. Please install CMake (minimum version 3.15)."
        exit 1
    fi

    CMAKE_VERSION=$(cmake --version | head -n1 | cut -d' ' -f3)
    echo "✅ CMake found: $CMAKE_VERSION"

    # Проверка компилятора C++
    if command -v g++ &> /dev/null; then
        echo "✅ g++ found: $(g++ --version | head -n1)"
    elif command -v clang++ &> /dev/null; then
        echo "✅ clang++ found: $(clang++ --version | head -n1)"
    else
        echo "⚠️  No C++ compiler found. Will attempt to use default."
    fi

    # Проверка FFmpeg (критично для декодера)
    if pkg-config --exists libavformat libavcodec libavutil libswscale libswresample 2>/dev/null; then
        echo "✅ FFmpeg found via pkg-config"
        pkg-config --modversion libavcodec | head -n1 | xargs echo "   libavcodec version:"
    else
        echo "⚠️  FFmpeg libraries not found via pkg-config"
        echo "   Install FFmpeg development libraries:"
        echo "   - Ubuntu/Debian: sudo apt-get install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev"
        echo "   - Fedora/RHEL: sudo dnf install ffmpeg-devel"
        echo "   - macOS: brew install ffmpeg"
        echo "   - Arch: sudo pacman -S ffmpeg"
        echo "   Continuing anyway (library may not work without FFmpeg)..."
    fi

    # Проверка OpenCV (опционально)
    if pkg-config --exists opencv4 2>/dev/null || pkg-config --exists opencv 2>/dev/null; then
        echo "✅ OpenCV found (optional)"
    else
        echo "⚠️  OpenCV not found (optional, will be disabled if not found)"
    fi

    # Проверка JNI для Desktop (опционально, требуется для Desktop JNI обертки)
    if [ -n "$JAVA_HOME" ]; then
        echo "✅ JAVA_HOME found: $JAVA_HOME"
        if [ -f "$JAVA_HOME/include/jni.h" ] || [ -f "$JAVA_HOME/include/linux/jni.h" ] || [ -f "$JAVA_HOME/include/darwin/jni.h" ]; then
            echo "✅ JNI headers found"
        else
            echo "⚠️  JNI headers not found in JAVA_HOME (Desktop JNI wrapper may not compile)"
        fi
    else
        echo "⚠️  JAVA_HOME not set (Desktop JNI wrapper will not be compiled)"
        echo "   Set JAVA_HOME to enable Desktop JNI support"
    fi
}

# Функция для сборки Linux
build_linux() {
    local arch=$1
    echo ""
    echo "📦 Building for Linux ($arch)..."

    LINUX_BUILD="$BUILD_DIR/linux-$arch"
    mkdir -p "$LINUX_BUILD"

    cd "$LINUX_BUILD"

    # Настройка архитектуры
    if [ "$arch" = "arm64" ]; then
        CMAKE_TOOLCHAIN=""
        CMAKE_ARCH="aarch64"
        # Для кросс-компиляции может потребоваться toolchain файл
    else
        CMAKE_ARCH="x86_64"
    fi

    # Подготовка аргументов CMake
    CMAKE_ARGS=(
        "$NATIVE_DIR"
        "-DCMAKE_BUILD_TYPE=Release"
        "-DCMAKE_INSTALL_PREFIX=$LINUX_BUILD/install"
        "-DCMAKE_SYSTEM_PROCESSOR=$CMAKE_ARCH"
        "-DENABLE_FFMPEG=ON"
        "-DENABLE_OPENCV=ON"
    )

    # Добавляем JNI если доступен
    if [ -n "$JAVA_HOME" ]; then
        CMAKE_ARGS+=("-DJAVA_HOME=$JAVA_HOME")
        echo "   Enabling Desktop JNI support"
    fi

    cmake "${CMAKE_ARGS[@]}"

    cmake --build . --config Release -j$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)

    # Копируем библиотеку в lib директорию
    LIB_DIR="$NATIVE_DIR/lib/linux/$arch"
    mkdir -p "$LIB_DIR"

    if [ -f "libvideo_processing.so" ]; then
        cp libvideo_processing.so "$LIB_DIR/"
        echo "✅ Linux ($arch) build completed"
        echo "   Library: $LIB_DIR/libvideo_processing.so"
        ls -lh "$LIB_DIR/libvideo_processing.so"
    elif [ -f "$LINUX_BUILD/libvideo_processing.so" ]; then
        cp "$LINUX_BUILD/libvideo_processing.so" "$LIB_DIR/"
        echo "✅ Linux ($arch) build completed"
        echo "   Library: $LIB_DIR/libvideo_processing.so"
    else
        echo "⚠️  Library file not found in expected location"
        find "$LINUX_BUILD" -name "*.so" -type f | head -5
    fi
}

# Функция для сборки macOS
build_macos() {
    local arch=$1
    echo ""
    echo "📦 Building for macOS ($arch)..."

    MACOS_BUILD="$BUILD_DIR/macos-$arch"
    mkdir -p "$MACOS_BUILD"

    cd "$MACOS_BUILD"

    # Настройка архитектуры
    if [ "$arch" = "arm64" ]; then
        CMAKE_ARCH="arm64"
        CMAKE_OSX_ARCHITECTURES="arm64"
    else
        CMAKE_ARCH="x86_64"
        CMAKE_OSX_ARCHITECTURES="x86_64"
    fi

    # Подготовка аргументов CMake
    CMAKE_ARGS=(
        "$NATIVE_DIR"
        "-DCMAKE_BUILD_TYPE=Release"
        "-DCMAKE_INSTALL_PREFIX=$MACOS_BUILD/install"
        "-DCMAKE_OSX_ARCHITECTURES=$CMAKE_OSX_ARCHITECTURES"
        "-DCMAKE_OSX_DEPLOYMENT_TARGET=11.0"
        "-DENABLE_FFMPEG=ON"
        "-DENABLE_OPENCV=ON"
    )

    # Добавляем JNI если доступен
    if [ -n "$JAVA_HOME" ]; then
        CMAKE_ARGS+=("-DJAVA_HOME=$JAVA_HOME")
        echo "   Enabling Desktop JNI support"
    fi

    cmake "${CMAKE_ARGS[@]}"

    cmake --build . --config Release -j$(sysctl -n hw.ncpu 2>/dev/null || echo 4)

    # Копируем библиотеку в lib директорию
    LIB_DIR="$NATIVE_DIR/lib/macos/$arch"
    mkdir -p "$LIB_DIR"

    if [ -f "libvideo_processing.dylib" ]; then
        cp libvideo_processing.dylib "$LIB_DIR/"
        echo "✅ macOS ($arch) build completed"
        echo "   Library: $LIB_DIR/libvideo_processing.dylib"
        ls -lh "$LIB_DIR/libvideo_processing.dylib"
    elif [ -f "$MACOS_BUILD/libvideo_processing.dylib" ]; then
        cp "$MACOS_BUILD/libvideo_processing.dylib" "$LIB_DIR/"
        echo "✅ macOS ($arch) build completed"
        echo "   Library: $LIB_DIR/libvideo_processing.dylib"
    else
        echo "⚠️  Library file not found in expected location"
        find "$MACOS_BUILD" -name "*.dylib" -type f | head -5
    fi
}

# Создание директорий
mkdir -p "$BUILD_DIR"
mkdir -p "$NATIVE_DIR/lib/linux/x64"
mkdir -p "$NATIVE_DIR/lib/linux/arm64"
mkdir -p "$NATIVE_DIR/lib/macos/x64"
mkdir -p "$NATIVE_DIR/lib/macos/arm64"

# Проверка зависимостей
check_dependencies

# Сборка
case "$PLATFORM" in
    linux)
        build_linux "$ARCH"
        ;;
    macos)
        build_macos "$ARCH"
        ;;
    all)
        # Определяем текущую ОС
        if [[ "$OSTYPE" == "linux-gnu"* ]]; then
            build_linux "$ARCH"
        elif [[ "$OSTYPE" == "darwin"* ]]; then
            build_macos "$ARCH"
        else
            echo "❌ Unsupported platform: $OSTYPE"
            echo "   Use 'linux' or 'macos' explicitly"
            exit 1
        fi
        ;;
    *)
        echo "❌ Unknown platform: $PLATFORM"
        echo "Usage: $0 [linux|macos|all] [x64|arm64]"
        exit 1
        ;;
esac

echo ""
echo "✨ Build completed successfully!"
echo ""
echo "📝 Next steps:"
echo "   1. Verify library exists: ls -lh $NATIVE_DIR/lib/*/$ARCH/libvideo_processing.*"
echo "   2. Test integration: ./gradlew :core:network:compileKotlinNative"
echo "   3. Uncomment code in VideoDecoder.native.kt"

