# PowerShell скрипт для проверки RTSP клиента
# Запускает тесты с реальным RTSP сервером

param(
    [string]$RtspUrl = "",
    [string]$Username = "",
    [string]$Password = ""
)

Write-Host "=== RTSP Client Test Script ===" -ForegroundColor Cyan
Write-Host ""

# Проверка переменных окружения
if ([string]::IsNullOrEmpty($RtspUrl)) {
    $RtspUrl = $env:TEST_RTSP_URL
}

if ([string]::IsNullOrEmpty($Username)) {
    $Username = $env:TEST_RTSP_USERNAME
}

if ([string]::IsNullOrEmpty($Password)) {
    $Password = $env:TEST_RTSP_PASSWORD
}

if ([string]::IsNullOrEmpty($RtspUrl)) {
    Write-Host "Внимание: TEST_RTSP_URL не задан." -ForegroundColor Yellow
    Write-Host "Запуск тестов без реального RTSP сервера (только Mock тесты)" -ForegroundColor Yellow
    Write-Host ""
    
    # Запуск Mock тестов
    Write-Host "Запуск Mock тестов..." -ForegroundColor Green
    ./gradlew :core:network:desktopTest --tests "*RtspClientNativeMockTest*" --no-daemon
    
    exit $LASTEXITCODE
}

Write-Host "RTSP URL: $RtspUrl" -ForegroundColor Green
if ($Username) { Write-Host "Username: $Username" -ForegroundColor Green }
Write-Host ""

# Установка переменных окружения для тестов
$env:TEST_RTSP_URL = $RtspUrl
if ($Username) { $env:TEST_RTSP_USERNAME = $Username }
if ($Password) { $env:TEST_RTSP_PASSWORD = $Password }

Write-Host "Запуск тестов с реальным RTSP сервером..." -ForegroundColor Green
Write-Host ""

# Запуск тестов
./gradlew :core:network:desktopTest --tests "*RtspRealStreamTest*" --no-daemon

$exitCode = $LASTEXITCODE

# Сброс переменных
Remove-Item Env:\TEST_RTSP_URL -ErrorAction SilentlyContinue
if ($Username) { Remove-Item Env:\TEST_RTSP_USERNAME -ErrorAction SilentlyContinue }
if ($Password) { Remove-Item Env:\TEST_RTSP_PASSWORD -ErrorAction SilentlyContinue }

exit $exitCode
