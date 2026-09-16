# Скрипт архивации документации проекта IP-CSS
# Версия: 1.0
# Дата создания: Январь 2025

param(
    [switch]$ShowHelp,

    [Parameter(Mandatory=$false)]
    [string]$Document = "",

    [Parameter(Mandatory=$false)]
    [string]$Version,

    [Parameter(Mandatory=$false)]
    [string]$NewVersion,

    [Parameter(Mandatory=$false)]
    [string]$Date = (Get-Date -Format "yyyy-MM-dd"),

    [Parameter(Mandatory=$false)]
    [switch]$AutoIncrement
)

if ($ShowHelp) {
    Write-Host "Copy a markdown document into docs/archive/<date> and refresh version/date headers."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\archive-documentation.ps1 -ShowHelp"
    Write-Host "  .\scripts\archive-documentation.ps1 -Document <path> [-Version <v>] [-NewVersion <v>] [-Date yyyy-MM-dd] [-AutoIncrement]"
    Write-Host ""
    Write-Host "Exit codes: 0 success; 1 missing -Document, missing versions, or file errors"
    exit 0
}

if ([string]::IsNullOrWhiteSpace($Document)) {
    Write-Host "Ошибка: укажите -Document или запустите с -ShowHelp." -ForegroundColor Red
    exit 1
}

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

# Автоматическое определение версии, если включен режим AutoIncrement
if ($AutoIncrement) {
    $VersionPattern = '\*\*Версия документации:\*\* (\d+)\.(\d+)(\.(\d+))?'
    if ($Content -match $VersionPattern) {
        $Major = [int]$Matches[1]
        $Minor = [int]$Matches[2]
        $Patch = if ($Matches[4]) { [int]$Matches[4] } else { 0 }

        $Version = if ($Patch -gt 0) { "$Major.$Minor.$Patch" } else { "$Major.$Minor" }

        # Увеличиваем версию на 1 (увеличиваем минорную версию)
        $NewMinor = $Minor + 1
        $NewVersion = if ($Patch -gt 0) {
            "$Major.$Minor.$($Patch + 1)"
        } else {
            "$Major.$NewMinor"
        }

        Write-Info "Автоматически определена версия: $Version → $NewVersion"
    } else {
        Write-Warning "⚠ Версия не найдена в документе. Используется версия 1.0 → 2.0"
        $Version = "1.0"
        $NewVersion = "2.0"
    }
}

# Проверка наличия версий
if (-not $Version -or -not $NewVersion) {
    Write-Error "Ошибка: Не указаны версии. Используйте -Version и -NewVersion или -AutoIncrement"
    exit 1
}

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


