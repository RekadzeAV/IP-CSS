# 🔐 Ротация секретов IP-CSS — журнал (шаблон)

> Документ ротации секретов. **Реальные значения хранятся локально** в `SECRETS_ROTATION.local.md` (в `.gitignore`, правило `*.local.*`).
> Здесь (в git) — только плейсхолдеры и карта замены.
> **Камеры сети 192.168.10.0/24 НЕ тронуты** — действующий сетевой контур.

## Новые значения (сгенерированы 07.09.2026) — СМ. `SECRETS_ROTATION.local.md`

| Переменная | Новое значение | Назначение |
|---|---|---|
| `DB_PASSWORD` (PostgreSQL) | `replace-with-strong-db-password` | доступ к PostgreSQL |
| `REDIS_PASSWORD` (Redis) | `replace-with-strong-redis-password` | доступ к Redis |
| `JWT_SECRET` | `replace-with-strong-jwt-secret` | подпись JWT-токенов API |
| `DATA_ENCRYPTION_KEY` | `replace-with-64-hex` | шифрование данных (AES-256) |
| `ADMIN_PASSWORD` | `replace-with-admin-password` | начальный логин администратора |

> ⚠️ `DATA_ENCRYPTION_KEY` — смена ключа шифрования делает ранее зашифрованные данные недоступными.

## Карта замены (реальные файлы — все локальные, gitignored)

| Файл | Статус |
|---|---|
| `.env` | ✅ обновлено 07.09 |
| `.env.docker-manual` | ✅ обновлено 07.09 (в `git`: только `.example`) |
| `config/postgresql.env` | ✅ обновлено 07.09 (в `git`: только `.example`) |
| `credentials-all.env` | ✅ обновлено 14.09 (кроме камер!) |
| `ai-agent/src/main.py` | ✅ `admin/admin` + `JWT_SECRET` вынесены в `ai-agent/.env` |
| `docker-compose.yml` | ✅ без изменений (читает из env, значения не зашиты) |

## Затронутые области авторизации
- PostgreSQL (HikariCP пул), Redis (Lettuce), JWT API (Bearer/cookie), Админ-консоль, AI-агент (FastAPI JWT).

## Безопасность: что выведено из git
- `.env.docker-manual`, `config/postgresql.env` — только `*.example`-версии с плейсхолдерами.
- `.gitignore`: `config/*.env`, `config/test-cameras*-network.json`, `test-cameras.rtsp.json`, `*.local.*` — реальные секреты/URL камер не попадают в git.
- `SECRETS_ROTATION.local.md` — локальная копия с реальными значениями (gitignored).

## ⚠️ Важно
- Старые пароли засветились в git-истории (коммит `e06c7888`) → **не использовать их повторно**. При необходимости — очистить историю (rebase/filter-repo) перед публикацией.
- После применения новых значений — перезапуск всех контуров (docker-compose, сервер, ai-agent).