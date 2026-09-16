# Единый список задач (To-Do) — IP-CSS

**Версия проекта:** Alfa-0.1.1  
**Дата:** 27 April 2026  
**Назначение:** одна точка входа для чеклистов, приоритетов и дорожной карты. Обновляйте этот файл при изменении плана; устаревшие снимки лежат в [archive/2026-03-30/](archive/2026-03-30/).

---

## Связанные документы

| Документ | Роль |
|----------|------|
| [status/PROJECT_STATUS.md](status/PROJECT_STATUS.md) | Общий статус, фазы, блокеры |
| [status/PROJECT_STATUS_PHASES.md](status/PROJECT_STATUS_PHASES.md) | Задачи по фазам с отметками |
| [status/ТАБЛИЦА_ГОТОВНОСТИ_ПРОЕКТА.md](status/ТАБЛИЦА_ГОТОВНОСТИ_ПРОЕКТА.md) | Таблица готовности по компонентам |
| [planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md](planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md) | Исторический план по блокерам (частично перекрыт этим To-Do) |
| [planning/RELEASE_GO_NO_GO_CHECKLIST.md](planning/RELEASE_GO_NO_GO_CHECKLIST.md) | Финальный GO/NO-GO |
| [reports/W4_MVP_PLATFORM_SMOKE_RUNBOOK.md](reports/W4_MVP_PLATFORM_SMOKE_RUNBOOK.md) | Прогон W4 (платформы + гейт) |
| [status/WEB_INTERFACE_OPEN_ITEMS_RUNBOOK.md](status/WEB_INTERFACE_OPEN_ITEMS_RUNBOOK.md) | Закрытие open-пунктов веб-интерфейса (1.6) |
| [TASKS_FOR_REFINEMENT_AND_DEBUGGING.md](TASKS_FOR_REFINEMENT_AND_DEBUGGING.md) | Углублённый список доработок и отладки (RTSP, компоненты) |
| [DOCUMENTATION_GAPS.md](DOCUMENTATION_GAPS.md) | Недостающая документация (бэклог) |
| [automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md) | Автоматическая приёмка (Gradle/web/CI вместо части ручных чеклистов) |
| [planning/VIDEO_SURVEILLANCE_COMPLIANCE_AUDIT_AND_TZ_2026.md](planning/VIDEO_SURVEILLANCE_COMPLIANCE_AUDIT_AND_TZ_2026.md) | Аудит видео vs отраслевой чеклист; формулировки для ТЗ; матрица внедрения |

### План MVP до 100% (в работе)

Поэтапный план с порядком этапов и критериями — в **[planning/PHASE1_MVP_TO_100_PLAN.md](planning/PHASE1_MVP_TO_100_PLAN.md)**. Содержимое этого файла **не дублируется** здесь; при расхождении формулировок приоритет у актуальной версии `PHASE1_MVP_TO_100_PLAN.md`.

Детализация разделов 3–4 того же направления: [planning/PHASE1_MVP_TO_100_SECTIONS_3_4_DETAILED_PLAN.md](planning/PHASE1_MVP_TO_100_SECTIONS_3_4_DETAILED_PLAN.md).

### Архив снятых с обслуживания списков

Подробные таблицы задач по областям (старый формат) и прежняя копия недельных чеклистов: **[archive/2026-03-30/](archive/2026-03-30/)**.

---

## Оглавление

1. [Фаза 1 (MVP) — дорожная карта по неделям](#1-фаза-1-mvp--дорожная-карта-по-неделям)
2. [Критический приоритет (MVP блокеры) — чеклист](#2-критический-приоритет-mvp-блокеры--чеклист)
3. [Высокий приоритет](#3-высокий-приоритет)
4. [Средний приоритет](#4-средний-приоритет)
5. [Фаза 1 (MVP) — оставшиеся задачи по ID](#5-фаза-1-mvp--оставшиеся-задачи-по-id)
6. [Фаза 2 — основной функционал](#6-фаза-2--основной-функционал)
7. [Фаза 3 — расширенный функционал](#7-фаза-3--расширенный-функционал)
8. [Фаза 4 — Enterprise](#8-фаза-4--enterprise)
9. [Сводка по приоритетам](#9-сводка-по-приоритетам)
10. [Бэклог документации (кратко)](#10-бэклог-документации-кратко)

---

## 1. Фаза 1 (MVP) — дорожная карта по неделям

Соответствие этапам плана: **Этап 0–1** (foundation + data/server), **Этап 2** (1.4 + 1.8), **Этап 3** (1.6 web), **Этап 4** (1.9 security), **Этап 5** (1.7 platforms), **Этап 6** (1.10 tests + go/no-go).

### Неделя 1 — Foundation + data/server baseline + старт транспорта

- [x] **W1-0** Зафиксировать must-have сценарии MVP и границы приёмки (согласовать iOS/Desktop вне блокеров при необходимости).
- [x] **W1-1** Довести миграции БД, smoke/rollback; закрыть хвост **repository V2** (1.3.5).
- [x] **W1-2** PostgreSQL: staging cutover + rollback rehearsal по runbook (1.5.6).
- [x] **W1-3** Старт **Этапа 2**: RTSP native integration + HLS runtime (long-run, reconnect, cleanup); screenshot `captureFrame(...)` (1.8).

### Неделя 2 — Завершение видео-транспорта + ONVIF

- [x] **W2-1** Закрыть production-path: discover → play → record → replay → events (без деградации на длительных прогонах).
- [x] **W2-2** Валидация **ONVIF Events** (P1-2 / F1-2): без камеры — тесты `OnvifEvent*`; с камерой — `scripts/onvif-events-api-verification.ps1`. См. [automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md).
- [x] **W2-3** Стабилизация HLS pipeline и FFmpeg/runtime (1.8.2).

### Неделя 3 — Web closure + Security MVP

- [x] **W3-1** Полная интеграция **WebSocket** в web: подписки, **reconnect с backoff**, дедуп сообщений при необходимости (P1-1).
- [x] **W3-2** **Безопасное хранение JWT** (httpOnly / refresh lifecycle) — закрыть P1-4.
- [x] **W3-3** Видеоплеер web: интеграция с RTSP/HLS backend path (P1-3 / F1-3).
- [x] **W3-4** Perf web: минимум в CI — `npm run build` + `npm test` в `server/web`; полный Lighthouse — по [WEB_INTERFACE_PERF_MANUAL_CHECKLIST.md](status/WEB_INTERFACE_PERF_MANUAL_CHECKLIST.md) § Автоматизация.
- [x] **W3-5** **Security MVP**: HTTPS enforcement, pinning на клиентах где применимо, минимальный аудит (P1-5, 1.9).

### Неделя 4 — Платформы + тесты + go/no-go

**Автоматизация и runbook:** [W4_MVP_PLATFORM_SMOKE_RUNBOOK.md](reports/W4_MVP_PLATFORM_SMOKE_RUNBOOK.md) · `scripts/w4-mvp-platform-and-gate.ps1` · раздел W4 в [RELEASE_GO_NO_GO_CHECKLIST.md](planning/RELEASE_GO_NO_GO_CHECKLIST.md).

**Примечание (2026-04-24):** отдельный **прогон** контуров **1.10** (API/DB integration + полный E2E) на текущем шаге **не обязателен**; позиция плана — **Фаза 2 § 2.1**, **подфокус 2.1.7** (AI + видеопоток → события), см. [PROJECT_STATUS_PHASES.md](status/PROJECT_STATUS_PHASES.md). Переход к **§ 2.2** и далее — только после явной фиксации закрытия сквозного контура 2.1.7 (не автоматически по чеклисту недели 4).

- [x] **W4-1** Android: video + background recording — стабильный сценарий (1.7.2, 1.8.7).
- [x] **W4-2** Desktop: long-run видеоплеер и события; ARM/x86 smoke (1.7.3).
- [x] **W4-3** Integration-тесты: API, БД/миграции, критичный video path (1.10.2–1.10.3) — **`./gradlew mvpAutomatedAcceptance`**, job **mvp-automated-acceptance** в CI (`scripts/ci/mvp-automated-acceptance.sh`: web + `video-e2e-go-no-go.ps1` с **`mvp-ci`** профилем в GitHub Actions + опционально Phase1 summary); JVM HLS: `HlsStreamRoutesIntegrationTest`, `HlsPublicRoutesIntegrationTest`, `HlsRecordingRoutesIntegrationTest`; см. [automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md).
- [x] **W4-4** Минимальный E2E smoke для критических пользовательских сценариев (1.10.4).
- [ ] **W4-5** Финальная матрица приёмки и **GO/NO-GO** ([RELEASE_GO_NO_GO_CHECKLIST.md](planning/RELEASE_GO_NO_GO_CHECKLIST.md), отчёты в `docs/reports/`); автоматический prerequisite — зелёный **mvp-automated-acceptance** + при необходимости `scripts/w4-mvp-platform-and-gate.ps1`.  
  _Статус на 2026-04-27: выполнен предварительный packaging precheck, но итоговый program gate остаётся `NO-GO` до закрытия runtime/matrix и field validation evidence._

**RTSP automation update (2026-04-27):** закрыт кодовый/тестовый контур по runtime diagnostics, stream status payload и WebSocket `stream_started` payload (`analyticsFrameSource`, `rtspConnectError`); детали: [reports/RTSP_FOCUS_AUTOMATION_STATUS_2026-04-27.md](reports/RTSP_FOCUS_AUTOMATION_STATUS_2026-04-27.md).
**RTSP gate update (2026-04-27):** после фиксов `video-runtime-platform-matrix.ps1` и `video-e2e-go-no-go.ps1` runtime decision по 1.8.A/B/C = `GO`, profile-aware release decision = `GO`, strict release decision остаётся `NO-GO` из-за conditional controls (включая canonical 1.8 floor).
**Field finalize automation (2026-04-27):** добавлены `scripts/nas-field-aggregate.ps1` (консолидация S1-S6/Decision из платформенных отчётов) и `scripts/nas-field-finalize.ps1` (aggregate + optional go/no-go recalculation в одном шаге).
**Field readiness + one-command finalize (2026-04-27):** добавлены `scripts/nas-field-readiness-check.ps1` (блокирующая проверка полноты S2-S6/Result) и `scripts/nas-field-full-finalize.ps1` (one-command pipeline: readiness/finalize/recalculate/sync docs).
**Field JSON orchestrator (2026-04-27):** добавлен `scripts/nas-field-auto-orchestrator.ps1` (preflight -> apply -> full finalize + execution report `docs/reports/NAS_FIELD_AUTOMATION_EXECUTION_<date>.md`).

**Инфраструктура оператора (вне недель W1–W4, зафиксировано 2026-04):** единый **`-ShowHelp`** для всех **`scripts/**/*.ps1`** и для **`native/build-stub-libs.ps1`**; реестр команд и пояснения — [automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md) §8.10–8.15 (включая **`native\`** в §8.15).

---

## 2. Критический приоритет (MVP блокеры) — чеклист

| ID | Задача | Статус | Оценка |
|----|--------|--------|--------|
| P1-1 | Интеграция WebSocket в веб: каналы cameras/events/recordings/notifications, Redux, UI | - [x] | 3–5 дней |
| P1-2 | ONVIF Events (1.4.3): тесты в репозитории + `onvif-events-api-verification.ps1` при наличии камеры | - [x] | 0.5–1 день |
| P1-3 | Видеоплеер + RTSP/HLS (веб и мобильные клиенты) | - [x] | 2–3 недели |
| P1-4 | Безопасное хранение JWT (httpOnly / refresh flow) | - [x] | 2–3 дня |
| P1-5 | SSL/TLS Certificate Pinning (клиенты) + HTTPS контур | - [x] | 1–2 недели |

---

## 3. Высокий приоритет

| ID | Задача | Статус | Оценка |
|----|--------|--------|--------|
| P2-1 | Пагинация пользователей (API + UI) | - [x] | 1–2 дня |
| P2-2 | Redis Rate Limiting (login + глобальный лимит /api/v1, исключения HLS/health/ws) | - [x] | 3–5 дней |
| P2-3 | RTSP ↔ FFmpeg: `FfmpegCli` (`FFMPEG_PATH`, `FFPROBE_PATH`, RTSP tcp/rw_timeout), запись + HLS + HLS из записи | - [x] | 1–2 недели |
| P2-4 | Security Headers (CSP, HSTS, …) — Ktor + Next.js | - [x] | 1 день |

---

## 4. Средний приоритет

| ID | Задача | Статус | Оценка |
|----|--------|--------|--------|
| P3-1 | Android UI + RTSP/видео (экран, протоколы, качества, observation-summary) | - [x] | 2–3 недели |
| P3-2 | Детекция движения (use case + frame processor + unit tests) | - [x] | 2–3 недели |
| P3-3 | Валидация на сервере (REST): `RequestValidator` + middleware-валидация на маршрутах | - [x] | 1 неделя |
| P3-4 | Логирование безопасности: audit-события auth/permission/input-validation (security logger) | - [x] | 3–5 дней |
| P3-5 | Декомпозиция и стабилизация ONVIF integration tests (`Lifecycle`/`Processing`/`Mapping`), legacy `OnvifEventIntegrationServiceTest` удалён | - [x] | 1–2 дня |
| P3-6 | **HLS:** 2K/4K (`QHD1440`, `UHD4K`), мастер-плейлист, веб/Android. *Бэклог:* нагрузка 6×FFmpeg, тюнинг битрейтов. См. [VIDEO_SURVEILLANCE… §Реализовано](planning/VIDEO_SURVEILLANCE_COMPLIANCE_AUDIT_AND_TZ_2026.md) | - [x] | 1–2 недели |
| P3-7 | **HLS HEVC:** `HLS_HEVC_FOR_2K4K` + libx265; **dual ladder** на сервере (`qhd1440_h264` / `uhd4k_h264` в adaptive + валидация) и пункты выбора fallback-качеств в web/Android. *Бэклог:* e2e | - [x] | 2–3 недели |
| P3-8 | **px/m:** домен + DTO + **GET** `/api/v1/cameras/{id}/observation-summary` + UI в web/Android/Desktop | - [x] | 2–3 недели |

---

## 5. Фаза 1 (MVP) — оставшиеся задачи по ID

Сопоставление с [PHASE1_MVP_TO_100_PLAN.md](planning/PHASE1_MVP_TO_100_PLAN.md): этапы 3.2–3.7.

| ID | Задача | Статус | Оценка |
|----|--------|--------|--------|
| F1-1 | RTSP клиент — активация и финальная интеграция (в т.ч. fallback/skip в unit tests без native runtime) | - [x] | 1–2 недели |
| F1-2 | ONVIF ручная приёмка и отчёт | - [x] | 0.5–1 день |
| F1-3 | Видеоплеер — интеграция с RTSP/HLS | - [x] | 2–3 недели |
| F1-4 | Миграции БД — 100% (версии, тесты, совместимость) | - [x] | 1–2 недели |
| F1-5 | Certificate Pinning и HTTPS (конфиг + platform loaders + тестовый контур) | - [x] | 1–2 недели |

**Актуализация (репозитории V2):** шесть основных репозиториев (Camera, Recording, Event, User, Settings, Notification) переведены на V2 и подключены через `RepositoriesV2Module`; статус в [PROJECT_STATUS_PHASES.md](status/PROJECT_STATUS_PHASES.md) §1.3.5.

**Актуализация (1.3 data layer, 2026-04-27):**
- Добавлены bulk SQLDelight запросы `deleteAll*` для `camera/event/user/notification` и перевод соответствующих `LocalDataSourceImpl` на прямой bulk-delete.
- В `RepositoriesV2Module` снят hardcoded `null` для remote и включен DI-подхват nullable remote data sources (`getOrNull<...RemoteDataSource>()`), подтверждено DI-интеграционными тестами.
- Расширен целевой test-matrix data layer: `BulkDeleteQueriesIntegrationTest`, `MigrationManagerIntegrationTest` (fresh/v1/v2/idempotent/downgrade-guard), DI-контур `RepositoriesV2ModuleInjectionTest` (local-first/remote-fallback/local-only/all-repositories-resolve).
- Таргетный пакет `:shared:testDebugUnitTest` по перечисленным data-layer тестам — `PASS`.

---

## 6. Фаза 2 — основной функционал

| ID | Задача | Описание | Оценка |
|----|--------|----------|--------|
| F2-1 | AI-аналитика (базовая) | Детекция движения/объектов, трекинг; события и уведомления | 2–3 месяца |
| F2-2 | Система уведомлений | Push, Email, Telegram; каналы и шаблоны | 2–3 недели |
| F2-3 | Управление записями (завершение) | Поиск, экспорт, квоты | 1–2 недели |
| F2-4 | Веб-интерфейс (завершение) | Все сценарии, ошибки, доступность | 2–3 недели |
| F2-5 | Use Cases для аналитики | Запуск/остановка, зоны, правила | 1–2 недели |
| F2-6 | Запись и архив: HEVC для >1080p | Авто-HEVC при height>1080 + `useH265` в API, метаданные фактического кодека в БД/API/UI, integration e2e для `/recordings`, `/recordings/{id}` и export route с `useH265` (сделано). *Бэклог:* полноценная playback-верификация медиакодека через ffprobe в CI | 2–3 недели |
| F2-7 | ИИ: on-prem vs VSaaS | `executionLocation` в `AnalyticsSettings`, cloud-ingest flow в `AnalyticsFrameProcessor` + `QueuedVsaasAnalyticsIngestClient` (очередь, retry/backoff, метрики доставки) — сделано. *Бэклог:* интеграция реального транспорта, security hardening и end-to-end telemetry | 2–4 недели |

**Актуализация backend (2026-04-27):**
- `F2-1 / 2.1.7` частично закрыт на сервере: введены `AnalyticsFrameInput` / `AnalyticsFrameSourceKind`, расширены метрики `GET /api/v1/cameras/{id}/analytics/metrics` (`frameSource`, `lastError`, `lastErrorAt`), оформлена матрица `PHASE2_1_7_ACCEPTANCE_MATRIX.md`.
- `F2-2 / 2.2.5` частично закрыт на сервере: единая маршрутизация каналов (`__notifyChannels` + fallback на legacy-флаги), API политики правил `GET/PUT /api/v1/analytics/rules/{id}/notification-policy`, тесты маршрутов/сервиса — `PASS`.
- `F2-2 / 2.2.4` частично закрыт на сервере: добавлены минимальные webhook/SMS каналы уведомлений (`__notifyWebhook`, `__notifyWebhookUrl`, `__notifySms`, `__notifySmsTo`) с retry/backoff; webhook подписывается HMAC. Бэклог: провайдер-специфичные интеграции и операционные сценарии.
- `F2-2 / 2.2.2` частично закрыт на сервере: email-канал поддерживает кастомные шаблоны (`__emailSubjectTemplate`, `__emailBodyTemplate`), правила аналитики могут задавать шаблоны через `additionalActions` (`emailSubjectTemplate`, `emailBodyTemplate`).
- `F2-2 / 2.2.3` частично закрыт на сервере: добавлен минимальный push-канал (`__notifyPush`, `__notifyPushTokens`) и sender-адаптер с retry/backoff; бэклог: полноценные FCM/APNs и управление device tokens.
- `F2-2 / 2.2.3` дополнено: реализован lifecycle API токенов `GET/POST/DELETE /api/v1/notifications/push-tokens` (регистрация/получение/отзыв), покрыт route-тестами.
- `F2-2 / 2.2.3` дополнено: `PushTokenService` переведен с чисто in-memory на минимальную персистентность через `SettingsRepository` (ключ `push_tokens_store_v1`), добавлены unit-тесты загрузки/сохранения.
- `F1 / 1.5` hardening дополнено: для Redis и rate limiting добавлены production guardrails — preflight `RedisConfig.validateRedisRequirementsForServerStartup()` (вызов при старте `Application.module()`), строгий режим обязательности Redis через `REDIS_REQUIRED`, а также fail-open/fail-closed тесты (`RateLimitMiddlewareFailureModeTest`, `RedisConfigPreflightTest`).
- `F1 / 1.5` дополнительно: legacy in-memory server repositories (`ServerEventRepository`, `ServerRecordingRepository`, `ServerSettingsRepository`) защищены runtime-guard в production; обход только через явный флаг `ALLOW_LEGACY_IN_MEMORY_REPOSITORIES=true` для исключительных dev/migration сценариев.
- `F1 / 1.5` техдолг снижён: общий валидатор `LegacyInMemoryRepositoryGuard` вынесен в отдельный модуль и подключен ко всем legacy in-memory server repositories; добавлен unit-тест `LegacyInMemoryRepositoryGuardTest` на сценарии production/dev/override.
- `F1 / 1.5` docs/env sync: обновлены `docs/ENVIRONMENT_VARIABLES.md`, `docs/POSTGRESQL_FINALIZATION_RUNBOOK.md` и `.env.example` — зафиксированы guardrail-переменные `REDIS_REQUIRED`, `RATE_LIMIT_FAIL_OPEN`, `ALLOW_LEGACY_IN_MEMORY_REPOSITORIES` и рекомендуемые production значения.
- `F1 / 1.5` test hardening: исправлена обработка malformed JSON для `POST /api/v1/recordings/start` (возврат `400 Invalid request body` вместо `500`), после чего `./gradlew :server:api:test` снова `PASS`.
- `F1 / 1.5` cleanup: мигрирован deprecated CORS import в `Application.kt` на `io.ktor.server.plugins.cors.routing.CORS`; одновременно нормализована обработка `BadRequestException` в `configureExceptionHandling()` (malformed body стабильно возвращает `400 Invalid request body`), `./gradlew :server:api:test` — `PASS`.
- `F1 / 1.5.7` scope lock: расширенная аутентификация (LDAP/SSO/Kerberos) официально зафиксирована как deferred из MVP/Phase 1 в `4.3.3` (Enterprise). Возврат в активную реализацию только при выполнении preconditions: утверждённый security design, staging IdP стенд и acceptance matrix e2e (login/callback/role mapping/failure modes).
- `F1 / 1.5.7` docs sync: синхронизирован `docs/status/PROJECT_STATUS.md` с тем же scope lock/deferred-формулированием для LDAP/SSO/Kerberos (`4.3.3`, не блокер MVP, чёткие preconditions re-activation).
- `F1 / 1.5.7` current-status lock: в `docs/status/CURRENT_STATUS.md` добавлен явный блок `Status Lock Update (2026-04-27)` с канонической фиксацией по `1.5.5`/`1.5.6`/`1.5.7` и условиями re-activation для deferred enterprise auth.
- `F1 / 1.5.x` status snapshot: добавлен отдельный канонический документ `docs/status/STATUS_LOCK_2026-04-27.md` и ссылки на него из `PROJECT_STATUS.md` и `CURRENT_STATUS.md` для безопасной навигации без зависимости от legacy/encoding-шумов.
- `F1 / 1.8` video gate snapshot: добавлен `docs/status/VIDEO_GATE_LOCK_2026-04-27.md` с фиксированным состоянием release-gate (`strict NO-GO`, `profile-aware GO`, `runtime GO`, final `NO-GO` до field evidence) и текущими блокерами `1.8.3/1.8.4/1.8.7`; ссылки добавлены в `PROJECT_STATUS.md` и `CURRENT_STATUS.md`.
- `F1 / status navigation` sync: в `docs/status/PROJECT_STATUS_PHASES.md` (блок `Связь`) добавлены прямые ссылки на `STATUS_LOCK_2026-04-27.md` и `VIDEO_GATE_LOCK_2026-04-27.md`, чтобы замкнуть навигацию между всеми source-of-truth статусами.
- `F1 / docs hygiene` index sync: `docs/WORKING_DOCUMENTS_INDEX.md` обновлён — lock-документы `status/STATUS_LOCK_2026-04-27.md` и `status/VIDEO_GATE_LOCK_2026-04-27.md` добавлены в раздел canonical status/control.
- `F1 / policy sync` source-of-truth: `docs/status/SOURCE_OF_TRUTH.md` обновлён — lock-документы `STATUS_LOCK_2026-04-27.md` и `VIDEO_GATE_LOCK_2026-04-27.md` закреплены как operational snapshots и добавлены в update workflow.
- `F1 / priority set sync`: `docs/HIGH_PRIORITY_ACTIVE_DOCS.md` обновлён — `STATUS_LOCK_2026-04-27.md` и `VIDEO_GATE_LOCK_2026-04-27.md` включены в P0; policy дополнена требованием обновлять lock snapshots вместе с `PROJECT_STATUS.md`.
- `F1 / lock memo`: добавлен one-page операторский memo `docs/reports/STATUS_LOCK_AUTOMATION_MEMO_2026-04-27.md` (что обновлять первым, текущий lock-срез, правило приоритета lock-доков); ссылка добавлена в `docs/WORKING_DOCUMENTS_INDEX.md`.
- `F1 / README quick-start`: в `docs/README.md` добавлен блок `Status updates quick start` с прямыми ссылками на `status/PROJECT_STATUS.md`, `status/STATUS_LOCK_2026-04-27.md`, `status/VIDEO_GATE_LOCK_2026-04-27.md` и `reports/STATUS_LOCK_AUTOMATION_MEMO_2026-04-27.md`.
- `F1 / root index quick-start`: в `DOCUMENTATION_INDEX.md` добавлены быстрые ссылки на lock-контур (`STATUS_LOCK_2026-04-27.md`, `VIDEO_GATE_LOCK_2026-04-27.md`, `STATUS_LOCK_AUTOMATION_MEMO_2026-04-27.md`) в разделах onboarding и статуса проекта.
- `F1 / metadata sync`: в `DOCUMENTATION_INDEX.md` обновлено поле `Последнее обновление` до `27 April 2026` с указанием текущего lock-contour sync-контекста.
- `F1 / metadata sync (footer)`: синхронизировано второе поле `Последнее обновление` в нижнем блоке `DOCUMENTATION_INDEX.md` (теперь также `27 April 2026`) для устранения внутреннего рассинхрона дат.
- `F1 / metadata sweep PASS`: проверены ключевые entry docs (`docs/README.md`, `docs/WORKING_DOCUMENTS_INDEX.md`, `docs/HIGH_PRIORITY_ACTIVE_DOCS.md`, `docs/status/SOURCE_OF_TRUTH.md`, `DOCUMENTATION_INDEX.md`) — `Last updated`/`Последнее обновление` согласованы на `27 April 2026`.
- `F1 / marker cleanup (safe mode)`: в `DOCUMENTATION_INDEX.md` добавлено правило интерпретации `⭐ НОВОЕ/ОБНОВЛЕНО` (возможны исторические метки) и явный приоритет lock-контура для текущих status-решений.

---

## 7. Фаза 3 — расширенный функционал

| ID | Задача | Описание | Оценка |
|----|--------|----------|--------|
| F3-1 | NAS платформы | SPK, QPKG, TrueNAS; документация | 3–4 месяца |
| F3-2 | Расширенная аналитика | ANPR, лица, отчёты | 2–3 месяца |
| F3-3 | Desktop UI | Завершение Compose Desktop | 2–3 месяца |
| F3-4 | ONVIF — расширение покрытия | Вендоры, edge-cases | 1–2 недели |
| F3-5 | ONVIF Analytics / Imaging | Опционально | 2–4 недели |
| F3-6 | Оценка **H.266 (VVC)** | [VVC_ROADMAP_ASSESSMENT.md](planning/VVC_ROADMAP_ASSESSMENT.md) (сделано). Повторная оценка при смене стека клиентов | 3–5 дней |

---

## 8. Фаза 4 — Enterprise

### 8.1 Расширенная безопасность

| ID | Задача | Оценка |
|----|--------|--------|
| F4-1 | LDAP/Active Directory | 2 недели |
| F4-2 | SSO (SAML, OAuth/OIDC) | 2 недели |
| F4-3 | Kerberos | 1 неделя |
| F4-4 | 2FA / TOTP | 1–2 недели |
| F4-5 | Аудит доступа | ~1 неделя |
| F4-6 | Мониторинг безопасности | 1–2 недели |
| F4-7 | End-to-end шифрование | 2–3 недели |

### 8.2 Облачная синхронизация

| ID | Задача | Оценка |
|----|--------|--------|
| F4-8 | Синхронизация между устройствами | 2–3 недели |
| F4-9 | Разрешение конфликтов | 1 неделя |
| F4-10 | Облачное хранилище (S3/MinIO/…) | 2–3 недели |
| F4-11 | Резервное копирование | 1–2 недели |

### 8.3 Масштабирование и кластеризация

| ID | Задача | Оценка |
|----|--------|--------|
| F4-12 | Поддержка кластеров | 3–4 недели |
| F4-13 | Координация узлов | 2–3 недели |
| F4-14 | Балансировка нагрузки | 2 недели |
| F4-15 | Репликация БД | 3–4 недели |
| F4-16 | Синхронизация реплик | 2 недели |

---

## 9. Сводка по приоритетам

| Приоритет | Количество задач | Фокус |
|-----------|------------------|--------|
| Критический (P1) | 5 | MVP блокеры (детали этапов — [PHASE1_MVP_TO_100_PLAN.md](planning/PHASE1_MVP_TO_100_PLAN.md)) |
| Высокий (P2) | 4 | Пагинация, Redis rate limit, RTSP↔сервер, security headers |
| Средний (P3) | 8 | Android UI, движение, валидация, security logging; HLS 2K/4K (часть закрыта), HEVC-HLS fallback, px/m UI |
| Фаза 1 (F1) | 5 | Оставшиеся задачи MVP |
| Фаза 2 (F2) | 7 | AI, уведомления, записи, веб, analytics UC; архив HEVC >1080p; VSaaS-требования |
| Фаза 3 (F3) | 6 | NAS, аналитика, Desktop, ONVIF; оценка VVC |
| Фаза 4 (F4) | 16 | Enterprise |

**Связанные документы:**  
[PROJECT_STATUS.md](status/PROJECT_STATUS.md) · [PROJECT_STATUS_PHASES.md](status/PROJECT_STATUS_PHASES.md) · [PHASE1_MVP_TO_100_PLAN.md](planning/PHASE1_MVP_TO_100_PLAN.md) · [archive/2026-03-30/TASKS_WITH_DESCRIPTIONS.md](archive/2026-03-30/TASKS_WITH_DESCRIPTIONS.md) (архив детальных таблиц по областям)

---

## 10. Бэклог документации (кратко)

Полный перечень: [DOCUMENTATION_GAPS.md](DOCUMENTATION_GAPS.md). Краткие ориентиры:

- [ ] Политика **VULNERABILITY_REPORTING** и публичный процесс disclosure.
- [ ] **API_RATE_LIMITING.md** / **API_VERSIONING.md** при стабилизации API.
- [ ] **MIGRATION_GUIDE.md** / **UPGRADE_GUIDE.md** перед релизами с миграциями схем.
- [ ] Платформенные **DEPLOYMENT_*** (Synology, QNAP, TrueNAS, K8s) по мере готовности пакетов.
- [ ] **BENCHMARKS.md** при появлении воспроизводимых метрик.

Чекбоксы здесь не дублируют [DOCUMENTATION_GAPS.md](DOCUMENTATION_GAPS.md); при закрытии пункта обновляйте оба при необходимости.
