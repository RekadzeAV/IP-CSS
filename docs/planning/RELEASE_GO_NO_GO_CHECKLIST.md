# Go/No-Go Checklist перед публикацией релиза

**Версия проекта:** Alfa-0.1.1  
**Последнее обновление:** 27 April 2026

## Цель

Единый чеклист финального решения **GO/NO-GO** перед публикацией релизных артефактов.

> Текущий синхронизированный статус (2026-04-27): packaging precheck `PASS`, итоговый program gate `NO-GO` до закрытия runtime/matrix и field validation evidence.

## 1) Build Gate (обязательный)

- [ ] Android APK: `:android:app:assembleRelease` — `BUILD SUCCESSFUL`
- [ ] Android AAB: `:android:app:bundleRelease` — `BUILD SUCCESSFUL`
- [ ] Desktop x86_64: `:platforms:client-desktop-x86_64:app:packageReleaseDistributionForCurrentOS` — `BUILD SUCCESSFUL`
- [ ] Desktop ARM: `:platforms:client-desktop-arm:app:packageReleaseDistributionForCurrentOS` — `BUILD SUCCESSFUL`
- [ ] Server JVM: `:server:api:build` — `BUILD SUCCESSFUL`
- [ ] NAS: `buildNasPackages` и/или `buildNasPackage*` — `BUILD SUCCESSFUL`

## 2) Artifact Gate (наличие и целостность)

- [ ] APK присутствует в `android/app/build/outputs/apk/release/`
- [ ] AAB присутствует в `android/app/build/outputs/bundle/release/`
- [ ] Desktop x86_64 пакет присутствует в `platforms/client-desktop-x86_64/app/build/compose/binaries/`
- [ ] Desktop ARM пакет присутствует в `platforms/client-desktop-arm/app/build/compose/binaries/`
- [ ] Server JAR/дистрибутив присутствует в `server/api/build/libs/` и/или `server/api/build/distributions/`
- [ ] NAS артефакты присутствуют в `build/` (`.spk`, `.qpkg`, `.apk`, `truenas-*`)

## 3) Smoke Gate (минимальная проверка запуска)

- [ ] Android: приложение устанавливается и стартует
- [ ] Desktop x86_64: пакет устанавливается, UI открывается
- [ ] Desktop ARM: пакет устанавливается, UI открывается
- [ ] Server: сервис стартует, health-check/API отвечает
- [ ] NAS: пакет устанавливается на целевой платформе, сервис стартует

Рекомендуемая автоматизация smoke-подтверждений:
- `scripts/android-video-background-smoke.ps1`
- `scripts/desktop-video-event-longrun-smoke.ps1`

## 4) Config & Security Gate

- [ ] Проверены release-конфиги и переменные окружения
- [ ] Проверена схема подписи Android артефактов
- [ ] Проверены сетевые/security настройки (HTTPS/pinning, где применимо)
- [ ] Подтверждено отсутствие временных debug-настроек в релизе

Автоматизируемая часть:
- `scripts/security-field-staging-validation.ps1`
- Runbook: [SECURITY_MVP_FIELD_VALIDATION_RUNBOOK.md](SECURITY_MVP_FIELD_VALIDATION_RUNBOOK.md)

## 5) Documentation Gate

- [ ] Актуализирован `docs/planning/LOCAL_RELEASE_BUILD_MASTER_PLAN.md`
- [ ] Актуализированы документы по конкретным типам выпуска
- [ ] Актуализирован `FINAL_VERIFICATION_REPORT.md`
- [ ] При необходимости обновлён `IMPLEMENTATION_STATUS_REPORT.md`

## 6) W4 MVP (Неделя 4) — платформы и приёмка

Связано с [PHASE1_MVP_TO_100_PLAN.md](PHASE1_MVP_TO_100_PLAN.md) (этапы 1.7 + 1.10) и дорожной картой в [TODO.md](../../archive/docs-duplicates-2026-08-08/TODO.md). Профили `local`/`staging`/`strict` для `w4-mvp-platform-and-gate.ps1` передают в Android/Desktop smoke флаги **compile-only** (сборка+тесты без обязательного устройства/long-run); полное железо — [W4_MVP_PLATFORM_SMOKE_RUNBOOK.md](../reports/W4_MVP_PLATFORM_SMOKE_RUNBOOK.md). Отключить: `-NoPlatformSmokeCompileOnlyGate`.

- [x] Выполнен автоматизируемый gate: `scripts/w4-mvp-platform-and-gate.ps1` (хотя бы без `-Full`; перед релизом — с `-Full` по матрице окружения).
- [x] При необходимости выполнен расширенный orchestration-режим:
  `scripts/w4-mvp-platform-and-gate.ps1 -IncludeWeb -IncludeAndroidSmoke -IncludeDesktopSmoke -IncludeSecurityValidation -GenerateGoNoGoSummary`
- [x] Использован профильный запуск W4 (рекомендуется):
  - `scripts/w4-mvp-platform-and-gate.ps1 -RunProfile local|staging|strict`
  - или wrapper: `scripts/run-w4-profile.ps1 -Profile local|staging|strict`
- [x] Зелёный контур **MVP automated acceptance**: `./gradlew mvpAutomatedAcceptance` и/или `scripts/ci/mvp-automated-acceptance.sh` (Gradle + web + video gate; в CI также `--generate-phase1-summary` и артефакт сводки), job **mvp-automated-acceptance** (см. [automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](../automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md)).
- [ ] Пройден ручной runbook там, где без железа не обойтись: [W4_MVP_PLATFORM_SMOKE_RUNBOOK.md](../reports/W4_MVP_PLATFORM_SMOKE_RUNBOOK.md) (W4-1 … W4-5).
- [x] Результаты и артефакты зафиксированы (логи, при необходимости `diagnostics/platform-smoke/…`).
- [x] Настроена автоматическая финализация field-стадии:
  - runbook: `docs/reports/NAS_FIELD_JSON_HANDOFF_RUNBOOK_2026-04-27.md`
  - `scripts/nas-field-auto-orchestrator.ps1 -Date <yyyy-MM-dd> -ResultsPath <json>`
  - `scripts/nas-field-results-preflight.ps1 -Date <yyyy-MM-dd> -ResultsPath <json>`
    - логическая проверка: `GO` запрещен при `FAIL` в `S2..S6`
  - `scripts/nas-field-apply-results.ps1 -Date <yyyy-MM-dd> -ResultsPath <json> [-RunFullFinalize]`
  - `scripts/nas-field-readiness-check.ps1 -Date <yyyy-MM-dd>`
  - `scripts/nas-field-aggregate.ps1 -Date <yyyy-MM-dd>`
  - `scripts/nas-field-finalize.ps1 -Date <yyyy-MM-dd> -RecalculateGate`
  - `scripts/nas-field-full-finalize.ps1 -Date <yyyy-MM-dd>` (one-command wrapper: readiness -> finalize -> docs sync)

Статус W4 на 2026-04-27:
- автоматизируемая часть W4 выполнена;
- hardware-dependent runbook и итоговое release-решение остаются открытыми (`NO-GO`).

## 7) Решение Go/No-Go

Для видео runtime/e2e допускается профильная оценка совместимости камер:
[VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](../reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md)
и gate-скрипт `scripts/video-e2e-go-no-go.ps1`.

### GO (выпуск разрешён)

- Все пункты Build Gate и Artifact Gate отмечены
- Нет критичных дефектов по результатам Smoke Gate
- Нет блокирующих рисков по безопасности/конфигурации

### CONDITIONAL (выпуск с зафиксированными ограничениями)

- Build/Artifact gates зелёные, но часть non-blocking runtime/evidence контролей в `PARTIAL`/`NOT_RUN`
- Runtime decision для текущего окружения не ниже `CONDITIONAL`
- Риски и ограничения явно перечислены в release record с планом закрытия

### NO-GO (выпуск запрещён)

- Любая красная релизная сборка
- Отсутствует обязательный артефакт
- Критичный дефект на старте/базовом сценарии

## Фиксация решения

- **Дата:** ____________________
- **Версия релиза:** ____________________
- **Решение:** GO / CONDITIONAL / NO-GO
- **Ответственный:** ____________________
- **Комментарии/риски:** ____________________
- **W4 runbook / gate:** см. [W4_MVP_PLATFORM_SMOKE_RUNBOOK.md](../reports/W4_MVP_PLATFORM_SMOKE_RUNBOOK.md), `scripts/w4-mvp-platform-and-gate.ps1`
- **Video e2e runbook / gate:** см. [VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](../reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md), `scripts/video-e2e-go-no-go.ps1`
- **Unified summary:** `scripts/generate-phase1-go-no-go-summary.ps1` -> `docs/reports/PHASE1_GO_NO_GO_SUMMARY_<date>.md` (параметр `-DecisionProfile Strict|MvpCi`: `Strict` — все `PARTIAL`/`CONDITIONAL` опускают итог; `MvpCi` — неблокирующие smoke `PARTIAL` и ONVIF `CONDITIONAL` при `requireOnvifEvidence: false` не опускают итог до `CONDITIONAL`). В сводку при наличии добавляется строка **W4 platform gate** из последнего `diagnostics/platform-smoke/w4-mvp-platform-gate-*.json` **только для контекста** (на итог не влияет).
