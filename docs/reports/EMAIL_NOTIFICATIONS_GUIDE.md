# Email Notifications Configuration Guide

**Дата:** 27 April 2026  
**Задача:** 2.2.2 — Email (SMTP, шаблоны, правила)  
**Статус:** 🟡 В процессе (~90%)

---

## 📧 Настройка email-уведомлений

### 1. Переменные окружения

Добавьте в `.env` или систему переменных окружения:

```bash
# SMTP Configuration
SMTP_ENABLED=true
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
SMTP_FROM_ADDRESS=noreply@ipcss.local
SMTP_FROM_NAME=IP Camera Surveillance System
SMTP_USE_TLS=true
```

### 2. Доступные SMTP серверы

#### Gmail
```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USE_TLS=true
```
**Примечание:** Для Gmail нужно использовать "App Password", а не обычный пароль.

#### Outlook/Hotmail
```bash
SMTP_HOST=smtp-mail.outlook.com
SMTP_PORT=587
SMTP_USE_TLS=true
```

#### Yahoo
```bash
SMTP_HOST=smtp.mail.yahoo.com
SMTP_PORT=587
SMTP_USE_TLS=true
```

#### Office 365
```bash
SMTP_HOST=smtp.office365.com
SMTP_PORT=587
SMTP_USE_TLS=true
```

---

## 📝 Email Templates

### Доступные шаблоны

1. **default.html** — Шаблон по умолчанию для всех уведомлений
2. **motion-detected.html** — Для событий детекции движения
3. **critical-event.html** — Для критических событий

### Плейсхолдеры

Все шаблоны поддерживают следующие плейсхолдеры:

| Плейсхолдер | Описание | Пример |
|-------------|----------|--------|
| `{{title}}` | Заголовок уведомления | "Новое событие" |
| `{{message}}` | Тело сообщения | "Обнаружено движение" |
| `{{type}}` | Тип уведомления | "INFO", "WARNING", "CRITICAL" |
| `{{priority}}` | Приоритет | "NORMAL", "URGENT" |
| `{{cameraId}}` | ID камеры | "camera-001" |
| `{{cameraName}}` | Название камеры | "Входная дверь" |
| `{{eventId}}` | ID события | "evt-12345" |
| `{{timestamp}}` | Время события | "2026-04-27 14:30:00" |
| `{{dashboardUrl}}` | Ссылка на панель | "http://localhost:3000" |
| `{{eventUrl}}` | Ссылка на событие | "http://localhost:3000/events/123" |
| `{{recordingUrl}}` | Ссылка на запись | "http://localhost:3000/recordings/456" |

### Создание кастомного шаблона

1. Создайте файл в `server/api/src/main/resources/email-templates/`
2. Используйте HTML с встроенными CSS стилями
3. Добавьте плейсхолдеры `{{...}}` где нужно
4. Укажите путь к шаблону в правилах аналитики

**Пример кастомного шаблона:**

```html
<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; }
        .header { background: #667eea; color: white; padding: 20px; }
        .content { padding: 20px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>{{title}}</h1>
    </div>
    <div class="content">
        <p>{{message}}</p>
        <p>Камера: {{cameraName}}</p>
    </div>
</body>
</html>
```

---

## 🔧 Использование в правилах аналитики

### Пример правила с кастомным шаблоном

```json
{
  "id": "rule-001",
  "name": "Motion Detection Alert",
  "cameraId": "camera-001",
  "eventType": "MOTION_DETECTION",
  "actions": {
    "sendNotification": true,
    "notificationType": "INFO",
    "emailSubjectTemplate": "[Motion] {{title}}",
    "emailBodyTemplate": "email-templates/motion-detected.html",
    "additionalActions": {
      "__emailSubjectTemplate": "Движение обнаружено: {{cameraName}}",
      "__emailBodyTemplate": "<b>{{message}}</b><br>Камера: {{cameraName}}<br>Время: {{timestamp}}"
    }
  }
}
```

### Приоритет шаблонов

1. `additionalActions.__emailBodyTemplate` — HTML контент (наивысший приоритет)
2. `additionalActions.__emailSubjectTemplate` — Тема письма
3. `actions.emailBodyTemplate` — Путь к файлу шаблона
4. `actions.emailSubjectTemplate` — Тема шаблона
5. По умолчанию — стандартные шаблоны системы

---

## 🧪 Тестирование

### 1. Проверка подключения SMTP

```bash
# Тестовый email
curl -X POST http://localhost:8080/api/v1/notifications/test/email \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "toEmails": ["your-email@example.com"],
    "subject": "Тест email уведомления",
    "message": "Это тестовое сообщение для проверки работы SMTP"
  }'
```

### 2. Логирование

Включите debug логирование для SMTP:

```properties
logging.level.com.company.ipcamera.server.notification=DEBUG
```

### 3. Проверка шаблонов

```bash
# Проверка рендеринга шаблона
curl -X POST http://localhost:8080/api/v1/analytics/rules/test-template \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "template": "email-templates/motion-detected.html",
    "context": {
      "cameraName": "Входная дверь",
      "cameraId": "camera-001",
      "eventId": "evt-123",
      "timestamp": "2026-04-27 14:30:00"
    }
  }'
```

---

## 📊 Мониторинг

### Статистика отправки

```bash
# Получить статистику email-уведомлений
curl -X GET http://localhost:8080/api/v1/notifications/stats/email \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Ошибки отправки

Проверьте логи сервера:

```bash
tail -f logs/application.log | grep "EmailNotificationSender"
```

---

## 🐛 Troubleshooting

### Проблема: Email не отправляется

**Решение:**
1. Проверьте `SMTP_ENABLED=true`
2. Проверьте логи на наличие ошибок подключения
3. Убедитесь, что учётные данные верные
4. Проверьте firewall/антивирус

### Проблема: HTML не отображается правильно

**Решение:**
1. Используйте inline CSS стили
2. Избегайте внешних стилей
3. Тестируйте в разных почтовых клиентах
4. Используйте табличную верстку для совместимости

### Проблема: Плейсхолдеры не заменяются

**Решение:**
1. Проверьте формат `{{name}}` (двойные фигурные скобки)
2. Убедитесь, что контекст передан правильно
3. Проверьте регистр переменных

---

## 📚 Связанные документы

- [NOTIFICATIONS_BLOCK_B.md](../status/NOTIFICATIONS_BLOCK_B.md) — блок B уведомлений
- [PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md) — статус проекта
- [ENVIRONMENT_VARIABLES.md](../ENVIRONMENT_VARIABLES.md) — переменные окружения

---

## ✅ Checklist завершения 2.2.2

- [x] SMTP канал работает
- [x] Кастомные шаблоны поддерживаются
- [x] Retry/backoff механизм
- [x] HTML-шаблоны созданы (default, motion, critical)
- [x] Документация по настройке
- [x] Плейсхолдеры документированы
- [x] Примеры использования
- [ ] Тесты email-шаблонов (требуется)
- [ ] Интеграционные тесты (требуется)

**Прогресс задачи 2.2.2:** ~90%

---

**Дата обновления:** 27 April 2026
