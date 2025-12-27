#!/bin/sh

# Stop script for IP-CSS on QNAP QTS (ARM)

INSTALL_DIR="${QPKG_ROOT:-/share/CACHEDEV1_DATA/.qpkg/ip-css}"
DATA_DIR="$INSTALL_DIR/data"
PID_FILE="$DATA_DIR/ip-css.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "IP-CSS is not running"
    exit 0
fi

# Read PIDs from file (one per line)
while IFS= read -r PID; do
    if [ -n "$PID" ] && ps -p "$PID" > /dev/null 2>&1; then
        echo "Stopping process $PID..."
        kill "$PID"

        # Wait for graceful shutdown (max 10 seconds)
        for i in 1 2 3 4 5 6 7 8 9 10; do
            if ! ps -p "$PID" > /dev/null 2>&1; then
                break
            fi
            sleep 1
        done

        # Force kill if still running
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "Force killing process $PID..."
            kill -9 "$PID"
        fi
    fi
done < "$PID_FILE"

# Remove PID file
rm -f "$PID_FILE"

echo "IP-CSS stopped"
exit 0
