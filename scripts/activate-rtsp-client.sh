#!/bin/bash

# Скрипт для автоматической активации RTSP клиента после компиляции библиотеки
# Использование: ./scripts/activate-rtsp-client.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NATIVE_CLIENT_FILE="$PROJECT_ROOT/core/network/src/nativeMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.native.kt"

echo "🔧 Activating RTSP Client implementation"
echo "File: $NATIVE_CLIENT_FILE"

# Проверка существования файла
if [ ! -f "$NATIVE_CLIENT_FILE" ]; then
    echo "❌ Error: File not found: $NATIVE_CLIENT_FILE"
    exit 1
fi

# Проверка наличия библиотеки
if [ ! -f "$PROJECT_ROOT/native/video-processing/build/libvideo_processing.dylib" ] && \
   [ ! -f "$PROJECT_ROOT/native/video-processing/build/libvideo_processing.so" ]; then
    echo "⚠️  Warning: Native library not found. Please compile it first:"
    echo "   ./scripts/build-native-lib.sh"
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Проверка наличия cinterop биндингов
BINDINGS_DIR=$(find "$PROJECT_ROOT/core/network/build" -path "*/rtsp_client.klib" -type d 2>/dev/null | head -1)
if [ -z "$BINDINGS_DIR" ]; then
    echo "⚠️  Warning: Cinterop bindings not found. Please generate them first:"
    echo "   ./gradlew :core:network:compileKotlinNative"
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""
echo "📝 This script will help you activate the RTSP client code."
echo "   It will uncomment the implementation in NativeRtspClient.native.kt"
echo ""
echo "⚠️  IMPORTANT: This is a destructive operation!"
echo "   Make sure you have:"
echo "   1. Compiled the native library"
echo "   2. Generated cinterop bindings"
echo "   3. Backed up the file (or using git)"
echo ""
read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 0
fi

# Создание резервной копии
BACKUP_FILE="${NATIVE_CLIENT_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
cp "$NATIVE_CLIENT_FILE" "$BACKUP_FILE"
echo "✅ Backup created: $BACKUP_FILE"

echo ""
echo "⚠️  Manual activation required!"
echo ""
echo "Please follow these steps manually:"
echo ""
echo "1. Open the file: $NATIVE_CLIENT_FILE"
echo ""
echo "2. Uncomment the import (line ~14):"
echo "   Change: // import com.company.ipcamera.core.network.rtsp.rtsp_client.*"
echo "   To:     import com.company.ipcamera.core.network.rtsp.rtsp_client.*"
echo ""
echo "3. Uncomment all TODO sections with implementation"
echo ""
echo "4. Uncomment helper functions at the end of the file"
echo ""
echo "5. Implement callbacks (setFrameCallback, setStatusCallback)"
echo ""
echo "For detailed instructions, see:"
echo "  - docs/RTSP_CLIENT_ACTIVATION_GUIDE.md"
echo "  - RTSP_ACTIVATION_CHECKLIST.md"
echo ""

read -p "Open activation guide? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if command -v open &> /dev/null; then
        open "$PROJECT_ROOT/docs/RTSP_CLIENT_ACTIVATION_GUIDE.md"
    elif command -v xdg-open &> /dev/null; then
        xdg-open "$PROJECT_ROOT/docs/RTSP_CLIENT_ACTIVATION_GUIDE.md"
    else
        echo "Please open: $PROJECT_ROOT/docs/RTSP_CLIENT_ACTIVATION_GUIDE.md"
    fi
fi

echo ""
echo "✅ Script completed. Please follow the manual steps above."

