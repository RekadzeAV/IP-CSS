#!/bin/bash

# Скрипт для получения пути к папке релиза с автоматическим созданием
# Использование: ./scripts/get-release-dir.sh <platform>
# Примеры:
#   ./scripts/get-release-dir.sh android
#   ./scripts/get-release-dir.sh ios
#   ./scripts/get-release-dir.sh desktop-x86_64

set -e

if [ $# -eq 0 ]; then
    echo "Error: Platform name is required"
    echo "Usage: $0 <platform>"
    echo "Available platforms: android, ios, desktop-x86_64, desktop-arm, sbc-arm, server-x86_64, nas-arm, nas-x86_64"
    exit 1
fi

PLATFORM="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RELEASE_BASE="$PROJECT_ROOT/Release/$PLATFORM"

# Проверка существования базовой папки платформы
if [ ! -d "$RELEASE_BASE" ]; then
    echo "Error: Platform directory does not exist: $RELEASE_BASE"
    echo "Available platforms: android, ios, desktop-x86_64, desktop-arm, sbc-arm, server-x86_64, nas-arm, nas-x86_64"
    exit 1
fi

# Получаем текущую дату в формате YYYY-MM-DD
CURRENT_DATE=$(date +%Y-%m-%d)

# Находим последний номер сборки за сегодня
LAST_BUILD_NUM=0
if [ -d "$RELEASE_BASE" ]; then
    for dir in "$RELEASE_BASE"/${CURRENT_DATE}-*; do
        if [ -d "$dir" ]; then
            # Извлекаем номер из имени папки (формат: YYYY-MM-DD-NNN)
            BUILD_NUM=$(basename "$dir" | sed "s/${CURRENT_DATE}-//" | sed 's/[^0-9]//g')
            if [ -n "$BUILD_NUM" ] && [ "$BUILD_NUM" -gt "$LAST_BUILD_NUM" ]; then
                LAST_BUILD_NUM=$BUILD_NUM
            fi
        fi
    done
fi

# Увеличиваем номер сборки
NEW_BUILD_NUM=$((LAST_BUILD_NUM + 1))

# Форматируем номер сборки с ведущими нулями (001, 002, ...)
BUILD_NUM_FORMATTED=$(printf "%03d" $NEW_BUILD_NUM)

# Создаем имя папки
BUILD_DIR_NAME="${CURRENT_DATE}-${BUILD_NUM_FORMATTED}"
BUILD_DIR_PATH="$RELEASE_BASE/$BUILD_DIR_NAME"

# Создаем папку
mkdir -p "$BUILD_DIR_PATH"

# Выводим путь к созданной папке
echo "$BUILD_DIR_PATH"
