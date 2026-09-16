#!/bin/bash
# QNAP QPKG Start/Stop Script

QPKG_NAME="IP-CSS"
QPKG_ROOT_PATH=`/sbin/getcfg $QPKG_NAME Install_Path -f /etc/config/qpkg.conf`
QPKG_DATA_PATH=`/sbin/getcfg SHARE_DEF defInstall -f /etc/config/def_share.cfg`/IP-CSS

export JAVA_HOME=/usr/java
export PATH=$JAVA_HOME/bin:$PATH
export IP_CSS_HOME="$QPKG_DATA_PATH"
export IP_CSS_CONFIG="$QPKG_DATA_PATH/config/application.conf"

start() {
    echo "Starting IP-CSS..."
    
    # Check if already running
    if [ -f "$QPKG_DATA_PATH/ip-css.pid" ]; then
        PID=$(cat "$QPKG_DATA_PATH/ip-css.pid")
        if ps -p $PID > /dev/null 2>&1; then
            echo "IP-CSS is already running (PID: $PID)"
            return 0
        fi
    fi
    
    # Start server
    cd "$QPKG_ROOT_PATH/bin"
    nohup java -jar ip-css-server.jar > "$QPKG_DATA_PATH/logs/stdout.log" 2>&1 &
    echo $! > "$QPKG_DATA_PATH/ip-css.pid"
    
    sleep 3
    
    # Verify started
    if [ -f "$QPKG_DATA_PATH/ip-css.pid" ]; then
        PID=$(cat "$QPKG_DATA_PATH/ip-css.pid")
        if ps -p $PID > /dev/null 2>&1; then
            echo "IP-CSS started successfully (PID: $PID)"
            logger -p local0.info "IP-CSS started (PID: $PID)"
            
            # Send QNAP notification
            if [ -x /usr/bin/qnotify ]; then
                /usr/bin/qnotify -t "IP-CSS" -m "IP-CSS has been started"
            fi
            
            return 0
        fi
    fi
    
    echo "Failed to start IP-CSS"
    logger -p local0.err "IP-CSS failed to start"
    return 1
}

stop() {
    echo "Stopping IP-CSS..."
    
    if [ -f "$QPKG_DATA_PATH/ip-css.pid" ]; then
        PID=$(cat "$QPKG_DATA_PATH/ip-css.pid")
        if ps -p $PID > /dev/null 2>&1; then
            kill $PID
            sleep 2
            
            # Force kill if still running
            if ps -p $PID > /dev/null 2>&1; then
                kill -9 $PID
            fi
            
            rm -f "$QPKG_DATA_PATH/ip-css.pid"
            echo "IP-CSS stopped"
            logger -p local0.info "IP-CSS stopped"
            
            # Send QNAP notification
            if [ -x /usr/bin/qnotify ]; then
                /usr/bin/qnotify -t "IP-CSS" -m "IP-CSS has been stopped"
            fi
            
            return 0
        fi
    fi
    
    echo "IP-CSS is not running"
    return 0
}

restart() {
    stop
    sleep 2
    start
}

status() {
    if [ -f "$QPKG_DATA_PATH/ip-css.pid" ]; then
        PID=$(cat "$QPKG_DATA_PATH/ip-css.pid")
        if ps -p $PID > /dev/null 2>&1; then
            echo "IP-CSS is running (PID: $PID)"
            return 0
        fi
    fi
    
    echo "IP-CSS is not running"
    return 1
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
