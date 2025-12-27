#!/bin/bash
# Скрипт архивации документации проекта IP-CSS
# Версия: 1.0
# Дата создания: Январь 2025

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Функции для вывода
info() {
    echo -e "${CYAN}$1${NC}"
}

success() {
    echo -e "${GREEN}$1${NC}"
}

error() {
    echo -e "${RED}$1${NC}"
}

warning() {
    echo -e "${YELLOW}$1${NC}"
}

# Проверка аргументов
if [ $# -lt 3 ]; then
    error "Использование: $0 <документ> <старая_версия> <новая_версия> [дата]"
    error "Пример: $0 docs/ARCHITECTURE.md 2.0 3.0 2025-01-27"
    exit 1
fi

DOCUMENT="$1"
VERSION="$2"
NEW_VERSION="$3"
DATE="${4:-$(date +%Y-%m-%d)}"

# Проверка существования документа
if [ ! -f "$DOCUMENT" ]; then
    error "Ошибка: Документ '$DOCUMENT' не найден!"
    exit 1
fi

# Получение абсолютного пути
DOCUMENT_PATH=$(realpath "$DOCUMENT")
DOCUMENT_NAME=$(basename "$DOCUMENT_PATH")
PROJECT_ROOT=$(dirname "$(dirname "$DOCUMENT_PATH")")

# Определение пути к архиву
ARCHIVE_DIR="$PROJECT_ROOT/docs/archive/$DATE"
ARCHIVE_PATH="$ARCHIVE_DIR/$DOCUMENT_NAME"

info "=========================================="
info "Архивация документации"
info "=========================================="
info "Документ: $DOCUMENT_NAME"
info "Версия: $VERSION → $NEW_VERSION"
info "Дата: $DATE"
info ""

# Создание папки архива
if [ ! -d "$ARCHIVE_DIR" ]; then
    info "Создание папки архива: $ARCHIVE_DIR"
    mkdir -p "$ARCHIVE_DIR"
    success "✓ Папка архива создана"
else
    info "Папка архива уже существует: $ARCHIVE_DIR"
fi

# Копирование документа в архив
info "Копирование документа в архив..."
cp "$DOCUMENT_PATH" "$ARCHIVE_PATH"
success "✓ Документ скопирован в архив: $ARCHIVE_PATH"

# Формат даты для документа (DD MMMM YYYY)
if command -v date &> /dev/null; then
    # Для Linux
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        DATE_FORMATTED=$(date -d "$DATE" "+%d %B %Y")
    # Для macOS
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        DATE_FORMATTED=$(date -j -f "%Y-%m-%d" "$DATE" "+%d %B %Y")
    else
        DATE_FORMATTED="$DATE"
    fi
else
    DATE_FORMATTED="$DATE"
fi

# Обновление версии в документе
info "Обновление версии в документе..."

# Создание временного файла
TEMP_FILE=$(mktemp)

# Обновление версии документации
if grep -q "\*\*Версия документации:\*\*" "$DOCUMENT_PATH"; then
    sed -E "s/\*\*Версия документации:\*\* [0-9]+\.[0-9]+(\.[0-9]+)?/\*\*Версия документации:\*\* $NEW_VERSION/g" "$DOCUMENT_PATH" > "$TEMP_FILE"
    mv "$TEMP_FILE" "$DOCUMENT_PATH"
    success "✓ Версия обновлена: $VERSION → $NEW_VERSION"
else
    warning "⚠ Шаблон версии не найден, добавление вручную..."
    # Добавляем версию после первого заголовка
    sed -E "1a\\
**Версия документации:** $NEW_VERSION
" "$DOCUMENT_PATH" > "$TEMP_FILE"
    mv "$TEMP_FILE" "$DOCUMENT_PATH"
fi

# Обновление даты последнего обновления
if grep -q "\*\*Дата последнего обновления:\*\*" "$DOCUMENT_PATH"; then
    sed -E "s/\*\*Дата последнего обновления:\*\* .+/\*\*Дата последнего обновления:\*\* $DATE_FORMATTED/g" "$DOCUMENT_PATH" > "$TEMP_FILE"
    mv "$TEMP_FILE" "$DOCUMENT_PATH"
    success "✓ Дата обновлена: $DATE_FORMATTED"
else
    warning "⚠ Шаблон даты не найден, добавление вручную..."
    # Добавляем дату после версии
    sed -E "/\*\*Версия документации:\*\* $NEW_VERSION/a\\
**Дата последнего обновления:** $DATE_FORMATTED
" "$DOCUMENT_PATH" > "$TEMP_FILE"
    mv "$TEMP_FILE" "$DOCUMENT_PATH"
fi

# Добавление информации о предыдущей версии
if ! grep -q "\*\*Предыдущая версия:\*\*" "$DOCUMENT_PATH"; then
    # Добавляем информацию о предыдущей версии после даты
    sed -E "/\*\*Дата последнего обновления:\*\* $DATE_FORMATTED/a\\
**Предыдущая версия:** $VERSION (архивирована: $DATE_FORMATTED)
" "$DOCUMENT_PATH" > "$TEMP_FILE"
    mv "$TEMP_FILE" "$DOCUMENT_PATH"
    success "✓ Добавлена информация о предыдущей версии"
fi

success "✓ Документ обновлен"

info ""
success "=========================================="
success "Архивация завершена успешно!"
success "=========================================="
info ""
info "Следующие шаги:"
info "1. Проверьте обновленный документ: $DOCUMENT_PATH"
info "2. Обновите DOCUMENTATION_INDEX.md (если нужно)"
info "3. Обновите docs/README.md (если нужно)"
info "4. Обновите docs/archive/README.md"
info ""


