#!/bin/sh

# Start script for IP-CSS on QNAP QTS

INSTALL_DIR="/share/CACHEDEV1_DATA/.qpkg/ip-css"
CONFIG_DIR="/share/CACHEDEV1_DATA/.qpkg/ip-css/config"
DATA_DIR="/share/CACHEDEV1_DATA/.qpkg/ip-css/data"
PID_FILE="$DATA_DIR/ip-css.pid"
LOG_FILE="$DATA_DIR/logs/ip-css.log"

# Create directories if they don't exist
mkdir -p "$DATA_DIR/logs"
mkdir -p "$DATA_DIR/db"
mkdir -p "$DATA_DIR/recordings"

# Check if already running
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if ps -p "$OLD_PID" > /dev/null 2>&1; then
        echo "IP-CSS is already running (PID: $OLD_PID)"
        exit 1
    fi
    rm -f "$PID_FILE"
fi

# Start API server
cd "$INSTALL_DIR"
nohup java -jar "$INSTALL_DIR/lib/server.jar" \
    --config "$CONFIG_DIR/config.yaml" \
    --pid-file "$PID_FILE" \
    >> "$LOG_FILE" 2>&1 &

API_PID=$!
echo "$API_PID" > "$PID_FILE"

# Wait a bit for API to start
sleep 2

# Start Web server
cd "$INSTALL_DIR/web/dist"
nohup node server.js \
    --port 8080 \
    --api-url http://localhost:8081 \
    >> "$LOG_FILE" 2>&1 &

WEB_PID=$!
echo "$WEB_PID" >> "$PID_FILE"

echo "IP-CSS started (API PID: $API_PID, Web PID: $WEB_PID)"
exit 0

