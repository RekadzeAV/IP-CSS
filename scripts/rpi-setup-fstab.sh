#!/bin/bash
# Configure /etc/fstab for automatic SMB mount on Raspberry Pi 4
# Date: 2026-07-08

set -e

# Configuration
NAS_HOST="192.168.10.40"
SHARE_PROJECT="AI-prodgect"
SHARE_BUILDS="AI-prodgect-2"
MOUNT_BASE="/mnt/nas"

# Credentials
SMB_USER="VSCode"
SMB_PASSWORD='$QN~snl0'

echo "=== Configuring /etc/fstab for automatic SMB mounts ==="
echo ""

# Backup fstab
echo "💾 Backing up /etc/fstab..."
sudo cp /etc/fstab /etc/fstab.backup.$(date +%Y%m%d_%H%M%S)

# Create credentials file using awk to avoid shell expansion
CREDENTIALS_FILE="/home/andrey/.smbcredentials"
echo "🔐 Creating credentials file at ${CREDENTIALS_FILE}..."
sudo mkdir -p /home/andrey
awk -v user="$SMB_USER" -v pass="$SMB_PASSWORD" 'BEGIN {print "username=" user "\npassword=" pass "\ndomain=WORKGROUP"}' > "$CREDENTIALS_FILE"
sudo chmod 600 "$CREDENTIALS_FILE"
sudo chown andrey:andrey "$CREDENTIALS_FILE"

# Create mount points
echo "📁 Creating mount points..."
sudo mkdir -p "${MOUNT_BASE}/${SHARE_PROJECT}"
sudo mkdir -p "${MOUNT_BASE}/${SHARE_BUILDS}/release"

# Remove existing entries for these mounts
echo "🧹 Removing old fstab entries..."
sudo sed -i "\|//${NAS_HOST}/${SHARE_PROJECT}|d" /etc/fstab
sudo sed -i "\|//${NAS_HOST}/${SHARE_BUILDS}|d" /etc/fstab

# Add new fstab entries with multiple SMB version support
echo "➕ Adding new fstab entries..."
echo "" | sudo tee -a /etc/fstab
echo "# NAS SMB Shares - Added $(date +%Y-%m-%d)" | sudo tee -a /etc/fstab
echo "# Using sec=ntlmssp for authentication compatibility" | sudo tee -a /etc/fstab
echo "//${NAS_HOST}/${SHARE_PROJECT} ${MOUNT_BASE}/${SHARE_PROJECT} cifs credentials=${CREDENTIALS_FILE},iocharset=utf8,file_mode=0775,dir_mode=0775,noperm,sec=ntlmssp,vers=3.0 0 0" | sudo tee -a /etc/fstab
echo "//${NAS_HOST}/${SHARE_BUILDS} ${MOUNT_BASE}/${SHARE_BUILDS} cifs credentials=${CREDENTIALS_FILE},iocharset=utf8,file_mode=0775,dir_mode=0775,noperm,sec=ntlmssp,vers=3.0 0 0" | sudo tee -a /etc/fstab

# Test fstab
echo "🧪 Testing fstab configuration..."
if sudo mount -a; then
    echo "   ✓ fstab is valid"
else
    echo "   ✗ fstab has errors, restoring backup..."
    sudo cp /etc/fstab.backup.$(date +%Y%m%d_%H%M%S) /etc/fstab
    exit 1
fi

# Verify mounts
echo "✅ Verifying mounts..."
if mountpoint -q "${MOUNT_BASE}/${SHARE_PROJECT}"; then
    echo "   ✓ ${SHARE_PROJECT} mounted"
else
    echo "   ⚠ ${SHARE_PROJECT} not mounted, trying manual mount..."
    sudo mount "${MOUNT_BASE}/${SHARE_PROJECT}"
fi

if mountpoint -q "${MOUNT_BASE}/${SHARE_BUILDS}"; then
    echo "   ✓ ${SHARE_BUILDS} mounted"
else
    echo "   ⚠ ${SHARE_BUILDS} not mounted, trying manual mount..."
    sudo mount "${MOUNT_BASE}/${SHARE_BUILDS}"
fi

# Set permissions
echo "🔧 Setting permissions..."
sudo chown -R andrey:andrey "${MOUNT_BASE}/${SHARE_PROJECT}"
sudo chown -R andrey:andrey "${MOUNT_BASE}/${SHARE_BUILDS}"
sudo chmod -R 775 "${MOUNT_BASE}/${SHARE_PROJECT}"
sudo chmod -R 775 "${MOUNT_BASE}/${SHARE_BUILDS}"

echo ""
echo "✅ fstab configured successfully!"
echo ""
echo "Mount points:"
echo "  Project: ${MOUNT_BASE}/${SHARE_PROJECT}"
echo "  Builds:  ${MOUNT_BASE}/${SHARE_BUILDS}"
echo ""
echo "The shares will now mount automatically on boot."
echo "To test: sudo umount ${MOUNT_BASE}/${SHARE_PROJECT} && sudo mount -a"