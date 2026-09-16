# Скрипт для автоматического запуска интеграционных тестов с RTSP сервером

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "RTSP Integration Test Runner" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Проверка наличия Docker
if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Docker не найден. Установите Docker для запуска тестов." -ForegroundColor Red
    exit 1
}

# Проверка наличия RTSP URL
$rtspUrl = $env:TEST_RTSP_URL

if ([string]::IsNullOrEmpty($rtspUrl)) {
    Write-Host "ℹ️  TEST_RTSP_URL не установлен. Запуск тестов с локальным RTSP сервером..." -ForegroundColor Yellow
    Write-Host ""
    
    # Остановка существующего контейнера
    Write-Host "🔄 Остановка существующего RTSP сервера..." -ForegroundColor Yellow
    docker rm -f rtsp-test 2>$null | Out-Null
    
    # Запуск mediamtx
    Write-Host "🚀 Запуск RTSP сервера (mediamtx)..." -ForegroundColor Green
    docker run -d --name rtsp-test -p 8554:8554 -p 8555:8555 bluenviron/mediamtx:latest | Out-Null
    
    # Ожидание запуска
    Write-Host "⏳ Ожидание запуска сервера (10 секунд)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
    
    # Проверка доступности
    Write-Host "🔍 Проверка доступности RTSP сервера..." -ForegroundColor Yellow
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8555" -TimeoutSec 5 -UseBasicParsing
        Write-Host "✅ RTSP сервер запущен" -ForegroundColor Green
    } catch {
        Write-Host "❌ Не удалось запустить RTSP сервер" -ForegroundColor Red
        docker logs rtsp-test
        docker rm -f rtsp-test 2>$null | Out-Null
        exit 1
    }
    
    # Установка переменной окружения
    $env:TEST_RTSP_URL = "rtsp://localhost:8554/test"
    Write-Host ""
    Write-Host "📡 RTSP URL: $($env:TEST_RTSP_URL)" -ForegroundColor Green
    Write-Host ""
    
    Write-Host "ℹ️  Для тестирования создайте поток:" -ForegroundColor Yellow
    Write-Host "   ffmpeg -re -i test.mp4 -c copy -f rtsp $($env:TEST_RTSP_URL)" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host "✅ Используется внешний RTSP URL: $rtspUrl" -ForegroundColor Green
    Write-Host ""
}

# Запуск тестов
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "🧪 Запуск интеграционных тестов" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$testResult = ./gradlew :core:network:desktopTest `
    --tests "*RtspRealStreamTest*" `
    --no-daemon

$exitCode = $LASTEXITCODE

# Остановка контейнера если он был запущен
if ([string]::IsNullOrEmpty($env:TEST_RTSP_URL)) {
    Write-Host ""
    Write-Host "🔄 Остановка RTSP сервера..." -ForegroundColor Yellow
    docker rm -f rtsp-test 2>$null | Out-Null
}

# Вывод результатов
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
if ($exitCode -eq 0) {
    Write-Host "✅ Все тесты пройдены!" -ForegroundColor Green
} else {
    Write-Host "❌ Некоторые тесты не пройдены (код: $exitCode)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Просмотр результатов:" -ForegroundColor Yellow
    Write-Host "  core/network/build/reports/tests/desktopTest/index.html" -ForegroundColor White
}
Write-Host "=========================================" -ForegroundColor Cyan

exit $exitCode
