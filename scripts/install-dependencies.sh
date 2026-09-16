#!/bin/bash

# Скрипт установки зависимостей для Linux/macOS
# Использование: ./scripts/install-dependencies.sh [--skip-ffmpeg] [--skip-opencv] [--skip-cmake]

set -e

SKIP_FFMPEG=false
SKIP_OPENCV=false
SKIP_CMAKE=false

# Парсинг аргументов
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-ffmpeg)
            SKIP_FFMPEG=true
            shift
            ;;
        --skip-opencv)
            SKIP_OPENCV=true
            shift
            ;;
        --skip-cmake)
            SKIP_CMAKE=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "=========================================="
echo "Установка зависимостей для сборки"
echo "=========================================="
echo ""

# Определение платформы
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    PLATFORM="linux"
    if command -v apt-get &> /dev/null; then
        PACKAGE_MANAGER="apt"
    elif command -v dnf &> /dev/null; then
        PACKAGE_MANAGER="dnf"
    elif command -v yum &> /dev/null; then
        PACKAGE_MANAGER="yum"
    elif command -v pacman &> /dev/null; then
        PACKAGE_MANAGER="pacman"
    else
        echo "Неизвестный менеджер пакетов"
        exit 1
    fi
elif [[ "$OSTYPE" == "darwin"* ]]; then
    PLATFORM="macos"
    if command -v brew &> /dev/null; then
        PACKAGE_MANAGER="brew"
    else
        echo "Homebrew не установлен. Установите Homebrew:"
        echo "  /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
        exit 1
    fi
else
    echo "Неподдерживаемая платформа: $OSTYPE"
    exit 1
fi

echo "Платформа: $PLATFORM"
echo "Менеджер пакетов: $PACKAGE_MANAGER"
echo ""

# Установка CMake
if [ "$SKIP_CMAKE" = false ]; then
    echo "Проверка CMake..."
    if command -v cmake &> /dev/null; then
        CMAKE_VERSION=$(cmake --version | head -n1 | cut -d' ' -f3)
        echo "✓ CMake уже установлен (версия $CMAKE_VERSION)"
    else
        echo "CMake не найден. Установка..."
        case $PACKAGE_MANAGER in
            apt)
                sudo apt-get update
                sudo apt-get install -y cmake
                ;;
            dnf)
                sudo dnf install -y cmake
                ;;
            yum)
                sudo yum install -y cmake
                ;;
            pacman)
                sudo pacman -S --noconfirm cmake
                ;;
            brew)
                brew install cmake
                ;;
        esac
        echo "✓ CMake установлен"
    fi
else
    echo "Пропуск установки CMake"
fi

# Установка FFmpeg
if [ "$SKIP_FFMPEG" = false ]; then
    echo ""
    echo "Проверка FFmpeg..."
    if command -v ffmpeg &> /dev/null; then
        FFMPEG_VERSION=$(ffmpeg -version | head -n1 | cut -d' ' -f3)
        echo "✓ FFmpeg уже установлен (версия $FFMPEG_VERSION)"

        # Проверяем наличие необходимых библиотек
        if pkg-config --exists libavformat libavcodec libavutil libswscale libswresample 2>/dev/null; then
            echo "✓ Все необходимые библиотеки FFmpeg найдены"
        else
            echo "⚠ Предупреждение: некоторые библиотеки FFmpeg не найдены через pkg-config"
            echo "  Убедитесь, что установлены dev-пакеты:"
            case $PACKAGE_MANAGER in
                apt)
                    echo "    sudo apt-get install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev"
                    ;;
                dnf)
                    echo "    sudo dnf install ffmpeg-devel"
                    ;;
                brew)
                    echo "    brew install ffmpeg"
                    ;;
            esac
        fi
    else
        echo "FFmpeg не найден. Установка..."
        case $PACKAGE_MANAGER in
            apt)
                sudo apt-get update
                sudo apt-get install -y ffmpeg libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev
                ;;
            dnf)
                sudo dnf install -y ffmpeg ffmpeg-devel
                ;;
            yum)
                sudo yum install -y ffmpeg ffmpeg-devel
                ;;
            pacman)
                sudo pacman -S --noconfirm ffmpeg
                ;;
            brew)
                brew install ffmpeg
                ;;
        esac
        echo "✓ FFmpeg установлен"
    fi
else
    echo "Пропуск установки FFmpeg"
fi

# Установка OpenCV (опционально)
if [ "$SKIP_OPENCV" = false ]; then
    echo ""
    echo "Проверка OpenCV..."
    if pkg-config --exists opencv4 2>/dev/null || pkg-config --exists opencv 2>/dev/null; then
        OPENCV_VERSION=$(pkg-config --modversion opencv4 2>/dev/null || pkg-config --modversion opencv 2>/dev/null)
        echo "✓ OpenCV уже установлен (версия $OPENCV_VERSION)"
    else
        echo "OpenCV не найден. OpenCV опционален для сборки."
        echo "Для установки OpenCV:"
        case $PACKAGE_MANAGER in
            apt)
                echo "  sudo apt-get install libopencv-dev"
                ;;
            dnf)
                echo "  sudo dnf install opencv-devel"
                ;;
            brew)
                echo "  brew install opencv"
                ;;
        esac
        echo ""
        echo "Или соберите из исходников: https://opencv.org/releases/"
    fi
else
    echo "Пропуск проверки OpenCV"
fi

# Проверка компилятора C++
echo ""
echo "Проверка компилятора C++..."
if command -v g++ &> /dev/null; then
    GCC_VERSION=$(g++ --version | head -n1 | cut -d' ' -f4)
    echo "✓ g++ найден (версия $GCC_VERSION)"
elif command -v clang++ &> /dev/null; then
    CLANG_VERSION=$(clang++ --version | head -n1 | cut -d' ' -f3)
    echo "✓ clang++ найден (версия $CLANG_VERSION)"
else
    echo "Компилятор C++ не найден!"
    echo "Установите один из следующих:"
    case $PACKAGE_MANAGER in
        apt)
            echo "  sudo apt-get install build-essential"
            ;;
        dnf)
            echo "  sudo dnf groupinstall 'Development Tools'"
            ;;
        brew)
            echo "  xcode-select --install  # для macOS"
            ;;
    esac
fi

echo ""
echo "=========================================="
echo "Проверка завершена!"
echo "=========================================="
echo ""
echo "Следующие шаги:"
echo "  1. Убедитесь, что все зависимости установлены"
echo "  2. Соберите нативную библиотеку:"
echo "     ./scripts/build-all-platforms.sh Release"
echo ""

