#!/bin/sh
# QNAP App Center service entry: receives start | stop | restart
# Called by QTS with QPKG_ROOT set

INSTALL_DIR="${QPKG_ROOT:-/share/CACHEDEV1_DATA/.qpkg/ip-css}"

case "$1" in
    start)
        "$INSTALL_DIR/start.sh"
        ;;
    stop)
        "$INSTALL_DIR/stop.sh"
        ;;
    restart)
        "$INSTALL_DIR/stop.sh"
        sleep 2
        "$INSTALL_DIR/start.sh"
        ;;
    *)
        echo "Usage: $0 {start|stop|restart}"
        exit 1
        ;;
esac

exit 0
