# Скрипт установки всех необходимых компонентов для сборки IP-CSS
# Для Windows (PowerShell)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Установка компонентов для сборки IP-CSS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Проверка прав администратора
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "ВНИМАНИЕ: Для установки некоторых компонентов требуются права администратора" -ForegroundColor Yellow
    Write-Host "Рекомендуется запустить скрипт от имени администратора" -ForegroundColor Yellow
    Write-Host ""
}

# Функция проверки наличия команды
function Test-Command {
    param($Command)
    $null -ne (Get-Command $Command -ErrorAction SilentlyContinue)
}

# Функция установки через Chocolatey
function Install-ChocoPackage {
    param($PackageName, $Description)

    Write-Host "Проверка: $Description..." -ForegroundColor Yellow
    if (Test-Command choco) {
        Write-Host "Установка $PackageName через Chocolatey..." -ForegroundColor Green
        choco install $PackageName -y --no-progress
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] $Description установлен успешно" -ForegroundColor Green
            return $true
        } else {
            Write-Host "[ERROR] Ошибка при установке $Description" -ForegroundColor Red
            return $false
        }
    } else {
        Write-Host "[SKIP] Chocolatey не установлен. Пропускаем автоматическую установку $Description" -ForegroundColor Red
        Write-Host "  Рекомендуется установить вручную или установить Chocolatey" -ForegroundColor Yellow
        return $false
    }
}

# Проверка и установка Chocolatey
Write-Host "=== Проверка Chocolatey ===" -ForegroundColor Cyan
if (-not (Test-Command choco)) {
    Write-Host "Chocolatey не установлен. Установка Chocolatey..." -ForegroundColor Yellow
    Write-Host "Требуются права администратора!" -ForegroundColor Yellow

    if ($isAdmin) {
        Set-ExecutionPolicy Bypass -Scope Process -Force
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
        iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

        # Обновить PATH
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

        if (Test-Command choco) {
            Write-Host "[OK] Chocolatey установлен успешно" -ForegroundColor Green
        } else {
            Write-Host "[ERROR] Ошибка при установке Chocolatey" -ForegroundColor Red
            Write-Host "  Пожалуйста, установите Chocolatey вручную: https://chocolatey.org/install" -ForegroundColor Yellow
            exit 1
        }
    } else {
        Write-Host "[ERROR] Требуются права администратора для установки Chocolatey" -ForegroundColor Red
        Write-Host "  Установите Chocolatey вручную: https://chocolatey.org/install" -ForegroundColor Yellow
        Write-Host "  Или запустите этот скрипт от имени администратора" -ForegroundColor Yellow
    }
} else {
    Write-Host "[OK] Chocolatey уже установлен" -ForegroundColor Green
    choco --version
}
Write-Host ""

# Проверка Java
Write-Host "=== Проверка Java ===" -ForegroundColor Cyan
$javaVersion = java -version 2>&1 | Select-String -Pattern "version"
if ($javaVersion) {
    Write-Host "[OK] Java установлена: $javaVersion" -ForegroundColor Green
    $javacVersion = javac -version 2>&1
    if ($javacVersion) {
        Write-Host "[OK] Java Compiler (javac) доступен: $javacVersion" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] javac не найден. Установите JDK (не JRE)" -ForegroundColor Red
    }
} else {
    Write-Host "[ERROR] Java не установлена" -ForegroundColor Red
    if ($isAdmin) {
        Install-ChocoPackage "openjdk17" "OpenJDK 17"
    } else {
        Write-Host "  Установите JDK 17 вручную: https://adoptium.net/" -ForegroundColor Yellow
    }
}
Write-Host ""

# Проверка Gradle (через wrapper)
Write-Host "=== Проверка Gradle Wrapper ===" -ForegroundColor Cyan
if (Test-Path "gradlew.bat") {
    Write-Host "[OK] gradlew.bat найден" -ForegroundColor Green
    Write-Host "  Gradle Wrapper будет автоматически скачан при первой сборке" -ForegroundColor Gray
} else {
    Write-Host "[ERROR] gradlew.bat не найден в текущей директории" -ForegroundColor Red
    Write-Host "  Убедитесь, что вы находитесь в корне проекта" -ForegroundColor Yellow
}
Write-Host ""

# Проверка и установка Node.js
Write-Host "=== Проверка Node.js ===" -ForegroundColor Cyan
if (Test-Command node) {
    $nodeVersion = node --version
    Write-Host "[OK] Node.js установлен: $nodeVersion" -ForegroundColor Green

    # Проверка версии (нужна 20+)
    $nodeVersionNum = [int]($nodeVersion -replace 'v(\d+)\..*', '$1')
    if ($nodeVersionNum -lt 20) {
        Write-Host "[WARN] Версия Node.js должна быть 20.0.0 или выше" -ForegroundColor Yellow
        if ($isAdmin) {
            Write-Host "Обновление Node.js..." -ForegroundColor Yellow
            Install-ChocoPackage "nodejs-lts" "Node.js LTS"
        }
    }

    if (Test-Command npm) {
        $npmVersion = npm --version
        Write-Host "[OK] npm установлен: $npmVersion" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] npm не найден" -ForegroundColor Red
    }
} else {
    Write-Host "[ERROR] Node.js не установлен" -ForegroundColor Red
    if ($isAdmin) {
        Install-ChocoPackage "nodejs-lts" "Node.js LTS"
        # Обновить PATH
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    } else {
        Write-Host "  Установите Node.js 20+ вручную: https://nodejs.org/" -ForegroundColor Yellow
    }
}
Write-Host ""

# Проверка и установка CMake
Write-Host "=== Проверка CMake ===" -ForegroundColor Cyan
if (Test-Command cmake) {
    $cmakeVersion = cmake --version | Select-Object -First 1
    Write-Host "[OK] CMake установлен: $cmakeVersion" -ForegroundColor Green
} else {
    Write-Host "[ERROR] CMake не установлен" -ForegroundColor Red
    if ($isAdmin) {
        Install-ChocoPackage "cmake" "CMake"
        # Обновить PATH
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    } else {
        Write-Host "  Установите CMake вручную: https://cmake.org/download/" -ForegroundColor Yellow
    }
}
Write-Host ""

# Проверка и установка FFmpeg
Write-Host "=== Проверка FFmpeg ===" -ForegroundColor Cyan
if (Test-Command ffmpeg) {
    $ffmpegVersion = ffmpeg -version | Select-Object -First 1
    Write-Host "[OK] FFmpeg установлен: $ffmpegVersion" -ForegroundColor Green
    Write-Host "  Примечание: Для сборки нативных библиотек также нужны dev библиотеки" -ForegroundColor Yellow
    Write-Host "  На Windows рекомендуется использовать vcpkg или установить вручную" -ForegroundColor Yellow
} else {
    Write-Host "[ERROR] FFmpeg не установлен" -ForegroundColor Red
    if ($isAdmin) {
        Install-ChocoPackage "ffmpeg" "FFmpeg"
        # Обновить PATH
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        Write-Host "  ВНИМАНИЕ: Для сборки нативных библиотек нужны dev библиотеки FFmpeg" -ForegroundColor Yellow
        Write-Host "  Рекомендуется использовать vcpkg или скачать dev версию вручную" -ForegroundColor Yellow
    } else {
        Write-Host "  Установите FFmpeg вручную: https://ffmpeg.org/download.html" -ForegroundColor Yellow
        Write-Host "  Для сборки нужны dev библиотеки (не только исполняемый файл)" -ForegroundColor Yellow
    }
}
Write-Host ""

# Проверка C++ компилятора
Write-Host "=== Проверка C++ компилятора ===" -ForegroundColor Cyan
$compilerFound = $false

# Проверка MSVC (Visual Studio)
if (Test-Path "C:\Program Files\Microsoft Visual Studio") {
    Write-Host "[OK] Visual Studio найдена (MSVC компилятор доступен)" -ForegroundColor Green
    $compilerFound = $true
}

# Проверка MinGW
if (Test-Command g++) {
    $gppVersion = g++ --version | Select-Object -First 1
    Write-Host "[OK] MinGW/GCC найден: $gppVersion" -ForegroundColor Green
    $compilerFound = $true
}

if (-not $compilerFound) {
    Write-Host "[ERROR] C++ компилятор не найден" -ForegroundColor Red
    Write-Host "  Рекомендуется установить один из вариантов:" -ForegroundColor Yellow
    Write-Host "  1. Visual Studio 2022 с компонентами C++" -ForegroundColor Yellow
    Write-Host "  2. MinGW-w64 через Chocolatey: choco install mingw" -ForegroundColor Yellow
    if ($isAdmin) {
        $installMingw = Read-Host "Установить MinGW через Chocolatey? (Y/N)"
        if ($installMingw -eq "Y" -or $installMingw -eq "y") {
            Install-ChocoPackage "mingw" "MinGW-w64"
        }
    }
}
Write-Host ""

# Проверка Android SDK
Write-Host "=== Проверка Android SDK ===" -ForegroundColor Cyan
if ($env:ANDROID_HOME) {
    Write-Host "[OK] ANDROID_HOME установлена: $env:ANDROID_HOME" -ForegroundColor Green
    if (Test-Path $env:ANDROID_HOME) {
        Write-Host "[OK] Android SDK директория существует" -ForegroundColor Green

        # Проверка наличия необходимых компонентов
        $sdkManager = Join-Path $env:ANDROID_HOME "cmdline-tools\latest\bin\sdkmanager.bat"
        if (-not (Test-Path $sdkManager)) {
            $sdkManager = Join-Path $env:ANDROID_HOME "tools\bin\sdkmanager.bat"
        }

        if (Test-Path $sdkManager) {
            Write-Host "[OK] sdkmanager найден" -ForegroundColor Green
        } else {
            Write-Host "[WARN] sdkmanager не найден. Установите Android SDK Command-line Tools" -ForegroundColor Yellow
        }
    } else {
        Write-Host "[ERROR] Android SDK директория не существует" -ForegroundColor Red
    }
} else {
    Write-Host "[ERROR] ANDROID_HOME не установлена" -ForegroundColor Red
    Write-Host "  Для сборки Android приложения нужен Android SDK" -ForegroundColor Yellow
    Write-Host "  Установите Android Studio или Android SDK Command-line Tools:" -ForegroundColor Yellow
    Write-Host "  https://developer.android.com/studio#command-tools" -ForegroundColor Yellow
}
Write-Host ""

# Итоговая сводка
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Итоговая сводка" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Перезагрузка переменных окружения
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

Write-Host "Проверка установленных компонентов:" -ForegroundColor Cyan
Write-Host ""

$components = @(
    @{Name="Java"; Cmd="java"; Required=$true},
    @{Name="Java Compiler (javac)"; Cmd="javac"; Required=$true},
    @{Name="Gradle Wrapper"; Cmd="Test-Path gradlew.bat"; Required=$true},
    @{Name="Node.js"; Cmd="node"; Required=$false},
    @{Name="npm"; Cmd="npm"; Required=$false},
    @{Name="CMake"; Cmd="cmake"; Required=$false},
    @{Name="FFmpeg"; Cmd="ffmpeg"; Required=$false},
    @{Name="Android SDK"; Cmd="if (`$env:ANDROID_HOME) { 'OK' } else { 'NOT SET' }"; Required=$false}
)

foreach ($comp in $components) {
    if ($comp.Cmd -like "Test-Path*") {
        $result = Invoke-Expression $comp.Cmd
        $status = if ($result) { "[OK]" } else { "[MISSING]" }
        $color = if ($result) { "Green" } else { "Red" }
    } elseif ($comp.Cmd -like "if*") {
        $result = Invoke-Expression $comp.Cmd
        $status = if ($result -eq "OK") { "[OK]" } else { "[MISSING]" }
        $color = if ($result -eq "OK") { "Green" } else { "Yellow" }
    } else {
        $result = Get-Command $comp.Cmd -ErrorAction SilentlyContinue
        $status = if ($result) { "[OK]" } else { "[MISSING]" }
        $color = if ($result) { "Green" } else { if ($comp.Required) { "Red" } else { "Yellow" } }
    }

    $required = if ($comp.Required) { " [ОБЯЗАТЕЛЬНО]" } else { " [ОПЦИОНАЛЬНО]" }
    Write-Host "$status $($comp.Name)$required" -ForegroundColor $color
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Следующие шаги:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Если были установлены новые компоненты, перезапустите терминал" -ForegroundColor Yellow
Write-Host "2. Для Android SDK установите необходимые компоненты:" -ForegroundColor Yellow
Write-Host "   sdkmanager platforms;android-34 build-tools;34.0.0 platform-tools" -ForegroundColor Gray
Write-Host "3. Для сборки нативных библиотек может потребоваться установка FFmpeg dev библиотек" -ForegroundColor Yellow
Write-Host "4. Проверьте документацию: LOCAL_BUILD_REQUIREMENTS.md" -ForegroundColor Yellow
Write-Host ""

# Проверка прав на запись
Write-Host "Проверка прав на запись в проект..." -ForegroundColor Cyan
$testFile = "test_write_permissions.txt"
try {
    "test" | Out-File -FilePath $testFile -ErrorAction Stop
    Remove-Item $testFile -ErrorAction Stop
    Write-Host "[OK] Права на запись подтверждены" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Нет прав на запись в текущую директорию" -ForegroundColor Red
    Write-Host "  Убедитесь, что у вас есть права на запись в проект" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Установка завершена!" -ForegroundColor Green

