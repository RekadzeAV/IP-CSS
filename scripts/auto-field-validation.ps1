#!/usr/bin/env pwsh
# Автоматизированный запуск полевой валидации
# Выполняет все этапы последовательно с автоматической проверкой результатов

param(
    [string]$Subnet = "192.168.1.0/24",
    [string]$ConfigPath = "config/test-cameras-local-network.example.json",
    [string]$ApiBaseUrl = "http://localhost:8080",
    [int]$HlsDurationSeconds = 120,
    [switch]$QuickMode,
    [switch]$SkipDiscovery,
    [switch]$SkipRtsp,
    [switch]$SkipHls,
    [switch]$SkipScreenshot,
    [switch]$GenerateReportOnly,
    [string]$OutputDir = "diagnostics/field-validation-automated",
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
Автоматизированная Полевая Валидация

Usage:
  .\scripts\auto-field-validation.ps1 [-QuickMode] [-Subnet <subnet>]

Options:
  -Subnet              Подсеть для обнаружения (default: 192.168.1.0/24)
  -ConfigPath          Конфигурация камер (default: config/test-cameras-local-network.example.json)
  -ApiBaseUrl          URL API сервера (default: http://localhost:8080)
  -HlsDurationSeconds  Длительность HLS теста (default: 120)
  -QuickMode           Быстрый режим (30s для всех тестов)
  -SkipDiscovery       Пропустить обнаружение камер
  -SkipRtsp            Пропустить RTSP тесты
  -SkipHls             Пропустить HLS тесты
  -SkipScreenshot      Пропустить Screenshot тесты
  -GenerateReportOnly  Только генерация отчёта (без запуска тестов)
  -OutputDir           Директория для результатов (default: diagnostics/field-validation-automated)
  -ShowHelp            Показать эту справку

Examples:
  # Полный автоматический запуск
  .\scripts\auto-field-validation.ps1

  # Быстрый режим
  .\scripts\auto-field-validation.ps1 -QuickMode

  # Только RTSP тесты
  .\scripts\auto-field-validation.ps1 -SkipHls -SkipScreenshot

"@
    exit 0
}

# ============================================================================
# Setup
# ============================================================================

$ErrorActionPreference = "Stop"
$startTime = Get-Date
$timestamp = $startTime.ToString("yyyyMMdd-HHmmss")

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

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host ("=" * 80) -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host ("=" * 80) -ForegroundColor Cyan
    Write-Host ""
}

function Write-Step {
    param([string]$Message)
    Write-Host "  → $Message" -ForegroundColor Yellow
}

function Write-Success {
    param([string]$Message)
    Write-Host "  ✓ $Message" -ForegroundColor Green
}

function Write-Failure {
    param([string]$Message)
    Write-Host "  ✗ $Message" -ForegroundColor Red
}

function Write-Warn {
    param([string]$Message)
    Write-Host "  ⚠ $Message" -ForegroundColor Yellow
}

function Test-ScriptExists {
    param([string]$ScriptName)
    $scriptPath = Join-Path $scriptDir "$ScriptName.ps1"
    return (Test-Path $scriptPath)
}

function Invoke-ValidationStep {
    param(
        [string]$StepName,
        [string]$ScriptName,
        [array]$Args,
        [switch]$IgnoreFailure
    )
    
    Write-Step "$StepName..."
    
    $scriptPath = Join-Path $scriptDir "$ScriptName.ps1"
    
    if (!(Test-Path $scriptPath)) {
        Write-Failure "Скрипт не найден: $ScriptName.ps1"
        return $false
    }
    
    try {
        $process = Start-Process powershell -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "`"$scriptPath`"", @Args -Wait -PassThru -NoNewWindow
        $process.WaitForExit()
        
        if ($process.ExitCode -eq 0) {
            Write-Success "$StepName завершена успешно"
            return $true
        } else {
            if ($IgnoreFailure) {
                Write-Warn "$StepName завершена с кодом $($process.ExitCode) (продолжаем)"
                return $true
            } else {
                Write-Failure "$StepName завершена с кодом $($process.ExitCode)"
                return $false
            }
        }
    } catch {
        if ($IgnoreFailure) {
            Write-Warn "$StepName завершилась с ошибкой: $_ (продолжаем)"
            return $true
        } else {
            Write-Failure "$StepName завершилась с ошибкой: $_"
            return $false
        }
    }
}

# ============================================================================
# Main Execution
# ============================================================================

Write-Section "Автоматизированная Полевая Валидация"

Write-Host "Время начала:     $($startTime.ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor White
Write-Host "Подсеть:          $Subnet" -ForegroundColor White
Write-Host "Конфигурация:     $ConfigPath" -ForegroundColor White
Write-Host "API URL:          $ApiBaseUrl" -ForegroundColor White
Write-Host "Режим:            $(if ($QuickMode) { 'QUICK' } else { 'FULL' })" -ForegroundColor White
Write-Host "Output Dir:       $OutputDir" -ForegroundColor White
Write-Host ""

# Результаты
$results = @{
    discovery = @{ status = "NOT_RUN"; camerasFound = 0 }
    rtsp = @{ status = "NOT_RUN"; passed = 0; failed = 0 }
    hls = @{ status = "NOT_RUN"; passed = 0; failed = 0 }
    screenshot = @{ status = "NOT_RUN"; passed = 0; failed = 0 }
    totalDuration = 0
    errors = @()
}

$totalDuration = [System.Diagnostics.Stopwatch]::StartNew()

# ============================================================================
# Phase 1: Pre-flight Check
# ============================================================================

Write-Section "Phase 1: Pre-flight Check"

if (Test-ScriptExists "field-validation-preflight") {
    $preflightArgs = @("-ApiBaseUrl", $ApiBaseUrl, "-ConfigPath", $ConfigPath)
    if (Invoke-ValidationStep -StepName "Pre-flight check" -ScriptName "field-validation-preflight" -Args $preflightArgs) {
        Write-Success "Pre-flight check пройден"
    } else {
        Write-Failure "Pre-flight check не пройден. Проверьте требования."
        $results.errors += "Pre-flight check failed"
    }
} else {
    Write-Warn "Скрипт pre-flight не найден, пропускаем"
}

# ============================================================================
# Phase 2: Camera Discovery
# ============================================================================

Write-Section "Phase 2: Camera Discovery"

if ($SkipDiscovery) {
    Write-Warn "Пропуск обнаружения камер (SkipDiscovery)"
    $results.discovery.status = "SKIPPED"
} else {
    if (Test-ScriptExists "discover-rtsp-cameras") {
        $discoveryArgs = @("-Subnet", $Subnet, "-OutputFile", (Join-Path $OutputDir "discovered-cameras.json"))
        
        if (Invoke-ValidationStep -StepName "Обнаружение камер" -ScriptName "discover-rtsp-cameras" -Args $discoveryArgs) {
            $results.discovery.status = "PASSED"
            
            # Подсчитать найденные камеры
            $discoveredFile = Join-Path $OutputDir "discovered-cameras.json"
            if (Test-Path $discoveredFile) {
                $discoveredData = Get-Content $discoveredFile -Raw | ConvertFrom-Json
                $results.discovery.camerasFound = $discoveredData.camerasFound
                Write-Host "  Найдено камер: $($results.discovery.camerasFound)" -ForegroundColor Green
            }
        } else {
            $results.discovery.status = "FAILED"
            $results.errors += "Camera discovery failed"
        }
    } else {
        Write-Failure "Скрипт обнаружения не найден"
        $results.discovery.status = "FAILED"
        $results.errors += "Discovery script not found"
    }
}

# ============================================================================
# Phase 3: RTSP Tests
# ============================================================================

Write-Section "Phase 3: RTSP Testing"

if ($SkipRtsp) {
    Write-Warn "Пропуск RTSP тестов (SkipRtsp)"
    $results.rtsp.status = "SKIPPED"
} else {
    if (!(Test-Path $ConfigPath)) {
        Write-Warn "Файл конфигурации не найден: $ConfigPath"
        Write-Info "Создайте на основе: config/test-cameras-local-network.example.json"
        $results.rtsp.status = "FAILED"
        $results.errors += "Config file not found"
    } else {
        if (Test-ScriptExists "test-rtsp-real-cameras") {
            $rtspArgs = @("-ConfigPath", $ConfigPath, "-OutputDir", (Join-Path $OutputDir "rtsp-tests"))
            
            if ($QuickMode) {
                $rtspArgs += "-ConnectionTestOnly"
            } else {
                $rtspArgs += "-FullTest"
            }
            
            if (Invoke-ValidationStep -StepName "RTSP тестирование" -ScriptName "test-rtsp-real-cameras" -Args $rtspArgs) {
                $results.rtsp.status = "PASSED"
            } else {
                $results.rtsp.status = "FAILED"
                $results.errors += "RTSP tests failed"
            }
        } else {
            Write-Failure "Скрипт RTSP тестов не найден"
            $results.rtsp.status = "FAILED"
            $results.errors += "RTSP test script not found"
        }
    }
}

# ============================================================================
# Phase 4: HLS Tests
# ============================================================================

Write-Section "Phase 4: HLS Testing"

if ($SkipHls) {
    Write-Warn "Пропуск HLS тестов (SkipHls)"
    $results.hls.status = "SKIPPED"
} else {
    if (Test-ScriptExists "hls-runtime-stability-test") {
        $hlsArgs = @("-OutputDir", (Join-Path $OutputDir "hls-tests"))
        
        if ($QuickMode) {
            $hlsArgs += "-SkipCleanupTest"
            $hlsArgs += "-SkipReconnectTest"
            $hlsArgs += "-LongRunDurationSeconds"
            $hlsArgs += "30"
        } else {
            $hlsArgs += "-FullTest"
            $hlsArgs += "-LongRunDurationSeconds"
            $hlsArgs += $HlsDurationSeconds
        }
        
        if (Invoke-ValidationStep -StepName "HLS тестирование" -ScriptName "hls-runtime-stability-test" -Args $hlsArgs) {
            $results.hls.status = "PASSED"
        } else {
            $results.hls.status = "FAILED"
            $results.errors += "HLS tests failed"
        }
    } else {
        Write-Failure "Скрипт HLS тестов не найден"
        $results.hls.status = "FAILED"
        $results.errors += "HLS test script not found"
    }
}

# ============================================================================
# Phase 5: Screenshot Tests
# ============================================================================

Write-Section "Phase 5: Screenshot Testing"

if ($SkipScreenshot) {
    Write-Warn "Пропуск Screenshot тестов (SkipScreenshot)"
    $results.screenshot.status = "SKIPPED"
} else {
    if (Test-ScriptExists "screenshot-pipeline-test") {
        $screenshotArgs = @("-OutputDir", (Join-Path $OutputDir "screenshot-tests"))
        
        if (Invoke-ValidationStep -StepName "Screenshot тестирование" -ScriptName "screenshot-pipeline-test" -Args $screenshotArgs) {
            $results.screenshot.status = "PASSED"
        } else {
            $results.screenshot.status = "FAILED"
            $results.errors += "Screenshot tests failed"
        }
    } else {
        Write-Failure "Скрипт Screenshot тестов не найден"
        $results.screenshot.status = "FAILED"
        $results.errors += "Screenshot test script not found"
    }
}

# ============================================================================
# Phase 6: Aggregate Results
# ============================================================================

Write-Section "Phase 6: Aggregate Results"

if (Test-ScriptExists "aggregate-test-results") {
    $aggregateArgs = @("-BaseDir", $OutputDir, "-OutputDir", (Join-Path $OutputDir "aggregated"))
    
    Invoke-ValidationStep -StepName "Агрегация результатов" -ScriptName "aggregate-test-results" -Args $aggregateArgs -IgnoreFailure
} else {
    Write-Warn "Скрипт агрегации не найден, пропускаем"
}

# ============================================================================
# Generate Final Report
# ============================================================================

$totalDuration.Stop()

Write-Section "Итоговый Отчёт"

# Markdown отчёт
$reportPath = Join-Path $OutputDir "field-validation-report-$timestamp.md"

$mdLines = @(
    "# Отчёт Автоматизированной Полевой Валидации",
    "",
    "- **Дата начала:** $($startTime.ToString('yyyy-MM-dd HH:mm:ss'))",
    "- **Дата окончания:** $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
    "- **Длительность:** $([math]::Round($totalDuration.Elapsed.TotalMinutes, 1)) минут",
    "- **Подсеть:** $Subnet",
    "- **Конфигурация:** $ConfigPath",
    "- **API URL:** $ApiBaseUrl",
    "- **Режим:** $(if ($QuickMode) { 'QUICK' } else { 'FULL' })",
    "",
    "## Сводка",
    "",
    "| Этап | Статус | Детали |",
    "|------|--------|--------|",
    "| Discovery | **$($results.discovery.status)** | $($results.discovery.camerasFound) камер найдено |",
    "| RTSP | **$($results.rtsp.status)** | $($results.rtsp.passed) passed, $($results.rtsp.failed) failed |",
    "| HLS | **$($results.hls.status)** | $($results.hls.passed) passed, $($results.hls.failed) failed |",
    "| Screenshot | **$($results.screenshot.status)** | $($results.screenshot.passed) passed, $($results.screenshot.failed) failed |",
    "",
    "## Детали по Этапам",
    ""
)

# Discovery
$mdLines += "### Discovery"
$mdLines += ""
$mdLines += "- Статус: $($results.discovery.status)"
$mdLines += "- Камер найдено: $($results.discovery.camerasFound)"
$mdLines += ""

# RTSP
$mdLines += "### RTSP Tests"
$mdLines += ""
$mdLines += "- Статус: $($results.rtsp.status)"
$mdLines += "- Прошедшие: $($results.rtsp.passed)"
$mdLines += "- Неудачные: $($results.rtsp.failed)"
$mdLines += ""

# HLS
$mdLines += "### HLS Tests"
$mdLines += ""
$mdLines += "- Статус: $($results.hls.status)"
$mdLines += "- Прошедшие: $($results.hls.passed)"
$mdLines += "- Неудачные: $($results.hls.failed)"
$mdLines += ""

# Screenshot
$mdLines += "### Screenshot Tests"
$mdLines += ""
$mdLines += "- Статус: $($results.screenshot.status)"
$mdLines += "- Прошедшие: $($results.screenshot.passed)"
$mdLines += "- Неудачные: $($results.screenshot.failed)"
$mdLines += ""

# Errors
if ($results.errors.Count -gt 0) {
    $mdLines += "## Ошибки"
    $mdLines += ""
    foreach ($error in $results.errors) {
        $mdLines += "- $error"
    }
    $mdLines += ""
}

# Next Steps
$mdLines += "## Следующие Шаги"
$mdLines += ""
if ($results.errors.Count -eq 0) {
    $mdLines += "✅ **Все тесты пройдены успешно!**"
    $mdLines += ""
    $mdLines += "1. Проверьте детальные отчёты в `$OutputDir"
    $mdLines += "2. Рассмотрите запуск long-run тестов (24h)"
    $mdLines += "3. Перейдите к PostgreSQL cutover"
} else {
    $mdLines += "❌ **Некоторые тесты не пройдены.**"
    $mdLines += ""
    $mdLines += "1. Исправьте ошибки согласно отчётам"
    $mdLines += "2. Перезапустите тесты"
    $mdLines += "3. Убедитесь, что все критические проблемы решены"
}
$mdLines += ""

$mdContent = $mdLines -join "`n"
Set-Content -Path $reportPath -Value $mdContent -Encoding UTF8

Write-Success "Отчёт сохранён: $reportPath"

# ============================================================================
# Final Summary
# ============================================================================

Write-Section "Финальная Сводка"

$totalPassed = ($results.discovery.status -eq "PASSED") + 
               ($results.rtsp.status -eq "PASSED") + 
               ($results.hls.status -eq "PASSED") + 
               ($results.screenshot.status -eq "PASSED")
$totalPhases = 4

$successRate = [math]::Round(($totalPassed / $totalPhases) * 100, 1)

Write-Host "Этапов пройдено: $totalPassed/$totalPhases ($successRate%)" -ForegroundColor $(if ($successRate -ge 75) { "Green" } elseif ($successRate -ge 50) { "Yellow" } else { "Red" })
Write-Host "Длительность:     $([math]::Round($totalDuration.Elapsed.TotalMinutes, 1)) минут" -ForegroundColor White
Write-Host "Ошибок:           $($results.errors.Count)" -ForegroundColor $(if ($results.errors.Count -gt 0) { "Red" } else { "Green" })
Write-Host ""

Write-Host "Результаты:" -ForegroundColor Cyan
Write-Host "  Discovery:  $($results.discovery.status) ($($results.discovery.camerasFound) камер)" -ForegroundColor $(if ($results.discovery.status -eq "PASSED") { "Green" } elseif ($results.discovery.status -eq "SKIPPED") { "Yellow" } else { "Red" })
Write-Host "  RTSP:       $($results.rtsp.status)" -ForegroundColor $(if ($results.rtsp.status -eq "PASSED") { "Green" } elseif ($results.rtsp.status -eq "SKIPPED") { "Yellow" } else { "Red" })
Write-Host "  HLS:        $($results.hls.status)" -ForegroundColor $(if ($results.hls.status -eq "PASSED") { "Green" } elseif ($results.hls.status -eq "SKIPPED") { "Yellow" } else { "Red" })
Write-Host "  Screenshot: $($results.screenshot.status)" -ForegroundColor $(if ($results.screenshot.status -eq "PASSED") { "Green" } elseif ($results.screenshot.status -eq "SKIPPED") { "Yellow" } else { "Red" })
Write-Host ""

Write-Host "Отчёты:" -ForegroundColor Cyan
Write-Host "  Итоговый:    $reportPath" -ForegroundColor Gray
Write-Host "  Discovery:   $(Join-Path $OutputDir 'discovered-cameras.json')" -ForegroundColor Gray
Write-Host "  RTSP:        $(Join-Path $OutputDir 'rtsp-tests/')" -ForegroundColor Gray
Write-Host "  HLS:         $(Join-Path $OutputDir 'hls-tests/')" -ForegroundColor Gray
Write-Host "  Screenshot:  $(Join-Path $OutputDir 'screenshot-tests/')" -ForegroundColor Gray
Write-Host "  Агрегировано: $(Join-Path $OutputDir 'aggregated/')" -ForegroundColor Gray
Write-Host ""

if ($results.errors.Count -gt 0) {
    Write-Failure "Полевая валидация завершена с ошибками"
    exit 1
} else {
    Write-Success "Полевая валидация завершена успешно!"
    exit 0
}
