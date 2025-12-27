# PowerShell скрипт для компиляции всех нативных библиотек на Windows
# Использование: .\scripts\build-all-native-libs.ps1

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$NativeDir = Join-Path $ProjectRoot "native"
$BuildDir = Join-Path $NativeDir "build"

Write-Host "🔨 Building all native libraries" -ForegroundColor Cyan
Write-Host "Native directory: $NativeDir" -ForegroundColor Gray
Write-Host "Build directory: $BuildDir" -ForegroundColor Gray

# Проверка зависимостей
function Check-Dependencies {
    Write-Host ""
    Write-Host "🔍 Checking dependencies..." -ForegroundColor Yellow

    # Проверка CMake
    $cmakePath = Get-Command cmake -ErrorAction SilentlyContinue
    if (-not $cmakePath) {
        Write-Host "❌ CMake is not installed or not in PATH" -ForegroundColor Red
        Write-Host "   Please install CMake from https://cmake.org/download/" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "✅ CMake found: $($cmakePath.Version)" -ForegroundColor Green

    # Проверка компилятора
    $gccPath = Get-Command gcc -ErrorAction SilentlyContinue
    $clPath = Get-Command cl -ErrorAction SilentlyContinue
    if (-not $gccPath -and -not $clPath) {
        Write-Host "⚠️  No C++ compiler found (gcc or cl)" -ForegroundColor Yellow
        Write-Host "   Install MinGW-w64 or Visual Studio Build Tools" -ForegroundColor Yellow
    } else {
        if ($gccPath) {
            Write-Host "✅ GCC found: $($gccPath.Source)" -ForegroundColor Green
        }
        if ($clPath) {
            Write-Host "✅ MSVC found: $($clPath.Source)" -ForegroundColor Green
        }
    }

    # Проверка FFmpeg (опционально)
    $ffmpegPath = Get-Command ffmpeg -ErrorAction SilentlyContinue
    if (-not $ffmpegPath) {
        Write-Host "⚠️  FFmpeg not found in PATH (optional)" -ForegroundColor Yellow
        Write-Host "   Download from https://ffmpeg.org/download.html" -ForegroundColor Yellow
    } else {
        Write-Host "✅ FFmpeg found" -ForegroundColor Green
    }
}

# Создание директорий
function Create-Directories {
    Write-Host ""
    Write-Host "📁 Creating directories..." -ForegroundColor Yellow

    $libDirs = @(
        "video-processing\lib\windows\x64",
        "analytics\lib\windows\x64",
        "codecs\lib\windows\x64"
    )

    foreach ($dir in $libDirs) {
        $fullPath = Join-Path $NativeDir $dir
        New-Item -ItemType Directory -Force -Path $fullPath | Out-Null
    }

    New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
    Write-Host "✅ Directories created" -ForegroundColor Green
}

# Сборка для Windows
function Build-Windows {
    Write-Host ""
    Write-Host "📦 Building for Windows..." -ForegroundColor Cyan

    $WindowsBuild = Join-Path $BuildDir "windows"
    New-Item -ItemType Directory -Force -Path $WindowsBuild | Out-Null

    Push-Location $WindowsBuild

    try {
        # Конфигурация CMake
        Write-Host "Configuring CMake..." -ForegroundColor Gray
        $cmakeArgs = @(
            $NativeDir,
            "-DCMAKE_BUILD_TYPE=Release",
            "-DENABLE_FFMPEG=ON",
            "-DENABLE_OPENCV=ON"
        )

        # Определение генератора
        if (Get-Command ninja -ErrorAction SilentlyContinue) {
            $cmakeArgs += "-G", "Ninja"
            Write-Host "Using Ninja generator" -ForegroundColor Gray
        } elseif (Get-Command mingw32-make -ErrorAction SilentlyContinue) {
            $cmakeArgs += "-G", "MinGW Makefiles"
            Write-Host "Using MinGW Makefiles generator" -ForegroundColor Gray
        } else {
            $cmakeArgs += "-G", "Visual Studio 17 2022", "-A", "x64"
            Write-Host "Using Visual Studio generator" -ForegroundColor Gray
        }

        & cmake @cmakeArgs
        if ($LASTEXITCODE -ne 0) {
            throw "CMake configuration failed"
        }

        # Сборка
        Write-Host "Building..." -ForegroundColor Gray
        & cmake --build . --config Release
        if ($LASTEXITCODE -ne 0) {
            throw "Build failed"
        }

        # Копирование библиотек
        Write-Host "Copying libraries..." -ForegroundColor Gray
        $libs = @(
            @{Source = "video-processing\libvideo_processing.dll"; Dest = "video-processing\lib\windows\x64"},
            @{Source = "analytics\libanalytics.dll"; Dest = "analytics\lib\windows\x64"},
            @{Source = "codecs\libcodecs.dll"; Dest = "codecs\lib\windows\x64"}
        )

        foreach ($lib in $libs) {
            $srcPath = Join-Path $WindowsBuild $lib.Source
            $destPath = Join-Path $NativeDir $lib.Dest
            if (Test-Path $srcPath) {
                Copy-Item $srcPath $destPath -Force
                Write-Host "  ✅ Copied $($lib.Source)" -ForegroundColor Green
            } else {
                Write-Host "  ⚠️  Not found: $($lib.Source)" -ForegroundColor Yellow
            }
        }

        Write-Host "✅ Windows build completed" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ Build failed: $_" -ForegroundColor Red
        exit 1
    }
    finally {
        Pop-Location
    }
}

# Основная логика
Check-Dependencies
Create-Directories
Build-Windows

Write-Host ""
Write-Host "✨ Build completed successfully!" -ForegroundColor Green

