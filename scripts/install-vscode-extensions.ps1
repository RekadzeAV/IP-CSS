# PowerShell скрипт для установки рекомендуемых расширений VS Code / Cursor
# Запуск: .\scripts\install-vscode-extensions.ps1

Write-Host "Установка рекомендуемых расширений VS Code / Cursor..." -ForegroundColor Green

$extensions = @(
    # Kotlin Multiplatform
    "fwcd.kotlin",
    "mathiasfrohlich.kotlin",
    "vscjava.vscode-gradle",
    "naco-siren.gradle-language-support",

    # C++ / Native
    "ms-vscode.cpptools",
    "ms-vscode.cpptools-extension-pack",
    "ms-vscode.cmake-tools",

    # TypeScript/JavaScript/React
    "dbaeumer.vscode-eslint",
    "esbenp.prettier-vscode",
    "ms-vscode.vscode-typescript-next",

    # SQLDelight
    "cashapp.sqldelight",

    # Docker
    "ms-azuretools.vscode-docker",

    # Git
    "eamodio.gitlens",
    "mhutchie.git-graph",

    # Markdown
    "yzhang.markdown-all-in-one",
    "davidanson.vscode-markdownlint",

    # Code Quality
    "detekt.detekt",
    "sonarsource.sonarlint-vscode",

    # Testing
    "firsttris.vscode-jest-runner",

    # Utilities
    "ms-vscode.vscode-json",
    "redhat.vscode-yaml",
    "usernamehw.errorlens",
    "streetsidesoftware.code-spell-checker",
    "streetsidesoftware.code-spell-checker-russian",

    # Project Management
    "alefragnani.project-manager",
    "formulahendry.auto-rename-tag"
)

$codeCommand = "code"
if (Get-Command "cursor" -ErrorAction SilentlyContinue) {
    $codeCommand = "cursor"
    Write-Host "Обнаружен Cursor, используем команду 'cursor'" -ForegroundColor Yellow
} elseif (Get-Command "code" -ErrorAction SilentlyContinue) {
    Write-Host "Используем команду 'code'" -ForegroundColor Yellow
} else {
    Write-Host "ОШИБКА: Не найдена команда 'code' или 'cursor'. Убедитесь, что VS Code или Cursor установлены и добавлены в PATH." -ForegroundColor Red
    exit 1
}

$installed = 0
$failed = 0
$skipped = 0

foreach ($ext in $extensions) {
    Write-Host "`nУстановка: $ext" -ForegroundColor Cyan

    # Проверяем, установлено ли расширение
    $result = & $codeCommand --list-extensions 2>$null | Select-String -Pattern "^$([regex]::Escape($ext))$"

    if ($result) {
        Write-Host "  ✓ Уже установлено" -ForegroundColor Green
        $skipped++
    } else {
        $installResult = & $codeCommand --install-extension $ext 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✓ Успешно установлено" -ForegroundColor Green
            $installed++
        } else {
            Write-Host "  ✗ Ошибка установки" -ForegroundColor Red
            Write-Host "    $installResult" -ForegroundColor Red
            $failed++
        }
    }
}

Write-Host "`n" -NoNewline
Write-Host "=" * 50 -ForegroundColor Cyan
Write-Host "Результаты установки:" -ForegroundColor Yellow
Write-Host "  Установлено: $installed" -ForegroundColor Green
Write-Host "  Пропущено (уже установлено): $skipped" -ForegroundColor Yellow
Write-Host "  Ошибок: $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "=" * 50 -ForegroundColor Cyan

if ($failed -eq 0) {
    Write-Host "`nВсе расширения успешно установлены!" -ForegroundColor Green
    Write-Host "Перезапустите VS Code / Cursor для применения изменений." -ForegroundColor Yellow
} else {
    Write-Host "`nНекоторые расширения не удалось установить. Проверьте логи выше." -ForegroundColor Red
}

