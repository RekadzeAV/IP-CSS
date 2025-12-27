# Скрипт установки FFmpeg для Windows
# Поддерживает: Windows 10/11, Windows Server

param(
    [switch]$UseChocolatey,
    [switch]$UseScoop,
    [switch]$Manual,
    [string]$InstallPath = "C:\ffmpeg"
)

$ErrorActionPreference = "Stop"

# Цвета для вывода
function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# Проверка наличия FFmpeg
function Test-FFmpeg {
    try {
        $version = & ffmpeg -version 2>&1 | Select-Object -First 1
        if ($version -match "ffmpeg version") {
            Write-Info "FFmpeg уже установлен: $version"

            # Проверка кодеков
            $codecs = & ffmpeg -codecs 2>&1
            if ($codecs -match "aac|libmp3lame|pcm") {
                Write-Info "Необходимые кодеки доступны"
                return $true
            } else {
                Write-Warn "FFmpeg установлен, но некоторые кодеки отсутствуют"
                return $false
            }
        }
    } catch {
        return $false
    }
    return $false
}

# Установка через Chocolatey
function Install-FFmpeg-Chocolatey {
    Write-Info "Установка FFmpeg через Chocolatey..."

    if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
        Write-Error "Chocolatey не установлен. Установите: https://chocolatey.org/install"
        return $false
    }

    choco install ffmpeg -y

    # Обновление PATH
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")

    Write-Info "FFmpeg успешно установлен через Chocolatey"
    return $true
}

# Установка через Scoop
function Install-FFmpeg-Scoop {
    Write-Info "Установка FFmpeg через Scoop..."

    if (-not (Get-Command scoop -ErrorAction SilentlyContinue)) {
        Write-Error "Scoop не установлен. Установите: https://scoop.sh"
        return $false
    }

    scoop install ffmpeg

    Write-Info "FFmpeg успешно установлен через Scoop"
    return $true
}

# Ручная установка
function Install-FFmpeg-Manual {
    Write-Info "Ручная установка FFmpeg..."

    $downloadUrl = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"
    $zipPath = "$env:TEMP\ffmpeg.zip"
    $extractPath = "$env:TEMP\ffmpeg-extract"

    Write-Info "Скачивание FFmpeg..."
    try {
        Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath -UseBasicParsing
    } catch {
        Write-Error "Ошибка при скачивании: $_"
        return $false
    }

    Write-Info "Распаковка..."
    if (Test-Path $extractPath) {
        Remove-Item $extractPath -Recurse -Force
    }
    Expand-Archive -Path $zipPath -DestinationPath $extractPath -Force

    # Находим папку с ffmpeg
    $ffmpegFolder = Get-ChildItem $extractPath -Directory | Where-Object { $_.Name -like "ffmpeg-*" } | Select-Object -First 1

    if (-not $ffmpegFolder) {
        Write-Error "Не найдена папка с FFmpeg"
        return $false
    }

    Write-Info "Копирование в $InstallPath..."
    if (Test-Path $InstallPath) {
        Remove-Item $InstallPath -Recurse -Force
    }
    Copy-Item $ffmpegFolder.FullName -Destination $InstallPath -Recurse

    # Добавление в PATH
    $binPath = Join-Path $InstallPath "bin"
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")

    if ($currentPath -notlike "*$binPath*") {
        [Environment]::SetEnvironmentVariable("Path", "$currentPath;$binPath", "User")
        $env:Path += ";$binPath"
        Write-Info "FFmpeg добавлен в PATH"
    }

    # Очистка
    Remove-Item $zipPath -Force
    Remove-Item $extractPath -Recurse -Force

    Write-Info "FFmpeg успешно установлен в $InstallPath"
    return $true
}

# Основная функция
function Main {
    Write-Info "Проверка установки FFmpeg..."

    if (Test-FFmpeg) {
        Write-Info "FFmpeg уже установлен и готов к использованию"
        return
    }

    $success = $false

    if ($UseChocolatey) {
        $success = Install-FFmpeg-Chocolatey
    } elseif ($UseScoop) {
        $success = Install-FFmpeg-Scoop
    } elseif ($Manual) {
        $success = Install-FFmpeg-Manual
    } else {
        # Автоматический выбор метода
        if (Get-Command choco -ErrorAction SilentlyContinue) {
            Write-Info "Обнаружен Chocolatey, используем его..."
            $success = Install-FFmpeg-Chocolatey
        } elseif (Get-Command scoop -ErrorAction SilentlyContinue) {
            Write-Info "Обнаружен Scoop, используем его..."
            $success = Install-FFmpeg-Scoop
        } else {
            Write-Info "Менеджеры пакетов не найдены, используем ручную установку..."
            $success = Install-FFmpeg-Manual
        }
    }

    if (-not $success) {
        Write-Error "Ошибка при установке FFmpeg"
        exit 1
    }

    # Финальная проверка
    Start-Sleep -Seconds 2
    if (Test-FFmpeg) {
        Write-Info "✓ FFmpeg успешно установлен и готов к использованию"

        Write-Info "Доступные аудио кодеки:"
        & ffmpeg -codecs 2>&1 | Select-String -Pattern "DEA.*(aac|mp3|pcm|g711)"

        Write-Info "Доступные видео кодеки:"
        & ffmpeg -codecs 2>&1 | Select-String -Pattern "DEV.*(h264|h265|hevc)"
    } else {
        Write-Error "FFmpeg установлен, но не найден в PATH. Перезапустите терминал."
    }
}

# Запуск
Main

