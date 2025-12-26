#!/bin/bash
# Скрипт для установки git hooks

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GIT_HOOKS_DIR="$PROJECT_ROOT/.git/hooks"

echo "Установка git hooks..."

# Создаем директорию hooks, если её нет
mkdir -p "$GIT_HOOKS_DIR"

# Копируем pre-commit hook
if [ -f "$PROJECT_ROOT/.git/hooks/pre-commit" ]; then
    echo "✓ Pre-commit hook уже установлен"
else
    cat > "$GIT_HOOKS_DIR/pre-commit" << 'EOF'
#!/bin/bash
# Pre-commit hook для проверки актуальности структуры проекта

# Генерируем структуру проекта
python3 scripts/generate-project-structure.py

# Проверяем, изменился ли файл структуры
if [ -n "$(git diff --name-only PROJECT_STRUCTURE_AUTO.md)" ]; then
    echo "⚠️  ВНИМАНИЕ: Структура проекта изменилась!"
    echo "Файл PROJECT_STRUCTURE_AUTO.md был обновлен."
    echo "Пожалуйста, добавьте его в коммит:"
    echo "  git add PROJECT_STRUCTURE_AUTO.md"
    echo ""
    read -p "Добавить PROJECT_STRUCTURE_AUTO.md в коммит? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git add PROJECT_STRUCTURE_AUTO.md
        echo "✓ PROJECT_STRUCTURE_AUTO.md добавлен в коммит"
    else
        echo "⚠️  Структура проекта не обновлена. Рекомендуется обновить её перед коммитом."
    fi
fi

exit 0
EOF
    chmod +x "$GIT_HOOKS_DIR/pre-commit"
    echo "✓ Pre-commit hook установлен"
fi

echo "✓ Git hooks установлены успешно"

