# SMS & Webhook Notifications Guide

**Дата:** 27 April 2026  
**Задача:** 2.2.4 — SMS, Webhook  
**Статус:** 🟡 В процессе (~80%)

---

## 📱 SMS Уведомления

### Поддерживаемые провайдеры

1. **Twilio** — глобальный SMS провайдер
2. **Generic HTTP API** — кастомные SMS шлюзы

---

## 🌐 Twilio SMS

### 1. Регистрация в Twilio

1. Создайте аккаунт на [twilio.com](https://www.twilio.com/)
2. Получите телефонный номер с SMS capability
3. Скопируйте Account SID и Auth Token

### 2. Переменные окружения

```bash
# Twilio Configuration
TWILIO_ENABLED=true
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_FROM_NUMBER=+1234567890
TWILIO_REGION=us1
```

### 3. Использование в правилах

```json
{
  "id": "rule-sms-001",
  "name": "Critical Alert SMS",
  "eventType": "CRITICAL",
  "actions": {
    "sendNotification": true,
    "notificationType": "CRITICAL",
    "additionalActions": {
      "__notifySms": true,
      "__notifySmsTo": "+1234567890,+0987654321"
    }
  }
}
```

### 4. Отправка через API

```bash
curl -X POST http://localhost:8080/api/v1/notifications/test/sms \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "to": ["+1234567890"],
    "message": "Тестовое SMS уведомление"
  }'
```

---

## 🔗 Webhook Уведомления

### 1. Настройка Webhook

```bash
# Webhook Configuration
WEBHOOK_ENABLED=true
WEBHOOK_DEFAULT_URL=https://your-server.com/webhook/ipcss
WEBHOOK_TIMEOUT_MS=10000
WEBHOOK_MAX_RETRIES=3
WEBHOOK_RETRY_BASE_DELAY_MS=1000

# HMAC Signature (опционально)
NOTIFICATION_WEBHOOK_SECRET=your-secret-key
```

### 2. Webhook Payload структура

```json
{
  "type": "EVENT_NOTIFICATION",
  "timestamp": 1714234567890,
  "event": {
    "id": "evt-12345",
    "type": "MOTION_DETECTION",
    "cameraId": "camera-001",
    "cameraName": "Входная дверь",
    "severity": "WARNING",
    "message": "Обнаружено движение",
    "metadata": {
      "snapshotUrl": "http://server/snapshot.jpg",
      "recordingUrl": "http://server/recording.mp4"
    }
  },
  "metadata": {
    "source": "ipcss-server",
    "version": "1.0"
  }
}
```

### 3. HMAC Signature Verification

Webhook подписи используют HMAC-SHA256:

```python
import hmac
import hashlib

def verify_signature(timestamp, body, signature, secret):
    expected = hmac.new(
        secret.encode(),
        f"{timestamp}.{body}".encode(),
        hashlib.sha256
    ).hexdigest()
    return hmac.compare_digest(expected, signature)
```

### 4. Webhook Headers

Все webhook запросы содержат:

- `Content-Type: application/json`
- `X-IPCSS-Timestamp: {timestamp}` (если включён HMAC)
- `X-IPCSS-Signature: {hmac_signature}` (если включён HMAC)
- `X-IPCSS-Signature-Ver: 1`

### 5. Пример обработки webhook (Node.js)

```javascript
app.post('/webhook/ipcss', (req, res) => {
  const signature = req.headers['x-ipcss-signature'];
  const timestamp = req.headers['x-ipcss-timestamp'];
  const body = JSON.stringify(req.body);
  
  // Verify signature
  const isValid = verifySignature(timestamp, body, signature, process.env.WEBHOOK_SECRET);
  if (!isValid) {
    return res.status(401).send('Invalid signature');
  }
  
  // Process event
  const event = req.body.event;
  console.log(`Received ${event.type} from ${event.cameraName}`);
  
  res.status(200).send('OK');
});
```

### 6. Retry Logic

Webhook sender использует exponential backoff:

- **Attempt 1:** Сразу
- **Attempt 2:** +1s
- **Attempt 3:** +2s
- **Максимум:** 3 попытки

---

## 🔧 Конфигурация в DI

### AppModule.kt

```kotlin
// Twilio SMS
smsNotificationSender = TwilioConfig.fromEnvironment()
    ?.let { TwilioSmsSender(it) }

// Advanced Webhook
notificationWebhookSender = WebhookConfig.fromEnvironment()
    ?.let { AdvancedWebhookSender(it) }
```

---

## 🧪 Тестирование

### 1. Тестовое SMS

```bash
curl -X POST http://localhost:8080/api/v1/notifications/test/sms \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "to": ["+1234567890"],
    "message": "Это тестовое SMS от IP-CSS"
  }'
```

### 2. Тестовый Webhook

```bash
curl -X POST http://localhost:8080/api/v1/notifications/test/webhook \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://webhook.site/your-unique-id",
    "payload": {
      "type": "TEST",
      "message": "Тестовое webhook уведомление"
    }
  }'
```

### 3. Логирование

```properties
logging.level.com.company.ipcamera.server.notification=DEBUG
```

---

## 📊 Мониторинг

### Статистика отправки

```bash
# SMS статистика
curl -X GET http://localhost:8080/api/v1/notifications/stats/sms \
  -H "Authorization: Bearer YOUR_TOKEN"

# Webhook статистика
curl -X GET http://localhost:8080/api/v1/notifications/stats/webhook \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 🐛 Troubleshooting

### Проблема: SMS не отправляется

**Решение:**
1. Проверьте `TWILIO_ENABLED=true`
2. Убедитесь, что Account SID и Auth Token верные
3. Проверьте формат номера (международный формат +1234567890)
4. Проверьте баланс Twilio аккаунта

### Проблема: Webhook не доставляется

**Решение:**
1. Проверьте `WEBHOOK_ENABLED=true`
2. Убедитесь, что URL доступен извне
3. Проверьте firewall/SSL сертификаты
4. Проверьте логи на наличие ошибок

### Проблема: HMAC signature не валидируется

**Решение:**
1. Убедитесь, что `NOTIFICATION_WEBHOOK_SECRET` совпадает
2. Проверьте timestamp (должен быть актуальным)
3. Используйте тот же алгоритм (HmacSHA256)

---

## ✅ Checklist завершения 2.2.4

- [x] Twilio SMS sender реализован
- [x] Advanced Webhook sender с HMAC
- [x] Retry/backoff логика
- [x] Документация по настройке
- [x] Примеры интеграции
- [ ] Twilio интеграционные тесты
- [ ] Webhook integration tests
- [ ] Провайдер-специфичные адаптеры

**Прогресс задачи 2.2.4:** ~80% (было ~50%)

---

**Дата обновления:** 27 April 2026
