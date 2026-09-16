# Full native test environment: CMake checks, build video_processing, build tests, copy test_config.json.
# Usage: .\scripts\build-test-environment.ps1

param(
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Orchestrate native video_processing + test build and test_config.json bootstrap (CMake/FFmpeg)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\build-test-environment.ps1 -ShowHelp"
    Write-Host "  .\scripts\build-test-environment.ps1"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Completed (warnings may be printed)"
    Write-Host "  1  CMake missing"
    exit 0
}

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

Write-Host "=== Building Complete Test Environment ===" -ForegroundColor Cyan
Write-Host ""

# РЁР°Рі 1: РџСЂРѕРІРµСЂРєР° Р·Р°РІРёСЃРёРјРѕСЃС‚РµР№
Write-Host "Step 1: Checking dependencies..." -ForegroundColor Yellow

# РџСЂРѕРІРµСЂРєР° CMake
try {
    $cmakeVersion = cmake --version 2>&1 | Select-Object -First 1
    Write-Host "[OK] CMake: $cmakeVersion" -ForegroundColor Green
} catch {
    Write-Host "[FAIL] CMake not found. Please install CMake." -ForegroundColor Red
    exit 1
}

# РџСЂРѕРІРµСЂРєР° FFmpeg
$ffmpegFound = $false
$ffmpegPaths = @("C:\ffmpeg", "$env:FFMPEG_DIR")
foreach ($path in $ffmpegPaths) {
    if ($path -and (Test-Path "$path\include\libavformat\avformat.h")) {
        Write-Host "[OK] FFmpeg found: $path" -ForegroundColor Green
        $env:FFMPEG_DIR = $path
        $ffmpegFound = $true
        break
    }
}

if (-not $ffmpegFound) {
    Write-Host "[WARN]  FFmpeg not found. Tests may not work properly." -ForegroundColor Yellow
    Write-Host "   Install FFmpeg or set FFMPEG_DIR environment variable" -ForegroundColor Yellow
}

Write-Host ""

# РЁР°Рі 2: РЎР±РѕСЂРєР° РѕСЃРЅРѕРІРЅРѕР№ Р±РёР±Р»РёРѕС‚РµРєРё
Write-Host "Step 2: Building main library..." -ForegroundColor Yellow

$NativeDir = Join-Path $ProjectRoot "native\video-processing"
$BuildDir = Join-Path $NativeDir "build\windows-x64"

if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
}

Push-Location $BuildDir
try {
    Write-Host "  Configuring CMake..." -ForegroundColor Gray

    $cmakeArgs = @(
        "..\..",
        "-DCMAKE_BUILD_TYPE=Release",
        "-DENABLE_FFMPEG=ON",
        "-DENABLE_OPENCV=OFF"
    )

    if ($env:FFMPEG_DIR) {
        $cmakeArgs += "-DFFMPEG_DIR=$env:FFMPEG_DIR"
    }

    cmake @cmakeArgs 2>&1 | Out-Null

    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [WARN]  CMake configuration may have issues" -ForegroundColor Yellow
        Write-Host "     (This is expected if compiler is not available)" -ForegroundColor Gray
    } else {
        Write-Host "  [OK] CMake configured" -ForegroundColor Green

        Write-Host "  Building..." -ForegroundColor Gray
        cmake --build . --config Release 2>&1 | Out-Null

        if ($LASTEXITCODE -eq 0) {
            Write-Host "  [OK] Library built successfully" -ForegroundColor Green
        } else {
            Write-Host "  [WARN]  Build may have issues (compiler required)" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "  [WARN]  Build step skipped (compiler may not be available)" -ForegroundColor Yellow
} finally {
    Pop-Location
}

Write-Host ""

# РЁР°Рі 3: РЎР±РѕСЂРєР° С‚РµСЃС‚РѕРІ
Write-Host "Step 3: Building tests..." -ForegroundColor Yellow

$TestDir = Join-Path $ProjectRoot "native\video-processing\test"
$TestBuildDir = Join-Path $TestDir "build"

if (-not (Test-Path $TestBuildDir)) {
    New-Item -ItemType Directory -Path $TestBuildDir -Force | Out-Null
}

Push-Location $TestBuildDir
try {
    Write-Host "  Configuring CMake..." -ForegroundColor Gray

    $cmakeArgs = @(
        "..",
        "-DCMAKE_BUILD_TYPE=Release",
        "-DENABLE_FFMPEG=ON"
    )

    if ($env:FFMPEG_DIR) {
        $cmakeArgs += "-DFFMPEG_DIR=$env:FFMPEG_DIR"
    }

    cmake @cmakeArgs 2>&1 | Out-Null

    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [WARN]  CMake configuration may have issues" -ForegroundColor Yellow
    } else {
        Write-Host "  [OK] CMake configured" -ForegroundColor Green

        Write-Host "  Building..." -ForegroundColor Gray
        cmake --build . --config Release 2>&1 | Out-Null

        if ($LASTEXITCODE -eq 0) {
            Write-Host "  [OK] Tests built successfully" -ForegroundColor Green
        } else {
            Write-Host "  [WARN]  Build may have issues (compiler required)" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "  [WARN]  Build step skipped (compiler may not be available)" -ForegroundColor Yellow
} finally {
    Pop-Location
}

Write-Host ""

# РЁР°Рі 4: РќР°СЃС‚СЂРѕР№РєР° РєРѕРЅС„РёРіСѓСЂР°С†РёРё С‚РµСЃС‚РѕРІ
Write-Host "Step 4: Setting up test configuration..." -ForegroundColor Yellow

$ConfigExample = Join-Path $TestDir "test_config.json.example"
$ConfigFile = Join-Path $TestDir "test_config.json"

if (-not (Test-Path $ConfigFile)) {
    if (Test-Path $ConfigExample) {
        Copy-Item $ConfigExample $ConfigFile -Force
        Write-Host "  [OK] Config file created: $ConfigFile" -ForegroundColor Green
        Write-Host "     Please edit with your camera settings" -ForegroundColor Gray
    } else {
        Write-Host "  [WARN]  Config example not found" -ForegroundColor Yellow
    }
} else {
    Write-Host "  [OK] Config file already exists" -ForegroundColor Green
}

Write-Host ""

# РС‚РѕРіРё
Write-Host "=== Setup Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Edit test_config.json with your camera settings" -ForegroundColor White
Write-Host "  2. Run tests: .\scripts\run-ffmpeg-tests.ps1 -Integration" -ForegroundColor White
Write-Host "  3. Analyze results: .\scripts\analyze-test-results.ps1 <results_file>" -ForegroundColor White
Write-Host ""
