#!/bin/bash
# Docker команды для Raspberry Pi 4

# Проверка наличия .env файла
if [ ! -f "credentials/.env" ]; then
    echo "❌ Ошибка: credentials/.env не найден!"
    exit 1
fi

# Загрузка переменных окружения
export $(grep -v '^#' credentials/.env | xargs)

# Проверка обязательных переменных
if [ -z "$DOCKER_HOST" ]; then
    echo "❌ Ошибка: DOCKER_HOST не заполнен в credentials/.env"
    exit 1
fi

# Обработка команды
COMMAND="${@:-ps}"

case "$COMMAND" in
    "ps"|"list")
        echo "🐳 Список контейнеров на Raspberry Pi..."
        docker -H $DOCKER_HOST ps -a
        ;;
    "pull")
        echo "📥 Pull образа: $1"
        docker -H $DOCKER_HOST pull $1
        ;;
    "run")
        echo "▶️  Запуск контейнера: $@"
        docker -H $DOCKER_HOST run -d $@
        ;;
    "logs")
        echo "📋 Логи контейнера: $1"
        docker -H $DOCKER_HOST logs $1
        ;;
    "stop")
        echo "⏹️  Остановка контейнера: $1"
        docker -H $DOCKER_HOST stop $1
        ;;
    "start")
        echo "▶️  Запуск контейнера: $1"
        docker -H $DOCKER_HOST start $1
        ;;
    "rm")
        echo "🗑️  Удаление контейнера: $1"
        docker -H $DOCKER_HOST rm $1
        ;;
    "images")
        echo "📦 Список образов на Raspberry Pi..."
        docker -H $DOCKER_HOST images
        ;;
    "portainer")
        echo "📊 Portainer URL: $PORTAINER_URL"
        echo "🔐 Логин: admin"
        echo "💡 Откройте в браузере: $PORTAINER_URL"
        ;;
    *)
        echo "🐳 Docker на Raspberry Pi"
        echo ""
        echo "Использование:"
        echo "  $0 ps          - Список контейнеров"
        echo "  $0 pull <img>  - Pull образа"
        echo "  $0 run <opts>  - Запуск контейнера"
        echo "  $0 logs <id>   - Логи контейнера"
        echo "  $0 stop <id>   - Остановка контейнера"
        echo "  $0 start <id>  - Запуск контейнера"
        echo "  $0 rm <id>     - Удаление контейнера"
        echo "  $0 images      - Список образов"
        echo "  $0 portainer   - Portainer URL"
        echo ""
        echo "Пример:"
        echo "  $0 pull nginx:latest"
        echo "  $0 run -d -p 8080:80 nginx:latest"
        ;;
esac
