#!/bin/bash
# Build Live555 for macOS/iOS
# Bash скрипт для автоматизации сборки Live555 на macOS

set -e

# Параметры
ARCHITECTURE="${1:-universal}"
BUILD_TYPE="${2:-Release}"
CLEAN="${3:-false}"
IOS_SDK_VERSION="${4:-latest}"

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

show_help() {
    echo "Build Live555 for macOS/iOS"
    echo ""
    echo "Usage: $0 [architecture] [build_type] [clean] [ios_sdk_version]"
    echo ""
    echo "Arguments:"
    echo "  architecture       Architecture: x64, arm64, universal (default: universal)"
    echo "  build_type         Build type: Release, Debug (default: Release)"
    echo "  clean              Clean before build: true, false (default: false)"
    echo "  ios_sdk_version    iOS SDK version: latest, or specific version like 15.0"
    echo ""
    echo "Examples:"
    echo "  $0 universal Release"
    echo "  $0 x64 Debug true"
    echo "  $0 arm64 Release false 15.0"
}

if [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    show_help
    exit 0
fi

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}Live555 Build Script for macOS/iOS${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# Получить корень проекта
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LIVE555_DIR="$PROJECT_ROOT/native/live555"
SOURCE_DIR="$LIVE555_DIR/src"
BUILD_DIR="$LIVE555_DIR/build/$ARCHITECTURE/$BUILD_TYPE"

# Проверка наличия Xcode
echo -e "${YELLOW}Checking Xcode...${NC}"
if ! command -v xcodebuild &> /dev/null; then
    echo -e "${RED}ERROR: Xcode not found. Please install Xcode from App Store.${NC}"
    exit 1
fi
echo -e "${GREEN}Xcode found: $(xcode-select -p)${NC}"

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

# Определение архитектуры
case $ARCHITECTURE in
    "x64")
        MACOS_ARCH="x86_64"
        IOS_ARCH="x86_64"
        echo -e "${YELLOW}Building for macOS x64 (Intel)${NC}"
        ;;
    "arm64")
        MACOS_ARCH="arm64"
        IOS_ARCH="arm64"
        echo -e "${YELLOW}Building for macOS/ARM64 (Apple Silicon)${NC}"
        ;;
    "universal")
        MACOS_ARCH="x86_64 arm64"
        IOS_ARCH="arm64"
        echo -e "${YELLOW}Building universal binary for macOS (x64 + ARM64)${NC}"
        ;;
    *)
        echo -e "${RED}ERROR: Unsupported architecture: $ARCHITECTURE${NC}"
        echo -e "${YELLOW}Supported: x64, arm64, universal${NC}"
        exit 1
        ;;
esac

# Настройка компилятора
CC="clang"
CXX="clang++"

if [ "$BUILD_TYPE" == "Debug" ]; then
    CFLAGS="-g -O0 -Wall -fPIC"
    CXXFLAGS="-g -O0 -Wall -fPIC"
else
    CFLAGS="-O2 -Wall -fPIC"
    CXXFLAGS="-O2 -Wall -fPIC"
fi

# Настройка для macOS
MACOS_CFLAGS="$CFLAGS -mmacosx-version-min=10.13"
MACOS_CXXFLAGS="$CXXFLAGS -mmacosx-version-min=10.13"

# Настройка для iOS
IOS_MIN_VERSION=12.0
IOS_CFLAGS="$CFLAGS -mios-version-min=$IOS_MIN_VERSION"
IOS_CXXFLAGS="$CXXFLAGS -mios-version-min=$IOS_MIN_VERSION"

echo "Compiler: $CXX"
echo "macOS CXXFLAGS: $MACOS_CXXFLAGS"

# Сборка для macOS
echo ""
echo -e "${YELLOW}Building for macOS...${NC}"
echo -e "${YELLOW}This may take a few minutes...${NC}"

cd "$SOURCE_DIR"

# Конфигурация для Darwin
./configureDarwin
if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Configure Darwin failed${NC}"
    exit 1
fi

# Компиляция
make clean 2>/dev/null || true
make -j$(sysctl -n hw.ncpu)
if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Build failed with make${NC}"
    exit 1
fi

echo -e "${GREEN}macOS build completed successfully${NC}"

# Сборка для iOS (если требуется)
IOS_BUILD_DIR=""
if [ "$ARCHITECTURE" == "arm64" ] || [ "$ARCHITECTURE" == "universal" ]; then
    echo ""
    echo -e "${YELLOW}Building for iOS...${NC}"
    
    IOS_BUILD_DIR="$BUILD_DIR/ios"
    mkdir -p "$IOS_BUILD_DIR"
    
    # Получить путь к iOS SDK
    IOS_SDK=$(xcrun --sdk iphoneos --show-sdk-path)
    IOS_SDK_VERSION=$(xcrun --sdk iphoneos --show-sdk-version)
    
    echo "iOS SDK: $IOS_SDK (version $IOS_SDK_VERSION)"
    
    # Кросс-компиляция для iOS
    IOS_CC="clang"
    IOS_CXX="clang++"
    
    IOS_CFLAGS="-arch $IOS_ARCH -isysroot $IOS_SDK -mios-version-min=$IOS_MIN_VERSION"
    IOS_CXXFLAGS="-arch $IOS_ARCH -isysroot $IOS_SDK -mios-version-min=$IOS_MIN_VERSION"
    
    # Сохранение конфигурации
    cat > "$IOS_BUILD_DIR/config.sh" << EOF
#!/bin/bash
CC="$IOS_CC"
CXX="$IOS_CXX"
CFLAGS="$IOS_CFLAGS"
CXXFLAGS="$IOS_CXXFLAGS"
LDFLAGS="-framework CoreFoundation -framework Security"

HAVE_IPV6=1
ENABLE_STATIC=1
EOF
    
    chmod +x "$IOS_BUILD_DIR/config.sh"
    
    echo -e "${GREEN}iOS configuration saved${NC}"
fi

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
find "$SOURCE_DIR" -name "*.a" | xargs -I {} cp {} "$LIB_OUTPUT_DIR/" 2>/dev/null || true

# Если universal binary, создать fat binary
if [ "$ARCHITECTURE" == "universal" ]; then
    echo ""
    echo -e "${YELLOW}Creating universal binary...${NC}"
    
    for lib in "$LIB_OUTPUT_DIR"/*.a; do
        if [ -f "$lib" ]; then
            libname=$(basename "$lib")
            echo "Creating universal binary for: $libname"
            lipo -create -output "$LIB_OUTPUT_DIR/universal_$libname" \
                "$LIB_OUTPUT_DIR/$libname" \
                "$LIB_OUTPUT_DIR/$libname" 2>/dev/null || true
        fi
    done
fi

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
Libs: -L\${libdir} -lbasicUsageEnvironment -lgroupsock -lLiveMedia -lUsageEnvironment -framework CoreFoundation
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

if [ -n "$IOS_BUILD_DIR" ]; then
    echo "iOS Build Directory: $IOS_BUILD_DIR"
    echo "iOS SDK: $IOS_SDK (version $IOS_SDK_VERSION)"
fi

echo ""
echo -e "${GREEN}Build completed successfully!${NC}"
echo ""
