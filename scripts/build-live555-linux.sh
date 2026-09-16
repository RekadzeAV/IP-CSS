#!/bin/bash
# Build Live555 for Linux
# Bash скрипт для автоматизации сборки Live555 на Linux

set -e

# Параметры
ARCHITECTURE="${1:-x64}"
BUILD_TYPE="${2:-Release}"
CLEAN="${3:-false}"

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

show_help() {
    echo "Build Live555 for Linux"
    echo ""
    echo "Usage: $0 [architecture] [build_type] [clean]"
    echo ""
    echo "Arguments:"
    echo "  architecture  Architecture: x64, arm64 (default: x64)"
    echo "  build_type    Build type: Release, Debug (default: Release)"
    echo "  clean         Clean before build: true, false (default: false)"
    echo ""
    echo "Examples:"
    echo "  $0 x64 Release"
    echo "  $0 arm64 Debug true"
}

if [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    show_help
    exit 0
fi

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}Live555 Build Script for Linux${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# Получить корень проекта
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LIVE555_DIR="$PROJECT_ROOT/native/live555"
SOURCE_DIR="$LIVE555_DIR/src"
BUILD_DIR="$LIVE555_DIR/build/$ARCHITECTURE/$BUILD_TYPE"

# Проверка наличия Git
echo -e "${YELLOW}Checking Git...${NC}"
if ! command -v git &> /dev/null; then
    echo -e "${RED}ERROR: Git not found. Please install Git.${NC}"
    exit 1
fi
echo -e "${GREEN}Git found: $(which git)${NC}"

# Клонирование репозитория если не существует
if [ ! -d "$SOURCE_DIR" ]; then
    echo ""
    echo -e "${YELLOW}Cloning Live555 repository...${NC}"
    mkdir -p "$SOURCE_DIR"
    cd "$SOURCE_DIR"
    git clone https://github.com/Live555/live555.git .
    if [ $? -ne 0 ]; then
        echo -e "${RED}ERROR: Failed to clone Live555 repository${NC}"
        exit 1
    fi
    echo -e "${GREEN}Live555 cloned successfully${NC}"
else
    echo -e "${YELLOW}Live555 source already exists, updating...${NC}"
    cd "$SOURCE_DIR"
    git pull
fi

# Очистка если требуется
if [ "$CLEAN" == "true" ]; then
    echo ""
    echo -e "${YELLOW}Cleaning previous builds...${NC}"
    rm -rf "$BUILD_DIR"
fi

# Создание директории сборки
mkdir -p "$BUILD_DIR"

# Настройка конфигурации
echo ""
echo -e "${YELLOW}Configuring for $ARCHITECTURE ($BUILD_TYPE)...${NC}"

# Определение компилятора
case $ARCHITECTURE in
    "x64")
        CC="gcc"
        CXX="g++"
        CFLAGS="-O2 -fPIC -Wall"
        CXXFLAGS="-O2 -fPIC -Wall"
        ;;
    "arm64")
        CC="aarch64-linux-gnu-gcc"
        CXX="aarch64-linux-gnu-g++"
        CFLAGS="-O2 -fPIC -Wall"
        CXXFLAGS="-O2 -fPIC -Wall"
        ;;
    *)
        echo -e "${RED}ERROR: Unsupported architecture: $ARCHITECTURE${NC}"
        echo -e "${YELLOW}Supported: x64, arm64${NC}"
        exit 1
        ;;
esac

# Настройка для Debug
if [ "$BUILD_TYPE" == "Debug" ]; then
    CFLAGS="-g -O0 -fPIC -Wall -DDEBUG"
    CXXFLAGS="-g -O0 -fPIC -Wall -DDEBUG"
fi

echo "Compiler: $CXX"
echo "CXXFLAGS: $CXXFLAGS"

# Сохранение конфигурации в файл
CONFIG_FILE="$BUILD_DIR/config.sh"
cat > "$CONFIG_FILE" << EOF
#!/bin/bash
# Live555 configuration for $ARCHITECTURE

CC="$CC"
CXX="$CXX"
CFLAGS="$CFLAGS"
CXXFLAGS="$CXXFLAGS"
LDFLAGS="-lpthread -ldl"

# Настройка для Linux
HAVE_IPV6=1
ENABLE_STATIC=1
EOF

chmod +x "$CONFIG_FILE"

# Сборка
echo ""
echo -e "${YELLOW}Building Live555...${NC}"
echo -e "${YELLOW}This may take a few minutes...${NC}"

cd "$SOURCE_DIR"

# Генерация Makefile
./configure --prefix="$BUILD_DIR"
if [ $? -ne 0 ];
then
    echo -e "${RED}ERROR: Configure failed${NC}"
    exit 1
fi

# Компиляция
make clean 2>/dev/null || true
make -j$(nproc)
if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Build failed with make${NC}"
    exit 1
fi

echo -e "${GREEN}Build completed successfully${NC}"

# Копирование библиотек и заголовков
echo ""
echo -e "${YELLOW}Copying libraries and headers...${NC}"

LIB_OUTPUT_DIR="$BUILD_DIR/lib"
INCLUDE_OUTPUT_DIR="$BUILD_DIR/include"
mkdir -p "$LIB_OUTPUT_DIR"
mkdir -p "$INCLUDE_OUTPUT_DIR"

# Копировать заголовки
find "$SOURCE_DIR" -name "*.h" -type f -exec cp {} "$INCLUDE_OUTPUT_DIR/" \;

# Копировать библиотеки
find "$SOURCE_DIR" -name "*.a" -o -name "*.so" | xargs -I {} cp {} "$LIB_OUTPUT_DIR/" 2>/dev/null || true

# Копировать исполняемые файлы
find "$SOURCE_DIR" -name "*.exe" -o -name "live555*" -type f -executable | xargs -I {} cp {} "$LIB_OUTPUT_DIR/" 2>/dev/null || true

echo -e "${GREEN}Libraries copied to: $LIB_OUTPUT_DIR${NC}"
echo -e "${GREEN}Headers copied to: $INCLUDE_OUTPUT_DIR${NC}"

# Создание pkg-config файла
echo ""
echo -e "${YELLOW}Creating pkg-config file...${NC}"

PKGCONFIG_FILE="$LIB_OUTPUT_DIR/pkgconfig/live555.pc"
mkdir -p "$(dirname "$PKGCONFIG_FILE")"

cat > "$PKGCONFIG_FILE" << EOF
prefix=$BUILD_DIR
exec_prefix=\${prefix}
libdir=\${prefix}/lib
includedir=\${prefix}/include

Name: live555
Description: Live Media Library
Version: 2023.11.23
Libs: -L\${libdir} -lbasicUsageEnvironment -lgroupsock -lLiveMedia -lUsageEnvironment
Cflags: -I\${includedir}
EOF

echo -e "${GREEN}pkg-config file created: $PKGCONFIG_FILE${NC}"

# Вывод итогов
echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}Build Summary${NC}"
echo -e "${CYAN}========================================${NC}"
echo "Architecture: $ARCHITECTURE"
echo "Build Type: $BUILD_TYPE"
echo "Output Directory: $BUILD_DIR"
echo "Libraries: $LIB_OUTPUT_DIR"
echo "Headers: $INCLUDE_OUTPUT_DIR"
echo "pkg-config: $PKGCONFIG_FILE"
echo ""
echo -e "${GREEN}Build completed successfully!${NC}"
echo ""
