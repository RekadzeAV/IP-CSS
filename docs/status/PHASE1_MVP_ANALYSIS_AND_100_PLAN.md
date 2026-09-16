# Фаза 1 MVP: Анализ текущего состояния и план доведения до 100%

**Проект:** IP-CSS  
**Версия:** Alfa-0.1.1  
**Дата актуализации:** 28 April 2026  
**Источники:** `PROJECT_STATUS_PHASES.md`, `MODULE_STATUS_BASELINE_2026-04-23.md`, `CANONICAL_BREAKDOWN_1_8_VIDEO_AND_RECORDING.md`, `PHASE1_MVP_TO_100_PLAN.md`, `SECURITY_MVP_READINESS.md`, `RTSP_INTEGRATION_STATUS_UPDATE.md`

---

## 1. Резюме

| Показатель | Значение |
|------------|----------|
| **Общий прогресс Фазы 1 (MVP)** | **~75%** |
| **Статус** | 🟡 В процессе |
| **Критических блокеров** | 4 |
| **Оценка до 100%** | 3–4 недели при фиксированном scope |
| **Релизный gate** | NO-GO (до закрытия runtime/field evidence) |

**Формула расчета:** взвешенное среднее по 10 подэтапам с учетом канонического breakdown для видео (1.8).

---

## 2. Детальный статус по подэтапам

### 2.1 Инфраструктура и основа (1.1)

| ID | Задача | Статус | % |
|----|--------|--------|---|
| 1.1.1 | Модульная структура (Gradle, KMP) | ✅ | 100 |
| 1.1.2 | SQLDelight схемы и настройка | ✅ | 100 |
| 1.1.3 | Документация и индекс | ✅ | 100 |
| 1.1.4 | CI/CD и Docker | ✅ | 100 |
| 1.1.5 | Версионирование (Alfa-0.1.1) | ✅ | 100 |

**Итого 1.1:** ✅ **100%**

---

### 2.2 Доменный слой (1.2)

| ID | Задача | Статус | % |
|----|--------|--------|---|
| 1.2.1 | Модели данных (Camera, Recording, Event, User, Settings, Notification) | ✅ | 100 |
| 1.2.2 | Интерфейсы репозиториев | ✅ | 100 |
| 1.2.3 | Use Cases: камеры (5), обнаружение (4), записи (6), события (3), настройки (2), PTZ (1), уведомления (3), пользователи (4) | ✅ | 100 |
| 1.2.4 | Use Cases для аналитики (DetectObjects, TrackObjects, DetectMotion, ANPR, DetectFaces) | ❌ | 0 |
| 1.2.5 | Доменный AnalyticsService (контракт) | 🟡 | 50 |

**Итого 1.2:** 🟡 **~82%** *(пересчитано: 28 Use Cases готовы, аналитика — вне MVP-обязательного scope)*

---

### 2.3 Слой данных (1.3)

| ID | Задача | Статус | % |
|----|--------|--------|---|
| 1.3.1 | SQLDelight репозитории для всех сущностей | ✅ | 100 |
| 1.3.2 | Entity мапперы | ✅ | 100 |
| 1.3.3 | DatabaseFactory (Android, iOS, Desktop) | ✅ | 100 |
| 1.3.4 | LocalDataSource / RemoteDataSource (6/6) | ✅ | 100 |
| 1.3.5 | Рефакторинг репозиториев V2 | ✅ | 100 |
| 1.3.6 | Миграции БД (MigrationManager, версионирование, тесты) | ✅ | 100 |

**Итого 1.3:** 🟢 **~98%** *(до 100% остаются release-level non-functional проверки)*

---

### 2.4 Сетевой слой (1.4)

| ID | Задача | Статус | % |
|----|--------|--------|---|
| 1.4.1 | ApiClient (HTTP) | ✅ | 100 |
| 1.4.2 | API сервисы и DTO | ✅ | 100 |
| 1.4.3 | OnvifClient (Discovery, Device, Media, PTZ, Digest Auth) | 🟢 | ~92 |
| 1.4.4 | WebSocketClient | 🟢 | ~92 |
| 1.4.5 | RtspClient | 🟡 | ~76 |
| 1.4.6 | ONVIF Event service (PullPoint, маппинг в события IP-CSS) | 🟢 | ~92 |
| 1.4.7 | ONVIF Digest Authentication | 🟢 | ~95 |

**Итого 1.4:** 🟢 **~94%**

> **Operational baseline 1.4** (automation/gates): automation backlog A1-A8 закрыт, strict status-sync аудит проходит (`exit 0`), one-command локальный smoke-контур доступен.

---

### 2.5 Серверная часть (1.5)

| ID | Задача | Статус | % |
|----|--------|--------|---|
| 1.5.1 | REST API (Ktor), все endpoints | ✅ | 100 |
| 1.5.2 | Аутентификация (JWT, refresh, logout) | ✅ | 100 |
| 1.5.3 | RBAC (requireRole) во всех маршрутах | ✅ | 100 |
| 1.5.4 | WebSocket сервер и интеграция с репозиториями | ✅ | 100 |
| 1.5.5 | Rate limiting (Redis + global limiter) | ✅ | 100 |
| 1.5.6 | Миграция на PostgreSQL | ⚠️ | ~70 |
| 1.5.7 | Расширенная аутентификация (LDAP, SSO, Kerberos) | 📋 | 0 *(deferred)* |

**Итого 1.5:** 🟡 **~88%**

> **Блокер 1.5.6:** Требуется финальный staging cutover + smoke/rollback rehearsal. Runbook: `POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md`.

---

### 2.6 Веб-интерфейс (1.6)

| ID | Задача | Статус | % |
|----|--------|--------|---|
| 1.6.1 | Next.js 14, основные страницы | ✅ | 100 |
| 1.6.2 | Redux store и API интеграция | ✅ | 100 |
| 1.6.3 | WebSocket в веб-клиенте (подписки, real-time) | ✅ | 100 |
| 1.6.4 | Видеоплеер (HLS, ошибки, загрузка) | ✅ | 100 |
| 1.6.5 | Безопасное хранение JWT (httpOnly cookies) | ✅ | 100 |

**Итого 1.6:** ✅ **~95%**

---

### 2.7 Мобильные и десктопные платформы (1.7)

| ID | Задача | Статус | % |
|----|--------|--------|---|
| 1.7.1 | Android: структура, навигация, экраны, ViewModels, DI | ✅ | 100 |
| 1.7.2 | Android: интеграция с RTSP/видео | ⚠️ | ~30 |
| 1.7.3 | Desktop (Compose): экраны, записи, события, видеоплеер | 🟡 | ~70 |
| 1.7.4 | iOS приложение | ❌ | 0 *(вне обязательного MVP)* |
| 1.7.5 | Android: фоновая работа, разрешения, Keystore для токенов | ⚠️ | ~50 |

**Итого 1.7:** 🟡 **~55%** *(Android ~40%, Desktop ~70%, iOS не учитывается в MVP-обязательном scope)*

---

### 2.8 Видео и запись (1.8) — канонический breakdown

| ID | Подэтап | Вес | Статус | % | Вклад |
|----|---------|-----|--------|---|-------|
| 1.8.1 | Recording Core (VideoRecordingService + API + Use Cases) | 20% | 🟡 | 82% | 16.4 |
| 1.8.2 | HLS Pipeline (live + recordings) | 15% | 🟡 | 88% | 13.2 |
| 1.8.3 | Screenshot Pipeline | 5% | 🟡 | 54% | 2.7 |
| 1.8.4 | RTSP Native Integration | 20% | ⚠️ | 58% | 11.6 |
| 1.8.5 | Web Video Player Stability | 12% | 🟡 | 75% | 9.0 |
| 1.8.6 | Desktop Video Player Stability | 10% | 🟡 | 62% | 6.2 |
| 1.8.7 | Android Video + Background Recording | 12% | ⚠️ | 35% | 4.2 |
| 1.8.8 | iOS Video Playback | 6% | ❌ | 0% | 0.0 |

**Итого 1.8:** 🟡 **~63.3%** *(взвешенно по формуле `Σ(Weight_i × Progress_i) / 100`)*

> **Критические блокеры 1.8:**
> - `1.8.4` RTSP native integration — infrastructure готова, но библиотека не скомпилирована, FFI не активирован.
> - `1.8.3` frame-based screenshot декодирование не завершено.
> - `1.8.7` Android background recording не доведена до DoD.

---

### 2.9 Безопасность MVP (1.9)

| ID | Задача | Статус | % |
|----|--------|--------|---|
| 1.9.1 | JWT аутентификация, RBAC | ✅ | 100 |
| 1.9.2 | CORS, Rate limiting, безопасная Docker-конфигурация | ✅ | 100 |
| 1.9.3 | Certificate Pinning (Desktop/Android) + HTTPS trust boundary (Web) | 🟡 | ~75 |
| 1.9.4 | HTTPS принудительно (редиректы, проверки) | 🟢 | ~90 |
| 1.9.5 | Шифрование учётных данных камер в БД | ⚠️ | ~80 |
| 1.9.6 | Логирование и аудит (критические операции) | ⚠️ | ~80 |

**Итого 1.9:** 🟡 **~88%**

> **Security MVP Readiness:** `NO-GO`. 4 из 6 контролей в статусе `CONDITIONAL` (требуют staging/field evidence).
> - Certificate pinning: мобильный/Desktop путь существует, требуется полевая валидация.
> - HTTPS: редирект/HSTS реализованы, требуется production topology validation.
> - Шифрование credentials: fail-closed + auto-migration legacy plaintext реализованы, требуется staging валидация.
> - Audit: SecurityLogger + PostgreSQL audit_log repository есть, требуется включение `AUDIT_PERSIST_ENABLED=true` в staging.

---

### 2.10 Тестирование MVP (1.10)

| ID | Задача | Статус | % |
|----|--------|--------|---|
| 1.10.1 | Unit-тесты (репозитории, Use Cases, RBAC) | 🟡 | ~45 |
| 1.10.2 | Интеграционные тесты API | 🟡 | ~40 |
| 1.10.3 | Интеграционные тесты БД/миграций | 🟡 | ~50 |
| 1.10.4 | E2E / UI тесты | ❌ | 0 |

**Итого 1.10:** ⚠️ **~28%**

> **Примечание:** JVM smoke API расширен (`HealthBasicIntegrationTest`, `HealthReadyIntegrationTest`, `HealthLiveIntegrationTest`). HLS интеграционные тесты созданы (`HlsStreamRoutesIntegrationTest`, `HlsPublicRoutesIntegrationTest`, `HlsRecordingRoutesIntegrationTest`). Shared data layer контур для миграций и DI wiring проходит. E2E/UI полностью отсутствуют.

---

## 3. Сводная таблица Фазы 1

| Этап | Название | Прогресс | Статус | Блокер для 100% |
|------|----------|----------|--------|-----------------|
| 1.1 | Инфраструктура | 100% | ✅ | Нет |
| 1.2 | Доменный слой | ~82% | 🟡 | Analytics Use Cases (вне MVP scope) |
| 1.3 | Слой данных | ~98% | 🟢 | Non-functional проверки |
| 1.4 | Сетевой слой | ~94% | 🟢 | RtspClient long-run validation |
| 1.5 | Серверная часть | ~88% | 🟡 | PostgreSQL staging cutover |
| 1.6 | Веб-интерфейс | ~95% | ✅ | Нет (критичные задачи закрыты) |
| 1.7 | Платформы | ~55% | 🟡 | Android video integration |
| 1.8 | Видео и запись | ~63% | 🟡 | **RTSP native integration** |
| 1.9 | Безопасность MVP | ~88% | 🟡 | Staging evidence |
| 1.10 | Тестирование | ~28% | ⚠️ | E2E/UI отсутствуют |

**Фаза 1 итог:** 🟡 **~75%**

---

## 4. Критические блокеры (приоритизированные)

| # | Блокер | Этап | Статус | Влияние | Оценка закрытия |
|---|--------|------|--------|---------|-----------------|
| 1 | **RTSP Native Integration** — FFI не активирован, библиотека не скомпилирована | 1.8.4 | ⚠️ | Блокирует production-path видео на всех платформах | 2–3 недели |
| 2 | **HLS Runtime Stability** — long-run/reconnect/cleanup требуют валидации | 1.8.2 | 🟡 | Риск деградации при длительном просмотре | 3–5 дней |
| 3 | **Android Video + Background Recording** — интеграция с RTSP не завершена | 1.8.7 | ⚠️ | Блокирует Android MVP | 1–2 недели |
| 4 | **PostgreSQL Finalization** — staging cutover + rollback rehearsal | 1.5.6 | ⚠️ | Блокирует production DB path | 2–3 дня |
| 5 | **Security MVP Evidence** — HTTPS/pinning/credential encryption/audit | 1.9 | 🟡 | Блокирует релизный gate | 3–5 дней |
| 6 | **Screenshot Pipeline** — frame-based decode не завершен | 1.8.3 | 🟡 | Функциональность неполная | 3–5 дней |
| 7 | **E2E/UI Testing** — полностью отсутствуют | 1.10.4 | ❌ | Блокирует воспроизводимость приемки | 1–2 недели |

---

## 5. План доведения Фазы 1 до 100%

### Принцип: code-first (сначала backend/transport, потом UI/E2E)

Этот порядок минимизирует риск переделок, когда UI и E2E делаются раньше стабилизации transport/runtime.

---

### Неделя 1: Foundation + Data + RTSP Compilation

**Цель:** Стабилизировать backend-базу и начать активацию нативного видео.

| День | Задача | Выход |
|------|--------|-------|
| 1–2 | Фиксация MVP scope, синхронизация статусных документов | Утвержденный `MVP_PHASE1_SCOPE_BOUNDARY.md` без конфликтов |
| 2–3 | PostgreSQL staging cutover + rollback rehearsal | Отчет `POSTGRESQL_FINALIZATION_STAGING_REPORT_*.md`, gate `GO` |
| 3–4 | Установка зависимостей RTSP (CMake, FFmpeg, pkg-config) | Рабочее окружение для сборки native |
| 4–5 | Компиляция `libvideo_processing` для Desktop + проверка экспорта символов | `.so/.dylib/.dll` + verified symbols |
| 5 | Генерация cinterop биндингов + проверка компиляции Kotlin | `core:network:compileKotlin*` проходит |

**Критерий готовности недели 1:**
- [ ] PostgreSQL staging gate `GO`
- [ ] Native библиотека скомпилирована для Desktop
- [ ] Kotlin cinterop компилируется без ошибок

---

### Неделя 2: RTSP Activation + HLS Runtime + Screenshot

**Цель:** Активировать production-path видео и закрыть runtime-стабильность.

| День | Задача | Выход |
|------|--------|-------|
| 1–2 | Активация FFI в `NativeRtspClient.native.kt` (раскомментировать импорты, StableRef callbacks) | Рабочий Kotlin ↔ C++ bridge |
| 2–3 | Интеграция RTSP с VideoPlayer (Web/Desktop), fallback path | End-to-end сценарий discover → play |
| 3–4 | HLS long-run validation (30+ минут, reconnect, cleanup) | Отчет long-run smoke |
| 4–5 | Screenshot Pipeline: завершить `captureFrame(...)` + FFmpeg fallback | `ScreenshotServiceTest` проходит с реальным кадром |
| 5 | ONVIF Events: автоматизируемый HTTP-сценарий `scripts/onvif-events-api-verification.ps1` | Evidence 4/6 камер (MVP profile) |

**Критерий готовности недели 2:**
- [ ] RTSP native подключается к реальной камере, H.264 воспроизводится
- [ ] HLS pipeline стабилен при long-run
- [ ] Screenshot pipeline проходит smoke

---

### Неделя 3: Security Closure + Web Finalization + Android

**Цель:** Закрыть security gate и стабилизировать клиентские платформы.

| День | Задача | Выход |
|------|--------|-------|
| 1–2 | Security evidence: HTTPS topology validation, certificate pinning field test, credential migration logs, audit persistence | `SECURITY_MVP_READINESS` → `GO` |
| 2–3 | Web closure: финальная интеграция видеоплеера с RTSP/HLS backend, регресс `npm run build + npm test` | Web P1 задачи закрыты |
| 3–4 | Android: интеграция видеоплеера с RTSP/HLS, background recording service | Android smoke проходит |
| 4–5 | Desktop: long-run validation видеоплеера (30+ минут), EventTimeline, RecordingPlayer | Desktop smoke проходит |
| 5 | Rate limiting + Redis preflight тесты, `RateLimitMiddlewareFailureModeTest` | Server security contour green |

**Критерий готовности недели 3:**
- [ ] Security MVP checklist `GO`
- [ ] Web video player стабилен
- [ ] Android проходит обязательные MVP smoke
- [ ] Desktop проходит long-run validation

---

### Неделя 4: Testing + Acceptance Gate

**Цель:** Закрыть разрыв по качеству и провести формальную приемку.

| День | Задача | Выход |
|------|--------|-------|
| 1–2 | Integration tests: API, БД/миграции, video pipeline (`./gradlew mvpAutomatedAcceptance`) | CI job `mvp-automated-acceptance` green |
| 2–3 | Video E2E gate: `scripts/video-e2e-go-no-go.ps1` с профилем `config/video-e2e-acceptance-profile.mvp-ci.json` | `video-e2e-go-no-go-report.md` |
| 3–4 | Минимальный E2E набор: discover → add camera → start stream → view → record → replay → event | Ручной/автоматизированный отчет |
| 4–5 | Сводка в go/no-go матрицу (`RELEASE_GO_NO_GO_CHECKLIST.md`) | Принятое решение GO / CONDITIONAL / NO-GO |

**Критерий готовности недели 4:**
- [ ] Обязательный тестовый контур стабильно green
- [ ] Все MVP-критичные сценарии покрыты воспроизводимыми проверками
- [ ] Go/No-Go пакет подтвержден

---

## 6. Definition of Done для Фазы 1 = 100%

Фаза 1 считается закрытой при **одновременном** выполнении:

1. ✅ Все критические блокеры для MVP закрыты (RTSP native, HLS runtime, PostgreSQL).
2. ✅ Видео-контур production-ready: discover → play → record → replay → events (устойчивый сценарий).
3. ✅ Web realtime/auth/video сценарии завершены (P1 задачи закрыты).
4. ✅ Security MVP критерии выполнены (все контроли `PASS`, нет `CONDITIONAL`/`FAIL`).
5. ✅ Integration/E2E smoke контур стабильно проходит в CI.
6. ✅ Android и Desktop проходят обязательные MVP smoke без критических регрессий.
7. ✅ Go/No-Go пакет подтвержден релизными отчетами (`RELEASE_GO_NO_GO_CHECKLIST.md`).

---

## 7. Приоритеты реализации (упорядоченные)

| Приоритет | Задача | Обоснование |
|-----------|--------|-------------|
| P0 | RTSP native integration + compilation + FFI activation | Главный production-blocker для видео на всех платформах |
| P0 | PostgreSQL staging cutover + rollback rehearsal | Блокирует production DB path |
| P1 | HLS runtime stability (long-run, reconnect, cleanup) | Без этого видео неустойчиво в продакшене |
| P1 | Security MVP evidence (HTTPS, pinning, credentials, audit) | Блокирует релизный gate |
| P2 | Android video + background recording | Блокирует Android MVP |
| P2 | Screenshot pipeline completion | Неполная функциональность |
| P2 | Desktop long-run validation | Требуется для parity |
| P3 | E2E/UI testing contour | Воспроизводимость приемки |
| P3 | Integration test expansion (REST API beyond HLS) | Покрытие качества |

---

## 8. Метрики успеха MVP (целевые)

| Метрика | Цель | Текущее |
|---------|------|---------|
| Успешный старт live playback | ≥ 95% при N≥100 | ~75% (Web) |
| Непрерывное воспроизведение без деградации | ≥ 30 минут | Не подтверждено (RTSP) |
| Ошибки start/stop записи | ≤ 5% | ~82% (Recording Core) |
| Время до первого кадра (TTFF) | ≤ 5с в LAN | ~88% (HLS) |
| Покрытие тестами (unit+integration) | ≥ 50% | ~28% |
| Security controls | Все `PASS` | 2 `PASS`, 4 `CONDITIONAL` |
| Сценарий discover → play → record | Полный E2E green | Частичный (зависит от RTSP) |

---

## 9. Связанные документы

- [PROJECT_STATUS_PHASES.md](PROJECT_STATUS_PHASES.md) — детальный статус по всем фазам
- [PHASE1_MVP_TO_100_PLAN.md](../planning/PHASE1_MVP_TO_100_PLAN.md) — базовый рабочий план доведения до 100%
- [MVP_PHASE1_SCOPE_BOUNDARY.md](../planning/MVP_PHASE1_SCOPE_BOUNDARY.md) — границы MVP
- [CRITICAL_BLOCKERS_REMEDIATION_PLAN.md](../planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md) — план устранения блокеров
- [CANONICAL_BREAKDOWN_1_8_VIDEO_AND_RECORDING.md](CANONICAL_BREAKDOWN_1_8_VIDEO_AND_RECORDING.md) — канонический breakdown видео
- [SECURITY_MVP_READINESS.md](SECURITY_MVP_READINESS.md) — security go/no-go
- [RTSP_INTEGRATION_STATUS_UPDATE.md](RTSP_INTEGRATION_STATUS_UPDATE.md) — статус RTSP
- [MODULE_STATUS_BASELINE_2026-04-23.md](MODULE_STATUS_BASELINE_2026-04-23.md) — operational baseline модулей
- [MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](../automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md) — автоматизированный контур приемки
- [RELEASE_GO_NO_GO_CHECKLIST.md](../planning/RELEASE_GO_NO_GO_CHECKLIST.md) — релизный чеклист

---

**Документ создан:** 28 April 2026  
**Следующее обновление:** После закрытия каждого критического блокера или по завершении недельной итерации.
