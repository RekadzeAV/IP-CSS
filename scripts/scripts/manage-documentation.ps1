# Скрипт управления документацией проекта IP-CSS
# Версия: 1.0
# Дата создания: Январь 2025
#
# Функционал:
# 1. Создание новых документов с версией "Alfa-0.0.1"
# 2. Обновление существующих документов с инкрементом версии
# 3. Слияние нескольких документов с архивацией исходных

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("create", "update", "merge")]
    [string]$Action = "create",

    [Parameter(Mandatory=$false)]
    [string]$Document = "",

    [Parameter(Mandatory=$false)]
    [string[]]$SourceDocuments = @(),

    [Parameter(Mandatory=$false)]
    [string]$OutputDocument = "",

    [Parameter(Mandatory=$false)]
    [string]$TargetDirectory = "",

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

# Получение версии проекта из gradle.properties
function Get-ProjectVersion {
    $gradleProps = Join-Path $PSScriptRoot "..\gradle.properties"
    if (Test-Path $gradleProps) {
        $content = Get-Content $gradleProps -Raw
        if ($content -match 'version=(.+)') {
            return $matches[1].Trim()
        }
    }
    return "Alfa-0.0.1"
}

# Инкремент версии проекта
function Increment-Version {
    param([string]$Version)

    if ($Version -match '^Alfa-(\d+)\.(\d+)\.(\d+)$') {
        $major = [int]$matches[1]
        $minor = [int]$matches[2]
        $patch = [int]$matches[3]

        # Инкрементируем последнюю цифру (patch)
        $patch++

        return "Alfa-$major.$minor.$patch"
    }

    Write-Warning "Не удалось распарсить версию: $Version. Используется версия по умолчанию."
    return "Alfa-0.0.2"
}

# Определение целевого каталога для документации
function Get-TargetDirectory {
    param([string]$DocumentName)

    # Если указан явно, используем его
    if ($script:TargetDirectory -and (Test-Path $script:TargetDirectory)) {
        return $script:TargetDirectory
    }

    $projectRoot = Split-Path $PSScriptRoot -Parent

    # Определяем по имени файла и структуре проекта
    $docsDir = Join-Path $projectRoot "docs"

    # Если файл начинается с определенных префиксов, размещаем в docs/
    $docsPrefixes = @("ARCHITECTURE", "API", "DEPLOYMENT", "DEVELOPMENT", "INTEGRATION",
                      "ONVIF", "RTSP", "WEBSOCKET", "LICENSE", "TESTING", "CONFIGURATION",
                      "SECURITY", "PERFORMANCE", "TROUBLESHOOTING", "ADMINISTRATOR",
                      "OPERATOR", "USER", "IMPLEMENTATION", "MISSING", "REQUIRED")

    foreach ($prefix in $docsPrefixes) {
        if ($DocumentName -like "$prefix*") {
            return $docsDir
        }
    }

    # Для остальных файлов - корень проекта
    return $projectRoot
}

# Извлечение версии из документа
function Get-DocumentVersion {
    param([string]$DocumentPath)

    if (-not (Test-Path $DocumentPath)) {
        return $null
    }

    $content = Get-Content $DocumentPath -Raw -Encoding UTF8

    # Ищем версию проекта используя Select-String для правильной работы с UTF-8
    $versionPattern = '\*\*Версия проекта:\*\* (Alfa-[0-9]+\.[0-9]+\.[0-9]+)'
    $match = [regex]::Match($content, $versionPattern)
    if ($match.Success) {
        return $match.Groups[1].Value
    }

    # Ищем версию документации (если нет версии проекта)
    $docVersionPattern = '\*\*Версия документации:\*\* ([0-9]+\.[0-9]+(\.[0-9]+)?)'
    $docMatch = [regex]::Match($content, $docVersionPattern)
    if ($docMatch.Success) {
        return $null  # Версия документации не используется для инкремента
    }

    return $null
}

# Создание нового документа
function New-Document {
    param(
        [string]$DocumentPath,
        [string]$Title,
        [string]$Version
    )

    $directory = Split-Path $DocumentPath -Parent
    if (-not (Test-Path $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }

    $dateFormatted = (Get-Date -Format "d MMMM yyyy")

    $content = @"
# $Title

**Версия проекта:** $Version
**Дата создания:** $dateFormatted

---

## Описание

[Описание документа]

---

**Версия проекта:** $Version
**Дата последнего обновления:** $dateFormatted
"@

    Set-Content -Path $DocumentPath -Value $content -Encoding UTF8
    Write-Success "✓ Документ создан: $DocumentPath"
}

# Обновление существующего документа
function Update-Document {
    param(
        [string]$DocumentPath,
        [string]$NewVersion
    )

    if (-not (Test-Path $DocumentPath)) {
        Write-Error "Документ не найден: $DocumentPath"
        return $false
    }

    $content = Get-Content $DocumentPath -Raw -Encoding UTF8
    $dateFormatted = (Get-Date -Format "d MMMM yyyy")

    # Обновление версии проекта
    $versionPattern = '\*\*Версия проекта:\*\* (Alfa-[0-9]+\.[0-9]+\.[0-9]+)'
    $versionMatch = [regex]::Match($content, $versionPattern)
    if ($versionMatch.Success) {
        $content = $content -replace $versionPattern, "**Версия проекта:** $NewVersion"
        Write-Success "✓ Версия проекта обновлена: $NewVersion"
    } else {
        # Добавляем версию проекта, если её нет
        $content = $content -replace '(^# .+?\r?\n)', "`$1`n**Версия проекта:** $NewVersion`n"
        Write-Warning "⚠ Версия проекта добавлена: $NewVersion"
    }

    # Обновление даты последнего обновления
    $datePattern = '\*\*Дата последнего обновления:\*\* .+'
    $dateMatch = [regex]::Match($content, $datePattern)
    if ($dateMatch.Success) {
        $content = $content -replace $datePattern, "**Дата последнего обновления:** $dateFormatted"
    } else {
        # Добавляем дату, если её нет
        $escapedVersion = [regex]::Escape($NewVersion)
        $versionCheckPattern = "(\*\*Версия проекта:\*\* $escapedVersion)"
        $versionCheckMatch = [regex]::Match($content, $versionCheckPattern)
        if ($versionCheckMatch.Success) {
            $content = $content -replace $versionCheckPattern, "`$1`n**Дата последнего обновления:** $dateFormatted"
        }
    }

    Set-Content -Path $DocumentPath -Value $content -Encoding UTF8 -NoNewline
    Write-Success "✓ Документ обновлен: $DocumentPath"

    return $true
}

# Архивирование документа
function Archive-Document {
    param(
        [string]$DocumentPath,
        [string]$ArchiveDate
    )

    $projectRoot = Split-Path $PSScriptRoot -Parent
    $archiveDir = Join-Path $projectRoot "docs\archive\$ArchiveDate"
    $documentName = Split-Path $DocumentPath -Leaf
    $archivePath = Join-Path $archiveDir $documentName

    if (-not (Test-Path $archiveDir)) {
        New-Item -ItemType Directory -Path $archiveDir -Force | Out-Null
    }

    Copy-Item -Path $DocumentPath -Destination $archivePath -Force
    Write-Success "✓ Документ архивирован: $archivePath"

    return $archivePath
}

# Слияние документов
function Merge-Documents {
    param(
        [string[]]$SourcePaths,
        [string]$OutputPath,
        [string]$Title,
        [string]$Version,
        [string]$ArchiveDate
    )

    Write-Info "=========================================="
    Write-Info "Слияние документов"
    Write-Info "=========================================="
    Write-Info "Исходные документы: $($SourcePaths.Count)"
    Write-Info "Результирующий документ: $OutputPath"
    Write-Info "Версия: $Version"
    Write-Info "Дата: $ArchiveDate"
    Write-Info ""

    # Архивируем исходные документы
    $archivedPaths = @()
    foreach ($sourcePath in $SourcePaths) {
        if (Test-Path $sourcePath) {
            $archivedPath = Archive-Document -DocumentPath $sourcePath -ArchiveDate $ArchiveDate
            $archivedPaths += $archivedPath

            # Удаляем исходный документ после архивации
            Remove-Item -Path $sourcePath -Force
            Write-Success "✓ Исходный документ удален: $sourcePath"
        } else {
            Write-Warning "⚠ Документ не найден: $sourcePath"
        }
    }

    # Создаем объединенный документ
    $dateFormatted = (Get-Date -Format "d MMMM yyyy")
    $mergedContent = @"
# $Title

**Версия проекта:** $Version
**Дата создания:** $dateFormatted
**Объединено из:** $($SourcePaths.Count) документов

---

"@

    # Объединяем содержимое исходных документов
    foreach ($sourcePath in $SourcePaths) {
        if (Test-Path (Join-Path (Split-Path $PSScriptRoot -Parent) "docs\archive\$ArchiveDate\$([System.IO.Path]::GetFileName($sourcePath))")) {
            $archivedPath = Join-Path (Split-Path $PSScriptRoot -Parent) "docs\archive\$ArchiveDate\$([System.IO.Path]::GetFileName($sourcePath))"
            $sourceContent = Get-Content $archivedPath -Raw -Encoding UTF8

            # Удаляем заголовки и метаданные из исходного документа
            $sourceContent = $sourceContent -replace '^# .+?\n', ''
            $sourceContent = $sourceContent -replace '\*\*Версия проекта:\*\* .+\n', ''
            $sourceContent = $sourceContent -replace '\*\*Версия документации:\*\* .+\n', ''
            $sourceContent = $sourceContent -replace '\*\*Дата создания:\*\* .+\n', ''
            $sourceContent = $sourceContent -replace '\*\*Дата последнего обновления:\*\* .+\n', ''
            $sourceContent = $sourceContent -replace '\*\*Предыдущая версия:\*\* .+\n', ''
            $sourceContent = $sourceContent -replace '^---\s*\n', ''

            $mergedContent += "## " + [System.IO.Path]::GetFileNameWithoutExtension($sourcePath) + "`n`n"
            $mergedContent += $sourceContent.Trim() + "`n`n---`n`n"
        }
    }

    $mergedContent += @"

---

**Версия проекта:** $Version
**Дата последнего обновления:** $dateFormatted
**Исходные документы архивированы:** $ArchiveDate
"@

    $outputDir = Split-Path $OutputPath -Parent
    if (-not (Test-Path $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    }

    Set-Content -Path $OutputPath -Value $mergedContent -Encoding UTF8
    Write-Success "✓ Объединенный документ создан: $OutputPath"

    Write-Info ""
    Write-Success "=========================================="
    Write-Success "Слияние завершено успешно!"
    Write-Success "=========================================="
}

# Основная логика
$projectVersion = Get-ProjectVersion
$projectRoot = Split-Path $PSScriptRoot -Parent

Write-Info "=========================================="
Write-Info "Управление документацией IP-CSS"
Write-Info "=========================================="
Write-Info "Действие: $Action"
Write-Info "Версия проекта: $projectVersion"
Write-Info ""

switch ($Action) {
    "create" {
        if (-not $Document) {
            Write-Error "Для действия 'create' необходимо указать параметр -Document"
            exit 1
        }

        $docPath = if ([System.IO.Path]::IsPathRooted($Document)) {
            $Document
        } else {
            Join-Path $projectRoot $Document
        }

        $docName = Split-Path $docPath -Leaf
        $title = [System.IO.Path]::GetFileNameWithoutExtension($docName)

        # Определяем целевой каталог
        $targetDir = Get-TargetDirectory -DocumentName $docName
        $finalPath = Join-Path $targetDir $docName

        Write-Info "Создание документа: $finalPath"
        New-Document -DocumentPath $finalPath -Title $title -Version $projectVersion
    }

    "update" {
        if (-not $Document) {
            Write-Error "Для действия 'update' необходимо указать параметр -Document"
            exit 1
        }

        $docPath = if ([System.IO.Path]::IsPathRooted($Document)) {
            $Document
        } else {
            Join-Path $projectRoot $Document
        }

        if (-not (Test-Path $docPath)) {
            Write-Error "Документ не найден: $docPath"
            exit 1
        }

        $currentVersion = Get-DocumentVersion -DocumentPath $docPath
        if ($currentVersion) {
            $newVersion = Increment-Version -Version $currentVersion
        } else {
            $newVersion = Increment-Version -Version $projectVersion
        }

        Write-Info "Обновление документа: $docPath"
        Write-Info "Версия: $currentVersion → $newVersion"

        # Архивируем старую версию
        Archive-Document -DocumentPath $docPath -ArchiveDate $Date

        # Обновляем документ
        Update-Document -DocumentPath $docPath -NewVersion $newVersion
    }

    "merge" {
        if ($SourceDocuments.Count -eq 0) {
            Write-Error "Для действия 'merge' необходимо указать параметр -SourceDocuments"
            exit 1
        }

        if (-not $OutputDocument) {
            Write-Error "Для действия 'merge' необходимо указать параметр -OutputDocument"
            exit 1
        }

        $sourcePaths = @()
        foreach ($sourceDoc in $SourceDocuments) {
            $sourcePath = if ([System.IO.Path]::IsPathRooted($sourceDoc)) {
                $sourceDoc
            } else {
                Join-Path $projectRoot $sourceDoc
            }
            $sourcePaths += $sourcePath
        }

        $outputPath = if ([System.IO.Path]::IsPathRooted($OutputDocument)) {
            $OutputDocument
        } else {
            $targetDir = Get-TargetDirectory -DocumentName (Split-Path $OutputDocument -Leaf)
            Join-Path $targetDir (Split-Path $OutputDocument -Leaf)
        }

        $title = [System.IO.Path]::GetFileNameWithoutExtension((Split-Path $outputPath -Leaf))
        $newVersion = Increment-Version -Version $projectVersion

        Merge-Documents -SourcePaths $sourcePaths -OutputPath $outputPath -Title $title -Version $newVersion -ArchiveDate $Date
    }
}

Write-Info ""
Write-Success "Операция завершена!"

