#!/usr/bin/env pwsh
# Pre-flight Check для Полевой Валидации
# Проверяет готовность системы к тестированию

param(
    [string]$ApiBaseUrl = "http://localhost:8080",
    [string]$ConfigPath = "config/test-cameras-local-network.example.json",
    [switch]$StrictMode,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
Pre-flight Check для Полевой Валидации

Usage:
  .\scripts\field-validation-preflight.ps1 [-ApiBaseUrl <url>] [-ConfigPath <path>]

Options:
  -ApiBaseUrl    URL API сервера (default: http://localhost:8080)
  -ConfigPath    Путь к конфигурации камер (default: config/test-cameras-local-network.example.json)
  -StrictMode    Строгий режим (ошибки = отказ)
  -ShowHelp      Показать эту справку

Examples:
  # Базовая проверка
  .\scripts\field-validation-preflight.ps1

  # Строгий режим
  .\scripts\field-validation-preflight.ps1 -StrictMode

"@
    exit 0
}

# ============================================================================
# Setup
# ============================================================================

$ErrorActionPreference = "Continue"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$ConfigPath = Join-Path $projectRoot $ConfigPath

# ============================================================================
# Helper Functions
# ============================================================================

function Write-Success {
    param([string]$Message)
    Write-Host "  ✓ $Message" -ForegroundColor Green
    return $true
}

function Write-Error {
    param([string]$Message)
    Write-Host "  ✗ $Message" -ForegroundColor Red
    return $false
}

function Write-Warn {
    param([string]$Message)
    Write-Host "  ⚠ $Message" -ForegroundColor Yellow
    return $false
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
# Checks
# ============================================================================

Write-Separator "Pre-flight Check для Полевой Валидации"
Write-Host "Timestamp:    $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor White
Write-Host "API URL:      $ApiBaseUrl" -ForegroundColor White
Write-Host "Config:       $ConfigPath" -ForegroundColor White
Write-Host "Strict Mode:  $StrictMode" -ForegroundColor White
Write-Host ""

$results = @{
    passed = 0
    failed = 0
    warnings = 0
}

# ============================================================================
# Check 1: PowerShell Version
# ============================================================================

Write-Info "Проверка PowerShell версии..."
$pwshVersion = $PSVersionTable.PSVersion.Major
if ($pwshVersion -ge 7) {
    Write-Success "PowerShell $pwshVersion (требуется 7+)"
    $results.passed++
} else {
    Write-Error "PowerShell $pwshVersion (требуется 7+)"
    $results.failed++
    if ($StrictMode) { exit 1 }
}

# ============================================================================
# Check 2: Required Scripts Exist
# ============================================================================

Write-Info "Проверка скриптов..."
$requiredScripts = @(
    "scripts/discover-rtsp-cameras.ps1",
    "scripts/field-validation.ps1",
    "scripts/test-rtsp-real-cameras.ps1",
    "scripts/hls-runtime-stability-test.ps1",
    "scripts/screenshot-pipeline-test.ps1"
)

$scriptsOk = $true
foreach ($script in $requiredScripts) {
    $scriptPath = Join-Path $projectRoot $script
    if (Test-Path $scriptPath) {
        Write-Success "Скрипт: $script"
    } else {
        Write-Error "Скрипт не найден: $script"
        $scriptsOk = $false
        $results.failed++
        if ($StrictMode) { exit 1 }
    }
}

if ($scriptsOk) {
    $results.passed++
}

# ============================================================================
# Check 3: Configuration File
# ============================================================================

Write-Info "Проверка конфигурации..."
if (Test-Path $ConfigPath) {
    Write-Success "Конфигурация найдена: $ConfigPath"
    
    try {
        $config = Get-Content $ConfigPath -Raw | ConvertFrom-Json
        
        if ($config.cameras -and $config.cameras.Count -gt 0) {
            Write-Success "Найдено камер: $($config.cameras.Count)"
            $results.passed++
        } else {
            Write-Warn "Камеры не определены в конфигурации"
            $results.warnings++
            if ($StrictMode) { exit 1 }
        }
        
        # Проверка на placeholder пароли
        $placeholderPasswords = ($config.cameras | Where-Object { $_.password -eq "CHANGE_ME" -or [string]::IsNullOrWhiteSpace($_.password) }).Count
        if ($placeholderPasswords -gt 0) {
            Write-Warn "Найдено $placeholderPasswords камер с placeholder паролями"
            $results.warnings++
        } else {
            Write-Success "Все пароли указаны"
        }
        
    } catch {
        Write-Error "Ошибка чтения конфигурации: $_"
        $results.failed++
        if ($StrictMode) { exit 1 }
    }
} else {
    Write-Warn "Файл конфигурации не найден: $ConfigPath"
    Write-Info "Создайте на основе шаблона: config/test-cameras-local-network.example.json"
    $results.warnings++
    if ($StrictMode) { exit 1 }
}

# ============================================================================
# Check 4: API Server Health
# ============================================================================

Write-Info "Проверка API сервера..."
try {
    $response = Invoke-WebRequest -Uri "$ApiBaseUrl/api/v1/health" -TimeoutSec 5 -UseBasicParsing
    
    if ($response.StatusCode -eq 200) {
        Write-Success "API сервер доступен: $ApiBaseUrl"
        $results.passed++
    } else {
        Write-Error "API сервер вернул статус $($response.StatusCode)"
        $results.failed++
        if ($StrictMode) { exit 1 }
    }
} catch {
    Write-Warn "API сервер недоступен: $_"
    Write-Info "Убедитесь, что сервер запущен: ./gradlew :server:api:run"
    $results.warnings++
    if ($StrictMode) { exit 1 }
}

# ============================================================================
# Check 5: FFmpeg Installation
# ============================================================================

Write-Info "Проверка FFmpeg..."
try {
    $ffmpegVersion = ffmpeg -version 2>&1 | Select-Object -First 1
    
    if ($ffmpegVersion -match "ffmpeg version") {
        Write-Success "FFmpeg установлен: $($ffmpegVersion -replace '.*version\s+', '' -replace '\s+', ' ')"
        $results.passed++
    } else {
        Write-Warn "FFmpeg не определён корректно"
        $results.warnings++
    }
} catch {
    Write-Warn "FFmpeg не найден в PATH"
    Write-Info "Установите: choco install ffmpeg"
    $results.warnings++
}

# ============================================================================
# Check 6: Required Directories
# ============================================================================

Write-Info "Проверка директорий..."
$requiredDirs = @(
    "diagnostics",
    "diagnostics/rtsp-tests",
    "diagnostics/hls-tests",
    "diagnostics/screenshot-tests",
    "diagnostics/field-validation"
)

$dirsOk = $true
foreach ($dir in $requiredDirs) {
    $dirPath = Join-Path $projectRoot $dir
    if (!(Test-Path $dirPath)) {
        try {
            New-Item -ItemType Directory -Path $dirPath -Force | Out-Null
            Write-Success "Создана директория: $dir"
        } catch {
            Write-Error "Не удалось создать директорию: $dir"
            $dirsOk = $false
            $results.failed++
            if ($StrictMode) { exit 1 }
        }
    } else {
        Write-Success "Директория существует: $dir"
    }
}

if ($dirsOk) {
    $results.passed++
}

# ============================================================================
# Check 7: Network Connectivity (Optional)
# ============================================================================

Write-Info "Проверка сетевой доступности..."
if ($config -and $config.cameras) {
    $testCamera = $config.cameras[0]
    $cameraIP = ($testCamera.rtspUrl -match '(\d{1,3}\.){3}\d{1,3}') ? $Matches[0] : $null
    
    if ($cameraIP) {
        try {
            $pingResult = Test-Connection -ComputerName $cameraIP -Count 1 -Quiet -ErrorAction Stop
            if ($pingResult) {
                Write-Success "Камера доступна: $cameraIP"
                $results.passed++
            } else {
                Write-Warn "Камера не отвечает на ping: $cameraIP"
                $results.warnings++
            }
        } catch {
            Write-Warn "Не удалось проверить ping: $_"
            $results.warnings++
        }
    }
}

# ============================================================================
# Summary
# ============================================================================

Write-Separator "Результаты Pre-flight Check"

$totalChecks = $results.passed + $results.failed + $results.warnings
$successRate = if ($totalChecks -gt 0) { ($results.passed / $totalChecks) * 100 } else { 0 }

Write-Host "Проверок пройдено: $($results.passed)" -ForegroundColor Green
Write-Host "Проверок не пройдено: $($results.failed)" -ForegroundColor $(if ($results.failed -gt 0) { "Red" } else { "Green" })
Write-Host "Предупреждений: $($results.warnings)" -ForegroundColor Yellow
Write-Host ""
Write-Host "Успешность: $([math]::Round($successRate, 1))%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } elseif ($successRate -ge 50) { "Yellow" } else { "Red" })
Write-Host ""

if ($results.failed -gt 0) {
    Write-Error "Pre-flight check FAILED. Исправьте ошибки перед запуском тестов."
    Write-Host ""
    Write-Info "Запустите с -StrictMode для детального отчёта"
    exit 1
} elseif ($results.warnings -gt 0) {
    Write-Warn "Pre-flight check PASSED с предупреждениями"
    Write-Host ""
    Write-Info "Рекомендуется устранить предупреждения перед запуском"
    exit 0
} else {
    Write-Success "Pre-flight check PASSED. Готово к запуску полевой валидации!"
    Write-Host ""
    Write-Info "Следующий шаг: .\scripts\field-validation.ps1 -RunAllTests"
    exit 0
}
