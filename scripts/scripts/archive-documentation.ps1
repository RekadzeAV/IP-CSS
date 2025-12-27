# Скрипт архивации документации проекта IP-CSS
# Версия: 1.0
# Дата создания: Январь 2025

param(
    [Parameter(Mandatory=$true)]
    [string]$Document,

    [Parameter(Mandatory=$true)]
    [string]$Version,

    [Parameter(Mandatory=$true)]
    [string]$NewVersion,

    [Parameter(Mandatory=$false)]
    [string]$Date = (Get-Date -Format "yyyy-MM-dd")
)

# Цвета для вывода
$ErrorColor = "Red"
$SuccessColor = "Green"
$InfoColor = "Cyan"
$WarningColor = "Yellow"

function Write-Info {
    param([string]$Message)
    Write-Host $Message -ForegroundColor $InfoColor
}

function Write-Success {
    param([string]$Message)
    Write-Host $Message -ForegroundColor $SuccessColor
}

function Write-Error {
    param([string]$Message)
    Write-Host $Message -ForegroundColor $ErrorColor
}

function Write-Warning {
    param([string]$Message)
    Write-Host $Message -ForegroundColor $WarningColor
}

# Проверка существования документа
if (-not (Test-Path $Document)) {
    Write-Error "Ошибка: Документ '$Document' не найден!"
    exit 1
}

# Получение абсолютного пути
$DocumentPath = Resolve-Path $Document
$DocumentName = Split-Path $DocumentPath -Leaf
$ProjectRoot = Split-Path (Split-Path $DocumentPath -Parent) -Parent

# Определение пути к архиву
$ArchiveDir = Join-Path $ProjectRoot "docs\archive\$Date"
$ArchivePath = Join-Path $ArchiveDir $DocumentName

Write-Info "=========================================="
Write-Info "Архивация документации"
Write-Info "=========================================="
Write-Info "Документ: $DocumentName"
Write-Info "Версия: $Version → $NewVersion"
Write-Info "Дата: $Date"
Write-Info ""

# Создание папки архива
if (-not (Test-Path $ArchiveDir)) {
    Write-Info "Создание папки архива: $ArchiveDir"
    New-Item -ItemType Directory -Path $ArchiveDir -Force | Out-Null
    Write-Success "✓ Папка архива создана"
} else {
    Write-Info "Папка архива уже существует: $ArchiveDir"
}

# Копирование документа в архив
Write-Info "Копирование документа в архив..."
Copy-Item -Path $DocumentPath -Destination $ArchivePath -Force
Write-Success "✓ Документ скопирован в архив: $ArchivePath"

# Чтение содержимого документа
Write-Info "Обновление версии в документе..."
$Content = Get-Content -Path $DocumentPath -Raw -Encoding UTF8

# Формат даты для документа (DD MMMM YYYY)
$DateFormatted = (Get-Date $Date -Format "d MMMM yyyy")
$DateFormatted = $DateFormatted -replace "(\d+) (\w+) (\d+)", '$1 $2 $3'

# Обновление версии документации
$VersionPattern = '\*\*Версия документации:\*\* \d+\.\d+(\.\d+)?'
if ($Content -match $VersionPattern) {
    $Content = $Content -replace $VersionPattern, "**Версия документации:** $NewVersion"
    Write-Success "✓ Версия обновлена: $Version → $NewVersion"
} else {
    Write-Warning "⚠ Шаблон версии не найден, добавление вручную..."
    # Добавляем версию после первого заголовка
    $Content = $Content -replace '(^# .+?\n)', "`$1`n**Версия документации:** $NewVersion`n"
}

# Обновление даты последнего обновления
$DatePattern = '\*\*Дата последнего обновления:\*\* .+'
if ($Content -match $DatePattern) {
    $Content = $Content -replace $DatePattern, "**Дата последнего обновления:** $DateFormatted"
    Write-Success "✓ Дата обновлена: $DateFormatted"
} else {
    Write-Warning "⚠ Шаблон даты не найден, добавление вручную..."
    # Добавляем дату после версии
    $Content = $Content -replace "(\*\*Версия документации:\*\* $NewVersion)", "`$1`n**Дата последнего обновления:** $DateFormatted"
}

# Добавление информации о предыдущей версии
$PreviousVersionPattern = '\*\*Предыдущая версия:\*\* .+'
if ($Content -notmatch $PreviousVersionPattern) {
    # Добавляем информацию о предыдущей версии после даты
    $Content = $Content -replace "(\*\*Дата последнего обновления:\*\* $DateFormatted)", "`$1`n**Предыдущая версия:** $Version (архивирована: $DateFormatted)"
    Write-Success "✓ Добавлена информация о предыдущей версии"
}

# Сохранение обновленного документа
Set-Content -Path $DocumentPath -Value $Content -Encoding UTF8 -NoNewline
Write-Success "✓ Документ обновлен"

Write-Info ""
Write-Success "=========================================="
Write-Success "Архивация завершена успешно!"
Write-Success "=========================================="
Write-Info ""
Write-Info "Следующие шаги:"
Write-Info "1. Проверьте обновленный документ: $DocumentPath"
Write-Info "2. Обновите DOCUMENTATION_INDEX.md (если нужно)"
Write-Info "3. Обновите docs/README.md (если нужно)"
Write-Info "4. Обновите docs/archive/README.md"
Write-Info ""


