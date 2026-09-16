#!/bin/bash
# Bash скрипт для сборки RTSP библиотеки на macOS (локально)
# Требует: Xcode 14+, CMake 3.20+, FFmpeg 6.0+

set -e

# Цветовой вывод
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Параметры
CONFIGURATION="${1:-Release}"
CLEAN="${2:-false}"
TARGET_ARCH="${3:-auto}"

print_header() {
    echo -e "${CYAN}=========================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}=========================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_step() {
    echo -e "${CYAN}🔹 $1${NC}"
}

# Проверка команды
command_exists() {
    command -v "$1" &> /dev/null
}

# Проверка архитектуры
detect_arch() {
    if [ "$(uname -m)" = "arm64" ]; then
        echo "arm64"
    else
        echo "x86_64"
    fi
}

# Проверка требований
check_requirements() {
    print_step "Проверка требований..."
    
    local missing_deps=()
    
    # Проверка CMake
    if ! command_exists cmake; then
        missing_deps+=("CMake")
        print_warning "CMake не найден"
    else
        local cmake_version=$(cmake --version | head -n 1 | grep -oP '\d+\.\d+\.\d+')
        print_success "CMake $cmake_version"
    fi
    
    # Проверка Xcode Command Line Tools
    if ! command_exists xcodebuild; then
        missing_deps+=("Xcode Command Line Tools")
        print_warning "Xcode Command Line Tools не установлены"
        print_info "Установите: xcode-select --install"
    else
        local xcode_version=$(xcodebuild -version | head -n 1)
        print_success "$xcode_version"
    fi
    
    # Проверка FFmpeg
    if ! command_exists ffmpeg; then
        missing_deps+=("FFmpeg")
        print_warning "FFmpeg не найден"
        print_info "Установите: brew install ffmpeg"
    else
        local ffmpeg_version=$(ffmpeg -version | head -n 1 | grep -oP 'ffmpeg version \K\S+')
        print_success "FFmpeg $ffmpeg_version"
    fi
    
    # Проверка Homebrew (опционально, но рекомендуется)
    if command_exists brew; then
        print_success "Homebrew установлен"
    else
        print_info "Homebrew не установлен (рекомендуется: /bin/bash -c \"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\")"
    fi
    
    # Проверка Java (для JNI)
    if [ -n "$JAVA_HOME" ]; then
        print_success "JAVA_HOME: $JAVA_HOME"
    else
        if command_exists java; then
            local java_version=$(java -version 2>&1 | head -n 1)
            print_success "Java: $java_version"
        else
            missing_deps+=("Java 17+")
            print_warning "Java не найдена"
            print_info "Установите: brew install openjdk@17"
        fi
    fi
    
    # Проверка pkg-config
    if ! command_exists pkg-config; then
        missing_deps+=("pkg-config")
        print_warning "pkg-config не найден"
    else
        print_success "pkg-config"
    fi
    
    # Вывод результата
    if [ ${#missing_deps[@]} -ne 0 ]; then
        echo ""
        print_error "Не хватает зависимостей: ${missing_deps[*]}"
        echo ""
        print_info "Установите недостающие зависимости через Homebrew:"
        print_info "  brew install cmake ffmpeg pkg-config"
        if [ -z "$JAVA_HOME" ]; then
            print_info "  brew install openjdk@17"
        fi
        print_info ""
        print_info "После установки запустите скрипт снова"
        exit 1
    fi
}

# Определение архитектуры
setup_arch() {
    local arch=$(detect_arch)
    
    if [ "$TARGET_ARCH" = "auto" ]; then
        TARGET_ARCH="$arch"
    fi
    
    print_step "Целевая архитектура: $TARGET_ARCH"
    
    if [ "$arch" != "$TARGET_ARCH" ]; then
        print_warning "Архитектура системы: $arch, целевая: $TARGET_ARCH"
        print_info "Убедитесь, что все зависимости собраны для $TARGET_ARCH"
    fi
    
    # Установим переменные для CMake
    case "$TARGET_ARCH" in
        arm64)
            export ARCHFLAGS="-arch arm64"
            export CMAKE_OSX_ARCHITECTURES="arm64"
            print_info "Сборка для Apple Silicon (M1/M2/M3)"
            ;;
        x86_64)
            export ARCHFLAGS="-arch x86_64"
            export CMAKE_OSX_ARCHITECTURES="x86_64"
            print_info "Сборка для Intel Mac"
            ;;
        "universal")
            export ARCHFLAGS="-arch arm64 -arch x86_64"
            export CMAKE_OSX_ARCHITECTURES="arm64;x86_64"
            print_info "Сборка для универсальной архитектуры (Universal Binary)"
            ;;
        *)
            print_error "Неподдерживаемая архитектура: $TARGET_ARCH"
            exit 1
            ;;
    esac
}

# Очистка предыдущей сборки
clean_build() {
    if [ "$CLEAN" = true ]; then
        print_step "Очистка предыдущей сборки..."
        if [ -d "build" ]; then
            rm -rf build
            print_success "Каталог build удален"
        else
            print_info "Каталог build не существует"
        fi
    fi
}

# Конфигурация CMake
configure_cmake() {
    print_step "Конфигурация CMake..."
    
    mkdir -p build
    cd build
    
    # Сборка аргументов CMake
    local cmake_args=(-DCMAKE_BUILD_TYPE="$CONFIGURATION")
    cmake_args+=(-DCMAKE_OSX_ARCHITECTURES="$CMAKE_OSX_ARCHITECTURES")
    cmake_args+=(-DCMAKE_OSX_DEPLOYMENT_TARGET="10.15")
    
    # Java для JNI
    if [ -n "$JAVA_HOME" ]; then
        cmake_args+=(-DJAVA_HOME="$JAVA_HOME")
        print_info "JAVA_HOME: $JAVA_HOME"
    fi
    
    # Путь к FFmpeg (если установлен через Homebrew)
    if command_exists brew; then
        local brew_prefix=$(brew --prefix)
        if [ -d "$brew_prefix/lib/pkgconfig" ]; then
            export PKG_CONFIG_PATH="$brew_prefix/lib/pkgconfig:$PKG_CONFIG_PATH"
            print_info "PKG_CONFIG_PATH: $PKG_CONFIG_PATH"
        fi
    fi
    
    print_info "Вызов: cmake ${cmake_args[*]} .."
    cmake "${cmake_args[@]}" ..
    
    if [ $? -ne 0 ];
    then
        print_error "Ошибка конфигурации CMake"
        exit 1
    fi
    
    print_success "CMake сконфигурирован"
}

# Сборка
build_library() {
    print_step "Сборка библиотеки..."
    
    cmake --build . --config "$CONFIGURATION" -j$(sysctl -n hw.ncpu)
    
    if [ $? -ne 0 ]; then
        print_error "Ошибка сборки"
        exit 1
    fi
    
    print_success "Сборка завершена"
}

# Установка библиотеки
install_library() {
    print_step "Установка библиотеки..."
    
    # Создаем каталог для macOS
    local lib_dir="lib/macos"
    mkdir -p "$lib_dir"
    
    # Ищем скомпилированную библиотеку
    local dylib_path=""
    
    if [ -f "lib/libvideo_processing.dylib" ]; then
        dylib_path="lib/libvideo_processing.dylib"
    elif [ -f "libvideo_processing.dylib" ]; then
        dylib_path="libvideo_processing.dylib"
    fi
    
    if [ -n "$dylib_path" ]; then
        # Получаем размер
        local size=$(du -h "$dylib_path" | cut -f1)
        
        # Копируем в lib/macos
        cp "$dylib_path" "$lib_dir/"
        
        print_success "Библиотека установлена: $lib_dir/libvideo_processing.dylib"
        print_info "Размер: $size"
    else
        print_error "Библиотека не найдена после сборки"
        exit 1
    fi
    
    # Проверяем экспортированные символы
    print_step "Проверка экспортируемых символов..."
    if nm -g "$lib_dir/libvideo_processing.dylib" | grep -q "rtsp_client"; then
        print_success "Символы RTSP экспортированы правильно"
    else
        print_warning "Символы RTSP не найдены (проверьте CMakeLists.txt)"
    fi
}

# Вывод результатов
show_results() {
    echo ""
    print_header "✅ Сборка завершена!"
    
    local lib_path="lib/macos/libvideo_processing.dylib"
    if [ -f "$lib_path" ]; then
        local size=$(du -h "$lib_path" | cut -f1)
        print_success "Библиотека: $lib_path"
        print_info "Размер: $size"
        print_info ""
        print_info "Для использования скопируйте библиотеку в:"
        print_info "  - core/network/src/jvmMain/resources/"
        print_info "  - Или добавьте в java.library.path"
    fi
    
    echo ""
    print_info "Следующие шаги:"
    print_info "1. Протестируйте: ./gradlew :core:network:desktopTest"
    print_info "2. Проверьте в приложении: запустите с RTSP камерой"
    print_info "3. Создайте релиз: git tag v1.0.0 && git push origin v1.0.0"
}

# Утилита для установки зависимостей
install_deps() {
    print_header "Установка зависимостей"
    
    print_info "Установка зависимостей через Homebrew..."
    echo ""
    
    if ! command_exists brew; then
        print_error "Homebrew не установлен"
        print_info "Установите Homebrew:"
        print_info "  /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
        exit 1
    fi
    
    # Обновим Homebrew
    print_step "Обновление Homebrew..."
    brew update
    
    # Установим зависимости
    print_step "Установка CMake, FFmpeg и других зависимостей..."
    brew install cmake ffmpeg pkg-config
    
    # Опционально: Java
    if [ -z "$JAVA_HOME" ]; then
        print_step "Установка Java 17..."
        brew install openjdk@17
        print_info ""
        print_info "Добавьте Java в PATH:"
        print_info '  echo \'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"\' >> ~/.zshrc'
        print_info '  source ~/.zshrc'
    fi
    
    print_success "Все зависимости установлены"
}

# Главная функция
main() {
    # Парсинг аргументов
    case "$1" in
        --clean|-c)
            CLEAN=true
            shift
            ;;
        --install-deps|-i)
            install_deps
            exit 0
            ;;
        --help|-h)
            echo "Использование: $0 [OPTIONS] [CONFIGURATION] [ARCH]"
            echo ""
            echo "Аргументы:"
            echo "  CONFIGURATION  Release или Debug (по умолчанию: Release)"
            echo "  ARCH           arm64, x86_64, universal, auto (по умолчанию: auto)"
            echo ""
            echo "Опции:"
            echo "  --clean, -c    Очистить предыдущую сборку"
            echo "  --install-deps, -i  Установить зависимости через Homebrew"
            echo "  --help, -h     Показать эту справку"
            echo ""
            echo "Примеры:"
            echo "  $0                    # Сборка Release для текущей архитектуры"
            echo "  $0 Debug              # Сборка Debug"
            echo "  $0 Release x86_64     # Сборка Release для Intel"
            echo "  $0 --clean            # Очистка и сборка"
            echo "  $0 --install-deps     # Установка зависимостей"
            exit 0
            ;;
    esac
    
    print_header "RTSP Native Library Builder (macOS)"
    
    check_requirements
    setup_arch
    clean_build
    configure_cmake
    build_library
    install_library
    show_results
}

# Запуск
main "$@"
