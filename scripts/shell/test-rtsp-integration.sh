#!/bin/bash
# Скрипт для автоматического запуска интеграционных тестов с RTSP сервером

set -e

echo "========================================="
echo "RTSP Integration Test Runner"
echo "========================================="
echo ""

# Проверка наличия Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker не найден. Установите Docker для запуска тестов."
    exit 1
fi

# Проверка наличия RTSP URL
RTSP_URL="${TEST_RTSP_URL:-}"

if [ -z "$RTSP_URL" ]; then
    echo "ℹ️  TEST_RTSP_URL не установлен. Запуск тестов с локальным RTSP сервером..."
    echo ""
    
    # Остановка существующего контейнера
    echo "🔄 Остановка существующего RTSP сервера..."
    docker rm -f rtsp-test 2>/dev/null || true
    
    # Запуск mediamtx
    echo "🚀 Запуск RTSP сервера (mediamtx)..."
    docker run -d --name rtsp-test -p 8554:8554 -p 8555:8555 bluenviron/mediamtx:latest
    
    # Ожидание запуска
    echo "⏳ Ожидание запуска сервера (10 секунд)..."
    sleep 10
    
    # Проверка доступности
    echo "🔍 Проверка доступности RTSP сервера..."
    if docker exec rtsp-test wget -q --spider http://localhost:8555; then
        echo "✅ RTSP сервер запущен"
    else
        echo "❌ Не удалось запустить RTSP сервер"
        docker logs rtsp-test
        docker rm -f rtsp-test
        exit 1
    fi
    
    # Установка переменной окружения
    export TEST_RTSP_URL="rtsp://localhost:8554/test"
    echo ""
    echo "📡 RTSP URL: $TEST_RTSP_URL"
    echo ""
    
    # Создание тестового потока (опционально)
    echo "ℹ️  Для тестирования создайте поток:"
    echo "   ffmpeg -re -i test.mp4 -c copy -f rtsp $TEST_RTSP_URL"
    echo ""
else
    echo "✅ Используется внешний RTSP URL: $RTSP_URL"
    echo ""
fi

# Запуск тестов
echo "========================================="
echo "🧪 Запуск интеграционных тестов"
echo "========================================="
echo ""

./gradlew :core:network:desktopTest \
    --tests "*RtspRealStreamTest*" \
    --no-daemon \
    -Dtest.single=RtspRealStreamTest

TEST_RESULT=$?

# Остановка контейнера если он был запущен
if [ -z "$TEST_RTSP_URL" ]; then
    echo ""
    echo "🔄 Остановка RTSP сервера..."
    docker rm -f rtsp-test 2>/dev/null || true
fi

# Вывод результатов
echo ""
echo "========================================="
if [ $TEST_RESULT -eq 0 ]; then
    echo "✅ Все тесты пройдены!"
else
    echo "❌ Некоторые тесты не пройдены (код: $TEST_RESULT)"
    echo ""
    echo "Просмотр результатов:"
    echo "  core/network/build/reports/tests/desktopTest/index.html"
fi
echo "========================================="

exit $TEST_RESULT
