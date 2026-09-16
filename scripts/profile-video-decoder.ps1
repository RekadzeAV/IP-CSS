# Run VideoDecoderPerformanceTest with JVM diagnostic logging (profile.log, Gradle reports).
# Usage: .\scripts\profile-video-decoder.ps1

param(
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Run JVM tests with -Pprofile=true and compilation logging; optionally start jvisualvm if on PATH."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\profile-video-decoder.ps1 -ShowHelp"
    Write-Host "  .\scripts\profile-video-decoder.ps1"
    Write-Host ""
    Write-Host "Artifacts:"
    Write-Host "  profile.log, build/reports/tests (see script tail messages)"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  Non-zero  Propagated from gradlew.bat"
    exit 0
}

Write-Host "=== VideoDecoder Profiling Script ===" -ForegroundColor Cyan
Write-Host ""

# Проверка наличия инструментов профилирования
$hasVisualVM = Get-Command jvisualvm -ErrorAction SilentlyContinue
$hasJProfiler = Get-Command jprofiler -ErrorAction SilentlyContinue

if (-not $hasVisualVM -and -not $hasJProfiler) {
    Write-Host "WARNING: No profiling tools found (jvisualvm or jprofiler)" -ForegroundColor Yellow
    Write-Host "Profiling will use built-in JVM metrics only" -ForegroundColor Yellow
    Write-Host ""
}

# Настройка JVM для профилирования
$env:JAVA_OPTS = "-XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation -XX:LogFile=profile.log"

# Запуск приложения с профилированием
Write-Host "Starting application with profiling..." -ForegroundColor Green
Write-Host ""

# Если доступен jvisualvm, запускаем его
if ($hasVisualVM) {
    Write-Host "Starting VisualVM..." -ForegroundColor Green
    Start-Process jvisualvm
    Write-Host "VisualVM started. Connect to the running application." -ForegroundColor Green
    Write-Host ""
}

# Запуск тестов с профилированием
Write-Host "Running performance tests..." -ForegroundColor Green
& .\gradlew.bat :core:network:jvmTest --tests "com.company.ipcamera.core.network.video.VideoDecoderPerformanceTest" `
    -Pprofile=true `
    -PjvmArgs="-XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation"

Write-Host ""
Write-Host "=== Profiling Complete ===" -ForegroundColor Cyan
Write-Host "Check profile.log for compilation logs"
Write-Host "Check build\reports\tests for test results"
