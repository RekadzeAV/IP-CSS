# Create next dated build folder under Release/<platform> and print its path (stdout).
# Usage: .\scripts\get-release-dir.ps1 -Platform android
# Windows PowerShell 5.1: use nested Join-Path (two-argument form only).

param(
    [string]$Platform = "",
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Create Release/<platform>/YYYY-MM-DD-NNN and write the new directory path to stdout."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\get-release-dir.ps1 -ShowHelp"
    Write-Host "  .\scripts\get-release-dir.ps1 -Platform android"
    Write-Host ""
    Write-Host "Examples -Platform: android, ios, desktop-x86_64, desktop-arm, sbc-arm, server-x86_64, nas-arm, nas-x86_64"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Directory created; path on stdout"
    Write-Host "  1  Unknown platform root (Release/<platform> missing)"
    exit 0
}

if ([string]::IsNullOrWhiteSpace($Platform)) {
    Write-Host "Platform is required. Use: .\scripts\get-release-dir.ps1 -ShowHelp" -ForegroundColor Red
    exit 1
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$ReleaseBase = Join-Path (Join-Path $ProjectRoot "Release") $Platform

# Проверка существования базовой папки платформы
if (-not (Test-Path $ReleaseBase)) {
    Write-Error "Platform directory does not exist: $ReleaseBase"
    Write-Host "Available platforms: android, ios, desktop-x86_64, desktop-arm, sbc-arm, server-x86_64, nas-arm, nas-x86_64"
    exit 1
}

# Получаем текущую дату в формате YYYY-MM-DD
$CurrentDate = Get-Date -Format "yyyy-MM-dd"

# Находим последний номер сборки за сегодня
$LastBuildNum = 0
if (Test-Path $ReleaseBase) {
    $ExistingDirs = Get-ChildItem -Path $ReleaseBase -Directory | Where-Object {
        $_.Name -match "^${CurrentDate}-(\d+)$"
    }

    foreach ($dir in $ExistingDirs) {
        if ($dir.Name -match "^${CurrentDate}-(\d+)$") {
            $BuildNum = [int]$Matches[1]
            if ($BuildNum -gt $LastBuildNum) {
                $LastBuildNum = $BuildNum
            }
        }
    }
}

# Увеличиваем номер сборки
$NewBuildNum = $LastBuildNum + 1

# Форматируем номер сборки с ведущими нулями (001, 002, ...)
$BuildNumFormatted = "{0:D3}" -f $NewBuildNum

# Создаем имя папки
$BuildDirName = "${CurrentDate}-${BuildNumFormatted}"
$BuildDirPath = Join-Path $ReleaseBase $BuildDirName

# Создаем папку
New-Item -ItemType Directory -Force -Path $BuildDirPath | Out-Null

# Выводим путь к созданной папке
Write-Output $BuildDirPath
