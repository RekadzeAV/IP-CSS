gjkyb byajhvfwb. dult ytj,[jlbvj#!/bin/bash
# Проверка подключения к Raspberry Pi

# Проверка наличия .env файла
if [ ! -f "credentials/.env" ]; then
    echo "❌ Ошибка: credentials/.env не найден!"
    echo "Скопируйте credentials/.env.example в credentials/.env и заполните данные"
    exit 1
fi

# Загрузка переменных окружения
export $(grep -v '^#' credentials/.env | xargs)

echo "🔍 Проверка подключения к Raspberry Pi..."
echo ""

# Проверка RPI_HOST
if [ -z "$RPI_HOST" ]; then
    echo "❌ RPI_HOST не заполнен"
    exit 1
fi

echo "📍 Проверяем доступность хоста: $RPI_HOST"
if ping -c 1 -W 2 $RPI_HOST > /dev/null 2>&1; then
    echo "✅ Хост доступен"
else
    echo "⚠️  Хост не доступен (проверьте IP адрес и сеть)"
fi

echo ""
echo "🔐 Проверяем SSH подключение..."
if [ -z "$RPI_PASSWORD" ]; then
    echo "⚠️  RPI_PASSWORD не заполнен (проверьте credentials/.env)"
else
    echo "✅ Пароль заполнен"
fi

echo ""
echo "🐳 Проверяем Docker доступность..."
if [ -z "$DOCKER_HOST" ]; then
    echo "⚠️  DOCKER_HOST не заполнен"
else
    echo "✅ DOCKER_HOST: $DOCKER_HOST"
    
    # Пробуем получить версию Docker
    if docker -H $DOCKER_HOST version > /dev/null 2>&1; then
        echo "✅ Docker доступен"
        docker -H $DOCKER_HOST version --format '{{.Server.Version}}'
    else
        echo "❌ Docker недоступен (проверьте подключение и права)"
    fi
fi

echo ""
echo "📊 Проверяем Portainer..."
if [ -z "$PORTAINER_URL" ]; then
    echo "⚠️  PORTAINER_URL не заполнен"
else
    echo "📍 Portainer URL: $PORTAINER_URL"
    
    # Пробуем проверить доступность
    if curl -s --connect-timeout 5 $PORTAINER_URL > /dev/null 2>&1; then
        echo "✅ Portainer доступен"
    else
        echo "❌ Portainer недоступен (проверьте URL и подключение)"
    fi
fi

echo ""
echo "📋 Итог:"
echo "  RPI_HOST:     $RPI_HOST"
echo "  RPI_USER:     $RPI_USER"
echo "  DOCKER_HOST:  $DOCKER_HOST"
echo "  PORTAINER:    $PORTAINER_URL"
