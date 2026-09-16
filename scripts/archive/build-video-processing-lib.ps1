# Build native video_processing library on Windows.
# Usage: .\scripts\build-video-processing-lib.ps1

param(
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Configure and build native/video_processing (CMake, FFmpeg, MSVC or MinGW)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\build-video-processing-lib.ps1 -ShowHelp"
    Write-Host "  .\scripts\build-video-processing-lib.ps1"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Build finished (see script output)"
    Write-Host "  1  Missing CMake or fatal build step"
    exit 0
}

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$NativeDir = Join-Path $ProjectRoot "native\video-processing"
$BuildDir = Join-Path $NativeDir "build"

Write-Host "Building native video_processing library for Windows" -ForegroundColor Cyan
Write-Host "Native directory: $NativeDir"
Write-Host "Build directory: $BuildDir"

# Dependency checks
function Check-Dependencies {
    Write-Host ""
    Write-Host "Checking dependencies..." -ForegroundColor Yellow

    # Check CMake
    try {
        $cmakeVersion = cmake --version 2>&1 | Select-Object -First 1
        Write-Host "CMake found: $cmakeVersion" -ForegroundColor Green
    } catch {
        Write-Host "CMake is not installed. Please install CMake (minimum version 3.15)." -ForegroundColor Red
        Write-Host "   Download from: https://cmake.org/download/" -ForegroundColor Yellow
        exit 1
    }

    # Check C++ compiler
    $compilerFound = $false

    # Check MSVC
    if (Get-Command cl.exe -ErrorAction SilentlyContinue) {
        Write-Host "MSVC compiler found" -ForegroundColor Green
        $compilerFound = $true
    }
    # Check MinGW
    elseif (Get-Command g++.exe -ErrorAction SilentlyContinue) {
        Write-Host "MinGW g++ compiler found" -ForegroundColor Green
        $compilerFound = $true
    }

    if (-not $compilerFound) {
        Write-Host "No C++ compiler found. Install Visual Studio or MinGW." -ForegroundColor Yellow
        Write-Host "   Visual Studio: https://visualstudio.microsoft.com/" -ForegroundColor Yellow
        Write-Host "   MinGW: https://www.mingw-w64.org/" -ForegroundColor Yellow
    }

    # Check FFmpeg
    $ffmpegFound = $false

    # Check via pkg-config (if available)
    if (Get-Command pkg-config -ErrorAction SilentlyContinue) {
        try {
            $null = pkg-config --exists libavformat libavcodec libavutil libswscale 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "FFmpeg found via pkg-config" -ForegroundColor Green
                $ffmpegFound = $true
            }
        } catch {
            # pkg-config may not work on Windows
        }
    }

    # Check standard install paths
    if (-not $ffmpegFound) {
        $ffmpegPaths = @(
            "C:\ffmpeg",
            "C:\Program Files\ffmpeg",
            "$env:ProgramFiles\ffmpeg",
            "$env:LOCALAPPDATA\ffmpeg"
        )

        foreach ($path in $ffmpegPaths) {
            if (Test-Path "$path\include\libavformat\avformat.h") {
                Write-Host "FFmpeg found: $path" -ForegroundColor Green
                $ffmpegFound = $true
                break
            }
        }
    }

    if (-not $ffmpegFound) {
        Write-Host "FFmpeg not found" -ForegroundColor Yellow
        Write-Host "   Install FFmpeg:" -ForegroundColor Yellow
        Write-Host "   - Chocolatey: choco install ffmpeg" -ForegroundColor Yellow
        Write-Host "   - Or download from: https://ffmpeg.org/download.html" -ForegroundColor Yellow
        Write-Host "   - Extract to C:\ffmpeg" -ForegroundColor Yellow
        Write-Host "   Continuing anyway (library may not work without FFmpeg)..." -ForegroundColor Yellow
    }

    # Check OpenCV (optional)
    $opencvFound = $false
    $opencvPaths = @(
        "$env:OPENCV_DIR",
        "C:\opencv",
        "C:\Program Files\opencv"
    )

    foreach ($path in $opencvPaths) {
        if ($path -and (Test-Path "$path\include\opencv2\opencv.hpp")) {
            Write-Host "OpenCV found: $path" -ForegroundColor Green
            $opencvFound = $true
            break
        }
    }

    if (-not $opencvFound) {
        Write-Host "OpenCV not found (optional)" -ForegroundColor Yellow
    }

    # Check JNI for Desktop wrapper (optional)
    if ($env:JAVA_HOME) {
        Write-Host "JAVA_HOME found: $env:JAVA_HOME" -ForegroundColor Green
        $jniHeaderPath = Join-Path $env:JAVA_HOME "include\jni.h"
        if (Test-Path $jniHeaderPath) {
            Write-Host "JNI headers found" -ForegroundColor Green
        } else {
            Write-Host "JNI headers not found in JAVA_HOME (Desktop JNI wrapper may not compile)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "JAVA_HOME not set (Desktop JNI wrapper will not be compiled)" -ForegroundColor Yellow
        Write-Host "   Set JAVA_HOME to enable Desktop JNI support" -ForegroundColor Yellow
    }
}

# Build library
function Build-Library {
    Write-Host ""
    Write-Host "Building library..." -ForegroundColor Cyan

    $WindowsBuild = Join-Path $BuildDir "windows-x64"
    New-Item -ItemType Directory -Force -Path $WindowsBuild | Out-Null

    Push-Location $WindowsBuild

    try {
        # Configure CMake
        $opencvAvailable =
            ($env:OPENCV_DIR -and (Test-Path (Join-Path $env:OPENCV_DIR "OpenCVConfig.cmake"))) -or
            (Test-Path "C:\opencv\build\OpenCVConfig.cmake") -or
            (Test-Path "C:\Program Files\opencv\build\OpenCVConfig.cmake")

        $enableOpenCv = "OFF"
        if ($opencvAvailable) {
            $enableOpenCv = "ON"
        }

        $cmakeArgs = @(
            "`"$NativeDir`"",
            "-DCMAKE_BUILD_TYPE=Release",
            "-DCMAKE_INSTALL_PREFIX=`"$WindowsBuild\install`"",
            "-DENABLE_FFMPEG=ON",
            "-DENABLE_OPENCV=$enableOpenCv"
        )

        # Add JNI if available
        if ($env:JAVA_HOME) {
            $cmakeArgs += "-DJAVA_HOME=`"$env:JAVA_HOME`""
            Write-Host "   Enabling Desktop JNI support" -ForegroundColor Gray
        }

        # Select generator
        $generator = $null
        if (Get-Command cl.exe -ErrorAction SilentlyContinue) {
            $generator = "Visual Studio 17 2022"
            $cmakeArgs += "-A", "x64"
            Write-Host "   Using Visual Studio generator" -ForegroundColor Gray
        } elseif (Get-Command g++.exe -ErrorAction SilentlyContinue) {
            $generator = "MinGW Makefiles"
            Write-Host "   Using MinGW generator" -ForegroundColor Gray
        } else {
            Write-Host "No generator specified, using default" -ForegroundColor Yellow
        }

        Write-Host "   Running CMake..." -ForegroundColor Gray
        if (Test-Path (Join-Path $WindowsBuild "CMakeCache.txt")) {
            Remove-Item (Join-Path $WindowsBuild "CMakeCache.txt") -Force
        }
        if (Test-Path (Join-Path $WindowsBuild "CMakeFiles")) {
            Remove-Item (Join-Path $WindowsBuild "CMakeFiles") -Recurse -Force
        }
        if ($generator) {
            & cmake "-G" $generator @cmakeArgs
        } else {
            & cmake @cmakeArgs
        }

        if ($LASTEXITCODE -ne 0) {
            throw "CMake configuration failed"
        }

        # Build
        Write-Host "   Building..." -ForegroundColor Gray
        & cmake --build . --config Release --parallel

        if ($LASTEXITCODE -ne 0) {
            throw "Build failed"
        }

        # Copy outputs
        $LibDir = Join-Path $NativeDir "lib\windows\x64"
        New-Item -ItemType Directory -Force -Path $LibDir | Out-Null

        $dllName = "video_processing.dll"
        $libName = "video_processing.lib"

        $dllPath = Join-Path $WindowsBuild $dllName
        $libPath = Join-Path $WindowsBuild $libName

        # Search for output in subdirectories
        if (-not (Test-Path $dllPath)) {
            $foundDll = Get-ChildItem -Path $WindowsBuild -Recurse -Filter $dllName | Select-Object -First 1
            if ($foundDll) {
                $dllPath = $foundDll.FullName
            }
        }

        if (Test-Path $dllPath) {
            Copy-Item $dllPath (Join-Path $LibDir $dllName) -Force
            Write-Host "Windows build completed" -ForegroundColor Green
            Write-Host "   Library: $LibDir\$dllName" -ForegroundColor Gray

            if (Test-Path $libPath) {
                Copy-Item $libPath (Join-Path $LibDir $libName) -Force
            }
        } else {
            Write-Host "Library file not found in expected location" -ForegroundColor Yellow
            Write-Host "   Searched in: $WindowsBuild" -ForegroundColor Gray
        }

    } finally {
        Pop-Location
    }
}

# Create directories
New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $NativeDir "lib\windows\x64") | Out-Null

# Run dependency checks
Check-Dependencies

# Run build
Build-Library

Write-Host ""
Write-Host "Build completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
$nativeLibraryPath = Join-Path $NativeDir "lib\windows\x64\video_processing.dll"
Write-Host "   1. Verify library exists: Test-Path '$nativeLibraryPath'" -ForegroundColor Gray
Write-Host "   2. Test integration: .\\gradlew.bat :core:network:compileKotlinNative" -ForegroundColor Gray
Write-Host "   3. Uncomment code in VideoDecoder.native.kt" -ForegroundColor Gray

