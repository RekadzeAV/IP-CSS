# PostgreSQL Finalization Runbook (1.5.6)

## Цель

Довести блок `1.5.6` до production-ready состояния: сервер в продакшене работает в режиме PostgreSQL-only, без неявных fallback на in-memory/embedded контур.

## Что уже зафиксировано в коде

- Добавлен режим `DB_MODE` (`postgres`/`embedded`), по умолчанию для production — `postgres`.
- Добавлен fail-fast preflight на старте приложения:
  - обязательны `DATABASE_URL` (или `POSTGRES_URL`), `DATABASE_USER`, `DATABASE_PASSWORD` (или `DB_PASSWORD`) в production/`DB_MODE=postgres`.
- В production запрещен fallback `ServerUserRepositoryInMemory`.
- Ужесточен bootstrap admin в PostgreSQL:
  - в production требуется `ADMIN_PASSWORD_HASH` или `ADMIN_PASSWORD`;
  - дефолтный development пароль больше не допускается.
- Readiness/health учитывает реальное состояние БД, если ожидается PostgreSQL.

## Обязательные переменные окружения (production)

```bash
ENVIRONMENT=production
NODE_ENV=production
DB_MODE=postgres
DATABASE_URL=jdbc:postgresql://<host>:5432/<db>
DATABASE_USER=<user>
DATABASE_PASSWORD=<password>
ENABLE_FLYWAY=true
```

Рекомендуется:

```bash
ADMIN_PASSWORD_HASH=<bcrypt-hash>
DATABASE_MAX_POOL_SIZE=10
```

## Preflight Checklist (до деплоя)

- [ ] Подготовлена PostgreSQL БД и доступ с узла приложения.
- [ ] Проверены креды и переменные окружения.
- [ ] Включен `DB_MODE=postgres`.
- [ ] Подтверждено, что Flyway миграции доступны.
- [ ] Подготовлен backup текущей базы (если это upgrade, а не fresh install).

## Cutover Procedure (staging -> production)

1. Обновить environment (DB_MODE/DB creds).
2. Развернуть новый build.
3. Дождаться старта приложения и применения миграций.
4. Проверить `/api/v1/health` и `/api/v1/health/ready`.
5. Выполнить smoke сценарии:
   - login;
   - refresh/logout;
   - GET `/api/v1/cameras`;
   - GET `/api/v1/events`;
   - GET `/api/v1/recordings`.
6. Проверить DB monitoring endpoints (под admin):
   - `/api/v1/database/health`;
   - `/api/v1/database/migrations`;
   - `/api/v1/database/pool/stats`.

## Rollback Procedure

1. Остановить новый deployment.
2. Восстановить предыдущий стабильный build.
3. Если миграции изменили схему несовместимо — восстановить БД из pre-cutover backup.
4. Повторно проверить health/readiness и ключевые smoke сценарии.

## CI Gate

Для контрактной проверки добавлен workflow:

- `.github/workflows/postgres-finalization-gate.yml`

И скрипт инвариантов:

- `scripts/ci/check-postgres-finalization.sh`

Эти проверки подтверждают наличие критичных guardrail-изменений по `1.5.6`.

## Staging Report

Для формальной фиксации cutover/rollback используйте шаблон:

- `docs/POSTGRESQL_FINALIZATION_STAGING_REPORT_TEMPLATE.md`
- Короткое итоговое решение (после smoke/rollback): `docs/POSTGRESQL_FINALIZATION_GO_NO_GO_TEMPLATE.md`

Быстрый smoke после cutover можно выполнить скриптом:

- `scripts/postgres-finalization-staging-smoke.ps1`

Preflight перед cutover/smoke (Docker + env + API reachability):

- `scripts/postgres-finalization-preflight.ps1`

Пример запуска preflight:

```powershell
.\scripts\postgres-finalization-preflight.ps1 `
  -BaseUrl "http://localhost:8080" `
  -Strict `
  -OutputFile "diagnostics\postgres-finalization\preflight-report.md"
```

Пример запуска:

```powershell
.\scripts\postgres-finalization-staging-smoke.ps1 `
  -BaseUrl "http://localhost:8080" `
  -Username "admin" `
  -Password "<admin-password>" `
  -OutputFile "diagnostics\postgres-finalization\staging-smoke-report.md"
```

По умолчанию скрипт завершается с кодом **`1`**, если API недоступен до проверок или в отчёте **Overall FAIL** (удобно для CI и `run-postgres-finalization-suite.ps1`). Чтобы после записи отчёта всегда получать **`0`**, как раньше, передайте **`-ExitZeroOnReportFail`**.

Если локальный staging runtime ещё не поднят, можно предварительно запустить:

```powershell
.\scripts\start-postgres-finalization-staging.ps1 -BaseUrl "http://localhost:8080" -TimeoutSec 180
```

Для генерации черновика итогового staging отчёта из smoke-результатов:

```powershell
.\scripts\postgres-finalization-generate-staging-report.ps1 `
  -SmokeReport "diagnostics\postgres-finalization\staging-smoke-report.md" `
  -OutputFile "diagnostics\postgres-finalization\staging-final-report.md" `
  -Environment "staging" `
  -BuildCommit "<commit-sha>" `
  -Owner "<owner>" `
  -Reviewer "<reviewer>"
```

Для запуска всего процесса одной командой (smoke + final report):

```powershell
.\scripts\run-postgres-finalization-suite.ps1 `
  -BaseUrl "http://localhost:8080" `
  -Username "admin" `
  -Password "<admin-password>" `
  -Environment "staging" `
  -BuildCommit "<commit-sha>" `
  -Owner "<owner>" `
  -Reviewer "<reviewer>"
```

Для полного one-shot контура (preflight + smoke + final + short go/no-go):

```powershell
.\scripts\run-postgres-finalization-suite.ps1 `
  -AutoStartRuntime `
  -ValidateReport `
  -GenerateGoNoGo `
  -RollbackEvidence "link-or-path-to-rollback-log" `
  -RuntimeStartTimeoutSec 180 `
  -BaseUrl "http://localhost:8080" `
  -Username "admin" `
  -Password "<admin-password>" `
  -Environment "staging" `
  -BuildCommit "<commit-sha>" `
  -Owner "<owner>" `
  -Reviewer "<reviewer>"
```

CI-gate режим (завершать suite с `exit 1`, если решение NO-GO):

```powershell
.\scripts\run-postgres-finalization-suite.ps1 `
  -GenerateGoNoGo `
  -FailOnNoGo `
  -RollbackEvidence "link-or-path-to-rollback-log" `
  -BaseUrl "http://localhost:8080" `
  -Username "admin" `
  -Password "<admin-password>" `
  -Environment "staging" `
  -BuildCommit "<commit-sha>" `
  -Owner "<owner>" `
  -Reviewer "<reviewer>"
```

Автогенерация короткого GO/NO-GO после staging suite:

```powershell
.\scripts\postgres-finalization-generate-go-no-go.ps1 `
  -PreflightReport "diagnostics\postgres-finalization\preflight-report.md" `
  -SmokeReport "diagnostics\postgres-finalization\staging-smoke-report.md" `
  -FinalReport "diagnostics\postgres-finalization\staging-final-report.md" `
  -RollbackEvidence "link-or-path-to-rollback-log" `
  -OutputFile "diagnostics\postgres-finalization\go-no-go.md" `
  -Environment "staging" `
  -BuildCommit "<commit-sha>" `
  -Owner "<owner>" `
  -Reviewer "<reviewer>"
```

При необходимости пропустить preflight внутри suite:

```powershell
.\scripts\run-postgres-finalization-suite.ps1 -SkipPreflight ...
```

Для полностью автоматического прогона с попыткой поднять runtime:

```powershell
.\scripts\run-postgres-finalization-suite.ps1 `
  -AutoStartRuntime `
  -ValidateReport `
  -RuntimeStartTimeoutSec 180 `
  -BaseUrl "http://localhost:8080" `
  -Username "admin" `
  -Password "<admin-password>" `
  -Environment "staging" `
  -BuildCommit "<commit-sha>" `
  -Owner "<owner>" `
  -Reviewer "<reviewer>"
```

Проверка финального отчёта отдельно (с exit code для CI):

```powershell
.\scripts\validate-postgres-finalization-report.ps1 `
  -ReportPath "diagnostics\postgres-finalization\staging-final-report.md"
```

Ручной GitHub gate (workflow_dispatch):

- `.github/workflows/postgres-finalization-report-validation.yml`
- Input: `report_path` (путь до итогового staging отчёта)

One-command GitHub gate (workflow_dispatch + artifacts):

- `.github/workflows/postgres-finalization-one-command-gate.yml`
- Inputs: `base_url`, `username`, `environment`, `owner`, `reviewer`, `rollback_evidence`, `run_on_self_hosted`, `password_env_var`, `admin_password`
- Password source order: runner env var `password_env_var` (default `IPCSS_ADMIN_PASSWORD`) -> fallback input `admin_password`
- `run_on_self_hosted=true` включает второй job на `self-hosted` runner (без `-SkipPreflight`, с `-AutoStartRuntime`)
- Self-hosted job использует `environment: ${{ inputs.environment }}`; настройте protection rules для `staging`/`preprod` в GitHub Environments

Environment policy (recommended):

- `staging`: минимум 1 approval, допускается запуск on-demand для smoke/cutover rehearsal
- `preprod`: минимум 2 approvals + обязательный `rollback_evidence`
- Для self-hosted runner хранить пароль в переменной окружения сервиса раннера (например `IPCSS_ADMIN_PASSWORD`), а не во входных параметрах запуска

## Критерии "1.5.6 = 100%"

- [ ] Production стартует только с PostgreSQL-конфигурацией.
- [ ] Нет production fallback на in-memory user repository.
- [ ] Admin bootstrap в production безопасен (без dev-password fallback).
- [ ] Readiness корректно отражает доступность PostgreSQL.
- [ ] CI gate `postgres-finalization-gate` стабильно зеленый.
- [ ] Выполнен staging cutover + rollback rehearsal.
