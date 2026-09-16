# RTSP Long-Run Stability Test Runner
# Запускает тест стабильности RTSP клиента на указанную длительность

param(
    [string]$RtspUrl = "",
    [int]$DurationMinutes = 60,
    [switch]$FastMode,  # 10 минут вместо 60
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$gradlew = Join-Path $ProjectRoot "gradlew.bat"

if ($ShowHelp) {
    Write-Host "RTSP Long-Run Stability Test Runner" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-rtsp-long-run-stability-test.ps1"
    Write-Host "  .\scripts\run-rtsp-long-run-stability-test.ps1 -RtspUrl 'rtsp://...'"
    Write-Host "  .\scripts\run-rtsp-long-run-stability-test.ps1 -DurationMinutes 120"
    Write-Host "  .\scripts\run-rtsp-long-run-stability-test.ps1 -FastMode"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -RtspUrl           RTSP URL тестовой камеры (обязательно для production)"
    Write-Host "  -DurationMinutes   Длительность теста в минутах (default: 60)"
    Write-Host "  -FastMode          Короткий тест 10 минут для быстрой проверки"
    Write-Host ""
    Write-Host "Примеры:"
    Write-Host "  # Быстрый тест 10 минут"
    Write-Host "  .\scripts\run-rtsp-long-run-stability-test.ps1 -FastMode"
    Write-Host ""
    Write-Host "  # Полный тест 24 часа с реальной камерой"
    Write-Host "  .\scripts\run-rtsp-long-run-stability-test.ps1 -RtspUrl 'rtsp://user:pass@192.168.1.100:554/stream' -DurationMinutes 1440"
    exit 0
}

# Проверка gradlew
if (-not (Test-Path $gradlew)) {
    Write-Host "❌ gradlew.bat не найден: $gradlew" -ForegroundColor Red
    exit 1
}

# Настройка длительности
if ($FastMode) {
    $DurationMinutes = 10
    Write-Host "⚠️  FAST MODE: Тест будет длиться только $DurationMinutes минут" -ForegroundColor Yellow
}

# Если RTSP URL не передан, используем заглушку (только для тестирования структуры)
if ([string]::IsNullOrWhiteSpace($RtspUrl)) {
    Write-Host "⚠️  RTSP URL не указан, запуск в режиме заглушки (без реального подключения)" -ForegroundColor Yellow
    Write-Host "   Для реального теста укажите -RtspUrl 'rtsp://...'" -ForegroundColor Yellow
    $RtspUrl = "rtsp://placeholder:placeholder@placeholder:554/placeholder"
}

$TestId = (Get-Date -Format "yyyyMMdd-HHmmss")
$OutputDir = Join-Path $ProjectRoot "docs\reports\rtsp-stability-tests"

Write-Host "==> RTSP Long-Run Stability Test" -ForegroundColor Cyan
Write-Host "  Test ID: $TestId"
Write-Host "  Duration: $DurationMinutes minutes"
Write-Host "  RTSP URL: $RtspUrl"
Write-Host "  Output: $OutputDir"
Write-Host ""

# Создаем директорию для отчетов
if (!(Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

# Запуск теста через Gradle
Write-Host "==> Запуск теста через Gradle..." -ForegroundColor Cyan

# Передаем параметры в тест
$env:RTSP_TEST_URL = $RtspUrl
$env:RTSP_TEST_DURATION_MINUTES = $DurationMinutes
$env:RTSP_TEST_OUTPUT_DIR = $OutputDir

& $gradlew ":shared:desktopTest --tests '*RtspLongRunStabilityTest*' --info" --no-daemon

$exitCode = $LASTEXITCODE

# Очищаем environment переменные
Remove-Item "env:RTSP_TEST_URL" -ErrorAction SilentlyContinue
Remove-Item "env:RTSP_TEST_DURATION_MINUTES" -ErrorAction SilentlyContinue
Remove-Item "env:RTSP_TEST_OUTPUT_DIR" -ErrorAction SilentlyContinue

if ($exitCode -eq 0) {
    Write-Host ""
    Write-Host "✅ Тест успешно завершен!" -ForegroundColor Green
    Write-Host "Отчеты сохранены в: $OutputDir" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "❌ Тест завершился с ошибками (exit code: $exitCode)" -ForegroundColor Red
    Write-Host "Проверьте логи выше для деталей" -ForegroundColor Yellow
}

exit $exitCode
