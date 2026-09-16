#!/usr/bin/env pwsh
# Integration Test для Полевой Валидации
# Полный цикл тестирования в одном скрипте

param(
    [string]$ConfigPath = "config/test-cameras-local-network.example.json",
    [string]$ApiBaseUrl = "http://localhost:8080",
    [int]$HlsDurationSeconds = 120,
    [string]$OutputDir = "diagnostics/integration-test",
    [switch]$QuickMode,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
Integration Test для Полевой Валидации

Usage:
  .\scripts\integration-test.ps1 [-QuickMode] [-ConfigPath <path>]

Options:
  -ConfigPath           Конфигурация камер (default: config/test-cameras-local-network.example.json)
  -ApiBaseUrl           URL API сервера (default: http://localhost:8080)
  -HlsDurationSeconds   Длительность HLS теста (default: 120)
  -OutputDir            Директория для отчётов (default: diagnostics/integration-test)
  -QuickMode            Быстрый режим (минимальное время тестов)
  -ShowHelp             Показать эту справку

Examples:
  # Полный тест
  .\scripts\integration-test.ps1

  # Быстрый режим
  .\scripts\integration-test.ps1 -QuickMode

"@
    exit 0
}

# ============================================================================
# Setup
# ============================================================================

$ErrorActionPreference = "Stop"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

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
    }
}

# ============================================================================
# Main Execution
# ============================================================================

Write-Separator "Integration Test для Полевой Валидации"
Write-Host "Timestamp:    $timestamp" -ForegroundColor White
Write-Host "Config:       $ConfigPath" -ForegroundColor White
Write-Host "API URL:      $ApiBaseUrl" -ForegroundColor White
Write-Host "Quick Mode:   $QuickMode" -ForegroundColor White
Write-Host ""

$totalResults = @{
    passed = 0
    failed = 0
    total = 0
}

# Phase 1: Pre-flight Check
Write-Separator "Phase 1: Pre-flight Check"

$preflightScript = Join-Path $scriptDir "field-validation-preflight.ps1"
if (Test-Path $preflightScript) {
    & $preflightScript -ApiBaseUrl $ApiBaseUrl -ConfigPath $ConfigPath
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Pre-flight check failed. Aborting."
        exit 1
    }
    $totalResults.passed++
    $totalResults.total++
} else {
    Write-Warn "Pre-flight script not found, skipping"
}

# Phase 2: Camera Discovery
Write-Separator "Phase 2: Camera Discovery"

$discoveryScript = Join-Path $scriptDir "discover-rtsp-cameras.ps1"
if (Test-Path $discoveryScript) {
    & $discoveryScript -OutputFile (Join-Path $OutputDir "discovered-cameras.json")
    if ($LASTEXITCODE -eq 0) {
        $totalResults.passed++
        $totalResults.total++
    } else {
        $totalResults.failed++
        $totalResults.total++
    }
}

# Phase 3: RTSP Tests
Write-Separator "Phase 3: RTSP Tests"

$rtspScript = Join-Path $scriptDir "test-rtsp-real-cameras.ps1"
if (Test-Path $rtspScript) {
    $rtspParams = @{
        ConfigPath = $ConfigPath
        OutputDir = (Join-Path $OutputDir "rtsp-tests")
    }
    
    if ($QuickMode) {
        $rtspParams["ConnectionTestOnly"] = $true
    } else {
        $rtspParams["FullTest"] = $true
    }
    
    & $rtspScript @rtspParams
    if ($LASTEXITCODE -eq 0) {
        $totalResults.passed++
        $totalResults.total++
    } else {
        $totalResults.failed++
        $totalResults.total++
    }
}

# Phase 4: HLS Tests
Write-Separator "Phase 4: HLS Tests"

$hlsScript = Join-Path $scriptDir "hls-runtime-stability-test.ps1"
if (Test-Path $hlsScript) {
    $hlsParams = @{
        LongRunDurationSeconds = $HlsDurationSeconds
        OutputDir = (Join-Path $OutputDir "hls-tests")
    }
    
    if ($QuickMode) {
        $hlsParams["SkipCleanupTest"] = $true
        $hlsParams["SkipReconnectTest"] = $true
        $hlsParams["LongRunDurationSeconds"] = 30
    }
    
    & $hlsScript @hlsParams
    if ($LASTEXITCODE -eq 0) {
        $totalResults.passed++
        $totalResults.total++
    } else {
        $totalResults.failed++
        $totalResults.total++
    }
}

# Phase 5: Screenshot Tests
Write-Separator "Phase 5: Screenshot Tests"

$screenshotScript = Join-Path $scriptDir "screenshot-pipeline-test.ps1"
if (Test-Path $screenshotScript) {
    & $screenshotScript -OutputDir (Join-Path $OutputDir "screenshot-tests")
    if ($LASTEXITCODE -eq 0) {
        $totalResults.passed++
        $totalResults.total++
    } else {
        $totalResults.failed++
        $totalResults.total++
    }
}

# Phase 6: Aggregate Results
Write-Separator "Phase 6: Aggregate Results"

$aggregateScript = Join-Path $scriptDir "aggregate-test-results.ps1"
if (Test-Path $aggregateScript) {
    & $aggregateScript -BaseDir $OutputDir -OutputDir (Join-Path $OutputDir "aggregated")
    if ($LASTEXITCODE -eq 0) {
        $totalResults.passed++
        $totalResults.total++
    }
}

# ============================================================================
# Final Summary
# ============================================================================

Write-Separator "Integration Test Summary"

$successRate = if ($totalResults.total -gt 0) {
    [math]::Round(($totalResults.passed / $totalResults.total) * 100, 1)
} else {
    0
}

Write-Host "Total Phases:  $($totalResults.total)" -ForegroundColor White
Write-Host "Passed:        $($totalResults.passed)" -ForegroundColor Green
Write-Host "Failed:        $($totalResults.failed)" -ForegroundColor $(if ($totalResults.failed -gt 0) { "Red" } else { "Green" })
Write-Host "Success Rate:  $successRate%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } elseif ($successRate -ge 50) { "Yellow" } else { "Red" })
Write-Host ""

# Generate final report
$reportPath = Join-Path $OutputDir "integration-test-report-$timestamp.md"
$reportLines = @(
    "# Integration Test Report",
    "",
    "- **Timestamp:** $timestamp",
    "- **Config:** $ConfigPath",
    "- **API URL:** $ApiBaseUrl",
    "- **Quick Mode:** $QuickMode",
    "",
    "## Summary",
    "",
    "| Metric | Value |",
    "|--------|-------|",
    "| Total Phases | $($totalResults.total) |",
    "| Passed | $($totalResults.passed) |",
    "| Failed | $($totalResults.failed) |",
    "| Success Rate | $successRate% |",
    "",
    "## Output Files",
    "",
    "- Discovered Cameras: `$(Join-Path $OutputDir "discovered-cameras.json")`",
    "- RTSP Tests: `$(Join-Path $OutputDir "rtsp-tests")`",
    "- HLS Tests: `$(Join-Path $OutputDir "hls-tests")`",
    "- Screenshot Tests: `$(Join-Path $OutputDir "screenshot-tests")`",
    "- Aggregated: `$(Join-Path $OutputDir "aggregated")`",
    ""
)

$reportLines | Set-Content -Path $reportPath -Encoding UTF8
Write-Success "Report saved to: $reportPath"

Write-Host ""
if ($totalResults.failed -gt 0) {
    Write-Error "Integration test completed with failures"
    exit 1
} else {
    Write-Success "Integration test PASSED!"
    exit 0
}
