#!/bin/bash

# Скрипт для настройки переменных окружения для тестирования VideoDecoder
# Использование: source ./scripts/setup-test-environment.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.test-env"
LOCAL_CAMERA_CONFIG="$PROJECT_ROOT/config/test-cameras.local.json"

echo "=== VideoDecoder Test Environment Setup ==="
echo ""

# 1) Приоритет: локальный JSON-конфиг камер
if [ -f "$LOCAL_CAMERA_CONFIG" ]; then
    echo "Found local camera config. Loading..."
    if command -v python >/dev/null 2>&1; then
        eval "$(python - <<PY
import json
import pathlib
cfg_path = pathlib.Path(r"$LOCAL_CAMERA_CONFIG")
cfg = json.loads(cfg_path.read_text(encoding="utf-8"))
cameras = cfg.get("cameras") or []
defaults = cfg.get("defaults") or {}
if not cameras:
    raise SystemExit(0)
cam = cameras[0]
host = cam.get("host")
if not host:
    raise SystemExit(0)
rtsp_port = cam.get("rtspPort", defaults.get("rtspPort", 554))
rtsp_path = cam.get("rtspPath", defaults.get("rtspPath", "/stream"))
username = cam.get("username", defaults.get("username", "admin"))
password = cam.get("password", defaults.get("password", "password"))
rtsp_url = f"rtsp://{host}:{rtsp_port}{rtsp_path}"
print(f'export TEST_CAMERA_H264_URL="{rtsp_url}"')
print(f'export TEST_CAMERA_H265_URL="{rtsp_url}"')
print(f'export TEST_CAMERA_MJPEG_URL="{rtsp_url}"')
print(f'export TEST_CAMERA_URL="{rtsp_url}"')
print(f'export TEST_RTSP_URL="{rtsp_url}"')
print(f'export TEST_CAMERA_H264_USERNAME="{username}"')
print(f'export TEST_CAMERA_H264_PASSWORD="{password}"')
print(f'export TEST_CAMERA_USERNAME="{username}"')
print(f'export TEST_CAMERA_PASSWORD="{password}"')
print(f'export TEST_RTSP_USERNAME="{username}"')
print(f'export TEST_RTSP_PASSWORD="{password}"')
PY
)"
        if [ -n "$TEST_CAMERA_H264_URL" ]; then
            echo "Loaded camera data from config/test-cameras.local.json"
        fi
    else
        echo "Python is not available, skip local JSON autoload."
    fi
fi

# 2) Fallback: существующий .test-env
if [ -z "$TEST_CAMERA_H264_URL" ] && [ -f "$ENV_FILE" ]; then
    echo "Found existing .test-env file. Loading..."
    source "$ENV_FILE"
    echo "Environment variables loaded from .test-env"
elif [ -z "$TEST_CAMERA_H264_URL" ]; then
    echo "No .test-env file found. Creating template..."
    cat > "$ENV_FILE" << 'EOF'
# VideoDecoder Test Environment Configuration
# Edit this file with your camera URLs and credentials

# H.264 Camera
export TEST_CAMERA_H264_URL="rtsp://camera-ip:554/stream"
export TEST_CAMERA_H264_USERNAME="admin"
export TEST_CAMERA_H264_PASSWORD="password"

# H.265 Camera
export TEST_CAMERA_H265_URL="rtsp://camera-ip:554/stream"
export TEST_CAMERA_H265_USERNAME="admin"
export TEST_CAMERA_H265_PASSWORD="password"

# MJPEG Camera
export TEST_CAMERA_MJPEG_URL="rtsp://camera-ip:554/stream"
export TEST_CAMERA_MJPEG_USERNAME="admin"
export TEST_CAMERA_MJPEG_PASSWORD="password"

# Generic (used if specific codec URLs not set)
export TEST_CAMERA_URL="rtsp://camera-ip:554/stream"
export TEST_CAMERA_USERNAME="admin"
export TEST_CAMERA_PASSWORD="password"

# Test Configuration
export TEST_DURATION_SECONDS=30
export TEST_FRAME_COUNT=750  # ~25 FPS * 30 seconds
EOF
    echo "Template created at: $ENV_FILE"
    echo ""
    echo "Please edit $ENV_FILE with your camera URLs and credentials"
    echo "Then run: source ./scripts/setup-test-environment.sh"
    exit 0
fi

# Проверка обязательных переменных
if [ -z "$TEST_CAMERA_H264_URL" ] && [ -z "$TEST_CAMERA_URL" ]; then
    echo "WARNING: No camera URLs configured!"
    echo "Please edit $ENV_FILE and set TEST_CAMERA_H264_URL or TEST_CAMERA_URL"
    echo ""
fi

# Установка значений по умолчанию
if [ -z "$TEST_CAMERA_H264_URL" ] && [ -n "$TEST_CAMERA_URL" ]; then
    export TEST_CAMERA_H264_URL="$TEST_CAMERA_URL"
fi

if [ -z "$TEST_CAMERA_H265_URL" ] && [ -n "$TEST_CAMERA_URL" ]; then
    export TEST_CAMERA_H265_URL="$TEST_CAMERA_URL"
fi

if [ -z "$TEST_CAMERA_MJPEG_URL" ] && [ -n "$TEST_CAMERA_URL" ]; then
    export TEST_CAMERA_MJPEG_URL="$TEST_CAMERA_URL"
fi

if [ -z "$TEST_CAMERA_USERNAME" ]; then
    export TEST_CAMERA_USERNAME="${TEST_CAMERA_H264_USERNAME:-admin}"
fi

if [ -z "$TEST_CAMERA_PASSWORD" ]; then
    export TEST_CAMERA_PASSWORD="${TEST_CAMERA_H264_PASSWORD:-password}"
fi

# Вывод текущей конфигурации
echo ""
echo "Current Test Configuration:"
echo "  H.264 URL: ${TEST_CAMERA_H264_URL:-not set}"
echo "  H.265 URL: ${TEST_CAMERA_H265_URL:-not set}"
echo "  MJPEG URL: ${TEST_CAMERA_MJPEG_URL:-not set}"
echo "  Username: ${TEST_CAMERA_USERNAME:-not set}"
echo "  Password: ${TEST_CAMERA_PASSWORD:-***hidden***}"
echo "  Test Duration: ${TEST_DURATION_SECONDS:-30} seconds"
echo ""
echo "Environment ready for testing!"
echo "Run: ./scripts/test-video-decoder.sh"
