#!/bin/bash
# Скрипт для мониторинга производительности тестов (Linux/macOS)
# Использование: ./scripts/monitor-test-performance.sh [--duration <seconds>]

set -e

DURATION=60
HELP=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --duration|-d)
            DURATION="$2"
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
    echo "Usage: ./scripts/monitor-test-performance.sh [options]"
    echo ""
    echo "Options:"
    echo "  --duration, -d <seconds>  Monitoring duration (default: 60)"
    echo "  --help, -h                 Show this help message"
    exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_BUILD_DIR="$PROJECT_ROOT/native/video-processing/test/build"
TEST_EXECUTABLE="$TEST_BUILD_DIR/video_processing_tests"

if [ ! -f "$TEST_EXECUTABLE" ]; then
    echo "❌ Test executable not found: $TEST_EXECUTABLE"
    echo "   Please build tests first"
    exit 1
fi

echo "=== Performance Monitoring ==="
echo "Duration: $DURATION seconds"
echo ""

# Создание файла для метрик
METRICS_FILE="$TEST_BUILD_DIR/performance_metrics_$(date +%Y%m%d_%H%M%S).csv"
echo "Timestamp,CPU%,MemoryMB,FPS,FramesDecoded" > "$METRICS_FILE"

echo "Starting performance monitoring..."
echo "Metrics will be saved to: $METRICS_FILE"
echo ""

START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION))

# Запуск тестов в фоне
"$TEST_EXECUTABLE" --integration > /dev/null 2>&1 &
TEST_PID=$!

# Функция очистки
cleanup() {
    kill $TEST_PID 2>/dev/null || true
}
trap cleanup EXIT

FRAME_COUNT=0
LAST_FRAME_COUNT=0

while [ $(date +%s) -lt $END_TIME ]; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))

    # Получение метрик процесса
    if ps -p $TEST_PID > /dev/null 2>&1; then
        # CPU и память (Linux)
        if command -v ps > /dev/null; then
            CPU_PERCENT=$(ps -p $TEST_PID -o %cpu= | tr -d ' ')
            MEMORY_KB=$(ps -p $TEST_PID -o rss= | tr -d ' ')
            MEMORY_MB=$(echo "scale=2; $MEMORY_KB / 1024" | bc)
        else
            CPU_PERCENT=0
            MEMORY_MB=0
        fi

        # Расчет FPS
        FPS=$(echo "scale=2; $FRAME_COUNT / $ELAPSED" | bc 2>/dev/null || echo "0")

        # Запись метрик
        echo "$ELAPSED,$CPU_PERCENT,$MEMORY_MB,$FPS,$FRAME_COUNT" >> "$METRICS_FILE"

        printf "[%ds] CPU: %s%% | Memory: %s MB | FPS: %s | Frames: %d\n" \
            "$ELAPSED" "$CPU_PERCENT" "$MEMORY_MB" "$FPS" "$FRAME_COUNT"
    fi

    sleep 1
done

cleanup

echo ""
echo "=== Monitoring Complete ==="
echo "Metrics saved to: $METRICS_FILE"
echo ""
echo "To analyze metrics, use:"
echo "  cat $METRICS_FILE | column -t -s,"
echo "  # or import into spreadsheet software"
