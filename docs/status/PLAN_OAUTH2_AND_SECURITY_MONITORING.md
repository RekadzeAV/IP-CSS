# План доработки: OAuth2/OIDC и мониторинг безопасности

**Дата:** 1 March 2026

---

## 1. Полный OAuth2/OIDC flow

### Цель
Реализовать вход через внешний IdP: редирект на провайдера → callback с кодом → обмен кода на токены → получение userinfo → создание/получение пользователя в системе → выдача JWT и cookies.

### Этапы

| № | Задача | Описание |
|---|--------|----------|
| 1.1 | Хранение state | Сохранять state (CSRF) в Redis с TTL 10 мин. Ключ: `oauth2_state:{state}`, значение: redirect_uri или JSON (redirect_uri, frontend_url). |
| 1.2 | OAuth2Service | Класс: `buildAuthorizationUrl(state)`, `exchangeCodeForTokens(code, redirectUri)`, `getUserInfo(accessToken)`. Использовать Ktor HttpClient, конфиг из EnterpriseAuthConfig. |
| 1.3 | Маппинг userinfo → User | Из userinfo (sub, preferred_username, email, name) формировать username, email, fullName; роль по умолчанию VIEWER или из claim/конфига. |
| 1.4 | GET /api/v1/auth/oauth2/login | Генерация state, сохранение в Redis, редирект 302 на authorization URL (client_id, redirect_uri, scope, response_type=code, state). |
| 1.5 | GET /api/v1/auth/oauth2/callback | Проверка state по Redis, обмен code на tokens, запрос userinfo, getOrCreateUserByUsername, выдача JWT + cookies, редирект на frontend (OAUTH2_FRONTEND_SUCCESS_URL или query param). |
| 1.6 | Обработка ошибок | При ошибке (invalid state, token error, userinfo error) редирект на frontend error URL с параметром error. |
| 1.7 | DI и конфиг | Зарегистрировать OAuth2Service, добавить OAUTH2_STATE_TTL_SEC, OAUTH2_FRONTEND_SUCCESS_URL, OAUTH2_FRONTEND_ERROR_URL в конфиг. |

### Зависимости
- Ktor HttpClient (уже используется в проекте).
- Redis (уже есть для rate limit и login attempts).

---

## 2. Мониторинг безопасности (алерты и дашборд)

### Цель
Подсистема алертов по событиям аудита и API дашборда для админов.

### Этапы

| № | Задача | Описание |
|---|--------|----------|
| 2.1 | Модель алерта | SecurityAlert: id, type, severity, title, description, createdAt, resolvedAt?, relatedEventIds, metadata (ip, userId, count). Типы: BRUTE_FORCE, SUSPICIOUS_LOGIN, RATE_LIMIT_ABUSE, MULTIPLE_FAILURES, и т.д. |
| 2.2 | SecurityAlertRepository | Интерфейс: save(alert), getById(id), getActive(limit), list(filter), resolve(id). In-memory реализация. |
| 2.3 | Правила детекции | SecurityMonitoringService: при append в AuditLogRepository (или периодически) проверять правила: например, «если за 5 мин ≥ 5 LOGIN_FAILURE с одного IP» → создать алерт BRUTE_FORCE. Опционально: подписка на события из SecurityLogger. |
| 2.4 | GET /api/v1/security/dashboard | Сводка: количество событий по типам за период, последние N событий, активные алерты (count, список). Только админ. |
| 2.5 | GET /api/v1/security/alerts | Список алертов с пагинацией и фильтрами (resolved, type, from, to). |
| 2.6 | POST /api/v1/security/alerts/:id/resolve | Отметить алерт как решённый. |
| 2.7 | Интеграция с аудитом | При записи события в AuditLogRepository вызывать проверку правил и создание алертов при срабатывании. |

### Зависимости
- AuditLogRepository (уже есть).
- SecurityEventType, SecurityEvent (уже есть).

---

## Порядок реализации

1. OAuth2: state storage → OAuth2Service → маршруты login + callback → DI и env.
2. Мониторинг: модель и репозиторий алертов → правила и SecurityMonitoringService → маршруты dashboard, alerts, resolve → интеграция с аудитом.

---

## Статус (1 March 2026)

- **OAuth2/OIDC:** реализовано. GET /auth/oauth2/login, GET /auth/oauth2/callback, OAuth2StateStore (Redis), OAuth2Service, выдача JWT и cookies.
- **Мониторинг:** реализовано. SecurityAlert, SecurityAlertRepository, SecurityMonitoringService (правила BRUTE_FORCE, RATE_LIMIT_ABUSE, SUSPICIOUS), GET /security/dashboard, GET /security/alerts, POST /security/alerts/:id/resolve. SecurityLogger вызывает onAuditEvent при каждом событии.
