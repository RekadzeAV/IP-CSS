#!/bin/bash

# Build script for NAS packages
# Usage: ./scripts/build-nas-package.sh <type> <arch> [version]
# Example: ./scripts/build-nas-package.sh synology x86_64 Alfa-0.1.1

set -e

PACKAGE_TYPE="${1:-synology}"
ARCH="${2:-x86_64}"
VERSION="${3:-Alfa-0.1.1}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Building NAS package: ${PACKAGE_TYPE} for ${ARCH}${NC}"

# Determine platform directory (x86_64 -> nas-x86_64; arm64/armv7 -> nas-arm)
if [ "$ARCH" = "x86_64" ] || [ "$ARCH" = "amd64" ]; then
    PLATFORM_DIR="$PROJECT_ROOT/platforms/nas-x86_64"
elif [ "$ARCH" = "arm64" ] || [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "armv7" ] || [ "$ARCH" = "arm" ] || [ "$ARCH" = "arm_32" ] || [ "$ARCH" = "rtd1296" ] || [ "$ARCH" = "rtd" ]; then
    PLATFORM_DIR="$PROJECT_ROOT/platforms/nas-arm"
else
    echo -e "${RED}Error: Unsupported architecture: $ARCH${NC}"
    exit 1
fi

# Build the server and web components first
echo -e "${YELLOW}Building server components...${NC}"
cd "$PROJECT_ROOT"

# Build API server
if ls "$PROJECT_ROOT/server/api/build/libs/server-api-"*.jar 1> /dev/null 2>&1; then
    echo -e "${GREEN}API server JAR already exists, skipping build${NC}"
else
    echo -e "${YELLOW}API server JAR not found, building...${NC}"
    ./gradlew :server:api:build --no-daemon
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Failed to build API server${NC}"
        exit 1
    fi
fi

# Build web interface
if [ -d "$PROJECT_ROOT/server/web/.next" ]; then
    echo -e "${GREEN}Web interface already built, skipping build${NC}"
else
    echo -e "${YELLOW}Web interface not found, building...${NC}"
    cd "$PROJECT_ROOT/server/web"
    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}Installing npm dependencies...${NC}"
        npm install
        if [ $? -ne 0 ]; then
            echo -e "${YELLOW}Warning: npm install failed. Web interface will not be included.${NC}"
            cd "$PROJECT_ROOT"
        else
            npm run build
            if [ $? -ne 0 ]; then
                echo -e "${YELLOW}Warning: npm build failed. Web interface will not be included.${NC}"
            fi
            cd "$PROJECT_ROOT"
        fi
    else
        npm run build
        if [ $? -ne 0 ]; then
            echo -e "${YELLOW}Warning: npm build failed. Web interface will not be included.${NC}"
        fi
        cd "$PROJECT_ROOT"
    fi
fi

# Create temporary build directory
BUILD_DIR="/tmp/ip-css-nas-build-$$"
mkdir -p "$BUILD_DIR"
trap "rm -rf $BUILD_DIR" EXIT

build_synology_package() {
    echo -e "${GREEN}Building Synology SPK package...${NC}"

    # Read version from gradle.properties if not provided or default
    if [ -z "$VERSION" ] || [ "$VERSION" = "Alfa-0.1.1" ]; then
        if [ -f "$PROJECT_ROOT/gradle.properties" ]; then
            GRADLE_VERSION=$(grep "^version=" "$PROJECT_ROOT/gradle.properties" | cut -d'=' -f2 | tr -d '[:space:]')
            if [ -n "$GRADLE_VERSION" ]; then
                VERSION="$GRADLE_VERSION"
                echo -e "${GREEN}Using version from gradle.properties: $VERSION${NC}"
            fi
        fi
    fi

    PACKAGE_DIR="$PLATFORM_DIR/packages/synology"
    OUTPUT_FILE="$PROJECT_ROOT/build/ip-css-${VERSION}-synology-${ARCH}.spk"

    # Check if icons exist
    if [ ! -f "$PACKAGE_DIR/icons/PACKAGE_ICON.PNG" ] || [ ! -f "$PACKAGE_DIR/icons/PACKAGE_ICON_256.PNG" ]; then
        echo -e "${YELLOW}Warning: Package icons not found. Package will be created without icons.${NC}"
        HAS_ICONS=false
    else
        HAS_ICONS=true
    fi

    # Create package structure (content that goes inside package.tgz)
    mkdir -p "$BUILD_DIR/package/bin"
    mkdir -p "$BUILD_DIR/package/lib"
    mkdir -p "$BUILD_DIR/package/web"
    mkdir -p "$BUILD_DIR/package/conf"
    mkdir -p "$BUILD_DIR/package/scripts"

    # Copy package bin (start.sh, stop.sh)
    if [ -d "$PACKAGE_DIR/package/bin" ]; then
        cp -r "$PACKAGE_DIR/package/bin"/* "$BUILD_DIR/package/bin/" 2>/dev/null || true
    fi
    find "$BUILD_DIR/package/bin" -name "*.sh" -type f -exec chmod +x {} \; 2>/dev/null || true

    # Find API server JAR (server-api-*.jar or api-*.jar, exclude -sources and -javadoc)
    SERVER_JAR=$(find "$PROJECT_ROOT/server/api/build/libs" -maxdepth 1 -name "*.jar" ! -name "*-sources*" ! -name "*-javadoc*" 2>/dev/null | head -1)
    if [ -n "$SERVER_JAR" ] && [ -f "$SERVER_JAR" ]; then
        cp "$SERVER_JAR" "$BUILD_DIR/package/lib/server.jar"
        echo -e "${GREEN}API server JAR included: $(basename "$SERVER_JAR")${NC}"
    else
        echo -e "${RED}Error: API server JAR not found in server/api/build/libs/. Build with: ./gradlew :server:api:build${NC}"
        exit 1
    fi

    # Copy web interface (optional)
    if [ -d "$PROJECT_ROOT/server/web/.next" ]; then
        mkdir -p "$BUILD_DIR/package/web/dist"
        cp -r "$PROJECT_ROOT/server/web/.next" "$BUILD_DIR/package/web/dist/" 2>/dev/null || true
        echo -e "${GREEN}Web interface included${NC}"
    else
        echo -e "${YELLOW}Warning: Web interface not found, package will include only API server${NC}"
        mkdir -p "$BUILD_DIR/package/web/dist"
    fi

    # Copy detect-nas-paths.sh into package/scripts if exists
    if [ -f "$PLATFORM_DIR/packages/qnap/scripts/detect-nas-paths.sh" ]; then
        cp "$PLATFORM_DIR/packages/qnap/scripts/detect-nas-paths.sh" "$BUILD_DIR/package/scripts/" 2>/dev/null || true
    fi
    find "$BUILD_DIR/package/scripts" -name "*.sh" -type f -exec chmod +x {} \; 2>/dev/null || true

    # Copy install scripts to BUILD_DIR root (Synology expects preinst, postinst, preuninst, postuninst at SPK root)
    for script in preinst postinst preuninst postuninst; do
        if [ -f "$PACKAGE_DIR/scripts/$script" ]; then
            cp "$PACKAGE_DIR/scripts/$script" "$BUILD_DIR/$script"
            chmod +x "$BUILD_DIR/$script"
        fi
    done

    # Copy icons if they exist
    if [ "$HAS_ICONS" = true ]; then
        mkdir -p "$BUILD_DIR/icons"
        cp "$PACKAGE_DIR/icons/PACKAGE_ICON.PNG" "$BUILD_DIR/icons/" 2>/dev/null || true
        cp "$PACKAGE_DIR/icons/PACKAGE_ICON_256.PNG" "$BUILD_DIR/icons/" 2>/dev/null || true
        echo -e "${GREEN}Package icons included${NC}"
    fi

    # Copy and update INFO file
    cp "$PACKAGE_DIR/INFO" "$BUILD_DIR/INFO"
    sed "s/version=\".*\"/version=\"$VERSION\"/" "$BUILD_DIR/INFO" > "$BUILD_DIR/INFO.tmp" && mv "$BUILD_DIR/INFO.tmp" "$BUILD_DIR/INFO"
    sed "s/arch=\".*\"/arch=\"$ARCH\"/" "$BUILD_DIR/INFO" > "$BUILD_DIR/INFO.tmp" && mv "$BUILD_DIR/INFO.tmp" "$BUILD_DIR/INFO"

    # Create package.tgz (content only, no install scripts)
    cd "$BUILD_DIR"
    tar -czf package.tgz -C package .

    # Create SPK: INFO + package.tgz + root-level install scripts (Synology format)
    mkdir -p "$(dirname "$OUTPUT_FILE")"
    cd "$BUILD_DIR"
    SPK_FILES="INFO package.tgz preinst postinst preuninst postuninst"
    if [ "$HAS_ICONS" = true ] && [ -d "icons" ] && [ -n "$(ls -A icons 2>/dev/null)" ]; then
        tar -czf "$OUTPUT_FILE" $SPK_FILES icons
    else
        tar -czf "$OUTPUT_FILE" $SPK_FILES
    fi
    echo -e "${GREEN}SPK package created: $OUTPUT_FILE${NC}"
}

build_qnap_package() {
    echo -e "${GREEN}Building QNAP QPKG package...${NC}"

    PACKAGE_DIR="$PLATFORM_DIR/packages/qnap"
    OUTPUT_FILE="$PROJECT_ROOT/build/ip-css-${VERSION}-qnap-${ARCH}.qpkg"

    # QNAP architecture in QPKG.INFO: x86_64, arm_64, arm_32
    QNAP_ARCH="$ARCH"
    case "$ARCH" in
        arm64|aarch64) QNAP_ARCH="arm_64" ;;
        armv7|arm)     QNAP_ARCH="arm_32" ;;
        x86_64|amd64)  QNAP_ARCH="x86_64" ;;
    esac

    # Package structure: init.sh, start.sh, stop.sh, uninstall.sh at root (QNAP service framework)
    mkdir -p "$BUILD_DIR/package"
    mkdir -p "$BUILD_DIR/package/lib"
    mkdir -p "$BUILD_DIR/package/web"
    mkdir -p "$BUILD_DIR/package/scripts"

    # Management scripts at package root (required by QNAP App Center)
    for script in init.sh start.sh stop.sh uninstall.sh service.sh; do
        if [ -f "$PACKAGE_DIR/scripts/$script" ]; then
            cp "$PACKAGE_DIR/scripts/$script" "$BUILD_DIR/package/$script"
            chmod +x "$BUILD_DIR/package/$script"
        fi
    done
    # Helper scripts in package/scripts/
    if [ -f "$PACKAGE_DIR/scripts/detect-nas-paths.sh" ]; then
        cp "$PACKAGE_DIR/scripts/detect-nas-paths.sh" "$BUILD_DIR/package/scripts/"
        chmod +x "$BUILD_DIR/package/scripts/detect-nas-paths.sh"
    fi

    # Server JAR (same logic as Synology)
    SERVER_JAR=$(find "$PROJECT_ROOT/server/api/build/libs" -maxdepth 1 -name "*.jar" ! -name "*-sources*" ! -name "*-javadoc*" 2>/dev/null | head -1)
    if [ -n "$SERVER_JAR" ] && [ -f "$SERVER_JAR" ]; then
        cp "$SERVER_JAR" "$BUILD_DIR/package/lib/server.jar"
        echo -e "${GREEN}API server JAR included: $(basename "$SERVER_JAR")${NC}"
    else
        echo -e "${RED}Error: API server JAR not found. Build with: ./gradlew :server:api:build${NC}"
        exit 1
    fi

    # Web interface (optional)
    if [ -d "$PROJECT_ROOT/server/web/.next" ]; then
        mkdir -p "$BUILD_DIR/package/web/dist"
        cp -r "$PROJECT_ROOT/server/web/.next" "$BUILD_DIR/package/web/dist/" 2>/dev/null || true
        echo -e "${GREEN}Web interface included${NC}"
    else
        mkdir -p "$BUILD_DIR/package/web/dist"
    fi

    # QPKG.INFO: copy and update version/architecture
    cp "$PACKAGE_DIR/QPKG.INFO" "$BUILD_DIR/QPKG.INFO"
    sed "s/<Version>.*<\/Version>/<Version>$VERSION<\/Version>/" "$BUILD_DIR/QPKG.INFO" > "$BUILD_DIR/QPKG.INFO.tmp" && mv "$BUILD_DIR/QPKG.INFO.tmp" "$BUILD_DIR/QPKG.INFO"
    sed "s/<Architecture>.*<\/Architecture>/<Architecture>$QNAP_ARCH<\/Architecture>/" "$BUILD_DIR/QPKG.INFO" > "$BUILD_DIR/QPKG.INFO.tmp" && mv "$BUILD_DIR/QPKG.INFO.tmp" "$BUILD_DIR/QPKG.INFO"

    # Create package.tgz and QPKG
    cd "$BUILD_DIR"
    tar -czf package.tgz -C package .
    mkdir -p "$(dirname "$OUTPUT_FILE")"
    tar -czf "$OUTPUT_FILE" QPKG.INFO package.tgz

    echo -e "${GREEN}QPKG package created: $OUTPUT_FILE${NC}"
}

build_asustor_package() {
    echo -e "${GREEN}Building Asustor APK package...${NC}"

    PACKAGE_DIR="$PLATFORM_DIR/packages/asustor"
    OUTPUT_FILE="$PROJECT_ROOT/build/ip-css-${VERSION}-asustor-${ARCH}.apk"

    # Asustor architecture in INFO: x86_64, armv8, rtd1296
    APKG_ARCH="$ARCH"
    case "$ARCH" in
        arm64|aarch64) APKG_ARCH="armv8" ;;
        armv7|arm)     APKG_ARCH="armv7" ;;
        rtd1296|rtd)   APKG_ARCH="rtd1296" ;;
        x86_64|amd64)  APKG_ARCH="x86_64" ;;
    esac

    mkdir -p "$BUILD_DIR/package/bin" "$BUILD_DIR/package/lib" "$BUILD_DIR/package/web" "$BUILD_DIR/package/scripts"

    # Bin scripts (start/stop for ADM service framework)
    if [ -d "$PACKAGE_DIR/package/bin" ]; then
        cp -r "$PACKAGE_DIR/package/bin"/* "$BUILD_DIR/package/bin/" 2>/dev/null || true
    fi
    chmod +x "$BUILD_DIR/package/bin"/*.sh 2>/dev/null || true

    # Server JAR
    SERVER_JAR=$(find "$PROJECT_ROOT/server/api/build/libs" -maxdepth 1 -name "*.jar" ! -name "*-sources*" ! -name "*-javadoc*" 2>/dev/null | head -1)
    if [ -n "$SERVER_JAR" ] && [ -f "$SERVER_JAR" ]; then
        cp "$SERVER_JAR" "$BUILD_DIR/package/lib/server.jar"
        echo -e "${GREEN}API server JAR included${NC}"
    else
        echo -e "${RED}Error: API server JAR not found. Build with: ./gradlew :server:api:build${NC}"
        exit 1
    fi

    # Web (optional)
    if [ -d "$PROJECT_ROOT/server/web/.next" ]; then
        mkdir -p "$BUILD_DIR/package/web/dist"
        cp -r "$PROJECT_ROOT/server/web/.next" "$BUILD_DIR/package/web/dist/" 2>/dev/null || true
    else
        mkdir -p "$BUILD_DIR/package/web/dist"
    fi

    # Install/uninstall scripts (in package for reference; ADM may expect at APK root)
    for s in preinst postinst preuninst postuninst; do
        if [ -f "$PACKAGE_DIR/scripts/$s" ]; then
            cp "$PACKAGE_DIR/scripts/$s" "$BUILD_DIR/package/scripts/"
            chmod +x "$BUILD_DIR/package/scripts/$s"
        fi
    done

    # INFO: version and architecture
    cp "$PACKAGE_DIR/INFO" "$BUILD_DIR/INFO"
    sed "s/version=\".*\"/version=\"$VERSION\"/" "$BUILD_DIR/INFO" > "$BUILD_DIR/INFO.tmp" && mv "$BUILD_DIR/INFO.tmp" "$BUILD_DIR/INFO"
    sed "s/architecture=\".*\"/architecture=\"$APKG_ARCH\"/" "$BUILD_DIR/INFO" > "$BUILD_DIR/INFO.tmp" && mv "$BUILD_DIR/INFO.tmp" "$BUILD_DIR/INFO"

    cd "$BUILD_DIR"
    tar -czf package.tgz -C package .

    # APK: INFO + package.tgz + install scripts at root (App Central)
    APK_FILES="INFO package.tgz"
    for s in preinst postinst preuninst postuninst; do
        if [ -f "$PACKAGE_DIR/scripts/$s" ]; then
            cp "$PACKAGE_DIR/scripts/$s" "$BUILD_DIR/$s"
            chmod +x "$BUILD_DIR/$s"
            APK_FILES="$APK_FILES $s"
        fi
    done
    mkdir -p "$(dirname "$OUTPUT_FILE")"
    tar -czf "$OUTPUT_FILE" $APK_FILES

    echo -e "${GREEN}APK package created: $OUTPUT_FILE${NC}"
}

build_truenas_package() {
    echo -e "${GREEN}Building TrueNAS package (CORE Jail + SCALE Docker/Kubernetes)...${NC}"

    PACKAGE_DIR="$PLATFORM_DIR/packages/truenas"
    OUTPUT_DIR="$PROJECT_ROOT/build/truenas-${VERSION}"

    mkdir -p "$OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR/kubernetes"

    # Docker Compose (SCALE)
    cp "$PACKAGE_DIR/docker-compose.yml" "$OUTPUT_DIR/"
    sed "s/:Alfa-0.0.1/:${VERSION}/g" "$OUTPUT_DIR/docker-compose.yml" > "$OUTPUT_DIR/docker-compose.yml.tmp" && mv "$OUTPUT_DIR/docker-compose.yml.tmp" "$OUTPUT_DIR/docker-compose.yml"

    # Kubernetes manifests (SCALE)
    cp "$PACKAGE_DIR/kubernetes"/*.yaml "$OUTPUT_DIR/kubernetes/" 2>/dev/null || true
    for f in "$OUTPUT_DIR/kubernetes"/*.yaml; do
        [ -f "$f" ] || continue
        sed "s/:Alfa-0.0.1/:${VERSION}/g" "$f" > "$f.tmp" && mv "$f.tmp" "$f"
    done

    # TrueNAS CORE (FreeBSD Jail) — script and README
    if [ -d "$PACKAGE_DIR/core" ]; then
        mkdir -p "$OUTPUT_DIR/core"
        cp -r "$PACKAGE_DIR/core"/* "$OUTPUT_DIR/core/" 2>/dev/null || true
        for f in "$OUTPUT_DIR/core"/*.sh; do
            [ -f "$f" ] && chmod +x "$f"
        done
    fi

    # README
    cat > "$OUTPUT_DIR/README.md" <<EOF
# IP-CSS for TrueNAS

## TrueNAS CORE (FreeBSD Jail)

See \`core/README.md\` and \`core/setup-jail.sh\` for creating an iocage jail and running IP-CSS inside it.

## TrueNAS SCALE (Docker)

1. Copy \`docker-compose.yml\` to your system
2. Run: \`docker-compose up -d\`

## TrueNAS SCALE (Kubernetes)

\`\`\`bash
kubectl apply -f kubernetes/
\`\`\`

## Access

- Web UI: http://your-nas-ip:8080
- API: http://your-nas-ip:8081

Version: $VERSION
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

