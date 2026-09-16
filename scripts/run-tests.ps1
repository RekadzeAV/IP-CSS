# Запуск всех тестов для RTSP Client
# IP-CSS RTSP Client - Test Runner

param(
    [string]$BuildType = "Release",
    [switch]$RunUnitTests,
    [switch]$RunIntegrationTests,
    [switch]$RunAllTests,
    [switch]$BuildOnly,
    [string]$TestFilter = ""
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path | Split-Path -Parent
$buildDir = Join-Path $projectRoot "build"
$testDir = Join-Path $buildDir "test"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RTSP Client Test Runner" -ForegroundColor Cyan
Write-Host "  Project: $projectRoot" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Функция для выполнения команды
function Invoke-Command-Safe {
    param([string]$Command, [string]$Description)
    Write-Host "[$Description] $Command" -ForegroundColor Yellow
    try {
        Invoke-Expression $Command
        if ($LASTEXITCODE -ne 0) {
            Write-Host "❌ FAILED: $Description" -ForegroundColor Red
            exit 1
        }
        Write-Host "✅ PASSED: $Description" -ForegroundColor Green
    } catch {
        Write-Host "❌ ERROR: $Description" -ForegroundColor Red
        throw
    }
}

# Сборка проекта
function Invoke-Build {
    Write-Host "`n--- Building Project ---" -ForegroundColor Cyan
    
    if (-not (Test-Path $buildDir)) {
        New-Item -ItemType Directory -Path $buildDir | Out-Null
    }
    
    Set-Location $buildDir
    
    # CMake configure
    Invoke-Command-Safe -Command "cmake .. -DCMAKE_BUILD_TYPE=$BuildType" -Description "CMake configure"
    
    # Build
    Invoke-Command-Safe -Command "cmake --build . --config $BuildType" -Description "Build project"
    
    Set-Location $projectRoot
}

# Запуск unit тестов
function Invoke-UnitTests {
    Write-Host "`n--- Running Unit Tests ---" -ForegroundColor Cyan
    
    $hwDecoderTest = Join-Path $buildDir "test/hw_decoder_tests/hw_decoder_tests.exe"
    if (Test-Path $hwDecoderTest) {
        Set-Location (Split-Path $hwDecoderTest)
        Write-Host "Running HW Decoder tests..." -ForegroundColor Yellow
        & $hwDecoderTest
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ HW Decoder tests PASSED" -ForegroundColor Green
        } else {
            Write-Host "❌ HW Decoder tests FAILED" -ForegroundColor Red
        }
        Set-Location $projectRoot
    } else {
        Write-Host "⚠️  HW Decoder tests not found (build with -DBUILD_HW_DECODER_TESTS=ON)" -ForegroundColor Yellow
    }
    
    # Video processing tests
    $videoTest = Join-Path $buildDir "test/video_processing_tests.exe"
    if (Test-Path $videoTest) {
        Set-Location (Split-Path $videoTest)
        Write-Host "Running Video Processing tests..." -ForegroundColor Yellow
        & $videoTest
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Video Processing tests PASSED" -ForegroundColor Green
        } else {
            Write-Host "❌ Video Processing tests FAILED" -ForegroundColor Red
        }
        Set-Location $projectRoot
    } else {
        Write-Host "⚠️  Video Processing tests not found" -ForegroundColor Yellow
    }
}

# Запуск интеграционных тестов
function Invoke-IntegrationTests {
    Write-Host "`n--- Running Integration Tests ---" -ForegroundColor Cyan
    
    $integrationTest = Join-Path $buildDir "test/integration_tests/integration_tests.exe"
    if (Test-Path $integrationTest) {
        Set-Location (Split-Path $integrationTest)
        Write-Host "Running Integration tests..." -ForegroundColor Yellow
        & $integrationTest
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Integration tests PASSED" -ForegroundColor Green
        } else {
            Write-Host "❌ Integration tests FAILED" -ForegroundColor Red
        }
        Set-Location $projectRoot
    } else {
        Write-Host "⚠️  Integration tests not found (build with -DBUILD_INTEGRATION_TESTS=ON)" -ForegroundColor Yellow
    }
}

# Запуск CTest
function Invoke-CTest {
    Write-Host "`n--- Running CTest ---" -ForegroundColor Cyan
    
    Set-Location $buildDir
    ctest --output-on-failure -C $BuildType
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ CTest PASSED" -ForegroundColor Green
    } else {
        Write-Host "❌ CTest FAILED" -ForegroundColor Red
    }
    Set-Location $projectRoot
}

# Main
Write-Host "`nOptions:" -ForegroundColor Cyan
Write-Host "  BuildType: $BuildType" -ForegroundColor White
Write-Host "  RunUnitTests: $RunUnitTests" -ForegroundColor White
Write-Host "  RunIntegrationTests: $RunIntegrationTests" -ForegroundColor White
Write-Host "  RunAllTests: $RunAllTests" -ForegroundColor White
Write-Host "  BuildOnly: $BuildOnly" -ForegroundColor White

# Если не указаны никакие тесты, запускаем все
if (-not $RunUnitTests -and -not $RunIntegrationTests -and -not $RunAllTests) {
    $RunAllTests = $true
}

# Build
Invoke-Build

if ($BuildOnly) {
    Write-Host "`n✅ Build only mode - skipping tests" -ForegroundColor Cyan
    exit 0
}

# Tests
if ($RunUnitTests -or $RunAllTests) {
    Invoke-UnitTests
}

if ($RunIntegrationTests -or $RunAllTests) {
    Invoke-IntegrationTests
}

if ($RunAllTests) {
    Invoke-CTest
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  All tests completed!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
