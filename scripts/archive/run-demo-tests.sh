#!/bin/bash

# Скрипт для запуска демо-тестов VideoDecoder (без реальных камер)
# Использование: ./scripts/run-demo-tests.sh

set -e

echo "=== VideoDecoder Demo Tests ==="
echo ""
echo "These tests use a simulator and don't require real cameras"
echo ""

# Запуск демо-тестов
echo "Running demo tests..."
./gradlew :core:network:jvmTest --tests "com.company.ipcamera.core.network.video.VideoDecoderDemoTest" --info

echo ""
echo "=== Demo Tests Complete ==="
echo ""
echo "To run tests with real cameras:"
echo "  1. Configure camera URLs in .test-env"
echo "  2. Run: source ./scripts/setup-test-environment.sh"
echo "  3. Run: ./scripts/test-video-decoder.sh"
