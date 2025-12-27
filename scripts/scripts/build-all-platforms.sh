#!/bin/bash
# Скрипт для сборки всех платформ

set -e

echo "Building IP Camera Surveillance System for all platforms"
echo "========================================================"

# Цвета для вывода
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Функция для вывода сообщений
info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Проверка зависимостей
check_dependencies() {
    info "Checking dependencies..."
    
    if ! command -v java &> /dev/null; then
        error "Java is not installed"
        exit 1
    fi
    
    if ! command -v ./gradlew &> /dev/null; then
        error "Gradle wrapper not found"
        exit 1
    fi
    
    info "Dependencies OK"
}

# Сборка Android
build_android() {
    info "Building Android..."
    ./gradlew :android:assembleDebug :android:assembleRelease
    if [ $? -eq 0 ]; then
        info "Android build successful"
    else
        error "Android build failed"
        exit 1
    fi
}

# Сборка Desktop
build_desktop() {
    info "Building Desktop..."
    ./gradlew :desktop:build
    if [ $? -eq 0 ]; then
        info "Desktop build successful"
    else
        error "Desktop build failed"
        exit 1
    fi
}

# Сборка Server
build_server() {
    info "Building Server..."
    ./gradlew :server:build
    if [ $? -eq 0 ]; then
        info "Server build successful"
    else
        error "Server build failed"
        exit 1
    fi
}

# Сборка Native библиотек
build_native() {
    info "Building Native libraries..."
    
    if [ ! -d "native/build" ]; then
        mkdir -p native/build
    fi
    
    cd native/build
    
    if command -v cmake &> /dev/null; then
        cmake ..
        cmake --build . --config Release
        if [ $? -eq 0 ]; then
            info "Native libraries build successful"
        else
            warning "Native libraries build failed (may be optional)"
        fi
    else
        warning "CMake not found, skipping native libraries"
    fi
    
    cd ../..
}

# Сборка Web интерфейса
build_web() {
    info "Building Web interface..."
    
    if [ -d "server/web" ]; then
        cd server/web
        
        if command -v npm &> /dev/null; then
            npm install
            npm run build
            if [ $? -eq 0 ]; then
                info "Web interface build successful"
            else
                warning "Web interface build failed"
            fi
        else
            warning "npm not found, skipping web interface"
        fi
        
        cd ../..
    fi
}

# Основная функция
main() {
    check_dependencies
    
    # Параметры командной строки
    PLATFORMS="${@:-all}"
    
    if [ "$PLATFORMS" == "all" ] || [[ "$PLATFORMS" == *"android"* ]]; then
        build_android
    fi
    
    if [ "$PLATFORMS" == "all" ] || [[ "$PLATFORMS" == *"desktop"* ]]; then
        build_desktop
    fi
    
    if [ "$PLATFORMS" == "all" ] || [[ "$PLATFORMS" == *"server"* ]]; then
        build_server
    fi
    
    if [ "$PLATFORMS" == "all" ] || [[ "$PLATFORMS" == *"native"* ]]; then
        build_native
    fi
    
    if [ "$PLATFORMS" == "all" ] || [[ "$PLATFORMS" == *"web"* ]]; then
        build_web
    fi
    
    info "Build completed successfully!"
}

# Запуск
main "$@"

