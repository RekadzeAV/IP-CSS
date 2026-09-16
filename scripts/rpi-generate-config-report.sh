#!/bin/bash
# Генерация отчёта о конфигурации Raspberry Pi 4

# Проверка наличия .env файла
if [ ! -f "credentials/.env" ]; then
    echo "❌ Ошибка: credentials/.env не найден!"
    exit 1
fi

# Загрузка переменных окружения
export $(grep -v '^#' credentials/.env | xargs)

# Проверка обязательных переменных
if [ -z "$RPI_HOST" ]; then
    echo "❌ Ошибка: RPI_HOST не заполнен"
    exit 1
fi

OUTPUT_FILE="docs/RASPBERRY_PI_CONFIG_$(date +%Y%m%d_%H%M%S).md"

echo "📝 Генерация отчёта о конфигурации..."
echo "   Файл: $OUTPUT_FILE"
echo ""

# Создаём отчёт
cat > "$OUTPUT_FILE" << 'HEADER'
# 📡 Raspberry Pi 4 - Отчёт о Конфигурации

HEADER

echo "## 📍 Основная Информация" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "- **Дата проверки:** $(date '+%Y-%m-%d %H:%M:%S')" >> "$OUTPUT_FILE"
echo "- **IP Адрес:** \`$RPI_HOST\`" >> "$OUTPUT_FILE"
echo "- **Пользователь:** \`$RPI_USER\`" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "## 🖥️ Система" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "### Информация об ОС" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
ssh $RPI_USER@$RPI_HOST "uname -a" 2>/dev/null >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Версия Raspberry Pi OS" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
ssh $RPI_USER@$RPI_HOST "cat /etc/os-release 2>/dev/null | grep PRETTY_NAME" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Аппаратная Информация" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
ssh $RPI_USER@$RPI_HOST "cat /proc/cpuinfo | grep -E 'Model|Revision|Serial' " >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Загрузка Системы" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
ssh $RPI_USER@$RPI_HOST "uptime" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "## 💾 Диск и Память" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Использование Диска" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
ssh $RPI_USER@$RPI_HOST "df -h" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Использование Памяти" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
ssh $RPI_USER@$RPI_HOST "free -h" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "## 🐳 Docker" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Версия Docker" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
docker -H $DOCKER_HOST version 2>/dev/null >> "$OUTPUT_FILE" || echo "Docker недоступен" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Установленные Образы" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
docker -H $DOCKER_HOST images 2>/dev/null >> "$OUTPUT_FILE" || echo "Не удалось получить список образов" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Активные Контейнеры" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
docker -H $DOCKER_HOST ps -a 2>/dev/null >> "$OUTPUT_FILE" || echo "Не удалось получить список контейнеров" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "## 📊 Portainer" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Статус Portainer" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
docker -H $DOCKER_HOST ps --filter "name=portainer" 2>/dev/null >> "$OUTPUT_FILE" || echo "Portainer контейнер не найден" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Информация Portainer API" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
curl -k -s "$PORTAINER_URL/api/version" 2>/dev/null | jq '.' >> "$OUTPUT_FILE" 2>/dev/null || echo "Не удалось получить информацию от Portainer API" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "## 🌐 Сеть" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### IP Адреса" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
ssh $RPI_USER@$RPI_HOST "hostname -I" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Сетевые Интерфейсы" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
ssh $RPI_USER@$RPI_HOST "ip -br addr show" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### DNS Настройки" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
ssh $RPI_USER@$RPI_HOST "cat /etc/resolv.conf" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Открытые Порты" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
ssh $RPI_USER@$RPI_HOST "sudo ss -tlnp 2>/dev/null || sudo netstat -tlnp" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "## 🔌 Docker Сети" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Список Сетей" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
docker -H $DOCKER_HOST network ls 2>/dev/null >> "$OUTPUT_FILE" || echo "Не удалось получить сети" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "## 📋 Установленные Пакеты" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "### Ключевые Пакеты" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
ssh $RPI_USER@$RPI_HOST "dpkg -l | grep -E 'docker|portainer|nginx' 2>/dev/null | head -20" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "## 📝 Заключениe" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "Отчёт сгенерирован автоматически." >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "**Доступ к Portainer:** $PORTAINER_URL" >> "$OUTPUT_FILE"
echo "**SSH:** ssh $RPI_USER@$RPI_HOST" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "✅ Отчёт сгенерирован: $OUTPUT_FILE"
echo ""
echo "📊 Краткая сводка:"
echo "   - Образов: $(docker -H $DOCKER_HOST images -q 2>/dev/null | wc -l)"
echo "   - Контейнеров: $(docker -H $DOCKER_HOST ps -a -q 2>/dev/null | wc -l)"
echo "   - Активных контейнеров: $(docker -H $DOCKER_HOST ps -q 2>/dev/null | wc -l)"
