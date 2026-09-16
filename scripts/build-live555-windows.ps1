# Build Live555 for Windows
# PowerShell скрипт для автоматизации сборки Live555 на Windows

param(
    [string]$Architecture = "x64",
    [string]$BuildType = "Release",
    [switch]$Clean,
    [switch]$Help
)

if ($Help) {
    Write-Host "Скрипт сборки Live555 для Windows"
    Write-Host ""
    Write-Host "Использование: .\build-live555-windows.ps1 [параметры]"
    Write-Host ""
    Write-Host "Параметры:"
    Write-Host "  -Architecture  Архитектура: x64, x86, arm64 (по умолчанию: x64)"
    Write-Host "  -BuildType     Тип сборки: Release, Debug (по умолчанию: Release)"
    Write-Host "  -Clean         Очистить перед сборкой"
    Write-Host "  -Help          Показать эту справку"
    exit 0
}

$ErrorActionPreference = "Stop"

# Путь к проекту
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Live555Dir = Join-Path $ProjectRoot "native\live555"
$SourceDir = Join-Path $Live555Dir "src"
$BuildDir = Join-Path $Live555Dir "build\$Architecture\$BuildType"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Live555 Build Script for Windows" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Проверка наличия Git
Write-Host "Проверка Git..." -ForegroundColor Yellow
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: Git not found. Please install Git and add to PATH." -ForegroundColor Red
    exit 1
}
Write-Host "Git found: $(Get-Command git | Select-Object -ExpandProperty Source)" -ForegroundColor Green

# Клонирование репозитория если не существует
if (-not (Test-Path $SourceDir)) {
    Write-Host ""
    Write-Host "Клонирование Live555 репозитория..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $SourceDir -Force | Out-Null
    Set-Location $SourceDir
    
    git clone https://github.com/Live555/live555.git .
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Failed to clone Live555 repository" -ForegroundColor Red
        exit 1
    }
    Write-Host "Live555 cloned successfully" -ForegroundColor Green
} else {
    Write-Host "Live555 source already exists, updating..." -ForegroundColor Yellow
    Set-Location $SourceDir
    git pull
}

# Очистка если требуется
if ($Clean) {
    Write-Host ""
    Write-Host "Очистка предыдущих сборок..." -ForegroundColor Yellow
    if (Test-Path $BuildDir) {
        Remove-Item -Recurse -Force $BuildDir
    }
}

# Создание директории сборки
New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null

# Настройка конфигурации
Write-Host ""
Write-Host "Настройка конфигурации для $Architecture ($BuildType)..." -ForegroundColor Yellow

$configFile = Join-Path $SourceDir "config\MSW"
if (-not (Test-Path $configFile)) {
    Write-Host "ERROR: Config file not found: $configFile" -ForegroundColor Red
    exit 1
}

# Копирование конфигурации
$customConfig = Join-Path $BuildDir "config.MSW"
Copy-Item $configFile $customConfig

# Настройка путей
$compilerPath = "cl.exe"
$archiverPath = "lib.exe"

# Настройка в зависимости от архитектуры
switch ($Architecture) {
    "x64" {
        # Настройка для x64
        $vsInstall = "${env:ProgramFiles}\Microsoft Visual Studio\2022\Community\VC\Tools\MSVC"
        if (-not (Test-Path $vsInstall)) {
            $vsInstall = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC"
        }
        
        if (Test-Path $vsInstall) {
            $msvcVersion = Get-ChildItem $vsInstall | Select-Object -First 1 | Select-Object -ExpandProperty Name
            $compilerPath = Join-Path $vsInstall "$msvcVersion\bin\Hostx64\x64\cl.exe"
            $archiverPath = Join-Path $vsInstall "$msvcVersion\bin\Hostx64\x64\lib.exe"
        }
        
        # Добавить пути в PATH
        $vsTools = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Shared\VC\tools\MSVC"
        if (Test-Path $vsTools) {
            $msvcVersion = Get-ChildItem $vsTools | Select-Object -First 1 | Select-Object -ExpandProperty Name
            $binPath = Join-Path $vsTools "$msvcVersion\bin\Hostx64\x64"
            $includePath = Join-Path $vsTools "$msvcVersion\include"
            $libPath = Join-Path $vsTools "$msvcVersion\lib\x64"
            $env:Path = "$binPath;$env:Path"
            $env:INCLUDE = $includePath
            $env:LIB = $libPath
        }
    }
    "x86" {
        # Настройка для x86
        # Аналогично но для x86
    }
    "arm64" {
        # Настройка для ARM64
        # Требуется Visual Studio 2019+
    }
    default {
        Write-Host "ERROR: Unsupported architecture: $Architecture" -ForegroundColor Red
        Write-Host "Supported: x64, x86, arm64" -ForegroundColor Yellow
        exit 1
    }
}

# Настройка переменной окружения для конфигурации
Set-Location $SourceDir
$env:CFLAGS = "/O2 /W3 /D_CRT_SECURE_NO_WARNINGS"
if ($BuildType -eq "Debug") {
    $env:CFLAGS = "/Od /W3 /D_DEBUG /D_CRT_SECURE_NO_WARNINGS"
}

Write-Host "Compiler: $compilerPath" -ForegroundColor Gray
Write-Host "Archiver: $archiverPath" -ForegroundColor Gray
Write-Host "CFLAGS: $env:CFLAGS" -ForegroundColor Gray

# Сборка
Write-Host ""
Write-Host "Сборка Live555..." -ForegroundColor Yellow
Write-Host "Это может занять несколько минут..." -ForegroundColor Gray

# Запуск генерации конфигурации
& "$SourceDir\generateMSW.bat"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to generate MSW configuration" -ForegroundColor Red
    exit 1
}

# Сборка makefiles (используем nmake)
Write-Host "Запуск nmake..." -ForegroundColor Gray
& nmake /f Makefile.MSC
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Build failed with nmake" -ForegroundColor Red
    exit 1
}

Write-Host "Build completed successfully" -ForegroundColor Green

# Копирование библиотек
Write-Host ""
Write-Host "Копирование библиотек..." -ForegroundColor Yellow

$libOutputDir = Join-Path $BuildDir "lib"
$includeOutputDir = Join-Path $BuildDir "include"
New-Item -ItemType Directory -Path $libOutputDir -Force | Out-Null
New-Item -ItemType Directory -Path $includeOutputDir -Force | Out-Null

# Копировать заголовки
Get-ChildItem -Path $SourceDir -Recurse -Filter "*.h" | Copy-Item -Destination $includeOutputDir -Force

# Копировать библиотеки
Get-ChildItem -Path $SourceDir -Recurse -Filter "*.lib" | Copy-Item -Destination $libOutputDir -Force

# Копировать исполняемые файлы
Get-ChildItem -Path $SourceDir -Recurse -Filter "*.exe" | Copy-Item -Destination $libOutputDir -Force

Write-Host "Libraries copied to: $libOutputDir" -ForegroundColor Green
Write-Host "Headers copied to: $includeOutputDir" -ForegroundColor Green

# Создание .def файла для экспорта символов
Write-Host ""
Write-Host "Создание .def файла..." -ForegroundColor Yellow

$defFile = Join-Path $BuildDir "live555.def"
$defContent = @"
LIBRARY live555
EXPORTS
"@

# Добавить экспортируемые символы (это упрощённый пример)
$symbols = @(
    "BasicUsageEnvironment_versionString",
    "UsageEnvironment_versionString",
    "OutlookRTSPVersionString"
)

foreach ($symbol in $symbols) {
    $defContent += "`r`n    $symbol"
}

Set-Content -Path $defFile -Value $defContent -Encoding ASCII

Write-Host ".def file created: $defFile" -ForegroundColor Green

# Вывод итогов
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Build Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Architecture: $Architecture"
Write-Host "Build Type: $BuildType"
Write-Host "Output Directory: $BuildDir"
Write-Host "Libraries: $libOutputDir"
Write-Host "Headers: $includeOutputDir"
Write-Host "DEF File: $defFile"
Write-Host ""
Write-Host "Build completed successfully!" -ForegroundColor Green
Write-Host ""

Set-Location $ProjectRoot
