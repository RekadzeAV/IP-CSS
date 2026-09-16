#!/bin/sh
# Asustor ADM Package Installation Script

APKG_NAME="IP-CSS"
APKG_ROOT="/usr/local/$APKG_NAME"
APKG_DATA="/share/MD0_DATA/$APKG_NAME"

echo "=== IP-CSS Installation (Asustor ADM) ==="
echo "Package: $APKG_NAME"
echo "Install Path: $APKG_ROOT"
echo "Data Path: $APKG_DATA"

# Create directories
echo "Creating directories..."
mkdir -p "$APKG_DATA/recordings"
mkdir -p "$APKG_DATA/config"
mkdir -p "$APKG_DATA/database"
mkdir -p "$APKG_DATA/logs"

# Set permissions
chmod 755 "$APKG_DATA"
chmod -R 755 "$APKG_DATA"/*

# Create default configuration
echo "Creating default configuration..."
cat > "$APKG_DATA/config/application.conf" << 'EOF'
# IP-CSS Configuration for Asustor
server {
    port = 8080
    host = "0.0.0.0"
}

database {
    path = "/usr/local/IP-CSS/data/database/ip-css.db"
}

storage {
    recordings_path = "/share/MD0_DATA/IP-CSS/recordings"
    data_path = "/share/MD0_DATA/IP-CSS"
}

logging {
    level = "INFO"
    file = "/usr/local/IP-CSS/data/logs/ip-css.log"
}

# Asustor Integration
asustor {
    enabled = true
    notify_enabled = true
    aicenter_enabled = true
}
EOF

# Create startup script
echo "Creating startup script..."
cat > "$APKG_ROOT/bin/start-ip-css.sh" << 'EOF'
#!/bin/sh

export JAVA_HOME=/usr/local/java
export PATH=$JAVA_HOME/bin:$PATH
export IP_CSS_HOME=/usr/local/IP-CSS/data
export IP_CSS_CONFIG=/usr/local/IP-CSS/data/config/application.conf

cd /usr/local/IP-CSS/bin
nohup java -jar ip-css-server.jar > /usr/local/IP-CSS/data/logs/stdout.log 2>&1 &
echo $! > /usr/local/IP-CSS/data/ip-css.pid
EOF

chmod +x "$APKG_ROOT/bin/start-ip-css.sh"

# Register with Asustor
echo "Registering with Asustor system..."

# Send notification via actrl
if [ -x /usr/bin/actrl ]; then
    /usr/bin/actrl --notify "IP-CSS installed successfully"
fi

logger -p local0.info "IP-CSS installed successfully"

echo "=== Installation Complete ==="
echo "Access: http://[NAS-IP]:8080"

exit 0
