#!/bin/sh

# Uninstall script for IP-CSS on QNAP QTS

INSTALL_DIR="/share/CACHEDEV1_DATA/.qpkg/ip-css"
DATA_DIR="/share/CACHEDEV1_DATA/.qpkg/ip-css/data"
CONFIG_DIR="/share/CACHEDEV1_DATA/.qpkg/ip-css/config"

# Stop the service first
"$INSTALL_DIR/scripts/stop.sh"

# Optionally remove data directory (uncomment if you want to remove user data)
# WARNING: This will delete all recordings and database!
# rm -rf "$DATA_DIR"
# rm -rf "$CONFIG_DIR"

exit 0

