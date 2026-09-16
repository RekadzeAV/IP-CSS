#!/bin/bash
# Валидация артефактов сборки IP-CSS

validate_artifacts() {
    echo "================================================================"
    echo "                    VALIDATING BUILD ARTIFACTS"
    echo "================================================================"
    local errors=0 warnings=0
    
    echo ""
    echo "[INFO] Checking native libraries..."
    local native_found=0
    find native/video-processing/lib native/build/lib -name "*.so" -o -name "*.dll" 2>/dev/null | while read lib; do
        echo "  [OK] $lib"
        native_found=1
    done
    [ $native_found -eq 0 ] && [ "${BUILD_NATIVE:-true}" = "true" ] && { echo "[ERROR] Native libraries not found"; errors=$((errors + 1)); } || warnings=$((warnings + 1))
    
    echo ""
    echo "[INFO] Checking Gradle artifacts..."
    local jar_found=0
    find server/api/build -name "*.jar" 2>/dev/null | while read jar; do
        echo "  [OK] $jar"
        jar_found=1
    done
    [ $jar_found -eq 0 ] && [ "${BUILD_KOTLIN:-true}" = "true" ] && { echo "[ERROR] API JAR not found"; errors=$((errors + 1)); } || warnings=$((warnings + 1))
    
    echo ""
    echo "[INFO] Checking web artifacts..."
    if [ -f "server/web/.next/BUILD_ID" ]; then
        echo "  [OK] Web build validated - BUILD_ID exists"
    else
        [ "${BUILD_WEB:-true}" = "true" ] && { echo "[ERROR] Web build incomplete"; errors=$((errors + 1)); } || warnings=$((warnings + 1))
    fi
    
    echo ""
    echo "================================================================"
    echo "                    VALIDATION SUMMARY"
    echo "================================================================"
    echo "Errors:   $errors"
    echo "Warnings: $warnings"
    
    if [ $errors -gt 0 ]; then
        echo "[ERROR] Validation FAILED with $errors errors"
        return 1
    else
        echo "[INFO] Validation PASSED ($warnings warnings)"
        return 0
    fi
}
