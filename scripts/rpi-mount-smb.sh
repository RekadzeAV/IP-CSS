#!/bin/bash
# SMB Mount Script for Raspberry Pi 4
# Mounts NAS shares for Jenkins builds
# Date: 2026-07-08

set -e

# Configuration
NAS_HOST="192.168.10.40"
SHARE_PROJECT="AI-prodgect"
SHARE_BUILDS="AI-prodgect-2"
MOUNT_BASE="/mnt/nas"

# Credentials from project
SMB_USER="VSCode"
SMB_PASSWORD='$QN~snl0'

echo "=== SMB Mount Configuration for Raspberry Pi 4 ==="
echo "NAS Host: $NAS_HOST"
echo "Shares: $SHARE_PROJECT, $SHARE_BUILDS"
echo ""

# Install required packages
echo "📦 Installing required packages..."
sudo apt-get update -qq
sudo apt-get install -y -qq cifs-utils keyutils

# Create mount points
echo "📁 Creating mount points..."
sudo mkdir -p "${MOUNT_BASE}/${SHARE_PROJECT}"
sudo mkdir -p "${MOUNT_BASE}/${SHARE_BUILDS}/release"

# Create credentials file for SMB using awk to avoid shell expansion
CREDENTIALS_FILE="/home/andrey/.smbcredentials"
echo "🔐 Creating SMB credentials file..."
awk -v user="$SMB_USER" -v pass="$SMB_PASSWORD" 'BEGIN {print "username=" user "\npassword=" pass "\ndomain=WORKGROUP"}' > "$CREDENTIALS_FILE"
sudo chmod 600 "$CREDENTIALS_FILE"
sudo chown andrey:andrey "$CREDENTIALS_FILE"

# Unmount if already mounted
echo "🔄 Checking existing mounts..."
if mountpoint -q "${MOUNT_BASE}/${SHARE_PROJECT}"; then
    echo "   Unmounting ${SHARE_PROJECT}..."
    sudo umount "${MOUNT_BASE}/${SHARE_PROJECT}"
fi
if mountpoint -q "${MOUNT_BASE}/${SHARE_BUILDS}"; then
    echo "   Unmounting ${SHARE_BUILDS}..."
    sudo umount "${MOUNT_BASE}/${SHARE_BUILDS}"
fi

# Mount SMB shares with multiple version attempts
echo "🔗 Mounting SMB shares..."

mount_smb() {
    local share_path="$1"
    local mount_point="$2"
    local share_name="$3"
    
    # Try different SMB versions
    for version in 3.0 2.1 1.0; do
        echo "   Trying SMB version $version for ${share_name}..."
        if sudo mount -t cifs "$share_path" "$mount_point" \
            -o credentials=${CREDENTIALS_FILE},iocharset=utf8,file_mode=0775,dir_mode=0775,noperm,vers=$version,sec=ntlmssp 2>/dev/null; then
            echo "   ✓ Mounted with SMB $version"
            return 0
        fi
    done
    
    # Final attempt without version specification
    echo "   Trying default SMB version for ${share_name}..."
    if sudo mount -t cifs "$share_path" "$mount_point" \
        -o credentials=${CREDENTIALS_FILE},iocharset=utf8,file_mode=0775,dir_mode=0775,noperm,sec=ntlmssp 2>/dev/null; then
        echo "   ✓ Mounted with default SMB version"
        return 0
    fi
    
    return 1
}

if ! mount_smb "//${NAS_HOST}/${SHARE_PROJECT}" "${MOUNT_BASE}/${SHARE_PROJECT}" "${SHARE_PROJECT}"; then
    echo "   ✗ Failed to mount ${SHARE_PROJECT}"
    echo "   Debug info:"
    echo "   - Check credentials: cat ${CREDENTIALS_FILE}"
    echo "   - Test manually: sudo mount -t cifs //${NAS_HOST}/${SHARE_PROJECT} ${MOUNT_BASE}/${SHARE_PROJECT} -o credentials=${CREDENTIALS_FILE},vers=3.0"
    echo "   - Check dmesg: dmesg | tail -20"
    exit 1
fi

if ! mount_smb "//${NAS_HOST}/${SHARE_BUILDS}" "${MOUNT_BASE}/${SHARE_BUILDS}" "${SHARE_BUILDS}"; then
    echo "   ✗ Failed to mount ${SHARE_BUILDS}"
    echo "   Debug info:"
    echo "   - Check credentials: cat ${CREDENTIALS_FILE}"
    echo "   - Test manually: sudo mount -t cifs //${NAS_HOST}/${SHARE_BUILDS} ${MOUNT_BASE}/${SHARE_BUILDS} -o credentials=${CREDENTIALS_FILE},vers=3.0"
    echo "   - Check dmesg: dmesg | tail -20"
    exit 1
fi

# Verify mounts
echo "✅ Verifying mounts..."
if mountpoint -q "${MOUNT_BASE}/${SHARE_PROJECT}"; then
    echo "   ✓ ${SHARE_PROJECT} mounted successfully"
    df -h "${MOUNT_BASE}/${SHARE_PROJECT}" | tail -1
else
    echo "   ✗ Failed to mount ${SHARE_PROJECT}"
    exit 1
fi

if mountpoint -q "${MOUNT_BASE}/${SHARE_BUILDS}"; then
    echo "   ✓ ${SHARE_BUILDS} mounted successfully"
    df -h "${MOUNT_BASE}/${SHARE_BUILDS}" | tail -1
else
    echo "   ✗ Failed to mount ${SHARE_BUILDS}"
    exit 1
fi

# Set permissions
echo "🔧 Setting permissions..."
sudo chown -R andrey:andrey "${MOUNT_BASE}/${SHARE_PROJECT}"
sudo chown -R andrey:andrey "${MOUNT_BASE}/${SHARE_BUILDS}"
sudo chmod -R 775 "${MOUNT_BASE}/${SHARE_PROJECT}"
sudo chmod -R 775 "${MOUNT_BASE}/${SHARE_BUILDS}"

# Create release directory structure
echo "📂 Creating release directory structure..."
sudo mkdir -p "${MOUNT_BASE}/${SHARE_BUILDS}/release"
sudo chown -R andrey:andrey "${MOUNT_BASE}/${SHARE_BUILDS}/release"

echo ""
echo "✅ SMB mounts configured successfully!"
echo ""
echo "Mount points:"
echo "  Project: ${MOUNT_BASE}/${SHARE_PROJECT}"
echo "  Builds:  ${MOUNT_BASE}/${SHARE_BUILDS}"
echo ""
echo "To unmount:"
echo "  sudo umount ${MOUNT_BASE}/${SHARE_PROJECT}"
echo "  sudo umount ${MOUNT_BASE}/${SHARE_BUILDS}"