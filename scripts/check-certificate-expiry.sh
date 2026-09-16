#!/bin/bash

# Скрипт для проверки срока действия SSL сертификатов
# Использование: ./check-certificate-expiry.sh <domain> [days_before_expiry]
# Пример: ./check-certificate-expiry.sh api.example.com 30

set -e

DOMAIN="${1:-}"
DAYS_BEFORE_EXPIRY="${2:-30}"

if [ -z "$DOMAIN" ]; then
    echo "Использование: $0 <domain> [days_before_expiry]"
    echo "Пример: $0 api.example.com 30"
    exit 1
fi

echo "Проверка срока действия сертификата для $DOMAIN..."
echo ""

# Получаем дату истечения сертификата
EXPIRY_DATE=$(echo | openssl s_client -connect "$DOMAIN:443" -servername "$DOMAIN" 2>/dev/null | \
    openssl x509 -noout -enddate 2>/dev/null | cut -d= -f2)

if [ -z "$EXPIRY_DATE" ]; then
    echo "Ошибка: Не удалось получить информацию о сертификате для $DOMAIN"
    exit 1
fi

# Конвертируем дату в epoch
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    EXPIRY_EPOCH=$(date -j -f "%b %d %H:%M:%S %Y %Z" "$EXPIRY_DATE" +%s 2>/dev/null || \
        date -j -f "%b %d %H:%M:%S %Y" "$EXPIRY_DATE" +%s)
else
    # Linux
    EXPIRY_EPOCH=$(date -d "$EXPIRY_DATE" +%s)
fi

CURRENT_EPOCH=$(date +%s)
DAYS_UNTIL_EXPIRY=$(( ($EXPIRY_EPOCH - $CURRENT_EPOCH) / 86400 ))

echo "Дата истечения: $EXPIRY_DATE"
echo "Дней до истечения: $DAYS_UNTIL_EXPIRY"
echo ""

if [ $DAYS_UNTIL_EXPIRY -lt 0 ]; then
    echo "❌ КРИТИЧНО: Сертификат истек!"
    exit 2
elif [ $DAYS_UNTIL_EXPIRY -lt $DAYS_BEFORE_EXPIRY ]; then
    echo "⚠️  ВНИМАНИЕ: Сертификат истекает через $DAYS_UNTIL_EXPIRY дней!"
    echo "Рекомендуется обновить сертификат."
    exit 1
else
    echo "✅ Сертификат действителен еще $DAYS_UNTIL_EXPIRY дней"
    exit 0
fi
