#!/bin/bash
# Валидация артефактов сборки IP-CSS
# Соответствует реальной структуре проекта

set -euo pipefail

validate_artifacts() {
    local errors=0
    echo "=== Validating Build Artifacts ==="

    # Native libraries (реальный путь из CMakeLists.txt)
    echo "[INFO] Checking native libraries..."
    local native_libs_found=0
    if [ -d "native/video-processing/lib" ]; then
        while IFS= read -r lib; do
            echo "[OK] Found: $lib"
            native_libs_found=1
        done < <(find native/video-processing/lib -name "*.so" -o -name "*.dylib" -o -name "*.dll" 2>/dev/null || true)
    fi
    if [ -d "native/build" ]; then
        while IFS= read -r lib; do
            echo "[OK] Found: $lib"
            native_libs_found=1
        done < <(find native/build -name "*.so" -o -name "*.dylib" -o -name "*.dll" 2>/dev/null || true)
    fi
    if [ $native_libs_found -eq 0 ]; then
        echo "[WARN] No native libraries found (may be expected if BUILD_NATIVE=false)"
    fi

    # Gradle JARs (реальные пути из build.gradle.kts)
    echo "[INFO] Checking Gradle artifacts..."
    local jar_found=0
    while IFS= read -r jar; do
        echo "[OK] Found: $jar"
        jar_found=1
    done < <(find . -path "*/server/api/build/outputs/*.jar" -o -path "*/server/api/build/libs/*.jar" 2>/dev/null || true)
    
    if [ $jar_found -eq 0 ]; then
        echo "[WARN] No API JAR found - check :server:api:assemble task"
        # Не прерывать - может быть нормальным для partial builds
    fi

    # Kotlin/Native binaries
    echo "[INFO] Checking Kotlin/Native binaries..."
    while IFS= read -r bin; do
        echo "[OK] Found: $bin"
    done < <(find . -path "*/shared/build/bin/*" -o -path "*/core/*/build/bin/*" 2>/dev/null || true)

    # Web build (проверка .next)
    echo "[INFO] Checking web artifacts..."
    if [ -d "server/web/.next" ]; then
        if [ -f "server/web/.next/BUILD_ID" ]; then
            echo "[OK] Web build validated - BUILD_ID exists"
        else
            echo "[ERROR] Web build incomplete - BUILD_ID missing"
            errors=$((errors + 1))
        fi
    else
        echo "[WARN] Web build directory not found: server/web/.next (may be expected if BUILD_WEB=false)"
    fi

    # Docker images (если BUILD_DOCKER=true)
    if [ "${BUILD_DOCKER:-false}" = "true" ]; then
        echo "[INFO] Checking Docker images..."
        if command -v docker &> /dev/null; then
            docker-compose images 2>/dev/null | grep -q "surveillance" || {
                echo "[WARN] No Docker images found"
            }
        fi
    fi

    if [ $errors -gt 0 ]; then
        echo "[ERROR] Validation failed with $errors errors"
        return 1
    else
        echo "[INFO] All artifacts validated successfully"
        return 0
    fi
}
