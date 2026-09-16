#!/bin/bash
# Скрипт для анализа результатов тестов (Linux/macOS)
# Использование: ./scripts/analyze-test-results.sh <test_output_file> [--html] [--markdown]

set -e

if [ $# -lt 1 ]; then
    echo "Usage: ./scripts/analyze-test-results.sh <test_output_file> [options]"
    echo ""
    echo "Options:"
    echo "  --html, -h        Generate HTML report"
    echo "  --markdown, -m    Generate Markdown report"
    echo "  --help            Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./scripts/analyze-test-results.sh test_results.txt --html"
    echo "  ./scripts/analyze-test-results.sh test_results.txt --markdown"
    exit 1
fi

INPUT_FILE="$1"
shift

HTML=false
MARKDOWN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --html|-h)
            HTML=true
            shift
            ;;
        --markdown|-m)
            MARKDOWN=true
            shift
            ;;
        --help)
            echo "Usage: ./scripts/analyze-test-results.sh <test_output_file> [options]"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_BUILD_DIR="$PROJECT_ROOT/native/video-processing/test/build"
ANALYZER_EXE="$TEST_BUILD_DIR/test_analyzer"

# Проверка входного файла
if [ ! -f "$INPUT_FILE" ]; then
    echo "❌ Input file not found: $INPUT_FILE"
    exit 1
fi

# Проверка анализатора
if [ ! -f "$ANALYZER_EXE" ]; then
    echo "⚠️  Test analyzer not found: $ANALYZER_EXE"
    echo "   Building analyzer..."

    cd "$TEST_BUILD_DIR"
    cmake --build . --target test_analyzer --config Release

    if [ ! -f "$ANALYZER_EXE" ]; then
        echo "❌ Analyzer not available. Please build tests first."
        exit 1
    fi
fi

echo "=== Analyzing Test Results ==="
echo "Input file: $INPUT_FILE"
echo ""

# Формирование аргументов
ANALYZER_ARGS=("$INPUT_FILE")
if [ "$HTML" = true ]; then
    ANALYZER_ARGS+=("--html")
fi
if [ "$MARKDOWN" = true ]; then
    ANALYZER_ARGS+=("--markdown")
fi

# Запуск анализатора
cd "$TEST_BUILD_DIR"
"$ANALYZER_EXE" "${ANALYZER_ARGS[@]}"

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "✅ Analysis complete"

    if [ "$HTML" = true ]; then
        HTML_FILE="${INPUT_FILE}.html"
        if [ -f "$HTML_FILE" ]; then
            echo "HTML report: $HTML_FILE"
        fi
    fi

    if [ "$MARKDOWN" = true ]; then
        MD_FILE="${INPUT_FILE}.md"
        if [ -f "$MD_FILE" ]; then
            echo "Markdown report: $MD_FILE"
        fi
    fi
else
    echo ""
    echo "❌ Analysis failed"
    exit $EXIT_CODE
fi
