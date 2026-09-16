#!/bin/bash

# Скрипт для определения системных путей NAS платформы
# Используется в start.sh и stop.sh для автоматической настройки путей

detect_nas_platform() {
    # Проверка Synology DSM
    if [ -f "/etc/synoinfo.conf" ] || [ -n "${SYNOLOGY}" ]; then
        echo "synology"
        return 0
    fi

    # Проверка QNAP QTS
    if [ -f "/etc/config/qpkg.conf" ] || [ -n "${QNAP}" ]; then
        echo "qnap"
        return 0
    fi

    # Проверка Asustor ADM
    if [ -f "/usr/local/AppCentral/AppCentral.conf" ] || [ -n "${ASUSTOR}" ]; then
        echo "asustor"
        return 0
    fi

    # Проверка TrueNAS CORE (FreeBSD)
    if [ -f "/etc/rc.conf.d/truenas" ] || [ -n "${TRUENAS_CORE}" ]; then
        echo "truenas-core"
        return 0
    fi

    # Проверка TrueNAS SCALE (Linux + Kubernetes)
    if [ -f "/etc/systemd/system/truenas.service" ] || [ -n "${TRUENAS_SCALE}" ]; then
        echo "truenas-scale"
        return 0
    fi

    echo "unknown"
    return 1
}

get_synology_paths() {
    # Определяем путь к первому тому
    if [ -d "/volume1" ]; then
        VOLUME_PATH="/volume1"
    else
        # Ищем первый доступный том
        for i in {1..10}; do
            if [ -d "/volume$i" ]; then
                VOLUME_PATH="/volume$i"
                break
            fi
        done
    fi

    # Fallback на /var/packages если том не найден
    if [ -z "$VOLUME_PATH" ]; then
        VOLUME_PATH="/var/packages/ip-css"
    fi

    echo "DATA_PATH=${VOLUME_PATH}/ip-css/data"
    echo "RECORDINGS_PATH=${VOLUME_PATH}/ip-css/recordings"
    echo "LOGS_PATH=/var/log/ip-css"
    echo "CONFIG_PATH=${VOLUME_PATH}/ip-css/config"
}

get_qnap_paths() {
    # Определяем путь к share
    if [ -d "/share/CACHEDEV1_DATA" ]; then
        SHARE_PATH="/share/CACHEDEV1_DATA"
    else
        # Ищем первый доступный share
        for share in /share/CACHEDEV*; do
            if [ -d "$share" ]; then
                SHARE_PATH="$share"
                break
            fi
        done
    fi

    # Fallback на /share/Public если share не найден
    if [ -z "$SHARE_PATH" ]; then
        SHARE_PATH="/share/Public"
    fi

    echo "DATA_PATH=${SHARE_PATH}/ip-css/data"
    echo "RECORDINGS_PATH=${SHARE_PATH}/ip-css/recordings"
    echo "LOGS_PATH=/var/log/ip-css"
    echo "CONFIG_PATH=${SHARE_PATH}/ip-css/config"
}

get_asustor_paths() {
    # Определяем путь к первому тому
    if [ -d "/volume1" ]; then
        VOLUME_PATH="/volume1"
    else
        # Ищем первый доступный том
        for i in {1..10}; do
            if [ -d "/volume$i" ]; then
                VOLUME_PATH="/volume$i"
                break
            fi
        done
    fi

    # Fallback на /home если том не найден
    if [ -z "$VOLUME_PATH" ]; then
        VOLUME_PATH="/home"
    fi

    echo "DATA_PATH=${VOLUME_PATH}/ip-css/data"
    echo "RECORDINGS_PATH=${VOLUME_PATH}/ip-css/recordings"
    echo "LOGS_PATH=/var/log/ip-css"
    echo "CONFIG_PATH=${VOLUME_PATH}/ip-css/config"
}

get_truenas_paths() {
    # Определяем путь к tank
    if [ -d "/mnt/tank" ]; then
        TANK_PATH="/mnt/tank"
    else
        # Ищем первый доступный pool в /mnt
        for pool in /mnt/*; do
            if [ -d "$pool" ] && [ -r "$pool" ]; then
                TANK_PATH="$pool"
                break
            fi
        done
    fi

    # Fallback на /mnt если pool не найден
    if [ -z "$TANK_PATH" ]; then
        TANK_PATH="/mnt"
    fi

    echo "DATA_PATH=${TANK_PATH}/ip-css/data"
    echo "RECORDINGS_PATH=${TANK_PATH}/ip-css/recordings"
    echo "LOGS_PATH=/var/log/ip-css"
    echo "CONFIG_PATH=${TANK_PATH}/ip-css/config"
}

# Основная логика
PLATFORM=$(detect_nas_platform)

case "$PLATFORM" in
    synology)
        get_synology_paths
        ;;
    qnap)
        get_qnap_paths
        ;;
    asustor)
        get_asustor_paths
        ;;
    truenas-core|truenas-scale)
        get_truenas_paths
        ;;
    *)
        # Пути по умолчанию
        echo "DATA_PATH=/var/packages/ip-css/var/data"
        echo "RECORDINGS_PATH=/var/packages/ip-css/var/recordings"
        echo "LOGS_PATH=/var/packages/ip-css/var/logs"
        echo "CONFIG_PATH=/var/packages/ip-css/etc"
        ;;
esac
