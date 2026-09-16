# Generate native/compile_commands.json for clangd (CMake EXPORT_COMPILE_COMMANDS)

param(
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Generate compile_commands.json under native/ (CMake -DCMAKE_EXPORT_COMPILE_COMMANDS=ON)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\generate-compile-commands.ps1 -ShowHelp"
    Write-Host "  .\scripts\generate-compile-commands.ps1"
    Write-Host ""
    Write-Host "Prerequisites:"
    Write-Host "  cmake on PATH; run from repo (script uses native/CMakeLists.txt)"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Generated or copied compile_commands.json"
    Write-Host "  1  CMake configuration failed"
    exit 0
}

$ErrorActionPreference = "Stop"

Write-Host "Generating compile_commands.json for native C++..." -ForegroundColor Green

$projectRoot = Split-Path -Parent $PSScriptRoot
$nativeDir = Join-Path $projectRoot "native"
$buildDir = Join-Path $nativeDir "build"

if (-not (Test-Path $buildDir)) {
    New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
}

Push-Location $nativeDir

try {
    Write-Host "Running CMake (export compile commands)..." -ForegroundColor Yellow

    $env:CMAKE_EXPORT_COMPILE_COMMANDS = "ON"
    cmake -B build -DCMAKE_EXPORT_COMPILE_COMMANDS=ON

    if ($LASTEXITCODE -eq 0) {
        $compileCommands = Join-Path $buildDir "compile_commands.json"
        $targetFile = Join-Path $nativeDir "compile_commands.json"

        if (Test-Path $compileCommands) {
            Copy-Item $compileCommands $targetFile -Force
            Write-Host "[OK] compile_commands.json written to $targetFile" -ForegroundColor Green
        } else {
            Write-Host "[WARN] compile_commands.json not found at $compileCommands" -ForegroundColor Yellow
            Write-Host "Creating minimal compile_commands.json..." -ForegroundColor Yellow

            $nativePosix = ($nativeDir -replace '\\', '/')
            $basicConfig = @"
[
  {
    "directory": "$nativePosix/video-processing",
    "command": "clang++ -std=c++17 -I$nativePosix/video-processing/include -I$nativePosix/video-processing/src -c",
    "file": "$nativePosix/video-processing/src/rtsp_client.cpp"
  }
]
"@
            Set-Content -Path $targetFile -Value $basicConfig
            Write-Host "[OK] Minimal compile_commands.json created" -ForegroundColor Green
        }
    } else {
        Write-Host "[FAIL] CMake configuration failed" -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

Write-Host "Done." -ForegroundColor Green
exit 0
