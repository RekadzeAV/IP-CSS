# Placeholder runner for enabled cameras from repo-root test-cameras-config.json (writes JSON stubs under test-results/).
# Usage: .\scripts\run-real-camera-tests.ps1 [-ConfigFile ...] [-CameraId ...]

param(
    [string]$ConfigFile = "test-cameras-config.json",
    [string]$CameraId = "",
    [int]$Duration = 60,
    [switch]$CollectMetrics = $true,
    [switch]$Help = $false,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($Help -or $ShowHelp) {
    Write-Host "Run stub per-camera JSON results from test-cameras-config.json (see script body / TODO for real runner)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-real-camera-tests.ps1 -ShowHelp"
    Write-Host '  .\scripts\run-real-camera-tests.ps1 [-ConfigFile <path>] [-CameraId <id>] [-Duration <sec>] [-CollectMetrics:$false]'
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -ConfigFile, -c  File under repo root (default test-cameras-config.json)"
    Write-Host "  -CameraId, -i   Single camera id; default all enabled"
    Write-Host "  -Duration, -d    Stub duration field (default 60)"
    Write-Host "  -CollectMetrics Stub metrics flag (default on)"
    Write-Host "  -Help, -h        Show help"
    Write-Host "  -ShowHelp        Same as -Help"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Completed"
    Write-Host "  1  Config missing or no enabled cameras"
    exit 0
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$ConfigPath = Join-Path $ProjectRoot $ConfigFile

if (-not (Test-Path $ConfigPath)) {
    Write-Host "[FAIL] Configuration file not found: $ConfigPath" -ForegroundColor Red
    Write-Host "   Please run: .\scripts\setup-test-cameras-config.ps1" -ForegroundColor Yellow
    exit 1
}

Write-Host "=== Real Camera Tests ===" -ForegroundColor Cyan
Write-Host ""

# Загрузка конфигурации
$config = Get-Content $ConfigPath | ConvertFrom-Json

# Фильтрация камер
$camerasToTest = if ($CameraId) {
    $config.cameras | Where-Object { $_.id -eq $CameraId -and $_.enabled }
} else {
    $config.cameras | Where-Object { $_.enabled }
}

if ($camerasToTest.Count -eq 0) {
    Write-Host "[FAIL] No enabled cameras found" -ForegroundColor Red
    exit 1
}

Write-Host "Found $($camerasToTest.Count) camera(s) to test" -ForegroundColor Green
Write-Host ""

# Создание директории для результатов
$ResultsDir = Join-Path $ProjectRoot "test-results"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$TestResultsDir = Join-Path $ResultsDir "camera-tests_$Timestamp"
New-Item -ItemType Directory -Path $TestResultsDir -Force | Out-Null

foreach ($camera in $camerasToTest) {
    Write-Host "=== Testing Camera: $($camera.name) ($($camera.id)) ===" -ForegroundColor Yellow
    Write-Host "  URL: $($camera.url)" -ForegroundColor Gray
    Write-Host "  Codec: $($camera.codec)" -ForegroundColor Gray
    Write-Host "  Resolution: $($camera.resolution)" -ForegroundColor Gray
    Write-Host ""

    # Создание файла результатов для камеры
    $CameraResultsFile = Join-Path $TestResultsDir "$($camera.id)_results.json"

    # Запуск теста (здесь должен быть вызов тестового класса)
    Write-Host "Starting test..." -ForegroundColor Yellow

    # TODO: Интеграция с тестовым классом
    # Пример структуры результатов:
    $results = @{
        cameraId = $camera.id
        cameraName = $camera.name
        startTime = (Get-Date).ToString("o")
        duration = $Duration
        metrics = @{
            totalFrames = 0
            decodedFrames = 0
            failedFrames = 0
            averageFPS = 0.0
            averageDecodeTime = 0.0
            memoryUsage = @()
            cpuUsage = @()
        }
        errors = @()
    }

    # Сохранение результатов
    $results | ConvertTo-Json -Depth 10 | Set-Content -Path $CameraResultsFile

    Write-Host "[OK] Test completed for $($camera.name)" -ForegroundColor Green
    Write-Host "   Results saved to: $CameraResultsFile" -ForegroundColor Gray
    Write-Host ""
}

Write-Host "=== All Tests Complete ===" -ForegroundColor Green
Write-Host "Results directory: $TestResultsDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "To analyze results:" -ForegroundColor Yellow
Write-Host "  .\scripts\analyze-camera-test-results.ps1 -ResultsDir $TestResultsDir" -ForegroundColor White
