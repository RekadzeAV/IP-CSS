#!/bin/bash
# =============================================================================
# IP-CSS NAS Package Builder
# Дата: 17.07.2026
# Сборка пакетов для Synology (SPK), QNAP (QPKG), Asustor (APK)
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$PROJECT_DIR/release-build/nas"
VERSION="${VERSION:-0.3.0}"

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  IP-CSS NAS Package Builder v${VERSION}${NC}"
echo -e "${GREEN}========================================${NC}"

# Проверка зависимостей
check_dependencies() {
    local deps=("java" "tar" "gzip" "docker")
    for dep in "${deps[@]}"; do
        if ! command -v "$dep" &> /dev/null; then
            echo -e "${RED}Error: $dep is required but not installed.${NC}"
            exit 1
        fi
    done
    echo -e "${GREEN}✓ All dependencies found${NC}"
}

# Сборка JAR
build_jar() {
    echo -e "${YELLOW}Building server JAR...${NC}"
    cd "$PROJECT_DIR"
    ./gradlew :server:api:shadowJar -x test
    echo -e "${GREEN}✓ JAR built successfully${NC}"
}

# =============================================================================
# Synology SPK Package
# =============================================================================
build_synology_spk() {
    echo -e "${YELLOW}Building Synology SPK package...${NC}"
    
    local spk_dir="$BUILD_DIR/synology"
    local package_dir="$spk_dir/package"
    
    mkdir -p "$package_dir"
    
    # INFO файл
    cat > "$spk_dir/INFO" << EOF
package="ipcss"
version="${VERSION}"
architecture="x86_64 bromolow"
description="IP Camera Surveillance System"
displayname="IP-CSS"
maintainer="IP-CSS Team"
maintainer_url="https://github.com/RekadzeAV/IP-CSS"
distributor="IP-CSS"
distributor_url="https://github.com/RekadzeAV/IP-CSS"
support_url="https://github.com/RekadzeAV/IP-CSS/issues"
thirdparty="yes"
startable="yes"
silent_install="no"
silent_upgrade="no"
EOF

    # Скрипт установки
    cat > "$spk_dir/scripts/install.sh" << 'SPKINST'
#!/bin/sh
# Synology SPK install script

# Создаём пользователя
synouser --add ipcss "IP-CSS" "ipcss" 0 "" 0

# Копируем файлы
cp -r /var/packages/ipcss/target/package/* /usr/local/ipcss/

# Настройка автозапуска
ln -sf /var/packages/ipcss/scripts/start-stop-status /usr/local/etc/rc.d/ipcss.sh

echo "IP-CSS installed successfully"
SPKINST
    chmod +x "$spk_dir/scripts/install.sh"
    
    # Скрипт start-stop-status
    cat > "$spk_dir/scripts/start-stop-status.sh" << 'SPKSS'
#!/bin/sh

case $1 in
    start)
        /usr/local/ipcss/bin/ipcss-server start
        ;;
    stop)
        /usr/local/ipcss/bin/ipcss-server stop
        ;;
    status)
        /usr/local/ipcss/bin/ipcss-server status
        ;;
    restart)
        $0 stop
        sleep 2
        $0 start
        ;;
esac
SPKSS
    chmod +x "$spk_dir/scripts/start-stop-status.sh"
    
    # Упаковка
    cd "$spk_dir"
    tar -czf "$BUILD_DIR/ipcss-${VERSION}-x86_64.spk" .
    echo -e "${GREEN}✓ Synology SPK: $BUILD_DIR/ipcss-${VERSION}-x86_64.spk${NC}"
}

# =============================================================================
# QNAP QPKG Package
# =============================================================================
build_qnap_qpkg() {
    echo -e "${YELLOW}Building QNAP QPKG package...${NC}"
    
    local qpkg_dir="$BUILD_DIR/qnap"
    mkdir -p "$qpkg_dir"
    
    # qpkg.cfg
    cat > "$qpkg_dir/qpkg.cfg" << EOF
QPKG_NAME="IPCSS"
QPKG_VER="${VERSION}"
QPKG_AUTHOR="IP-CSS Team"
QPKG_DESC="IP Camera Surveillance System"
QPKG_ARCH="x86_64"
QPKG_WEBUI="/ipcss"
QPKG_SERVICE=ipcss
EOF

    # package_routines
    cat > "$qpkg_dir/package_routines.sh" << 'QPKGROUT'
#!/bin/sh

qinstall() {
    echo "Installing IP-CSS..."
    cp -r "$QPKG_DIR/package" /usr/local/ipcss/
    return 0
}

qstart() {
    echo "Starting IP-CSS..."
    /usr/local/ipcss/bin/ipcss-server start
    return 0
}

qstop() {
    echo "Stopping IP-CSS..."
    /usr/local/ipcss/bin/ipcss-server stop
    return 0
}

qremove() {
    echo "Removing IP-CSS..."
    rm -rf /usr/local/ipcss
    return 0
}
QPKGROUT
    chmod +x "$qpkg_dir/package_routines.sh"
    
    # Упаковка
    cd "$qpkg_dir"
    tar -czf "$BUILD_DIR/ipcss-${VERSION}-x86_64.qpkg" .
    echo -e "${GREEN}✓ QNAP QPKG: $BUILD_DIR/ipcss-${VERSION}-x86_64.qpkg${NC}"
}

# =============================================================================
# Asustor APK Package
# =============================================================================
build_asustor_apk() {
    echo -e "${YELLOW}Building Asustor APK package...${NC}"
    
    local apk_dir="$BUILD_DIR/asustor"
    mkdir -p "$apk_dir/CONTROL"
    
    # control
    cat > "$apk_dir/CONTROL/control" << EOF
Package: ipcss
Version: ${VERSION}
Architecture: x86_64
Maintainer: IP-CSS Team
Description: IP Camera Surveillance System
EOF

    # preinst
    cat > "$apk_dir/CONTROL/preinst" << 'APKPRE'
#!/bin/sh
echo "Pre-install IP-CSS..."
exit 0
APKPRE
    chmod +x "$apk_dir/CONTROL/preinst"
    
    # postinst
    cat > "$apk_dir/CONTROL/postinst" << 'APKPOST'
#!/bin/sh
echo "IP-CSS installed successfully"
exit 0
APKPOST
    chmod +x "$apk_dir/CONTROL/postinst"
    
    # Упаковка
    cd "$apk_dir"
    tar -czf "$BUILD_DIR/ipcss-${VERSION}-x86_64.apk" .
    echo -e "${GREEN}✓ Asustor APK: $BUILD_DIR/ipcss-${VERSION}-x86_64.apk${NC}"
}

# =============================================================================
# Docker образ для TrueNAS SCALE
# =============================================================================
build_truenas_docker() {
    echo -e "${YELLOW}Building TrueNAS SCALE Docker image...${NC}"
    
    cd "$PROJECT_DIR"
    docker build -t "ipcss-server:${VERSION}" -f platforms/nas-x86_64/Dockerfile .
    docker tag "ipcss-server:${VERSION}" "ipcss-server:latest"
    
    echo -e "${GREEN}✓ Docker image: ipcss-server:${VERSION}${NC}"
}

# =============================================================================
# Main
# =============================================================================
main() {
    check_dependencies
    build_jar
    
    mkdir -p "$BUILD_DIR"
    
    case "${1:-all}" in
        synology)
            build_synology_spk
            ;;
        qnap)
            build_qnap_qpkg
            ;;
        asustor)
            build_asustor_apk
            ;;
        truenas)
            build_truenas_docker
            ;;
        all)
            build_synology_spk
            build_qnap_qpkg
            build_asustor_apk
            build_truenas_docker
            ;;
        *)
            echo "Usage: $0 {synology|qnap|asustor|truenas|all}"
            exit 1
            ;;
    esac
    
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  Build complete!${NC}"
    echo -e "${GREEN}  Output: $BUILD_DIR${NC}"
    echo -e "${GREEN}========================================${NC}"
}

main "$@"
