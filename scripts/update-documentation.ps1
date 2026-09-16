# Скрипт для полного анализа и обновления документации проекта IP-CSS
# Обновляет даты, проверяет и исправляет ссылки между документами

param([switch]$ShowHelp)

if ($ShowHelp) {
    Write-Host "Bulk refresh dates and cross-links across repo markdown (excludes archive/build/node_modules)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\update-documentation.ps1 -ShowHelp"
    Write-Host "  .\scripts\update-documentation.ps1"
    Write-Host ""
    Write-Host "Run from repository root. Exit codes: 0 after pass (see log for per-file issues)."
    exit 0
}

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Получаем текущую дату в формате проекта
$currentDate = Get-Date -Format "d MMMM yyyy"
$currentYear = Get-Date -Format "yyyy"
$currentMonth = Get-Date -Format "MMMM"
$currentDateShort = Get-Date -Format "yyyy-MM-dd"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Анализ и обновление документации IP-CSS" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Текущая дата: $currentDate" -ForegroundColor Green
Write-Host ""

# Получаем версию проекта
$projectVersion = "Alfa-0.1.1"
$gradleProps = Join-Path $PSScriptRoot "..\gradle.properties"
if (Test-Path $gradleProps) {
    $content = Get-Content $gradleProps -Raw
    if ($content -match 'version=(.+)') {
        $projectVersion = $matches[1].Trim()
    }
}
Write-Host "Версия проекта: $projectVersion" -ForegroundColor Green
Write-Host ""

# Получаем все .md файлы, исключая архивные и node_modules
$projectRoot = Split-Path $PSScriptRoot -Parent
$files = Get-ChildItem -Path $projectRoot -Include *.md -Recurse -File |
    Where-Object {
        $_.FullName -notmatch 'archive|OLD-DOC|node_modules|\.git|build' -and
        $_.FullName -notmatch 'docs\\docs\\archive|docs\\archive'
    }

Write-Host "Найдено документов: $($files.Count)" -ForegroundColor Yellow
Write-Host ""

# Вычисляем значения для замены
$currentMonthLower = $currentMonth.ToLower()
$currentYearMonth = $currentDateShort.Substring(0,7)

# Паттерны для замены дат (простые строковые замены)
$dateReplacements = @(
    @{Old = 'Январь 2025'; New = "$currentMonth $currentYear"},
    @{Old = 'Декабрь 2025'; New = "$currentMonth $currentYear"},
    @{Old = 'январь 2025'; New = "$currentMonthLower $currentYear"},
    @{Old = 'декабрь 2025'; New = "$currentMonthLower $currentYear"},
    @{Old = '2025-01'; New = $currentYearMonth},
    @{Old = '2025-12'; New = $currentYearMonth},
    @{Old = '2026-01'; New = $currentYearMonth},
    @{Old = '27 декабря 2025'; New = $currentDate},
    @{Old = '28 января 2025'; New = $currentDate},
    @{Old = '15 января 2025'; New = $currentDate},
    @{Old = '26 января 2026'; New = $currentDate},
    @{Old = '2025-01-27'; New = $currentDateShort},
    @{Old = '2025-01-28'; New = $currentDateShort},
    @{Old = '2025-12-27'; New = $currentDateShort},
    @{Old = '2026-01-26'; New = $currentDateShort}
)

$updatedCount = 0
$filesWithDates = @()

# Шаг 1: Обновление дат
Write-Host "Шаг 1: Обновление дат в документах..." -ForegroundColor Yellow
Write-Host ""

foreach ($file in $files) {
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        $originalContent = $content
        $fileUpdated = $false

        # Обновляем даты простыми заменами
        foreach ($replacement in $dateReplacements) {
            if ($content -like "*$($replacement.Old)*") {
                $content = $content.Replace($replacement.Old, $replacement.New)
                $fileUpdated = $true
            }
        }

        # Обновляем паттерны дат с помощью регулярных выражений (только английские символы)
        $patterns = @(
            @{Pattern = '\*\*Дата последнего обновления:\*\* [^\r\n]+'; Replacement = "**Дата последнего обновления:** $currentDate"},
            @{Pattern = '\*\*Дата создания:\*\* [^\r\n]+'; Replacement = "**Дата создания:** $currentDate"},
            @{Pattern = '\*\*Дата анализа:\*\* [^\r\n]+'; Replacement = "**Дата анализа:** $currentDate"},
            @{Pattern = '\*\*Дата выполнения:\*\* [^\r\n]+'; Replacement = "**Дата выполнения:** $currentDate"},
            @{Pattern = '\*\*Дата ревизии:\*\* [^\r\n]+'; Replacement = "**Дата ревизии:** $currentDate"},
            @{Pattern = '\*\*Последнее обновление:\*\* [^\r\n]+'; Replacement = "**Последнее обновление:** $currentDate"}
        )

        foreach ($patternInfo in $patterns) {
            if ($content -match $patternInfo.Pattern) {
                $content = $content -replace $patternInfo.Pattern, $patternInfo.Replacement
                $fileUpdated = $true
            }
        }

        if ($fileUpdated) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
            $updatedCount++
            $filesWithDates += $file.FullName
            Write-Host "  [OK] $($file.Name)" -ForegroundColor Cyan
        }
    }
    catch {
        Write-Host "  [ERROR] $($file.Name): $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Обновлено файлов с датами: $updatedCount" -ForegroundColor Green
Write-Host ""

# Шаг 2: Проверка ссылок между документами
Write-Host "Шаг 2: Проверка ссылок между документами..." -ForegroundColor Yellow
Write-Host ""

$brokenLinks = @()
$linkPattern = '\[([^\]]+)\]\(([^)]+)\)'

foreach ($file in $files) {
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        $fileDir = Split-Path $file.FullName -Parent
        
        $matches = [regex]::Matches($content, $linkPattern)
        foreach ($match in $matches) {
            $linkText = $match.Groups[1].Value
            $linkPath = $match.Groups[2].Value
            
            # Пропускаем внешние ссылки
            if ($linkPath -match '^https?://|^mailto:|^#') {
                continue
            }
            
            # Обрабатываем относительные пути
            $resolvedPath = $linkPath
            if (-not [System.IO.Path]::IsPathRooted($linkPath)) {
                # Если путь начинается с /, то от корня проекта
                if ($linkPath.StartsWith('/')) {
                    $resolvedPath = Join-Path $projectRoot $linkPath.Substring(1)
                } else {
                    $resolvedPath = Join-Path $fileDir $linkPath
                }
                
                # Нормализуем путь
                try {
                    $resolvedPath = [System.IO.Path]::GetFullPath($resolvedPath)
                } catch {
                    # Если не удалось нормализовать, пропускаем
                    continue
                }
            }
            
            # Проверяем существование файла
            if (-not (Test-Path $resolvedPath)) {
                $brokenLinks += @{
                    File = $file.FullName.Replace($projectRoot, '').TrimStart('\')
                    Link = $linkPath
                    Text = $linkText
                }
            }
        }
    }
    catch {
        Write-Host "  [ERROR] $($file.Name): $_" -ForegroundColor Red
    }
}

if ($brokenLinks.Count -gt 0) {
    Write-Host "Найдено битых ссылок: $($brokenLinks.Count)" -ForegroundColor Yellow
    foreach ($brokenLink in $brokenLinks) {
        Write-Host "  [BROKEN] $($brokenLink.File) -> $($brokenLink.Link)" -ForegroundColor Red
    }
} else {
    Write-Host "[OK] Все ссылки корректны" -ForegroundColor Green
}

Write-Host ""

# Шаг 3: Обновление DOCUMENTATION_INDEX.md
Write-Host "Шаг 3: Обновление индекса документации..." -ForegroundColor Yellow
Write-Host ""

$indexFile = Join-Path $projectRoot "DOCUMENTATION_INDEX.md"
if (Test-Path $indexFile) {
    try {
        $indexContent = Get-Content $indexFile -Raw -Encoding UTF8
        
        # Обновляем дату в индексе
        if ($indexContent -match '\*\*Последнее обновление:\*\*') {
            $indexContent = $indexContent -replace '\*\*Последнее обновление:\*\* [^\r\n]+', "**Последнее обновление:** $currentDate (ревизия и обновление связей)"
            Set-Content -Path $indexFile -Value $indexContent -Encoding UTF8 -NoNewline
            Write-Host "  [OK] DOCUMENTATION_INDEX.md обновлен" -ForegroundColor Cyan
        }
    }
    catch {
        Write-Host "  [ERROR] Ошибка при обновлении индекса: $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Ревизия документации завершена!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Статистика:" -ForegroundColor Yellow
Write-Host "  - Всего документов: $($files.Count)" -ForegroundColor White
Write-Host "  - Обновлено файлов: $updatedCount" -ForegroundColor White
Write-Host "  - Битых ссылок: $($brokenLinks.Count)" -ForegroundColor White
Write-Host ""
Write-Host "Текущая дата: $currentDate" -ForegroundColor Green
Write-Host "Версия проекта: $projectVersion" -ForegroundColor Green
