# Блок B. Система уведомлений

Краткий статус и план по задачам B.1–B.6.

---

## B.1 Email-уведомления — выполнено

**Реализовано:**

- **SmtpConfig** (`server/api/.../notification/SmtpConfig.kt`) — конфигурация SMTP из переменных окружения:
  - `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`
  - `SMTP_FROM_ADDRESS`, `SMTP_FROM_NAME`, `SMTP_USE_TLS`, `SMTP_ENABLED`
- **EmailNotificationSender** (`server/api/.../notification/EmailNotificationSender.kt`):
  - Отправка писем через Jakarta Mail (SMTP)
  - Шаблоны: тема и тело с плейсхолдерами `{{title}}`, `{{message}}`, `{{type}}`, `{{priority}}`, `{{cameraId}}`, `{{eventId}}`
  - Повторные попытки: до 3 раз с паузой 2 с при ошибке
- **NotificationService** (сервер):
  - После сохранения уведомления в БД и отправки по WebSocket при наличии SMTP-конфига отправляет копию по email
  - Адреса: один пользователь по `userId` или все пользователи с заполненным `email` (из `ServerUserRepository`)
- **DI**: опциональная регистрация `EmailNotificationSender` (если задан `SMTP_HOST`), передача в `NotificationService` вместе с `ServerUserRepository`
- **Документация**: в `docs/ENVIRONMENT_VARIABLES.md` добавлены `SMTP_FROM_ADDRESS`, `SMTP_FROM_NAME`, `SMTP_USE_TLS`, `SMTP_ENABLED`

**Зависимость:** `com.sun.mail:jakarta.mail:2.0.1` в `server/api/build.gradle.kts`.

---

## B.2 Push-уведомления — запланировано

- FCM (Android), APNs (iOS)
- Регистрация устройств (токены), доставка push с бэкенда

---

## B.3 SMS-уведомления — запланировано

- Провайдер (Twilio и др.), лимиты, шаблоны, привязка номера к пользователю

---

## B.4 Webhook-уведомления — запланировано

- POST на настраиваемый URL при событиях
- Типы событий, формат тела, retry, таймауты

---

## B.5 Правила уведомлений — запланировано

- Типы событий, условия, выбор каналов (in-app, email, push, SMS, webhook), приоритеты
- Связь с существующим `AnalyticsRuleService` и правилами аналитики

---

## B.6 UI настроек уведомлений — запланировано

- Страница/модалки: управление правилами, включение/выключение каналов, тестовые отправки
