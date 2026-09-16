#!/bin/bash
# Bash script to run all CI/CD checks locally
# Usage: ./scripts/ci-run-all.sh [--profile mvp|staging|strict] [--skip-native] [--skip-tests]

set -e

PROFILE="mvp"
SKIP_NATIVE=false
SKIP_TESTS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        --skip-native)
            SKIP_NATIVE=true
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --help)
            echo "Run all CI/CD checks locally"
            echo ""
            echo "Usage: ./scripts/ci-run-all.sh [--profile mvp|staging|strict] [--skip-native] [--skip-tests]"
            echo ""
            echo "Options:"
            echo "  --profile mvp|staging|strict  Verification profile (default: mvp)"
            echo "  --skip-native  Skip native library build checks"
            echo "  --skip-tests  Skip test execution"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "========================================"
echo "  CI/CD Local Runner - Profile: $PROFILE"
echo "========================================"
echo ""

START_TIME=$(date +%s)
STEP_RESULTS=()

invoke_step() {
    local name="$1"
    shift
    
    echo "==> $name"
    local step_start=$(date +%s)
    
    if "$@"; then
        local step_end=$(date +%s)
        local duration=$((step_end - step_start))
        STEP_RESULTS+=("PASS|$duration|$name")
        echo "   ✅ PASS"
    else
        local step_end=$(date +%s)
        local duration=$((step_end - step_start))
        STEP_RESULTS+=("FAIL|$duration|$name")
        echo "   ❌ FAIL"
        if [ "$PROFILE" = "strict" ]; then
            echo "Strict mode: failing on step failure"
            exit 1
        fi
    fi
}

# 1. KMP Phase 1 Python checks
invoke_step "KMP Phase 1 Python Checks" bash -c '
    python3 scripts/ci/check-commonmain-forbidden-imports.py . &&
    python3 scripts/ci/check-security-expect-actual-signatures.py . &&
    python3 scripts/ci/check-no-jvm-deps-in-native-source-sets.py . &&
    python3 scripts/ci/check-video-runtime-matrix-config.py --root . &&
    python3 scripts/ci/validate-video-e2e-profile.py --root .
'

# 2. Gradle Metadata compilation
invoke_step "Gradle Metadata Compilation" bash -c './gradlew :core:common:compileKotlinMetadata :core:network:compileKotlinMetadata :shared:compileKotlinMetadata --no-daemon'

# 3. Desktop Tests
if [ "$SKIP_TESTS" = false ]; then
    invoke_step "Desktop Tests" bash -c './gradlew :core:common:desktopTest :core:network:desktopTest --no-daemon'
fi

# 4. Native Linux Build
if [ "$SKIP_NATIVE" = false ]; then
    invoke_step "Native Linux Build" bash -c '
        if ! command -v cmake &> /dev/null; then
            echo "CMake not found"
            exit 1
        fi
        
        if ! command -v g++ &> /dev/null; then
            echo "g++ not found"
            exit 1
        fi
        
        ./scripts/build-video-processing-lib.sh linux x64 Release
        
        if [ ! -f "native/video-processing/lib/linux/x64/libvideo_processing.so" ]; then
            echo "libvideo_processing.so not found"
            exit 1
        fi
        
        ./gradlew :core:network:cinteropVideoProcessingNativeLinux --no-daemon
    '
fi

# 5. Android Build (staging/strict only)
if [[ "$PROFILE" == "staging" || "$PROFILE" == "strict" ]]; then
    invoke_step "Android Build" bash -c './gradlew :android:app:assembleDebug --no-daemon'
fi

# 6. Server Build (staging/strict only)
if [[ "$PROFILE" == "staging" || "$PROFILE" == "strict" ]]; then
    invoke_step "Server API Build" bash -c './gradlew :server:api:build --no-daemon'
fi

# Generate report
END_TIME=$(date +%s)
TOTAL_DURATION=$((END_TIME - START_TIME))

REPORT_DIR="diagnostics/ci"
mkdir -p "$REPORT_DIR"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
REPORT_PATH="$REPORT_DIR/local-run-report-$TIMESTAMP.md"

{
    echo "# CI/CD Local Run Report"
    echo ""
    echo "- Profile: **$PROFILE**"
    echo "- Start: $(date -d @$START_TIME '+%Y-%m-%d %H:%M:%S')"
    echo "- End: $(date -d @$END_TIME '+%Y-%m-%d %H:%M:%S')"
    echo "- Total Duration: $(echo "scale=1; $TOTAL_DURATION / 60" | bc) minutes"
    echo ""
    echo "| Step | Status | Duration (s) |"
    echo "|------|--------|--------------|"
    
    for result in "${STEP_RESULTS[@]}"; do
        IFS='|' read -r status duration name <<< "$result"
        echo "| $name | $status | $duration |"
    done
    
    echo ""
    echo "## Summary"
    echo ""
    PASSED_COUNT=$(echo "${STEP_RESULTS[@]}" | grep -o "PASS" | wc -l)
    TOTAL_COUNT=${#STEP_RESULTS[@]}
    echo "- Passed: $PASSED_COUNT / $TOTAL_COUNT"
    if [ "$PASSED_COUNT" -eq "$TOTAL_COUNT" ]; then
        echo "- Overall: ✅ SUCCESS"
    else
        echo "- Overall: ⚠️ PARTIAL"
    fi
} > "$REPORT_PATH"

echo ""
echo "========================================"
echo "  CI/CD Run Complete"
echo "========================================"

PASSED_COUNT=$(echo "${STEP_RESULTS[@]}" | grep -o "PASS" | wc -l)
TOTAL_COUNT=${#STEP_RESULTS[@]}

echo "Passed: $PASSED_COUNT / $TOTAL_COUNT"
echo "Total Duration: $(echo "scale=1; $TOTAL_DURATION / 60" | bc) minutes"
echo "Report: $REPORT_PATH"

if [ "$PASSED_COUNT" -ne "$TOTAL_COUNT" ] && [ "$PROFILE" = "strict" ]; then
    exit 1
fi

exit 0
