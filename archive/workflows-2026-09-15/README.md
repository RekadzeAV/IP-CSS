# Архив GitHub Actions workflows — 15.09.2026

**Событие:** по решению владельца каталог `.github/workflows/` актуализирован — удалены 10 устаревших/дублирующих/сломанных workflow-файлов. Сохранено 13 workflow + README. Оригиналы лежат в этом каталоге (без изменений, `git rm` с сохранением истории).

## Состав архива (10 файлов)

| Файл | Причина удаления |
|---|---|
| `build-native.yml` | Дубль нативной сборки: перекрывается `ffmpeg-tests.yml` + `rtsp-native-build.yml` + таска `buildNativeLibraries` (`build.gradle.kts:446`). Никогда не запускался (`NEVER_RUN`) |
| `build-native-libraries.yml` | Дубль нативной сборки (Linux/macOS matrix уже в `rtsp-native-build.yml`); сборка ведётся локально через `scripts/build-video-processing-lib.*`. Никогда не запускался |
| `native-windows-build.yml` | Дубль Windows-нативной сборки (MinGW/msbuild); входил в ту же группу. `startup_failure` при последнем прогоне |
| `rtsp-client-ci.yml` | Смешанная ответственность (native + docs-триггер), дублирует нативные workflows; использовал устаревшие `checkout@v3`/`upload-artifact@v3`/`cache@v3` |
| `cd.yml` | Полный дубль релизного контура `release.yml` (теги `v*.*.*` → Docker + GitHub Release); использовал архивный `actions/create-release@v1` (архивирован GitHub) |
| `python-ci-gates.yml` | Полный дубль `verify-kmp-phase1`-джоба из `ci.yml` (kmp) + дубль daily-гейта `phase1-mvp-verify.yml`; тот же скрипт `verify-kmp-phase1.py --ci-profile` |
| `phase1-mvp-verify.yml` | Полный дубль покрытия `ci.yml` (validate/test/server/android/kmp): те же гейты с меньшей строгостью; ежедневный schedule дублировал nightly |
| `postgres-finalization-one-command-gate.yml` | Одноразовый staging-инструмент (manual-only) по завершённому этапу финализации PostgreSQL; требует staging-URL, которого нет |
| `postgres-finalization-report-validation.yml` | Одноразовый валидатор отчёта того же этапа (manual-only); базовый инвариант-гейт сохранён в `postgres-finalization-gate.yml` |
| `nightly-live-integration.yml` | Требует живых камер (secrets `TEST_RTSP_*`, порт A1) — в GH Actions окружения нет; ручной запуск покрыт таской `ciLiveIntegrationSuite` (`build.gradle.kts:313`) локально |

## Общие проблемы удалённых (подтверждено аудитом 15.09)

- Все были в состоянии `disabled_manually` (отключены вручную в мае–июне 2026 после массовых `startup_failure`, вызванных настройкой репозитория `allowed_actions: local_only` — устранена 15.09).
- 8 из 10 — дубли функциональности сохранённых workflows (`ci.yml`/`nightly.yml`/нативные).
- Часть использовала устаревшие major-версии actions (v2/v3) из блок-листа Dependabot.

## Как восстановить

```powershell
# Одиночный файл
Copy-Item archive/workflows-2026-09-15/<файл>.yml .github/workflows/

# Через git (история сохранена)
git log --oneline --diff-filter=D -- '.github/workflows/<файл>.yml'
git checkout <sha>^ -- .github/workflows/<файл>.yml
```

Перед восстановлением убедитесь, что функциональность не перекрыта сохранёнными workflows (см. таблицу выше) и репозиторий разрешает внешние actions (`Settings → Actions → General → Actions permissions → Allow all`).

## Ссылки

- Актуальный состав и политика версий: [`.github/workflows/README.md`](../../.github/workflows/README.md)
- Манифест архивов: [`ARCHIVE_MANIFEST.md`](../../ARCHIVE_MANIFEST.md)
