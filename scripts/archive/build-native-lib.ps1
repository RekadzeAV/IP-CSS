# РЎРєСЂРёРїС‚ СЃР±РѕСЂРєРё РЅР°С‚РёРІРЅРѕР№ Р±РёР±Р»РёРѕС‚РµРєРё video_processing РґР»СЏ Windows
# РСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ: .\scripts\build-native-lib.ps1 [arch] [build_type]
# РџСЂРёРјРµСЂС‹:
#   .\scripts\build-native-lib.ps1 x64 Release
#   .\scripts\build-native-lib.ps1 x86 Debug

param(
    [string]$Arch = "x64",
    [string]$BuildType = "Release",
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Build native video_processing library (Windows) via CMake in native/video-processing"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\build-native-lib.ps1 -ShowHelp"
    Write-Host "  .\scripts\build-native-lib.ps1 [x64|x86] [Release|Debug]"
    Write-Host "  .\scripts\build-native-lib.ps1 x64 Release"
    Write-Host ""
    Write-Host "Arguments (positional):"
    Write-Host "  1  Architecture (default x64)"
    Write-Host "  2  Build type (default Release)"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Configure and build succeeded"
    Write-Host "  1  Missing toolchain, CMake failure, or build failure"
    exit 0
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$NativeDir = Join-Path $ProjectRoot "native\video-processing"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Building native library: video_processing" -ForegroundColor Cyan
Write-Host "Platform: windows" -ForegroundColor Cyan
Write-Host "Architecture: $Arch" -ForegroundColor Cyan
Write-Host "Build Type: $BuildType" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

Set-Location $NativeDir

# РЎРѕР·РґР°РЅРёРµ РґРёСЂРµРєС‚РѕСЂРёРё СЃР±РѕСЂРєРё
$BuildDir = Join-Path "build" "windows\$Arch"
New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
Set-Location $BuildDir

# РћРїСЂРµРґРµР»РµРЅРёРµ РіРµРЅРµСЂР°С‚РѕСЂР° CMake
$Generator = "Visual Studio 17 2022"
if (Get-Command cmake -ErrorAction SilentlyContinue) {
    $cmakeVersion = (cmake --version | Select-String -Pattern "version (\d+)" | ForEach-Object { $_.Matches[0].Groups[1].Value })
    if ([int]$cmakeVersion -lt 3.20) {
        $Generator = "Visual Studio 16 2019"
    }
}

# РџСЂРѕРІРµСЂРєР° РЅР°Р»РёС‡РёСЏ Visual Studio
$VSInstalled = $false
if (Test-Path "C:\Program Files\Microsoft Visual Studio\2022\Community\Common7\IDE\devenv.exe") {
    $VSInstalled = $true
} elseif (Test-Path "C:\Program Files\Microsoft Visual Studio\2022\Professional\Common7\IDE\devenv.exe") {
    $VSInstalled = $true
} elseif (Test-Path "C:\Program Files\Microsoft Visual Studio\2022\Enterprise\Common7\IDE\devenv.exe") {
    $VSInstalled = $true
}

# РСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ MinGW РµСЃР»Рё Visual Studio РЅРµ СѓСЃС‚Р°РЅРѕРІР»РµРЅ
if (-not $VSInstalled) {
    if (Get-Command mingw32-make -ErrorAction SilentlyContinue) {
        $Generator = "MinGW Makefiles"
        Write-Host "Using MinGW Makefiles generator" -ForegroundColor Yellow
    } else {
        Write-Host "Error: Visual Studio or MinGW not found" -ForegroundColor Red
        Write-Host "Please install Visual Studio 2019+ or MinGW-w64" -ForegroundColor Red
        exit 1
    }
}

# РќР°СЃС‚СЂРѕР№РєР° CMake
$cmakeArgs = @(
    "..\..\.."
    "-G", $Generator
    "-DCMAKE_BUILD_TYPE=$BuildType"
    "-DENABLE_FFMPEG=ON"
    "-DENABLE_OPENCV=ON"
)

if ($Generator -like "Visual Studio*") {
    $cmakeArgs += "-A", $Arch
}

cmake @cmakeArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "CMake configuration failed" -ForegroundColor Red
    exit 1
}

# РЎР±РѕСЂРєР°
if ($Generator -like "Visual Studio*") {
    cmake --build . --config $BuildType --parallel
} else {
    cmake --build . --config $BuildType -j $(Get-CimInstance Win32_ComputerSystem | Select-Object -ExpandProperty NumberOfLogicalProcessors)
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed" -ForegroundColor Red
    exit 1
}

# РљРѕРїРёСЂРѕРІР°РЅРёРµ Р±РёР±Р»РёРѕС‚РµРєРё
$LibOutputDir = Join-Path $ProjectRoot "native\video-processing\lib\windows\$Arch"
New-Item -ItemType Directory -Force -Path $LibOutputDir | Out-Null

$libFiles = @(
    "video_processing.dll",
    "libvideo_processing.dll",
    "$BuildType\video_processing.dll",
    "$BuildType\libvideo_processing.dll"
)

$copied = $false
foreach ($libFile in $libFiles) {
    $libPath = Join-Path (Get-Location) $libFile
    if (Test-Path $libPath) {
        Copy-Item $libPath $LibOutputDir -Force
        $destName = [System.IO.Path]::GetFileName($libPath)
        Write-Host ('Library copied to: ' + (Join-Path $LibOutputDir $destName)) -ForegroundColor Green
        $copied = $true
        break
    }
}

if (-not $copied) {
    Write-Host "Warning: Library file not found" -ForegroundColor Yellow
    Write-Host "Expected location: $BuildDir" -ForegroundColor Yellow
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Build completed successfully!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan

