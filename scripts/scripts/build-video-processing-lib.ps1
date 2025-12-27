# Скрипт для сборки нативной библиотеки video_processing на Windows
# Использование: .\scripts\build-video-processing-lib.ps1

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$NativeDir = Join-Path $ProjectRoot "native\video-processing"
$BuildDir = Join-Path $NativeDir "build"

Write-Host "🔨 Building native video_processing library for Windows" -ForegroundColor Cyan
Write-Host "Native directory: $NativeDir"
Write-Host "Build directory: $BuildDir"

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

    # Проверка MSVC
    if (Get-Command cl.exe -ErrorAction SilentlyContinue) {
        Write-Host "✅ MSVC compiler found" -ForegroundColor Green
        $compilerFound = $true
    }
    # Проверка MinGW
    elseif (Get-Command g++.exe -ErrorAction SilentlyContinue) {
        Write-Host "✅ MinGW g++ compiler found" -ForegroundColor Green
        $compilerFound = $true
    }

    if (-not $compilerFound) {
        Write-Host "⚠️  No C++ compiler found. Install Visual Studio or MinGW." -ForegroundColor Yellow
        Write-Host "   Visual Studio: https://visualstudio.microsoft.com/" -ForegroundColor Yellow
        Write-Host "   MinGW: https://www.mingw-w64.org/" -ForegroundColor Yellow
    }

    # Проверка FFmpeg
    $ffmpegFound = $false

    # Проверка через pkg-config (если установлен)
    if (Get-Command pkg-config -ErrorAction SilentlyContinue) {
        try {
            $null = pkg-config --exists libavformat libavcodec libavutil libswscale 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✅ FFmpeg found via pkg-config" -ForegroundColor Green
                $ffmpegFound = $true
            }
        } catch {
            # pkg-config может не работать на Windows
        }
    }

    # Проверка в стандартных местах
    if (-not $ffmpegFound) {
        $ffmpegPaths = @(
            "C:\ffmpeg",
            "C:\Program Files\ffmpeg",
            "$env:ProgramFiles\ffmpeg",
            "$env:LOCALAPPDATA\ffmpeg"
        )

        foreach ($path in $ffmpegPaths) {
            if (Test-Path "$path\include\libavformat\avformat.h") {
                Write-Host "✅ FFmpeg found: $path" -ForegroundColor Green
                $ffmpegFound = $true
                break
            }
        }
    }

    if (-not $ffmpegFound) {
        Write-Host "⚠️  FFmpeg not found" -ForegroundColor Yellow
        Write-Host "   Install FFmpeg:" -ForegroundColor Yellow
        Write-Host "   - Chocolatey: choco install ffmpeg" -ForegroundColor Yellow
        Write-Host "   - Or download from: https://ffmpeg.org/download.html" -ForegroundColor Yellow
        Write-Host "   - Extract to C:\ffmpeg" -ForegroundColor Yellow
        Write-Host "   Continuing anyway (library may not work without FFmpeg)..." -ForegroundColor Yellow
    }

    # Проверка OpenCV (опционально)
    $opencvFound = $false
    $opencvPaths = @(
        "$env:OPENCV_DIR",
        "C:\opencv",
        "C:\Program Files\opencv"
    )

    foreach ($path in $opencvPaths) {
        if ($path -and (Test-Path "$path\include\opencv2\opencv.hpp")) {
            Write-Host "✅ OpenCV found: $path" -ForegroundColor Green
            $opencvFound = $true
            break
        }
    }

    if (-not $opencvFound) {
        Write-Host "⚠️  OpenCV not found (optional)" -ForegroundColor Yellow
    }
}

# Сборка библиотеки
function Build-Library {
    Write-Host ""
    Write-Host "📦 Building library..." -ForegroundColor Cyan

    $WindowsBuild = Join-Path $BuildDir "windows-x64"
    New-Item -ItemType Directory -Force -Path $WindowsBuild | Out-Null

    Push-Location $WindowsBuild

    try {
        # Настройка CMake
        $cmakeArgs = @(
            "`"$NativeDir`"",
            "-DCMAKE_BUILD_TYPE=Release",
            "-DCMAKE_INSTALL_PREFIX=`"$WindowsBuild\install`"",
            "-DENABLE_FFMPEG=ON",
            "-DENABLE_OPENCV=ON"
        )

        # Определение генератора
        if (Get-Command cl.exe -ErrorAction SilentlyContinue) {
            # MSVC
            $cmakeArgs += "-G", "Visual Studio 17 2022"
            $cmakeArgs += "-A", "x64"
            Write-Host "   Using Visual Studio generator" -ForegroundColor Gray
        } elseif (Get-Command g++.exe -ErrorAction SilentlyContinue) {
            # MinGW
            $cmakeArgs += "-G", "MinGW Makefiles"
            Write-Host "   Using MinGW generator" -ForegroundColor Gray
        } else {
            Write-Host "⚠️  No generator specified, using default" -ForegroundColor Yellow
        }

        Write-Host "   Running CMake..." -ForegroundColor Gray
        & cmake @cmakeArgs

        if ($LASTEXITCODE -ne 0) {
            throw "CMake configuration failed"
        }

        # Сборка
        Write-Host "   Building..." -ForegroundColor Gray
        & cmake --build . --config Release --parallel

        if ($LASTEXITCODE -ne 0) {
            throw "Build failed"
        }

        # Копирование библиотеки
        $LibDir = Join-Path $NativeDir "lib\windows\x64"
        New-Item -ItemType Directory -Force -Path $LibDir | Out-Null

        $dllName = "video_processing.dll"
        $libName = "video_processing.lib"

        $dllPath = Join-Path $WindowsBuild $dllName
        $libPath = Join-Path $WindowsBuild $libName

        # Поиск библиотеки (может быть в подпапках)
        if (-not (Test-Path $dllPath)) {
            $foundDll = Get-ChildItem -Path $WindowsBuild -Recurse -Filter $dllName | Select-Object -First 1
            if ($foundDll) {
                $dllPath = $foundDll.FullName
            }
        }

        if (Test-Path $dllPath) {
            Copy-Item $dllPath (Join-Path $LibDir $dllName) -Force
            Write-Host "✅ Windows build completed" -ForegroundColor Green
            Write-Host "   Library: $LibDir\$dllName" -ForegroundColor Gray

            if (Test-Path $libPath) {
                Copy-Item $libPath (Join-Path $LibDir $libName) -Force
            }
        } else {
            Write-Host "⚠️  Library file not found in expected location" -ForegroundColor Yellow
            Write-Host "   Searched in: $WindowsBuild" -ForegroundColor Gray
        }

    } finally {
        Pop-Location
    }
}

# Создание директорий
New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $NativeDir "lib\windows\x64") | Out-Null

# Проверка зависимостей
Check-Dependencies

# Сборка
Build-Library

Write-Host ""
Write-Host "✨ Build completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Next steps:" -ForegroundColor Cyan
Write-Host "   1. Verify library exists: Test-Path `"$NativeDir\lib\windows\x64\video_processing.dll`"" -ForegroundColor Gray
Write-Host "   2. Test integration: .\gradlew.bat :core:network:compileKotlinNative" -ForegroundColor Gray
Write-Host "   3. Uncomment code in VideoDecoder.native.kt" -ForegroundColor Gray

