# PostgreSQL: staging cutover, smoke и rehearsal отката (1.5.6)

**Назначение:** воспроизводимые шаги закрытия production path БД для MVP. Код опирается на `DB_MODE`, `DATABASE_URL` / `POSTGRES_URL`, fail-fast в production — см. `server/api/src/main/kotlin/com/company/ipcamera/server/config/ServerConfig.kt` и `DatabaseConfig.kt`.

## 1. Переменные окружения

| Переменная | Назначение |
|------------|------------|
| `DB_MODE` | `embedded` — локальная/dev без обязательного Postgres URL в не-production; `postgres` — явный режим PostgreSQL. В production при пустом значении сервер трактует режим как `postgres` (см. `ServerConfig.fromEnvironment`). |
| `DATABASE_URL` или `POSTGRES_URL` | JDBC URL (обязателен при production **или** при `DB_MODE=postgres` для строгой конфигурации — см. `DatabaseConfig.validateDatabaseRequirementsForServerStartup`). |
| `DATABASE_USER` | Пользователь БД (обязателен в production при валидации). |
| `DATABASE_PASSWORD` или `DB_PASSWORD` | Пароль. |
| `ENABLE_FLYWAY` | По умолчанию `true`; миграции через Flyway с fallback на SQLDelight schema. |
| `DATABASE_READ_REPLICA_URL` | Опционально: read replica (пул только для чтения). |

Пример baseline для HTTPS/staging: [config/https-baseline.example.env](../../config/https-baseline.example.env) (при наличии в репозитории).

## 2. Staging cutover (прямой порядок)

1. **Снимок и бэкап** текущей БД (дамп `pg_dump` или снапшот тома), зафиксировать версию приложения и миграций.
2. **Окно обслуживания** — остановить инстансы API, завершить активные сессии записи по runbook эксплуатации.
3. **Развернуть** сборку с актуальными миграциями; выставить `ENVIRONMENT`/`NODE_ENV` согласно staging; `DB_MODE=postgres`; задать `DATABASE_*`.
4. **Старт сервера** — убедиться в логах: пул Hikari, успешные миграции или явный fallback-сообщение; health `/api/v1/health/ready` с `database: OK`.
5. **Smoke:** создание камеры, discovery (если применимо), одна запись, чтение событий, логин веб-клиента.

## 3. Rehearsal отката (обязательно до production)

1. Остановить API.
2. Вернуть предыдущий артефакт (JAR/контейнер) и **предыдущий** набор переменных (`DB_MODE=embedded` или старый URL), если откат на SQLite допустим для стенда; либо восстановить **дамп** Postgres на известную точку.
3. Старт, smoke минимальный (health, один read-only запрос).
4. Зафиксировать время отката и проблему в отчёте (шаблон имени для evidence: `POSTGRESQL_FINALIZATION_STAGING_REPORT_*.md` в `docs/reports/` — используется gate `RequirePostgresEvidence` в `scripts/w4-mvp-platform-and-gate.ps1`).

## 4. Связанные документы

- [server/api/POSTGRESQL_MIGRATION_NOTES.md](../../server/api/POSTGRESQL_MIGRATION_NOTES.md)
- [docs/planning/POSTGRESQL_MIGRATION_COMPLETE.md](POSTGRESQL_MIGRATION_COMPLETE.md)
- [docs/ENVIRONMENT_VARIABLES.md](../ENVIRONMENT_VARIABLES.md)
