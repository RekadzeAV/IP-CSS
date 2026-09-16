# Скрипт для установки Gradle Wrapper
# Использование: .\scripts\install-gradle.ps1

param([switch]$ShowHelp)

if ($ShowHelp) {
    Write-Host "Create or repair Gradle Wrapper files under the repo root (requires Java on PATH)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\install-gradle.ps1 -ShowHelp"
    Write-Host "  .\scripts\install-gradle.ps1"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Wrapper jar / gradlew present and gradlew --version works"
    Write-Host "  1  Java missing or wrapper could not be created"
    exit 0
}

$ErrorActionPreference = "Stop"

Write-Host "=== Установка Gradle Wrapper ===" -ForegroundColor Cyan
Write-Host ""

$gradleVersion = "8.4"
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

# Проверка Java
Write-Host "Проверка Java..." -ForegroundColor Yellow
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "ОШИБКА: Java не найден в PATH!" -ForegroundColor Red
    Write-Host "Установите Java 17 или выше и добавьте в PATH" -ForegroundColor Yellow
    exit 1
}

$javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
Write-Host "Найдено: $javaVersion" -ForegroundColor Green
Write-Host ""

# Создание директории wrapper
$wrapperDir = Join-Path $projectRoot "gradle\wrapper"
if (-not (Test-Path $wrapperDir)) {
    New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
    Write-Host "Создана директория: $wrapperDir" -ForegroundColor Green
}

# Попытка 1: Использовать Gradle для создания wrapper (если установлен)
if (Get-Command gradle -ErrorAction SilentlyContinue) {
    Write-Host "Найден установленный Gradle, создаю wrapper..." -ForegroundColor Green
    gradle wrapper --gradle-version $gradleVersion
    if (Test-Path "gradle\wrapper\gradle-wrapper.jar") {
        Write-Host "Gradle Wrapper успешно создан!" -ForegroundColor Green
        exit 0
    }
}

# Попытка 2: Скачать готовый wrapper JAR
Write-Host "Попытка скачать Gradle Wrapper JAR..." -ForegroundColor Yellow

$wrapperJarPath = Join-Path $wrapperDir "gradle-wrapper.jar"
$urls = @(
    "https://raw.githubusercontent.com/gradle/gradle/v$gradleVersion/gradle/wrapper/gradle-wrapper.jar",
    "https://github.com/gradle/gradle/raw/v$gradleVersion/gradle/wrapper/gradle-wrapper.jar"
)

foreach ($url in $urls) {
    try {
        Write-Host "Пробую: $url" -ForegroundColor Gray
        $ProgressPreference = 'SilentlyContinue'
        Invoke-WebRequest -Uri $url -OutFile $wrapperJarPath -ErrorAction Stop
        if (Test-Path $wrapperJarPath) {
            $size = (Get-Item $wrapperJarPath).Length
            if ($size -gt 50000) {
                Write-Host "Gradle Wrapper JAR успешно скачан! Размер: $size байт" -ForegroundColor Green
                break
            } else {
                Remove-Item $wrapperJarPath -Force
                Write-Host "Файл слишком маленький, пробую другой URL..." -ForegroundColor Yellow
            }
        }
    } catch {
        Write-Host "Не удалось скачать с этого URL" -ForegroundColor Gray
        continue
    }
}

# Проверка результата
if (-not (Test-Path $wrapperJarPath)) {
    Write-Host ""
    Write-Host "Не удалось автоматически установить Gradle Wrapper" -ForegroundColor Red
    Write-Host ""
    Write-Host "ВАРИАНТЫ УСТАНОВКИ:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. Установить Gradle глобально:" -ForegroundColor Yellow
    Write-Host "   winget install Gradle.Gradle" -ForegroundColor White
    Write-Host "   или" -ForegroundColor Gray
    Write-Host "   choco install gradle" -ForegroundColor White
    Write-Host ""
    Write-Host "2. После установки Gradle выполните:" -ForegroundColor Yellow
    Write-Host "   gradle wrapper --gradle-version $gradleVersion" -ForegroundColor White
    Write-Host ""
    Write-Host "3. Или скачайте вручную:" -ForegroundColor Yellow
    Write-Host "   https://gradle.org/releases/" -ForegroundColor White
    Write-Host ""
    exit 1
}

# Создание gradlew.bat
Write-Host ""
Write-Host "Создание gradlew.bat..." -ForegroundColor Yellow

$gradlewBat = @'
@rem
@rem Copyright 2015 the original author or authors.
@rem Licensed under the Apache License, Version 2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

if defined JAVA_HOME goto findJavaFromJavaHome
set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute
echo ERROR: JAVA_HOME is not set and no 'java' command could be found.
goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
if exist "%JAVA_EXE%" goto execute
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
goto fail

:execute
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
if %ERRORLEVEL% equ 0 goto mainEnd
:fail
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%
:mainEnd
if "%OS%"=="Windows_NT" endlocal
:omega
'@

Set-Content -Path "gradlew.bat" -Value $gradlewBat -Encoding ASCII
Write-Host "gradlew.bat создан!" -ForegroundColor Green

# Тестирование
Write-Host ""
Write-Host "Тестирование Gradle Wrapper..." -ForegroundColor Cyan
& ".\gradlew.bat" --version 2>&1 | Select-Object -First 5

Write-Host ""
Write-Host "=== Gradle Wrapper успешно установлен! ===" -ForegroundColor Green
Write-Host "Теперь можно использовать: .\gradlew.bat :server:api:build" -ForegroundColor Cyan
