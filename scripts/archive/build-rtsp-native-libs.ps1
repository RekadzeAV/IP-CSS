# Скрипт для сборки нативной библиотеки video_processing с RTSP клиентом для всех платформ
# Использование: .\scripts\build-rtsp-native-libs.ps1 [platform] [architecture]
# Платформы: windows, linux, macos, android, ios, all
# Архитектуры: x64, arm64, x86 (зависит от платформы)

param(
    [switch]$ShowHelp,

    [Parameter(Position=0)]
    [ValidateSet("windows", "linux", "macos", "android", "ios", "all")]
    [string]$Platform = "windows",

    [Parameter(Position=1)]
    [ValidateSet("x64", "arm64", "x86", "arm64-v8a", "armeabi-v7a", "simulator-arm64")]
    [string]$Architecture = "x64",

    [Parameter(Position=2)]
    [ValidateSet("Release", "Debug")]
    [string]$BuildType = "Release"
)

if ($ShowHelp) {
    Write-Host "Build native video_processing (RTSP) under native/video-processing via CMake."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\build-rtsp-native-libs.ps1 -ShowHelp"
    Write-Host "  .\scripts\build-rtsp-native-libs.ps1 [[-Platform] windows|linux|macos|android|ios|all] [[-Architecture] ...] [[-BuildType] Release|Debug]"
    Write-Host ""
    Write-Host "Exit codes: 0 success, 1 missing tool or build failure (see script output)."
    exit 0
}

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$NativeDir = Join-Path $ProjectRoot "native\video-processing"
$BuildDir = Join-Path $NativeDir "build"

Write-Host "🔨 Building RTSP native library (video_processing)" -ForegroundColor Cyan
Write-Host "Platform: $Platform" -ForegroundColor Gray
Write-Host "Architecture: $Architecture" -ForegroundColor Gray
Write-Host "Build Type: $BuildType" -ForegroundColor Gray
Write-Host "Native directory: $NativeDir" -ForegroundColor Gray

# Проверка зависимостей
function Check-Dependencies {
    Write-Host ""
    Write-Host "🔍 Checking dependencies..." -ForegroundColor Yellow

    # Проверка CMake
    try {
        $cmakeVersion = cmake --version 2>&1 | Select-Object -First 1
        Write-Host "✅ CMake found: $cmakeVersion" -ForegroundColor Green
    } catch {
        Write-Host "❌ CMake is not installed. Please install CMake (minimum version 3.15)." -ForegroundColor Red
        Write-Host "   Download from: https://cmake.org/download/" -ForegroundColor Yellow
        exit 1
    }

    # Проверка компилятора
    $compilerFound = $false
    $compilerType = ""

    if (Get-Command cl.exe -ErrorAction SilentlyContinue) {
        Write-Host "✅ MSVC compiler found" -ForegroundColor Green
        $compilerFound = $true
        $compilerType = "msvc"
    } elseif (Get-Command g++.exe -ErrorAction SilentlyContinue) {
        Write-Host "✅ MinGW g++ compiler found" -ForegroundColor Green
        $compilerFound = $true
        $compilerType = "mingw"
    } elseif (Get-Command clang++.exe -ErrorAction SilentlyContinue) {
        Write-Host "✅ Clang++ compiler found" -ForegroundColor Green
        $compilerFound = $true
        $compilerType = "clang"
    }

    if (-not $compilerFound) {
        Write-Host "⚠️  No C++ compiler found in PATH" -ForegroundColor Yellow
        Write-Host "   Install one of:" -ForegroundColor Yellow
        Write-Host "   - Visual Studio Build Tools: https://visualstudio.microsoft.com/downloads/" -ForegroundColor Yellow
        Write-Host "   - MinGW-w64: https://www.mingw-w64.org/" -ForegroundColor Yellow
        Write-Host "   - LLVM/Clang: https://llvm.org/builds/" -ForegroundColor Yellow
        Write-Host "   Continuing anyway (may fail during build)..." -ForegroundColor Yellow
    }

    # Проверка FFmpeg (опционально, но рекомендуется)
    $ffmpegFound = $false
    $ffmpegPaths = @(
        "C:\ffmpeg",
        "C:\Program Files\ffmpeg",
        "$env:ProgramFiles\ffmpeg",
        "$env:LOCALAPPDATA\ffmpeg",
        "$env:FFMPEG_DIR"
    )

    foreach ($path in $ffmpegPaths) {
        if ($path -and (Test-Path "$path\include\libavformat\avformat.h")) {
            Write-Host "✅ FFmpeg found: $path" -ForegroundColor Green
            $ffmpegFound = $true
            $script:FFMPEG_DIR = $path
            break
        }
    }

    if (-not $ffmpegFound) {
        Write-Host "⚠️  FFmpeg not found (optional but recommended)" -ForegroundColor Yellow
        Write-Host "   RTSP client will work but with limited functionality" -ForegroundColor Yellow
        Write-Host "   Install FFmpeg:" -ForegroundColor Yellow
        Write-Host "   - Chocolatey: choco install ffmpeg" -ForegroundColor Yellow
        Write-Host "   - Or download from: https://ffmpeg.org/download.html" -ForegroundColor Yellow
        Write-Host "   - Extract to C:\ffmpeg" -ForegroundColor Yellow
    }

    return @{
        CompilerFound = $compilerFound
        CompilerType = $compilerType
        FFmpegFound = $ffmpegFound
    }
}

# Сборка для Windows
function Build-Windows {
    param(
        [string]$Arch,
        [string]$BuildType
    )

    Write-Host ""
    Write-Host "📦 Building for Windows ($Arch)..." -ForegroundColor Cyan

    $WindowsBuild = Join-Path $BuildDir "windows-$Arch"
    New-Item -ItemType Directory -Force -Path $WindowsBuild | Out-Null

    Push-Location $WindowsBuild

    try {
        # Настройка CMake
        $cmakeArgs = @(
            "`"$NativeDir`"",
            "-DCMAKE_BUILD_TYPE=$BuildType",
            "-DCMAKE_INSTALL_PREFIX=`"$WindowsBuild\install`"",
            "-DENABLE_FFMPEG=$(if ($script:FFMPEG_DIR) { 'ON' } else { 'OFF' })",
            "-DENABLE_OPENCV=OFF"
        )

        # Добавляем путь к FFmpeg, если найден
        if ($script:FFMPEG_DIR) {
            $cmakeArgs += "-DFFMPEG_DIR=`"$script:FFMPEG_DIR`""
        }

        # Определение генератора
        if ($script:CompilerType -eq "msvc") {
            $cmakeArgs += "-G", "Visual Studio 17 2022"
            $cmakeArgs += "-A", "x64"
            Write-Host "   Using Visual Studio generator" -ForegroundColor Gray
        } elseif ($script:CompilerType -eq "mingw") {
            $cmakeArgs += "-G", "MinGW Makefiles"
            Write-Host "   Using MinGW generator" -ForegroundColor Gray
        } else {
            Write-Host "   Using default generator" -ForegroundColor Gray
        }

        Write-Host "   Running CMake..." -ForegroundColor Gray
        & cmake @cmakeArgs

        if ($LASTEXITCODE -ne 0) {
            throw "CMake configuration failed"
        }

        # Сборка
        Write-Host "   Building..." -ForegroundColor Gray
        if ($script:CompilerType -eq "msvc") {
            & cmake --build . --config $BuildType --parallel
        } else {
            $cpuCount = (Get-CimInstance Win32_ComputerSystem).NumberOfLogicalProcessors
            & cmake --build . --config $BuildType -j $cpuCount
        }

        if ($LASTEXITCODE -ne 0) {
            throw "Build failed"
        }

        # Копирование библиотеки
        $LibDir = Join-Path $NativeDir "lib\windows\$Arch"
        New-Item -ItemType Directory -Force -Path $LibDir | Out-Null

        $dllName = "video_processing.dll"
        $libName = "video_processing.lib"

        # Поиск библиотеки
        $dllPath = $null
        $libPath = $null

        if (Test-Path $dllName) {
            $dllPath = (Resolve-Path $dllName).Path
        } else {
            $foundDll = Get-ChildItem -Path $WindowsBuild -Recurse -Filter $dllName -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($foundDll) {
                $dllPath = $foundDll.FullName
            }
        }

        if ($script:CompilerType -eq "msvc") {
            if (Test-Path $libName) {
                $libPath = (Resolve-Path $libName).Path
            } else {
                $foundLib = Get-ChildItem -Path $WindowsBuild -Recurse -Filter $libName -ErrorAction SilentlyContinue | Select-Object -First 1
                if ($foundLib) {
                    $libPath = $foundLib.FullName
                }
            }
        }

        if ($dllPath) {
            Copy-Item $dllPath (Join-Path $LibDir $dllName) -Force
            Write-Host "✅ Windows ($Arch) build completed" -ForegroundColor Green
            Write-Host "   Library: $LibDir\$dllName" -ForegroundColor Gray

            if ($libPath) {
                Copy-Item $libPath (Join-Path $LibDir $libName) -Force
                Write-Host "   Import library: $LibDir\$libName" -ForegroundColor Gray
            }

            # Показываем размер файла
            $fileInfo = Get-Item (Join-Path $LibDir $dllName)
            Write-Host "   Size: $([math]::Round($fileInfo.Length / 1MB, 2)) MB" -ForegroundColor Gray
        } else {
            Write-Host "⚠️  Library file not found" -ForegroundColor Yellow
            Write-Host "   Searched in: $WindowsBuild" -ForegroundColor Gray
            Write-Host "   Build may have failed or library is in unexpected location" -ForegroundColor Yellow
        }

    } catch {
        Write-Host "❌ Build failed: $_" -ForegroundColor Red
        Write-Host "   Error details:" -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
        exit 1
    } finally {
        Pop-Location
    }
}

# Создание директорий
New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $NativeDir "lib\windows\x64") | Out-Null

# Проверка зависимостей
$deps = Check-Dependencies
$script:CompilerType = $deps.CompilerType

# Сборка
if ($Platform -eq "windows" -or $Platform -eq "all") {
    Build-Windows -Arch $Architecture -BuildType $BuildType
}

Write-Host ""
Write-Host "✨ Build process completed!" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Next steps:" -ForegroundColor Cyan
Write-Host "   1. Verify library: Test-Path `"$NativeDir\lib\windows\$Architecture\video_processing.dll`"" -ForegroundColor Gray
Write-Host "   2. Test Kotlin integration: .\gradlew.bat :core:network:compileKotlinNativeWindows" -ForegroundColor Gray
Write-Host "   3. Check build logs if there were warnings" -ForegroundColor Gray
