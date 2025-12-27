#!/bin/sh

# Initialization script for QNAP QPKG
# This script is called during package installation

INSTALL_DIR="/share/CACHEDEV1_DATA/.qpkg/ip-css"
DATA_DIR="/share/CACHEDEV1_DATA/.qpkg/ip-css/data"
CONFIG_DIR="/share/CACHEDEV1_DATA/.qpkg/ip-css/config"

# Create necessary directories
mkdir -p "$DATA_DIR/db"
mkdir -p "$DATA_DIR/recordings"
mkdir -p "$DATA_DIR/logs"
mkdir -p "$CONFIG_DIR"

# Set permissions
chmod -R 755 "$DATA_DIR"
chmod -R 755 "$CONFIG_DIR"

# Create default configuration if not exists
if [ ! -f "$CONFIG_DIR/config.yaml" ]; then
    cat > "$CONFIG_DIR/config.yaml" <<EOF
server:
  port: 8081
  host: 0.0.0.0

web:
  port: 8080
  host: 0.0.0.0

database:
  path: $DATA_DIR/db/ip-css.db

storage:
  recordings_path: $DATA_DIR/recordings

logging:
  level: INFO
  file: $DATA_DIR/logs/ip-css.log
EOF
    chmod 644 "$CONFIG_DIR/config.yaml"
fi

exit 0


