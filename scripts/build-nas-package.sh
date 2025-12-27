#!/bin/bash

# Build script for NAS packages
# Usage: ./scripts/build-nas-package.sh <type> <arch> [version]
# Example: ./scripts/build-nas-package.sh synology x86_64 Alfa-0.0.1

set -e

PACKAGE_TYPE="${1:-synology}"
ARCH="${2:-x86_64}"
VERSION="${3:-Alfa-0.0.1}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Building NAS package: ${PACKAGE_TYPE} for ${ARCH}${NC}"

# Determine platform directory
if [ "$ARCH" = "x86_64" ] || [ "$ARCH" = "amd64" ]; then
    PLATFORM_DIR="$PROJECT_ROOT/platforms/nas-x86_64"
elif [ "$ARCH" = "arm64" ] || [ "$ARCH" = "aarch64" ]; then
    PLATFORM_DIR="$PROJECT_ROOT/platforms/nas-arm"
else
    echo -e "${RED}Error: Unsupported architecture: $ARCH${NC}"
    exit 1
fi

# Build the server and web components first
echo -e "${YELLOW}Building server components...${NC}"
cd "$PROJECT_ROOT"
./gradlew :server:api:build --no-daemon
./gradlew :server:web:build --no-daemon || {
    echo -e "${YELLOW}Note: Web build may require Node.js. Continuing...${NC}"
}

# Create temporary build directory
BUILD_DIR="/tmp/ip-css-nas-build-$$"
mkdir -p "$BUILD_DIR"
trap "rm -rf $BUILD_DIR" EXIT

build_synology_package() {
    echo -e "${GREEN}Building Synology SPK package...${NC}"

    PACKAGE_DIR="$PLATFORM_DIR/packages/synology"
    OUTPUT_FILE="$PROJECT_ROOT/build/ip-css-${VERSION}-synology-${ARCH}.spk"

    # Create package structure
    mkdir -p "$BUILD_DIR/package"
    mkdir -p "$BUILD_DIR/package/bin"
    mkdir -p "$BUILD_DIR/package/lib"
    mkdir -p "$BUILD_DIR/package/web"
    mkdir -p "$BUILD_DIR/package/conf"
    mkdir -p "$BUILD_DIR/package/scripts"

    # Copy package files
    cp -r "$PACKAGE_DIR/package/bin"/* "$BUILD_DIR/package/bin/" 2>/dev/null || true
    cp "$PROJECT_ROOT/server/api/build/libs/server-api-*.jar" "$BUILD_DIR/package/lib/server.jar" 2>/dev/null || true
    cp -r "$PROJECT_ROOT/server/web/.next" "$BUILD_DIR/package/web/dist" 2>/dev/null || true

    # Copy scripts
    cp "$PACKAGE_DIR/scripts"/* "$BUILD_DIR/package/scripts/" 2>/dev/null || true
    chmod +x "$BUILD_DIR/package/bin"/*.sh 2>/dev/null || true
    chmod +x "$BUILD_DIR/package/scripts"/* 2>/dev/null || true

    # Copy INFO file
    cp "$PACKAGE_DIR/INFO" "$BUILD_DIR/INFO"
    sed -i "s/version=\".*\"/version=\"$VERSION\"/" "$BUILD_DIR/INFO"
    sed -i "s/arch=\".*\"/arch=\"$ARCH\"/" "$BUILD_DIR/INFO"

    # Create package.tgz
    cd "$BUILD_DIR"
    tar -czf package.tgz -C package .

    # Create SPK file
    mkdir -p "$(dirname "$OUTPUT_FILE")"
    tar -czf "$OUTPUT_FILE" INFO package.tgz

    echo -e "${GREEN}SPK package created: $OUTPUT_FILE${NC}"
}

build_qnap_package() {
    echo -e "${GREEN}Building QNAP QPKG package...${NC}"

    PACKAGE_DIR="$PLATFORM_DIR/packages/qnap"
    OUTPUT_FILE="$PROJECT_ROOT/build/ip-css-${VERSION}-qnap-${ARCH}.qpkg"

    # Create package structure
    mkdir -p "$BUILD_DIR/package"
    mkdir -p "$BUILD_DIR/package/bin"
    mkdir -p "$BUILD_DIR/package/lib"
    mkdir -p "$BUILD_DIR/package/web"
    mkdir -p "$BUILD_DIR/package/scripts"

    # Copy package files
    cp -r "$PACKAGE_DIR/scripts"/* "$BUILD_DIR/package/scripts/" 2>/dev/null || true
    cp "$PROJECT_ROOT/server/api/build/libs/server-api-*.jar" "$BUILD_DIR/package/lib/server.jar" 2>/dev/null || true
    cp -r "$PROJECT_ROOT/server/web/.next" "$BUILD_DIR/package/web/dist" 2>/dev/null || true

    chmod +x "$BUILD_DIR/package/scripts"/*.sh 2>/dev/null || true

    # Copy QPKG.INFO file
    cp "$PACKAGE_DIR/QPKG.INFO" "$BUILD_DIR/QPKG.INFO"
    sed -i "s/<Version>.*<\/Version>/<Version>$VERSION<\/Version>/" "$BUILD_DIR/QPKG.INFO"
    sed -i "s/<Architecture>.*<\/Architecture>/<Architecture>$ARCH<\/Architecture>/" "$BUILD_DIR/QPKG.INFO"

    # Create package.tgz
    cd "$BUILD_DIR"
    tar -czf package.tgz -C package .

    # Create QPKG file
    mkdir -p "$(dirname "$OUTPUT_FILE")"
    tar -czf "$OUTPUT_FILE" QPKG.INFO package.tgz

    echo -e "${GREEN}QPKG package created: $OUTPUT_FILE${NC}"
}

build_asustor_package() {
    echo -e "${GREEN}Building Asustor APK package...${NC}"

    PACKAGE_DIR="$PLATFORM_DIR/packages/asustor"
    OUTPUT_FILE="$PROJECT_ROOT/build/ip-css-${VERSION}-asustor-${ARCH}.apk"

    # Create package structure
    mkdir -p "$BUILD_DIR/package"
    mkdir -p "$BUILD_DIR/package/bin"
    mkdir -p "$BUILD_DIR/package/lib"
    mkdir -p "$BUILD_DIR/package/web"
    mkdir -p "$BUILD_DIR/package/scripts"

    # Copy package files
    cp -r "$PACKAGE_DIR/package/bin"/* "$BUILD_DIR/package/bin/" 2>/dev/null || true
    cp "$PROJECT_ROOT/server/api/build/libs/server-api-*.jar" "$BUILD_DIR/package/lib/server.jar" 2>/dev/null || true
    cp -r "$PROJECT_ROOT/server/web/.next" "$BUILD_DIR/package/web/dist" 2>/dev/null || true

    # Copy scripts
    cp "$PACKAGE_DIR/scripts"/* "$BUILD_DIR/package/scripts/" 2>/dev/null || true
    chmod +x "$BUILD_DIR/package/bin"/*.sh 2>/dev/null || true
    chmod +x "$BUILD_DIR/package/scripts"/* 2>/dev/null || true

    # Copy INFO file
    cp "$PACKAGE_DIR/INFO" "$BUILD_DIR/INFO"
    sed -i "s/version=\".*\"/version=\"$VERSION\"/" "$BUILD_DIR/INFO"
    sed -i "s/architecture=\".*\"/architecture=\"$ARCH\"/" "$BUILD_DIR/INFO"

    # Create package.tgz
    cd "$BUILD_DIR"
    tar -czf package.tgz -C package .

    # Create APK file
    mkdir -p "$(dirname "$OUTPUT_FILE")"
    tar -czf "$OUTPUT_FILE" INFO package.tgz

    echo -e "${GREEN}APK package created: $OUTPUT_FILE${NC}"
}

build_truenas_package() {
    echo -e "${GREEN}Building TrueNAS package (Docker/Kubernetes)...${NC}"

    PACKAGE_DIR="$PLATFORM_DIR/packages/truenas"
    OUTPUT_DIR="$PROJECT_ROOT/build/truenas-${VERSION}"

    mkdir -p "$OUTPUT_DIR"

    # Copy Docker Compose file
    cp "$PACKAGE_DIR/docker-compose.yml" "$OUTPUT_DIR/"
    sed -i "s/:Alfa-0.0.1/:${VERSION}/g" "$OUTPUT_DIR/docker-compose.yml"

    # Copy Kubernetes manifests
    mkdir -p "$OUTPUT_DIR/kubernetes"
    cp "$PACKAGE_DIR/kubernetes"/*.yaml "$OUTPUT_DIR/kubernetes/" 2>/dev/null || true
    find "$OUTPUT_DIR/kubernetes" -type f -name "*.yaml" -exec sed -i "s/:Alfa-0.0.1/:${VERSION}/g" {} \;

    # Create README
    cat > "$OUTPUT_DIR/README.md" <<EOF
# IP-CSS for TrueNAS

## Installation

### TrueNAS SCALE (Docker)

1. Copy \`docker-compose.yml\` to your TrueNAS SCALE system
2. Run: \`docker-compose up -d\`

### TrueNAS SCALE (Kubernetes)

1. Apply Kubernetes manifests:
   \`\`\`bash
   kubectl apply -f kubernetes/
   \`\`\`

## Access

- Web UI: http://your-nas-ip:8080
- API: http://your-nas-ip:8081

## Version

$VERSION
EOF

    echo -e "${GREEN}TrueNAS package created in: $OUTPUT_DIR${NC}"
}

# Execute build function based on package type
case "$PACKAGE_TYPE" in
    synology)
        build_synology_package
        ;;
    qnap)
        build_qnap_package
        ;;
    asustor)
        build_asustor_package
        ;;
    truenas)
        build_truenas_package
        ;;
    *)
        echo -e "${RED}Error: Unknown package type: $PACKAGE_TYPE${NC}"
        echo "Supported types: synology, qnap, asustor, truenas"
        exit 1
        ;;
esac

echo -e "${GREEN}Build completed successfully!${NC}"

