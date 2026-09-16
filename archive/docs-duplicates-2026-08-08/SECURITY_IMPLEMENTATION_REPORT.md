# 🔐 Security Implementation Report: HTTPS + 2FA + Audit

**Дата:** 2026-01-28  
**Статус:** ✅ Завершено  
**Версия:** 1.0

---

## 📋 Обзор

Этот документ описывает реализацию компонентов безопасности для Фазы 1 (MVP).

### Выполненные компоненты:

1. ✅ **HTTPS Redirect** — Принудительное перенаправление HTTP → HTTPS
2. ✅ **HSTS** — HTTP Strict Transport Security
3. ✅ **2FA TOTP** — Двухфакторная аутентификация
4. ✅ **Audit Logging** — Логирование событий безопасности
5. ✅ **Security Monitoring** — Мониторинг подозрительной активности

---

## 1️⃣ HTTPS Redirect

### Реализация

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/HttpsRedirectMiddleware.kt`

**Конфигурация:**
```bash
# .env.production
FORCE_HTTPS=true
HTTPS_PORT=443
```

### Возможности:

- ✅ Автоматический редирект HTTP → HTTPS (301 Moved Permanently)
- ✅ Поддержка `X-Forwarded-Host` для reverse proxy
- ✅ Сохранение path и query parameters
- ✅ Настройка целевого HTTPS порта
- ✅ Включение/выключение через `FORCE_HTTPS`

### Пример:

```
HTTP:  http://example.com/login?redirect=/dashboard
       ↓ (301 redirect)
HTTPS: https://example.com/login?redirect=/dashboard
```

---

## 2️⃣ HSTS (HTTP Strict Transport Security)

### Реализация

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/HstsMiddleware.kt`

**Конфигурация:**
```bash
FORCE_HTTPS=true
HSTS_MAX_AGE=31536000  # 1 год
HSTS_INCLUDE_SUBDOMAINS=true
```

### Заголовки:

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

### Возможности:

- ✅ Принудительное использование HTTPS браузерами
- ✅ Защита от SSL-stripping атак
- ✅ Включение для поддоменов
- ✅ Preload-ready (опционально)

---

## 3️⃣ 2FA TOTP (Двухфакторная аутентификация)

### Реализация

**Файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/security/TotpService.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/AuthRoutes.kt`

**Конфигурация:**
```bash
# .env.production
TOTP_ISSUER=IP-CSS
TWO_FACTOR_REQUIRED=false  # true для обязательной 2FA
TOTP_WINDOW_SIZE=1  # Допуск ±1 интервал (30 сек)
```

### API Endpoints:

#### 3.1 Setup 2FA

```http
POST /api/v1/auth/2fa/setup
Authorization: Bearer <access_token>

Response:
{
  "success": true,
  "data": {
    "secret": "JBSWY3DPEHPK3PXP",
    "qrData": "otpauth://totp/IP-CSS:username?secret=...",
    "qrPngBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
  }
}
```

#### 3.2 Confirm 2FA

```http
POST /api/v1/auth/2fa/confirm
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "code": "123456"
}

Response:
{
  "success": true,
  "message": "2FA enabled"
}
```

#### 3.3 Login with 2FA

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}

Response (если 2FA включена):
{
  "success": true,
  "data": {
    "needs2fa": true,
    "tempToken": "eyJhbGciOiJIUzI1NiIs...",
    "user": { ... }
  }
}

POST /api/v1/auth/2fa/verify
Content-Type: application/json

{
  "tempToken": "eyJhbGciOiJIUzI1NiIs...",
  "code": "123456"
}

Response:
{
  "success": true,
  "data": {
    "accessToken": "",
    "refreshToken": "",
    "user": { ... }
  }
}
```

#### 3.4 Disable 2FA

```http
POST /api/v1/auth/2fa/disable
Authorization: Bearer <access_token>

Response:
{
  "success": true,
  "message": "2FA disabled"
}
```

### Возможности:

- ✅ Генерация секрета (32 символа, Base32)
- ✅ QR-код для Google Authenticator / Authy
- ✅ Проверка кода с допуском ±1 интервал
- ✅ Временный токен для промежуточного этапа
- ✅ Полная интеграция с login flow
- ✅ Backup codes (опционально, через UserRepository)

---

## 4️⃣ Audit Logging (Логирование событий безопасности)

### Реализация

**Файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/security/AuditLogRepository.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/security/PostgresAuditLogRepository.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/security/InMemoryAuditLogRepository.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/security/SecurityLogger.kt`

**Конфигурация:**
```bash
# .env.production
AUDIT_PERSIST_ENABLED=true
DATABASE_URL=jdbc:postgresql://localhost:5432/ipcss
```

### События для логирования:

| Событие | Метод | Описание |
|---------|-------|----------|
| `logLoginSuccess` | SecurityLogger | Успешный вход |
| `logLoginFailure` | SecurityLogger | Неудачный вход |
| `logLogout` | SecurityLogger | Выход из системы |
| `logPasswordChange` | SecurityLogger | Смена пароля |
| `log2faEnabled` | SecurityLogger | Включение 2FA |
| `log2faDisabled` | SecurityLogger | Отключение 2FA |
| `logPermissionChange` | SecurityLogger | Изменение прав |
| `logSuspiciousActivity` | SecurityLogger | Подозрительная активность |
| `logInvalidToken` | SecurityLogger | Неверный токен |
| `logDataExport` | SecurityLogger | Экспорт данных |

### Пример записи аудита:

```kotlin
SecurityLogger.logLoginSuccess(
    userId = "user-123",
    username = "admin",
    ipAddress = "192.168.1.100",
    userAgent = "Mozilla/5.0 ..."
)
```

### Структура записи:

```sql
CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    event_type TEXT NOT NULL,
    user_id TEXT,
    username TEXT,
    ip_address TEXT,
    user_agent TEXT,
    details JSONB,
    timestamp BIGINT NOT NULL,
    integrity_hash TEXT  -- Для защиты от подделки
);
```

### Возможности:

- ✅ Персистентность в PostgreSQL
- ✅ In-memory режим для разработки
- ✅ Integrity hash для защиты от подделки
- ✅ JSONB детали для гибкости
- ✅ Интеграция с SecurityMonitoringService

---

## 5️⃣ Security Monitoring (Мониторинг безопасности)

### Реализация

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/security/SecurityMonitoringService.kt`

### Возможности:

- ✅ Обнаружение аномалий (множественные неудачные входы)
- ✅ Блокировка IP при брутфорсе
- ✅ Alerts для подозрительной активности
- ✅ Интеграция с AuditLogRepository
- ✅ Real-time мониторинг через WebSocket

### Пример alert:

```kotlin
SecurityLogger.logSuspiciousActivity(
    activity = "Multiple failed login attempts",
    userId = null,
    ipAddress = "192.168.1.100",
    metadata = mapOf(
        "attempt_count" to 10,
        "time_window" to "5 minutes"
    )
)
```

---

## 6️⃣ Дополнительные меры безопасности

### 6.1 Rate Limiting

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/RateLimitMiddleware.kt`

```bash
# Redis-based rate limiting
LOGIN_MAX_ATTEMPTS=5
LOGIN_WINDOW_MINUTES=15
```

### 6.2 Token Blacklist

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/security/TokenBlacklistService.kt`

- ✅ Отзыв refresh токенов при logout
- ✅ Блокировка скомпрометированных токенов
- ✅ Redis-based blacklist с TTL

### 6.3 Token Rotation

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/security/TokenRotationService.kt`

- ✅ Полная ротация токенов при refresh
- ✅ Защита от replay атак
- ✅ Old token blacklist

### 6.4 CAPTCHA

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/security/CaptchaValidator.kt`

- ✅ reCAPTCHA v3 интеграция
- ✅ Требуется после N неудачных попыток
- ✅ Прозрачно для пользователя

### 6.5 CSRF Protection

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/CsrfMiddleware.kt`

- ✅ CSRF токены для state-changing операций
- ✅ SameSite=Lax cookies
- ✅ Double-submit cookie pattern

---

## 📊 Checklist безопасности

| Компонент | Статус | Файл |
|-----------|--------|------|
| HTTPS Redirect | ✅ | `HttpsRedirectMiddleware.kt` |
| HSTS | ✅ | `HstsMiddleware.kt` |
| 2FA TOTP | ✅ | `TotpService.kt`, `AuthRoutes.kt` |
| Audit Logging | ✅ | `AuditLogRepository.kt`, `SecurityLogger.kt` |
| Security Monitoring | ✅ | `SecurityMonitoringService.kt` |
| Rate Limiting | ✅ | `RateLimitMiddleware.kt` |
| Token Blacklist | ✅ | `TokenBlacklistService.kt` |
| Token Rotation | ✅ | `TokenRotationService.kt` |
| CAPTCHA | ✅ | `CaptchaValidator.kt` |
| CSRF Protection | ✅ | `CsrfMiddleware.kt` |
| LDAP/AD Auth | ✅ | `LdapAuthService.kt` |
| OAuth2/OIDC | ✅ | `OAuth2Service.kt` |
| Kerberos | ⚠️ | `KerberosAuthService.kt` (stub) |
| Data Encryption | ✅ | `DataEncryptionService.kt` |

---

## 🚀 Быстрый старт

### 1. Настройка .env

```bash
# HTTPS
FORCE_HTTPS=true
HTTPS_PORT=443
HSTS_MAX_AGE=31536000

# 2FA
TOTP_ISSUER=IP-CSS
TWO_FACTOR_REQUIRED=false

# Audit
AUDIT_PERSIST_ENABLED=true
DATABASE_URL=jdbc:postgresql://localhost:5432/ipcss
DATABASE_USER=postgres
DATABASE_PASSWORD=secure_password

# Rate Limiting
LOGIN_MAX_ATTEMPTS=5
LOGIN_WINDOW_MINUTES=15

# Data Encryption
DATA_ENCRYPTION_KEY=0123456789abcdef0123456789abcdef
```

### 2. Запуск сервера

```bash
cd server/api
./gradlew run
```

### 3. Проверка 2FA

```bash
# Setup 2FA
curl -X POST http://localhost:8080/api/v1/auth/2fa/setup \
  -H "Authorization: Bearer <token>"

# Confirm 2FA
curl -X POST http://localhost:8080/api/v1/auth/2fa/confirm \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"code": "123456"}'
```

### 4. Проверка аудита

```sql
SELECT * FROM audit_log 
WHERE event_type = 'login_success' 
ORDER BY timestamp DESC 
LIMIT 10;
```

---

## 🐛 Troubleshooting

### Проблема: 2FA QR не сканируется

**Решение:**
1. Проверить `TOTP_ISSUER` (без пробелов)
2. Убедиться, что время синхронизировано (NTP)
3. Использовать Google Authenticator или Authy

### Проблема: Audit логи не сохраняются

**Решение:**
1. Проверить `AUDIT_PERSIST_ENABLED=true`
2. Проверить подключение к PostgreSQL
3. Проверить миграции Flyway (таблица `audit_log`)

### Проблема: HTTPS redirect не работает

**Решение:**
1. Проверить `FORCE_HTTPS=true`
2. Проверить reverse proxy настройки (nginx)
3. Проверить `X-Forwarded-Host` header

---

## 📚 Связанные документы

- [POSTGRESQL_PRODUCTION_OPTIMIZATION.md](docs/POSTGRESQL_PRODUCTION_OPTIMIZATION.md)
- [HLS_LOW_LATENCY_OPTIMIZATION.md](docs/HLS_LOW_LATENCY_OPTIMIZATION.md)
- [EnterpriseAuthConfig.kt](server/api/src/main/kotlin/com/company/ipcamera/server/config/EnterpriseAuthConfig.kt)

---

**Подготовлено:** NLP-Core-Team  
**Дата:** 2026-01-28  
**Статус:** ✅ Завершено
