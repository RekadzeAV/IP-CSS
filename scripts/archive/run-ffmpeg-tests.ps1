# РЎРєСЂРёРїС‚ РґР»СЏ Р·Р°РїСѓСЃРєР° С‚РµСЃС‚РѕРІ FFmpeg РґРµРєРѕРґРёСЂРѕРІР°РЅРёСЏ
# РСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ: .\scripts\run-ffmpeg-tests.ps1 [--unit] [--integration] [--config <file>]

param(
    [switch]$Unit = $false,
    [switch]$Integration = $false,
    [string]$Config = "native/video-processing/test/test_config.json",
    [switch]$Help = $false,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($Help -or $ShowHelp) {
    Write-Host "Run native video_processing_tests.exe (unit / integration) from test/build."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-ffmpeg-tests.ps1 -ShowHelp"
    Write-Host "  .\scripts\run-ffmpeg-tests.ps1 [options]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Unit, -u           Run unit tests only"
    Write-Host "  -Integration, -i    Run integration tests (with -Unit or default all)"
    Write-Host "  -Config, -c         Path to test config (default native/video-processing/test/test_config.json)"
    Write-Host "  -Help, -h           Show this help"
    Write-Host "  -ShowHelp           Same as -Help"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\scripts\run-ffmpeg-tests.ps1 -Unit"
    Write-Host "  .\scripts\run-ffmpeg-tests.ps1 -Integration -Config my_config.json"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Tests passed"
    Write-Host "  1  Missing test exe or test failures (propagated from native runner)"
    exit 0
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$TestBuildDir = Join-Path $ProjectRoot "native\video-processing\test\build"

Write-Host "=== FFmpeg Testing Script ===" -ForegroundColor Cyan
Write-Host ""

# РџСЂРѕРІРµСЂРєР° РЅР°Р»РёС‡РёСЏ СЃРѕР±СЂР°РЅРЅС‹С… С‚РµСЃС‚РѕРІ
$TestExecutable = Join-Path $TestBuildDir "video_processing_tests.exe"
if (-not (Test-Path $TestExecutable)) {
    Write-Host "[FAIL] Test executable not found: $TestExecutable" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please build tests first:" -ForegroundColor Yellow
    Write-Host "  cd native/video-processing/test" -ForegroundColor Gray
    Write-Host "  mkdir build && cd build" -ForegroundColor Gray
    Write-Host "  cmake .. -DENABLE_FFMPEG=ON" -ForegroundColor Gray
    Write-Host "  cmake --build . --config Release" -ForegroundColor Gray
    exit 1
}

Write-Host "[OK] Test executable found" -ForegroundColor Green
Write-Host ""

# РћРїСЂРµРґРµР»РµРЅРёРµ СЂРµР¶РёРјР° Р·Р°РїСѓСЃРєР°
$RunUnit = $Unit
$RunIntegration = $Integration

# Р•СЃР»Рё РЅРµ СѓРєР°Р·Р°РЅРѕ, Р·Р°РїСѓСЃРєР°РµРј РІСЃРµ С‚РµСЃС‚С‹
if (-not $RunUnit -and -not $RunIntegration) {
    $RunUnit = $true
    $RunIntegration = $true
    Write-Host "No specific mode specified, running all tests" -ForegroundColor Yellow
    Write-Host ""
}

# Р¤РѕСЂРјРёСЂРѕРІР°РЅРёРµ Р°СЂРіСѓРјРµРЅС‚РѕРІ
$TestArgs = @()

if ($RunIntegration) {
    $TestArgs += "--integration"

    if ($Config) {
        $ConfigPath = if ([System.IO.Path]::IsPathRooted($Config)) {
            $Config
        } else {
            Join-Path $ProjectRoot $Config
        }

        if (Test-Path $ConfigPath) {
            $TestArgs += "--config"
            $TestArgs += $ConfigPath
            Write-Host "Using config: $ConfigPath" -ForegroundColor Cyan
        } else {
            Write-Host "[WARN] Config file not found: $ConfigPath" -ForegroundColor Yellow
            Write-Host "   Using default configuration" -ForegroundColor Yellow
        }
    }
}

Write-Host ""
Write-Host "=== Running Tests ===" -ForegroundColor Cyan
Write-Host "Executable: $TestExecutable" -ForegroundColor Gray
if ($TestArgs.Count -gt 0) {
    Write-Host "Arguments: $($TestArgs -join ' ')" -ForegroundColor Gray
}
Write-Host ""

# Р—Р°РїСѓСЃРє С‚РµСЃС‚РѕРІ
Push-Location $TestBuildDir
try {
    $OutputFile = Join-Path $TestBuildDir "test_results_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"

    Write-Host "Running tests..." -ForegroundColor Yellow
    Write-Host "Results will be saved to: $OutputFile" -ForegroundColor Gray
    Write-Host ""

    & $TestExecutable $TestArgs | Tee-Object -FilePath $OutputFile

    $ExitCode = $LASTEXITCODE

    Write-Host ""
    Write-Host "=== Test Results ===" -ForegroundColor Cyan
    if ($ExitCode -eq 0) {
        Write-Host "[OK] All tests passed!" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Some tests failed (exit code: $ExitCode)" -ForegroundColor Red
    }

    Write-Host "Results saved to: $OutputFile" -ForegroundColor Gray

    exit $ExitCode
} finally {
    Pop-Location
}
