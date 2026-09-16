#!/bin/bash

# Скрипт для настройки SSL сертификатов Let's Encrypt
# Использование: ./setup-ssl-letsencrypt.sh <domain> <email>
# Пример: ./setup-ssl-letsencrypt.sh api.example.com admin@example.com

set -e

DOMAIN="${1:-}"
EMAIL="${2:-}"

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
    echo "Использование: $0 <domain> <email>"
    echo "Пример: $0 api.example.com admin@example.com"
    exit 1
fi

echo "Настройка SSL сертификатов Let's Encrypt для $DOMAIN..."
echo ""

# Проверяем, установлен ли certbot
if ! command -v certbot &> /dev/null; then
    echo "Установка certbot..."

    # Определяем дистрибутив
    if [ -f /etc/debian_version ]; then
        sudo apt-get update
        sudo apt-get install -y certbot python3-certbot-nginx
    elif [ -f /etc/redhat-release ]; then
        sudo yum install -y certbot python3-certbot-nginx
    else
        echo "Ошибка: Неподдерживаемый дистрибутив. Установите certbot вручную."
        exit 1
    fi
fi

# Проверяем, что nginx установлен и настроен
if ! command -v nginx &> /dev/null; then
    echo "Ошибка: nginx не установлен. Установите nginx перед запуском этого скрипта."
    exit 1
fi

# Получаем сертификат
echo "Получение SSL сертификата для $DOMAIN..."
sudo certbot certonly \
    --nginx \
    --non-interactive \
    --agree-tos \
    --email "$EMAIL" \
    -d "$DOMAIN"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ SSL сертификат успешно получен!"
    echo ""
    echo "Следующие шаги:"
    echo "1. Обновите конфигурацию nginx (config/nginx/nginx.conf.example)"
    echo "2. Укажите пути к сертификатам:"
    echo "   ssl_certificate /etc/letsencrypt/live/$DOMAIN/fullchain.pem;"
    echo "   ssl_certificate_key /etc/letsencrypt/live/$DOMAIN/privkey.pem;"
    echo "3. Перезагрузите nginx: sudo systemctl reload nginx"
    echo ""
    echo "Настройка автоматического обновления сертификатов..."

    # Создаем cron job для автоматического обновления
    (crontab -l 2>/dev/null; echo "0 0 * * * certbot renew --quiet --post-hook 'systemctl reload nginx'") | crontab -

    echo "✅ Автоматическое обновление сертификатов настроено (ежедневно в 00:00)"
else
    echo "Ошибка: Не удалось получить SSL сертификат"
    exit 1
fi
