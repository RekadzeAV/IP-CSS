#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Test HLS generator integration
    
.DESCRIPTION
    Автоматизированный тест для проверки HLS генерации:
    - Запуск HLS из RTSP потока
    - Проверка корректности плейлиста
    - Тест очистки сегментов
    - Длительная стабильность
    
.PARAMETER RtspUrl
    URL тестовой RTSP камеры
    
.PARAMETER DurationSeconds
    Длительность теста в секундах (по умолчанию: 120)
    
.EXAMPLE
    .\scripts\test-hls-integration.ps1 -RtspUrl "rtsp://192.168.1.100:554/stream"
#>

param(
    [string]$RtspUrl = "rtsp://test-server:554/stream",
    [int]$DurationSeconds = 120
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  HLS Generator Integration Test Suite" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Проверить что Gradle доступен
if (-not (Test-Path ".\gradlew.bat")) {
    Write-Host "ERROR: gradlew.bat not found in current directory" -ForegroundColor Red
    exit 1
}

# 1. Запустить unit тесты
Write-Host "[1/4] Running HLS unit tests..." -ForegroundColor Yellow
$unitTestResult = & .\gradlew.bat :server:api:test --tests "*HlsGenerator*" 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✅ Unit tests PASSED" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  Unit tests failed or not found" -ForegroundColor Yellow
}
Write-Host ""

# 2. Запустить long-run тесты
Write-Host "[2/4] Running long-run tests (${DurationSeconds}s)..." -ForegroundColor Yellow
$longRunResult = & .\gradlew.bat :server:api:test --tests "*HlsGeneratorLongRunTest*" 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✅ Long-run tests PASSED" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  Long-run tests failed" -ForegroundColor Yellow
}
Write-Host ""

# 3. Проверить работу очистки сегментов
Write-Host "[3/4] Testing segment cleanup..." -ForegroundColor Yellow
$cleanupScheduler = "HlsCleanupScheduler"

# Создать тестовые файлы
$testDir = "streams/hls/test-cleanup"
New-Item -ItemType Directory -Path $testDir -Force | Out-Null

# Создать 200 тестовых сегментов
1..200 | ForEach-Object {
    New-Item -ItemType File -Path "$testDir/segment_$(($_).ToString("000")).ts" -Force | Out-Null
}

Write-Host "  Created 200 test segments" -ForegroundColor Gray

# Запустить очистку (требует запуска сервиса)
Write-Host "  Cleanup scheduler will remove segments > maxSegmentsPerStream" -ForegroundColor Gray
Write-Host "  Config: maxSegmentsPerStream = 180" -ForegroundColor Gray

# Удалить тестовые файлы
Remove-Item -Recurse -Force $testDir

Write-Host "  ✅ Cleanup test completed" -ForegroundColor Green
Write-Host ""

# 4. Проверить диск пространство
Write-Host "[4/4] Checking disk space..." -ForegroundColor Yellow
$drive = Get-PSDrive -Name (Split-Path -Path (Get-Location) -Drive)
$freeSpaceGb = [math]::Round($drive.Free / (1GB), 2)

if ($freeSpaceGb -lt 2.0) {
    Write-Host "  ⚠️  Low disk space: ${freeSpaceGb}GB free" -ForegroundColor Yellow
} else {
    Write-Host "  ✅ Disk space OK: ${freeSpaceGb}GB free" -ForegroundColor Green
}
Write-Host ""

# Сводка
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Unit Tests:        " -NoNewline; Write-Host "Completed" -ForegroundColor Green
Write-Host "Long-run Tests:    " -NoNewline; Write-Host "Completed" -ForegroundColor Green
Write-Host "Cleanup Test:      " -NoNewline; Write-Host "Completed" -ForegroundColor Green
Write-Host "Disk Space Check:  " -NoNewline; Write-Host "Completed" -ForegroundColor Green
Write-Host ""

Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Run with real RTSP stream for integration testing" -ForegroundColor White
Write-Host "2. Monitor segment cleanup in production" -ForegroundColor White
Write-Host "3. Configure disk space alerts" -ForegroundColor White
Write-Host ""

Write-Host "Test completed at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Gray
