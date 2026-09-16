#!/bin/bash
# Скрипт для настройки тестовых камер (Linux/macOS)
# Использование: ./scripts/setup-test-cameras.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CONFIG_EXAMPLE="$PROJECT_ROOT/native/video-processing/test/test_config.json.example"
CONFIG_FILE="$PROJECT_ROOT/native/video-processing/test/test_config.json"

echo "=== Test Camera Setup ==="
echo ""

# Проверка существования примера конфигурации
if [ ! -f "$CONFIG_EXAMPLE" ]; then
    echo "❌ Example config not found: $CONFIG_EXAMPLE"
    exit 1
fi

# Проверка существования конфигурации
if [ -f "$CONFIG_FILE" ]; then
    echo "⚠️  Config file already exists: $CONFIG_FILE"
    read -p "Overwrite? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted"
        exit 0
    fi
fi

echo "Creating test configuration..."
echo ""

# Копирование примера
cp "$CONFIG_EXAMPLE" "$CONFIG_FILE"
echo "✅ Config file created: $CONFIG_FILE"
echo ""

echo "=== Configuration Guide ==="
echo ""
echo "Please edit the config file with your camera settings:"
echo "  $CONFIG_FILE"
echo ""
echo "Required fields for each camera:"
echo "  - name: Camera identifier"
echo "  - url: RTSP URL (rtsp://ip:port/path)"
echo "  - username: Username (optional, leave empty if not needed)"
echo "  - password: Password (optional)"
echo "  - codec: H264 or H265"
echo "  - resolution: WIDTHxHEIGHT (e.g., 1920x1080)"
echo "  - fps: Frame rate"
echo ""

echo "Example camera entry:"
cat << 'EOF'
{
  "name": "Test Camera 1",
  "url": "rtsp://192.168.1.100:554/stream",
  "username": "admin",
  "password": "password",
  "codec": "H264",
  "resolution": "1920x1080",
  "fps": 25
}
EOF

echo ""
echo "After editing, run tests with:"
echo "  ./scripts/run-ffmpeg-tests.sh --integration"
echo ""

# Открытие файла в редакторе (опционально)
read -p "Open config file in default editor? (Y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
    ${EDITOR:-nano} "$CONFIG_FILE"
fi
