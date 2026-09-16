#!/bin/bash
# Основной скрипт сборки IP-CSS для Linux/macOS
# Не использует set -e для корректной обработки ошибок

set -o pipefail

# =============================================================================
# Инициализация логирования
# =============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/build-logger.sh"

# Инициализация лога сборки
BUILD_ID="${BUILD_ID:-$(date +%Y%m%d_%H%M%S)}"
init_logging "$BUILD_ID"

log_info "IP-CSS Build Pipeline Starting"
log_info "Build ID: $BUILD_ID"

# =============================================================================
# Константы и конфигурация
# =============================================================================
BUILD_START=$(date +%s)
MAX_ATTEMPTS=3
SAFE_JOBS_CPP=4
SAFE_JOBS_GRADLE=2
SAFE_JOBS_NPM=4

# Флаги сборки (по умолчанию)
BUILD_NATIVE="${BUILD_NATIVE:-true}"
BUILD_KOTLIN="${BUILD_KOTLIN:-true}"
BUILD_WEB="${BUILD_WEB:-true}"
BUILD_DOCKER="${BUILD_DOCKER:-false}"

# =============================================================================
# Функции детекции и подготовки
# =============================================================================

detect_environment() {
    log_stage_start "Environment Detection"
    
    # Определение ОС
    if [[ "$(uname -s)" == "Linux" ]]; then
        OS="linux"
    elif [[ "$(uname -s)" == "Darwin" ]]; then
        OS="macos"
    else
        log_error "Unsupported OS: $(uname -s)"
        log_stage_end "Environment Detection" 1
        exit 1
    fi
    
    log_info "OS: $OS"
    
    # Проверка CPU и RAM
    if [ "$OS" = "linux" ]; then
        CPU_CORES=$(nproc)
        TOTAL_RAM_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
        FREE_RAM_KB=$(grep MemAvailable /proc/meminfo | awk '{print $2}')
    elif [ "$OS" = "macos" ]; then
        CPU_CORES=$(sysctl -n hw.ncpu)
        TOTAL_RAM_KB=$(sysctl -n hw.memsize)
        FREE_RAM_KB=$(vm_stat | awk '/Pages free/ {print int($3 * 4096 / 1024)}')
    fi
    
    FREE_RAM_GB=$((FREE_RAM_KB / 1024 / 1024))
    
    # Расчёт SAFE_JOBS
    SAFE_JOBS_CPP=$((FREE_RAM_GB / 3))
    [ $SAFE_JOBS_CPP -lt 1 ] && SAFE_JOBS_CPP=1
    [ $SAFE_JOBS_CPP -gt $CPU_CORES ] && SAFE_JOBS_CPP=$CPU_CORES
    
    SAFE_JOBS_GRADLE=$((FREE_RAM_GB / 4))
    [ $SAFE_JOBS_GRADLE -lt 1 ] && SAFE_JOBS_GRADLE=1
    [ $SAFE_JOBS_GRADLE -gt 4 ] && SAFE_JOBS_GRADLE=4
    
    SAFE_JOBS_NPM=$((FREE_RAM_GB / 2))
    [ $SAFE_JOBS_NPM -lt 1 ] && SAFE_JOBS_NPM=1
    [ $SAFE_JOBS_NPM -gt 8 ] && SAFE_JOBS_NPM=8
    
    log_info "System: CPU=$CPU_CORES cores, RAM=${FREE_RAM_GB}GB free"
    log_info "SAFE_JOBS: CPP=$SAFE_JOBS_CPP, GRADLE=$SAFE_JOBS_GRADLE, NPM=$SAFE_JOBS_NPM"
    
    log_stage_end "Environment Detection" 0
}

validate_dependencies() {
    log_stage_start "Dependency Validation"
    local missing=0
    
    # Java 17
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -1 | grep -oP 'version "\K[0-9]+')
        if [ "${JAVA_VERSION:-0}" -lt 17 ]; then
            log_error "Java 17+ required, found $JAVA_VERSION"
            missing=$((missing + 1))
        else
            log_success "Java $JAVA_VERSION"
        fi
    else
        log_error "Java not found"
        missing=$((missing + 1))
    fi
    
    # CMake 3.15+
    if command -v cmake &> /dev/null; then
        CMAKE_VERSION=$(cmake --version | head -1 | grep -oP '\d+\.\d+')
        CMAKE_MAJOR=$(echo "$CMAKE_VERSION" | cut -d. -f1)
        CMAKE_MINOR=$(echo "$CMAKE_VERSION" | cut -d. -f2)
        if [ "$CMAKE_MAJOR" -lt 3 ] || ([ "$CMAKE_MAJOR" -eq 3 ] && [ "$CMAKE_MINOR" -lt 15 ]); then
            log_error "CMake 3.15+ required, found $CMAKE_VERSION"
            missing=$((missing + 1))
        else
            log_success "CMake $CMAKE_VERSION"
        fi
    else
        log_error "CMake not found"
        missing=$((missing + 1))
    fi
    
    # Node.js 20+ (согласно package.json engines)
    if command -v node &> /dev/null; then
        NODE_VERSION=$(node --version | cut -d'v' -f2 | cut -d'.' -f1)
        if [ "${NODE_VERSION:-0}" -lt 20 ]; then
            log_error "Node.js 20+ required (see package.json engines), found $NODE_VERSION"
            log_info "Install Node.js 20+: nvm install 20 or https://nodejs.org/"
            missing=$((missing + 1))
        else
            log_success "Node.js $NODE_VERSION"
        fi
    else
        log_error "Node.js not found"
        log_info "Install Node.js 20+: nvm install 20 or https://nodejs.org/"
        missing=$((missing + 1))
    fi
    
    # Git
    if ! command -v git &> /dev/null; then
        log_error "Git not found"
        missing=$((missing + 1))
    else
        log_success "Git $(git --version | cut -d' ' -f3)"
    fi
    
    # Git LFS (опционально, но рекомендуется)
    if command -v git-lfs &> /dev/null; then
        log_success "Git LFS $(git-lfs version | head -1 | cut -d' ' -f3)"
    else
        log_warn "Git LFS not found (large files may fail to checkout)"
    fi
    
    log_metrics "Dependency Validation"
    log_stage_end "Dependency Validation" $([ $missing -gt 0 ] && echo 1 || echo 0)
    
    [ $missing -gt 0 ] && exit 1
}

prepare_environment() {
    log_stage_start "Environment Preparation"
    
    # Создание .env если отсутствует
    if [ ! -f ".env" ] && [ -f ".env.example" ]; then
        log_info "Creating .env from .env.example..."
        cp .env.example .env
        
        # Генерация секретов с fallback для систем без openssl
        if command -v openssl &> /dev/null; then
            DB_PASS=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)
            JWT_SEC=$(openssl rand -base64 64 | tr -dc 'a-zA-Z0-9' | head -c 64)
        elif [ -e /dev/urandom ]; then
            DB_PASS=$(head -c 32 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 32)
            JWT_SEC=$(head -c 64 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 64)
        else
            DB_PASS=$(echo "${RANDOM}${RANDOM}$(date +%s)" | md5sum | head -c 32)
            JWT_SEC=$(echo "${RANDOM}${RANDOM}${RANDOM}$(date +%s)" | md5sum | head -c 64)
        fi
        
        # Валидация сгенерированных секретов
        if [ -z "$DB_PASS" ] || [ ${#DB_PASS} -lt 16 ]; then
            log_error "Failed to generate DB_PASSWORD"
            log_stage_end "Environment Preparation" 1
            exit 1
        fi
        if [ -z "$JWT_SEC" ] || [ ${#JWT_SEC} -lt 32 ]; then
            log_error "Failed to generate JWT_SECRET"
            log_stage_end "Environment Preparation" 1
            exit 1
        fi
        
        sed -i "s/^DB_PASSWORD=.*/DB_PASSWORD=$DB_PASS/" .env
        sed -i "s/^JWT_SECRET=.*/JWT_SECRET=$JWT_SEC/" .env
        log_success ".env created with random secrets"
    fi
    
    # Настройка ccache
    if command -v ccache &> /dev/null; then
        export CC="ccache gcc"
        export CXX="ccache g++"
        export CMAKE_C_COMPILER_LAUNCHER=ccache
        export CMAKE_CXX_COMPILER_LAUNCHER=ccache
        ccache -M 20G
        log_success "ccache enabled with 20GB limit"
    fi
    
    # Подготовка gradle.properties
    if [ -f "gradle.properties" ]; then
        if ! grep -q "org.gradle.caching=true" gradle.properties; then
            echo "org.gradle.caching=true" >> gradle.properties
        fi
        if ! grep -q "org.gradle.parallel=true" gradle.properties; then
            echo "org.gradle.parallel=true" >> gradle.properties
        fi
        log_success "Gradle caching enabled"
    fi
    
    log_metrics "Environment Preparation"
    log_stage_end "Environment Preparation" 0
}

# =============================================================================
# Функции сборки
# =============================================================================

build_native() {
    log_stage_start "Native C++ Build"
    log_metrics "Native Build Start"
    
    cd native
    
    # Проверка vcpkg
    if [ ! -d "vcpkg" ]; then
        log_info "Cloning vcpkg..."
        git clone https://github.com/Microsoft/vcpkg.git || {
            log_error "Failed to clone vcpkg"
            log_stage_end "Native C++ Build" 1
            cd ..
            return 1
        }
        if [ "$OS" = "linux" ] || [ "$OS" = "macos" ]; then
            ./vcpkg/bootstrap-vcpkg.sh || {
                log_error "Failed to bootstrap vcpkg"
                log_stage_end "Native C++ Build" 1
                cd ..
                return 1
            }
        fi
    fi
    
    # Создаём build директорию
    mkdir -p build && cd build
    
    # Конфигурация CMake
    log_info "Configuring CMake..."
    cmake .. \
        -DCMAKE_BUILD_TYPE=RelWithDebInfo \
        -DCMAKE_TOOLCHAIN_FILE=../vcpkg/scripts/buildsystems/vcpkg.cmake \
        -DCMAKE_EXPORT_COMPILE_COMMANDS=ON \
        -DBUILD_SHARED_LIBS=ON \
        -DENABLE_FFMPEG=ON \
        -DENABLE_OPENCV=ON \
        -DENABLE_GPU=OFF \
        -DCMAKE_INSTALL_PREFIX=../install || {
            log_error "CMake configuration failed"
            cat CMakeFiles/CMakeOutput.log
            log_stage_end "Native C++ Build" 1
            cd ../..
            return 1
        }
    
    # Сборка
    log_info "Building with -j $SAFE_JOBS_CPP..."
    cmake --build . -j $SAFE_JOBS_CPP --target install 2>&1 | tee ../../native_build.log
    
    local exit_code=${PIPESTATUS[0]}
    cd ../..
    
    log_metrics "Native Build End"
    log_stage_end "Native C++ Build" $exit_code
    
    if [ $exit_code -eq 0 ]; then
        log_success "Native build completed"
        return 0
    else
        log_error "Native build failed with exit code $exit_code"
        return 1
    fi
}

build_gradle() {
    log_stage_start "Gradle Kotlin Build"
    log_metrics "Gradle Build Start"
    
    local GRADLE_TASKS
    GRADLE_TASKS=$(get_gradle_tasks "full")
    
    log_info "Running: ./gradlew $GRADLE_TASKS"
    
    ./gradlew $GRADLE_TASKS \
        --no-daemon \
        --max-workers=$SAFE_JOBS_GRADLE \
        -Dorg.gradle.jvmargs="-Xmx6g -XX:MaxMetaspaceSize=768m" \
        -Porg.gradle.parallel=true \
        2>&1 | tee gradle_build.log
    
    local exit_code=${PIPESTATUS[0]}
    
    log_metrics "Gradle Build End"
    log_stage_end "Gradle Kotlin Build" $exit_code
    
    if [ $exit_code -eq 0 ]; then
        log_success "Gradle build completed"
        return 0
    else
        log_error "Gradle build failed with exit code $exit_code"
        tail -100 gradle_build.log | grep -i "error\|exception\|failed" | head -20 | while read line; do
            log_warn "$line"
        done
        return 1
    fi
}

build_web() {
    log_stage_start "Web Build"
    log_metrics "Web Build Start"
    
    cd server/web
    
    if [ ! -d "node_modules" ]; then
        log_info "Installing npm dependencies..."
        npm ci --legacy-peer-deps --fetch-timeout=600000 || {
            log_warn "npm ci failed, trying npm install..."
            npm install --legacy-peer-deps
        }
    fi
    
    export NODE_OPTIONS="--max-old-space-size=4096"
    npm run build 2>&1 | tee ../../web_build.log
    
    local exit_code=${PIPESTATUS[0]}
    cd ../..
    
    log_metrics "Web Build End"
    log_stage_end "Web Build" $exit_code
    
    if [ $exit_code -eq 0 ]; then
        if [ -d "server/web/.next" ] && [ -f "server/web/.next/BUILD_ID" ]; then
            log_success "Web build completed successfully"
            return 0
        else
            log_error "Web build incomplete - .next directory missing"
            return 1
        fi
    else
        log_error "Web build failed with exit code $exit_code"
        tail -100 web_build.log | grep -i "error\|failed" | head -20 | while read line; do
            log_warn "$line"
        done
        return 1
    fi
}

build_web() {
    echo "=== Building Web Interface ==="
    
    cd server/web
    
    # Проверка node_modules
    if [ ! -d "node_modules" ]; then
        echo "[INFO] Installing npm dependencies..."
        npm ci --legacy-peer-deps --fetch-timeout=600000 || {
            echo "[WARN] npm ci failed, trying npm install..."
            npm install --legacy-peer-deps
        }
    fi
    
    # Установка переменной памяти
    export NODE_OPTIONS="--max-old-space-size=4096"
    
    # Сборка
    npm run build 2>&1 | tee ../../web_build.log
    
    local exit_code=${PIPESTATUS[0]}
    cd ../..
    
    if [ $exit_code -eq 0 ]; then
        if [ -d "server/web/.next" ] && [ -f "server/web/.next/BUILD_ID" ]; then
            echo "[INFO] Web build completed successfully"
            return 0
        else
            echo "[ERROR] Web build incomplete - .next directory missing"
            return 1
        fi
    else
        echo "[ERROR] Web build failed with exit code $exit_code"
        tail -100 web_build.log | grep -i "error\|failed" | head -20
        return 1
    fi
}
    
# =============================================================================
# Обработка ошибок
# =============================================================================

handle_error() {
    local LOG_FILE=$1
    local STAGE=$2
    local EXIT_CODE=$3
    local ATTEMPT=${4:-1}
    
    log_error "Build stage '$STAGE' failed with exit code $EXIT_CODE (attempt $ATTEMPT)"
    
    if [ -f "$LOG_FILE" ]; then
        log_info "Last 50 lines of log:"
        tail -50 "$LOG_FILE" | while read line; do
            log_debug "$line"
        done
    fi
    
    case "$STAGE" in
        "native")
            if grep -q "No space left on device" "$LOG_FILE" 2>/dev/null; then
                log_info "Cleaning cache to free space..."
                ccache -C 2>/dev/null || true
                rm -rf native/build
            elif grep -q "Killed" "$LOG_FILE" 2>/dev/null; then
                log_info "Out of memory - reducing SAFE_JOBS_CPP"
                SAFE_JOBS_CPP=$((SAFE_JOBS_CPP / 2))
                [ $SAFE_JOBS_CPP -lt 1 ] && SAFE_JOBS_CPP=1
            fi
            ;;
        "gradle")
            if grep -q "OutOfMemoryError" "$LOG_FILE" 2>/dev/null; then
                log_info "Out of memory - reducing SAFE_JOBS_GRADLE"
                SAFE_JOBS_GRADLE=1
                export GRADLE_OPTS="-Xmx8g"
            fi
            ;;
        "web")
            if grep -q "JavaScript heap out of memory" "$LOG_FILE" 2>/dev/null; then
                log_info "Increasing Node.js memory"
                export NODE_OPTIONS="--max-old-space-size=8192"
            fi
            ;;
    esac
    
    [ $ATTEMPT -ge $MAX_ATTEMPTS ] && return 1
    return 0
}

validate_artifacts() {
    log_stage_start "Artifact Validation"
    
    local errors=0
    
    # Native libraries
    log_info "Checking native libraries..."
    if find native/video-processing/lib native/build/lib -name "*.so" -o -name "*.dll" -o -name "*.dylib" 2>/dev/null | grep -q .; then
        log_success "Native libraries found"
    else
        log_warn "No native libraries found (may be expected if BUILD_NATIVE=false)"
    fi
    
    # Gradle JARs
    log_info "Checking Gradle artifacts..."
    if find server/api/build -name "*.jar" 2>/dev/null | grep -q .; then
        log_success "API JAR found"
    else
        log_warn "No API JAR found"
    fi
    
    # Web build
    log_info "Checking web artifacts..."
    if [ -d "server/web/.next" ] && [ -f "server/web/.next/BUILD_ID" ]; then
        log_success "Web build validated"
    else
        log_warn "Web build directory not found (may be expected if BUILD_WEB=false)"
    fi
    
    log_metrics "Artifact Validation"
    log_stage_end "Artifact Validation" $errors
    
    return 0
}

generate_summary() {
    local build_end=$(date +%s)
    local duration=$((build_end - BUILD_START))
    local duration_min=$((duration / 60))
    local duration_sec=$((duration % 60))
    
    log_info ""
    log_info "================================================================"
    log_info "                    BUILD SUMMARY"
    log_info "================================================================"
    log_info "Status:                    SUCCESS"
    log_info "Duration:                  ${duration_min}m ${duration_sec}s"
    log_info "Start time:                $(date -d "@$BUILD_START" 2>/dev/null || date -r $BUILD_START)"
    log_info "End time:                  $(date)"
    log_info ""
    log_info "System:"
    log_info "  OS:                      $OS"
    log_info "  CPU:                     $CPU_CORES cores"
    log_info "  RAM (free):              ${FREE_RAM_GB}GB"
    log_info ""
    log_info "Configuration:"
    log_info "  SAFE_JOBS_CPP:           $SAFE_JOBS_CPP"
    log_info "  SAFE_JOBS_GRADLE:        $SAFE_JOBS_GRADLE"
    log_info "  SAFE_JOBS_NPM:           $SAFE_JOBS_NPM"
    log_info "  BUILD_NATIVE:            $BUILD_NATIVE"
    log_info "  BUILD_KOTLIN:            $BUILD_KOTLIN"
    log_info "  BUILD_WEB:               $BUILD_WEB"
    log_info "================================================================"
}

# =============================================================================
# Валидация и сводка
# =============================================================================

validate_artifacts() {
    # Импортируем функцию из validate-artifacts.sh
    source "$(dirname "$0")/validate-artifacts.sh"
    validate_artifacts
}

generate_summary() {
    local build_end=$(date +%s)
    local duration=$((build_end - BUILD_START))
    local duration_min=$((duration / 60))
    local duration_sec=$((duration % 60))
    
    echo ""
    echo "================================================================"
    echo "                    BUILD SUMMARY"
    echo "================================================================"
    echo "Status:                    SUCCESS"
    echo "Duration:                  ${duration_min}m ${duration_sec}s"
    echo "Start time:                $(date -d "@$BUILD_START" 2>/dev/null || date -r $BUILD_START)"
    echo "End time:                  $(date)"
    echo ""
    echo "System:"
    echo "  OS:                      $OS"
    echo "  CPU:                     $CPU_CORES cores"
    echo "  RAM (free):              ${FREE_RAM_GB}GB"
    echo ""
    echo "Configuration:"
    echo "  SAFE_JOBS_CPP:           $SAFE_JOBS_CPP"
    echo "  SAFE_JOBS_GRADLE:        $SAFE_JOBS_GRADLE"
    echo "  SAFE_JOBS_NPM:           $SAFE_JOBS_NPM"
    echo "  BUILD_NATIVE:            $BUILD_NATIVE"
    echo "  BUILD_KOTLIN:            $BUILD_KOTLIN"
    echo "  BUILD_WEB:               $BUILD_WEB"
    echo ""
    echo "Log files:"
    echo "  Native:                  native_build.log"
    echo "  Gradle:                  gradle_build.log"
    echo "  Web:                     web_build.log"
    echo "================================================================"
}

# =============================================================================
# Основной pipeline
# =============================================================================

main() {
    log_info "================================================================"
    log_info "         IP-CSS Build Pipeline - Starting"
    log_info "================================================================"
    
    # 1. Детекция окружения
    detect_environment
    validate_dependencies || {
        log_error "Dependency validation failed"
        finalize_logging 1
        exit 1
    }
    prepare_environment
    
    # 2. Основной цикл сборки
    local ATTEMPT=1
    while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
        log_info "=== Build Attempt $ATTEMPT/$MAX_ATTEMPTS ==="
        
        # Native
        if [ "$BUILD_NATIVE" = "true" ]; then
            if ! build_native; then
                if ! handle_error "native_build.log" "native" $? $ATTEMPT; then
                    log_error "Native build failed after $MAX_ATTEMPTS attempts"
                    finalize_logging 1
                    exit 1
                fi
                ATTEMPT=$((ATTEMPT + 1))
                continue
            fi
        fi
        
        # Gradle
        if [ "$BUILD_KOTLIN" = "true" ]; then
            if ! build_gradle; then
                if ! handle_error "gradle_build.log" "gradle" $? $ATTEMPT; then
                    log_error "Gradle build failed after $MAX_ATTEMPTS attempts"
                    finalize_logging 1
                    exit 1
                fi
                ATTEMPT=$((ATTEMPT + 1))
                continue
            fi
        fi
        
        # Web
        if [ "$BUILD_WEB" = "true" ]; then
            if ! build_web; then
                if ! handle_error "web_build.log" "web" $? $ATTEMPT; then
                    log_error "Web build failed after $MAX_ATTEMPTS attempts"
                    finalize_logging 1
                    exit 1
                fi
                ATTEMPT=$((ATTEMPT + 1))
                continue
            fi
        fi
        
        break
    done
    
    # 3. Валидация
    if ! validate_artifacts; then
        log_error "Artifact validation failed"
        finalize_logging 1
        exit 1
    fi
    
    # 4. Сводка
    generate_summary
    
    # 5. Завершение логирования
    finalize_logging 0
    
    log_success "Pipeline completed successfully"
    exit 0
}

# Запуск
main "$@"
