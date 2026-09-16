#!/bin/sh
# Asustor ADM Package Uninstall Script

APKG_NAME="IP-CSS"
APKG_ROOT="/usr/local/$APKG_NAME"
APKG_DATA="/share/MD0_DATA/$APKG_NAME"

echo "=== IP-CSS Uninstallation (Asustor ADM) ==="

# Stop service
echo "Stopping IP-CSS service..."
if [ -f "$APKG_ROOT/bin/stop-ip-css.sh" ]; then
    "$APKG_ROOT/bin/stop-ip-css.sh"
fi

# Preserve data
echo "Preserving data in: $APKG_DATA"
echo "To completely remove data, manually delete this folder."

# Remove package files
echo "Removing package files..."
rm -rf "$APKG_ROOT"

# Send notification
if [ -x /usr/bin/actrl ]; then
    /usr/bin/actrl --notify "IP-CSS uninstalled successfully"
fi

logger -p local0.info "IP-CSS uninstalled successfully"

echo "=== Uninstallation Complete ==="

exit 0
