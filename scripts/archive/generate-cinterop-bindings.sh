#!/bin/bash
# Скрипт для генерации cinterop биндингов для всех платформ
# Использование: ./scripts/generate-cinterop-bindings.sh [--platform <platform>]

set -e

PLATFORM="all"
HELP=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --platform|-p)
            PLATFORM="$2"
            shift 2
            ;;
        --help|-h)
            HELP=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

if [ "$HELP" = true ]; then
    echo "Usage: ./scripts/generate-cinterop-bindings.sh [options]"
    echo ""
    echo "Options:"
    echo "  --platform, -p <platform>  Platform to generate bindings for (all|linux|macos-x64|macos-arm64|windows)"
    echo "  --help, -h                 Show this help message"
    exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== Generating CInterop Bindings ==="
echo ""

# Проверка наличия библиотек
test_library_exists() {
    [ -f "$1" ]
}

# Платформы и их библиотеки
declare -A PLATFORMS
PLATFORMS[linux]="native/video-processing/lib/linux/x64/libvideo_processing.so"
PLATFORMS[macos-x64]="native/video-processing/lib/macos/x64/libvideo_processing.dylib"
PLATFORMS[macos-arm64]="native/video-processing/lib/macos/arm64/libvideo_processing.dylib"
PLATFORMS[windows]="native/video-processing/lib/windows/x64/video_processing.dll"

# Gradle задачи
declare -A GRADLE_TASKS
GRADLE_TASKS[linux]=":core:network:generateCInteropVideoProcessingNativeLinux"
GRADLE_TASKS[macos-x64]=":core:network:generateCInteropVideoProcessingNativeMacosX64"
GRADLE_TASKS[macos-arm64]=":core:network:generateCInteropVideoProcessingNativeMacosArm64"
GRADLE_TASKS[windows]=":core:network:generateCInteropVideoProcessingNativeWindows"

# Выбор платформ
if [ "$PLATFORM" = "all" ]; then
    SELECTED_PLATFORMS=("linux" "macos-x64" "macos-arm64" "windows")
else
    if [[ -v PLATFORMS[$PLATFORM] ]]; then
        SELECTED_PLATFORMS=("$PLATFORM")
    else
        echo "ERROR: Unknown platform: $PLATFORM"
        echo "Available platforms: ${!PLATFORMS[*]}"
        exit 1
    fi
fi

# Проверка библиотек и генерация
for platform in "${SELECTED_PLATFORMS[@]}"; do
    library_path="$PROJECT_ROOT/${PLATFORMS[$platform]}"
    gradle_task="${GRADLE_TASKS[$platform]}"

    echo "=== Platform: $platform ==="

    # Проверка наличия библиотеки
    if ! test_library_exists "$library_path"; then
        echo "WARNING: Library not found: $library_path"
        echo "  Please build the library first:"
        case $platform in
            linux)
                echo "    ./scripts/build-video-processing-linux.sh"
                ;;
            macos-x64)
                echo "    ./scripts/build-video-processing-macos.sh x64"
                ;;
            macos-arm64)
                echo "    ./scripts/build-video-processing-macos.sh arm64"
                ;;
            windows)
                echo "    .\scripts\build-video-processing-lib.ps1"
                ;;
        esac
        echo "  Skipping this platform..."
        continue
    fi

    echo "Library found: $library_path"

    # Генерация биндингов через Gradle
    echo "Generating cinterop bindings..."

    cd "$PROJECT_ROOT"
    if ./gradlew "$gradle_task"; then
        echo "✅ Bindings generated successfully for $platform"
    else
        echo "❌ Failed to generate bindings for $platform"
        exit 1
    fi

    echo ""
done

echo "=== Generation Complete ==="
echo ""
echo "Next steps:"
echo "  1. Check generated bindings in build/classes/kotlin/*/cinterop/"
echo "  2. Activate VideoDecoder.native.kt code"
echo "  3. Build the project: ./gradlew :core:network:build"
