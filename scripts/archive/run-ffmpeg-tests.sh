#!/bin/bash
# Скрипт для запуска тестов FFmpeg декодирования (Linux/macOS)
# Использование: ./scripts/run-ffmpeg-tests.sh [--unit] [--integration] [--config <file>]

set -e

UNIT=false
INTEGRATION=false
CONFIG="native/video-processing/test/test_config.json"
HELP=false

# Парсинг аргументов
while [[ $# -gt 0 ]]; do
    case $1 in
        --unit|-u)
            UNIT=true
            shift
            ;;
        --integration|-i)
            INTEGRATION=true
            shift
            ;;
        --config|-c)
            CONFIG="$2"
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
    echo "Usage: ./scripts/run-ffmpeg-tests.sh [options]"
    echo ""
    echo "Options:"
    echo "  --unit, -u          Run unit tests only"
    echo "  --integration, -i   Run integration tests (requires --unit or runs all)"
    echo "  --config, -c        Path to test config file (default: test_config.json)"
    echo "  --help, -h          Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./scripts/run-ffmpeg-tests.sh --unit"
    echo "  ./scripts/run-ffmpeg-tests.sh --integration --config my_config.json"
    echo "  ./scripts/run-ffmpeg-tests.sh --unit --integration"
    exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_BUILD_DIR="$PROJECT_ROOT/native/video-processing/test/build"

echo "=== FFmpeg Testing Script ==="
echo ""

# Проверка наличия собранных тестов
TEST_EXECUTABLE="$TEST_BUILD_DIR/video_processing_tests"
if [ ! -f "$TEST_EXECUTABLE" ]; then
    echo "❌ Test executable not found: $TEST_EXECUTABLE"
    echo ""
    echo "Please build tests first:"
    echo "  cd native/video-processing/test"
    echo "  mkdir -p build && cd build"
    echo "  cmake .. -DENABLE_FFMPEG=ON"
    echo "  cmake --build . --config Release"
    exit 1
fi

echo "✅ Test executable found"
echo ""

# Определение режима запуска
if [ "$UNIT" = false ] && [ "$INTEGRATION" = false ]; then
    UNIT=true
    INTEGRATION=true
    echo "No specific mode specified, running all tests"
    echo ""
fi

# Формирование аргументов
TEST_ARGS=()

if [ "$INTEGRATION" = true ]; then
    TEST_ARGS+=("--integration")

    if [ -n "$CONFIG" ]; then
        if [ -f "$CONFIG" ]; then
            TEST_ARGS+=("--config")
            TEST_ARGS+=("$CONFIG")
            echo "Using config: $CONFIG"
        elif [ -f "$PROJECT_ROOT/$CONFIG" ]; then
            TEST_ARGS+=("--config")
            TEST_ARGS+=("$PROJECT_ROOT/$CONFIG")
            echo "Using config: $PROJECT_ROOT/$CONFIG"
        else
            echo "⚠️  Config file not found: $CONFIG"
            echo "   Using default configuration"
        fi
    fi
fi

echo ""
echo "=== Running Tests ==="
echo "Executable: $TEST_EXECUTABLE"
if [ ${#TEST_ARGS[@]} -gt 0 ]; then
    echo "Arguments: ${TEST_ARGS[*]}"
fi
echo ""

# Запуск тестов
cd "$TEST_BUILD_DIR"

OUTPUT_FILE="test_results_$(date +%Y%m%d_%H%M%S).txt"

echo "Running tests..."
echo "Results will be saved to: $OUTPUT_FILE"
echo ""

"$TEST_EXECUTABLE" "${TEST_ARGS[@]}" | tee "$OUTPUT_FILE"

EXIT_CODE=${PIPESTATUS[0]}

echo ""
echo "=== Test Results ==="
if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ All tests passed!"
else
    echo "❌ Some tests failed (exit code: $EXIT_CODE)"
fi

echo "Results saved to: $OUTPUT_FILE"

exit $EXIT_CODE
