# RTSP Long-Run Stability Test

**Назначение:** Проведение длительных тестов стабильности RTSP клиента (24+ часа)  
**Цель:** Выявить утечки памяти, деградацию производительности, проблемы с reconnect

---

## Предварительные требования

1. **Доступ к RTSP камере** (реальной или mock серверу)
   - RTSP URL: `rtsp://username:password@host:port/path`
   - Поддерживаемые кодеки: H.264, H.265, MJPEG

2. **Настроенный проект**
   - Gradle сборка работает
   - Desktop tests проходят

3. **Конфигурация камеры**
   - Создать тестовую камеру в БД или использовать mock

---

## Скрипт тестирования

```powershell
# scripts/rtsp-long-run-stability-test.ps1

param(
    [string]$RtspUrl = "rtsp://demo:demo@192.168.1.100:554/stream",
    [int]$DurationMinutes = 1440,  # 24 часа по умолчанию
    [string]$OutputDir = "docs/reports/rtsp-stability-tests",
    [switch]$FastMode,  # Для тестирования: 5-10 минут вместо 24 часов
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "RTSP Long-Run Stability Test" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\rtsp-long-run-stability-test.ps1"
    Write-Host "  .\scripts\rtsp-long-run-stability-test.ps1 -RtspUrl 'rtsp://...'"
    Write-Host "  .\scripts\rtsp-long-run-stability-test.ps1 -DurationMinutes 60"
    Write-Host "  .\scripts\rtsp-long-run-stability-test.ps1 -FastMode"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -RtspUrl         RTSP URL тестовой камеры"
    Write-Host "  -DurationMinutes Длительность теста в минутах (default: 1440 = 24ч)"
    Write-Host "  -OutputDir       Директория для отчетов"
    Write-Host "  -FastMode        Короткий тест для проверки (5-10 минут)"
    exit 0
}

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $ScriptDir)

# Настройка длительности
if ($FastMode) {
    $DurationMinutes = 10
    Write-Host "⚠️  FAST MODE: Тест будет длиться только $DurationMinutes минут" -ForegroundColor Yellow
}

$OutputDir = Join-Path $ProjectRoot $OutputDir
if (!(Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$TestId = (Get-Date -Format "yyyyMMdd-HHmmss")
$ReportFile = Join-Path $OutputDir "rtsp-stability-$TestId.md"
$LogFile = Join-Path $OutputDir "rtsp-stability-$TestId.log"

# Headers для отчета
$reportContent = @"
# RTSP Long-Run Stability Test Report

**Test ID:** $TestId  
**Date:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")  
**RTSP URL:** $RtspUrl  
**Duration:** $DurationMinutes minutes  
**Fast Mode:** $($FastMode.IsPresent)  

---

## Test Configuration

- **Project Root:** $ProjectRoot
- **Output Directory:** $OutputDir
- **Log File:** $LogFile

---

## Test Execution

"@

# Запуск теста
Write-Host "==> RTSP Long-Run Stability Test" -ForegroundColor Cyan
Write-Host "  Test ID: $TestId"
Write-Host "  Duration: $DurationMinutes minutes"
Write-Host "  RTSP URL: $RtspUrl"
Write-Host "  Log File: $LogFile"
Write-Host ""

# TODO: Реализовать тестовый runner для RTSP long-run
# Это будет Kotlin desktop application который:
# 1. Подключается к RTSP камере
# 2. Запускает поток видео
# 3. Мониторит memory usage, FPS, errors
# 4. Логгирует все события
# 5. Генерирует отчет

Write-Host "⚠️  Тестовый runner ещё не реализован" -ForegroundColor Yellow
Write-Host "Необходимо создать: shared/src/desktopTest/kotlin/.../RtspLongRunStabilityTest.kt"

# Placeholder для будущей реализации
$reportContent += @"
### TODO: Реализация тестового runner'а

Необходимо создать Kotlin desktop тест который:

1. Инициализирует RtspClient
2. Подключается к RTSP потоку
3. Запускает мониторинг:
   - Memory usage (периодически)
   - FPS (кадры в секунду)
   - Количество reconnections
   - Ошибки подключения/декодирования
4. Продолжает работу в течение $DurationMinutes минут
5. Генерирует финальный отчет

**Файл для реализации:**
\``shared/src/desktopTest/kotlin/com/company/ipcamera/shared/test/RtspLongRunStabilityTest.kt\``

"@

# Сохранение отчета
$reportContent | Out-File -FilePath $ReportFile -Encoding UTF8

Write-Host ""
Write-Host "✅ Отчет сохранён: $ReportFile" -ForegroundColor Green
Write-Host "⚠️  Требуется реализация тестового runner'а" -ForegroundColor Yellow

exit 0
```
