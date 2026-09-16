#!/usr/bin/env pwsh
# Полевая валидация RTSP, HLS и Screenshot с локальными камерами

param(
    [string]$ConfigPath = "config/test-cameras-local-network.example.json",
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$RunAllTests,
    [switch]$RunRtspTests,
    [switch]$RunHlsTests,
    [switch]$RunScreenshotTests,
    [switch]$RunDiscovery,
    [string]$DiscoverySubnet = "192.168.1.0/24",
    [int]$HlsDurationSeconds = 120,
    [string]$OutputDir = "diagnostics/field-validation",
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
Полевая валидация с локальными камерами

Usage:
  .\scripts\field-validation.ps1 [-RunAllTests] [-ConfigPath <path>]

Options:
  -ConfigPath           Путь к конфигурации камер (default: config/test-cameras-local-network.example.json)
  -BaseUrl              URL API сервера (default: http://localhost:8080)
  -RunAllTests          Запустить все тесты (RTSP + HLS + Screenshot)
  -RunRtspTests         Запустить только RTSP тесты
  -RunHlsTests          Запустить только HLS тесты
  -RunScreenshotTests   Запустить только Screenshot тесты
  -RunDiscovery         Выполнить обнаружение камер в сети
  -DiscoverySubnet      Подсеть для обнаружения (default: 192.168.1.0/24)
  -HlsDurationSeconds   Длительность HLS теста (default: 120)
  -OutputDir            Директория для отчётов (default: diagnostics/field-validation)
  -ShowHelp             Показать эту справку

Examples:
  # Обнаружение камер
  .\scripts\field-validation.ps1 -RunDiscovery

  # Полный цикл тестирования
  .\scripts\field-validation.ps1 -RunAllTests

  # Только RTSP тесты
  .\scripts\field-validation.ps1 -RunRtspTests

  # С кастомной конфигурацией
  .\scripts\field-validation.ps1 -RunAllTests -ConfigPath config/my-cameras.json

"@
    exit 0
}

# ============================================================================
# Setup
# ============================================================================

$ErrorActionPreference = "Stop"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

# Resolve paths
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$ConfigPath = Join-Path $projectRoot $ConfigPath
$OutputDir = Join-Path $projectRoot $OutputDir

if (!(Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

# ============================================================================
# Helper Functions
# ============================================================================

function Write-Success {
    param([string]$Message)
    Write-Host "  ✓ $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "  ✗ $Message" -ForegroundColor Red
}

function Write-Warn {
    param([string]$Message)
    Write-Host "  ⚠ $Message" -ForegroundColor Yellow
}

function Write-Info {
    param([string]$Message)
    Write-Host "  ℹ $Message" -ForegroundColor Cyan
}

function Write-Separator {
    param([string]$Title = "")
    if ($Title) {
        Write-Host ""
        Write-Host ("=" * 80) -ForegroundColor DarkGray
        Write-Host "  $Title" -ForegroundColor DarkGray
        Write-Host ("=" * 80) -ForegroundColor DarkGray
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host ("-" * 80) -ForegroundColor DarkGray
        Write-Host ""
    }
}

# ============================================================================
# Main Execution
# ============================================================================

Write-Separator "Полевая валидация с локальными камерами"
Write-Host "Timestamp:    $timestamp" -ForegroundColor White
Write-Host "Config:       $ConfigPath" -ForegroundColor White
Write-Host "Base URL:     $BaseUrl" -ForegroundColor White
Write-Host "Output:       $OutputDir" -ForegroundColor White
Write-Host ""

$totalResults = @{
    rtsp = @{ passed = 0; failed = 0; skipped = 0 }
    hls = @{ passed = 0; failed = 0; skipped = 0 }
    screenshot = @{ passed = 0; failed = 0; skipped = 0 }
}

# Шаг 1: Обнаружение камер
if ($RunDiscovery) {
    Write-Separator "Шаг 1: Обнаружение камер"
    
    $discoveryScript = Join-Path $scriptDir "discover-rtsp-cameras.ps1"
    if (Test-Path $discoveryScript) {
        & $discoveryScript -Subnet $DiscoverySubnet -OutputFile (Join-Path $OutputDir "discovered-cameras.json")
    } else {
        Write-Warn "Скрипт обнаружения не найден: $discoveryScript"
    }
    
    Write-Separator
}

# Шаг 2: Проверка конфигурации
if ($RunAllTests -or $RunRtspTests -or $RunHlsTests -or $RunScreenshotTests) {
    if (!(Test-Path $ConfigPath)) {
        Write-Error "Файл конфигурации не найден: $ConfigPath"
        Write-Host ""
        Write-Info "Создайте конфигурацию на основе шаблона:"
        Write-Info "  config/test-cameras-local-network.example.json"
        exit 1
    }
    
    Write-Success "Конфигурация найдена: $ConfigPath"
}

# Шаг 3: RTSP тесты
if ($RunAllTests -or $RunRtspTests) {
    Write-Separator "Шаг 2: RTSP тестирование"
    
    $rtspScript = Join-Path $scriptDir "test-rtsp-real-cameras.ps1"
    if (Test-Path $rtspScript) {
        try {
            $rtspResult = & $rtspScript -ConfigPath $ConfigPath -OutputDir (Join-Path $OutputDir "rtsp-tests")
            if ($LASTEXITCODE -eq 0) {
                $totalResults.rtsp.passed++
            } else {
                $totalResults.rtsp.failed++
            }
        } catch {
            Write-Warn "RTSP тесты завершены с ошибками"
            $totalResults.rtsp.failed++
        }
    } else {
        Write-Warn "Скрипт RTSP тестов не найден"
        $totalResults.rtsp.skipped++
    }
    
    Write-Separator
}

# Шаг 4: HLS тесты
if ($RunAllTests -or $RunHlsTests) {
    Write-Separator "Шаг 3: HLS тестирование"
    
    $hlsScript = Join-Path $scriptDir "hls-runtime-stability-test.ps1"
    if (Test-Path $hlsScript) {
        try {
            & $hlsScript -LongRunDurationSeconds $HlsDurationSeconds -OutputDir (Join-Path $OutputDir "hls-tests")
            if ($LASTEXITCODE -eq 0) {
                $totalResults.hls.passed++
            } else {
                $totalResults.hls.failed++
            }
        } catch {
            Write-Warn "HLS тесты завершены с ошибками"
            $totalResults.hls.failed++
        }
    } else {
        Write-Warn "Скрипт HLS тестов не найден"
        $totalResults.hls.skipped++
    }
    
    Write-Separator
}

# Шаг 5: Screenshot тесты
if ($RunAllTests -or $RunScreenshotTests) {
    Write-Separator "Шаг 4: Screenshot тестирование"
    
    $screenshotScript = Join-Path $scriptDir "screenshot-pipeline-test.ps1"
    if (Test-Path $screenshotScript) {
        try {
            & $screenshotScript -OutputDir (Join-Path $OutputDir "screenshot-tests")
            if ($LASTEXITCODE -eq 0) {
                $totalResults.screenshot.passed++
            } else {
                $totalResults.screenshot.failed++
            }
        } catch {
            Write-Warn "Screenshot тесты завершены с ошибками"
            $totalResults.screenshot.failed++
        }
    } else {
        Write-Warn "Скрипт Screenshot тестов не найден"
        $totalResults.screenshot.skipped++
    }
    
    Write-Separator
}

# ============================================================================
# Final Summary
# ============================================================================

Write-Separator "Итоги полевой валидации"

$totalPassed = $totalResults.rtsp.passed + $totalResults.hls.passed + $totalResults.screenshot.passed
$totalFailed = $totalResults.rtsp.failed + $totalResults.hls.failed + $totalResults.screenshot.failed
$totalSkipped = $totalResults.rtsp.skipped + $totalResults.hls.skipped + $totalResults.screenshot.skipped

Write-Host "RTSP Tests:    " -NoNewline
Write-Host "Passed: $($totalResults.rtsp.passed), Failed: $($totalResults.rtsp.failed), Skipped: $($totalResults.rtsp.skipped)" -ForegroundColor $(if ($totalResults.rtsp.failed -eq 0) { "Green" } else { "Red" })

Write-Host "HLS Tests:     " -NoNewline
Write-Host "Passed: $($totalResults.hls.passed), Failed: $($totalResults.hls.failed), Skipped: $($totalResults.hls.skipped)" -ForegroundColor $(if ($totalResults.hls.failed -eq 0) { "Green" } else { "Red" })

Write-Host "Screenshot:    " -NoNewline
Write-Host "Passed: $($totalResults.screenshot.passed), Failed: $($totalResults.screenshot.failed), Skipped: $($totalResults.screenshot.skipped)" -ForegroundColor $(if ($totalResults.screenshot.failed -eq 0) { "Green" } else { "Red" })

Write-Host ""
Write-Host "Total:         " -NoNewline
Write-Host "Passed: $totalPassed, Failed: $totalFailed, Skipped: $totalSkipped" -ForegroundColor $(if ($totalFailed -eq 0) { "Green" } else { "Yellow" })

Write-Host ""
Write-Host "Отчёты в: $OutputDir" -ForegroundColor Cyan

# Генерация итогового отчёта
$reportPath = Join-Path $OutputDir "field-validation-report-$timestamp.md"
$reportLines = @(
    "# Отчёт полевой валидации",
    "",
    "- **Дата:** $timestamp",
    "- **Конфигурация:** $ConfigPath",
    "- **Base URL:** $BaseUrl",
    "",
    "## Результаты",
    "",
    "| Тесты | Прошло | Не удалось | Пропущено |",
    "|-------|--------|------------|-----------|",
    "| RTSP | $($totalResults.rtsp.passed) | $($totalResults.rtsp.failed) | $($totalResults.rtsp.skipped) |",
    "| HLS | $($totalResults.hls.passed) | $($totalResults.hls.failed) | $($totalResults.hls.skipped) |",
    "| Screenshot | $($totalResults.screenshot.passed) | $($totalResults.screenshot.failed) | $($totalResults.screenshot.skipped) |",
    "| **Всего** | **$totalPassed** | **$totalFailed** | **$totalSkipped** |",
    "",
    "## Отчёты",
    ""
)

$reportLines | Set-Content -Path $reportPath -Encoding UTF8
Write-Success "Итоговый отчёт: $reportPath"

Write-Host ""
if ($totalFailed -gt 0) {
    Write-Warn "Некоторые тесты завершены с ошибками"
    exit 1
} else {
    Write-Success "Все тесты успешно завершены!"
    exit 0
}
