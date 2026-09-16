#!/bin/bash

# Скрипт для мониторинга ошибок certificate pinning
# Использование: ./monitor-certificate-pinning.sh [log_file]
# Пример: ./monitor-certificate-pinning.sh /var/log/ip-css/security.log

set -e

LOG_FILE="${1:-/var/log/ip-css/security.log}"
ALERT_EMAIL="${ALERT_EMAIL:-security@example.com}"
THRESHOLD="${THRESHOLD:-5}"  # Количество ошибок для алерта

echo "Мониторинг ошибок certificate pinning в $LOG_FILE..."
echo ""

# Проверяем наличие лог-файла
if [ ! -f "$LOG_FILE" ]; then
    echo "Ошибка: Лог-файл $LOG_FILE не найден"
    exit 1
fi

# Подсчитываем ошибки certificate pinning за последний час
ERROR_COUNT=$(grep -c "CERTIFICATE_PINNING_FAILURE" "$LOG_FILE" 2>/dev/null || echo "0")

echo "Найдено ошибок certificate pinning: $ERROR_COUNT"

if [ "$ERROR_COUNT" -gt "$THRESHOLD" ]; then
    echo "⚠️  ВНИМАНИЕ: Превышен порог ошибок certificate pinning!"
    echo ""

    # Получаем последние ошибки
    echo "Последние ошибки:"
    grep "CERTIFICATE_PINNING_FAILURE" "$LOG_FILE" | tail -n 10

    # Отправляем алерт (если настроен email)
    if [ -n "$ALERT_EMAIL" ] && command -v mail &> /dev/null; then
        echo "Отправка алерта на $ALERT_EMAIL..."
        {
            echo "Subject: Certificate Pinning Failure Alert"
            echo "To: $ALERT_EMAIL"
            echo ""
            echo "Обнаружено $ERROR_COUNT ошибок certificate pinning за последний час."
            echo ""
            echo "Последние ошибки:"
            grep "CERTIFICATE_PINNING_FAILURE" "$LOG_FILE" | tail -n 10
        } | mail -s "Certificate Pinning Failure Alert" "$ALERT_EMAIL"
    fi

    exit 1
else
    echo "✅ Количество ошибок в пределах нормы"
    exit 0
fi
