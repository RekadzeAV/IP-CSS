# Скрипт для обновления дат в документации
# Обновляет все даты на текущую системную дату

param([switch]$ShowHelp)

if ($ShowHelp) {
    Write-Host "Replace hard-coded legacy date strings in markdown under the current directory (Get-ChildItem .)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\update-dates.ps1 -ShowHelp"
    Write-Host "  .\scripts\update-dates.ps1"
    Write-Host ""
    Write-Host "Run from repository root so .md files are found. Exit codes: 0 after scan/write."
    exit 0
}

$currentDate = Get-Date -Format "dd MMMM yyyy"
$currentYear = Get-Date -Format "yyyy"
$currentMonth = Get-Date -Format "MMMM"
$currentDateShort = Get-Date -Format "yyyy-MM-dd"

Write-Host "Текущая дата: $currentDate" -ForegroundColor Green
Write-Host "Обновление дат в документации..." -ForegroundColor Yellow

# UTF-8 byte keys/values keep this file ASCII-safe for Windows PowerShell 5.1 parsers
$replacements = @{}
$replacements[[System.Text.Encoding]::UTF8.GetString([byte[]](208,175,208,189,208,178,208,176,209,128,209,140,32,50,48,50,53))] = [System.Text.Encoding]::UTF8.GetString([byte[]](208,175,208,189,208,178,208,176,209,128,209,140,32,50,48,50,54))
$replacements[[System.Text.Encoding]::UTF8.GetString([byte[]](208,148,208,181,208,186,208,176,208,177,209,128,209,140,32,50,48,50,53))] = [System.Text.Encoding]::UTF8.GetString([byte[]](208,175,208,189,208,178,208,176,209,128,209,140,32,50,48,50,54))
$replacements[[System.Text.Encoding]::UTF8.GetString([byte[]](209,143,208,189,208,178,208,176,209,128,209,140,32,50,48,50,53))] = [System.Text.Encoding]::UTF8.GetString([byte[]](209,143,208,189,208,178,208,176,209,128,209,140,32,50,48,50,54))
$replacements[[System.Text.Encoding]::UTF8.GetString([byte[]](208,180,208,181,208,186,208,176,208,177,209,128,209,140,32,50,48,50,53))] = [System.Text.Encoding]::UTF8.GetString([byte[]](209,143,208,189,208,178,208,176,209,128,209,140,32,50,48,50,54))
$replacements[[System.Text.Encoding]::UTF8.GetString([byte[]](50,48,50,53,45,48,49))] = [System.Text.Encoding]::UTF8.GetString([byte[]](50,48,50,54,45,48,49))
$replacements[[System.Text.Encoding]::UTF8.GetString([byte[]](50,48,50,53,45,49,50))] = [System.Text.Encoding]::UTF8.GetString([byte[]](50,48,50,54,45,48,49))
$replacements[[System.Text.Encoding]::UTF8.GetString([byte[]](50,55,32,208,180,208,181,208,186,208,176,208,177,209,128,209,143,32,50,48,50,53))] = [System.Text.Encoding]::UTF8.GetString([byte[]](50,54,32,209,143,208,189,208,178,208,176,209,128,209,143,32,50,48,50,54))
$replacements[[System.Text.Encoding]::UTF8.GetString([byte[]](50,56,32,209,143,208,189,208,178,208,176,209,128,209,143,32,50,48,50,53))] = [System.Text.Encoding]::UTF8.GetString([byte[]](50,54,32,209,143,208,189,208,178,208,176,209,128,209,143,32,50,48,50,54))
$replacements[[System.Text.Encoding]::UTF8.GetString([byte[]](49,53,32,209,143,208,189,208,178,208,176,209,128,209,143,32,50,48,50,53))] = [System.Text.Encoding]::UTF8.GetString([byte[]](50,54,32,209,143,208,189,208,178,208,176,209,128,209,143,32,50,48,50,54))
$replacements[[System.Text.Encoding]::UTF8.GetString([byte[]](50,48,50,53,45,48,49,45,50,55))] = [System.Text.Encoding]::UTF8.GetString([byte[]](50,48,50,54,45,48,49,45,50,54))
$replacements[[System.Text.Encoding]::UTF8.GetString([byte[]](50,48,50,53,45,48,49,45,50,56))] = [System.Text.Encoding]::UTF8.GetString([byte[]](50,48,50,54,45,48,49,45,50,54))

# Получаем все .md файлы, исключая архивные и node_modules
$files = Get-ChildItem -Path . -Include *.md -Recurse -File |
    Where-Object {
        $_.FullName -notmatch 'archive|OLD-DOC|node_modules|\.git|build' -and
        $_.FullName -notmatch 'docs\\docs\\archive|docs\\archive'
    }

$updatedCount = 0

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $originalContent = $content
    $fileUpdated = $false

    foreach ($oldDate in $replacements.Keys) {
        $newDate = $replacements[$oldDate]
        if ($content -match [regex]::Escape($oldDate)) {
            $content = $content -replace [regex]::Escape($oldDate), $newDate
            $fileUpdated = $true
        }
    }

    if ($fileUpdated) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        $updatedCount++
        Write-Host "Обновлен: $($file.FullName)" -ForegroundColor Cyan
    }
}

Write-Host "`nОбновлено файлов: $updatedCount" -ForegroundColor Green
Write-Host "Ревизия документации завершена!" -ForegroundColor Green
