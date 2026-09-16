#!/bin/bash

# Скрипт для получения SHA-256 fingerprint сертификата
# Использование: ./get-certificate-fingerprint.sh <hostname> [port]
# Пример: ./get-certificate-fingerprint.sh api.example.com 443

set -e

HOST="${1:-}"
PORT="${2:-443}"

if [ -z "$HOST" ]; then
    echo "Использование: $0 <hostname> [port]"
    echo "Пример: $0 api.example.com 443"
    exit 1
fi

echo "Получение SHA-256 fingerprint для $HOST:$PORT..."
echo ""

# Получаем сертификат и вычисляем SHA-256 fingerprint
FINGERPRINT=$(echo | openssl s_client -connect "$HOST:$PORT" -servername "$HOST" 2>/dev/null | \
    openssl x509 -fingerprint -sha256 -noout | \
    cut -d'=' -f2)

if [ -z "$FINGERPRINT" ]; then
    echo "Ошибка: Не удалось получить fingerprint для $HOST:$PORT"
    exit 1
fi

# Форматируем в формате для certificate pinning (sha256/...)
PIN="sha256/$FINGERPRINT"

echo "✅ SHA-256 Fingerprint:"
echo "$PIN"
echo ""
echo "Добавьте в конфигурацию certificate pinning:"
echo "  \"$HOST\": ["
echo "    \"$PIN\""
echo "  ]"
echo ""

# Получаем информацию о сертификате
echo "Информация о сертификате:"
echo | openssl s_client -connect "$HOST:$PORT" -servername "$HOST" 2>/dev/null | \
    openssl x509 -noout -subject -issuer -dates
