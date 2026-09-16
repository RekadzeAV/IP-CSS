#!/bin/bash
# Проверка установленной конфигурации на Raspberry Pi 4

# Проверка наличия .env файла
if [ ! -f "credentials/.env" ]; then
    echo "❌ Ошибка: credentials/.env не найден!"
    exit 1
fi

# Загрузка переменных окружения
export $(grep -v '^#' credentials/.env | xargs)

# Проверка обязательных переменных
if [ -z "$DOCKER_HOST" ] || [ -z "$RPI_HOST" ]; then
    echo "❌ Ошибка: DOCKER_HOST или RPI_HOST не заполнены"
    exit 1
fi

echo "=========================================="
echo "🔍 Проверка конфигурации Raspberry Pi 4"
echo "=========================================="
echo ""
echo "📍 Хост: $RPI_HOST"
echo "👤 Пользователь: $RPI_USER"
echo ""

# 1. Проверка SSH подключения
echo "=========================================="
echo "1️⃣  SSH Подключение"
echo "=========================================="
echo "🔌 Проверяем SSH..."

if ssh -o ConnectTimeout=5 -o BatchMode=yes $RPI_USER@$RPI_HOST "echo 'SSH OK'" > /dev/null 2>&1; then
    echo "✅ SSH подключается"
else
    echo "❌ SSH не подключается"
    echo "   Проверьте:"
    echo "   - Пароль пользователя"
    echo "   - IP адрес"
    echo "   - Файрвол на RPi"
    exit 1
fi
echo ""

# 2. Информация о системе
echo "=========================================="
echo "2️⃣  Информация о системе"
echo "=========================================="
echo "📊 Получаем информацию о системе..."
echo ""

ssh $RPI_USER@$RPI_HOST << 'EOF'
echo "🖥️  ОС:"
uname -a
echo ""
echo "💾 Диск:"
df -h /
echo ""
echo "📦 Память:"
free -h
echo ""
echo "🔥 Процессор:"
cat /proc/cpuinfo | grep "model name" | head -1
echo ""
echo "⚡ Температура CPU:"
cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null | awk '{printf "%.1f°C\n", $1/1000}' || echo "N/A"
EOF
echo ""

# 3. Проверка Docker
echo "=========================================="
echo "3️⃣  Docker"
echo "=========================================="
echo "🐳 Проверяем Docker..."
echo ""

# Версия Docker
echo "📋 Версия Docker:"
docker -H $DOCKER_HOST version --format '🔹 Server: {{.Server.Version}}' 2>/dev/null || echo "❌ Docker недоступен"
echo ""

# Статус Docker сервис
echo "🔧 Статус Docker сервиса:"
ssh $RPI_USER@$RPI_HOST "systemctl is-active docker" 2>/dev/null | \
    awk '{if ($1=="active") print "✅ Docker сервис активен"; else print "❌ Docker сервис не активен"}' || echo "❌ Не удалось проверить"
echo ""

# Список образов
echo "📦 Установленные образы:"
docker -H $DOCKER_HOST images --format '🔹 {{.Repository}}:{{.Tag}} ({{.Size}})' 2>/dev/null | head -10 || echo "   Нет образов"
echo ""

# Список контейнеров
echo "🚀 Активные контейнеры:"
docker -H $DOCKER_HOST ps --format '🔹 {{.Names}} ({{.Image}}) - {{.Status}}' 2>/dev/null | head -10 || echo "   Нет активных контейнеров"
echo ""

# 4. Проверка Portainer
echo "=========================================="
echo "4️⃣  Portainer"
echo "=========================================="
echo "📊 Проверяем Portainer..."
echo ""

echo "🌐 URL: $PORTAINER_URL"
echo "👤 Логин: $PORTAINER_ADMIN"
echo ""

# Проверка доступности Portainer
echo "🔍 Доступность Portainer:"
if curl -k --connect-timeout 10 "$PORTAINER_URL" > /dev/null 2>&1; then
    echo "✅ Portainer доступен через HTTPS"
else
    echo "❌ Portainer недоступен"
    echo "   Проверьте:"
    echo "   - Порт 9443 открыт"
    echo "   - Portainer контейнер запущен"
fi
echo ""

# Portainer контейнер
echo "🐳 Portainer контейнер:"
docker -H $DOCKER_HOST ps --filter "name=portainer" --format '🔹 {{.Names}} - {{.Status}}' 2>/dev/null || echo "   Portainer контейнер не найден"
echo ""

# 5. Сетевая конфигурация
echo "=========================================="
echo "5️⃣  Сеть"
echo "=========================================="
echo "🌐 Сетевая информация:"
echo ""

ssh $RPI_USER@$RPI_HOST << 'EOF'
echo "📡 IP адреса:"
hostname -I
echo ""
echo "🔌 Сетевой интерфейс:"
ip -br addr show | grep -v "lo\|docker"
echo ""
echo "🌐 DNS:"
cat /etc/resolv.conf | grep nameserver
EOF
echo ""

# 6. Проверка портов
echo "=========================================="
echo "6️⃣  Открытые порты"
echo "=========================================="
echo "🔌 Проверяем открытые порты..."
echo ""

ssh $RPI_USER@$RPI_HOST "sudo ss -tlnp 2>/dev/null | grep -E '22|80|443|9000|9443' || sudo netstat -tlnp 2>/dev/null | grep -E '22|80|443|9000|9443'" | \
    awk '{if (NR>1) print "🔹 " $0}' || echo "   Не удалось проверить порты"
echo ""

# 7. Docker сеть
echo "=========================================="
echo "7️⃣  Docker сети"
echo "=========================================="
echo "🌐 Docker сети:"
echo ""

docker -H $DOCKER_HOST network ls --format '🔹 {{.Name}} ({{.Driver}})' 2>/dev/null || echo "   Не удалось получить сети"
echo ""

# 8. Итоговая сводка
echo "=========================================="
echo "📋 ИТОГОВАЯ СВОДКА"
echo "=========================================="
echo ""
echo "✅ Проверено:"
echo "   - SSH подключение: $(ssh -o ConnectTimeout=5 -o BatchMode=yes $RPI_USER@$RPI_HOST 'echo OK' 2>/dev/null && echo '✅ OK' || echo '❌ FAIL')"
echo "   - Docker: $(docker -H $DOCKER_HOST version > /dev/null 2>&1 && echo '✅ OK' || echo '❌ FAIL')"
echo "   - Portainer: $(curl -k --connect-timeout 10 $PORTAINER_URL > /dev/null 2>&1 && echo '✅ OK' || echo '❌ FAIL')"
echo ""
echo "📊 Система:"
ssh $RPI_USER@$RPI_HOST "uptime" | awk '{print "   ⏱️  " $3 $4}'
echo ""
echo "💾 Диск:"
ssh $RPI_USER@$RPI_HOST "df -h / | tail -1" | awk '{print "   📦 Используется: " $3 " из " $2 " (" $5 ")"}'
echo ""

echo "=========================================="
echo "🔗 Полезные ссылки"
echo "=========================================="
echo ""
echo "📊 Portainer: $PORTAINER_URL"
echo "🐳 Docker: ssh $RPI_USER@$RPI_HOST"
echo ""
echo "📝 Команды:"
echo "   docker -H $DOCKER_HOST ps          - Список контейнеров"
echo "   docker -H $DOCKER_HOST images      - Список образов"
echo "   ssh $RPI_USER@$RPI_HOST            - SSH подключение"
echo ""
echo "=========================================="
