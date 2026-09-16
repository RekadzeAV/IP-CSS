#!/bin/bash
# Bash скрипт для установки рекомендуемых расширений VS Code / Cursor
# Запуск: ./scripts/install-vscode-extensions.sh

echo "Установка рекомендуемых расширений VS Code / Cursor..."

# Определяем команду (code или cursor)
if command -v cursor &> /dev/null; then
    CODE_CMD="cursor"
    echo "Обнаружен Cursor, используем команду 'cursor'"
elif command -v code &> /dev/null; then
    CODE_CMD="code"
    echo "Используем команду 'code'"
else
    echo "ОШИБКА: Не найдена команда 'code' или 'cursor'. Убедитесь, что VS Code или Cursor установлены и добавлены в PATH."
    exit 1
fi

# Список расширений
extensions=(
    # Kotlin Multiplatform
    "fwcd.kotlin"
    "mathiasfrohlich.kotlin"
    "vscjava.vscode-gradle"
    "naco-siren.gradle-language-support"

    # C++ / Native
    "ms-vscode.cpptools"
    "ms-vscode.cpptools-extension-pack"
    "ms-vscode.cmake-tools"

    # TypeScript/JavaScript/React
    "dbaeumer.vscode-eslint"
    "esbenp.prettier-vscode"
    "ms-vscode.vscode-typescript-next"

    # SQLDelight
    "cashapp.sqldelight"

    # Docker
    "ms-azuretools.vscode-docker"

    # Git
    "eamodio.gitlens"
    "mhutchie.git-graph"

    # Markdown
    "yzhang.markdown-all-in-one"
    "davidanson.vscode-markdownlint"

    # Code Quality
    "detekt.detekt"
    "sonarsource.sonarlint-vscode"

    # Testing
    "firsttris.vscode-jest-runner"

    # Utilities
    "ms-vscode.vscode-json"
    "redhat.vscode-yaml"
    "usernamehw.errorlens"
    "streetsidesoftware.code-spell-checker"
    "streetsidesoftware.code-spell-checker-russian"

    # Project Management
    "alefragnani.project-manager",
    "formulahendry.auto-rename-tag",

    # TypeScript Navigation & Code Exploration
    "christian-kohler.path-intellisense"
)

installed=0
failed=0
skipped=0

for ext in "${extensions[@]}"; do
    echo ""
    echo "Установка: $ext"

    # Проверяем, установлено ли расширение
    if $CODE_CMD --list-extensions 2>/dev/null | grep -q "^${ext}$"; then
        echo "  ✓ Уже установлено"
        ((skipped++))
    else
        if $CODE_CMD --install-extension "$ext" 2>/dev/null; then
            echo "  ✓ Успешно установлено"
            ((installed++))
        else
            echo "  ✗ Ошибка установки"
            ((failed++))
        fi
    fi
done

echo ""
echo "=================================================="
echo "Результаты установки:"
echo "  Установлено: $installed"
echo "  Пропущено (уже установлено): $skipped"
echo "  Ошибок: $failed"
echo "=================================================="

if [ $failed -eq 0 ]; then
    echo ""
    echo "Все расширения успешно установлены!"
    echo "Перезапустите VS Code / Cursor для применения изменений."
else
    echo ""
    echo "Некоторые расширения не удалось установить. Проверьте логи выше."
fi


