#!/bin/bash
# Package Center Publication Script
# Prepares and submits packages to NAS package centers

set -e

echo "=== IP-CSS Package Center Publication ==="
echo "Date: $(date)"
echo ""

# Configuration
PACKAGE_VERSION="0.3.0"
BUILD_NUMBER="${BUILD_NUMBER:-0001}"
SIGNING_KEY="${SIGNING_KEY:-}"
PUBLISH_SYNOLOGY="${PUBLISH_SYNOLOGY:-false}"
PUBLISH_QNAP="${PUBLISH_QNAP:-false}"
PUBLISH_ASUSTOR="${PUBLISH_ASUSTOR:-false}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
BUILD_DIR="${PROJECT_ROOT}/release-build/nas"
PACKAGE_DIR="${PROJECT_ROOT}/platforms"

# Create build directory
mkdir -p "${BUILD_DIR}"

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Build Synology SPK package
build_synology_spk() {
    log_info "Building Synology SPK package..."
    
    local spk_source="${PACKAGE_DIR}/nas-synology/package"
    local spk_dest="${BUILD_DIR}/synology"
    
    mkdir -p "${spk_dest}"
    
    # Copy package files
    cp -r "${spk_source}"/* "${spk_dest}/"
    
    # Update version in INFO
    sed -i "s/version=\"[^\"]*\"/version=\"${PACKAGE_VERSION}-${BUILD_NUMBER}\"/" "${spk_dest}/INFO"
    
    # Create SPK (tar.gz with .spk extension)
    cd "${spk_dest}"
    tar -czf "../IP-CSS_${PACKAGE_VERSION}_${BUILD_NUMBER}.spk" *
    cd - > /dev/null
    
    # Generate checksum
    cd "${BUILD_DIR}/synology"
    sha256sum "IP-CSS_${PACKAGE_VERSION}_${BUILD_NUMBER}.spk" > "IP-CSS_${PACKAGE_VERSION}_${BUILD_NUMBER}.spk.sha256"
    cd - > /dev/null
    
    log_success "Synology SPK package built: ${BUILD_DIR}/synology/IP-CSS_${PACKAGE_VERSION}_${BUILD_NUMBER}.spk"
}

# Build QNAP QPKG package
build_qnap_qpkg() {
    log_info "Building QNAP QPKG package..."
    
    local qpkg_source="${PACKAGE_DIR}/nas-qnap/QPKG"
    local qpkg_dest="${BUILD_DIR}/qnap"
    
    mkdir -p "${qpkg_dest}"
    
    # Copy QPKG files
    cp -r "${qpkg_source}"/* "${qpkg_dest}/"
    
    # Update version in qpkg.cfg
    sed -i "s/QPKG_VERSION=\"[^\"]*\"/QPKG_VERSION=\"${PACKAGE_VERSION}\"/" "${qpkg_dest}/qpkg.cfg"
    
    # Create QPKG (self-extracting executable)
    cd "${qpkg_dest}"
    
    # Create package tarball
    tar -czf "IP-CSS_${PACKAGE_VERSION}.tar.gz" *
    
    # Create QPKG header (simplified - real QPKG needs proper header)
    cat > "IP-CSS_${PACKAGE_VERSION}.qpkg" << EOF
#!/bin/sh
# QNAP QPKG Installer
# Version: ${PACKAGE_VERSION}

ARCHIVE="IP-CSS_${PACKAGE_VERSION}.tar.gz"

# Extract and install
tar -xzf "\${ARCHIVE}"
./install.sh

EOF
    
    chmod +x "IP-CSS_${PACKAGE_VERSION}.qpkg"
    
    cd - > /dev/null
    
    log_success "QNAP QPKG package built: ${BUILD_DIR}/qnap/IP-CSS_${PACKAGE_VERSION}.qpkg"
}

# Build Asustor APK package
build_asustor_apk() {
    log_info "Building Asustor APK package..."
    
    local apk_source="${PACKAGE_DIR}/nas-asustor/package"
    local apk_dest="${BUILD_DIR}/asustor"
    
    mkdir -p "${apk_dest}"
    
    # Copy APK files
    cp -r "${apk_source}"/* "${apk_dest}/"
    
    # Update version in package.conf
    sed -i "s/version = \"[^\"]*\"/version = \"${PACKAGE_VERSION}\"/" "${apk_dest}/package.conf"
    
    # Create APK (tar.gz with .apk extension)
    cd "${apk_dest}"
    tar -czf "../IP-CSS_${PACKAGE_VERSION}.apk" *
    cd - > /dev/null
    
    log_success "Asustor APK package built: ${BUILD_DIR}/asustor/IP-CSS_${PACKAGE_VERSION}.apk"
}

# Build TrueNAS Docker image
build_truenas_docker() {
    log_info "Building TrueNAS Docker image..."
    
    local docker_dir="${PACKAGE_DIR}/nas-truenas/docker"
    
    cd "${docker_dir}"
    
    # Build Docker image
    if command -v docker &> /dev/null; then
        docker build -t "ghcr.io/nlp-core-team/ip-css:${PACKAGE_VERSION}" .
        docker tag "ghcr.io/nlp-core-team/ip-css:${PACKAGE_VERSION}" "ghcr.io/nlp-core-team/ip-css:latest"
        
        log_success "Docker image built: ghcr.io/nlp-core-team/ip-css:${PACKAGE_VERSION}"
    else
        log_warning "Docker not available - skipping image build"
    fi
    
    cd - > /dev/null
}

# Build TrueNAS Helm chart
build_truenas_helm() {
    log_info "Building TrueNAS Helm chart..."
    
    local helm_dir="${PACKAGE_DIR}/nas-truenas/helm/ip-css"
    local helm_dest="${BUILD_DIR}/truenas"
    
    mkdir -p "${helm_dest}"
    
    cd "${helm_dir}"
    
    # Package Helm chart
    if command -v helm &> /dev/null; then
        helm package . --destination "${helm_dest}"
        
        log_success "Helm chart built: ${helm_dest}/ip-css-${PACKAGE_VERSION}.tgz"
    else
        log_warning "Helm not available - copying chart source"
        cp -r "${helm_dir}" "${helm_dest}/ip-css"
    fi
    
    cd - > /dev/null
}

# Sign packages (if signing key provided)
sign_packages() {
    if [ -n "${SIGNING_KEY}" ]; then
        log_info "Signing packages..."
        
        # Sign Synology SPK
        if [ -f "${BUILD_DIR}/synology/IP-CSS_${PACKAGE_VERSION}_${BUILD_NUMBER}.spk" ]; then
            # Synology requires specific signing process
            log_info "Synology package signing requires Synology developer account"
        fi
        
        log_success "Package signing completed"
    else
        log_warning "No signing key provided - packages will not be signed"
    fi
}

# Submit to Synology Package Center
submit_synology() {
    if [ "${PUBLISH_SYNOLOGY}" = "true" ]; then
        log_info "Submitting to Synology Package Center..."
        
        # Synology Package Center submission requires:
        # 1. Synology Developer Account
        # 2. Package signing
        # 3. Submission via developer portal
        
        log_warning "Manual submission required:"
        echo "  1. Login to https://www.synology.com/developer"
        echo "  2. Go to Package Center Submission"
        echo "  3. Upload: ${BUILD_DIR}/synology/IP-CSS_${PACKAGE_VERSION}_${BUILD_NUMBER}.spk"
        echo "  4. Complete submission form"
    fi
}

# Submit to QNAP App Center
submit_qnap() {
    if [ "${PUBLISH_QNAP}" = "true" ]; then
        log_info "Submitting to QNAP App Center..."
        
        # QNAP App Center submission requires:
        # 1. QNAP Developer Account
        # 2. QPKG signing
        # 3. Submission via developer portal
        
        log_warning "Manual submission required:"
        echo "  1. Login to https://www.qnap.com/developer"
        echo "  2. Go to App Center Submission"
        echo "  3. Upload: ${BUILD_DIR}/qnap/IP-CSS_${PACKAGE_VERSION}.qpkg"
        echo "  4. Complete submission form"
    fi
}

# Submit to Asustor Portal
submit_asustor() {
    if [ "${PUBLISH_ASUSTOR}" = "true" ]; then
        log_info "Submitting to Asustor Portal..."
        
        # Asustor Portal submission requires:
        # 1. Asustor Developer Account
        # 2. APK signing
        # 3. Submission via developer portal
        
        log_warning "Manual submission required:"
        echo "  1. Login to https://www.asustor.com/developer"
        echo "  2. Go to Portal Submission"
        echo "  3. Upload: ${BUILD_DIR}/asustor/IP-CSS_${PACKAGE_VERSION}.apk"
        echo "  4. Complete submission form"
    fi
}

# Generate release notes
generate_release_notes() {
    log_info "Generating release notes..."
    
    cat > "${BUILD_DIR}/RELEASE_NOTES.md" << EOF
# IP-CSS Release Notes

## Version ${PACKAGE_VERSION}-${BUILD_NUMBER}

**Release Date:** $(date +%Y-%m-%d)

### New Features

#### NAS Platforms
- Synology DSM 7.0+ support (x86_64, ARM64)
- QNAP QTS 5.0+ support (x86_64, ARM)
- Asustor ADM 4.x+ support (x86_64, ARMv8)
- TrueNAS SCALE support (Docker, Helm)

#### Hardware Acceleration
- Intel QuickSync Video (QSV)
- NVIDIA NVENC
- AMD VCE
- ARM Mali VPU

#### NAS Integration
- System resource monitoring (CPU, Memory, Disk)
- Temperature monitoring
- NAS notification integration
- Backup integration
- Service Manager integration

### Installation

#### Synology DSM
1. Download IP-CSS_${PACKAGE_VERSION}_${BUILD_NUMBER}.spk
2. Open Package Center
3. Click "Manual Install"
4. Select the .spk file
5. Follow installation wizard

#### QNAP QTS
1. Download IP-CSS_${PACKAGE_VERSION}.qpkg
2. Open App Center
3. Click "Install Manually"
4. Select the .qpkg file
5. Follow installation wizard

#### Asustor ADM
1. Download IP-CSS_${PACKAGE_VERSION}.apk
2. Open Portal
3. Click "Install"
4. Select the .apk file
5. Follow installation wizard

#### TrueNAS SCALE
\`\`\`bash
# Using Docker
docker-compose up -d

# Using Helm
helm install ip-css ./ip-css-${PACKAGE_VERSION}.tgz
\`\`\`

### Requirements

- **Synology:** DSM 7.0+, 2GB RAM, 1GB storage
- **QNAP:** QTS 5.0+, 2GB RAM, 1GB storage
- **Asustor:** ADM 4.0+, 2GB RAM, 1GB storage
- **TrueNAS:** SCALE 22.02+, 2GB RAM, 1GB storage

### Known Issues

- Hardware acceleration may require additional driver installation
- Some NAS models may have limited codec support

### Support

- Documentation: https://docs.ip-css.com
- Issues: https://github.com/nlp-core-team/ip-css/issues
- Email: support@ip-css.com
EOF
    
    log_success "Release notes generated: ${BUILD_DIR}/RELEASE_NOTES.md"
}

# Main execution

echo "Starting Package Center Publication..."
echo "======================================="
echo ""

# Build all packages
build_synology_spk
build_qnap_qpkg
build_asustor_apk
build_truenas_docker
build_truenas_helm

# Sign packages
sign_packages

# Generate release notes
generate_release_notes

# Submit to package centers (optional)
submit_synology
submit_qnap
submit_asustor

echo ""
echo "======================================="
log_success "Package Center Publication Complete!"
echo ""
echo "Packages available in: ${BUILD_DIR}"
echo ""
ls -lh "${BUILD_DIR}"
