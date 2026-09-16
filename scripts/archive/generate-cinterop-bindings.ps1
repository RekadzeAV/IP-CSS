# Скрипт для генерации cinterop биндингов для всех платформ
# Использование: .\scripts\generate-cinterop-bindings.ps1 [--platform <platform>]

param(
    [string]$Platform = "all",
    [switch]$Help = $false,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($Help -or $ShowHelp) {
    Write-Host "Run Gradle cinterop tasks for video_processing per native target (requires built native libs)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\generate-cinterop-bindings.ps1 -ShowHelp"
    Write-Host "  .\scripts\generate-cinterop-bindings.ps1 [-Platform all|linux|macos-x64|macos-arm64|windows]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Platform, -p  Single platform or all (default all)"
    Write-Host "  -Help, -h      Show help"
    Write-Host "  -ShowHelp      Same as -Help"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Finished (per-platform skips if library missing)"
    Write-Host "  1  Unknown -Platform value"
    exit 0
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

Write-Host "=== Generating CInterop Bindings ===" -ForegroundColor Cyan
Write-Host ""

# Проверка наличия библиотек
function Test-LibraryExists {
    param([string]$Path)
    return (Test-Path $Path)
}

# Платформы и их библиотеки
$Platforms = @{
    "linux" = @{
        "target" = "nativeLinux"
        "library" = "native\video-processing\lib\linux\x64\libvideo_processing.so"
        "gradleTask" = ":core:network:generateCInteropVideoProcessingNativeLinux"
    }
    "macos-x64" = @{
        "target" = "nativeMacosX64"
        "library" = "native\video-processing\lib\macos\x64\libvideo_processing.dylib"
        "gradleTask" = ":core:network:generateCInteropVideoProcessingNativeMacosX64"
    }
    "macos-arm64" = @{
        "target" = "nativeMacosArm64"
        "library" = "native\video-processing\lib\macos\arm64\libvideo_processing.dylib"
        "gradleTask" = ":core:network:generateCInteropVideoProcessingNativeMacosArm64"
    }
    "windows" = @{
        "target" = "nativeWindows"
        "library" = "native\video-processing\lib\windows\x64\video_processing.dll"
        "gradleTask" = ":core:network:generateCInteropVideoProcessingNativeWindows"
    }
}

# Выбор платформ для генерации
$SelectedPlatforms = @()
if ($Platform -eq "all") {
    $SelectedPlatforms = $Platforms.Keys
} else {
    if ($Platforms.ContainsKey($Platform)) {
        $SelectedPlatforms = @($Platform)
    } else {
        Write-Host "ERROR: Unknown platform: $Platform" -ForegroundColor Red
        Write-Host "Available platforms: $($Platforms.Keys -join ', ')" -ForegroundColor Yellow
        exit 1
    }
}

# Проверка библиотек и генерация
foreach ($platformKey in $SelectedPlatforms) {
    $platformInfo = $Platforms[$platformKey]
    $libraryPath = Join-Path $ProjectRoot $platformInfo.library

    Write-Host "=== Platform: $platformKey ===" -ForegroundColor Yellow

    # Проверка наличия библиотеки
    if (-not (Test-LibraryExists $libraryPath)) {
        Write-Host "WARNING: Library not found: $libraryPath" -ForegroundColor Yellow
        Write-Host "  Please build the library first:" -ForegroundColor Gray
        switch ($platformKey) {
            "linux" { Write-Host "    ./scripts/build-video-processing-linux.sh" -ForegroundColor White }
            "macos-x64" { Write-Host "    ./scripts/build-video-processing-macos.sh x64" -ForegroundColor White }
            "macos-arm64" { Write-Host "    ./scripts/build-video-processing-macos.sh arm64" -ForegroundColor White }
            "windows" { Write-Host "    .\scripts\build-video-processing-lib.ps1" -ForegroundColor White }
        }
        Write-Host "  Skipping this platform..." -ForegroundColor Gray
        continue
    }

    Write-Host "Library found: $libraryPath" -ForegroundColor Green

    # Генерация биндингов через Gradle
    Write-Host "Generating cinterop bindings..." -ForegroundColor Yellow
    $gradleTask = $platformInfo.gradleTask

    try {
        Push-Location $ProjectRoot
        $result = & .\gradlew.bat $gradleTask 2>&1

        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Bindings generated successfully for $platformKey" -ForegroundColor Green
        } else {
            Write-Host "❌ Failed to generate bindings for $platformKey" -ForegroundColor Red
            Write-Host $result -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ Error generating bindings: $_" -ForegroundColor Red
    } finally {
        Pop-Location
    }

    Write-Host ""
}

Write-Host "=== Generation Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Check generated bindings in build/classes/kotlin/*/cinterop/" -ForegroundColor White
Write-Host "  2. Activate VideoDecoder.native.kt code" -ForegroundColor White
Write-Host "  3. Build the project: .\gradlew.bat :core:network:build" -ForegroundColor White
