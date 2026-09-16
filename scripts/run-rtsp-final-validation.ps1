#!/usr/bin/env pwsh
# RTSP Final Validation Suite
# Комплексное тестирование RTSP клиента перед выпуском

param(
    [string]$Profile = "smoke",  # smoke, short, long, full
    [string]$ConfigFile = "config/test-cameras.rtsp.json",
    [string]$OutputDir = "docs/reports/rtsp-validation",
    [switch]$SkipEmulators,
    [switch]$SkipRealCameras,
    [switch]$Verbose,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "RTSP Final Validation Suite" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-rtsp-final-validation.ps1 -Profile smoke"
    Write-Host "  .\scripts\run-rtsp-final-validation.ps1 -Profile long -Verbose"
    Write-Host ""
    Write-Host "Profiles:"
    Write-Host "  smoke   - Быстрый тест (10 мин, 3 эмулятора)" -ForegroundColor Green
    Write-Host "  short   - Короткий тест (2 часа, 3 камеры)" -ForegroundColor Green
    Write-Host "  long    - Долгосрочный тест (24 часа, 2 камеры)" -ForegroundColor Green
    Write-Host "  full    - Полное тестирование (48 часов, 7 камер)" -ForegroundColor Green
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Profile        Профиль тестирования (default: smoke)"
    Write-Host "  -ConfigFile     Файл конфигурации камер"
    Write-Host "  -OutputDir      Директория для отчётов"
    Write-Host "  -SkipEmulators  Пропустить эмуляторы"
    Write-Host "  -SkipRealCameras Пропустить реальные камеры"
    Write-Host "  -Verbose        Подробный вывод"
    exit 0
}

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RTSP Final Validation Suite" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Profile: $Profile" -ForegroundColor Green
Write-Host "Config: $ConfigFile" -ForegroundColor Green
Write-Host "Output: $OutputDir" -ForegroundColor Green
Write-Host "Start Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

# Проверка конфигурации
if (-not (Test-Path $ConfigFile)) {
    Write-Host "ERROR: Config file not found: $ConfigFile" -ForegroundColor Red
    exit 1
}

# Чтение конфигурации
$config = Get-Content $ConfigFile | ConvertFrom-Json
$testProfile = $config.testProfiles.$Profile

if (-not $testProfile) {
    Write-Host "ERROR: Unknown profile: $Profile" -ForegroundColor Red
    Write-Host "Available profiles: smoke, short, long, full" -ForegroundColor Yellow
    exit 1
}

# Создаем директорию для отчётов
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

# Профили тестов
$profiles = @{
    smoke = @{
        DurationMinutes = 10
        Description = "Быстрый smoke тест"
    }
    short = @{
        DurationMinutes = 120
        Description = "Короткий тест стабильности"
    }
    long = @{
        DurationMinutes = 1440
        Description = "Долгосрочный тест (24 часа)"
    }
    full = @{
        DurationMinutes = 2880
        Description = "Полное тестирование (48 часов)"
    }
}

$profile = $profiles[$Profile]

Write-Host ""
Write-Host "Test Profile Details:" -ForegroundColor Cyan
Write-Host "  Description: $($profile.Description)" -ForegroundColor Green
Write-Host "  Duration: $($profile.DurationMinutes) minutes" -ForegroundColor Green
Write-Host "  Cameras: $($testProfile.cameras.Count)" -ForegroundColor Green
Write-Host ""

# Метрики теста
$testResults = @{
    Profile = $Profile
    StartTime = (Get-Date).ToString('o')
    DurationMinutes = $profile.DurationMinutes
    TotalCameras = 0
    SuccessfulCameras = 0
    FailedCameras = 0
    CameraResults = @()
    OverallStatus = "Running"
}

# Функция логирования
function Write-ValidationLog {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logEntry = "[$timestamp] [$Level] $Message"
    Write-Host $logEntry
    Add-Content -Path "$OutputDir/validation.log" -Value $logEntry
}

# Функция тестирования одной камеры
function Test-RTSPCamera {
    param(
        [string]$CameraId,
        [string]$CameraName,
        [string]$RtspUrl,
        [int]$DurationMinutes
    )
    
    Write-ValidationLog "Testing camera: $CameraName ($RtspUrl)"
    
    $result = @{
        CameraId = $CameraId
        CameraName = $CameraName
        RtspUrl = $RtspUrl
        StartTime = (Get-Date).ToString('o')
        Status = "Running"
        ConnectionAttempts = 0
        SuccessfulConnections = 0
        FailedConnections = 0
        FramesReceived = 0
        Errors = @()
        MemoryUsage = @()
    }
    
    try {
        # Запуск теста камеры через Gradle
        $gradleArgs = @(
            ":shared:desktopTest",
            "--tests", "com.company.ipcamera.shared.test.RtspValidationTest",
            "-Dcamera.id=$CameraId",
            "-Dcamera.name=$CameraName",
            "-Dcamera.rtsp.url=$RtspUrl",
            "-Dtest.duration.minutes=$DurationMinutes"
        )
        
        if ($Verbose) {
            $gradleArgs += "--info"
        }
        
        Write-ValidationLog "Starting Gradle test for $CameraName..."
        
        $testProcess = Start-Process -FilePath "gradlew" -ArgumentList $gradleArgs -NoNewWindow -PassThru -Wait
        
        if ($testProcess.ExitCode -eq 0) {
            $result.Status = "PASSED"
            Write-ValidationLog "Camera test PASSED: $CameraName" -Level "SUCCESS"
        }
        else {
            $result.Status = "FAILED"
            $result.Errors += @{
                Timestamp = (Get-Date).ToString('o')
                Type = "TestFailure"
                Message = "Gradle test failed with exit code: $($testProcess.ExitCode)"
            }
            Write-ValidationLog "Camera test FAILED: $CameraName (exit code: $($testProcess.ExitCode))" -Level "ERROR"
        }
    }
    catch {
        $result.Status = "ERROR"
        $result.Errors += @{
            Timestamp = (Get-Date).ToString('o')
            Type = "Exception"
            Message = $_.Exception.Message
        }
        Write-ValidationLog "Camera test ERROR: $CameraName - $($_.Exception.Message)" -Level "FATAL"
    }
    
    $result.EndTime = (Get-Date).ToString('o')
    return $result
}

# Основной цикл тестирования
Write-ValidationLog "Starting RTSP validation tests..."

$startTime = Get-Date
$passedCameras = 0
$failedCameras = 0
$skippedCameras = 0

foreach ($camera in $config.cameras) {
    # Проверка флагов пропуска
    if ($SkipEmulators -and $camera.type -eq "emulator") {
        Write-ValidationLog "Skipping emulator: $($camera.name)" -Level "INFO"
        $skippedCameras++
        continue
    }
    
    if ($SkipRealCameras -and $camera.type -ne "emulator") {
        Write-ValidationLog "Skipping real camera: $($camera.name)" -Level "INFO"
        $skippedCameras++
        continue
    }
    
    # Проверка включена ли камера
    if ($camera.enabled -eq $false) {
        Write-ValidationLog "Skipping disabled camera: $($camera.name)" -Level "INFO"
        $skippedCameras++
        continue
    }
    
    # Проверка входит ли камера в профиль
    if ($Profile -ne "full" -and $Profile -ne "long") {
        if ($camera.type -eq "real") {
            Write-ValidationLog "Skipping real camera for $Profile profile: $($camera.name)" -Level "INFO"
            $skippedCameras++
            continue
        }
    }
    
    $testResults.TotalCameras++
    
    Write-Host ""
    Write-Host "Testing camera $($testResults.TotalCameras): $($camera.name)" -ForegroundColor Cyan
    
    $cameraResult = Test-RTSPCamera `
        -CameraId $camera.id `
        -CameraName $camera.name `
        -RtspUrl $camera.rtspUrl `
        -DurationMinutes $profile.DurationMinutes
    
    $testResults.CameraResults += $cameraResult
    
    if ($cameraResult.Status -eq "PASSED") {
        $passedCameras++
        Write-Host "  ✅ PASSED" -ForegroundColor Green
    }
    elseif ($cameraResult.Status -eq "FAILED") {
        $failedCameras++
        Write-Host "  ❌ FAILED" -ForegroundColor Red
    }
    else {
        $failedCameras++
        Write-Host "  ⚠️  ERROR" -ForegroundColor Yellow
    }
}

# Финализация
$testResults.EndTime = (Get-Date).ToString('o')
$testResults.Duration = [math]::Round(((Get-Date) - $startTime).TotalMinutes, 2)
$testResults.PassedCameras = $passedCameras
$testResults.FailedCameras = $failedCameras
$testResults.SkippedCameras = $skippedCameras

if ($failedCameras -eq 0) {
    $testResults.OverallStatus = "PASSED"
}
else {
    $testResults.OverallStatus = "FAILED"
}

# Генерация отчёта
Write-ValidationLog ""
Write-ValidationLog "========================================"
Write-ValidationLog "Validation Complete"
Write-ValidationLog "========================================"
Write-ValidationLog "Total Cameras: $($testResults.TotalCameras)"
Write-ValidationLog "Passed: $passedCameras"
Write-ValidationLog "Failed: $failedCameras"
Write-ValidationLog "Skipped: $skippedCameras"
Write-ValidationLog "Duration: $($testResults.Duration) minutes"
Write-ValidationLog "Overall Status: $($testResults.OverallStatus)"

# Сохранение результатов
$resultsFile = "$OutputDir/validation-results-$Profile-$((Get-Date -Format 'yyyyMMdd-HHmmss')).json"
$testResults | ConvertTo-Json -Depth 10 | Out-File -FilePath $resultsFile -Encoding UTF8

# Генерация markdown отчёта
$reportFile = "$OutputDir/validation-report-$Profile-$((Get-Date -Format 'yyyyMMdd-HHmmss')).md"

$report = Build-ValidationReport -Results $testResults
$report | Out-File -FilePath $reportFile -Encoding UTF8

# Вывод итогов
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Validation Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Profile: $Profile" -ForegroundColor Green
Write-Host "Status: $($testResults.OverallStatus)" -ForegroundColor $(if ($testResults.OverallStatus -eq "PASSED") { "Green" } else { "Red" })
Write-Host "Cameras: $($testResults.TotalCameras) total, $passedCameras passed, $failedCameras failed, $skippedCameras skipped" -ForegroundColor Green
Write-Host "Duration: $($testResults.Duration) minutes" -ForegroundColor Green
Write-Host ""
Write-Host "Reports:" -ForegroundColor Cyan
Write-Host "  JSON: $resultsFile" -ForegroundColor Green
Write-Host "  MD:   $reportFile" -ForegroundColor Green
Write-Host "  Log:  $OutputDir/validation.log" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

# Exit code
if ($testResults.OverallStatus -eq "PASSED") {
    exit 0
}
else {
    exit 1
}

# Функция генерации markdown отчёта
function Build-ValidationReport {
    param([hashtable]$Results)
    
    $report = @"
# RTSP Final Validation Report

**Profile:** $($Results.Profile)  
**Date:** $((Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))  
**Status:** $(if ($Results.OverallStatus -eq "PASSED") { "✅ PASSED" } else { "❌ FAILED" })

## Summary

| Metric | Value |
|--------|-------|
| Total Cameras | $($Results.TotalCameras) |
| Passed | $($Results.PassedCameras) |
| Failed | $($Results.FailedCameras) |
| Skipped | $($Results.SkippedCameras) |
| Duration | $($Results.Duration) minutes |

## Camera Results

"@

    foreach ($camera in $Results.CameraResults) {
        $report += @"

### $($camera.CameraName)

| Field | Value |
|-------|-------|
| ID | $($camera.CameraId) |
| RTSP URL | $($camera.RtspUrl) |
| Status | $(if ($camera.Status -eq "PASSED") { "✅ PASSED" } else { "❌ $($camera.Status)" }) |
| Start Time | $($camera.StartTime) |
| End Time | $($camera.EndTime) |

"@

        if ($camera.Errors.Count -gt 0) {
            $report += "#### Errors`n`n"
            foreach ($error in $camera.Errors) {
                $report += "- **[$($error.Type)]** $($error.Message)`n"
            }
            $report += "`n"
        }
    }

    $report += @"
## Test Environment

- **OS:** $([System.Environment]::OSVersion.VersionString)
- **PowerShell:** $($PSVersionTable.PSVersion)
- **Test Time:** $($Results.StartTime) - $($Results.EndTime)

---

*Generated by RTSP Final Validation Suite*
"@

    return $report
}
