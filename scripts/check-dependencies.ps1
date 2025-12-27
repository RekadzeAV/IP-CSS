# Простой скрипт проверки зависимостей
# Без установки, только проверка

Write-Host "========================================"
Write-Host "Проверка компонентов для сборки IP-CSS"
Write-Host "========================================"
Write-Host ""

function Test-Command {
    param($Command)
    $null -ne (Get-Command $Command -ErrorAction SilentlyContinue)
}

# Java
Write-Host "Java:" -ForegroundColor Cyan
if (Test-Command java) {
    $version = java -version 2>&1 | Select-Object -First 1
    Write-Host "  [OK] $version" -ForegroundColor Green
    if (Test-Command javac) {
        $javacVer = javac -version 2>&1
        Write-Host "  [OK] javac доступен: $javacVer" -ForegroundColor Green
    } else {
        Write-Host "  [ERROR] javac не найден" -ForegroundColor Red
    }
} else {
    Write-Host "  [MISSING] Java не установлена" -ForegroundColor Red
}
Write-Host ""

# Gradle Wrapper
Write-Host "Gradle Wrapper:" -ForegroundColor Cyan
if (Test-Path "gradlew.bat") {
    Write-Host "  [OK] gradlew.bat найден" -ForegroundColor Green
} else {
    Write-Host "  [ERROR] gradlew.bat не найден" -ForegroundColor Red
}
Write-Host ""

# Node.js
Write-Host "Node.js:" -ForegroundColor Cyan
if (Test-Command node) {
    $nodeVer = node --version
    Write-Host "  [OK] Node.js: $nodeVer" -ForegroundColor Green
    if (Test-Command npm) {
        $npmVer = npm --version
        Write-Host "  [OK] npm: $npmVer" -ForegroundColor Green
    } else {
        Write-Host "  [ERROR] npm не найден" -ForegroundColor Red
    }
} else {
    Write-Host "  [MISSING] Node.js не установлен" -ForegroundColor Red
}
Write-Host ""

# CMake
Write-Host "CMake:" -ForegroundColor Cyan
if (Test-Command cmake) {
    $cmakeVer = cmake --version | Select-Object -First 1
    Write-Host "  [OK] $cmakeVer" -ForegroundColor Green
} else {
    Write-Host "  [MISSING] CMake не установлен" -ForegroundColor Red
}
Write-Host ""

# FFmpeg
Write-Host "FFmpeg:" -ForegroundColor Cyan
if (Test-Command ffmpeg) {
    $ffmpegVer = ffmpeg -version | Select-Object -First 1
    Write-Host "  [OK] $ffmpegVer" -ForegroundColor Green
} else {
    Write-Host "  [MISSING] FFmpeg не установлен" -ForegroundColor Red
}
Write-Host ""

# C++ Compiler
Write-Host "C++ Compiler:" -ForegroundColor Cyan
$compilerFound = $false
if (Test-Path "C:\Program Files\Microsoft Visual Studio") {
    Write-Host "  [OK] Visual Studio найдена (MSVC)" -ForegroundColor Green
    $compilerFound = $true
}
if (Test-Command g++) {
    $gppVer = g++ --version | Select-Object -First 1
    Write-Host "  [OK] MinGW/GCC: $gppVer" -ForegroundColor Green
    $compilerFound = $true
}
if (-not $compilerFound) {
    Write-Host "  [MISSING] C++ компилятор не найден" -ForegroundColor Red
}
Write-Host ""

# Android SDK
Write-Host "Android SDK:" -ForegroundColor Cyan
if ($env:ANDROID_HOME) {
    Write-Host "  [OK] ANDROID_HOME: $env:ANDROID_HOME" -ForegroundColor Green
    if (Test-Path $env:ANDROID_HOME) {
        Write-Host "  [OK] SDK директория существует" -ForegroundColor Green
    } else {
        Write-Host "  [ERROR] SDK директория не существует" -ForegroundColor Red
    }
} else {
    Write-Host "  [MISSING] ANDROID_HOME не установлена" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================"
Write-Host "Проверка завершена!"
Write-Host "========================================"

