# Детальный план статуса проекта IP-CSS по фазам

**Версия проекта:** Alfa-0.1.1  
**Дата:** 27 April 2026  
**Связь:** [PROJECT_STATUS.md](PROJECT_STATUS.md) | STATUS_LOCK_2026-04-27.md *(утерян/в архиве)* | VIDEO_GATE_LOCK_2026-04-27.md *(утерян/в архиве)* | [CRITICAL_BLOCKERS_REMEDIATION_PLAN.md](../planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md) | [PHASE1_MVP_TO_100_PLAN.md](../planning/PHASE1_MVP_TO_100_PLAN.md) | [MVP_PHASE1_SCOPE_BOUNDARY.md](../planning/MVP_PHASE1_SCOPE_BOUNDARY.md) | [TODO.md](../../archive/docs-duplicates-2026-08-08/TODO.md) | [archive/2026-03-30/TASKS_WITH_DESCRIPTIONS.md](../archive/2026-03-30/TASKS_WITH_DESCRIPTIONS.md) | [SECURITY_MVP_READINESS.md](SECURITY_MVP_READINESS.md) | [MODULE_STATUS_BASELINE_2026-04-23.md](MODULE_STATUS_BASELINE_2026-04-23.md) | [../reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](../reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md)

---

## Легенда статусов

| Символ | Значение |
|--------|----------|
| ✅ | Завершено |
| 🟡 | В процессе |
| ⚠️ | Начато |
| ❌ | Не начато |
| 📋 | Запланировано |

---

## Сводка по фазам

| Фаза | Название | Прогресс | Статус |
|------|----------|----------|--------|
| 1 | MVP | ~90% | 🟡 В процессе |
| 2 | Основной функционал | ~40% | 🟡 В процессе |
| 3 | Расширенный функционал | ~10% | ⚠️ Запланировано |
| 4 | Enterprise | ~15% | ⚠️ Запланировано (часть реализована) |

**Общий прогресс проекта:** ~85% (синхронизировано с `PROJECT_STATUS.md`; релизный gate-статус обновляется по документам в `docs/reports/`)

**Последнее обновление:** 2026-05-28 — Выполнение 12 задач Фазы 1 завершено (инструменты готовы на 100%)

---

# Фаза 1: MVP — детальный статус

**Цель:** Минимально жизнеспособный продукт (обнаружение камер, просмотр потоков, записи, события, веб и базовые клиенты).

## 1.1 Инфраструктура и основа

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 1.1.1 | Модульная структура (Gradle, KMP) | ✅ | settings.gradle.kts, core/, shared/, server/, platforms/ |
| 1.1.2 | SQLDelight схемы и настройка | ✅ | Схемы для Camera, Recording, Event, User, Settings, Notification |
| 1.1.3 | Документация и индекс | ✅ | docs/, DOCUMENTATION_INDEX.md, docs/README.md |
| 1.1.4 | CI/CD и Docker | ✅ | Конфигурация присутствует |
| 1.1.5 | Версионирование (Alfa-0.1.1) | ✅ | gradle.properties, libs.versions.toml |

**Итого этап 1.1:** ✅ 100%

---

## 1.2 Доменный слой

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 1.2.1 | Модели данных (Camera, Recording, Event, User, Settings, Notification) | ✅ | commonMain domain models |
| 1.2.2 | Интерфейсы репозиториев | ✅ | Все репозитории объявлены |
| 1.2.3 | Use Cases: камеры (5), обнаружение (4), записи (6), события (3), настройки (2), PTZ (1), уведомления (3), пользователи (4) | ✅ | 28 Use Cases |
| 1.2.4 | Use Cases для аналитики (DetectObjects, TrackObjects, DetectMotion, ANPR, DetectFaces) | ❌ | Запланировано в Фазе 2 |
| 1.2.5 | Доменный AnalyticsService (контракт) | 🟡 | Интерфейс есть, реализация нативная частичная |

**Итого этап 1.2:** 🟡 ~55%

---

## 1.3 Слой данных

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 1.3.1 | SQLDelight репозитории для всех сущностей | ✅ | Camera, Recording, Event, User, Settings, Notification |
| 1.3.2 | Entity мапперы | ✅ | Для всех сущностей |
| 1.3.3 | DatabaseFactory (Android, iOS, Desktop) | ✅ | Платформенные реализации |
| 1.3.4 | LocalDataSource / RemoteDataSource (6/6) | ✅ | 100% |
| 1.3.5 | Рефакторинг репозиториев V2 | ✅ | 6/6 (100%): Camera, Recording, Event, User, Settings, Notification — `RepositoriesV2Module`; подтверждено DI-тестами (`RepositoriesV2ModuleInjectionTest`) для local-first, remote-fallback и local-only профилей |
| 1.3.6 | Миграции БД (версионирование, MigrationManager, тесты) | ✅ | MigrationManager + версии + расширенный integration-контур (`MigrationManagerIntegrationTest`: fresh/v1/v2/idempotent/downgrade-guard), SQLDelight migration verify — PASS |

**Итого этап 1.3:** 🟢 ~98% (V2 6/6 + расширенный тест-контур data layer и DI wiring; до 100% остаются release-level non-functional проверки)

---

## 1.4 Сетевой слой

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 1.4.1 | ApiClient (HTTP) | ✅ | Полностью |
| 1.4.2 | API сервисы и DTO | ✅ | Все |
| 1.4.3 | OnvifClient (Discovery, Device, Media, PTZ, Digest Auth) | 🟢 | ~92%, проверка на 6 камерах (ONVIF 200/6; Media+Events 4/6), WS-Discovery улучшен |
| 1.4.4 | WebSocketClient | 🟢 | ~92%, базовая функциональность + reconnect/rate limiting + burst/stress-тесты очереди + E2E сценарий connect/auth/subscribe/reconnect |
| 1.4.5 | RtspClient | 🟢 | ~85%, интеграция с NativeRtspClient + fallback; `disconnect()` всегда отменяет `receiveJob`; JVM-тесты fallback lifecycle (`RtspClientTest`); **cinterop + Kotlin/Native (mingwX64) компилируется успешно**; длительные реальные потоки — в полевой валидации |
| 1.4.6 | ONVIF Event service (PullPoint, маппинг в события IP-CSS) | 🟢 | ~92%, PullPoint/renew/pull/sync реализованы + покрыты сервисными тестами timeout/sync/pull/renew/mapping; по совместимости камер подтверждено 4/6 (MVP профиль задокументирован) |
| 1.4.7 | ONVIF Digest Authentication | 🟢 | ~95%, расширенная обработка nonce/stale/realm + кэш параметров |

**Итого этап 1.4:** 🟢 ~95%

Operational baseline `1.4` (automation/gates, 2026-04-27):

- Automation backlog `A1-A8` закрыт (см. `docs/status/NETWORK_LAYER_1_4_AUTOMATION_BACKLOG.md`).
- Strict status-sync аудит проходит: `.\scripts\sync-network-layer-1-4-status.ps1 -FailOnLegacyConflicts` (`exit 0`).
- Локальный preflight smoke доступен через `.\scripts\check-network-layer-ci-prerequisites.ps1 -RequireLiveChecks -RequireStatusSyncAudit -LocalSmoke` (`WARN` без fail, только локальный режим).
- One-command локальный smoke-контур: `.\scripts\run-network-layer-1-4-local-smoke.ps1`.
- CI aggregate gate `network-layer-aggregate-decision` работает в strict-режиме и запускается только при совместном включении:
  - `ENABLE_NETWORK_LAYER_AGGREGATE_DECISION=true`
  - `ENABLE_NETWORK_LAYER_LIVE_CHECKS=true`
  - `ENABLE_NETWORK_LAYER_STATUS_SYNC_AUDIT=true`
- Операционный runbook: `docs/status/NETWORK_LAYER_STATUS_SYNC_RUNBOOK.md`.

---

## 1.5 Серверная часть

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 1.5.1 | REST API (Ktor), все endpoints | ✅ | Камеры, записи, события, пользователи, настройки, здоровье |
| 1.5.2 | Аутентификация (JWT, refresh, logout) | ✅ | AuthRoutes |
| 1.5.3 | RBAC (requireRole) во всех маршрутах | ✅ | Event, Recording, Camera, Health и др. |
| 1.5.4 | WebSocket сервер и интеграция с репозиториями | ✅ | Event, Recording, Camera — события в реальном времени |
| 1.5.5 | Rate limiting | ✅ | Реализован (Redis + global limiter); добавлены guardrails: `RATE_LIMIT_FAIL_OPEN` (prod default fail-closed), `REDIS_REQUIRED` preflight и тесты `RateLimitMiddlewareFailureModeTest` / `RedisConfigPreflightTest` |
| 1.5.6 | Миграция на PostgreSQL | 🟢 | ~95%, **инструменты созданы**: `migration-smoke-test.ps1`, `postgresql-cutover.ps1`; smoke тесты, cutover/rollback автоматизация готовы; требуется полевая валидация на staging |
| 1.5.7 | Расширенная аутентификация (LDAP, SSO, Kerberos) | 📋 | Официально deferred из MVP/Phase 1 в Enterprise 4.3.3; для возврата в active execution нужны: утверждённый security design (IdP/realm mapping), staging IdP стенд (LDAP/SAML/OIDC/Kerberos), и e2e acceptance matrix для login/callback/role mapping/failure modes |

**Итого этап 1.5:** 🟢 ~93%

---

## 1.6 Веб-интерфейс

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 1.6.1 | Next.js 14, основные страницы | ✅ | Камеры, записи, события, настройки |
| 1.6.2 | Redux store и API интеграция | ✅ | Все сервисы |
| 1.6.3 | WebSocket в веб-клиенте (подписки, real-time) | ✅ | Закрыто (P1-1): каналы cameras/events/recordings/notifications + Redux/UI |
| 1.6.4 | Видеоплеер (HLS, ошибки, загрузка) | ✅ | Закрыто (P1-3 web): HLS + RTSP->HLS (adaptive/master) ветка покрыта Jest |
| 1.6.5 | Безопасное хранение JWT (httpOnly cookies и т.д.) | ✅ | Закрыто (P1-4): httpOnly cookie + refresh/interceptors + proactive scheduler |

**Итого этап 1.6:** ✅ ~95%

---

## 1.7 Мобильные и десктопные платформы

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 1.7.1 | Android: структура, навигация, экраны, ViewModels, DI | ✅ | Koin, базовые экраны |
| 1.7.2 | Android: интеграция с RTSP/видео | ⚠️ | ~30% |
| 1.7.3 | Desktop (Compose): экраны, записи, события, видеоплеер | 🟡 | ~70%, EventTimeline, RecordingPlayer, VideoPlayer |
| 1.7.4 | iOS приложение | ❌ | Не начато; **вне обязательного MVP Фазы 1** по [MVP_PHASE1_SCOPE_BOUNDARY.md](../planning/MVP_PHASE1_SCOPE_BOUNDARY.md) (не блокирует GO веб+Android+Desktop+server) |
| 1.7.5 | Android: фоновая работа, разрешения, Keystore для токенов | ⚠️ | Частично |

**Итого этап 1.7:** 🟡 ~30% (мобильные), Desktop ~70%

---

## 1.8 Видео и запись

> Canonical source of truth: `docs/status/CANONICAL_BREAKDOWN_1_8_VIDEO_AND_RECORDING.md`
>
> Runtime/release gate for current camera compatibility profile:
> `scripts/video-e2e-go-no-go.ps1` + `docs/reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md`
> (strict release decision, profile-aware release decision, runtime decision).
>
> В GitHub Actions job **mvp-automated-acceptance** для gate без предзагруженных diagnostics подставляется **`config/video-e2e-acceptance-profile.mvp-ci.json`** (переопределение: **`MVP_VIDEO_ACCEPTANCE_PROFILE`**); см. [MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](../automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md).
>
> **Обновление 2026-05-28:** Созданы инструменты тестирования:
> - `scripts/hls-runtime-stability-test.ps1` — HLS long-run, cleanup, reconnect тесты
> - `scripts/screenshot-pipeline-test.ps1` — Screenshot pipeline тесты
> - `native/video-processing/src/audio_decoder.cpp` — Аудио декодеры (AAC, PCMU, PCMA) с AV синхронизацией

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 1.8.1 | Recording Core (VideoRecordingService + API + Use Cases) | 🟡 | ~86%, integration coverage существенно усилен (recording lifecycle + error mapping tests) |
| 1.8.2 | HLS Pipeline (live + recordings) | 🟢 | ~95%, **инструменты тестирования готовы**: `hls-runtime-stability-test.ps1`; long-run, cleanup, reconnect тесты созданы; требуется полевая валидация |
| 1.8.3 | Screenshot Pipeline | 🟢 | ~80%, **инструменты тестирования готовы**: `screenshot-pipeline-test.ps1`; обработка ошибок и таймаутов протестирована; требуется полевая валидация |
| 1.8.4 | RTSP Native Integration | 🟢 | ~90%, **аудио декодирование реализовано**: AAC, PCMU, PCMA с AV синхронизацией; `audio_decoder.cpp` готов; инструменты тестирования созданы |
| 1.8.5 | Web Video Player Stability | 🟡 | ~75%, основной риск в backend/runtime устойчивости |
| 1.8.6 | Desktop Video Player Stability | 🟡 | ~62%, базовая функциональность есть, нужна валидация long-run |
| 1.8.7 | Android Video + Background Recording | ⚠️ | ~48%, закрыты service-очереди и probe/unit-контур; остаются instrumented/e2e recovery |
| 1.8.8 | iOS Video Playback | ❌ | 0%, не начато |

**Итого этап 1.8:** 🟢 ~80% (инструменты готовы, требуется полевая валидация)

---

## 1.9 Безопасность (MVP)

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 1.9.1 | JWT аутентификация, RBAC | ✅ | Реализовано |
| 1.9.2 | CORS, Rate limiting, безопасная Docker-конфигурация | ✅ | Есть |
| 1.9.3 | Certificate Pinning (Desktop/Android) + HTTPS trust boundary (Web) | 🟡 | Android: Ktor OkHttp engine + preconfigured pinning (сборка ✅); Web: доверие браузера; полевая валидация HTTPS/pins на staging — [SECURITY_MVP_FIELD_VALIDATION_RUNBOOK.md](../planning/SECURITY_MVP_FIELD_VALIDATION_RUNBOOK.md) |
| 1.9.4 | HTTPS принудительно (редиректы, проверки) | 🟢 | `FORCE_HTTPS` + `installHttpsRedirect` / HSTS + `X-Forwarded-Proto`; дублирующий HSTS из SecurityHeaders снят; тесты `HttpsRedirectAndHstsMiddlewareTest` |
| 1.9.5 | Шифрование учётных данных камер в БД | ⚠️ | Реализован fail-closed + авто-миграция legacy plaintext при старте; требуется валидация на staging |
| 1.9.6 | Логирование и аудит (критические операции) | ⚠️ | SecurityLogger + AuditRoutes; добавлен PostgreSQL audit_log repository для production-профиля |

**Итого этап 1.9:** 🟡 ~80%

---

## 1.10 Тестирование (MVP)

Канонические **ID 1.10.1–1.10.4** ниже совпадают с дорожной картой ([PHASE1_MVP_TO_100_PLAN.md](../planning/PHASE1_MVP_TO_100_PLAN.md)). Детальная разбивка с подпунктами **1.10.x.y** (другая сетка номеров: там блок «1.10.3» — UI) — в архивном снимке [ТАБЛИЦА_ГОТОВНОСТИ_ПРОЕКТА.md](../archive/status-legacy-2026-04-27/ТАБЛИЦА_ГОТОВНОСТИ_ПРОЕКТА.md) §«Этап 1.10».

Справка по локальным сценариям приёмки и вспомогательным скриптам (единый **`-ShowHelp`** для **`scripts/**/*.ps1`**, **`native/build-stub-libs.ps1`**) — [MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](../automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md) §8.10–8.15.

**Обновление 2026-05-28:** Созданы инструменты тестирования для Фазы 1:
- `scripts/migration-smoke-test.ps1` — smoke тесты миграций БД
- `scripts/postgresql-cutover.ps1` — PostgreSQL cutover/rollback
- `scripts/hls-runtime-stability-test.ps1` — HLS runtime stability тесты
- `scripts/screenshot-pipeline-test.ps1` — Screenshot pipeline тесты
- `scripts/test-rtsp-real-cameras.ps1` — RTSP тестирование с реальными камерами
- `config/test-cameras.rtsp.json` — конфигурация 10 тестовых камер

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 1.10.1 | Unit-тесты (репозитории, Use Cases, RBAC) | 🟡 | Частично (CameraRepository, DiscoverCameras, RbacTest); расширен JVM smoke API: `HealthBasicIntegrationTest`, `HealthReadyIntegrationTest`, `HealthLiveIntegrationTest` |
| 1.10.2 | Интеграционные тесты API | 🟢 | ~60%, **инструменты созданы**: HLS integration tests, migration tests, RTSP/HLS/Screenshot тесты; требуется полевая валидация |
| 1.10.3 | Интеграционные тесты БД/миграций | 🟢 | ~95%, **инструменты созданы**: `migration-smoke-test.ps1`; расширен контур для shared data layer, требуется полевая валидация |
| 1.10.4 | E2E / UI тесты | ❌ | Не начато |

**Итого этап 1.10:** 🟢 ~50% (инструменты готовы, требуется полевая валидация)

**Исполнение (2026-05-28):** Выполнено 12 задач Фазы 1 (W1-0, W1-1, W1-2, W1-3, W2-1, W2-2). Инструментальная часть готова на 100%. Для завершения требуется полевая валидация (staging сервер, реальные камеры).

---

**Фаза 1 итог:** 🟢 ~90% (инструменты готовы на 100%; остаются блокеры полевой валидации: PostgreSQL staging cutover, RTSP/HLS/Screenshot тестирование с реальными камерами, Certificate Pinning field validation).

---

# Фаза 2: Основной функционал — детальный статус

**Цель:** AI-аналитика (базовая), уведомления, управление записями, завершение веб-интерфейса.

## 2.1 AI-аналитика

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 2.1.1 | FFI/JNI биндинги (Kotlin ↔ C++) для motion/object/anpr/face | ⚠️ | Начато (native/analytics, Android JNI, JVM `NativeAnalytics`); iOS/полный паритет платформ — нет |
| 2.1.2 | Детекция движения (пороги, зоны, события) | 🟡 | `DetectMotionUseCase`, `AnalyticsFrameProcessor`, `VideoAnalyticsService` + нативный детектор (JVM/Android); зоны/порог/`motionEventCooldown` в настройках; полевая калибровка и стабильность — в работе |
| 2.1.3 | Детекция объектов (TFLite/YOLO, классы, confidence) | 🟡 | `AnalyticsServiceImpl` (JVM/Android) + `detectObjects` в `VideoAnalyticsService`; зависит от модели (`objectDetectionModelPath`) и окружения |
| 2.1.4 | Трекинг объектов (ID, траектории) | 🟡 | `trackObjects` / `updateTracking` в JVM-реализации; не везде связан с UI и политиками событий |
| 2.1.5 | ANPR (распознавание номеров) | ✅ | native/analytics, Use Case, события, LicensePlateRepository (см. BLOCKS_8_9) |
| 2.1.6 | Детекция лиц (bbox, embeddings) | ✅ | face_detector, DetectFacesUseCase, события (BLOCKS_8_9) |
| 2.1.7 | Интеграция AI с видеопотоками (RTSP/HLS → анализ → события) | 🟡 | Частично: добавлены единый тип источника кадров `AnalyticsFrameInput`/`AnalyticsFrameSourceKind`, расширенные метрики `/analytics/metrics` (`frameSource`, `lastError`, `lastErrorAt`), матрица приёмки `PHASE2_1_7_ACCEPTANCE_MATRIX.md`; HLS-only всё ещё без серверного frame-source для аналитики, сквозная стабильность остаётся подфокусом |
| 2.1.8 | REST API и Use Cases для настроек аналитики по камерам | 🟡 | `GET`/`POST` `/api/v1/cameras/{id}/analytics`, правила/webhook в `AnalyticsRoutes`; расширение сценариев — в бэклоге |

**Итого этап 2.1:** 🟡 ~45% (лица и ANPR закрыты; движение/объекты/трекинг — в коде, без полной приёмки; **2.1.7** — главный оставшийся сквозной разрыв).

**Подфокус исполнения (актуально):** **2.1.7** — связать устойчивый поток кадров (RTSP/HLS/server path) с аналитикой и доменными событиями по согласованной матрице сценариев.

---

## 2.2 Система уведомлений

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 2.2.1 | WebSocket уведомления (в реальном времени) | ✅ | Интеграция с репозиториями есть |
| 2.2.2 | Email (SMTP, шаблоны, правила) | 🟡 | Частично: SMTP канал работает, поддержка кастомных шаблонов, HTML-шаблоны (default, motion, critical), документация по настройке. См. [EMAIL_NOTIFICATIONS_GUIDE.md](../reports/EMAIL_NOTIFICATIONS_GUIDE.md) |
| 2.2.3 | Push (FCM, APNs) | 🟡 | Частично: FCM sender (Android), APNs sender (iOS), lifecycle API токенов, retry/backoff; остаются Web Push и полевая валидация. См. [PUSH_NOTIFICATIONS_GUIDE.md](../reports/PUSH_NOTIFICATIONS_GUIDE.md) |
| 2.2.4 | SMS, Webhook | 🟡 | Частично: Twilio SMS sender, Advanced Webhook sender с HMAC подписью, retry/backoff; остаются интеграционные тесты и провайдер-специфичные адаптеры. См. [SMS_WEBHOOK_NOTIFICATIONS_GUIDE.md](../reports/SMS_WEBHOOK_NOTIFICATIONS_GUIDE.md) |

**Итого этап 2.2:** 🟡 ~85%

---

## 2.3 Улучшения веб-интерфейса

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 2.3.1 | RecordingList с фильтрами и пагинацией | ✅ | Полностью реализовано: 3 режима (grid/list/table), расширенные фильтры (камера, статус, дата, качество), сортировка, пагинация, массовые операции, WebSocket live обновления, localStorage. См. [PHASE2_2_3_WEB_INTERFACE_COMPLETION.md](../reports/PHASE2_2_3_WEB_INTERFACE_COMPLETION.md) |
| 2.3.2 | EventTimeline с привязкой к видео | ✅ | Реализовано: временная шкала событий, синхронизация с видеоплеером, все типы событий, навигация и фильтрация |
| 2.3.3 | Графики и отчёты (статистика, экспорт CSV/PDF) | ✅ | Реализовано: страница `/reports` с 4 вкладками, 6+ графиков (Recharts), экспорт CSV/PDF, фильтрация по камере и периоду. См. [PHASE2_2_3_3_2_3_5_IMPLEMENTATION_STATUS.md](../reports/PHASE2_2_3_3_2_3_5_IMPLEMENTATION_STATUS.md) |
| 2.3.4 | Производительность и адаптивность | 🟡 | Частично: виртуализация, debouncing, memoization; Lighthouse конфигурация и скрипты созданы; PWA (Service Worker, Manifest, Offline page) реализовано; требуется запуск audit и оптимизация на основе результатов. См. [PHASE2_2_4_PERFORMANCE_AND_PWA.md](../reports/PHASE2_2_4_PERFORMANCE_AND_PWA.md) |

**Итого этап 2.3:** 🟡 ~95%

---

**Фаза 2 итог:** 🟡 ~77% (уточнены § 2.1.7, § 2.2.2, § 2.2.3, § 2.2.4, § 2.2.5 по факту серверных доработок и § 2.3.1-2.3.3, § 2.3.5 — реализованы, § 2.3.4 — PWA готово, требуется Lighthouse audit)

---

# Фаза 3: Расширенный функционал — детальный статус

**Цель:** NAS-пакеты, расширенная аналитика, завершение Desktop, ONVIF Event/Analytics/Imaging.

## 3.1 NAS платформы

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 3.1.1 | Инфраструктура сборки (Gradle, multi-arch, скрипты) | ✅ | `buildAllNasPackages`, CI smoke/checksum precheck зафиксирован |
| 3.1.2 | Synology SPK | 🟡 | Пакеты собраны (preliminary), полевые S2-S6 pending |
| 3.1.3 | QNAP QPKG | 🟡 | Пакеты собраны (preliminary), полевые S2-S6 pending |
| 3.1.4 | Asustor APK | 🟡 | Пакеты собраны (preliminary), полевые S2-S6 pending |
| 3.1.5 | TrueNAS CORE/SCALE | 🟡 | Bundle сформирован (preliminary), host validation pending |
| 3.1.6 | Документация NAS | 🟡 | Master index/go-no-go/runbook/notes оформлены, финал после field validation |

**Итого этап 3.1:** 🟡 ~80% (packaging precheck `PASS`, итоговый program gate `NO-GO` до закрытия field/runtime evidence)

**Операционный auto-статус (2026-04-27):** ✅ автоматизированный контур 3.1 выполнен end-to-end (`phase3-auto-execution.ps1` + `-RunBuild`), включая сборку артефактов и smoke/precheck; для финального `GO` остаются только полевые S2-S6 на реальных устройствах. См. `docs/reports/PHASE3_AUTO_EXECUTION_STATUS_2026-04-27.md`, `docs/reports/PHASE3_FULL_AUTO_EXECUTION_STATUS_2026-04-27.md`, `docs/reports/NAS_FIELD_ONE_PAGE_CHECKLIST_2026-04-27.md`.

---

## 3.2 Desktop приложения

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 3.2.1 | Просмотр видео в реальном времени (RTSP/HLS) | 🟡 | VideoPlayer + multi-camera optimization: stream-priority, viewport visibility, grace mode, render telemetry (FPS/dropped), anti-noise metrics publish; RTSP native integration остаётся в работе; детальный план до 100%: `docs/reports/DESKTOP_3_2_1_TO_100_EXECUTION_CHECKLIST_2026-04-27.md` |
| 3.2.2 | Управление записями и событиями (списки, воспроизведение) | 🟡 | RecordingsScreen, EventsScreen, EventTimeline |
| 3.2.3 | Настройки и системная интеграция (трей, автозапуск) | 🟡 | SystemIntegration.kt, SettingsScreen |
| 3.2.4 | Desktop ARM — паритет с x86_64 | 🟡 | Общая кодовая база |

**Итого этап 3.2:** 🟡 ~55%

**Операционный auto-статус (2026-04-27):** ✅ baseline автоматизации выполнен (`phase3-desktop-auto-execution.ps1`), compile + desktop test контур стабильно проходит; chain-run `phase3-continue-auto.ps1` также `PASS` (`docs/reports/PHASE3_CONTINUE_AUTO_EXECUTION_STATUS_2026-04-27.md`). При этом latest desktop smoke остаётся compile/tests-only (`runtimeLongRun = NOT_RUN` в `diagnostics/platform-smoke/desktop/desktop-video-event-smoke-20260427-120728.md`), поэтому для `3.2.1` сохраняется `CONDITIONAL GO` до runtime evidence.

---

## 3.3 Расширенная аналитика

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 3.3.1 | ANPR полный цикл (база номеров, поиск, алерты) | ✅ | Реализовано (BLOCKS_8_9) |
| 3.3.2 | Распознавание лиц (compareFaces, FaceRepository, face_gallery) | 🟡 | Интерфейсы; миграция и реализация — в плане |
| 3.3.3 | Аналитические отчёты (ReportServiceImpl, CSV/PDF) | 🟡 | Интерфейс и маршруты; реализация — в плане |

**Итого этап 3.3:** 🟡 ~40%

**Операционный auto-статус (2026-04-27):** ✅ baseline автоматизации выполнен (`phase3-analytics-auto-execution.ps1`): `FaceRepository` integration test, server API compile baseline, `FaceGalleryRoutes` test и `NativeAnalyticsTest` проходят. См. `docs/reports/PHASE3_ANALYTICS_AUTO_EXECUTION_STATUS_2026-04-27.md`.

---

**Фаза 3 итог:** 🟡 ~65% (автоматизируемая часть закрыта; финальный релизный `GO` зависит от полевой NAS-валидации S2-S6 и итогового sign-off).

**Автоконсолидация field-стадии (2026-04-27):**
- `scripts/nas-field-aggregate.ps1` — собирает S1-S6 и platform decision из `NAS_FIELD_REPORT_*` в `NAS_FIELD_AGGREGATOR_2026-04-27.md`.
- `scripts/nas-field-finalize.ps1` — aggregate + optional `video-e2e-go-no-go.ps1` пересчёт в одном шаге.
- Текущий автостатус финализации: `Field validation = NO-GO`, `Final decision = NO-GO` (прогон `nas-field-finalize.ps1 -SkipReadinessCheck -RecalculateGate`); для перехода к `GO` требуется заполнение реальных S2-S6.

---

# Фаза 4: Enterprise — детальный статус

**Цель:** Облачная синхронизация, кластеризация, расширенная безопасность (LDAP, SSO, 2FA, аудит).

## 4.1 Облачная синхронизация и хранилище

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 4.1.1 | CloudSyncService (syncCameras, syncSettings, syncEvents, syncAll) | ✅ | CloudSyncServiceImpl, SyncRoutes ([CLOUD_SYNC_AND_STORAGE.md](CLOUD_SYNC_AND_STORAGE.md)) |
| 4.1.2 | ConflictResolver (last-write-wins, merge) | ✅ | Реализовано |
| 4.1.3 | CloudStorageService, CloudStorageProvider (FileBased) | ✅ | API для загрузки/скачивания записей |
| 4.1.4 | S3/MinIO/GCS/Azure провайдеры | ❌ | В плане |
| 4.1.5 | BackupSchedulerService, DatabaseBackupService, restore API | ✅ | Реализовано |

**Итого этап 4.1:** 🟡 ~70%

---

## 4.2 Кластеризация и репликация

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 4.2.1 | ClusterService (Redis heartbeat, nodeId) | ✅ | ([CLUSTER_LOAD_BALANCING_REPLICATION.md](CLUSTER_LOAD_BALANCING_REPLICATION.md)) |
| 4.2.2 | API /cluster/nodes, /cluster/me | ✅ | ClusterRoutes |
| 4.2.3 | Health с nodeId для LB | ✅ | HealthRoutes |
| 4.2.4 | Read replica (DATABASE_READ_REPLICA_URL) | ✅ | Конфигурация и пул |
| 4.2.5 | Документация nginx/LB | 🟡 | Примеры в документе |

**Итого этап 4.2:** ✅ ~90%

---

## 4.3 Расширенная безопасность

| ID | Задача | Статус | Примечание |
|----|--------|--------|------------|
| 4.3.1 | OAuth2/OIDC (state, login/callback, DI) | ✅ | OAuth2Service, AuthRoutes ([PLAN_OAUTH2_AND_SECURITY_MONITORING.md](PLAN_OAUTH2_AND_SECURITY_MONITORING.md)) |
| 4.3.2 | SecurityMonitoringService, SecurityAlert, правила | ✅ | SecurityAlertRepository, SecurityMonitoringRoutes |
| 4.3.3 | LDAP/AD, Kerberos, SSO (SAML) | 🟡 | LdapAuthService, KerberosAuthService, ExternalAuthProvider — заглушки/частично |
| 4.3.4 | 2FA / TOTP | 🟡 | TotpService есть |
| 4.3.5 | Аудит (AuditLogRepository, AuditRoutes) | ✅ | InMemoryAuditLogRepository, SecurityLogger |

**Итого этап 4.3:** 🟡 ~60%

---

**Фаза 4 итог:** 🟡 ~70% (много уже реализовано: sync, cluster, OAuth2, security monitoring, audit).

---

# Критические блокеры (сводка)

| # | Блокер | Статус | Документ |
|---|--------|--------|----------|
| 1 | RTSP клиент — интеграция с нативной библиотекой | ⚠️ В работе | [CRITICAL_BLOCKERS_REMEDIATION_PLAN.md](../planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md) |
| 2 | Видеоплеер — интеграция с RTSP/HLS | ⚠️ В работе | Там же |
| 3 | Certificate Pinning и HTTPS принудительно | ⚠️ В работе | Там же |
| 4 | ONVIF Event service (события камер → IP-CSS) | 🟡 Почти завершено | Реализовано ~92%; остаётся финальная ручная валидация 1.4.3 |
| 5 | WebSocket в веб-клиенте (полная интеграция) | ✅ Завершено | Каналы cameras/events/recordings/notifications подключены; P1-1 закрыт |
| 6 | Безопасное хранение JWT (httpOnly и т.д.) | ✅ Завершено | httpOnly cookie + refresh/interceptors; P1-4 закрыт |

---

# Связанные документы

- **[PROJECT_STATUS.md](PROJECT_STATUS.md)** — основной статус и карта проекта
- **[CRITICAL_BLOCKERS_REMEDIATION_PLAN.md](../planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md)** — план по блокерам
- **[PHASE1_MVP_TO_100_PLAN.md](../planning/PHASE1_MVP_TO_100_PLAN.md)** — зафиксированный рабочий план доведения Фазы 1 (MVP) до 100%
- **[TODO.md](../../archive/docs-duplicates-2026-08-08/TODO.md)** — единый список задач (To-Do)
- **[archive/2026-03-30/TASKS_WITH_DESCRIPTIONS.md](../archive/2026-03-30/TASKS_WITH_DESCRIPTIONS.md)** — архив: задачи с описаниями по областям
- **[BLOCKS_8_9_ANALYTICS_AND_FEATURES.md](../planning/BLOCKS_8_9_ANALYTICS_AND_FEATURES.md)** — блоки 8–9 (аналитика и доп. функционал)
- **[CLOUD_SYNC_AND_STORAGE.md](CLOUD_SYNC_AND_STORAGE.md)** — синхронизация и облачное хранилище
- **[CLUSTER_LOAD_BALANCING_REPLICATION.md](CLUSTER_LOAD_BALANCING_REPLICATION.md)** — кластер и репликация БД
- **[PLAN_OAUTH2_AND_SECURITY_MONITORING.md](PLAN_OAUTH2_AND_SECURITY_MONITORING.md)** — OAuth2 и мониторинг безопасности
- **[SECURITY_MVP_READINESS.md](SECURITY_MVP_READINESS.md)** — go/no-go checklist по 1.9 Безопасность MVP
- **[../reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](../reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md)** — video e2e profile-aware gate (strict/runtime/profile-aware decisions)
- **[POSTGRESQL_FINALIZATION_RUNBOOK.md](../POSTGRESQL_FINALIZATION_RUNBOOK.md)** — runbook и checklist закрытия 1.5.6
- **[POSTGRESQL_FINALIZATION_STAGING_REPORT_TEMPLATE.md](../POSTGRESQL_FINALIZATION_STAGING_REPORT_TEMPLATE.md)** — шаблон отчета staging cutover/rollback для финальной приёмки 1.5.6
- **[WORKING_DOCUMENTS_INDEX.md](../../archive/docs-deprecated-2026-09-04/WORKING_DOCUMENTS_INDEX.md)** — рабочий индекс документации
- **[DOCUMENTATION_INDEX.md](../../DOCUMENTATION_INDEX.md)** — корневой индекс документации
