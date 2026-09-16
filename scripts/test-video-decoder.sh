#!/bin/bash

# Скрипт для тестирования VideoDecoder на реальных камерах
# Использование: ./scripts/test-video-decoder.sh

set -e

echo "=== VideoDecoder Testing Script ==="
echo ""

# Проверка переменных окружения
if [ -z "$TEST_CAMERA_H264_URL" ] && [ -z "$TEST_CAMERA_H265_URL" ] && [ -z "$TEST_CAMERA_MJPEG_URL" ]; then
    echo "WARNING: No camera URLs configured in environment variables"
    echo ""
    echo "To run tests, set the following environment variables:"
    echo "  export TEST_CAMERA_H264_URL='rtsp://camera-ip:554/stream'"
    echo "  export TEST_CAMERA_H265_URL='rtsp://camera-ip:554/stream'"
    echo "  export TEST_CAMERA_MJPEG_URL='rtsp://camera-ip:554/stream'"
    echo "  export TEST_CAMERA_USERNAME='admin' (optional)"
    echo "  export TEST_CAMERA_PASSWORD='password' (optional)"
    echo ""
    echo "Running unit tests only..."
    echo ""
fi

# Запуск unit тестов
echo "Running VideoDecoder unit tests..."
./gradlew :core:network:jvmTest --tests "com.company.ipcamera.core.network.video.VideoDecoderPerformanceTest" || true

# Запуск integration тестов (если настроены камеры)
if [ -n "$TEST_CAMERA_H264_URL" ] || [ -n "$TEST_CAMERA_H265_URL" ] || [ -n "$TEST_CAMERA_MJPEG_URL" ]; then
    echo ""
    echo "Running VideoDecoder integration tests with real cameras..."
    ./gradlew :core:network:jvmTest --tests "com.company.ipcamera.core.network.video.VideoDecoderIntegrationTest" || true
fi

echo ""
echo "=== Testing Complete ==="
