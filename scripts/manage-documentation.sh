#!/bin/bash
# Скрипт управления документацией проекта IP-CSS
# Версия: 1.0
# Дата создания: Январь 2025
#
# Функционал:
# 1. Создание новых документов с версией "Alfa-0.0.1"
# 2. Обновление существующих документов с инкрементом версии
# 3. Слияние нескольких документов с архивацией исходных

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

# Получение версии проекта из gradle.properties
get_project_version() {
    local gradle_props="$(dirname "$0")/../gradle.properties"
    if [ -f "$gradle_props" ]; then
        local version=$(grep "^version=" "$gradle_props" | cut -d'=' -f2 | tr -d ' ')
        if [ -n "$version" ]; then
            echo "$version"
            return
        fi
    fi
    echo "Alfa-0.0.1"
}

# Инкремент версии проекта
increment_version() {
    local version="$1"

    if [[ $version =~ ^Alfa-([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
        local major="${BASH_REMATCH[1]}"
        local minor="${BASH_REMATCH[2]}"
        local patch="${BASH_REMATCH[3]}"

        # Инкрементируем последнюю цифру (patch)
        patch=$((patch + 1))

        echo "Alfa-$major.$minor.$patch"
    else
        warning "Не удалось распарсить версию: $version. Используется версия по умолчанию."
        echo "Alfa-0.0.2"
    fi
}

# Определение целевого каталога для документации
get_target_directory() {
    local doc_name="$1"
    local script_dir="$(dirname "$0")"
    local project_root="$(cd "$script_dir/.." && pwd)"

    # Если указан явно, используем его
    if [ -n "$TARGET_DIRECTORY" ] && [ -d "$TARGET_DIRECTORY" ]; then
        echo "$TARGET_DIRECTORY"
        return
    fi

    local docs_dir="$project_root/docs"

    # Определяем по имени файла
    case "$doc_name" in
        ARCHITECTURE*|API*|DEPLOYMENT*|DEVELOPMENT*|INTEGRATION*|ONVIF*|RTSP*|WEBSOCKET*|LICENSE*|TESTING*|CONFIGURATION*|SECURITY*|PERFORMANCE*|TROUBLESHOOTING*|ADMINISTRATOR*|OPERATOR*|USER*|IMPLEMENTATION*|MISSING*|REQUIRED*)
            echo "$docs_dir"
            ;;
        *)
            echo "$project_root"
            ;;
    esac
}

# Извлечение версии из документа
get_document_version() {
    local doc_path="$1"

    if [ ! -f "$doc_path" ]; then
        echo ""
        return
    fi

    local version=$(grep -oP '\*\*Версия проекта:\*\* \K(Alfa-[0-9]+\.[0-9]+\.[0-9]+)' "$doc_path" 2>/dev/null || echo "")
    echo "$version"
}

# Создание нового документа
new_document() {
    local doc_path="$1"
    local title="$2"
    local version="$3"

    local directory="$(dirname "$doc_path")"
    mkdir -p "$directory"

    local date_formatted=$(date "+%d %B %Y" 2>/dev/null || date "+%Y-%m-%d")

    cat > "$doc_path" <<EOF
# $title

**Версия проекта:** $version
**Дата создания:** $date_formatted

---

## Описание

[Описание документа]

---

**Версия проекта:** $version
**Дата последнего обновления:** $date_formatted
EOF

    success "✓ Документ создан: $doc_path"
}

# Обновление существующего документа
update_document() {
    local doc_path="$1"
    local new_version="$2"

    if [ ! -f "$doc_path" ]; then
        error "Документ не найден: $doc_path"
        return 1
    fi

    local date_formatted=$(date "+%d %B %Y" 2>/dev/null || date "+%Y-%m-%d")
    local temp_file=$(mktemp)

    # Обновление версии проекта
    if grep -q '\*\*Версия проекта:\*\*' "$doc_path"; then
        sed -E "s/\*\*Версия проекта:\*\* (Alfa-[0-9]+\.[0-9]+\.[0-9]+)/\*\*Версия проекта:\*\* $new_version/g" "$doc_path" > "$temp_file"
        mv "$temp_file" "$doc_path"
        success "✓ Версия проекта обновлена: $new_version"
    else
        # Добавляем версию проекта, если её нет
        sed -E "1a\\
**Версия проекта:** $new_version
" "$doc_path" > "$temp_file"
        mv "$temp_file" "$doc_path"
        warning "⚠ Версия проекта добавлена: $new_version"
    fi

    # Обновление даты последнего обновления
    if grep -q '\*\*Дата последнего обновления:\*\*' "$doc_path"; then
        sed -E "s/\*\*Дата последнего обновления:\*\* .+/\*\*Дата последнего обновления:\*\* $date_formatted/g" "$doc_path" > "$temp_file"
        mv "$temp_file" "$doc_path"
    else
        # Добавляем дату, если её нет
        sed -E "/\*\*Версия проекта:\*\* $new_version/a\\
**Дата последнего обновления:** $date_formatted
" "$doc_path" > "$temp_file"
        mv "$temp_file" "$doc_path"
    fi

    success "✓ Документ обновлен: $doc_path"
    return 0
}

# Архивирование документа
archive_document() {
    local doc_path="$1"
    local archive_date="$2"

    local script_dir="$(dirname "$0")"
    local project_root="$(cd "$script_dir/.." && pwd)"
    local archive_dir="$project_root/docs/archive/$archive_date"
    local doc_name="$(basename "$doc_path")"
    local archive_path="$archive_dir/$doc_name"

    mkdir -p "$archive_dir"
    cp "$doc_path" "$archive_path"
    success "✓ Документ архивирован: $archive_path"

    echo "$archive_path"
}

# Слияние документов
merge_documents() {
    local source_paths=("$@")
    local output_path="${source_paths[-1]}"
    unset 'source_paths[-1]'
    local title="${source_paths[-1]}"
    unset 'source_paths[-1]'
    local version="${source_paths[-1]}"
    unset 'source_paths[-1]'
    local archive_date="${source_paths[-1]}"
    unset 'source_paths[-1]'

    info "=========================================="
    info "Слияние документов"
    info "=========================================="
    info "Исходные документы: ${#source_paths[@]}"
    info "Результирующий документ: $output_path"
    info "Версия: $version"
    info "Дата: $archive_date"
    info ""

    # Архивируем исходные документы
    local archived_paths=()
    for source_path in "${source_paths[@]}"; do
        if [ -f "$source_path" ]; then
            local archived_path=$(archive_document "$source_path" "$archive_date")
            archived_paths+=("$archived_path")

            # Удаляем исходный документ после архивации
            rm -f "$source_path"
            success "✓ Исходный документ удален: $source_path"
        else
            warning "⚠ Документ не найден: $source_path"
        fi
    done

    # Создаем объединенный документ
    local date_formatted=$(date "+%d %B %Y" 2>/dev/null || date "+%Y-%m-%d")
    local output_dir="$(dirname "$output_path")"
    mkdir -p "$output_dir"

    {
        echo "# $title"
        echo ""
        echo "**Версия проекта:** $version"
        echo "**Дата создания:** $date_formatted"
        echo "**Объединено из:** ${#source_paths[@]} документов"
        echo ""
        echo "---"
        echo ""

        # Объединяем содержимое исходных документов
        for source_path in "${source_paths[@]}"; do
            local script_dir="$(dirname "$0")"
            local project_root="$(cd "$script_dir/.." && pwd)"
            local archived_path="$project_root/docs/archive/$archive_date/$(basename "$source_path")"

            if [ -f "$archived_path" ]; then
                echo "## $(basename "$source_path" .md)"
                echo ""

                # Удаляем заголовки и метаданные из исходного документа
                sed -E '/^# /d; /\*\*Версия проекта:\*\* /d; /\*\*Версия документации:\*\* /d; /\*\*Дата создания:\*\* /d; /\*\*Дата последнего обновления:\*\* /d; /\*\*Предыдущая версия:\*\* /d; /^---$/d' "$archived_path"

                echo ""
                echo "---"
                echo ""
            fi
        done

        echo ""
        echo "---"
        echo ""
        echo "**Версия проекта:** $version"
        echo "**Дата последнего обновления:** $date_formatted"
        echo "**Исходные документы архивированы:** $archive_date"
    } > "$output_path"

    success "✓ Объединенный документ создан: $output_path"

    info ""
    success "=========================================="
    success "Слияние завершено успешно!"
    success "=========================================="
}

# Парсинг аргументов
ACTION="create"
DOCUMENT=""
SOURCE_DOCUMENTS=()
OUTPUT_DOCUMENT=""
TARGET_DIRECTORY=""
DATE=$(date +%Y-%m-%d)

while [[ $# -gt 0 ]]; do
    case $1 in
        --action|-a)
            ACTION="$2"
            shift 2
            ;;
        --document|-d)
            DOCUMENT="$2"
            shift 2
            ;;
        --sources|-s)
            shift
            while [[ $# -gt 0 ]] && [[ ! "$1" =~ ^-- ]]; do
                SOURCE_DOCUMENTS+=("$1")
                shift
            done
            ;;
        --output|-o)
            OUTPUT_DOCUMENT="$2"
            shift 2
            ;;
        --target|-t)
            TARGET_DIRECTORY="$2"
            shift 2
            ;;
        --date)
            DATE="$2"
            shift 2
            ;;
        *)
            error "Неизвестный параметр: $1"
            echo "Использование: $0 [--action create|update|merge] [--document PATH] [--sources PATH1 PATH2 ...] [--output PATH] [--target DIR] [--date YYYY-MM-DD]"
            exit 1
            ;;
    esac
done

# Основная логика
PROJECT_VERSION=$(get_project_version)
SCRIPT_DIR="$(dirname "$0")"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

info "=========================================="
info "Управление документацией IP-CSS"
info "=========================================="
info "Действие: $ACTION"
info "Версия проекта: $PROJECT_VERSION"
info ""

case "$ACTION" in
    create)
        if [ -z "$DOCUMENT" ]; then
            error "Для действия 'create' необходимо указать параметр --document"
            exit 1
        fi

        if [[ "$DOCUMENT" = /* ]]; then
            DOC_PATH="$DOCUMENT"
        else
            DOC_PATH="$PROJECT_ROOT/$DOCUMENT"
        fi

        DOC_NAME=$(basename "$DOC_PATH")
        TITLE=$(basename "$DOC_NAME" .md)

        # Определяем целевой каталог
        TARGET_DIR=$(get_target_directory "$DOC_NAME")
        FINAL_PATH="$TARGET_DIR/$DOC_NAME"

        info "Создание документа: $FINAL_PATH"
        new_document "$FINAL_PATH" "$TITLE" "$PROJECT_VERSION"
        ;;

    update)
        if [ -z "$DOCUMENT" ]; then
            error "Для действия 'update' необходимо указать параметр --document"
            exit 1
        fi

        if [[ "$DOCUMENT" = /* ]]; then
            DOC_PATH="$DOCUMENT"
        else
            DOC_PATH="$PROJECT_ROOT/$DOCUMENT"
        fi

        if [ ! -f "$DOC_PATH" ]; then
            error "Документ не найден: $DOC_PATH"
            exit 1
        fi

        CURRENT_VERSION=$(get_document_version "$DOC_PATH")
        if [ -n "$CURRENT_VERSION" ]; then
            NEW_VERSION=$(increment_version "$CURRENT_VERSION")
        else
            NEW_VERSION=$(increment_version "$PROJECT_VERSION")
        fi

        info "Обновление документа: $DOC_PATH"
        info "Версия: $CURRENT_VERSION → $NEW_VERSION"

        # Архивируем старую версию
        archive_document "$DOC_PATH" "$DATE"

        # Обновляем документ
        update_document "$DOC_PATH" "$NEW_VERSION"
        ;;

    merge)
        if [ ${#SOURCE_DOCUMENTS[@]} -eq 0 ]; then
            error "Для действия 'merge' необходимо указать параметр --sources"
            exit 1
        fi

        if [ -z "$OUTPUT_DOCUMENT" ]; then
            error "Для действия 'merge' необходимо указать параметр --output"
            exit 1
        fi

        SOURCE_PATHS=()
        for source_doc in "${SOURCE_DOCUMENTS[@]}"; do
            if [[ "$source_doc" = /* ]]; then
                SOURCE_PATHS+=("$source_doc")
            else
                SOURCE_PATHS+=("$PROJECT_ROOT/$source_doc")
            fi
        done

        if [[ "$OUTPUT_DOCUMENT" = /* ]]; then
            OUTPUT_PATH="$OUTPUT_DOCUMENT"
        else
            TARGET_DIR=$(get_target_directory "$(basename "$OUTPUT_DOCUMENT")")
            OUTPUT_PATH="$TARGET_DIR/$(basename "$OUTPUT_DOCUMENT")"
        fi

        TITLE=$(basename "$OUTPUT_PATH" .md)
        NEW_VERSION=$(increment_version "$PROJECT_VERSION")

        merge_documents "${SOURCE_PATHS[@]}" "$OUTPUT_PATH" "$TITLE" "$NEW_VERSION" "$DATE"
        ;;

    *)
        error "Неизвестное действие: $ACTION"
        echo "Доступные действия: create, update, merge"
        exit 1
        ;;
esac

info ""
success "Операция завершена!"

