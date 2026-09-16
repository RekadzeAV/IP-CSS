#!/bin/sh
# IP-CSS Docker entrypoint
# Автоматически настраивает окружение для NAS/Docker

set -e

# ========== Настройка пользователя ==========
if [ "$(id -u)" = '0' ]; then
    # Создаём пользователя если нужно
    USER_ID=${PUID:-1000}
    GROUP_ID=${PGID:-1000}
    
    if ! getent group ipcss > /dev/null 2>&1; then
        addgroup -g "$GROUP_ID" ipcss
    fi
    if ! getent passwd ipcss > /dev/null 2>&1; then
        adduser -D -H -u "$USER_ID" -G ipcss ipcss
    fi
    
    # Настройка прав на директории
    chown -R ipcss:ipcss /app/data /app/exports /app/streams 2>/dev/null || true
    
    exec su-exec ipcss "$@"
fi

exec "$@"
