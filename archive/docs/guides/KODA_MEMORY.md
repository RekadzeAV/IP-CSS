# KODA Memory — История и решения проекта IP-CSS

> Этот файл хранит ключевые решения, архитектурные соглашения и важные выводы проекта. Читается KODA перед каждой новой задачей.

**Последнее обновление:** 28 April 2026  
**Версия проекта:** Alfa-0.1.1

---

## 📋 О проекте

**Название**: IP-CSS (IP Camera Surveillance System)  
**Тип**: Кроссплатформенная система видеонаблюдения с AI-аналитикой  
**Основные технологии**: Kotlin Multiplatform, Ktor, SQLDelight, JavaCPP, FFmpeg, Docker, NGINX, Next.js

**Платформы:**
- **Серверные**: ARM (Raspberry Pi, Orange Pi), x86_64 (Linux, Windows, macOS), NAS (Synology, QNAP, Asustor)
- **Клиентские**: Android, iOS/macOS, Desktop (Windows, Linux, macOS)

**Текущий статус проекта:** ~81% завершённости

---

## 🔑 Ключевые архитектурные решения

### 1. Kotlin Multiplatform архитектура
- **shared/** - основной KMM модуль с бизнес-логикой (Clean Architecture: domain → data → common)
- **core/common/** - общие типы (Resolution, CameraStatus) + security expect/actual реестр
- **core/network/** - сетевые клиенты (Ktor, WebSocket, RTSP, ONVIF)
- Зависимости: `shared` зависит от `core:network` и `core:common`, `core:network` зависит только от `core:common` (избежание циклических зависимостей)
- **KMP Phase 1 завершён** — платформенная стабилизация: `expect/actual` security coverage, `commonMain` platform boundary guards, source-set dependency leakage checks

### 2. База данных
- **SQLDelight** для всех платформ (Android, iOS, Desktop)
- Единая схема в `shared/src/commonMain/sqldelight/`
- Платформо-специфичные `DatabaseFactory` реализации
- Репозитории: `CameraRepositoryImplSqlDelight`, `RecordingRepositoryImplSqlDelight`, `EventRepositoryImplSqlDelight` и др.

### 3. Серверная часть
- **Ktor REST API** с JWT аутентификацией
- WebSocket сервер для real-time уведомлений
- Middleware: Rate limiting, RBAC, Cookie auth, Security logging, HTTPS принудительно (HSTS/redirect)
- 85% API endpoints реализовано
- Серверные репозитории: in-memory (миграция на PostgreSQL запланирована)

### 4. Web-интерфейс
- Next.js 15 + TypeScript + Redux Toolkit
- 75% готовность (основные страницы, API интеграция, WebSocket, аутентификация)
- ⚠️ Видеоплеер требует интеграции с RTSP/HLS

### 5. Нативные компоненты
- C++ библиотеки через JavaCPP/FFmpeg
- Обработка видео, детекция объектов, трекинг, ANPR
- Собираются через CMake отдельно от Gradle
- RTSP клиент: ~50% готовности (C++ 85%, Kotlin 100%, cinterop 100%, совместимость 0%)

### 6. Платформенная организация
- Каждая платформа имеет директорию в `platforms/`
- Платформо-специфичный код через expect/actual
- **Desktop (Compose Desktop):** ~70% готовности (`platforms/client-desktop-x86_64/`)
- **Android:** ~35% готовности (базовая структура + ViewModels + DI)
- **iOS:** не начато
- Документация по платформам: `docs/PLATFORM_STRUCTURE.md`

---

## 🐛 Исправленные баги

### Gradle и компиляция
- **Kotlin Config Fix** - исправлена конфигурация Kotlin Multiplatform
- **JAVACPP Types Fix** - исправлены типы JavaCPP для FFmpeg
- **Expect/Actual проблемы** - resolved через KMP Phase 1 проверки
- **KMP Phase 1 Platform Stabilization** — завершена (security expect/actual coverage, commonMain boundaries, source-set dependency checks)
- **RTSP Compilation Fixes** (26-04-2026) — исправлены компиляционные ошибки RTSP клиента
- **Core Network Fixes** — исправлены зависимости core:network модуля

### Написание и сборка
- **Синхронизация с Gradle** - настройка wrapper и версий
- **Native компиляция** - CMake конфигурация для кроссплатформенной сборки
- **Docker Pre-release Gates** — автоматизированные проверки перед релизом

### См. также отчеты:
- `COMPILATION_FIXES_REPORT.md`
- `COMPILATION_FIXES_SUMMARY.md`
- `FINAL_IMPLEMENTATION_STATUS.md`
- `docs/kmp-phase1-progress.md`
- `docs/reports/PHASE1_AUTO_EXECUTION_STATUS_2026-04-26.md`
- `docs/reports/PHASE2_FINAL_COMPLETION_REPORT_2026-04-28.md`

---

## 📝 Важные заметки

### 1. Критические блокеры (обновлено 27 April 2026)
- **Video E2E gate** — runtime decision `GO`, strict release `NO-GO`, profile-aware release `GO` (canonical 1.8 = 64%, readiness signals 91.4%)
- **RTSP client production-readiness** — ~50% (long-run soak-test, drop/reconnect runtime validation)
- **ONVIF клиент** — базовая реализация + тесты (~40%), WS-Discovery частично реализован
- **AI-аналитика** — базовая структура (~15%), требуется ML интеграция
- **Desktop `3.2.1` → 100%** — smoke runtime evidence NOT_RUN, требуется завершение интеграции
- **PostgreSQL staging cutover/rollback rehearsal** — запланировано для server data path

### 2. CI/CD проверки
- **KMP Phase 1 verification** — обязательные проверки перед PR:
  - `python scripts/ci/verify-kmp-phase1.py` (one-shot: все KMP проверки + ключевые Gradle задачи)
  - `python scripts/ci/verify-kmp-phase1.py --ci-profile` (CI-эквивалент: `--skip-gradle --strict-runtime-matrix`)
  - `python scripts/ci/verify-kmp-phase1.py --strict-runtime-matrix` (строгий режим)
  - `python scripts/ci/check-commonmain-forbidden-imports.py .`
  - `python scripts/ci/check-security-expect-actual-signatures.py .`
  - `python scripts/ci/check-no-jvm-deps-in-native-source-sets.py .`
  - `python scripts/ci/check-video-runtime-matrix-config.py --root .`
  - `python scripts/ci/validate-video-e2e-profile.py --root .`
  - `./gradlew :core:common:compileKotlinMetadata :core:network:compileKotlinMetadata :shared:compileKotlinMetadata --no-daemon`
  - `./gradlew :core:common:desktopTest --no-daemon`
- **Docs link check** — валидация markdown ссылок в CI pipeline (`docs-link-check-active-scope`)
- **Docker Pre-release Gate** — автоматизированные проверки (`scripts/docker-pre-release-gate.ps1`)
- **Phase3 auto-execution** — автоматизированное выполнение Phase3 задач (`scripts/phase3-continue-auto.ps1`)

### 3. Security требования
- Все секреты через `.env` (в `.gitignore`)
- JWT токены с refresh mechanism (httpOnly cookies)
- BCrypt для хеширования паролей
- Certificate pinning для production (реализовано, требует field validation)
- HTTPS принудительно (middleware + HSTS/redirect)
- Security expect/actual registry: `SecureLocalDataEncryption`, `SecurePasswordEncryption`, `SecureMobileSecurityLogger`, `DigestCrypto`

### 4. Тестирование
- Unit тесты в `shared/src/commonTest/`
- Mock объекты для всех источников данных
- Integration тесты для ONVIF и WebSocket
- Smoke тесты для video pipeline
- Security contract tests (desktop/common-safe)
- Покрытие тестами: ~15% (целевое: 50%+)

### 5. Документация
- Актуальный статус: `docs/status/PROJECT_STATUS.md`
- Baseline модулей: `docs/status/MODULE_STATUS_BASELINE_2026-04-23.md`
- Детальный план: `docs/planning/DETAILED_DEVELOPMENT_PLAN.md`
- Video E2E runbook: `docs/reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md`
- KMP Phase 1: `docs/kmp-phase1-progress.md`, `docs/kmp-phase1-dod-checklist.md`, `docs/kmp-security-contract.md`
- Phase2 completion: `docs/reports/PHASE2_FINAL_COMPLETION_REPORT_2026-04-28.md`
- Phase3 auto-execution: `docs/reports/PHASE3_CONTINUE_AUTO_EXECUTION_STATUS_2026-04-27.md`
- Desktop implementation: `platforms/client-desktop-x86_64/IMPLEMENTATION_STATUS.md`

---

## 📚 Ссылки на документацию

### Основные документы
- [Основная документация](DOCUMENTATION_INDEX.md)
- [Структура проекта](PROJECT_STRUCTURE.md)
- [Статус проекта](docs/status/PROJECT_STATUS.md)
- [Архитектура](docs/ARCHITECTURE.md)
- [Руководство по разработке](docs/DEVELOPMENT.md)

### Технические детали
- [ONVIF клиент](docs/ONVIF_CLIENT.md)
- [RTSP клиент](docs/RTSP_CLIENT.md)
- [WebSocket клиент](docs/WEBSOCKET_CLIENT.md)
- [API документация](docs/API.md)
- [Certificate Pinning](docs/SECURITY_CERTIFICATE_PINNING_HTTPS_COMPLETE.md)
- [HTTPS Setup](docs/HTTPS_SETUP.md)

### Планирование и статус
- [Детальный план разработки](docs/planning/DETAILED_DEVELOPMENT_PLAN.md)
- [Критические блокеры](docs/planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md)
- [Baseline модулей](docs/status/MODULE_STATUS_BASELINE_2026-04-23.md)
- [Phase1 MVP to 100%](docs/planning/PHASE1_MVP_TO_100_PLAN.md)
- [Phase2 completion](docs/reports/PHASE2_FINAL_COMPLETION_REPORT_2026-04-28.md)
- [Phase3 auto-execution](docs/reports/PHASE3_CONTINUE_AUTO_EXECUTION_STATUS_2026-04-27.md)
- [Desktop implementation](platforms/client-desktop-x86_64/IMPLEMENTATION_STATUS.md)
- [Desktop detailed plan](docs/DESKTOP_DETAILED_PLAN.md)
- [Desktop refinement plan](docs/DESKTOP_REFINEMENT_PLAN.md)

### CI/CD и тестирование
- [KMP Phase 1 progress](docs/kmp-phase1-progress.md)
- [KMP Phase 1 DoD checklist](docs/kmp-phase1-dod-checklist.md)
- [KMP security contract](docs/kmp-security-contract.md)
- [Video E2E runbook](docs/reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md)
- [Тестирование](docs/TESTING.md)
- [Docker pre-release gate](docs/reports/DOCKER_PRE_RELEASE_GATE_REPORT_2026-04-28_005339.md)

### Платформы
- [Platform structure](docs/PLATFORM_STRUCTURE.md)
- [Platforms](docs/PLATFORMS.md)
- [NAS platforms analysis](docs/NAS_PLATFORMS_ANALYSIS.md)

---

## 📅 История изменений

| Дата | Изменения | Автор |
|------|-----------|-------|
| 28 April 2026 | Обновление: KMP Phase 1 completion, Phase2/Phase3 статусы, Desktop 70%, RTSP 50%, Certificate Pinning/HTTPS, Docker pre-release gates, security expect/actual registry | KODA |
| 27 April 2026 | Обновление: Video E2E gate (strict/profile-aware/runtime), Phase3 auto-execution, NAS field validation | KODA |
| 26 April 2026 | Первоначальное создание, заполнение контекста проекта | KODA |
| 23 April 2026 | Baseline модулей зафиксирован | Команда |

---

*Файл поддерживается автоматически KODA агентом*
