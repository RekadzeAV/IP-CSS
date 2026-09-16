#!/bin/bash

# Скрипт для подготовки проекта к сборке тестового релиза Synology NAS
# Выполняет основные проверки и подготовительные действия

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Подготовка к сборке Synology NAS пакета${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Функция проверки наличия команды
check_command() {
    if ! command -v "$1" > /dev/null 2>&1; then
        echo -e "${RED}✗ $1 не найден${NC}"
        return 1
    else
        echo -e "${GREEN}✓ $1 найден${NC}"
        return 0
    fi
}

# Функция проверки файла
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓ $1 существует${NC}"
        return 0
    else
        echo -e "${RED}✗ $1 не найден${NC}"
        return 1
    fi
}

# Функция проверки директории
check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓ $1 существует${NC}"
        return 0
    else
        echo -e "${RED}✗ $1 не найден${NC}"
        return 1
    fi
}

# Проверка зависимостей
echo -e "${YELLOW}Проверка зависимостей...${NC}"
MISSING_DEPS=0

check_command "java" || MISSING_DEPS=1
check_command "gradle" || MISSING_DEPS=1
check_command "node" || echo -e "${YELLOW}⚠ Node.js не найден (опционально для веб-интерфейса)${NC}"
check_command "npm" || echo -e "${YELLOW}⚠ npm не найден (опционально для веб-интерфейса)${NC}"

if [ $MISSING_DEPS -eq 1 ]; then
    echo -e "${RED}Критические зависимости отсутствуют!${NC}"
    exit 1
fi

echo ""

# Проверка структуры проекта
echo -e "${YELLOW}Проверка структуры проекта...${NC}"
MISSING_FILES=0

check_file "gradle.properties" || MISSING_FILES=1
check_file "scripts/build-nas-package.sh" || MISSING_FILES=1
check_file "platforms/nas-x86_64/packages/synology/INFO" || MISSING_FILES=1
check_file "platforms/nas-x86_64/packages/synology/scripts/preinst" || MISSING_FILES=1
check_file "platforms/nas-x86_64/packages/synology/package/bin/start.sh" || MISSING_FILES=1
check_file "platforms/nas-x86_64/packages/synology/package/bin/stop.sh" || MISSING_FILES=1

if [ $MISSING_FILES -eq 1 ]; then
    echo -e "${RED}Критические файлы отсутствуют!${NC}"
    exit 1
fi

echo ""

# Проверка иконок
echo -e "${YELLOW}Проверка иконок...${NC}"
ICON_MISSING=0

if ! check_file "platforms/nas-x86_64/packages/synology/icons/PACKAGE_ICON.PNG"; then
    ICON_MISSING=1
fi

if ! check_file "platforms/nas-x86_64/packages/synology/icons/PACKAGE_ICON_256.PNG"; then
    ICON_MISSING=1
fi

if [ $ICON_MISSING -eq 1 ]; then
    echo -e "${YELLOW}⚠ Иконки отсутствуют. Создайте их перед сборкой.${NC}"
    echo -e "${YELLOW}  Используйте ImageMagick или графический редактор.${NC}"
fi

echo ""

# Проверка собранных артефактов
echo -e "${YELLOW}Проверка собранных артефактов...${NC}"
ARTIFACTS_MISSING=0

# Проверка API сервера
if ls server/api/build/libs/server-api-*.jar 1> /dev/null 2>&1; then
    echo -e "${GREEN}✓ API сервер собран${NC}"
else
    echo -e "${YELLOW}⚠ API сервер не собран${NC}"
    ARTIFACTS_MISSING=1
fi

# Проверка веб-интерфейса
if [ -d "server/web/.next" ]; then
    echo -e "${GREEN}✓ Веб-интерфейс собран${NC}"
else
    echo -e "${YELLOW}⚠ Веб-интерфейс не собран${NC}"
    ARTIFACTS_MISSING=1
fi

echo ""

# Проверка прав доступа на скрипты
echo -e "${YELLOW}Проверка прав доступа на скрипты...${NC}"
FIX_PERMS=0

for script in platforms/nas-x86_64/packages/synology/package/bin/*.sh \
              platforms/nas-x86_64/packages/synology/scripts/* \
              platforms/nas-arm/packages/synology/package/bin/*.sh \
              platforms/nas-arm/packages/synology/scripts/*; do
    if [ -f "$script" ] && [ ! -x "$script" ]; then
        echo -e "${YELLOW}⚠ $script не имеет прав на выполнение${NC}"
        FIX_PERMS=1
    fi
done

if [ $FIX_PERMS -eq 1 ]; then
    echo -e "${BLUE}Исправление прав доступа...${NC}"
    chmod +x platforms/nas-x86_64/packages/synology/package/bin/*.sh 2>/dev/null || true
    chmod +x platforms/nas-x86_64/packages/synology/scripts/* 2>/dev/null || true
    chmod +x platforms/nas-arm/packages/synology/package/bin/*.sh 2>/dev/null || true
    chmod +x platforms/nas-arm/packages/synology/scripts/* 2>/dev/null || true
    echo -e "${GREEN}✓ Права доступа исправлены${NC}"
fi

echo ""

# Резюме
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Резюме проверки${NC}"
echo -e "${BLUE}========================================${NC}"

if [ $ICON_MISSING -eq 1 ]; then
    echo -e "${RED}✗ Иконки отсутствуют - требуется создать${NC}"
fi

if [ $ARTIFACTS_MISSING -eq 1 ]; then
    echo -e "${YELLOW}⚠ Артефакты не собраны${NC}"
    echo -e "${BLUE}Выполните:${NC}"
    echo -e "  ${BLUE}./gradlew :server:api:build${NC}"
    echo -e "  ${BLUE}cd server/web && npm install && npm run build${NC}"
fi

if [ $ICON_MISSING -eq 0 ] && [ $ARTIFACTS_MISSING -eq 0 ]; then
    echo -e "${GREEN}✓ Все проверки пройдены!${NC}"
    echo -e "${BLUE}Можно выполнять сборку пакета:${NC}"
    echo -e "  ${BLUE}./scripts/build-nas-package.sh synology x86_64 Alfa-0.1.1${NC}"
else
    echo -e "${YELLOW}⚠ Требуется выполнить дополнительные действия${NC}"
    echo -e "${BLUE}См. план: PLAN_SYNOLOGY_RELEASE_FIXES.md${NC}"
fi

echo ""
