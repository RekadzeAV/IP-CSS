# Скрипт для установки FFmpeg dev библиотек на Windows
# Использование: .\scripts\install-ffmpeg-dev.ps1

param([switch]$ShowHelp)

if ($ShowHelp) {
    Write-Host "Download FFmpeg shared dev build to C:\ffmpeg and set user FFMPEG_DIR (Windows)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\install-ffmpeg-dev.ps1 -ShowHelp"
    Write-Host "  .\scripts\install-ffmpeg-dev.ps1"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Success"
    Write-Host "  1  Download/extract failed"
    exit 0
}

$ErrorActionPreference = "Stop"

Write-Host "📦 Installing FFmpeg development libraries for Windows" -ForegroundColor Cyan

# Проверка существующего FFmpeg
$ffmpegPath = Get-Command ffmpeg.exe -ErrorAction SilentlyContinue
if ($ffmpegPath) {
    Write-Host "✅ FFmpeg executable found: $($ffmpegPath.Source)" -ForegroundColor Green
}

# Пути для установки FFmpeg dev
$ffmpegDir = "C:\ffmpeg"
$ffmpegUrl = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl-shared.zip"
$ffmpegDevUrl = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl-shared.zip"

Write-Host ""
Write-Host "📥 Downloading FFmpeg dev libraries..." -ForegroundColor Yellow
Write-Host "   URL: $ffmpegDevUrl" -ForegroundColor Gray

# Создаем временную директорию
$tempDir = Join-Path $env:TEMP "ffmpeg-install"
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null

try {
    # Скачиваем FFmpeg
    $zipFile = Join-Path $tempDir "ffmpeg-dev.zip"

    Write-Host "   Downloading..." -ForegroundColor Gray
    Invoke-WebRequest -Uri $ffmpegDevUrl -OutFile $zipFile -UseBasicParsing

    Write-Host "   Extracting..." -ForegroundColor Gray
    Expand-Archive -Path $zipFile -DestinationPath $tempDir -Force

    # Находим распакованную директорию
    $extractedDir = Get-ChildItem -Path $tempDir -Directory | Where-Object { $_.Name -like "ffmpeg*" } | Select-Object -First 1

    if ($extractedDir) {
        Write-Host "   Copying to $ffmpegDir..." -ForegroundColor Gray

        # Создаем целевую директорию
        New-Item -ItemType Directory -Force -Path $ffmpegDir | Out-Null

        # Копируем файлы
        Copy-Item -Path "$($extractedDir.FullName)\*" -Destination $ffmpegDir -Recurse -Force

        Write-Host "✅ FFmpeg dev libraries installed to: $ffmpegDir" -ForegroundColor Green

        # Проверяем наличие заголовочных файлов
        if (Test-Path "$ffmpegDir\include\libavformat\avformat.h") {
            Write-Host "✅ Dev headers found" -ForegroundColor Green

            # Устанавливаем переменную окружения
            [Environment]::SetEnvironmentVariable("FFMPEG_DIR", $ffmpegDir, "User")
            $env:FFMPEG_DIR = $ffmpegDir

            Write-Host ""
            Write-Host "📝 Environment variable FFMPEG_DIR set to: $ffmpegDir" -ForegroundColor Cyan
            Write-Host "   Restart your terminal or run: `$env:FFMPEG_DIR = `"$ffmpegDir`"" -ForegroundColor Gray
        } else {
            Write-Host "⚠️  Dev headers not found in expected location" -ForegroundColor Yellow
            Write-Host "   Please check: $ffmpegDir\include\libavformat\" -ForegroundColor Gray
        }
    } else {
        Write-Host "❌ Could not find extracted FFmpeg directory" -ForegroundColor Red
    }

} catch {
    Write-Host "❌ Error installing FFmpeg dev libraries: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "📝 Manual installation:" -ForegroundColor Yellow
    Write-Host "   1. Download FFmpeg from: https://github.com/BtbN/FFmpeg-Builds/releases" -ForegroundColor Gray
    Write-Host "   2. Extract to C:\ffmpeg" -ForegroundColor Gray
    Write-Host "   3. Set environment variable: `$env:FFMPEG_DIR = 'C:\ffmpeg'" -ForegroundColor Gray
    exit 1
} finally {
    # Очистка временных файлов
    if (Test-Path $tempDir) {
        Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ""
Write-Host "✨ FFmpeg dev installation completed!" -ForegroundColor Green
