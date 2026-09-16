#!/bin/bash
# NAS Mount Script for IP-CSS
# Date: 2026-06-09

NAS_IP="192.168.10.37"
SHARE_PATH="/storage/recordings"
MOUNT_POINT="/data/recordings"

echo "=== NAS Mount Script ==="
echo "NAS: $NAS_IP"
echo "Share: $SHARE_PATH"
echo "Mount: $MOUNT_POINT"

# Create mount point if it doesn't exist
sudo mkdir -p $MOUNT_POINT

# Unmount if already mounted
if mountpoint -q $MOUNT_POINT; then
    echo "Unmounting existing mount..."
    sudo umount $MOUNT_POINT
fi

# Mount NFS share
echo "Mounting NFS share..."
sudo mount -t nfs ${NAS_IP}:${SHARE_PATH} $MOUNT_POINT

# Verify mount
if mountpoint -q $MOUNT_POINT; then
    echo "✅ NAS mounted successfully"
    df -h $MOUNT_POINT
else
    echo "❌ Failed to mount NAS"
    exit 1
fi

# Set permissions
sudo chown -R 1000:1000 $MOUNT_POINT
sudo chmod -R 755 $MOUNT_POINT

echo "✅ NAS integration complete"
