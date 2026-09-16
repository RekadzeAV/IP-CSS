# Скрипт установки Android SDK для сборки Android приложения IP-CSS
# Для Windows (PowerShell)

param([switch]$ShowHelp)

if ($ShowHelp) {
    Write-Host "Interactive Android SDK guidance for IP-CSS (ANDROID_HOME, sdkmanager, optional Chocolatey)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\install-android-sdk.ps1 -ShowHelp"
    Write-Host "  .\scripts\install-android-sdk.ps1"
    Write-Host ""
    Write-Host "Exit codes: 0 when ANDROID_HOME exists and sdkmanager path succeeds; otherwise interactive (cancel may still exit 0)."
    exit 0
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Установка Android SDK для IP-CSS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Проверка прав администратора
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

# Проверка наличия Android SDK
if ($env:ANDROID_HOME) {
    Write-Host "[OK] ANDROID_HOME уже установлена: $env:ANDROID_HOME" -ForegroundColor Green
    if (Test-Path $env:ANDROID_HOME) {
        Write-Host "[OK] Android SDK директория существует" -ForegroundColor Green

        # Проверка sdkmanager
        $sdkManager = Join-Path $env:ANDROID_HOME "cmdline-tools\latest\bin\sdkmanager.bat"
        if (-not (Test-Path $sdkManager)) {
            $sdkManager = Join-Path $env:ANDROID_HOME "tools\bin\sdkmanager.bat"
        }

        if (Test-Path $sdkManager) {
            Write-Host "[OK] sdkmanager найден" -ForegroundColor Green
            Write-Host ""
            Write-Host "Установка необходимых компонентов..." -ForegroundColor Yellow
            & $sdkManager "platforms;android-34" "build-tools;34.0.0" "platform-tools" "cmdline-tools;latest"
            Write-Host "[OK] Компоненты Android SDK установлены" -ForegroundColor Green
        } else {
            Write-Host "[ERROR] sdkmanager не найден" -ForegroundColor Red
            Write-Host "  Установите Android SDK Command-line Tools вручную" -ForegroundColor Yellow
        }
        exit 0
    }
}

Write-Host "Android SDK не установлен. Выберите способ установки:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Android Studio (рекомендуется) - полная IDE для разработки Android" -ForegroundColor Cyan
Write-Host "   Скачать: https://developer.android.com/studio" -ForegroundColor Gray
Write-Host "   После установки Android Studio SDK будет установлен автоматически" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Android SDK Command-line Tools (только SDK, без IDE)" -ForegroundColor Cyan
Write-Host "   Скачать: https://developer.android.com/studio#command-tools" -ForegroundColor Gray
Write-Host ""

$choice = Read-Host "Выберите вариант (1 или 2) или нажмите Enter для отмены"

if ($choice -eq "1") {
    Write-Host ""
    Write-Host "Откройте браузер и скачайте Android Studio:" -ForegroundColor Yellow
    Write-Host "https://developer.android.com/studio" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "После установки Android Studio:" -ForegroundColor Yellow
    Write-Host "1. Запустите Android Studio" -ForegroundColor Gray
    Write-Host "2. Перейдите в Settings > Appearance & Behavior > System Settings > Android SDK" -ForegroundColor Gray
    Write-Host "3. Установите Android SDK Platform 34 и Build Tools 34.0.0" -ForegroundColor Gray
    Write-Host "4. Установите переменную окружения ANDROID_HOME:" -ForegroundColor Gray
    Write-Host '   [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk", "User")' -ForegroundColor Cyan
    Write-Host ""

    $openBrowser = Read-Host "Открыть страницу загрузки Android Studio в браузере? (Y/N)"
    if ($openBrowser -eq "Y" -or $openBrowser -eq "y") {
        Start-Process "https://developer.android.com/studio"
    }

} elseif ($choice -eq "2") {
    Write-Host ""
    Write-Host "Установка Android SDK Command-line Tools..." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Инструкции:" -ForegroundColor Cyan
    Write-Host "1. Скачайте Command-line Tools: https://developer.android.com/studio#command-tools" -ForegroundColor Gray
    Write-Host "2. Распакуйте в директорию, например: C:\Android\sdk\cmdline-tools\latest" -ForegroundColor Gray
    Write-Host "3. Установите переменную окружения ANDROID_HOME:" -ForegroundColor Gray
    Write-Host '   [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Android\sdk", "User")' -ForegroundColor Cyan
    Write-Host "4. Добавьте в PATH: $env:ANDROID_HOME\cmdline-tools\latest\bin" -ForegroundColor Gray
    Write-Host "5. Перезапустите терминал и выполните:" -ForegroundColor Gray
    Write-Host '   sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"' -ForegroundColor Cyan
    Write-Host ""

    $openBrowser = Read-Host "Открыть страницу загрузки Command-line Tools в браузере? (Y/N)"
    if ($openBrowser -eq "Y" -or $openBrowser -eq "y") {
        Start-Process "https://developer.android.com/studio#command-tools"
    }

    # Попытка автоматической установки через Chocolatey (если доступен)
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    if (Get-Command choco -ErrorAction SilentlyContinue) {
        Write-Host ""
        $installViaChoco = Read-Host "Установить Android SDK через Chocolatey? (Y/N)"
        if ($installViaChoco -eq "Y" -or $installViaChoco -eq "y") {
            if ($isAdmin) {
                Write-Host "Установка через Chocolatey..." -ForegroundColor Yellow
                choco install android-sdk -y

                # Настройка переменной окружения
                $sdkPath = "C:\ProgramData\Android\android-sdk"
                if (Test-Path $sdkPath) {
                    [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdkPath, "User")
                    $env:ANDROID_HOME = $sdkPath
                    Write-Host "[OK] ANDROID_HOME установлена: $sdkPath" -ForegroundColor Green

                    # Установка компонентов
                    $sdkManager = Join-Path $sdkPath "cmdline-tools\latest\bin\sdkmanager.bat"
                    if (Test-Path $sdkManager) {
                        Write-Host "Установка компонентов..." -ForegroundColor Yellow
                        & $sdkManager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
                    }
                }
            } else {
                Write-Host "[ERROR] Требуются права администратора для установки через Chocolatey" -ForegroundColor Red
            }
        }
    }
} else {
    Write-Host "Установка отменена" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Проверка установки" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($env:ANDROID_HOME) {
    Write-Host "[OK] ANDROID_HOME: $env:ANDROID_HOME" -ForegroundColor Green
    if (Test-Path $env:ANDROID_HOME) {
        Write-Host "[OK] Директория существует" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Директория не существует" -ForegroundColor Red
    }
} else {
    Write-Host "[MISSING] ANDROID_HOME не установлена" -ForegroundColor Red
    Write-Host ""
    Write-Host "После установки Android SDK установите переменную окружения:" -ForegroundColor Yellow
    Write-Host '[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk", "User")' -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Или для Command-line Tools:" -ForegroundColor Yellow
    Write-Host '[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Android\sdk", "User")' -ForegroundColor Cyan
}

Write-Host ""
Write-Host "После установки перезапустите терминал для применения изменений PATH" -ForegroundColor Yellow
Write-Host ""

