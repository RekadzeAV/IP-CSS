#!/bin/sh
# Uninstall script for QNAP QPKG (runs before package removal)

INSTALL_DIR="${QPKG_ROOT:-/share/CACHEDEV1_DATA/.qpkg/ip-css}"
DATA_DIR="$INSTALL_DIR/data"
CONFIG_DIR="$INSTALL_DIR/config"

# Stop the service first
if [ -x "$INSTALL_DIR/stop.sh" ]; then
    "$INSTALL_DIR/stop.sh"
fi

# Remove systemd service if exists
if [ -f "/etc/systemd/system/ip-css.service" ]; then
    systemctl stop ip-css > /dev/null 2>&1
    systemctl disable ip-css > /dev/null 2>&1
    rm -f "/etc/systemd/system/ip-css.service"
    systemctl daemon-reload
fi

# Remove init.d script if exists
if [ -f "/etc/init.d/ip-css" ]; then
    if command -v chkconfig > /dev/null 2>&1; then
        chkconfig --del ip-css
    fi
    if command -v update-rc.d > /dev/null 2>&1; then
        update-rc.d -f ip-css remove
    fi
    rm -f "/etc/init.d/ip-css"
fi

# Optionally remove data directory (uncomment if you want to remove user data)
# WARNING: This will delete all recordings and database!
# rm -rf "$DATA_DIR"
# rm -rf "$CONFIG_DIR"

exit 0


