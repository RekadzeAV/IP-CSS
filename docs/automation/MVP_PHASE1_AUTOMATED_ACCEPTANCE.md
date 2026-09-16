# Автоматическая приёмка Фазы 1 (MVP) — замена ручных этапов

**Назначение:** зафиксировать команды и артефакты, которые **заменяют** пункты плана с «ручной проверкой» там, где это возможно без доступа к конкретному железу.

**Связь с планом:** [PHASE1_MVP_TO_100_PLAN.md](../planning/PHASE1_MVP_TO_100_PLAN.md), [TODO.md](../../archive/docs-duplicates-2026-08-08/TODO.md). Границы MVP: [MVP_PHASE1_SCOPE_BOUNDARY.md](../planning/MVP_PHASE1_SCOPE_BOUNDARY.md). ONVIF events: [MVP_ONVIF_EVENTS_VERIFICATION.md](MVP_ONVIF_EVENTS_VERIFICATION.md).

---

## 1. Единый контур (CI / локально)

| Было (ручное) | Автоматическая замена |
|----------------|------------------------|
| **W4-3** Integration-тесты API / БД / критичный путь | `./gradlew mvpAutomatedAcceptance` (см. ниже) + job `mvp-automated-acceptance` в CI |
| **W3-4** Ручной perf-pass по чеклисту | Минимум: `npm run build` + `npm test` в `server/web` (регресс сборки и unit-тестов). Полный Lighthouse по-прежнему опционален для «идеальных» метрик — см. [WEB_INTERFACE_PERF_MANUAL_CHECKLIST.md](../status/WEB_INTERFACE_PERF_MANUAL_CHECKLIST.md) § Автоматизация |
| **W2-2 / P1-2** ONVIF Events на камере | Без реальной камеры: юнит/интеграционные тесты `OnvifEvent*` + lifecycle (`CameraEventMonitoringServiceJvmTest`). С конфигом камеры: `scripts/onvif-events-api-verification.ps1` (HTTP + optional WS e2e + latency gate). Сводка шагов: [MVP_ONVIF_EVENTS_VERIFICATION.md](MVP_ONVIF_EVENTS_VERIFICATION.md) |
| **W4-5** Матрица GO/NO-GO | `scripts/w4-mvp-platform-and-gate.ps1` (опционально) + зелёный контур приёмки; в CI — артефакт **`PHASE1_GO_NO_GO_SUMMARY_*.md|json`** из `generate-phase1-go-no-go-summary.ps1` после `mvp-automated-acceptance.sh --generate-phase1-summary`. Чеклист: [RELEASE_GO_NO_GO_CHECKLIST.md](../planning/RELEASE_GO_NO_GO_CHECKLIST.md) |

В **`.github/workflows/ci.yml`** задано **`concurrency`** с `cancel-in-progress: true`: новый push в ту же ветку или обновление PR отменяет незавершённый предыдущий прогон этого workflow. Сборка **NAS** (`.github/workflows/build-nas-packages.yml`) и **CD** (`.github/workflows/cd.yml`) для JDK/Gradle cache используют тот же composite **`.github/actions/setup-jdk-gradle`**, что и основной CI.

Для сетевого слоя `1.4` доступны opt-in automation jobs:

- `network-layer-live-checks` (RTSP/WS live, requires repo var + secrets)
- `network-layer-status-sync-audit` (strict legacy-conflict gate)
- `network-layer-aggregate-decision` (strict aggregate `GO/CONDITIONAL/NO_GO` gate + decision artifact)

Локальный one-command smoke:

```powershell
.\scripts\run-network-layer-1-4-local-smoke.ps1
```

Runbook: [NETWORK_LAYER_STATUS_SYNC_RUNBOOK.md](../status/NETWORK_LAYER_STATUS_SYNC_RUNBOOK.md)

---

## 2. Gradle

```bash
./gradlew mvpAutomatedAcceptance
```

Выполняет:

- `:shared:desktopTest` — JVM-интеграционные тесты (в т.ч. миграции БД, репозитории на SQLite)
- `:core:network:desktopTest` — JVM-тесты сетевого слоя и JNI-моста RTSP (`RtspClientTest`, `NativeRtspClientBridgeJvmTest`; при сборке `video_processing` в окружении можно включить жёсткую проверку `create()!=0` через env **`REQUIRE_NATIVE_RTSP_BRIDGE=true`**)
- `:server:api:test` — тесты сервера (в т.ч. сервисы, маршруты; публичный health smoke: `HealthLiveIntegrationTest`, `HealthBasicIntegrationTest`, `HealthReadyIntegrationTest`; HLS HTTP: `HlsStreamRoutesIntegrationTest`, `HlsPublicRoutesIntegrationTest`, `HlsRecordingRoutesIntegrationTest`)

**Паритет с Gradle-шагами job’ов `.github/workflows/ci.yml` (без Python/detekt/npm):**

- `./gradlew ciKmpLinuxSanity` — агрегат Linux KMP + metadata + `checkKotlinGradlePluginConfigurationErrors` (часть `code-quality`).
- `./gradlew ciBuildCoreSharedModules` — `:core:common:build`, `:core:network:build`, `:shared:build` в одном графе (job `build-and-test`; отдельные `:test` не нужны — входят в `build` → `check`).

Дополнительные runbook’и после зелёного `mvpAutomatedAcceptance`:

- PostgreSQL cutover / rollback: [POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md](../planning/POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md)
- Security field validation: [SECURITY_MVP_FIELD_VALIDATION_RUNBOOK.md](../planning/SECURITY_MVP_FIELD_VALIDATION_RUNBOOK.md)
- W4 gate (platform smoke с `-CompileOnly` / `-CompileAndTestOnly` в профилях): [W4_MVP_PLATFORM_SMOKE_RUNBOOK.md](../reports/W4_MVP_PLATFORM_SMOKE_RUNBOOK.md)

### 2.1 KMP stabilization gates (Phase 1)

Перед финальным GO/NO-GO для фазы рекомендуется прогонять единый one-shot verifier:

```bash
python scripts/ci/verify-kmp-phase1.py
```

Платформенные обёртки:

```powershell
.\scripts\ci\verify-kmp-phase1.ps1
```

Справка по флагам PowerShell-обёртки (без запуска Python/Gradle): `.\scripts\ci\verify-kmp-phase1.ps1 -ShowHelp` (эквивалентно `-Help`).

```bash
./scripts/ci/verify-kmp-phase1.sh
```

Быстрый режим без Gradle:

- Python: `python scripts/ci/verify-kmp-phase1.py --skip-gradle`
- PowerShell: `.\scripts\ci\verify-kmp-phase1.ps1 -SkipGradle`
- Bash: `./scripts/ci/verify-kmp-phase1.sh --skip-gradle`
- Strict runtime-matrix mode:
  - Python: `python scripts/ci/verify-kmp-phase1.py --strict-runtime-matrix`
  - PowerShell: `.\scripts\ci\verify-kmp-phase1.ps1 -StrictRuntimeMatrix`
  - Bash: `./scripts/ci/verify-kmp-phase1.sh --strict-runtime-matrix`

CI Python-gates используют эквивалент:

```bash
python scripts/ci/verify-kmp-phase1.py --ci-profile
```

Сохранить machine-readable отчёт:

```bash
python scripts/ci/verify-kmp-phase1.py --ci-profile --report-json diagnostics/kmp/verify-report.json
```

В CI отчёт сохраняется и публикуется как artifact:

- Artifact name: `kmp-phase1-verify-report`
- File: `diagnostics/kmp/verify-report.json`
- Markdown summary: `diagnostics/kmp/verify-report.md` (также добавляется в GitHub Actions Job Summary)

Платформенные аналоги CI-profile:

- PowerShell: `.\scripts\ci\verify-kmp-phase1.ps1 -CiProfile`
- Bash: `./scripts/ci/verify-kmp-phase1.sh --ci-profile`

Дополнительно для конфигов matrix:

```bash
python scripts/ci/check-video-runtime-matrix-config.py --root .
python scripts/ci/validate-video-e2e-profile.py --root .
```

---

## 3. Web (`server/web`)

```bash
cd server/web
npm ci
npm run build
npm test
```

Или одной командой из корня репозитория: `scripts/ci/mvp-automated-acceptance.sh` (Linux/macOS) / `scripts/ci/mvp-automated-acceptance.ps1` (Windows). Оба скрипта по умолчанию выполняют **Gradle + web + video gate** (через `pwsh` / `powershell` для `.ps1` на Linux/macOS; на GitHub `ubuntu-latest` PowerShell уже в образе).

В **GitHub Actions** (`GITHUB_ACTIONS` задан) video gate подставляет **`config/video-e2e-acceptance-profile.mvp-ci.json`**: все контроли 1.8.x в отчёте считаются *optional* для profile-aware решения, чтобы job не требовал заранее выгруженных `diagnostics/network-smoke` и long-run matrix. Локально и на self-hosted по-прежнему используется `config/video-e2e-acceptance-profile.local.json`, если не задана переменная **`MVP_VIDEO_ACCEPTANCE_PROFILE`** (явный путь к JSON-профилю — перекрывает и CI).

Опционально после video gate — сводный Phase1 отчёт (по умолчанию профиль **`MvpCi`**, не валит приёмку при агрегате `CONDITIONAL`; `NO_GO` — exit `2`):

```bash
bash scripts/ci/mvp-automated-acceptance.sh --generate-phase1-summary
bash scripts/ci/mvp-automated-acceptance.sh --generate-phase1-summary --phase1-summary-profile Strict
```

```powershell
.\scripts\ci\mvp-automated-acceptance.ps1 -GeneratePhase1Summary
.\scripts\ci\mvp-automated-acceptance.ps1 -GeneratePhase1Summary -Phase1SummaryProfile Strict
```

Windows note: `mvp-automated-acceptance.ps1` performs `npm ci --include=dev` and includes retry with `node_modules` cleanup to mitigate transient `ENOTEMPTY` lock errors in `server/web/node_modules`. Bash-скрипт использует тот же `npm ci --include=dev` и двухпопыточный retry.

---

## 4. ONVIF Events (камера в сети)

Требуется `config/test-cameras.local.json` и запущенный API с `ONVIF_EVENTS_ENABLED=true`.

```powershell
.\scripts\onvif-events-api-verification.ps1 `
  -AdminPassword <pwd> `
  -EnableWebSocketCheck `
  -MaxEventWaitSeconds 30 `
  -PollIntervalMs 5000 `
  -PullTimeoutMs 500 `
  -LatencyBufferMs 10000
```

Старый путь `onvif-manual-verification.ps1` вызывает тот же сценарий.

### 4.1 Resilience-pass (restart / re-subscribe)

Для короткой автоматической проверки устойчивости после baseline:

```powershell
.\scripts\onvif-events-resilience-verification.ps1 `
  -ApiBase "http://localhost:8080" `
  -AdminUser "admin" `
  -AdminPassword "<pwd>" `
  -CameraIndex 0 `
  -WaitSeconds 15
```

Опционально можно передать команду рестарта API:

```powershell
.\scripts\onvif-events-resilience-verification.ps1 `
  -AdminPassword "<pwd>" `
  -RestartCommand ".\scripts\restart-api-local.ps1" `
  -RestartWaitSeconds 20
```

`restart-api-local.ps1` лежит в репозитории как **заглушка-хук**: задайте `IPCSS_RESTART_API_COMMAND` (PowerShell-выражение для рестарта вашего API) либо вызывайте скрипт с `-Command '...'`. Иначе скрипт завершится с кодом `1`. Справка: `.\scripts\restart-api-local.ps1 -ShowHelp`.

Артефакты сохраняются в `diagnostics/onvif-events` (`*.json` + `*summary.md`).

Поля в JSON-артефакте API-проверки:

- `websocketCheckEnabled`, `websocketStatus`, `websocketEventReceived`, `websocketEventType`
- `latencyMeasured`, `latencyMs`, `latencyThresholdMs`, `latencyStatus`

Если API/камера недоступны в момент запуска и нужно не падать, а зафиксировать `NOT_RUN/PARTIAL`:

```powershell
.\scripts\onvif-events-resilience-verification.ps1 `
  -AdminPassword "<pwd>" `
  -AllowApiUnavailable
```

Скрипт в этом режиме также вызывает `generate-phase1-onvif-evidence-report.ps1` (можно отключить: `-GenerateEvidenceReport:$false`).

### 4.2 Unified evidence report (1.4.3 + 1.4.1 + UI regression)

После прогонов можно сформировать единый отчёт:

```powershell
.\scripts\generate-phase1-onvif-evidence-report.ps1 `
  -Onvif143Status PARTIAL `
  -Unit141Status PASS `
  -UiRegressionStatus PASS `
  -Notes "Local run; API diagnostics from latest onvif-events artifacts"
```

По умолчанию отчёт сохраняется в `docs/reports/ONVIF_PHASE1_EVIDENCE_REPORT_<date>.md`.

### 4.3 One-command orchestrator

Для последовательного запуска `1.4.1` + UI regression + `1.4.3` и автоматической генерации unified report:

```powershell
.\scripts\run-phase1-onvif-acceptance.ps1 `
  -ApiBase "http://localhost:8080" `
  -AdminUser "admin" `
  -AdminPassword "<pwd>" `
  -CameraIndex 0 `
  -WaitSeconds 15 `
  -EnableWebSocketCheck `
  -MaxEventWaitSeconds 30 `
  -PollIntervalMs 5000 `
  -PullTimeoutMs 500 `
  -LatencyBufferMs 10000 `
  -AllowApiUnavailable $true `
  -FailOnNoGo
```

Скрипт:
- запускает preflight `validate-onvif-test-camera-config.ps1`;
- запускает `OnvifEventMapperTest`, `OnvifEventSubscriptionServiceTest`, `CameraEventMonitoringServiceJvmTest`;
- запускает `server/web` regression `eventsSlice.test.ts` + `useWebSocket.test.tsx`;
- выполняет resilience-проход для ONVIF (soft-mode при `-AllowApiUnavailable`);
- обновляет `docs/reports/ONVIF_PHASE1_EVIDENCE_REPORT_<date>.md`;
- сохраняет machine-readable итог в `diagnostics/onvif-events/phase1-onvif-acceptance-<run-id>.json`.

В JSON также рассчитывается gate-решение:
- `GO`: `1.4.1=PASS`, `uiRegression=PASS`, `1.4.3=PASS`
- `CONDITIONAL`: core-проверки PASS, но `1.4.3` в `PARTIAL`/`NOT_RUN`
- `NO_GO`: любой из блоков в `FAIL`

Дополнительно для `1.4.3`:
- при включённом `-EnableWebSocketCheck` обе фазы resilience должны дать `websocketStatus=PASS`;
- latency gate не должен дать `latencyStatus=FAIL` ни в baseline, ни в re-subscribe фазе;
- если summary `PASS`, но WS/latency условия нарушены, итог `1.4.3` понижается до `PARTIAL`.

Дополнительно в JSON пишется `statuses.preflight` и блок `preflight` с деталями precheck конфига камер.

Для CI:
- `-FailOnNoGo` -> exit code `2` при `NO_GO`
- `-FailOnConditional` -> exit code `3` при `CONDITIONAL`
- `-RequireOnvifEvidence` -> повышает требование к `1.4.3`: если статус `PARTIAL/NOT_RUN`, итог сразу `NO_GO` (вместо `CONDITIONAL`)

### 4.4 Quick runbook (real camera stand)

Минимальный операционный сценарий для целевого `GO`:

1. Подготовить:
   - API запущен и доступен (пример: `http://localhost:8080`);
   - `ONVIF_EVENTS_ENABLED=true`;
   - заполнен `config/test-cameras.local.json`;
   - есть admin пароль (`$env:IPCSS_ADMIN_PASSWORD` или `-AdminPassword`).

   Быстрый preflight:

```powershell
.\scripts\validate-onvif-test-camera-config.ps1
```

2. Выполнить единый прогон:

```powershell
.\scripts\run-phase1-onvif-acceptance.ps1 `
  -ApiBase "http://localhost:8080" `
  -AdminUser "admin" `
  -AdminPassword "<pwd>" `
  -CameraIndex 0 `
  -WaitSeconds 15 `
  -EnableWebSocketCheck `
  -MaxEventWaitSeconds 30 `
  -PollIntervalMs 5000 `
  -PullTimeoutMs 500 `
  -LatencyBufferMs 10000 `
  -FailOnNoGo `
  -FailOnConditional
```

Альтернатива (без передачи пароля в командной строке):

```powershell
.\scripts\run-phase1-onvif-acceptance-secure.ps1 `
  -ApiBase "http://localhost:8080" `
  -AdminUser "admin" `
  -CameraIndex 0 `
  -WaitSeconds 15 `
  -EnableWebSocketCheck `
  -FailOnNoGo `
  -FailOnConditional
```

Поведение `run-phase1-onvif-acceptance-secure.ps1`:
- если задан `IPCSS_ADMIN_PASSWORD`, использует его (подходит для non-interactive/CI);
- если env не задан, безопасно запрашивает пароль интерактивно как `SecureString`.
- для strict non-interactive режима используйте `-NoPrompt` (скрипт завершится ошибкой, если env-пароль не задан).

Пример non-interactive запуска:

```powershell
$env:IPCSS_ADMIN_PASSWORD = "<pwd>"
.\scripts\run-phase1-onvif-acceptance-secure.ps1 `
  -ApiBase "http://localhost:8080" `
  -AdminUser "admin" `
  -CameraIndex 0 `
  -WaitSeconds 15 `
  -EnableWebSocketCheck `
  -NoPrompt `
  -RequireOnvifEvidence `
  -FailOnNoGo `
  -FailOnConditional
```

3. Проверить артефакты:
   - `docs/reports/ONVIF_PHASE1_EVIDENCE_REPORT_<date>.md`
   - `diagnostics/onvif-events/onvif-resilience-<run-id>-summary.md`
   - `diagnostics/onvif-events/phase1-onvif-acceptance-<run-id>.json`

В acceptance JSON секция `artifacts` включает self-reference (`acceptanceJson`) и путь к summary/report.
Секция `policy` фиксирует режим принятия решения (например, `requireOnvifEvidence=true/false`).

4. Критерий целевого `GO`:
   - `1.4.1 = PASS`
   - `uiRegression = PASS`
   - `1.4.3 = PASS`
   - в JSON: `decision.goNoGo = "GO"`

5. Быстрый просмотр последнего решения:

```powershell
.\scripts\show-latest-phase1-onvif-decision.ps1
```

One-command запуск (прогон + вывод решения):

```powershell
.\scripts\run-phase1-onvif-gate.ps1 `
  -ApiBase "http://localhost:8080" `
  -AdminUser "admin" `
  -EnableWebSocketCheck `
  -RequireOnvifEvidence `
  -NoPrompt `
  -FailOnNoGo `
  -FailOnConditional
```

Machine-readable режим one-command wrapper:

```powershell
.\scripts\run-phase1-onvif-gate.ps1 `
  -NoPrompt `
  -RequireOnvifEvidence `
  -ShowLatestOnRunError `
  -AsJson
```

`-AsJson` возвращает единый объект:
- `run` — результат запуска acceptance (`failed`, `error`, `showLatestOnRunError`);
- `decision` — последний gate snapshot (`statuses`, `decision`, `policy`, `diagnostics`, `artifacts`);
- `artifactsValidation` — результат `validate-phase1-onvif-artifacts.ps1`;
- `artifactsValidationExitCode` — код валидации артефактов (`0/1/2`).

Коды выхода `run-phase1-onvif-gate.ps1`:
- `0`: запуск acceptance не упал (дальнейшая gate-семантика определяется полями `decision` и флагами `FailOnNoGo/FailOnConditional`);
- `1`: acceptance-run завершился ошибкой (при `-ShowLatestOnRunError` JSON всё равно печатается);
- `2/3`: возможны при пробросе из acceptance-слоя (`FailOnNoGo` / `FailOnConditional`).

По умолчанию `run-phase1-onvif-gate.ps1` дополнительно выполняет `validate-phase1-onvif-artifacts.ps1`.
Отключение post-check: `-SkipArtifactsValidation`.
Если acceptance-run завершился ошибкой, можно добавить `-ShowLatestOnRunError`, чтобы скрипт всё равно вывел последнее известное решение и диагностику.

Unified CLI help for ONVIF wrappers/utilities:

```powershell
.\scripts\run-phase1-onvif-acceptance.ps1 -ShowHelp
.\scripts\run-phase1-onvif-acceptance-secure.ps1 -ShowHelp
.\scripts\run-phase1-onvif-gate.ps1 -ShowHelp
.\scripts\show-latest-phase1-onvif-decision.ps1 -ShowHelp
.\scripts\validate-phase1-onvif-artifacts.ps1 -ShowHelp
.\scripts\onvif-events-api-verification.ps1 -ShowHelp
.\scripts\onvif-events-resilience-verification.ps1 -ShowHelp
.\scripts\validate-onvif-test-camera-config.ps1 -ShowHelp
.\scripts\generate-phase1-onvif-evidence-report.ps1 -ShowHelp
.\scripts\onvif-manual-verification.ps1 -ShowHelp
.\scripts\restart-api-local.ps1 -ShowHelp
```

Для `onvif-events-api-verification.ps1` эквивалентно: `-Help` (тот же вывод, что и `-ShowHelp`).

`onvif-manual-verification.ps1` — совместимое имя: печатает одну строку о legacy-обёртке и вызывает `onvif-events-api-verification.ps1 -ShowHelp` (остальные аргументы при `-ShowHelp` не пробрасываются).

Для машинного формата:

```powershell
.\scripts\show-latest-phase1-onvif-decision.ps1 -AsJson
```

CI-режим (падение по решению):

```powershell
.\scripts\show-latest-phase1-onvif-decision.ps1 -FailOnNoGo -FailOnConditional
```

- `NO_GO` -> exit code `2`
- `CONDITIONAL` -> exit code `3`

Проверка целостности артефактов (после прогона):

```powershell
.\scripts\validate-phase1-onvif-artifacts.ps1
```

Коды выхода:
- `0`: PASS
- `1`: WARN (некритичные расхождения/отсутствующие optional поля)
- `2`: FAIL (критичная несогласованность артефактов)

---

## 5. Платформенный gate (сборка)

```powershell
.\scripts\w4-mvp-platform-and-gate.ps1 -Full -IncludeWeb
```

---

## 6. Что остаётся ручным

- Длительные прогоны видео (long-run HLS), поведение на конкретной модели камеры без эмулятора.
- Полный Lighthouse в «обычном» Chrome при необходимости валидных LCP/INP (см. perf-чеклист).

### 6.1 Semi-automated long-run for 1.8

Для снижения риска `1.8 runtime long-run / platform smoke` используйте:

```powershell
.\scripts\video-runtime-longrun-smoke.ps1 `
  -BaseUrl "http://localhost:8080" `
  -Username "admin" `
  -Password "<admin-password>" `
  -DurationMinutes 30 `
  -IntervalSec 30 `
  -HealthPath "/api/v1/health/ready" `
  -PlaylistUrls @("http://localhost:8080/api/v1/hls/cam-1/playlist.m3u8")
```

Отчёт сохраняется в `diagnostics/video-longrun/video-longrun-report-*.md` и может использоваться как evidence для `1.8.2/1.8.4/1.8.5/1.8.6`.

### 6.2 Platform smoke matrix wrapper (desktop/android/self-hosted)

Для прогона нескольких long-run сценариев одной командой:

1) Скопируйте `config/video-runtime-matrix.example.json` в `config/video-runtime-matrix.local.json` и заполните реальными URL/паролями.

2) Запустите:

```powershell
.\scripts\video-runtime-platform-matrix.ps1 `
  -ConfigPath "config\video-runtime-matrix.local.json" `
  -OutputDir "diagnostics\video-longrun-matrix"
```

Итоговый агрегированный отчёт: `diagnostics/video-longrun-matrix/<run-id>/video-runtime-matrix-summary.md`.

---

## 7. Windows: конфликт при параллельных запусках

Если два процесса Gradle одновременно выполняют `:shared:desktopTest`, возможна ошибка удаления `shared/build/test-results/desktopTest/...` (файл занят). Завершите лишний `gradlew` (`.\gradlew --stop`) и повторите запуск.

В модулях **`shared`**, **`core:common`** и **`core:network`** для задач `Test` задан уникальный `binaryResultsDirectory` на конфигурацию (`test-results/<task>-binary-<nanoTime>`), чтобы Gradle не пытался удалять заблокированный `output.bin` от предыдущего прогона на Windows.

---

## 8. Platform smoke automation (Android/Desktop/Security)

Для практического закрытия W4-этапов добавлены отдельные smoke-скрипты с machine-readable артефактами. Справка по CLI: **§8.10–8.14** (`scripts\`), **§8.15** (`native\`).

### 8.1 Android video/background smoke

```powershell
.\scripts\android-video-background-smoke.ps1
```

- Артефакты: `diagnostics/platform-smoke/android/android-video-background-smoke-*.md|json`
- Статусы: `PASS` / `PARTIAL` / `FAIL`
- Дополнительно можно запускать install на подключенном устройстве:

```powershell
.\scripts\android-video-background-smoke.ps1 -RunInstallIfDevicePresent
```

### 8.2 Desktop video/event long-run smoke

```powershell
.\scripts\desktop-video-event-longrun-smoke.ps1
```

- Артефакты: `diagnostics/platform-smoke/desktop/desktop-video-event-smoke-*.md|json`
- Без runtime URL формируется `PARTIAL` (compile+tests выполнены, long-run не запущен)
- Для runtime long-run evidence:

```powershell
.\scripts\desktop-video-event-longrun-smoke.ps1 `
  -BaseUrl "http://localhost:8080" `
  -Username "admin" `
  -Password "<pwd>" `
  -PlaylistUrls @("http://localhost:8080/api/v1/hls/cam-1/playlist.m3u8")
```

### 8.3 Security field/staging validation

```powershell
.\scripts\security-field-staging-validation.ps1
```

- Артефакты: `diagnostics/security/security-field-staging-validation-*.md|json`
- Использует `scripts/security-mvp-readiness-check.ps1` и фиксирует наличие конфигов pinning/https baseline

### 8.4 Unified Phase 1 Go/No-Go summary

```powershell
.\scripts\generate-phase1-go-no-go-summary.ps1
# Строгий агрегат (по умолчанию): PARTIAL smoke, CONDITIONAL ONVIF и soft-state `recordingWsGate` (`NOT_FOUND/NOT_RUN/PARTIAL`) опускают итог до CONDITIONAL.
.\scripts\generate-phase1-go-no-go-summary.ps1 -DecisionProfile Strict
# Профиль MVP/CI без камеры/ADB: неблокирующие PARTIAL smoke; ONVIF CONDITIONAL при `requireOnvifEvidence: false` не опускают итог, но `recordingWsGate=NOT_FOUND` дает CONDITIONAL.
.\scripts\generate-phase1-go-no-go-summary.ps1 -DecisionProfile MvpCi
```

- Артефакты: `docs/reports/PHASE1_GO_NO_GO_SUMMARY_<date>.md|json`
- Агрегирует решения из video/security/ONVIF/android/desktop gate-источников + `recordingWsGate` (`release-build/test/recording-ws-gate-wrapper-*.json`); при наличии последнего `diagnostics/platform-smoke/w4-mvp-platform-gate-*.json` добавляет строку **W4 platform gate** и блок `gates.w4PlatformGate` в JSON **только для контекста** (на финальный GO/CONDITIONAL/NO_GO не влияет).

### 8.5 W4 orchestration (extended mode)

```powershell
.\scripts\w4-mvp-platform-and-gate.ps1 `
  -IncludeWeb `
  -IncludeAndroidSmoke `
  -IncludeDesktopSmoke `
  -IncludeSecurityValidation `
  -GenerateGoNoGoSummary
```

Поведение:
- `-RunProfile local` и `staging` передают в summary профиль **`MvpCi`** (`-GoNoGoDecisionProfile MvpCi`); `strict` — **`Strict`**. Принудительно строгий итог при локальном профиле: `-StrictPhase1Summary`.
- `PARTIAL` в optional smoke/security шагах не валит orchestration (если не включён `-FailOnPartial`).
- Для summary-скрипта коды `2` (`NO_GO`) и `3` (`CONDITIONAL`) считаются non-fatal и фиксируются как partial signal.
- При `-FailOnPartial` любой partial-сигнал переводится в fail-fast режим.
- `-TreatPartialAsSuccess` переводит общий exit-code orchestration в `0` (при сохранении статуса `PARTIAL SUCCESS` в отчёте).
- После каждого прогона создаются артефакты:
  - `diagnostics/platform-smoke/w4-mvp-platform-gate-<run-id>.md`
  - `diagnostics/platform-smoke/w4-mvp-platform-gate-<run-id>.json`

### 8.6 W4 profile modes

`scripts/w4-mvp-platform-and-gate.ps1` поддерживает профильные пресеты:

- `-RunProfile custom` (по умолчанию) — только явно переданные флаги.
- `-RunProfile local` — включает:
  - `-IncludeWeb -IncludeAndroidSmoke -IncludeSecurityValidation -GenerateGoNoGoSummary -TreatPartialAsSuccess`
- `-RunProfile staging` — включает:
  - `-IncludeWeb -IncludeAndroidSmoke -IncludeDesktopSmoke -IncludeSecurityValidation -GenerateGoNoGoSummary -RequirePostgresEvidence -TreatPartialAsSuccess`
- `-RunProfile strict` — включает:
  - `-IncludeWeb -IncludeAndroidSmoke -IncludeDesktopSmoke -IncludeSecurityValidation -GenerateGoNoGoSummary -RequirePostgresEvidence -FailOnPartial`

Примеры:

```powershell
.\scripts\w4-mvp-platform-and-gate.ps1 -RunProfile local
```

```powershell
.\scripts\w4-mvp-platform-and-gate.ps1 -RunProfile strict
```

### 8.7 Quick wrapper for profiles

Для короткого запуска профилей без длинной команды:

```powershell
.\scripts\run-w4-profile.ps1 -RunProfile local
```

```powershell
.\scripts\run-w4-profile.ps1 -RunProfile strict
```

```powershell
.\scripts\run-w4-profile.ps1 -RunProfile local -AndroidLocalFrameAnalytics
```

Скрипт:
- вызывает `w4-mvp-platform-and-gate.ps1` с выбранным профилем (опционально: `-NoPlatformSmokeCompileOnlyGate`, `-AndroidLocalFrameAnalytics`, параметры desktop smoke);
- возвращает тот же exit code, что и базовый W4 gate;
- печатает пути к последним W4-артефактам (`.md` и `.json`).

### 8.8 Quick diagnosis of latest W4 run

```powershell
.\scripts\show-latest-w4-gate-status.ps1
```

JSON-режим:

```powershell
.\scripts\show-latest-w4-gate-status.ps1 -AsJson
```

CI-поведение:
- `-FailOnPartial` -> exit `3` when overall is `PARTIAL`
- `-FailOnFail` -> exit `2` when overall is `FAIL`

### 8.9 One-command run + diagnose

```powershell
.\scripts\run-w4-and-diagnose.ps1 -RunProfile local
```

```powershell
.\scripts\run-w4-and-diagnose.ps1 -RunProfile strict
```

```powershell
.\scripts\run-w4-and-diagnose.ps1 -RunProfile local -AndroidLocalFrameAnalytics
```

Скрипт:
- запускает `run-w4-profile.ps1` для выбранного профиля (в т.ч. `-NoPlatformSmokeCompileOnlyGate`, `-AndroidLocalFrameAnalytics`, параметры desktop smoke — как у профиля);
- затем вызывает `show-latest-w4-gate-status.ps1`;
- возвращает финальный exit code, согласованный с исходом W4 запуска.

### 8.10 Unified CLI usage help (`-ShowHelp`)

Для быстрой проверки параметров без запуска gate-процедур. Вывод секций: **Usage**, **Options**, **Exit codes**, при необходимости **Output** / **Steps**.

```powershell
.\scripts\w4-mvp-platform-and-gate.ps1 -ShowHelp
.\scripts\run-w4-profile.ps1 -ShowHelp
.\scripts\show-latest-w4-gate-status.ps1 -ShowHelp
.\scripts\run-w4-and-diagnose.ps1 -ShowHelp
```

### 8.11 CI acceptance and video gate (`-ShowHelp`)

```powershell
.\scripts\ci\mvp-automated-acceptance.ps1 -ShowHelp
.\scripts\ci\verify-kmp-phase1.ps1 -ShowHelp
.\scripts\video-e2e-go-no-go.ps1 -ShowHelp
```

Для `verify-kmp-phase1.ps1` эквивалентно: `-Help`.

### 8.12 Platform smoke, security validation, consolidated summary (`-ShowHelp`)

```powershell
.\scripts\android-video-background-smoke.ps1 -ShowHelp
.\scripts\desktop-video-event-longrun-smoke.ps1 -ShowHelp
.\scripts\security-field-staging-validation.ps1 -ShowHelp
.\scripts\generate-phase1-go-no-go-summary.ps1 -ShowHelp
.\scripts\security-mvp-readiness-check.ps1 -ShowHelp
.\scripts\video-runtime-longrun-smoke.ps1 -ShowHelp
.\scripts\video-runtime-platform-matrix.ps1 -ShowHelp
.\scripts\recording-ws-lifecycle-acceptance-evidence.ps1 -ShowHelp
.\scripts\run-recording-ws-lifecycle-acceptance.ps1 -ShowHelp
.\scripts\run-recording-ws-gate.ps1 -ShowHelp
.\scripts\show-latest-recording-ws-gate-status.ps1 -ShowHelp
.\scripts\generate-phase1-go-no-go-summary.ps1 -ShowHelp
```

### 8.13 PostgreSQL finalization (1.5.6) (`-ShowHelp`)

Связанный runbook: [POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md](../planning/POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md).

```powershell
.\scripts\postgres-finalization-preflight.ps1 -ShowHelp
.\scripts\postgres-finalization-staging-smoke.ps1 -ShowHelp
.\scripts\postgres-finalization-generate-staging-report.ps1 -ShowHelp
.\scripts\postgres-finalization-generate-go-no-go.ps1 -ShowHelp
.\scripts\run-postgres-finalization-suite.ps1 -ShowHelp
.\scripts\start-postgres-finalization-staging.ps1 -ShowHelp
.\scripts\validate-postgres-finalization-report.ps1 -ShowHelp
```

`run-postgres-finalization-suite.ps1` прерывается с ненулевым кодом, если падает preflight или (при `-AutoStartRuntime`) `start-postgres-finalization-staging.ps1` (нет Docker / ошибка `docker compose` / таймаут health), либо если `postgres-finalization-staging-smoke.ps1` вернул `1` (Overall FAIL или API недоступен; для старого поведения «всегда 0 после отчёта» у smoke есть `-ExitZeroOnReportFail`), а также при ошибке генерации/валидации отчёта или GO/NO-GO.

### 8.14 Локальные конфиги и нативная сборка (`-ShowHelp`)

См. также [VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](../reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md) (`bootstrap-local-test-configs`). Для локальных камер из того же JSON: `network-layer-smoke-test` (порты/ONVIF), `onvif-user-rights-check` (GetServices / Events / PullPoint). Шаблоны конфигов: `setup-test-cameras-config` (корневой `test-cameras-config.json`), `setup-test-cameras` (native `test_config.json`); переменные окружения для VideoDecoder — **dot-source** `. .\scripts\setup-test-environment.ps1`, справка только через `powershell -File ... -ShowHelp`.

```powershell
.\scripts\bootstrap-local-test-configs.ps1 -ShowHelp
.\scripts\check-dependencies.ps1 -ShowHelp
.\scripts\build-native-lib.ps1 -ShowHelp
.\scripts\install-dependencies.ps1 -ShowHelp
.\scripts\quick-install.ps1 -ShowHelp
.\scripts\check-ide-setup.ps1 -ShowHelp
.\scripts\generate-compile-commands.ps1 -ShowHelp
.\scripts\install-build-dependencies.ps1 -ShowHelp
.\scripts\network-layer-smoke-test.ps1 -ShowHelp
.\scripts\onvif-user-rights-check.ps1 -ShowHelp
.\scripts\setup-test-cameras-config.ps1 -ShowHelp
.\scripts\setup-test-cameras.ps1 -ShowHelp
powershell -NoProfile -File .\scripts\setup-test-environment.ps1 -ShowHelp
.\scripts\test-video-decoder.ps1 -ShowHelp
.\scripts\run-ffmpeg-tests.ps1 -ShowHelp
.\scripts\analyze-test-results.ps1 -ShowHelp
.\scripts\build-test-environment.ps1 -ShowHelp
.\scripts\build-video-processing-lib.ps1 -ShowHelp
.\scripts\get-certificate-fingerprint.ps1 -ShowHelp
.\scripts\bootstrap-local-dev-env.ps1 -ShowHelp
.\scripts\run-demo-tests.ps1 -ShowHelp
.\scripts\build-all-native-libs.ps1 -ShowHelp
.\scripts\run-demo-tests-simple.ps1 -ShowHelp
.\scripts\run-real-camera-tests.ps1 -ShowHelp
.\scripts\build-nas-package.ps1 -ShowHelp
.\scripts\analyze-camera-test-results.ps1 -ShowHelp
.\scripts\increment-version.ps1 -ShowHelp
.\scripts\get-release-dir.ps1 -ShowHelp
.\scripts\process-f2-results.ps1 -ShowHelp
.\scripts\monitor-test-performance.ps1 -ShowHelp
.\scripts\profile-video-decoder.ps1 -ShowHelp
.\scripts\monitor-ffmpeg-performance.ps1 -ShowHelp
.\scripts\activate-native-decoder.ps1 -ShowHelp
.\scripts\generate-cinterop-bindings.ps1 -ShowHelp
.\scripts\install-ffmpeg.ps1 -ShowHelp
.\scripts\install-gradle.ps1 -ShowHelp
.\scripts\build-rtsp-native-libs.ps1 -ShowHelp
.\scripts\build-android-native-libs.ps1 -ShowHelp
.\scripts\install-ffmpeg-dev.ps1 -ShowHelp
.\scripts\install-android-sdk.ps1 -ShowHelp
.\scripts\install-vscode-extensions.ps1 -ShowHelp
.\scripts\download-models.ps1 -ShowHelp
.\scripts\download-yolo-models.ps1 -ShowHelp
.\scripts\download-face-recognition-models.ps1 -ShowHelp
.\scripts\download-behavior-analysis-models.ps1 -ShowHelp
.\scripts\download-crowd-density-models.ps1 -ShowHelp
.\scripts\archive-documentation.ps1 -ShowHelp
.\scripts\manage-documentation.ps1 -ShowHelp
.\scripts\update-documentation.ps1 -ShowHelp
.\scripts\update-dates.ps1 -ShowHelp
.\scripts\cleanup-old-branches.ps1 -ShowHelp
.\scripts\create-platform-branches.ps1 -ShowHelp
```

Для `run-ffmpeg-tests.ps1` эквивалентно **`-Help`** / **`-h`**; для `analyze-test-results.ps1` — **`-Help`** (первый позиционный аргумент по-прежнему можно передать как путь к логу без `-InputFile`). Для **`get-certificate-fingerprint.ps1`** рабочий запуск требует **`-Hostname`** (без него — exit `1`); тело скрипта в UTF-8 с BOM из‑за русских сообщений. **`bootstrap-local-dev-env.ps1`** — полный bootstrap Windows (профили `dev` / `test` / `release` / `full`). Для **`run-real-camera-tests.ps1`** справка также через **`-Help`** / **`-h`**. **`build-nas-package.ps1`** — сборка SPK (Synology) и смежные шаги. **`analyze-camera-test-results.ps1`** — сводка по `*_results.json` (после `run-real-camera-tests.ps1`); эквивалентно **`-Help`** / **`-h`**. **`get-release-dir.ps1`** — новая папка под `Release/<platform>/`; путь к платформе можно передать позиционно. **`increment-version.ps1`** — патч-версия в `gradle.properties`; файл в UTF-8 **с BOM** (кириллица в теле). **`process-f2-results.ps1`** — обновление `WEB_INTERFACE_*` из лога F2 (UTF-8 **с BOM**); корень репозитория берётся из расположения скрипта, не из жёсткого пути. **`monitor-test-performance.ps1`** — метрики CSV рядом с native-тестами (**`-Help`** / **`-h`**). **`profile-video-decoder.ps1`** — JVM-профилирование `VideoDecoderPerformanceTest`. **`monitor-ffmpeg-performance.ps1`** — отчёт по FFmpeg/железу (UTF-8 **с BOM**, русский текст в отчёте). **`activate-native-decoder.ps1`** / **`generate-cinterop-bindings.ps1`** — цепочка cinterop (**`-ShowHelp`** ≡ **`-Help`**). **`install-ffmpeg.ps1`** / **`install-gradle.ps1`** — локальная установка FFmpeg и починка Gradle Wrapper (UTF-8 **с BOM**). **`build-rtsp-native-libs.ps1`** — CMake-сборка `native/video-processing` (платформа позиционно или **`-Platform`**). **`build-android-native-libs.ps1`** — NDK-сборка под Android (**`-Arch`** или позиционный аргумент). **`install-ffmpeg-dev.ps1`** / **`install-android-sdk.ps1`** / **`install-vscode-extensions.ps1`** — dev-окружение (UTF-8 **с BOM**). Скрипты **`download-models.ps1`** и **`download-*-models.ps1`** — выгрузка ONNX/моделей в `data/models` (часть с **Read-Host** при перезаписи). **`archive-documentation.ps1`** / **`manage-documentation.ps1`** / **`update-documentation.ps1`** — сопровождение `docs` (UTF-8 **с BOM**); **`update-dates.ps1`** — точечная замена дат от **текущего каталога** (запуск из корня репо); пары «строка→строка» заданы через **`UTF8` + `byte[]`**, чтобы кириллица в исходнике не ломала разбор **PS 5.1**. **`cleanup-old-branches.ps1`** / **`create-platform-branches.ps1`** — вспомогательные git-сценарии (английский вывод).

### 8.15 Заглушки в дереве `native` (`-ShowHelp`)

Скрипты вне `scripts\` (корень **`native\`**): только вспомогательные каталоги для cinterop smoke, не полноценная сборка нативных библиотек.

```powershell
.\native\build-stub-libs.ps1 -ShowHelp
```

**`native/build-stub-libs.ps1`** — создаёт структуры `video-processing` / `analytics` / `codecs` → `lib\windows\x64` (плейсхолдеры; реальные DLL — через **`build-rtsp-native-libs.ps1`** и см. §8.14). Файл в UTF-8 **с BOM**.
