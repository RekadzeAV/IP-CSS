# PowerShell скрипт для компиляции нативных библиотек для Android на Windows
# Использование: .\scripts\build-android-native-libs.ps1 [arch]
# Архитектуры: armeabi-v7a, arm64-v8a, x86, x86_64, all
#
# Требования:
# - Android NDK (установлен и ANDROID_NDK_HOME установлен)
# - CMake 3.15+
# - MinGW или Visual Studio Build Tools

param(
    [switch]$ShowHelp,
    [Parameter(Position = 0)]
    [ValidateSet("armeabi-v7a", "arm64-v8a", "x86", "x86_64", "all")]
    [string]$Arch = "all"
)

if ($ShowHelp) {
    Write-Host "Build native libraries for Android (NDK + CMake) into native/build/android."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\build-android-native-libs.ps1 -ShowHelp"
    Write-Host "  .\scripts\build-android-native-libs.ps1 [[-Arch] armeabi-v7a|arm64-v8a|x86|x86_64|all]"
    Write-Host ""
    Write-Host "Requires: ANDROID_NDK_HOME or NDK_HOME, CMake on PATH."
    Write-Host "Exit codes: 0 success, 1 missing NDK/Cmake or build failure"
    exit 0
}

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$NativeDir = Join-Path $ProjectRoot "native"
$BuildDir = Join-Path $NativeDir "build\android"

Write-Host "🔨 Building native libraries for Android" -ForegroundColor Cyan
Write-Host "Architecture: $Arch" -ForegroundColor Gray
Write-Host "Native directory: $NativeDir" -ForegroundColor Gray
Write-Host "Build directory: $BuildDir" -ForegroundColor Gray

# Определение NDK пути
if (-not $env:ANDROID_NDK_HOME -and -not $env:NDK_HOME) {
    # Попытка найти NDK в стандартных местах
    $ndkPath = "$env:LOCALAPPDATA\Android\Sdk\ndk"
    if (Test-Path $ndkPath) {
        $ndkVersions = Get-ChildItem $ndkPath -Directory | Sort-Object Name -Descending
        if ($ndkVersions.Count -gt 0) {
            $env:ANDROID_NDK_HOME = $ndkVersions[0].FullName
            Write-Host "Found NDK at: $($env:ANDROID_NDK_HOME)" -ForegroundColor Green
        }
    }

    if (-not $env:ANDROID_NDK_HOME) {
        Write-Host "❌ Android NDK not found. Please set ANDROID_NDK_HOME or NDK_HOME" -ForegroundColor Red
        Write-Host "   Or install NDK via Android Studio SDK Manager" -ForegroundColor Yellow
        exit 1
    }
} else {
    $env:ANDROID_NDK_HOME = if ($env:ANDROID_NDK_HOME) { $env:ANDROID_NDK_HOME } else { $env:NDK_HOME }
    Write-Host "Using NDK at: $($env:ANDROID_NDK_HOME)" -ForegroundColor Green
}

$NdkPath = $env:ANDROID_NDK_HOME

# Проверка CMake
$cmakePath = Get-Command cmake -ErrorAction SilentlyContinue
if (-not $cmakePath) {
    Write-Host "❌ CMake is not installed or not in PATH" -ForegroundColor Red
    Write-Host "   Please install CMake from https://cmake.org/download/" -ForegroundColor Yellow
    exit 1
}
Write-Host "✅ CMake found: $($cmakePath.Version)" -ForegroundColor Green

# Функция для сборки для конкретной архитектуры
function Build-ForArch {
    param(
        [string]$Arch,
        [string]$Abi,
        [string]$Toolchain
    )

    Write-Host ""
    Write-Host "📦 Building for Android $Arch ($Abi)..." -ForegroundColor Cyan

    $BuildDirArch = Join-Path $BuildDir $Arch
    New-Item -ItemType Directory -Force -Path $BuildDirArch | Out-Null

    Push-Location $BuildDirArch

    try {
        # Настройка toolchain
        $ToolchainFile = Join-Path $NdkPath "build\cmake\android.toolchain.cmake"
        if (-not (Test-Path $ToolchainFile)) {
            Write-Host "❌ Android toolchain file not found: $ToolchainFile" -ForegroundColor Red
            Write-Host "   Please check your NDK installation" -ForegroundColor Yellow
            exit 1
        }

        # CMake конфигурация для Android
        Write-Host "Configuring CMake..." -ForegroundColor Gray
        $cmakeArgs = @(
            $NativeDir,
            "-DCMAKE_BUILD_TYPE=Release",
            "-DCMAKE_TOOLCHAIN_FILE=$ToolchainFile",
            "-DANDROID_ABI=$Abi",
            "-DANDROID_PLATFORM=android-21",
            "-DANDROID_STL=c++_shared",
            "-DENABLE_FFMPEG=OFF",
            "-DENABLE_OPENCV=OFF",
            "-DENABLE_TENSORFLOW=OFF",
            "-DCMAKE_INSTALL_PREFIX=$BuildDirArch\install"
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
        $libExt = "so"
        $libPrefix = "lib"

        $libs = @(
            @{Name = "video_processing"; Source = "video-processing\${libPrefix}video_processing.${libExt}"; Dest = "video-processing\lib\android\$Arch"},
            @{Name = "analytics"; Source = "analytics\${libPrefix}analytics.${libExt}"; Dest = "analytics\lib\android\$Arch"},
            @{Name = "codecs"; Source = "codecs\${libPrefix}codecs.${libExt}"; Dest = "codecs\lib\android\$Arch"}
        )

        foreach ($lib in $libs) {
            $srcPath = Join-Path $BuildDirArch $lib.Source
            $destPath = Join-Path $NativeDir $lib.Dest
            New-Item -ItemType Directory -Force -Path $destPath | Out-Null

            if (Test-Path $srcPath) {
                Copy-Item $srcPath $destPath -Force
                Write-Host "  ✅ Copied $($lib.Name).${libExt}" -ForegroundColor Green
            } else {
                Write-Host "  ⚠️  Not found: $($lib.Source)" -ForegroundColor Yellow
            }
        }

        Write-Host "✅ Android $Arch build completed" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ Build failed: $_" -ForegroundColor Red
        exit 1
    }
    finally {
        Pop-Location
    }
}

# Создание директорий
New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null

# Сборка
switch ($Arch) {
    "armeabi-v7a" {
        Build-ForArch "armeabi-v7a" "armeabi-v7a" "arm-linux-androideabi"
    }
    "arm64-v8a" {
        Build-ForArch "arm64-v8a" "arm64-v8a" "aarch64-linux-android"
    }
    "x86" {
        Build-ForArch "x86" "x86" "i686-linux-android"
    }
    "x86_64" {
        Build-ForArch "x86_64" "x86_64" "x86_64-linux-android"
    }
    "all" {
        Build-ForArch "armeabi-v7a" "armeabi-v7a" "arm-linux-androideabi"
        Build-ForArch "arm64-v8a" "arm64-v8a" "aarch64-linux-android"
        Build-ForArch "x86" "x86" "i686-linux-android"
        Build-ForArch "x86_64" "x86_64" "x86_64-linux-android"
    }
    default {
        Write-Host "❌ Unknown architecture: $Arch" -ForegroundColor Red
        Write-Host "Usage: .\build-android-native-libs.ps1 [armeabi-v7a|arm64-v8a|x86|x86_64|all]" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""
Write-Host "✨ Android build completed successfully!" -ForegroundColor Green



