#!/bin/bash

# Скрипт для профилирования VideoDecoder
# Использование: ./scripts/profile-video-decoder.sh

set -e

echo "=== VideoDecoder Profiling Script ==="
echo ""

# Проверка наличия инструментов профилирования
if ! command -v jvisualvm &> /dev/null && ! command -v jprofiler &> /dev/null; then
    echo "WARNING: No profiling tools found (jvisualvm or jprofiler)"
    echo "Profiling will use built-in JVM metrics only"
    echo ""
fi

# Настройка JVM для профилирования
export JAVA_OPTS="-XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation -XX:LogFile=profile.log"

# Запуск приложения с профилированием
echo "Starting application with profiling..."
echo ""

# Если доступен jvisualvm, запускаем его
if command -v jvisualvm &> /dev/null; then
    echo "Starting VisualVM..."
    jvisualvm &
    echo "VisualVM started. Connect to the running application."
    echo ""
fi

# Запуск тестов с профилированием
echo "Running performance tests..."
./gradlew :core:network:jvmTest --tests "com.company.ipcamera.core.network.video.VideoDecoderPerformanceTest" \
    -Pprofile=true \
    -PjvmArgs="-XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation"

echo ""
echo "=== Profiling Complete ==="
echo "Check profile.log for compilation logs"
echo "Check build/reports/tests for test results"
