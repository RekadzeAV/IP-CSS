Дополни информацию по , #!/bin/bash
# Подключение к Raspberry Pi 4

# Проверка наличия .env файла
if [ ! -f "credentials/.env" ]; then
    echo "❌ Ошибка: credentials/.env не найден!"
    echo "Скопируйте credentials/.env.example в credentials/.env и заполните данные"
    exit 1
fi

# Загрузка переменных окружения
export $(grep -v '^#' credentials/.env | xargs)

# Проверка обязательных переменных
if [ -z "$RPI_HOST" ] || [ -z "$RPI_USER" ]; then
    echo "❌ Ошибка: RPI_HOST или RPI_USER не заполнены в credentials/.env"
    exit 1
fi

echo "🔌 Подключение к Raspberry Pi..."
echo "   Host: $RPI_HOST"
echo "   User: $RPI_USER"
echo ""

# Подключение через SSH
ssh ${RPI_USER}@${RPI_HOST}
