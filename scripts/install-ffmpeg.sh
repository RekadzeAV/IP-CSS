#!/bin/bash
# Скрипт установки FFmpeg для Linux/macOS
# Поддерживает: Ubuntu/Debian, RHEL/Fedora, macOS, Alpine

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Функция для вывода сообщений
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Определение ОС
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        if [ -f /etc/os-release ]; then
            . /etc/os-release
            OS=$ID
            OS_VERSION=$VERSION_ID
        elif type lsb_release >/dev/null 2>&1; then
            OS=$(lsb_release -si | tr '[:upper:]' '[:lower:]')
        elif [ -f /etc/lsb-release ]; then
            . /etc/lsb-release
            OS=$DISTRIB_ID
        elif [ -f /etc/debian_version ]; then
            OS=debian
        elif [ -f /etc/redhat-release ]; then
            OS=rhel
        else
            OS=$(uname -s | tr '[:upper:]' '[:lower:]')
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS="macos"
    else
        OS="unknown"
    fi
}

# Проверка наличия FFmpeg
check_ffmpeg() {
    if command -v ffmpeg &> /dev/null; then
        FFMPEG_VERSION=$(ffmpeg -version | head -n 1 | cut -d ' ' -f 3)
        log_info "FFmpeg уже установлен: версия $FFMPEG_VERSION"

        # Проверка поддержки необходимых кодеков
        if ffmpeg -codecs 2>/dev/null | grep -q "aac\|libmp3lame\|pcm"; then
            log_info "Необходимые кодеки доступны"
            return 0
        else
            log_warn "FFmpeg установлен, но некоторые кодеки отсутствуют"
            return 1
        fi
    else
        return 1
    fi
}

# Установка для Ubuntu/Debian
install_ubuntu_debian() {
    log_info "Установка FFmpeg для Ubuntu/Debian..."

    sudo apt-get update
    sudo apt-get install -y \
        ffmpeg \
        libavformat-dev \
        libavcodec-dev \
        libavutil-dev \
        libswscale-dev \
        libswresample-dev \
        libavfilter-dev \
        libavdevice-dev

    log_info "FFmpeg успешно установлен"
}

# Установка для RHEL/Fedora/CentOS
install_rhel_fedora() {
    log_info "Установка FFmpeg для RHEL/Fedora..."

    if command -v dnf &> /dev/null; then
        sudo dnf install -y \
            ffmpeg \
            ffmpeg-devel \
            libavformat \
            libavcodec \
            libavutil \
            libswscale \
            libswresample
    elif command -v yum &> /dev/null; then
        # Для CentOS/RHEL 7 может потребоваться EPEL
        sudo yum install -y epel-release
        sudo yum install -y \
            ffmpeg \
            ffmpeg-devel
    fi

    log_info "FFmpeg успешно установлен"
}

# Установка для Alpine Linux
install_alpine() {
    log_info "Установка FFmpeg для Alpine Linux..."

    sudo apk add --no-cache \
        ffmpeg \
        ffmpeg-dev \
        libavformat \
        libavcodec \
        libavutil \
        libswscale \
        libswresample

    log_info "FFmpeg успешно установлен"
}

# Установка для macOS
install_macos() {
    log_info "Установка FFmpeg для macOS..."

    if ! command -v brew &> /dev/null; then
        log_error "Homebrew не установлен. Установите Homebrew: https://brew.sh"
        exit 1
    fi

    brew install ffmpeg

    log_info "FFmpeg успешно установлен"
}

# Установка из исходников (для максимальной производительности)
install_from_source() {
    log_warn "Установка из исходников может занять много времени..."
    read -p "Продолжить? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        return 1
    fi

    BUILD_DIR="/tmp/ffmpeg-build"
    mkdir -p "$BUILD_DIR"
    cd "$BUILD_DIR"

    log_info "Скачивание исходников FFmpeg..."
    if [ ! -d "ffmpeg" ]; then
        git clone https://git.ffmpeg.org/ffmpeg.git
    fi

    cd ffmpeg
    git pull

    log_info "Конфигурация сборки..."
    ./configure \
        --enable-gpl \
        --enable-version3 \
        --enable-nonfree \
        --enable-libx264 \
        --enable-libx265 \
        --enable-libmp3lame \
        --enable-libaom \
        --enable-libvpx \
        --enable-libfdk-aac \
        --enable-libopus \
        --enable-libvorbis \
        --enable-libtheora \
        --enable-libass \
        --enable-libfreetype \
        --enable-libfribidi \
        --enable-libfontconfig \
        --enable-libpulse \
        --enable-libxcb \
        --enable-libxcb-shm \
        --enable-libxcb-xfixes \
        --enable-libxcb-shape \
        --enable-pthreads \
        --enable-hardcoded-tables \
        --enable-avresample \
        --enable-libswresample \
        --enable-shared \
        --disable-static \
        --prefix=/usr/local

    log_info "Компиляция (это может занять 30-60 минут)..."
    make -j$(nproc)

    log_info "Установка..."
    sudo make install
    sudo ldconfig

    log_info "FFmpeg успешно установлен из исходников"
}

# Основная функция
main() {
    log_info "Проверка установки FFmpeg..."

    if check_ffmpeg; then
        log_info "FFmpeg уже установлен и готов к использованию"
        exit 0
    fi

    detect_os
    log_info "Обнаружена ОС: $OS"

    case $OS in
        ubuntu|debian)
            install_ubuntu_debian
            ;;
        rhel|fedora|centos)
            install_rhel_fedora
            ;;
        alpine)
            install_alpine
            ;;
        macos)
            install_macos
            ;;
        *)
            log_error "Неподдерживаемая ОС: $OS"
            log_info "Попытка установки из исходников..."
            install_from_source
            ;;
    esac

    # Финальная проверка
    if check_ffmpeg; then
        log_info "✓ FFmpeg успешно установлен и готов к использованию"

        # Показываем информацию о кодеках
        log_info "Доступные аудио кодеки:"
        ffmpeg -codecs 2>/dev/null | grep -E "DEA.*(aac|mp3|pcm|g711)" || true

        log_info "Доступные видео кодеки:"
        ffmpeg -codecs 2>/dev/null | grep -E "DEV.*(h264|h265|hevc)" || true
    else
        log_error "Ошибка при установке FFmpeg"
        exit 1
    fi
}

# Запуск
main "$@"

