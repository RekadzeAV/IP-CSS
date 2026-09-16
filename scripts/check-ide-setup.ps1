# Скрипт для проверки настройки IDE
# Использование: .\scripts\check-ide-setup.ps1

param(
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Check IDE / dev tooling (clangd, compile_commands.json, CMake, Node, npm deps hints, repo config files)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\check-ide-setup.ps1 -ShowHelp"
    Write-Host "  .\scripts\check-ide-setup.ps1"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  All checks passed (same condition as [SUCCESS] banner)"
    Write-Host "  1  One or more required checks failed"
    exit 0
}

Write-Host "Проверка настройки IDE..." -ForegroundColor Cyan
Write-Host ""

$allGood = $true

# Проверка clangd
Write-Host "1. Проверка clangd..." -ForegroundColor Yellow
$clangdFound = $false
try {
    $clangdVersion = clangd --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   [OK] Clangd установлен" -ForegroundColor Green
        Write-Host "   Версия: $($clangdVersion[0])" -ForegroundColor Gray
        $clangdFound = $true
    }
} catch {
    # Ignore
}

if (-not $clangdFound) {
    Write-Host "   [FAIL] Clangd не найден" -ForegroundColor Red
    Write-Host "   Рекомендация: choco install llvm" -ForegroundColor Yellow
    $allGood = $false
}

# Проверка compile_commands.json
Write-Host ""
Write-Host "2. Проверка compile_commands.json..." -ForegroundColor Yellow
$compileCommands = "native\compile_commands.json"
if (Test-Path $compileCommands) {
    $fileInfo = Get-Item $compileCommands
    if ($fileInfo.Length -gt 1000) {
        Write-Host "   [OK] compile_commands.json существует ($($fileInfo.Length) байт)" -ForegroundColor Green
    } else {
        Write-Host "   [WARN] compile_commands.json слишком мал" -ForegroundColor Yellow
        $allGood = $false
    }
} else {
    Write-Host "   [FAIL] compile_commands.json не найден" -ForegroundColor Red
    Write-Host "   Рекомендация: .\scripts\generate-compile-commands.ps1" -ForegroundColor Yellow
    $allGood = $false
}

# Проверка CMake
Write-Host ""
Write-Host "3. Проверка CMake..." -ForegroundColor Yellow
try {
    $cmakeVersion = cmake --version 2>&1 | Select-Object -First 1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   [OK] CMake установлен" -ForegroundColor Green
        Write-Host "   $cmakeVersion" -ForegroundColor Gray
    } else {
        Write-Host "   [FAIL] CMake не найден" -ForegroundColor Red
        $allGood = $false
    }
} catch {
    Write-Host "   [FAIL] CMake не найден" -ForegroundColor Red
    $allGood = $false
}

# Проверка Node.js и npm
Write-Host ""
Write-Host "4. Проверка Node.js и npm..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>&1
    $npmVersion = npm --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   [OK] Node.js установлен: $nodeVersion" -ForegroundColor Green
        Write-Host "   [OK] npm установлен: $npmVersion" -ForegroundColor Green
    } else {
        Write-Host "   [FAIL] Node.js не найден" -ForegroundColor Red
        $allGood = $false
    }
} catch {
    Write-Host "   [FAIL] Node.js не найден" -ForegroundColor Red
    $allGood = $false
}

# Проверка зависимостей npm
Write-Host ""
Write-Host "5. Проверка npm зависимостей..." -ForegroundColor Yellow
$packageJson = "server\web\package.json"
$nodeModules = "server\web\node_modules"
if (Test-Path $packageJson) {
    if (Test-Path $nodeModules) {
        Write-Host "   [OK] node_modules существует" -ForegroundColor Green
    } else {
        Write-Host "   [WARN] node_modules не найден" -ForegroundColor Yellow
        Write-Host "   Рекомендация: cd server\web; npm install" -ForegroundColor Yellow
    }
} else {
    Write-Host "   [WARN] package.json не найден" -ForegroundColor Yellow
}

# Проверка конфигурационных файлов
Write-Host ""
Write-Host "6. Проверка конфигурационных файлов..." -ForegroundColor Yellow
$configFiles = @(
    ".clangd",
    "native\.clangd",
    ".clang-format",
    ".vscode\settings.json",
    ".vscode\extensions.json",
    "server\web\.eslintrc.json",
    "server\web\.prettierrc.json",
    "server\web\tsconfig.json"
)

$missingFiles = @()
foreach ($file in $configFiles) {
    if (Test-Path $file) {
        Write-Host "   [OK] $file" -ForegroundColor Green
    } else {
        Write-Host "   [FAIL] $file не найден" -ForegroundColor Red
        $missingFiles += $file
        $allGood = $false
    }
}

# Итоговый результат
Write-Host ""
Write-Host ("=" * 50) -ForegroundColor Cyan
if ($allGood -and $missingFiles.Count -eq 0) {
    Write-Host "[SUCCESS] Все проверки пройдены успешно!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Следующие шаги:" -ForegroundColor Yellow
    Write-Host "1. Установите рекомендуемые расширения VS Code" -ForegroundColor White
    Write-Host "2. Перезапустите языковые серверы (Ctrl+Shift+P -> 'clangd: Restart language server')" -ForegroundColor White
    Write-Host "3. Перезагрузите VS Code для применения всех настроек" -ForegroundColor White
} else {
    Write-Host "[WARN] Некоторые проверки не пройдены" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Рекомендации:" -ForegroundColor Yellow
    if (-not (Test-Path $compileCommands)) {
        Write-Host "- Запустите: .\scripts\generate-compile-commands.ps1" -ForegroundColor White
    }
    if ($missingFiles.Count -gt 0) {
        Write-Host "- Отсутствуют конфигурационные файлы (см. выше)" -ForegroundColor White
    }
    Write-Host ""
    Write-Host "Подробные инструкции: docs\IDE_SETUP_INSTRUCTIONS.md" -ForegroundColor Cyan
}
Write-Host ("=" * 50) -ForegroundColor Cyan

if ($allGood -and $missingFiles.Count -eq 0) {
    exit 0
}
exit 1
