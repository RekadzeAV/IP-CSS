# Docker + Web Manual Runbook (2026-04-27)

## Назначение

Памятка для ручного (физического) тестирования серверной части и web-интерфейса в локальном Docker-стенде.

## Актуальные параметры авторизации (зафиксировано)

- Логин: `admin`
- Пароль: `ADMIN_PASSWORD@`

Данные заданы через `.env.docker-manual` и проброшены в контейнер `surveillance`.

## Что уже подготовлено

- Docker-образ сервера: `ip-css-surveillance:latest`
- Compose-конфигурация для ручного режима:
  - `docker-compose.yml`
  - `docker-compose.manual.yml`
- Env-файл ручного стенда:
  - `.env.docker-manual`

## Параметры подключения после запуска

### API / Backend

- Base URL: `http://localhost:8080`
- Health: `http://localhost:8080/api/v1/health`

### PostgreSQL

- Host: `localhost`
- Port: `55432`
- Database: `surveillance`
- User: `surveillance`
- Password: `ManualCheckDb#2026!Secure`

### Redis

- Host: `localhost`
- Port: `56379`
- Password: `ManualCheckRedis#2026!Secure`

### Web UI (Next.js)

- URL: `http://localhost:3000`
- Страница входа: `http://localhost:3000/login`

## Как запустить серверный Docker-стенд

Из корня репозитория:

`docker compose --env-file .env.docker-manual -f docker-compose.yml -f docker-compose.manual.yml up -d`

Проверка статуса:

`docker compose --env-file .env.docker-manual -f docker-compose.yml -f docker-compose.manual.yml ps`

Проверка health:

`curl http://localhost:8080/api/v1/health`

## Как запустить Web UI

Из `server/web`:

1. `npm install` (если зависимости еще не установлены)
2. `npm run dev`
3. Открыть `http://localhost:3000/login`

## Логирование (настроено)

Файловое логирование сервера включено через `logback`:

- Конфиг: `server/api/src/main/resources/logback.xml`
- Каталог логов в проекте: `data/logs`
- Основной файл: `data/logs/server.log`
- Ротация: `data/logs/server.YYYY-MM-DD.N.log.gz`

Пояснение:

- Логи продолжают идти в `docker logs` (stdout/stderr).
- Дополнительно те же события пишутся в файл в каталоге проекта (через volume `./data/logs:/app/logs`).

Проверка, что логирование работает:

1. Вызвать `curl http://localhost:8080/api/v1/health`
2. Убедиться, что обновился `data/logs/server.log`

Дополнительно:

- Памятка по каталогу логов: `data/logs/README.md`

## Проверка авторизации (ручной API smoke)

### Логин

`POST http://localhost:8080/api/v1/auth/login`

Body:

```json
{
  "username": "admin",
  "password": "ADMIN_PASSWORD@"
}
```

Ожидается: `success=true`, выставлены cookies `access_token` и `refresh_token`.

### Проверка защищенного доступа

В web-клиенте доступ к защищенным разделам должен открываться после логина.
Для чистого API-теста можно использовать Bearer `access_token` из cookie и вызвать:

`GET http://localhost:8080/api/v1/users/me`

## Команды остановки

Остановить контейнеры (без удаления данных):

`docker compose --env-file .env.docker-manual -f docker-compose.yml -f docker-compose.manual.yml down`

Остановить и удалить данные (сброс БД/Redis):

`docker compose --env-file .env.docker-manual -f docker-compose.yml -f docker-compose.manual.yml down -v`

## Что проверено в этом цикле

- Docker-стек поднят и работает (`surveillance`, `postgres`, `redis` healthy).
- API health отвечает `200`.
- Web UI доступен на `http://localhost:3000/login`.
- Логин `admin / ADMIN_PASSWORD@` успешен.
- Доступ к защищенному endpoint `users/me` подтвержден с валидным access token.
- Проверено файловое логирование: `data/logs/server.log` создается и наполняется.

## Дополнительная проверка (2026-04-27, вечер)

### Перезапуск контейнера и очистка логов

- Контейнер `ip-camera-surveillance` перезапущен.
- Лог `data/logs/server.log` очищен (файл пересоздан после старта сервиса).
- После операций API остается доступным: `GET /api/v1/health` -> `200`.

### Проверка подключений камер из проекта

Источник данных:

- `config/test-cameras.local.json` (локальный список камер для интеграционных проверок).

Проверено сетевое подключение к камерам:

- RTSP порт `554` — **open** для всех камер в списке.
- HTTP порты по defaults:
  - `80` — **open**
  - `8080` — **closed**
  - `443` — **closed**

Итог:

- Сетевой доступ до всех перечисленных камер подтвержден по RTSP (`554`) и HTTP (`80`).

## Инцидент: discover/connect ошибки (глубокий разбор и фиксы)

Проблемы, выявленные в логах:

1. `RateLimitMiddleware` выбрасывал `IllegalArgumentException` (ошибка Redis Range API).
2. Для preflight/ранних ответов middleware пытались повторно писать заголовки (`response already completed`).
3. SQLDelight-запросы к таблице `user` были без кавычек (`user`), что конфликтует с PostgreSQL keyword.
4. На части окружений/старых схем не хватало ожидаемых колонок SQLDelight/Flyway parity.
5. Optional remote data sources в Koin могли ронять создание репозиториев (nullable single edge-case).

Что исправлено:

- `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/RateLimitMiddleware.kt`
  - исправлен диапазон для `zremrangebyscore`;
  - добавлена защита от записи заголовков/ответа при `response.isCommitted`.
- `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/SecurityHeadersMiddleware.kt`
  - добавлена проверка `response.isCommitted` перед добавлением заголовков.
- `shared/src/commonMain/sqldelight/com/company/ipcamera/shared/database/CameraDatabase.sq`
  - таблица и запросы к `"user"` переведены на quoted identifier для PostgreSQL.
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/local/PostgresFlywaySchemaSync.kt`
  - добавлены idempotent `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` для ключевых полей core-таблиц.
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/di/RepositoriesV2Module.kt`
  - добавлен безопасный optional-resolve (`safeGetOptional`) для remote data sources.

Результат после внедрения:

- В логах отсутствуют прежние `RateLimit`/`response already completed` ошибки.
- `MigrationValidator` проходит успешно.
- `CameraService` стартует штатно (`Started camera monitoring`).
- `GET /api/v1/cameras/discover` с валидной авторизацией возвращает корректный ответ `success=true`.

## Discover fallback (known hosts) — внедрено

Чтобы стабилизировать поиск камер в Docker-сети (где multicast discovery часто ограничен), добавлен fallback-механизм:

- если ONVIF discover вернул пустой список, сервер читает `config/test-cameras.local.json` и проверяет TCP-доступность камер;
- в выдачу попадают reachable камеры с маркером:
  - `model = known-host-fallback`
  - `manufacturer = configured`
- fallback включен в manual compose профиле:
  - `DISCOVERY_FALLBACK_ENABLED=true`
  - `DISCOVERY_KNOWN_HOSTS_CONFIG=/app/discovery-config/test-cameras.local.json`
  - volume: `./config:/app/discovery-config:ro`

Проверка:

- `GET /api/v1/cameras/discover` теперь возвращает список известных reachable-камер (например `192.168.10.17`, `192.168.10.20`, ...).

## Снижение шумов в логах — внедрено

- Ожидаемые `401` на probe-эндпоинтах (`/api/v1/auth/ws-token`, `/api/v1/users/me`) переведены в `debug`-уровень.
- Slow-request warning больше не срабатывает для WebSocket-пути `/api/v1/ws`.
- UPnP cleanup warning `Not a member of group` классифицируется как ожидаемый и переводится в `debug`.

## E2E smoke (auth + discover) — добавлен

Для быстрой регрессионной проверки добавлен скрипт:

- `scripts/server-auth-discovery-smoke.ps1`

Проверяемая цепочка:

1. `POST /api/v1/auth/login`
2. `POST /api/v1/auth/refresh`
3. `GET /api/v1/auth/ws-token`
4. `GET /api/v1/events/statistics`
5. `GET /api/v1/cameras/discover?refresh=true`

Запуск:

- `.\scripts\server-auth-discovery-smoke.ps1`

Дополнительно:

- отдельная памятка по Docker-сети и fallback-discovery: `docs/reports/DOCKER_DISCOVERY_NETWORK_MEMO_2026-04-27.md`

## Метрики discover/auth (встроенные) — добавлено

Добавлены лёгкие in-memory метрики для оперативной диагностики без внешнего стека:

- discover:
  - `discoverRequests`
  - `discoverFallbackUsed`
  - `discoverFailures`
- auth:
  - `authLoginSuccess`
  - `authLoginFailure`
  - `authRefreshSuccess`
  - `authRefreshFailure`
  - `authWsTokenSuccess`
  - `authWsTokenFailure`
- events:
  - `eventStatisticsSuccess`
  - `eventStatisticsFailure`

Endpoint:

- `GET /api/v1/health/metrics` (требует JWT + роль `ADMIN`)

## Финальный acceptance-чеклист (manual стенд)

Статус на текущий момент:

- [x] Docker стек поднимается и сервисы `healthy`.
- [x] `POST /api/v1/auth/login` работает с `admin / ADMIN_PASSWORD@`.
- [x] `POST /api/v1/auth/refresh` проходит в cookie-сессии.
- [x] `GET /api/v1/auth/ws-token` успешно отдает токен.
- [x] `GET /api/v1/events/statistics` возвращает `success=true` (без `500`/serialization error).
- [x] `GET /api/v1/cameras/discover?refresh=true` возвращает non-empty список через known-host fallback.
- [x] Лог-шум по ожидаемым `401` probe-эндпоинтам снижен.
- [x] `GET /api/v1/health/metrics` доступен для `ADMIN` и показывает рост счетчиков.
- [x] E2E smoke `scripts/server-auth-discovery-smoke.ps1` проходит стабильно.

## Runtime tuning (prod/staging)

Rate-limit параметры можно калибровать через env без изменения кода:

- `RATE_LIMIT_LOGIN_MAX_ATTEMPTS`
- `RATE_LIMIT_LOGIN_WINDOW_MINUTES`
- `RATE_LIMIT_LOGIN_BLOCK_MINUTES`
- `RATE_LIMIT_GENERAL_MAX_ATTEMPTS`
- `RATE_LIMIT_GENERAL_WINDOW_MINUTES`
- `RATE_LIMIT_GENERAL_BLOCK_MINUTES`
- `RATE_LIMIT_REGISTRATION_MAX_ATTEMPTS`
- `RATE_LIMIT_REGISTRATION_WINDOW_MINUTES`
- `RATE_LIMIT_REGISTRATION_BLOCK_MINUTES`

## Metrics guard (pre-release gate)

Добавлен автоматизированный guard по runtime-метрикам:

- Скрипт: `scripts/server-metrics-guard.ps1`
- Проверяет:
  - `discoverFailures` (threshold по умолчанию `0`)
  - суммарные auth-failures (`authLoginFailure + authRefreshFailure + authWsTokenFailure`, default `5`)
  - `eventStatisticsFailure` (default `0`)

Пример запуска:

- `powershell -ExecutionPolicy Bypass -File scripts/server-metrics-guard.ps1`

Переопределение порогов:

- `powershell -ExecutionPolicy Bypass -File scripts/server-metrics-guard.ps1 -MaxAuthFailures 10 -MaxDiscoverFailures 1`

## Unified pre-release gate

Для единого прогона smoke + metrics guard добавлен orchestrator:

- `scripts/server-pre-release-gate.ps1`

Пример запуска:

- `powershell -ExecutionPolicy Bypass -File scripts/server-pre-release-gate.ps1`

Результат:

- Скрипт возвращает `0` при `GO`, `1` при `NO-GO`.
- Генерирует markdown-отчёт в `docs/reports/DOCKER_PRE_RELEASE_GATE_REPORT_*.md`.
- Генерирует machine-readable JSON рядом: `docs/reports/DOCKER_PRE_RELEASE_GATE_REPORT_*.json`.

## Nightly gate (retry + rotation)

Для регулярного unattended-прогона добавлен wrapper:

- `scripts/server-nightly-gate.ps1`

Что делает:

- запускает `server-pre-release-gate.ps1` с retry/backoff;
- при transient ошибках делает повторные попытки;
- пишет сводку последнего прогона в `docs/reports/DOCKER_PRE_RELEASE_GATE_LAST.md`;
- пишет machine-readable сводку в `docs/reports/DOCKER_PRE_RELEASE_GATE_LAST.json`;
- ротирует старые отчёты `DOCKER_PRE_RELEASE_GATE_REPORT_*.md` (по умолчанию хранит 20 последних).
- опционально отправляет webhook с JSON payload (`-WebhookUrl`).
- в webhook payload добавляет человекочитаемое поле `text` (для Telegram/Slack).
- опционально экспортирует артефакты в `release-build/test/server-docker-gate` (`-ExportToReleaseBuild`).

Пример запуска:

- `powershell -ExecutionPolicy Bypass -File scripts/server-nightly-gate.ps1 -Attempts 3 -InitialBackoffSeconds 10`
- с авто-экспортом в release-build: добавить `-ExportToReleaseBuild`
- отдельная папка экспорта на каждый запуск: добавить `-ExportWithTimestampSubdir`
- strict режим по экспорту: добавить `-FailOnExportError`

Пример с webhook:

- `powershell -ExecutionPolicy Bypass -File scripts/server-nightly-gate.ps1 -WebhookUrl "https://example/hooks/ip-css-gate"`
- строгий режим (падать при ошибке доставки): добавить `-FailOnWebhookError`
- кастомный префикс текста: `-WebhookTextPrefix "[Prod Gate]"`
- расширенный текст по попыткам: добавить `-WebhookIncludeAttemptsTable`
- Telegram Bot API payload (`chat_id + text`):
  - `powershell -ExecutionPolicy Bypass -File scripts/server-nightly-gate.ps1 -WebhookUrl "https://api.telegram.org/bot<TOKEN>/sendMessage" -TelegramBotFormat -TelegramChatId "<CHAT_ID>"`

## Export artifacts to release-build/test

Чтобы зафиксировать server gate артефакты для релизного цикла/CI, добавлен экспорт:

- `scripts/server-export-release-artifacts.ps1`
- по умолчанию копирует последние Docker gate отчеты в `release-build/test/server-docker-gate`.
- создает manifest: `DOCKER_GATE_EXPORT_MANIFEST.md`.
- создает стабильные статус-файлы для CI/dashboard:
  - `server-docker-gate-status.json`
  - `server-docker-gate-status.md`
- поддерживает ротацию артефактов:
  - flat-mode: `-KeepExportReports` (по умолчанию `20`)
  - timestamp-mode: `-KeepExportRuns` (по умолчанию `20`)

Пример запуска:

- `powershell -ExecutionPolicy Bypass -File scripts/server-export-release-artifacts.ps1`
- отдельная папка на каждый запуск: добавить `-IncludeTimestampSubdir`
- ограничить число сохраненных выгрузок: `-KeepExportReports 10` / `-KeepExportRuns 10`

Ручная до-проверка перед эксплуатацией:

- [x] Подтвердить UI-сценарий login/logout/refresh на `server/web` в браузере.
- [ ] Подтвердить доступ к физическим камерам в целевой сети (не только TCP reachability).
- [x] Зафиксировать итоговые артефакты smoke в релизной папке/CI отчете.
