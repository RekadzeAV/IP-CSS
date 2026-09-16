#!/usr/bin/env pwsh
# Агрегация всех результатов тестов полевой валидации

param(
    [string]$BaseDir = "diagnostics",
    [string]$OutputDir = "diagnostics/aggregated",
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
Агрегация результатов тестов полевой валидации

Usage:
  .\scripts\aggregate-test-results.ps1 [-BaseDir <path>] [-OutputDir <path>]

Options:
  -BaseDir    Базовая директория с результатами (default: diagnostics)
  -OutputDir  Директория для агрегированного отчёта (default: diagnostics/aggregated)
  -ShowHelp   Показать эту справку

Examples:
  # Базовая агрегация
  .\scripts\aggregate-test-results.ps1

  # С кастомными путями
  .\scripts\aggregate-test-results.ps1 -BaseDir diagnostics -OutputDir reports/final

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
$BaseDir = Join-Path $projectRoot $BaseDir
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

function Get-LatestFile {
    param(
        [string]$Pattern,
        [string]$Directory
    )
    
    $files = Get-ChildItem -Path $Directory -Filter $Pattern -Recurse -File | 
             Where-Object { $_.Name -match "\d{8}-\d{6}" } |
             Sort-Object LastWriteTime -Descending |
             Select-Object -First 1
    
    return $files
}

function Parse-JsonReport {
    param(
        [string]$FilePath
    )
    
    try {
        $json = Get-Content -Path $FilePath -Raw | ConvertFrom-Json
        return $json
    } catch {
        return $null
    }
}

# ============================================================================
# Collect Results
# ============================================================================

Write-Separator "Агрегация результатов тестов"
Write-Host "Base Dir:     $BaseDir" -ForegroundColor White
Write-Host "Output Dir:   $OutputDir" -ForegroundColor White
Write-Host "Timestamp:    $timestamp" -ForegroundColor White
Write-Host ""

$aggregatedResults = @{
    timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    summary = @{
        totalTests = 0
        passedTests = 0
        failedTests = 0
        skippedTests = 0
    }
    tests = @{}
    metrics = @{}
    recommendations = @()
}

# 1. RTSP Тесты
Write-Info "Поиск RTSP тестов..."
$rtspReport = Get-LatestFile -Pattern "rtsp-test-*.json" -Directory $BaseDir

if ($rtspReport) {
    Write-Success "Найдены RTSP тесты: $($rtspReport.FullName)"
    $rtspData = Parse-JsonReport -FilePath $rtspReport.FullName
    
    if ($rtspData) {
        $aggregatedResults.tests.rtsp = @{
            file = $rtspReport.Name
            timestamp = $rtspData.timestamp
            summary = $rtspData.summary
        }
        
        $aggregatedResults.summary.totalTests += $rtspData.summary.total
        $aggregatedResults.summary.passedTests += $rtspData.summary.passed
        $aggregatedResults.summary.failedTests += $rtspData.summary.failed
        $aggregatedResults.summary.skippedTests += $rtspData.summary.skipped
    }
} else {
    Write-Warn "RTSP тесты не найдены"
    $aggregatedResults.tests.rtsp = @{ status = "NOT_FOUND" }
}

# 2. HLS Тесты
Write-Info "Поиск HLS тестов..."
$hlsReport = Get-LatestFile -Pattern "hls-runtime-stability-test-*.json" -Directory $BaseDir

if ($hlsReport) {
    Write-Success "Найдены HLS тесты: $($hlsReport.FullName)"
    $hlsData = Parse-JsonReport -FilePath $hlsReport.FullName
    
    if ($hlsData) {
        $aggregatedResults.tests.hls = @{
            file = $hlsReport.Name
            timestamp = $hlsData.timestamp
            summary = $hlsData.summary
        }
        
        $aggregatedResults.summary.totalTests += $hlsData.summary.total
        $aggregatedResults.summary.passedTests += $hlsData.summary.passed
        $aggregatedResults.summary.failedTests += $hlsData.summary.failed
        $aggregatedResults.summary.skippedTests += $hlsData.summary.skipped
    }
} else {
    Write-Warn "HLS тесты не найдены"
    $aggregatedResults.tests.hls = @{ status = "NOT_FOUND" }
}

# 3. Screenshot Тесты
Write-Info "Поиск Screenshot тестов..."
$screenshotReport = Get-LatestFile -Pattern "screenshot-pipeline-test-*.json" -Directory $BaseDir

if ($screenshotReport) {
    Write-Success "Найдены Screenshot тесты: $($screenshotReport.FullName)"
    $screenshotData = Parse-JsonReport -FilePath $screenshotReport.FullName
    
    if ($screenshotData) {
        $aggregatedResults.tests.screenshot = @{
            file = $screenshotReport.Name
            timestamp = $screenshotData.timestamp
            summary = $screenshotData.summary
        }
        
        $aggregatedResults.summary.totalTests += $screenshotData.summary.total
        $aggregatedResults.summary.passedTests += $screenshotData.summary.passed
        $aggregatedResults.summary.failedTests += $screenshotData.summary.failed
        $aggregatedResults.summary.skippedTests += $screenshotData.summary.skipped
    }
} else {
    Write-Warn "Screenshot тесты не найдены"
    $aggregatedResults.tests.screenshot = @{ status = "NOT_FOUND" }
}

# 4. Обнаружение камер
Write-Info "Поиск обнаруженных камер..."
$discoveryReport = Get-LatestFile -Pattern "discovered-cameras.json" -Directory $BaseDir

if ($discoveryReport) {
    Write-Success "Найдено обнаружение камер: $($discoveryReport.FullName)"
    $discoveryData = Parse-JsonReport -FilePath $discoveryReport.FullName
    
    if ($discoveryData) {
        $aggregatedResults.metrics.camerasFound = $discoveryData.camerasFound
        $aggregatedResults.metrics.subnet = $discoveryData.subnet
    }
}

# ============================================================================
# Generate Recommendations
# ============================================================================

Write-Info "Генерация рекомендаций..."

if ($aggregatedResults.summary.failedTests -gt 0) {
    $aggregatedResults.recommendations += "🔴 Исправить $([int]$aggregatedResults.summary.failedTests) неудачных тестов перед релизом"
}

if ($aggregatedResults.summary.skippedTests -gt 0) {
    $aggregatedResults.recommendations += "🟡 Проверить $([int]$aggregatedResults.summary.skippedTests) пропущенных тестов"
}

if ($aggregatedResults.summary.passedTests -eq $aggregatedResults.summary.totalTests -and $aggregatedResults.summary.totalTests -gt 0) {
    $aggregatedResults.recommendations += "✅ Все тесты пройдены успешно. Готово к полевой валидации."
}

if ($aggregatedResults.metrics.camerasFound -eq 0) {
    $aggregatedResults.recommendations += "⚠️ Камеры не обнаружены. Проверьте сетевую конфигурацию."
}

# ============================================================================
# Generate Reports
# ============================================================================

Write-Separator "Генерация отчётов"

# 1. Markdown отчёт
$reportPath = Join-Path $OutputDir "aggregated-test-results-$timestamp.md"

$mdLines = @(
    "# Агрегированный Отчёт Тестирования",
    "",
    "- **Дата:** $($aggregatedResults.timestamp)",
    "",
    "## Сводка",
    "",
    "| Метрика | Значение |",
    "|---------|----------|",
    "| Всего тестов | $($aggregatedResults.summary.totalTests) |",
    "| Прошло успешно | $($aggregatedResults.summary.passedTests) |",
    "| Не удалось | $($aggregatedResults.summary.failedTests) |",
    "| Пропущено | $($aggregatedResults.summary.skippedTests) |",
    "| Успешность | $(if ($aggregatedResults.summary.totalTests -gt 0) { '{0:P1}' -f ($aggregatedResults.summary.passedTests / $aggregatedResults.summary.totalTests) } else { 'N/A' }) |",
    "",
    "## Результаты по Тестам",
    ""
)

foreach ($testName in $aggregatedResults.tests.Keys) {
    $testData = $aggregatedResults.tests[$testName]
    $mdLines += "### $testName"
    $mdLines += ""
    
    if ($testData.status -eq "NOT_FOUND") {
        $mdLines += "- **Статус:** Не найдено"
    } else {
        $mdLines += "- **Файл:** $($testData.file)"
        $mdLines += "- **Время:** $($testData.timestamp)"
        $mdLines += "- **Всего:** $($testData.summary.total)"
        $mdLines += "- **Прошло:** $($testData.summary.passed)"
        $mdLines += "- **Не удалось:** $($testData.summary.failed)"
        $mdLines += "- **Пропущено:** $($testData.summary.skipped)"
    }
    
    $mdLines += ""
}

if ($aggregatedResults.metrics.Keys.Count -gt 0) {
    $mdLines += "## Метрики"
    $mdLines += ""
    foreach ($metric in $aggregatedResults.metrics.Keys) {
        $mdLines += "- **$metric:** $($aggregatedResults.metrics[$metric])"
    }
    $mdLines += ""
}

$mdLines += "## Рекомендации"
$mdLines += ""
foreach ($rec in $aggregatedResults.recommendations) {
    $mdLines += "- $rec"
}
$mdLines += ""

$mdContent = $mdLines -join "`n"
Set-Content -Path $reportPath -Value $mdContent -Encoding UTF8
Write-Success "Markdown отчёт: $reportPath"

# 2. JSON отчёт
$reportJsonPath = Join-Path $OutputDir "aggregated-test-results-$timestamp.json"
$aggregatedResults | ConvertTo-Json -Depth 10 | Set-Content -Path $reportJsonPath -Encoding UTF8
Write-Success "JSON отчёт: $reportJsonPath"

# 3. Краткая сводка для CI/CD
$ciReportPath = Join-Path $OutputDir "ci-summary-$timestamp.txt"
$ciLines = @(
    "TEST_SUMMARY",
    "total=$($aggregatedResults.summary.totalTests)",
    "passed=$($aggregatedResults.summary.passedTests)",
    "failed=$($aggregatedResults.summary.failedTests)",
    "skipped=$($aggregatedResults.summary.skippedTests)",
    "success_rate=$(if ($aggregatedResults.summary.totalTests -gt 0) { $aggregatedResults.summary.passedTests / $aggregatedResults.summary.totalTests } else { 0 })"
)
$ciLines | Set-Content -Path $ciReportPath -Encoding UTF8
Write-Success "CI/CD сводка: $ciReportPath"

# ============================================================================
# Final Summary
# ============================================================================

Write-Separator "Итоговая Сводка"
Write-Host "Всего тестов:  $($aggregatedResults.summary.totalTests)" -ForegroundColor White
Write-Host "Прошло:        $($aggregatedResults.summary.passedTests)" -ForegroundColor Green
Write-Host "Не удалось:    $($aggregatedResults.summary.failedTests)" -ForegroundColor $(if ($aggregatedResults.summary.failedTests -gt 0) { "Red" } else { "Green" })
Write-Host "Пропущено:     $($aggregatedResults.summary.skippedTests)" -ForegroundColor Yellow
Write-Host ""

if ($aggregatedResults.recommendations.Count -gt 0) {
    Write-Host "Рекомендации:" -ForegroundColor Cyan
    foreach ($rec in $aggregatedResults.recommendations) {
        Write-Host "  $rec" -ForegroundColor Gray
    }
    Write-Host ""
}

Write-Host "Отчёты:" -ForegroundColor Cyan
Write-Host "  Markdown: $reportPath" -ForegroundColor Gray
Write-Host "  JSON:     $reportJsonPath" -ForegroundColor Gray
Write-Host "  CI/CD:    $ciReportPath" -ForegroundColor Gray
Write-Host ""

if ($aggregatedResults.summary.failedTests -gt 0) {
    Write-Error "Некоторые тесты завершены с ошибками!"
    exit 1
} else {
    Write-Success "Агрегация завершена успешно!"
    exit 0
}
