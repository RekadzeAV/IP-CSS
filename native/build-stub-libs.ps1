# Создание заглушек библиотек для тестирования интеграции
# Это временное решение для проверки работы cinterop

$ErrorActionPreference = "Stop"

$NativeDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$LibDirs = @(
    "video-processing\lib\windows\x64",
    "analytics\lib\windows\x64",
    "codecs\lib\windows\x64"
)

Write-Host "Creating stub libraries for integration testing..." -ForegroundColor Cyan

foreach ($libDir in $LibDirs) {
    $fullPath = Join-Path $NativeDir $libDir
    New-Item -ItemType Directory -Force -Path $fullPath | Out-Null

    $libName = Split-Path (Split-Path $libDir -Parent) -Leaf
    $dllPath = Join-Path $fullPath "lib$libName.dll"

    # Создаем простую заглушку DLL
    # В реальности здесь должен быть скомпилированный файл
    Write-Host "  Placeholder: $dllPath" -ForegroundColor Gray
}

Write-Host "`nNote: These are placeholders. Real libraries need to be compiled with:" -ForegroundColor Yellow
Write-Host "  - CMake and MinGW/MSVC compiler" -ForegroundColor Yellow
Write-Host "  - Proper handling of paths with non-ASCII characters" -ForegroundColor Yellow
Write-Host "  - Or build on Linux/macOS where path encoding is not an issue" -ForegroundColor Yellow

