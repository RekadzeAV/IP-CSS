#!/bin/bash
# Развертывание тестового Docker образа на Raspberry Pi

# Проверка наличия .env файла
if [ ! -f "credentials/.env" ]; then
    echo "❌ Ошибка: credentials/.env не найден!"
    echo "Скопируйте credentials/.env.example в credentials/.env и заполните данные"
    exit 1
fi

# Загрузка переменных окружения
export $(grep -v '^#' credentials/.env | xargs)

# Проверка обязательных переменных
if [ -z "$DOCKER_HOST" ]; then
    echo "❌ Ошибка: DOCKER_HOST не заполнен"
    exit 1
fi

echo "🐳 Развертывание тестового Docker образа на Raspberry Pi..."
echo "   Docker Host: $DOCKER_HOST"
echo ""

# Список тестовых образов
echo "📦 Доступные тестовые образы:"
echo "  1. nginx:latest - Веб-сервер"
echo "  2. redis:alpine - Redis кэш"
echo "  3. postgres:alpine - PostgreSQL база данных"
echo "  4. mongo:alpine - MongoDB база данных"
echo "  5. alpine:latest - Минимальный Linux"
echo ""

# Выбор образа
read -p "Выберите номер образа [1-5]: " choice

case $choice in
    1)
        IMAGE="nginx:latest"
        PORTS="-p 8080:80"
        NAME="test-nginx"
        ;;
    2)
        IMAGE="redis:alpine"
        PORTS="-p 6379:6379"
        NAME="test-redis"
        ;;
    3)
        IMAGE="postgres:alpine"
        PORTS="-p 5432:5432"
        NAME="test-postgres"
        echo "🔑 Пароль для postgres: postgres"
        ;;
    4)
        IMAGE="mongo:alpine"
        PORTS="-p 27017:27017"
        NAME="test-mongo"
        ;;
    5)
        IMAGE="alpine:latest"
        PORTS=""
        NAME="test-alpine"
        ;;
    *)
        echo "❌ Неверный выбор!"
        exit 1
        ;;
esac

echo ""
echo "🚀 Развертывание: $IMAGE"
echo ""

# Проверка доступности образа
echo "📥 Проверка образа..."
if docker -H $DOCKER_HOST pull $IMAGE; then
    echo "✅ Образ успешно загружен"
else
    echo "❌ Ошибка при загрузке образа"
    exit 1
fi

# Остановка существующего контейнера
echo "🛑 Остановка существующего контейнера (если есть)..."
docker -H $DOCKER_HOST stop $NAME > /dev/null 2>&1
docker -H $DOCKER_HOST rm $NAME > /dev/null 2>&1

# Запуск нового контейнера
echo "▶️  Запуск контейнера..."
if [ -n "$PORTS" ]; then
    docker -H $DOCKER_HOST run -d $PORTS --name $NAME $IMAGE
else
    docker -H $DOCKER_HOST run -d --name $NAME $IMAGE
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Контейнер успешно запущен!"
    echo ""
    echo "📊 Информация:"
    echo "   Имя: $NAME"
    echo "   Образ: $IMAGE"
    
    if [ -n "$PORTS" ]; then
        PORT_NUM=$(echo $PORTS | cut -d'-' -f2 | cut -d':' -f1)
        HOST_IP=$(echo $DOCKER_HOST | cut -d'@' -f2 | cut -d':' -f1)
        echo "   Доступ: http://$HOST_IP:$PORT_NUM"
    fi
    
    echo ""
    echo "📋 Полезные команды:"
    echo "   Логи:     docker -H $DOCKER_HOST logs $NAME"
    echo "   Статус:   docker -H $DOCKER_HOST ps"
    echo "   Остановить: docker -H $DOCKER_HOST stop $NAME"
    echo "   Удалить:  docker -H $DOCKER_HOST rm $NAME"
    echo ""
else
    echo "❌ Ошибка при запуске контейнера"
    exit 1
fi
