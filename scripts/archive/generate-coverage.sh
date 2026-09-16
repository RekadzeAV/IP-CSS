#!/bin/bash
# Генерация coverage report для RTSP Client
# IP-CSS RTSP Client - Coverage Script

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$PROJECT_ROOT/build"
COVERAGE_DIR="$PROJECT_ROOT/coverage"

echo "========================================"
echo "  RTSP Client Coverage Report Generator"
echo "========================================"

# Очистка
rm -rf "$BUILD_DIR"
rm -rf "$COVERAGE_DIR"
mkdir -p "$COVERAGE_DIR"

# Сборка с coverage
echo ""
echo "[1/4] Building with coverage..."
cmake -B "$BUILD_DIR" \
  -DCMAKE_BUILD_TYPE=Debug \
  -DCMAKE_CXX_FLAGS="--coverage -fprofile-arcs -ftest-coverage" \
  -DCMAKE_C_FLAGS="--coverage -fprofile-arcs -ftest-coverage" \
  -DBUILD_HW_DECODER_TESTS=ON \
  -DBUILD_INTEGRATION_TESTS=ON

cmake --build "$BUILD_DIR" -j$(nproc)

# Запуск тестов
echo ""
echo "[2/4] Running tests..."
cd "$BUILD_DIR"
ctest --output-on-failure

# Генерация coverage report
echo ""
echo "[3/4] Generating coverage report..."
lcov --capture --directory "$BUILD_DIR" --output-file "$COVERAGE_DIR/coverage.info"
lcov --remove "$COVERAGE_DIR/coverage.info" '/usr/*' '*/vcpkg/*' '*/test/*' --output-file "$COVERAGE_DIR/coverage.info"

# Summary
echo ""
echo "[4/4] Coverage summary..."
lcov --list "$COVERAGE_DIR/coverage.info"

# HTML report
echo ""
echo "Generating HTML report..."
genhtml "$COVERAGE_DIR/coverage.info" --output-directory "$COVERAGE_DIR/html"

# Результат
echo ""
echo "========================================"
echo "  Coverage Report Generated!"
echo "========================================"
echo ""
echo "HTML Report: $COVERAGE_DIR/html/index.html"
echo "Info File:   $COVERAGE_DIR/coverage.info"
echo ""

# Проверка порога
TOTAL_COVERAGE=$(lcov --list "$COVERAGE_DIR/coverage.info" | grep -E "^\s*total" | awk '{print $NF}' | tr -d '%')
echo "Total Coverage: ${TOTAL_COVERAGE}%"

if [ -n "$TOTAL_COVERAGE" ]; then
    THRESHOLD=30
    if (( $(echo "$TOTAL_COVERAGE < $THRESHOLD" | bc -l) )); then
        echo ""
        echo "⚠️  Warning: Coverage below threshold ($THRESHOLD%)"
    else
        echo ""
        echo "✅ Coverage above threshold ($THRESHOLD%)"
    fi
fi

echo ""
echo "Done!"
