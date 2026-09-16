#!/bin/sh

# Start script for IP-CSS on QNAP QTS

INSTALL_DIR="${QPKG_ROOT:-/share/CACHEDEV1_DATA/.qpkg/ip-css}"
SCRIPT_DIR="$(cd "$(dirname "${0}")" && pwd)"

# Detect NAS platform and get system paths (script may be in scripts/ or same dir)
if [ -f "$SCRIPT_DIR/scripts/detect-nas-paths.sh" ]; then
    eval "$("$SCRIPT_DIR/scripts/detect-nas-paths.sh")"
elif [ -f "$SCRIPT_DIR/detect-nas-paths.sh" ]; then
    eval "$("$SCRIPT_DIR/detect-nas-paths.sh")"
else
    # Fallback to default paths
    DATA_PATH="$INSTALL_DIR/data"
    RECORDINGS_PATH="$INSTALL_DIR/data/recordings"
    LOGS_PATH="$INSTALL_DIR/data/logs"
    CONFIG_PATH="$INSTALL_DIR/config"
fi

CONFIG_DIR="${CONFIG_PATH:-$INSTALL_DIR/config}"
DATA_DIR="${DATA_PATH:-$INSTALL_DIR/data}"
RECORDINGS_DIR="${RECORDINGS_PATH:-$INSTALL_DIR/data/recordings}"
LOGS_DIR="${LOGS_PATH:-$INSTALL_DIR/data/logs}"

PID_FILE="$DATA_DIR/ip-css.pid"
LOG_FILE="$LOGS_DIR/ip-css.log"
JAVA_BIN="${JAVA_BIN:-java}"

# Create directories if they don't exist
mkdir -p "$LOGS_DIR"
mkdir -p "$DATA_DIR/db"
mkdir -p "$RECORDINGS_DIR"
mkdir -p "$DATA_DIR/screenshots"

# Check if already running
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(head -n 1 "$PID_FILE")
    if ps -p "$OLD_PID" > /dev/null 2>&1; then
        echo "IP-CSS is already running (PID: $OLD_PID)"
        exit 1
    fi
    rm -f "$PID_FILE"
fi

# Check if Java is available
if ! command -v "$JAVA_BIN" > /dev/null 2>&1; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 17 or later via QNAP App Center"
    exit 1
fi

# Export environment variables
export DATABASE_PATH="$DATA_DIR/db/ip-css.db"
export RECORDINGS_PATH="$RECORDINGS_DIR"
export SCREENSHOTS_PATH="$DATA_DIR/screenshots"
export JWT_SECRET="${JWT_SECRET:-$(openssl rand -hex 32 2>/dev/null || echo 'change-this-in-production')}"
export REDIS_HOST="${REDIS_HOST:-localhost}"
export REDIS_PORT="${REDIS_PORT:-6379}"

# Start API server
cd "$INSTALL_DIR"
nohup "$JAVA_BIN" \
    -Xmx512m \
    -Xms256m \
    -jar "$INSTALL_DIR/lib/server.jar" \
    -config="$CONFIG_DIR/config.yaml" \
    >> "$LOG_FILE" 2>&1 &

API_PID=$!
echo "$API_PID" > "$PID_FILE"

# Wait a bit for API to start
sleep 3

# Check if API started successfully
if ! ps -p "$API_PID" > /dev/null 2>&1; then
    echo "Error: API server failed to start"
    exit 1
fi

# Start Web server if Next.js dist exists
if [ -d "$INSTALL_DIR/web/dist" ] && [ -f "$INSTALL_DIR/web/dist/server.js" ]; then
    cd "$INSTALL_DIR/web/dist"
    export NEXT_PUBLIC_API_URL="http://localhost:8081"
    nohup node server.js \
        --port 8080 \
        >> "$LOG_FILE" 2>&1 &

    WEB_PID=$!
    echo "$WEB_PID" >> "$PID_FILE"
    echo "IP-CSS started (API PID: $API_PID, Web PID: $WEB_PID)"
else
    echo "IP-CSS API started (PID: $API_PID)"
    echo "Warning: Web UI not found, only API is running"
fi

exit 0
