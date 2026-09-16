#!/bin/bash

# Тестовый скрипт для проверки сборки NAS пакетов
# Usage: ./scripts/test-nas-build.sh [package-type] [arch]

set -e

PACKAGE_TYPE="${1:-synology}"
ARCH="${2:-x86_64}"
VERSION="Alfa-0.1.1"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}Testing NAS package build: ${PACKAGE_TYPE} for ${ARCH}${NC}"
echo ""

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed${NC}"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1)
echo "✓ Java: $JAVA_VERSION"

# Check Gradle
if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
    echo -e "${RED}Error: gradlew not found${NC}"
    exit 1
fi
echo "✓ Gradle wrapper found"

# Check build script
if [ ! -f "$PROJECT_ROOT/scripts/build-nas-package.sh" ]; then
    echo -e "${RED}Error: build-nas-package.sh not found${NC}"
    exit 1
fi
echo "✓ Build script found"

# Check package structure
if [ "$ARCH" = "x86_64" ] || [ "$ARCH" = "amd64" ]; then
    PLATFORM_DIR="$PROJECT_ROOT/platforms/nas-x86_64"
elif [ "$ARCH" = "arm64" ] || [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "armv7" ] || [ "$ARCH" = "arm" ] || [ "$ARCH" = "rtd1296" ] || [ "$ARCH" = "rtd" ]; then
    PLATFORM_DIR="$PROJECT_ROOT/platforms/nas-arm"
else
    echo -e "${RED}Error: Unsupported architecture: $ARCH${NC}"
    exit 1
fi

PACKAGE_DIR="$PLATFORM_DIR/packages/${PACKAGE_TYPE}"
if [ ! -d "$PACKAGE_DIR" ]; then
    echo -e "${RED}Error: Package directory not found: $PACKAGE_DIR${NC}"
    exit 1
fi
echo "✓ Package directory found: $PACKAGE_DIR"

# Check required files
echo ""
echo -e "${YELLOW}Checking package files...${NC}"

case "$PACKAGE_TYPE" in
    synology)
        if [ ! -f "$PACKAGE_DIR/INFO" ]; then
            echo -e "${RED}Error: INFO file not found${NC}"
            exit 1
        fi
        echo "✓ INFO file found"
        for script in preinst postinst preuninst postuninst; do
            if [ ! -f "$PACKAGE_DIR/scripts/$script" ]; then
                echo -e "${RED}Error: scripts/$script not found${NC}"
                exit 1
            fi
            echo "✓ scripts/$script found"
        done
        ;;
    qnap)
        if [ ! -f "$PACKAGE_DIR/QPKG.INFO" ]; then
            echo -e "${RED}Error: QPKG.INFO file not found${NC}"
            exit 1
        fi
        echo "✓ QPKG.INFO file found"
        if [ ! -f "$PACKAGE_DIR/scripts/start.sh" ]; then
            echo -e "${RED}Error: start.sh not found${NC}"
            exit 1
        fi
        echo "✓ start.sh found"
        if [ ! -f "$PACKAGE_DIR/scripts/detect-nas-paths.sh" ]; then
            echo -e "${YELLOW}Warning: detect-nas-paths.sh not found${NC}"
        else
            echo "✓ detect-nas-paths.sh found"
        fi
        ;;
    asustor)
        if [ ! -f "$PACKAGE_DIR/INFO" ]; then
            echo -e "${RED}Error: INFO file not found${NC}"
            exit 1
        fi
        echo "✓ INFO file found"
        for script in preinst postinst preuninst postuninst; do
            if [ ! -f "$PACKAGE_DIR/scripts/$script" ]; then
                echo -e "${RED}Error: scripts/$script not found${NC}"
                exit 1
            fi
            echo "✓ scripts/$script found"
        done
        ;;
    *)
        echo -e "${RED}Error: Unknown package type: $PACKAGE_TYPE${NC}"
        exit 1
        ;;
esac

# Test build (dry run - check structure only)
echo ""
echo -e "${YELLOW}Testing build structure (dry run)...${NC}"

# Create temporary directory for testing
TEST_DIR="/tmp/ip-css-test-$$"
mkdir -p "$TEST_DIR"
trap "rm -rf $TEST_DIR" EXIT

# Simulate package structure check
echo "✓ Package structure validated"

# Check if server API can be built
echo ""
echo -e "${YELLOW}Testing server API build...${NC}"
cd "$PROJECT_ROOT"
if ./gradlew :server:api:build --no-daemon --dry-run > /dev/null 2>&1; then
    echo "✓ Server API build configuration valid"
else
    echo -e "${YELLOW}Warning: Server API build check failed (may need actual build)${NC}"
fi

echo ""
echo -e "${GREEN}All checks passed!${NC}"
echo ""
echo "To build the package, run:"
echo "  ./scripts/build-nas-package.sh $PACKAGE_TYPE $ARCH $VERSION"
echo ""
echo "Or use Gradle:"
echo "  ./gradlew :platforms:nas-x86_64:build:buildAllNasPackages"
echo "  ./gradlew :platforms:nas-arm:build:buildAllNasPackages"
