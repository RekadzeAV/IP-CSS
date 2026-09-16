# РЎРєСЂРёРїС‚ РґР»СЏ Р°РЅР°Р»РёР·Р° СЂРµР·СѓР»СЊС‚Р°С‚РѕРІ С‚РµСЃС‚РѕРІ
# РСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ: .\scripts\analyze-test-results.ps1 <test_output_file> [--html] [--markdown]

param(
    [string]$InputFile = "",
    [switch]$Html = $false,
    [switch]$Markdown = $false,
    [switch]$Help = $false,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($Help -or $ShowHelp) {
    Write-Host "Run test_analyzer.exe on a native test log (HTML/Markdown optional)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\analyze-test-results.ps1 -ShowHelp"
    Write-Host "  .\scripts\analyze-test-results.ps1 -InputFile <test_output.txt> [-Html] [-Markdown]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -InputFile  Path to test output log (required unless -ShowHelp)"
    Write-Host "  -Html       Generate HTML report next to input"
    Write-Host "  -Markdown   Generate Markdown report next to input"
    Write-Host "  -Help       Show this help"
    Write-Host "  -ShowHelp   Same as -Help"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\scripts\analyze-test-results.ps1 -InputFile test_results.txt -Html"
    Write-Host "  .\scripts\analyze-test-results.ps1 -InputFile test_results.txt -Markdown"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Analyzer succeeded"
    Write-Host "  1  Missing input, missing analyzer build, or analyzer failure"
    exit 0
}

if ([string]::IsNullOrWhiteSpace($InputFile)) {
    Write-Host "InputFile is required. Example: .\scripts\analyze-test-results.ps1 -InputFile native\video-processing\test\build\test_results.txt" -ForegroundColor Red
    exit 1
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$TestBuildDir = Join-Path $ProjectRoot "native\video-processing\test\build"
$AnalyzerExe = Join-Path $TestBuildDir "test_analyzer.exe"

# РџСЂРѕРІРµСЂРєР° РІС…РѕРґРЅРѕРіРѕ С„Р°Р№Р»Р°
if (-not (Test-Path $InputFile)) {
    Write-Host "[FAIL] Input file not found: $InputFile" -ForegroundColor Red
    exit 1
}

# РџСЂРѕРІРµСЂРєР° Р°РЅР°Р»РёР·Р°С‚РѕСЂР°
if (-not (Test-Path $AnalyzerExe)) {
    Write-Host "[WARN] Test analyzer not found: $AnalyzerExe" -ForegroundColor Yellow
    Write-Host "   Building analyzer..." -ForegroundColor Yellow

    Push-Location $TestBuildDir
    try {
        cmake --build . --target test_analyzer --config Release
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[FAIL] Failed to build analyzer" -ForegroundColor Red
            Pop-Location
            exit 1
        }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $AnalyzerExe)) {
    Write-Host "[FAIL] Analyzer not available. Please build tests first." -ForegroundColor Red
    exit 1
}

Write-Host "=== Analyzing Test Results ===" -ForegroundColor Cyan
Write-Host "Input file: $InputFile" -ForegroundColor Gray
Write-Host ""

# Р¤РѕСЂРјРёСЂРѕРІР°РЅРёРµ Р°СЂРіСѓРјРµРЅС‚РѕРІ
$AnalyzerArgs = @($InputFile)
if ($Html) {
    $AnalyzerArgs += "--html"
}
if ($Markdown) {
    $AnalyzerArgs += "--markdown"
}

# Р—Р°РїСѓСЃРє Р°РЅР°Р»РёР·Р°С‚РѕСЂР°
Push-Location $TestBuildDir
try {
    & $AnalyzerExe $AnalyzerArgs

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[OK] Analysis complete" -ForegroundColor Green

        if ($Html) {
            $htmlFile = "$InputFile.html"
            if (Test-Path $htmlFile) {
                Write-Host "HTML report: $htmlFile" -ForegroundColor Cyan
            }
        }

        if ($Markdown) {
            $mdFile = "$InputFile.md"
            if (Test-Path $mdFile) {
                Write-Host "Markdown report: $mdFile" -ForegroundColor Cyan
            }
        }
    } else {
        Write-Host ""
        Write-Host "[FAIL] Analysis failed" -ForegroundColor Red
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
