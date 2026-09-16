#!/bin/bash
# QNAP QPKG Removal Script

QPKG_NAME="IP-CSS"
QPKG_ROOT_PATH=`/sbin/getcfg $QPKG_NAME Install_Path -f /etc/config/qpkg.conf`
QPKG_DATA_PATH=`/sbin/getcfg SHARE_DEF defInstall -f /etc/config/def_share.cfg`/IP-CSS

echo "=== IP-CSS Uninstallation ==="

# Stop service if running
echo "Stopping IP-CSS service..."
if [ -f "$QPKG_ROOT_PATH/ip-css.sh" ]; then
    "$QPKG_ROOT_PATH/ip-css.sh" stop
fi

# Remove from QNAP registry
echo "Removing from QNAP registry..."
/sbin/setcfg $QPKG_NAME Enabled FALSE -f /etc/config/qpkg.conf

# Preserve data (don't delete recordings/config)
echo "Preserving data in: $QPKG_DATA_PATH"
echo "To completely remove data, manually delete this folder."

# Remove package files
echo "Removing package files..."
rm -rf "$QPKG_ROOT_PATH"

# Send notification
logger -p local0.info "IP-CSS uninstalled successfully"

echo "=== Uninstallation Complete ==="
echo "Configuration and recordings preserved in:"
echo "  $QPKG_DATA_PATH"

exit 0
