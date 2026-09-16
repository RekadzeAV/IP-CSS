#!/bin/bash
# Скрипт автоматического инкремента версии продукта IP-CSS
# Версия: 1.0
# Дата создания: Январь 2026
#
# Использование:
#   ./scripts/increment-version.sh          # Инкремент версии на 0.0.1
#   ./scripts/increment-version.sh --dry-run  # Показать новую версию без изменения

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
get_current_version() {
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

# Инкремент версии проекта (увеличивает последнюю цифру на 1)
increment_version() {
    local version="$1"

    if [[ $version =~ ^Alfa-([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
        local major="${BASH_REMATCH[1]}"
        local minor="${BASH_REMATCH[2]}"
        local patch="${BASH_REMATCH[3]}"

        # Инкрементируем последнюю цифру (patch) на 1
        patch=$((patch + 1))

        echo "Alfa-$major.$minor.$patch"
    else
        error "Не удалось распарсить версию: $version"
        exit 1
    fi
}

# Обновление версии в gradle.properties
update_version_in_gradle_props() {
    local gradle_props="$(dirname "$0")/../gradle.properties"
    local new_version="$1"
    local dry_run="$2"

    if [ "$dry_run" = "--dry-run" ]; then
        info "DRY RUN: Версия будет изменена на: $new_version"
        return 0
    fi

    if [ ! -f "$gradle_props" ]; then
        error "Файл gradle.properties не найден: $gradle_props"
        exit 1
    fi

    # Обновляем версию в gradle.properties
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' "s/^version=.*/version=$new_version/" "$gradle_props"
    else
        # Linux
        sed -i "s/^version=.*/version=$new_version/" "$gradle_props"
    fi

    success "✓ Версия обновлена в gradle.properties: $new_version"
}

# Обновление версии в документах (опционально)
update_version_in_documents() {
    local new_version="$1"
    local dry_run="$2"

    if [ "$dry_run" = "--dry-run" ]; then
        info "DRY RUN: Версия в документах будет обновлена на: $new_version"
        return 0
    fi

    local project_root="$(dirname "$0")/.."
    local docs_dir="$project_root/docs"
    local root_docs=("$project_root/README.md" "$project_root/DOCUMENTATION_INDEX.md")

    # Обновляем версию в основных документах
    for doc in "${root_docs[@]}"; do
        if [ -f "$doc" ]; then
            if [[ "$OSTYPE" == "darwin"* ]]; then
                sed -i '' "s/\*\*Версия проекта:\*\* Alfa-[0-9]\+\.[0-9]\+\.[0-9]\+/\*\*Версия проекта:\*\* $new_version/g" "$doc"
            else
                sed -i "s/\*\*Версия проекта:\*\* Alfa-[0-9]\+\.[0-9]\+\.[0-9]\+/\*\*Версия проекта:\*\* $new_version/g" "$doc"
            fi
        fi
    done

    info "✓ Версия обновлена в основных документах"
}

# Главная функция
main() {
    local dry_run="$1"
    local gradle_props="$(dirname "$0")/../gradle.properties"

    if [ ! -f "$gradle_props" ]; then
        error "Файл gradle.properties не найден: $gradle_props"
        exit 1
    fi

    info "=========================================="
    info "Автоматическое обновление версии продукта"
    info "=========================================="

    local current_version=$(get_current_version)
    info "Текущая версия: $current_version"

    local new_version=$(increment_version "$current_version")
    info "Новая версия: $new_version"

    if [ "$dry_run" = "--dry-run" ]; then
        warning "РЕЖИМ ПРОВЕРКИ: изменения не будут применены"
    fi

    update_version_in_gradle_props "$new_version" "$dry_run"
    update_version_in_documents "$new_version" "$dry_run"

    if [ "$dry_run" != "--dry-run" ]; then
        success "=========================================="
        success "Версия успешно обновлена!"
        success "Текущая версия: $current_version → $new_version"
        success "=========================================="
    else
        info "=========================================="
        info "Проверка завершена. Для применения изменений запустите без --dry-run"
        info "=========================================="
    fi
}

# Запуск
main "$@"
