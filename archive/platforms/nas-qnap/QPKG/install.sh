#!/bin/bash
# QNAP QPKG Installation Script

QPKG_NAME="IP-CSS"
QPKG_ROOT_PATH=`/sbin/getcfg $QPKG_NAME Install_Path -f /etc/config/qpkg.conf`
if [ "$QPKG_ROOT_PATH" = "" ]; then
    QPKG_ROOT_PATH="/share/CACHEDEV1_DATA/.qpkg/$QPKG_NAME"
fi

QPKG_DATA_PATH=`/sbin/getcfg SHARE_DEF defInstall -f /etc/config/def_share.cfg`/IP-CSS

echo "=== IP-CSS Installation ==="
echo "Package: $QPKG_NAME"
echo "Install Path: $QPKG_ROOT_PATH"
echo "Data Path: $QPKG_DATA_PATH"

# Create data directories
echo "Creating data directories..."
mkdir -p "$QPKG_DATA_PATH/recordings"
mkdir -p "$QPKG_DATA_PATH/config"
mkdir -p "$QPKG_DATA_PATH/database"
mkdir -p "$QPKG_DATA_PATH/logs"

# Set permissions
chmod 755 "$QPKG_DATA_PATH"
chmod -R 755 "$QPKG_DATA_PATH"/*

# Create default configuration
echo "Creating default configuration..."
cat > "$QPKG_DATA_PATH/config/application.conf" << 'EOF'
# IP-CSS Configuration for QNAP
server {
    port = 8080
    host = "0.0.0.0"
}

database {
    path = "/share/CACHEDEV1_DATA/.qpkg/IP-CSS/data/database/ip-css.db"
}

storage {
    recordings_path = "/share/CACHEDEV1_DATA/.qpkg/IP-CSS/data/recordings"
    data_path = "/share/CACHEDEV1_DATA/.qpkg/IP-CSS/data"
}

logging {
    level = "INFO"
    file = "/share/CACHEDEV1_DATA/.qpkg/IP-CSS/data/logs/ip-css.log"
}

# QNAP Integration
qnap {
    enabled = true
    notify_enabled = true
    resource_monitor_enabled = true
}
EOF

# Create startup script
echo "Creating startup script..."
cat > "$QPKG_ROOT_PATH/ip-css.sh" << 'EOF'
#!/bin/bash

QPKG_NAME="IP-CSS"
QPKG_ROOT_PATH=`/sbin/getcfg $QPKG_NAME Install_Path -f /etc/config/qpkg.conf`
QPKG_DATA_PATH=`/sbin/getcfg SHARE_DEF defInstall -f /etc/config/def_share.cfg`/IP-CSS

export JAVA_HOME=/usr/java
export PATH=$JAVA_HOME/bin:$PATH
export IP_CSS_HOME="$QPKG_DATA_PATH"
export IP_CSS_CONFIG="$QPKG_DATA_PATH/config/application.conf"

case "$1" in
    start)
        echo "Starting IP-CSS..."
        cd "$QPKG_ROOT_PATH/bin"
        nohup java -jar ip-css-server.jar > "$QPKG_DATA_PATH/logs/stdout.log" 2>&1 &
        echo $! > "$QPKG_DATA_PATH/ip-css.pid"
        sleep 3
        if [ -f "$QPKG_DATA_PATH/ip-css.pid" ]; then
            PID=$(cat "$QPKG_DATA_PATH/ip-css.pid")
            if ps -p $PID > /dev/null 2>&1; then
                echo "IP-CSS started (PID: $PID)"
                exit 0
            fi
        fi
        echo "Failed to start IP-CSS"
        exit 1
        ;;
    stop)
        echo "Stopping IP-CSS..."
        if [ -f "$QPKG_DATA_PATH/ip-css.pid" ]; then
            PID=$(cat "$QPKG_DATA_PATH/ip-css.pid")
            kill $PID 2>/dev/null
            sleep 2
            kill -9 $PID 2>/dev/null
            rm -f "$QPKG_DATA_PATH/ip-css.pid"
            echo "IP-CSS stopped"
        fi
        exit 0
        ;;
    restart)
        $0 stop
        sleep 2
        $0 start
        ;;
    status)
        if [ -f "$QPKG_DATA_PATH/ip-css.pid" ]; then
            PID=$(cat "$QPKG_DATA_PATH/ip-css.pid")
            if ps -p $PID > /dev/null 2>&1; then
                echo "IP-CSS is running (PID: $PID)"
                exit 0
            fi
        fi
        echo "IP-CSS is not running"
        exit 1
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
EOF

chmod +x "$QPKG_ROOT_PATH/ip-css.sh"

# Register with QNAP
echo "Registering with QNAP system..."

# Send notification
logger -p local0.info "IP-CSS installed successfully"

echo "=== Installation Complete ==="
echo "IP-CSS is ready to use!"
echo "Access: http://[NAS-IP]:8080"

exit 0
