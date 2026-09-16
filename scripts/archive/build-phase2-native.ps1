# Build Phase 2 Native Libraries
# РђРІС‚РѕРјР°С‚РёР·РёСЂРѕРІР°РЅРЅР°СЏ СЃР±РѕСЂРєР° РЅР°С‚РёРІРЅС‹С… Р±РёР±Р»РёРѕС‚РµРє РґР»СЏ Р­С‚Р°РїР° 2

[CmdletBinding()]
param(
    [Parameter()]
    [ValidateSet('Release', 'Debug')]
    [string]$BuildType = 'Release',
    
    [Parameter()]
    [ValidateSet('windows', 'linux', 'macos')]
    [string]$Platform = 'windows',
    
    [Parameter()]
    [switch]$SkipOpenCV,
    
    [Parameter()]
    [switch]$CleanBuild,
    
    [Parameter()]
    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Phase 2 Native Library Build" -ForegroundColor Cyan
Write-Host "Platform: $Platform, BuildType: $BuildType" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# РџСѓС‚Рё
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$NativeDir = Join-Path $RootDir "native\video-processing"
$BuildDir = Join-Path $NativeDir "build"

# РџСЂРѕРІРµСЂРєР° Р·Р°РІРёСЃРёРјРѕСЃС‚РµР№
Write-Host "`n[1/5] Checking dependencies..." -ForegroundColor Yellow

# FFmpeg
Write-Host "  Checking FFmpeg..." -ForegroundColor Gray
try {
    $ffmpegVersion = ffmpeg -version 2>&1 | Select-Object -First 1
    Write-Host "  OK FFmpeg: $ffmpegVersion" -ForegroundColor Green
} catch {
    Write-Host "  ERROR FFmpeg not found!" -ForegroundColor Red
    exit 1
}

# CMake
Write-Host "  Checking CMake..." -ForegroundColor Gray
try {
    $cmakeVersion = cmake --version | Select-Object -First 1
    Write-Host "  OK CMake: $cmakeVersion" -ForegroundColor Green
} catch {
    Write-Host "  ERROR CMake not found!" -ForegroundColor Red
    exit 1
}

# РљРѕРјРїРёР»СЏС‚РѕСЂ
Write-Host "  Checking Compiler..." -ForegroundColor Gray
if ($Platform -eq 'windows') {
    try {
        $gppVersion = g++ --version | Select-Object -First 1
        Write-Host "  OK g++: $gppVersion" -ForegroundColor Green
    } catch {
        Write-Host "  ERROR g++ not found!" -ForegroundColor Red
        exit 1
    }
}

# РџСЂРѕРІРµСЂРєР° СЂРµР¶РёРјР° CheckOnly
if ($CheckOnly) {
    Write-Host "`nOK Dependency check completed" -ForegroundColor Green
    Write-Host "Skipping build (CheckOnly mode)" -ForegroundColor Yellow
    exit 0
}

# РћС‡РёСЃС‚РєР° РїСЂРµРґС‹РґСѓС‰РµР№ СЃР±РѕСЂРєРё
if ($CleanBuild -and (Test-Path $BuildDir)) {
    Write-Host "`n[2/5] Cleaning previous build..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force $BuildDir
    Write-Host "  OK Cleaned" -ForegroundColor Green
}

# РЎРѕР·РґР°РЅРёРµ РґРёСЂРµРєС‚РѕСЂРёРё
if (-not (Test-Path $BuildDir)) {
    Write-Host "`n[2/5] Creating build directory..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $BuildDir | Out-Null
    Write-Host "  OK Created: $BuildDir" -ForegroundColor Green
}

# CMake РєРѕРЅС„РёРіСѓСЂР°С†РёСЏ
Write-Host "`n[3/5] Configuring with CMake..." -ForegroundColor Yellow

$OpenCVFlag = if ($SkipOpenCV) { "OFF" } else { "ON" }

$CMakeArgs = @(
    "..",
    "-G", "MinGW Makefiles",
    "-DENABLE_FFMPEG=ON",
    "-DENABLE_OPENCV=$OpenCVFlag",
    "-DCMAKE_BUILD_TYPE=$BuildType",
    "-DCMAKE_CXX_COMPILER=g++",
    "-DCMAKE_C_COMPILER=gcc"
)

Write-Host "  Running: cmake "
Set-Location $BuildDir

$cmakeOutput = cmake $CMakeArgs 2>&1 | Out-String

if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR CMake configuration failed!" -ForegroundColor Red
    Write-Host $cmakeOutput
    exit 1
}

# РџР°СЂСЃРёРЅРі РІС‹РІРѕРґР° CMake
if ($cmakeOutput -match "FFmpeg enabled") {
    Write-Host "  OK FFmpeg enabled" -ForegroundColor Green
}
if ($cmakeOutput -match "OpenCV enabled") {
    Write-Host "  OK OpenCV enabled" -ForegroundColor Green
} elseif ($cmakeOutput -match "OpenCV not found" -or $SkipOpenCV) {
    Write-Host "  WARN OpenCV disabled (not found or skipped)" -ForegroundColor Yellow
}

Write-Host "  OK CMake configuration completed" -ForegroundColor Green

# РЎР±РѕСЂРєР°
Write-Host "`n[4/5] Building native library..." -ForegroundColor Yellow

$buildOutput = cmake --build . --config $BuildType -j8 2>&1 | Out-String

if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR Build failed!" -ForegroundColor Red
    
    # РџР°СЂСЃРёРЅРі РѕС€РёР±РѕРє
    $errors = $buildOutput | Select-String "error:"
    if ($errors) {
        Write-Host "`nErrors found:" -ForegroundColor Red
        $errors | Select-Object -First 10 | ForEach-Object {
            Write-Host "  $($_.Line.Trim())" -ForegroundColor Red
        }
    }
    
    Write-Host "`nFull output saved to diagnostics folder" -ForegroundColor Yellow
    
    # РЎРѕС…СЂР°РЅРµРЅРёРµ Р»РѕРіР°
    $logDir = Join-Path $RootDir "diagnostics"
    if (-not (Test-Path $logDir)) {
        New-Item -ItemType Directory -Path $logDir | Out-Null
    }
    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $logFile = Join-Path $logDir "native-build-$timestamp.log"
    $buildOutput | Out-File -FilePath $logFile -Encoding UTF8
    Write-Host "Log saved to: $logFile" -ForegroundColor Gray
    
    exit 1
}

Write-Host "  OK Build completed successfully" -ForegroundColor Green

# РџСЂРѕРІРµСЂРєР° СЌРєСЃРїРѕСЂС‚Р° СЃРёРјРІРѕР»РѕРІ
Write-Host "`n[5/5] Verifying exported symbols..." -ForegroundColor Yellow

$LibraryPath = Join-Path $BuildDir "bin\windows\x64\video_processing.dll"

if (Test-Path $LibraryPath) {
    Write-Host "  OK Library found: $LibraryPath" -ForegroundColor Green
    
    # РџСЂРѕРІРµСЂРєР° СЌРєСЃРїРѕСЂС‚РёСЂРѕРІР°РЅРЅС‹С… СЃРёРјРІРѕР»РѕРІ
    Write-Host "  Checking rtsp_client symbols..." -ForegroundColor Gray
    
    $symbols = dumpbin /EXPORTS $LibraryPath 2>&1 | Out-String
    $rtspSymbols = $symbols | Select-String "rtsp_client"
    
    if ($rtspSymbols) {
        Write-Host "  OK Found $($rtspSymbols.Count) rtsp_client symbols" -ForegroundColor Green
        $rtspSymbols | Select-Object -First 10 | ForEach-Object {
            Write-Host "    $($_.Line.Trim())" -ForegroundColor Gray
        }
    } else {
        Write-Host "  WARN No rtsp_client symbols found!" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ERROR Library not found at expected path!" -ForegroundColor Red
    Write-Host "  Searching for library..." -ForegroundColor Gray
    
    $foundLibs = Get-ChildItem -Path $BuildDir -Recurse -Filter "video_processing.dll" -ErrorAction SilentlyContinue
    if ($foundLibs) {
        Write-Host "  Found at: $($foundLibs[0].FullName)" -ForegroundColor Green
        $LibraryPath = $foundLibs[0].FullName
    } else {
        Write-Host "  ERROR Library not found anywhere!" -ForegroundColor Red
        exit 1
    }
}

# РС‚РѕРі
Write-Host "`n==========================================" -ForegroundColor Cyan
Write-Host "Build Summary" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Platform: $Platform" -ForegroundColor Gray
Write-Host "Build Type: $BuildType" -ForegroundColor Gray
Write-Host "FFmpeg: Enabled" -ForegroundColor Gray
Write-Host "OpenCV: $OpenCVFlag" -ForegroundColor Gray
Write-Host "Library: $LibraryPath" -ForegroundColor Gray
Write-Host "Status: SUCCESS" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan

exit 0
