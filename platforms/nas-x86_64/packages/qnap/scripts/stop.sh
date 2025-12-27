#!/bin/sh

# Stop script for IP-CSS on QNAP QTS

DATA_DIR="/share/CACHEDEV1_DATA/.qpkg/ip-css/data"
PID_FILE="$DATA_DIR/ip-css.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "IP-CSS is not running"
    exit 0
fi

# Read PIDs from file
PIDS=$(cat "$PID_FILE")

# Kill all processes
for PID in $PIDS; do
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "Stopping process $PID..."
        kill "$PID"

        # Wait for graceful shutdown
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
done

# Remove PID file
rm -f "$PID_FILE"

echo "IP-CSS stopped"
exit 0

