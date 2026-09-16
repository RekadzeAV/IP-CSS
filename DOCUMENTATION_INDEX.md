# Полный индекс документации проекта IP-CSS

**Версия документации:** 3.1
**Версия проекта:** 0.5.1.1-beta
**Последнее обновление:** 08 August 2026 (consolidation & version bump)

> **📚 Архивная документация:** Устаревшие документы и неиспользуемые модули сохранены в `archive/`
> **🔄 План реструктуризации:** [REFACTORING_PLAN_2026-06-29.md](archive/docs-deprecated-2026-09-04/REFACTORING_PLAN_2026-06-29.md)
> **🗂️ Сводный план проекта (0.5.1.1-beta):** [PLAN_SUMMARY.md](PLAN_SUMMARY.md) — все планы в одном документе

---

## 📚 Оглавление

1. [Начало работы](#начало-работы)
2. [Архитектура и структура](#архитектура-и-структура)
3. [Статус проекта](#статус-проекта)
4. [Техническая документация](#техническая-документация)
5. [Компоненты системы](#компоненты-системы)
6. [Разработка](#разработка)
7. [Развертывание](#развертывание)

---

## Начало работы

### Для новых разработчиков

1. **[README.md](README.md)** - Обзор проекта, быстрый старт
2. **[docs/status/PROJECT_STATUS.md](docs/status/PROJECT_STATUS.md)** - Статус и карта проекта (~81% прогресса) ⭐ ОСНОВНОЙ
   - quick status updates: `docs/status/STATUS_LOCK_2026-04-27.md`, `docs/status/VIDEO_GATE_LOCK_2026-04-27.md`, `docs/reports/STATUS_LOCK_AUTOMATION_MEMO_2026-04-27.md`
3. **[archive/docs-duplicates-2026-08-08/TODO.md](archive/docs-duplicates-2026-08-08/TODO.md)** - Архив: единый список задач (To-Do) заменён `PLAN_*`, `PLAN_EXECUTION_MASTER.md` и `REMAINING_TASKS.md`
4. **[docs/status/PROJECT_STATUS_PHASES.md](docs/status/PROJECT_STATUS_PHASES.md)** - Детальный план по фазам, этапам и задачам со статусом
5. **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Архитектура системы
6. **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)** - Структура проекта и модули
7. **[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)** - Руководство по разработке

---

## Архитектура и структура

- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Архитектура системы, слои, модули, принципы проектирования
- **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)** - Детальная структура проекта, модули, зависимости (ручная документация)
- **[docs/PROJECT_STRUCTURE_MANAGEMENT.md](docs/PROJECT_STRUCTURE_MANAGEMENT.md)** - Управление структурой проекта, автоматическое обновление ⭐ НОВОЕ
- **[docs/PLATFORMS.md](docs/PLATFORMS.md)** - Разделение разработки по платформам (Android, iOS, Desktop, Web, NAS)

---

## Статус проекта

> **Правило маркеров (актуально с 27 April 2026):**
> метки `⭐ НОВОЕ` / `⭐ ОБНОВЛЕНО` в этом индексе могут быть историческими.
> Для текущей operational-приоритизации status-обновлений используйте lock-контур:
> `docs/status/PROJECT_STATUS.md` + `docs/status/STATUS_LOCK_2026-04-27.md` + `docs/status/VIDEO_GATE_LOCK_2026-04-27.md` + `docs/reports/STATUS_LOCK_AUTOMATION_MEMO_2026-04-27.md`.

### Статус и карта проекта ⭐ ОБЪЕДИНЕНО

- **[docs/status/PROJECT_STATUS.md](docs/status/PROJECT_STATUS.md)** - Статус и план разработки проекта (~81%, фазы, блокеры) ⭐ ОСНОВНОЙ
- **Status quick-start (active wave):** `docs/status/STATUS_LOCK_2026-04-27.md` + `docs/status/VIDEO_GATE_LOCK_2026-04-27.md` + `docs/reports/STATUS_LOCK_AUTOMATION_MEMO_2026-04-27.md`
- **[docs/reports/STATUS_AUDIT_2026-04-23.md](docs/reports/STATUS_AUDIT_2026-04-23.md)** - Аудит статусов по коду: подтверждения и оговорки по ключевым процентам ⭐ НОВОЕ
- **[docs/status/PROJECT_STATUS_PHASES.md](docs/status/PROJECT_STATUS_PHASES.md)** - Детальный план по фазам, этапам и задачам со статусом выполнения
- **[docs/TODO.md](archive/docs-duplicates-2026-08-08/TODO.md)** - Единый список задач (To-Do): приоритеты P1–P4, недели W1–W4, фазы F1–F4; ссылки на план MVP и runbook (см. также [PHASE1_MVP_TO_100_PLAN.md](docs/planning/PHASE1_MVP_TO_100_PLAN.md))
- **[docs/archive/2026-03-30/TASKS_WITH_DESCRIPTIONS.md](docs/archive/2026-03-30/TASKS_WITH_DESCRIPTIONS.md)** - Архив: детальные таблицы задач по областям (заменён единым [TODO.md](archive/docs-duplicates-2026-08-08/TODO.md))
- **[docs/reports/ФАЗА_1_MVP_ДЕТАЛЬНЫЙ_ОТЧЕТ.md](archive/docs-deprecated-2026-09-04/ФАЗА_1_MVP_ДЕТАЛЬНЫЙ_ОТЧЕТ.md)** - Детальный отчет по Фазе 1: MVP (архив; актуальный статус — `REMAINING_TASKS.md`)
- **[docs/reports/BUILD_STATUS_CONSOLIDATED.md](docs/reports/BUILD_STATUS_CONSOLIDATED.md)** - Консолидированный отчет о статусе сборки ⭐ НОВОЕ
- **[docs/reports/DOCUMENTATION_CONSOLIDATION_REPORT_2026_01_28.md](docs/reports/DOCUMENTATION_CONSOLIDATION_REPORT_2026_01_28.md)** - Отчет о консолидации документации ⭐ НОВОЕ
- **[docs/reports/RELEASE_GO_NO_GO_RECORD_2026-03-27.md](docs/reports/RELEASE_GO_NO_GO_RECORD_2026-03-27.md)** - Зафиксированное решение GO/NO-GO по локальной релизной готовности ⭐ НОВОЕ
- **[docs/reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](docs/reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md)** - Runbook для video e2e strict/profile-aware/runtime решений ⭐ НОВОЕ
- **[docs/reports/DOCUMENTATION_LINK_AUDIT_2026-04-23.md](docs/reports/DOCUMENTATION_LINK_AUDIT_2026-04-23.md)** - Итоговый аудит связности и валидности ссылок документации ⭐ НОВОЕ
- **[docs/reports/SESSION_PROGRESS_2026-04-23.md](docs/reports/SESSION_PROGRESS_2026-04-23.md)** - Сводка изменений текущей сессии ⭐ НОВОЕ
- **[docs/reports/SESSION_COMMIT_GROUPING_2026-04-23.md](docs/reports/SESSION_COMMIT_GROUPING_2026-04-23.md)** - Группировка изменений для безопасных коммитов ⭐ НОВОЕ

### Статус компонентов

- **[docs/status/NATIVE_LIBRARIES_STATUS.md](docs/status/NATIVE_LIBRARIES_STATUS.md)** - Статус нативных библиотек (объединяет все документы по нативным библиотекам) ⭐ НОВОЕ
- **[docs/status/DATA_LAYER_STATUS.md](docs/status/DATA_LAYER_STATUS.md)** - Статус реализации Слоя данных (объединяет все документы по Data Layer) ⭐ НОВОЕ
- **[docs/status/CLOUD_SYNC_AND_STORAGE.md](docs/status/CLOUD_SYNC_AND_STORAGE.md)** - Синхронизация, облачное хранилище, бэкапы (реализация 4.1)
- **[docs/status/CLUSTER_LOAD_BALANCING_REPLICATION.md](docs/status/CLUSTER_LOAD_BALANCING_REPLICATION.md)** - Кластеризация, балансировка, репликация БД (реализация 4.2)
- **[docs/status/PLAN_OAUTH2_AND_SECURITY_MONITORING.md](docs/status/PLAN_OAUTH2_AND_SECURITY_MONITORING.md)** - OAuth2/OIDC и мониторинг безопасности

### Детальный анализ

- **[docs/IMPLEMENTATION_STATUS.md](archive/docs-duplicates-2026-08-08/IMPLEMENTATION_STATUS.md)** - Детальный статус реализации всех компонентов
- **[docs/MISSING_FUNCTIONALITY.md](archive/docs-deprecated-2026-09-04/MISSING_FUNCTIONALITY.md)** - Детальный анализ нереализованного функционала
- **[docs/TECHNICAL_DEBT.md](archive/docs-deprecated-2026-09-04/TECHNICAL_DEBT.md)** - Технический долг с разбивкой по типам доработок и примерными сроками ⭐ НОВОЕ
- **[docs/analysis/АНАЛИЗ_3.2.1_CameraRepositoryImpl.md](docs/analysis/АНАЛИЗ_3.2.1_CameraRepositoryImpl.md)** - Детальный анализ состояния разработки CameraRepositoryImpl (SQLDelight) с планом реализации кэширования ⭐ НОВОЕ
- **[docs/analysis/АНАЛИЗ_VIDEOPLAYER_6.3.3.md](docs/analysis/АНАЛИЗ_VIDEOPLAYER_6.3.3.md)** - Детальный анализ состояния разработки VideoPlayer (6.3.3) ⭐ НОВОЕ

### Планирование

- **[docs/POETAPNYJ_PLAN_REALIZACII.md](docs/POETAPNYJ_PLAN_REALIZACII.md)** - Поэтапный план реализации: фазы 0–5, цели, порядок выполнения, критерии готовности MVP ⭐ НОВОЕ
- **[docs/MVP_PHASED_IMPLEMENTATION_PLAN.md](archive/docs-deprecated-2026-09-04/MVP_PHASED_IMPLEMENTATION_PLAN.md)** - Детальный план MVP с задачами и статусом выполнения по каждой фазе
- **[docs/planning/LOCAL_RELEASE_BUILD_MASTER_PLAN.md](docs/planning/LOCAL_RELEASE_BUILD_MASTER_PLAN.md)** - Мастер-план локальной релизной сборки по типам выпуска ⭐ НОВОЕ
- **[docs/planning/PHASE1_MVP_TO_100_PLAN.md](docs/planning/PHASE1_MVP_TO_100_PLAN.md)** - Зафиксированный рабочий план доведения Фазы 1 (MVP) до 100% (базовый документ процесса разработки)
  - [docs/planning/PHASE1_MVP_TO_100_SECTIONS_3_4_DETAILED_PLAN.md](docs/planning/PHASE1_MVP_TO_100_SECTIONS_3_4_DETAILED_PLAN.md) - Детализация разделов 3–4: пошаговые этапы, приоритеты при дефиците ресурса, статус прогресса (P2, Android HLS)
  - [docs/planning/MVP_PHASE1_SCOPE_BOUNDARY.md](docs/planning/MVP_PHASE1_SCOPE_BOUNDARY.md) - Границы MVP: must-have сценарии, iOS вне критического пути Фазы 1
  - [docs/planning/POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md](docs/planning/POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md) - Staging cutover, smoke, rehearsal отката PostgreSQL (1.5.6)
  - [docs/planning/SECURITY_MVP_FIELD_VALIDATION_RUNBOOK.md](docs/planning/SECURITY_MVP_FIELD_VALIDATION_RUNBOOK.md) - Полевая валидация security MVP (pinning, HTTPS, credentials, audit)
  - [docs/planning/HLS_PIPELINE_1_8_2_LIVE_RECORDINGS_PLAN.md](docs/planning/HLS_PIPELINE_1_8_2_LIVE_RECORDINGS_PLAN.md) - HLS live + recordings (1.8.2): FFmpeg env, DoD, бэклог
  - [docs/planning/LOCAL_RELEASE_ANDROID_APK.md](docs/planning/LOCAL_RELEASE_ANDROID_APK.md) - Android APK release
  - [docs/planning/LOCAL_RELEASE_ANDROID_AAB.md](docs/planning/LOCAL_RELEASE_ANDROID_AAB.md) - Android AAB release
  - [docs/planning/LOCAL_RELEASE_DESKTOP_X86_64.md](docs/planning/LOCAL_RELEASE_DESKTOP_X86_64.md) - Desktop x86_64 release
  - [docs/planning/LOCAL_RELEASE_DESKTOP_ARM.md](docs/planning/LOCAL_RELEASE_DESKTOP_ARM.md) - Desktop ARM release
  - [docs/planning/LOCAL_RELEASE_SERVER_JVM.md](docs/planning/LOCAL_RELEASE_SERVER_JVM.md) - Server JVM release build
  - [docs/planning/LOCAL_RELEASE_NAS_PACKAGES.md](docs/planning/LOCAL_RELEASE_NAS_PACKAGES.md) - NAS release packages
- **[docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md](docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md)** - Финальный чеклист GO/NO-GO перед публикацией релиза ⭐ НОВОЕ
- **[docs/status/MODULE_STATUS_BASELINE_2026-04-23.md](docs/status/MODULE_STATUS_BASELINE_2026-04-23.md)** - Единый baseline модулей и прогресса на 23 April 2026 ⭐ НОВОЕ
- **[docs/BUILD_STABILIZATION.md](archive/docs/guides/BUILD_STABILIZATION.md)** - Стабилизация сборки: KMP (androidNative опционально), Koin, варианты сборки без Android, с чего продолжить ⭐ НОВОЕ
- **[docs/TESTING_EXECUTION_RESULT_PHASE0.md](archive/docs-deprecated-2026-09-04/TESTING_EXECUTION_RESULT_PHASE0.md)** - Результат проверки сборки по Фазе 0
- **[docs/TIMELINE_GUIDE.md](docs/TIMELINE_GUIDE.md)** - Руководство по ведению временной шкалы (обязательное ведение) ⭐ НОВОЕ
- **[docs/planning/DESKTOP_SERVER_IMPLEMENTATION_PLAN_V2.md](docs/planning/DESKTOP_SERVER_IMPLEMENTATION_PLAN_V2.md)** - 🎯 Детальный план реализации Desktop + Server с корректной последовательностью (6 фаз, 15 спринтов, 592 часа) ⭐ НОВОЕ
- **[archive/docs-deprecated-2026-09-04/DEVELOPMENT_PLAN.md](archive/docs-deprecated-2026-09-04/DEVELOPMENT_PLAN.md)** - План дальнейшей разработки по фазам
- **[PLAN_DEVELOPMENT.md](PLAN_DEVELOPMENT.md)** - План функциональных доработок (0.5.1.1-beta)
- **[PLAN_FIXES.md](PLAN_FIXES.md)** - План исправлений и рисков (0.5.1.1-beta)
- **[PLAN_VULNERABILITIES.md](PLAN_VULNERABILITIES.md)** - План устранения уязвимостей зависимостей: реестр VULN, CI-гейты, dependabot, процесс фиксации новых (0.5.1.1-beta)
- **[PLAN_TECH_DEBT_TODO.md](PLAN_TECH_DEBT_TODO.md)** - Техдолг и консолидация ToDo (0.5.1.1-beta)
- **[PLAN_REFACTORING.md](PLAN_REFACTORING.md)** - План рефакторинга (0.5.1.1-beta)
- **[PLAN_TESTING.md](PLAN_TESTING.md)** - План тестирования (0.5.1.1-beta)
- **[PLAN_TEST_BUILDS.md](PLAN_TEST_BUILDS.md)** - Выпуск тестовых сборок: установка, настройка, запуск (0.5.1.1-beta)
- **[PLAN_RELEASE.md](PLAN_RELEASE.md)** - План релиза (0.5.1.1-beta)
- **[PLAN_DOCUMENTATION.md](PLAN_DOCUMENTATION.md)** - Консолидация документации (0.5.1.1-beta)
- **[PLAN_RECOMMENDATIONS.md](PLAN_RECOMMENDATIONS.md)** - Рекомендации по тестированию, релизу, улучшениям (0.5.1.1-beta)
- **[PLAN_ROADMAP.md](PLAN_ROADMAP.md)** - План развития (Roadmap) (0.5.1.1-beta)
- **[docs/MIGRATION_IMPLEMENTATION_PLAN.md](archive/docs-deprecated-2026-09-04/MIGRATION_IMPLEMENTATION_PLAN.md)** - План реализации миграций базы данных (3.1.4) ⭐ НОВОЕ

> **📦 Архивные документы:** Старые версии документов (PROJECT_ROADMAP.md, DEVELOPMENT_ROADMAP.md, DEVELOPMENT_MAP.md, CURRENT_STATUS.md, DETAILED_DEVELOPMENT_PLAN.md, INSTALLATION_SUMMARY.md, QUICK_INSTALL.md, LOCAL_BUILD_REQUIREMENTS.md, NATIVE_LIBRARIES_*.md, DATA_LAYER_*.md) сохранены в `docs/archive/duplicates-2026-01-27/`

---

## Техническая документация

### API

- **[docs/API.md](docs/API.md)** - REST API документация (endpoints, модели, примеры запросов/ответов)
- **[docs/API_EXAMPLES.md](archive/docs/api/API_EXAMPLES.md)** - Практические примеры использования API (JavaScript, Python, Kotlin) ⭐ НОВОЕ
- **[server/web/README.md](server/web/README.md)** - Документация веб-интерфейса (Next.js)
- **[server/web/WEB_UI_IMPLEMENTATION.md](server/web/WEB_UI_IMPLEMENTATION.md)** - Детальная документация реализации веб-интерфейса

### Конфигурация и настройка

- **[docs/CONFIGURATION.md](docs/CONFIGURATION.md)** - Руководство по настройке и конфигурации системы ⭐ НОВОЕ
- **[docs/ENVIRONMENT_VARIABLES.md](docs/ENVIRONMENT_VARIABLES.md)** - Документация переменных окружения ⭐ НОВОЕ
- **[docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)** - Руководство по устранению неполадок ⭐ НОВОЕ

### Протоколы и клиенты

- **[docs/IP_CAMERA_ANALYSIS.md](archive/docs/analysis/IP_CAMERA_ANALYSIS.md)** - 📊 Полный анализ IP-камер: производители, особенности, протоколы, требования к проекту ⭐ НОВОЕ
- **[docs/RTSP_CLIENT.md](archive/docs-duplicates-2026-08-08/RTSP_CLIENT.md)** - Полная документация RTSP клиента (использование, API, установка, активация, статус реализации) ⭐ ОБНОВЛЕНО
- **[docs/RTSP_CODEC_AND_PLATFORM_LIMITATIONS.md](archive/docs-duplicates-2026-08-08/RTSP_CODEC_AND_PLATFORM_LIMITATIONS.md)** - Ограничения: поддерживаемые кодеки/профили, нужен ли FFmpeg на устройстве по платформам
- **[docs/rtsp/NATIVE_LIBRARY_BUILD.md](docs/rtsp/NATIVE_LIBRARY_BUILD.md)** - Руководство по сборке нативной библиотеки RTSP клиента ⭐ НОВЫЙ
- **[docs/rtsp/BUILD_QUICKSTART.md](docs/rtsp/BUILD_QUICKSTART.md)** - Быстрый старт: сборка нативной библиотеки ⭐ НОВЫЙ
- **[docs/ONVIF_CLIENT.md](docs/ONVIF_CLIENT.md)** - Документация ONVIF клиента (использование, API, статус реализации) ⭐ ОБНОВЛЕНО
- **[docs/ONVIF_CLIENT_IMPLEMENTATION_PLAN.md](archive/docs/onvif/ONVIF_CLIENT_IMPLEMENTATION_PLAN.md)** - 📋 Детальный пошаговый план реализации ONVIF клиента (5 этапов) ⭐ НОВОЕ
- **[docs/ONVIF_DIGEST_AUTH.md](docs/ONVIF_DIGEST_AUTH.md)** - 🔐 Digest Authentication для ONVIF (в разработке) ⭐ НОВОЕ
- **[docs/ONVIF_UPNP.md](docs/ONVIF_UPNP.md)** - 🔍 UPnP интеграция для обнаружения устройств (запланировано) ⭐ НОВОЕ
- **[docs/ONVIF_TROUBLESHOOTING.md](docs/ONVIF_TROUBLESHOOTING.md)** - 🔧 Решение проблем и FAQ по ONVIF ⭐ НОВОЕ
- **[docs/WEBSOCKET_CLIENT.md](archive/docs-duplicates-2026-08-08/WEBSOCKET_CLIENT.md)** - Документация WebSocket клиента (использование, API, статус реализации)
- **[docs/WEBSOCKET_CLIENT_IMPLEMENTATION_PLAN.md](archive/docs-duplicates-2026-08-08/WEBSOCKET_CLIENT_IMPLEMENTATION_PLAN.md)** - Детальный план реализации WebSocket клиента (6 этапов, обработка бинарных сообщений, очередь, rate limiting) ⭐ НОВОЕ
- **[docs/VIDEO_PLAYER_RTSP_HLS_INTEGRATION.md](archive/docs-duplicates-2026-08-08/VIDEO_PLAYER_RTSP_HLS_INTEGRATION.md)** - Полная документация интеграции видеоплеера с RTSP/HLS (веб и Android) ⭐ НОВОЕ
- **[docs/ADAPTIVE_BITRATE_AND_WEBRTC_IMPLEMENTATION.md](archive/docs-deprecated-2026-09-04/ADAPTIVE_BITRATE_AND_WEBRTC_IMPLEMENTATION.md)** - Документация адаптивного битрейта для HLS и WebRTC ⭐ НОВОЕ
- **[docs/JANUS_MEDIA_SERVER_INTEGRATION.md](archive/docs/guides/JANUS_MEDIA_SERVER_INTEGRATION.md)** - Интеграция Janus Media Server для WebRTC (RTSP-to-WebRTC) ⭐ НОВОЕ

> **📝 RTSP документация:** Подробные инструкции по установке, активации и интеграции включены в [docs/RTSP_CLIENT.md](archive/docs-duplicates-2026-08-08/RTSP_CLIENT.md). Дополнительные документы в [docs/rtsp/](docs/rtsp/) содержат версионированные версии.

### Системные компоненты

- ⏸️ **[docs/LICENSE_SYSTEM.md](archive/docs-deprecated-2026-09-04/LICENSE_SYSTEM.md)** - Система лицензирования (отложено - вынесено за рамки проекта)
- **[docs/INTEGRATION_GUIDE.md](docs/INTEGRATION_GUIDE.md)** - Руководство по интеграции библиотек (XML парсинг, Live555, FFmpeg, OpenCV, TensorFlow Lite)
- **[docs/AI_ANALYTICS.md](docs/AI_ANALYTICS.md)** - AI-аналитика в системах видеонаблюдения (детекция объектов, ANPR, трекинг, машинное зрение) ⭐ НОВОЕ

### База данных

- **[docs/MIGRATION_IMPLEMENTATION_PLAN.md](archive/docs-deprecated-2026-09-04/MIGRATION_IMPLEMENTATION_PLAN.md)** - План реализации системы миграций базы данных (SQLDelight) ⭐ НОВОЕ
  - 9 этапов реализации
  - Система версионирования схем
  - MigrationManager и утилиты
  - Тестирование и документация
- **[docs/analysis/АНАЛИЗ_3.2.1_CameraRepositoryImpl.md](docs/analysis/АНАЛИЗ_3.2.1_CameraRepositoryImpl.md)** - Детальный анализ CameraRepositoryImpl (SQLDelight) с планом реализации кэширования ⭐ НОВОЕ
  - Текущее состояние реализации (~90% готово)
  - План работ по кэшированию (6 задач, 7-9 дней)
  - Технические детали и примеры кода
  - Критерии готовности

### Серверные сервисы

- **[docs/VIDEO_RECORDING_IMPLEMENTATION.md](archive/docs-deprecated-2026-09-04/VIDEO_RECORDING_IMPLEMENTATION.md)** - Документация реализации записи видео (VideoRecordingService, VideoStreamService, FfmpegService, StorageService)
- **Серверные сервисы API (server/api/src/main/kotlin/.../service/):**
  - `VideoRecordingService` - управление жизненным циклом записей (старт, стоп, пауза, возобновление)
  - `VideoStreamService` - управление видеопотоками (запуск/остановка)
  - `HlsGeneratorService` - генерация HLS плейлистов и сегментов для веб-плеера
  - `ScreenshotService` - создание снимков с камер
  - `FfmpegService` - обертка для работы с FFmpeg (thumbnail'ы, конвертация, метаданные)
  - `StorageService` - управление хранилищем записей (проверка места, очистка, расчет использования)
  - `PasswordService` - хеширование паролей (BCrypt)
- **Серверные репозитории (server/api/src/main/kotlin/.../repository/):**
  - `ServerUserRepository` - управление пользователями на сервере (JWT аутентификация, хеширование паролей, управление ролями)
  - `ServerEventRepository` - репозиторий событий для сервера (SQLDelight, интеграция с WebSocket, фильтрация и пагинация)
  - `ServerRecordingRepository` / `ServerRecordingRepositorySqlDelight` - репозиторий записей для сервера (SQLDelight, интеграция с VideoRecordingService и WebSocket, управление файлами)
  - `ServerSettingsRepository` - репозиторий настроек для сервера (SQLDelight, импорт/экспорт, валидация значений)
- **Серверные middleware (server/api/src/main/kotlin/.../middleware/):**
  - `AuthorizationMiddleware` - проверка прав доступа (RBAC)
  - `CookieAuthMiddleware` - управление аутентификацией через cookies
  - `RateLimitMiddleware` - ограничение частоты запросов (защита от брутфорса)
  - `ValidationMiddleware` - валидация входящих запросов (использует RequestValidator)

---

## Компоненты системы

### Сетевые клиенты

- **[docs/ONVIF_CLIENT.md](docs/ONVIF_CLIENT.md)** - ONVIF клиент для работы с IP-камерами ⭐ ОБНОВЛЕНО
- **[docs/ONVIF_CLIENT_IMPLEMENTATION_PLAN.md](archive/docs/onvif/ONVIF_CLIENT_IMPLEMENTATION_PLAN.md)** - План реализации ONVIF клиента ⭐ НОВОЕ
- **[docs/ONVIF_DIGEST_AUTH.md](docs/ONVIF_DIGEST_AUTH.md)** - Digest Authentication ⭐ НОВОЕ
- **[docs/ONVIF_UPNP.md](docs/ONVIF_UPNP.md)** - UPnP интеграция ⭐ НОВОЕ
- **[docs/ONVIF_TROUBLESHOOTING.md](docs/ONVIF_TROUBLESHOOTING.md)** - Решение проблем ⭐ НОВОЕ
- **[docs/RTSP_CLIENT.md](archive/docs-duplicates-2026-08-08/RTSP_CLIENT.md)** - RTSP клиент для получения видеопотоков
- **[docs/WEBSOCKET_CLIENT.md](archive/docs-duplicates-2026-08-08/WEBSOCKET_CLIENT.md)** - WebSocket клиент для real-time коммуникации
- **[docs/WEBSOCKET_CLIENT_IMPLEMENTATION_PLAN.md](archive/docs-duplicates-2026-08-08/WEBSOCKET_CLIENT_IMPLEMENTATION_PLAN.md)** - План реализации WebSocket клиента (бинарные сообщения, очередь, rate limiting) ⭐ НОВОЕ

### AI-аналитика и машинное зрение

- **[docs/AI_ANALYTICS.md](docs/AI_ANALYTICS.md)** - Комплексная документация по AI-аналитике ⭐ НОВОЕ
  - Детекция объектов (люди, транспортные средства)
  - Распознавание номеров авто (ANPR/LPR)
  - Трекинг объектов
  - Детекция движения
  - Распознавание лиц
- **[docs/AI_MODELS_RECOMMENDATIONS.md](archive/docs-deprecated-2026-09-04/AI_MODELS_RECOMMENDATIONS.md)** - Рекомендации по дополнительным AI моделям ⭐ НОВОЕ
  - Face Recognition (распознавание лиц)
  - Behavior Analysis (анализ поведения, детекция падений)
  - Crowd Density Estimation (оценка плотности толпы)
  - Fire/Smoke Detection (детекция огня и дыма)
  - И другие модели для расширенной аналитики
  - Машинное зрение и компьютерное зрение
  - Интеграция с системой
  - Производительность и оптимизация

### Интеграция библиотек

- **[docs/INTEGRATION_GUIDE.md](docs/INTEGRATION_GUIDE.md)** - Руководство по интеграции внешних библиотек
- **[docs/REQUIRED_LIBRARIES.md](docs/REQUIRED_LIBRARIES.md)** - Полный список требуемых библиотек
- **[docs/REQUIRED_LIBRARIES_SUMMARY.md](docs/REQUIRED_LIBRARIES_SUMMARY.md)** - Краткая сводка библиотек

### Производительность и оптимизация

- **[docs/PERFORMANCE.md](docs/PERFORMANCE.md)** - Руководство по оптимизации производительности ⭐ НОВОЕ
- **[docs/VIDEO_CODECS.md](docs/VIDEO_CODECS.md)** - Документация по поддерживаемым видеокодекам ⭐ НОВОЕ
- **[docs/planning/VIDEO_SURVEILLANCE_COMPLIANCE_AUDIT_AND_TZ_2026.md](docs/planning/VIDEO_SURVEILLANCE_COMPLIANCE_AUDIT_AND_TZ_2026.md)** - Аудит видеопараметров IP-CSS vs отраслевой чеклист: пробелы, формулировки для ТЗ (разрешение, FPS, HEVC, ONVIF, px/m, ИИ/VSaaS) ⭐ НОВОЕ
- **[docs/planning/HLS_TRANSCODE_PROFILES.md](docs/planning/HLS_TRANSCODE_PROFILES.md)** - Таблица профилей HLS (разрешение, FPS, битрейт, HEVC-флаг) ⭐ НОВОЕ
- **[docs/planning/VSaaS_REQUIREMENTS_DRAFT.md](docs/planning/VSaaS_REQUIREMENTS_DRAFT.md)** - Черновик требований VSaaS / размещение аналитики ⭐ НОВОЕ
- **[docs/planning/VVC_ROADMAP_ASSESSMENT.md](docs/planning/VVC_ROADMAP_ASSESSMENT.md)** - Оценка H.266/VVC (go/no-go) ⭐ НОВОЕ

### Нативные библиотеки

- **[docs/status/NATIVE_LIBRARIES_STATUS.md](docs/status/NATIVE_LIBRARIES_STATUS.md)** - Статус нативных библиотек (объединяет анализ, сборку, реализацию, интеграцию) ⭐ НОВОЕ
- **[docs/rtsp/NATIVE_LIBRARY_BUILD.md](docs/rtsp/NATIVE_LIBRARY_BUILD.md)** - Инструкция по сборке нативной библиотеки RTSP клиента и активации декодера
- **[docs/NATIVE_LIBRARIES_INTEGRATION.md](docs/NATIVE_LIBRARIES_INTEGRATION.md)** - Интеграция нативных C++ библиотек

---

## Разработка

### Руководства

- **[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)** - Руководство по разработке (настройка окружения, сборка, запуск)
- **[docs/TIMELINE_GUIDE.md](docs/TIMELINE_GUIDE.md)** - Руководство по ведению временной шкалы проекта (обязательное) ⭐ НОВОЕ
- **[archive/docs-deprecated-2026-09-04/DEVELOPMENT_PLAN.md](archive/docs-deprecated-2026-09-04/DEVELOPMENT_PLAN.md)** - План дальнейшей разработки
- **Desktop приложение:**
  - **[docs/DESKTOP_IMPLEMENTATION_PLAN.md](docs/archive/2026-04-27/plans-obsolete/DESKTOP_IMPLEMENTATION_PLAN.md)** - Базовый план реализации Desktop приложения ⭐ НОВОЕ
  - **[docs/DESKTOP_DETAILED_PLAN.md](archive/docs/guides/DESKTOP_DETAILED_PLAN.md)** - Детальный план с разбивкой на задачи (8 этапов, матрица задач) ⭐ НОВОЕ
  - **[docs/DESKTOP_PLAN_SUMMARY.md](docs/archive/2026-04-27/plans-obsolete/DESKTOP_PLAN_SUMMARY.md)** - Краткая сводка плана Desktop приложения ⭐ НОВОЕ
  - **[docs/DESKTOP_REFINEMENT_PLAN.md](archive/docs/guides/DESKTOP_REFINEMENT_PLAN.md)** - План доработки Desktop приложения (574 часа, 4 фазы) ⭐ НОВОЕ
  - **[docs/DESKTOP_OPTIMIZATION_GUIDE.md](archive/docs/guides/DESKTOP_OPTIMIZATION_GUIDE.md)** - Руководство по оптимизации Desktop приложения ⭐ НОВОЕ
  - **[docs/DESKTOP_DOCUMENTATION_UPDATE.md](archive/docs/guides/DESKTOP_DOCUMENTATION_UPDATE.md)** - Отчет об обновлении документации Desktop приложения ⭐ НОВОЕ
  - **[platforms/client-desktop-x86_64/IMPLEMENTATION_STATUS.md](platforms/client-desktop-x86_64/IMPLEMENTATION_STATUS.md)** - Статус реализации Desktop приложения (~70% прогресса) ⭐ НОВОЕ
- **[docs/DEVELOPMENT_TOOLS.md](archive/docs/guides/DEVELOPMENT_TOOLS.md)** - Инструменты разработки
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Руководство по участию в разработке (включая требования по ведению TIMELINE.md)
- **[docs/ISSUE_DESCRIPTION_GUIDELINES.md](archive/docs/guides/ISSUE_DESCRIPTION_GUIDELINES.md)** - Правила формирования описаний для Issues (ошибки, баги UI, дизайн, производительность, идеи) ⭐ НОВОЕ

### Установка и сборка

- **[docs/installation/INSTALL_INSTRUCTIONS.md](docs/installation/INSTALL_INSTRUCTIONS.md)** - Руководство по установке компонентов для сборки (объединяет все документы по установке) ⭐ ОБНОВЛЕНО
- **[docs/BUILD_QUICK_REFERENCE.md](archive/docs/guides/BUILD_QUICK_REFERENCE.md)** - Быстрая справка по командам и конфигурации ⭐ НОВОЕ
- **[docs/LOCAL_BUILD.md](archive/docs/guides/LOCAL_BUILD.md)** - Локальная сборка и публикация пакетов (быстрый старт) ⭐ НОВОЕ
- **[docs/BUILD_ORGANIZATION.md](archive/docs/guides/BUILD_ORGANIZATION.md)** - Организация локальной сборки (архитектура, процесс, конфигурация) ⭐ НОВОЕ
- **[docs/BUILD_TROUBLESHOOTING.md](archive/docs/guides/BUILD_TROUBLESHOOTING.md)** - Устранение проблем при локальной сборке ⭐ НОВОЕ

### Тестирование

- **[docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md)** - Контур `./gradlew mvpAutomatedAcceptance`, **`scripts/ci/mvp-automated-acceptance.sh`** / **`mvp-automated-acceptance.ps1`** (web + video gate; в GitHub Actions — **`config/video-e2e-acceptance-profile.mvp-ci.json`**, Phase1 summary), KMP verify, W4, runbook’и; раздел **8** (в т.ч. **§8.15** — `native/build-stub-libs.ps1`, `-ShowHelp`)
- **[docs/automation/MVP_ONVIF_EVENTS_VERIFICATION.md](docs/automation/MVP_ONVIF_EVENTS_VERIFICATION.md)** - Проверка ONVIF Events для MVP (тесты + скрипт с камерой)
- **[docs/TESTING.md](docs/TESTING.md)** - Стратегия тестирования, типы тестов
- **[docs/TESTS_SUMMARY.md](archive/docs-duplicates-2026-08-08/TESTS_SUMMARY.md)** - Сводка созданных тестов
- **[docs/TESTING_IMPLEMENTATION_SUMMARY.md](archive/docs-duplicates-2026-08-08/TESTING_IMPLEMENTATION_SUMMARY.md)** - Сводка по реализации тестирования (45 тестов создано) ⭐ ОБНОВЛЕНО
- **[docs/TASKS_FOR_REFINEMENT_AND_DEBUGGING.md](archive/docs-deprecated-2026-09-04/TASKS_FOR_REFINEMENT_AND_DEBUGGING.md)** - Детальный список задач для доработки и отладки ⭐ НОВОЕ
- **[shared/src/commonTest/README.md](shared/src/commonTest/README.md)** - Документация тестов для модуля shared
- **Integration тесты:**
  - Certificate Pinning integration тесты с реальными сертификатами ⭐ НОВОЕ
  - RTSP клиент integration тесты с реальными серверами ⭐ НОВОЕ
- **Platform-specific тесты:**
  - Android: Certificate Pinning unit тесты ⭐ НОВОЕ
  - iOS: Certificate Pinning unit тесты ⭐ НОВОЕ
  - JVM: Certificate Pinning unit тесты ⭐ НОВОЕ

### Анализ и проектирование

- **[docs/DOCUMENTATION_ANALYSIS_REPORT.md](archive/docs/meta/DOCUMENTATION_ANALYSIS_REPORT.md)** - Комплексный аналитический отчет о состоянии документации ⭐ НОВОЕ
- **[docs/ANALYSIS_SUMMARY_2025.md](docs/archive/2026-04-27/analyses-old/ANALYSIS_SUMMARY_2025.md)** - Резюме анализа проекта (january 2026) ⭐ НОВОЕ
- **[docs/DEEP_ANALYSIS_2025.md](docs/archive/2026-04-27/analyses-old/DEEP_ANALYSIS_2025.md)** - Углубленный расширенный анализ проекта (january 2026) ⭐ НОВОЕ
- **[docs/PROMPT_ANALYSIS.md](docs/PROMPT_ANALYSIS.md)** - Анализ исходного промта проекта
- **[docs/ANALYSIS_ERRORS.md](archive/docs/analysis/ANALYSIS_ERRORS.md)** - Анализ ошибок и проблем
- **[docs/DOCUMENTATION_GAPS.md](archive/docs/meta/DOCUMENTATION_GAPS.md)** - Анализ недостающей документации
- **[docs/PROJECT_FULL_ANALYSIS.md](archive/docs-duplicates-2026-08-08/PROJECT_FULL_ANALYSIS.md)** - Полный углубленный анализ всего проекта

---

## Развертывание

- **[docs/DEPLOYMENT_GUIDE.md](archive/docs/deployment/DEPLOYMENT_GUIDE.md)** - Руководство по развертыванию (Docker, Kubernetes, NAS)

### HTTPS и Certificate Pinning (продакшен)

- **[docs/HTTPS_AND_PINS_PRODUCTION_QUICKSTART.md](archive/docs/guides/HTTPS_AND_PINS_PRODUCTION_QUICKSTART.md)** - Краткая инструкция: как включить HTTPS на сервере, как сгенерировать и добавить pins для продакшена
- **[docs/PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md](docs/PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md)** - Полная настройка: HTTPS (nginx/Let's Encrypt), certificate pinning, мониторинг, ротация сертификатов
- **[config/README-CERTIFICATE-PINS.md](archive/config/certificate-pins/README-CERTIFICATE-PINS.md)** - Формат конфига pins, пути по платформам, переменные окружения
- **[docs/NAS_PLATFORMS_ANALYSIS.md](archive/docs/analysis/NAS_PLATFORMS_ANALYSIS.md)** - Детальный анализ NAS платформ (производители, ОС, архитектуры, форматы пакетов, план реализации)
- **[docs/planning/NAS_PLATFORM_IMPLEMENTATION_PLAN.md](docs/planning/NAS_PLATFORM_IMPLEMENTATION_PLAN.md)** - Поэтапный план реализации NAS платформы (4 фазы, 8 недель) ⭐ НОВОЕ
- **[platforms/nas-x86_64/IMPLEMENTATION_STATUS.md](platforms/nas-x86_64/IMPLEMENTATION_STATUS.md)** - Статус реализации NAS x86_64 платформы (~55% прогресса) ⭐ НОВОЕ
- **[docker-compose.yml](docker-compose.yml)** - Docker Compose конфигурация
- **[Dockerfile](Dockerfile)** - Docker образ для сервера

### CI/CD инфраструктура

- **Inf-pipeline/README.md *(утерян/в архиве)*** - Обзор документации по CI/CD инфраструктуре ⭐ НОВОЕ
- **Inf-pipeline/RASPBERRY_PI_JENKINS_ANALYSIS.md *(утерян/в архиве)*** - Анализ установки Jenkins + Blue Ocean на Raspberry Pi 4 и Pi 5 ⭐ НОВОЕ
- **Inf-pipeline/SYNOLOGY_GIT_MIRROR_ANALYSIS.md *(утерян/в архиве)*** - Анализ создания зеркала репозитория GitHub на Synology RS2416+ (DSM 7.2) ⭐ НОВОЕ

---

## Документация для пользователей

### Руководства по ролям

- **[docs/ENGINEER_GUIDE.md](archive/docs/guides/ENGINEER_GUIDE.md)** - Руководство для инженеров по развертыванию и первичной настройке ⭐ НОВОЕ
  - Полная установка и настройка всех компонентов системы
  - Развертывание на различных платформах (Docker, Linux, Windows, NAS)
  - Первичная конфигурация системы
  - Настройка безопасности и производительности
  - Мониторинг и диагностика
  - Устранение неполадок

- **[docs/ADMINISTRATOR_GUIDE.md](docs/ADMINISTRATOR_GUIDE.md)** - Руководство для администраторов системы
  - Установка и развертывание на всех платформах (серверы, NAS, микрокомпьютеры)
  - Поддержка всех архитектур (x86_64, ARM, ARM64)
  - Начальная настройка системы
  - Управление пользователями и правами доступа
  - Конфигурация системы и камер
  - Мониторинг и обслуживание
  - Резервное копирование и восстановление
  - Безопасность
  - Устранение неполадок

- **[docs/OPERATOR_GUIDE.md](docs/OPERATOR_GUIDE.md)** - Руководство для операторов системы
  - Работа с камерами (добавление, редактирование, настройка)
  - Просмотр видео в реальном времени
  - Управление записями (просмотр, поиск, экспорт)
  - Работа с событиями (просмотр, подтверждение, фильтрация)
  - Управление PTZ камерами
  - Настройки оператора
  - Поддержка всех платформ (веб, мобильные, desktop)

- **[docs/USER_GUIDE.md](docs/USER_GUIDE.md)** - Руководство для конечных пользователей
  - Просмотр камер в реальном времени
  - Просмотр записей и событий
  - Экспорт записей
  - Настройки профиля
  - Работа с мобильными приложениями
  - Часто задаваемые вопросы
  - Поддержка всех платформ и устройств

---

## Дополнительные документы

### Управление документацией

- **[docs/DOCUMENTATION_MANAGEMENT.md](archive/docs/meta/DOCUMENTATION_MANAGEMENT.md)** - Система управления документацией (создание, обновление, слияние) ⭐ НОВОЕ
- **[docs/DOCUMENTATION_VERSIONING.md](archive/docs/meta/DOCUMENTATION_VERSIONING.md)** - Процесс версионирования документации ⭐ НОВОЕ
- **[docs/DOCUMENTATION_REVISION_REPORT.md](archive/docs/meta/DOCUMENTATION_REVISION_REPORT.md)** - Отчет о ревизии документации (January 2026) ⭐ НОВОЕ
- **[docs/DOCUMENTATION_GAPS.md](archive/docs/meta/DOCUMENTATION_GAPS.md)** - Анализ недостающей документации
- **[docs/DOCUMENTATION_OPTIMIZATION_PROPOSALS.md](archive/docs/meta/DOCUMENTATION_OPTIMIZATION_PROPOSALS.md)** - Предложения по оптимизации документации ⭐ НОВОЕ
- **[docs/archive/README.md](docs/archive/README.md)** - Описание архива документации ⭐ НОВОЕ

### Безопасность и аутентификация

- **[docs/SECURITY_HEADERS.md](docs/SECURITY_HEADERS.md)** - Полная документация по Security Headers (CSP, HSTS, X-Frame-Options и др.) ⭐ НОВОЕ
- **[docs/SECURITY_AUDIT_REPORT.md](archive/docs-duplicates-2026-08-08/SECURITY_AUDIT_REPORT.md)** - Отчет аудита безопасности (обновлено: январь 2026) ⚠️ 25 уязвимостей выявлено
- **[docs/SECURITY_BEST_PRACTICES.md](docs/SECURITY_BEST_PRACTICES.md)** - Лучшие практики безопасности ⭐ ОБНОВЛЕНО (добавлен раздел о Security Headers)
- **[docs/kmp-security-contract.md](docs/kmp-security-contract.md)** - Контракт KMP security/encryption (expect/actual, поведение ошибок, redaction логов) ⭐ НОВОЕ
- **[docs/kmp-phase1-dod-checklist.md](docs/kmp-phase1-dod-checklist.md)** - Чеклист DoD Фазы 1 KMP stabilization ⭐ НОВОЕ
- **[docs/kmp-phase1-progress.md](docs/kmp-phase1-progress.md)** - Журнал выполнения Фазы 1 KMP stabilization ⭐ НОВОЕ
- **[scripts/ci/verify-kmp-phase1.py](scripts/ci/verify-kmp-phase1.py)** - One-shot локальная верификация KMP Phase 1 (python, cross-platform) ⭐ НОВОЕ
- **[scripts/ci/verify-kmp-phase1.ps1](scripts/ci/verify-kmp-phase1.ps1)** - One-shot локальная верификация KMP Phase 1 для Windows PowerShell ⭐ НОВОЕ
- **[scripts/ci/verify-kmp-phase1.sh](scripts/ci/verify-kmp-phase1.sh)** - One-shot локальная верификация KMP Phase 1 для Linux/macOS/WSL ⭐ НОВОЕ
- **[scripts/ci/check-video-runtime-matrix-config.py](scripts/ci/check-video-runtime-matrix-config.py)** - Валидация `config/video-runtime-matrix*.json` (strict/non-strict режимы) ⭐ НОВОЕ
- **[scripts/ci/validate-video-e2e-profile.py](scripts/ci/validate-video-e2e-profile.py)** - Валидация `config/video-e2e-acceptance-profile*.json` (example/local) ⭐ НОВОЕ
- **[docs/USER_MANAGEMENT_SSO_KERBEROS_ANALYSIS.md](archive/docs-duplicates-2026-08-08/USER_MANAGEMENT_SSO_KERBEROS_ANALYSIS.md)** - Углубленный анализ управления пользователями и интеграции с SSO/Kerberos
- **[docs/SECURITY_REMEDIATION_PLAN.md](archive/docs-deprecated-2026-09-04/SECURITY_REMEDIATION_PLAN.md)** - План устранения уязвимостей безопасности ⭐ ОБНОВЛЕНО (Security Headers отмечены как завершенные)
- **[docs/TOKEN_STORAGE_IMPLEMENTATION.md](archive/docs-deprecated-2026-09-04/TOKEN_STORAGE_IMPLEMENTATION.md)** - Детальная документация реализации хранения токенов в httpOnly cookies ⭐ НОВОЕ
  - Архитектура решения
  - Серверная и клиентская реализация
  - Безопасность и защита от атак
  - Миграция с localStorage
  - Тестирование и troubleshooting

### Отчеты

- **[docs/analysis/PROJECT_REVIEW.md](docs/analysis/PROJECT_REVIEW.md)** - Обзор проекта

### Документация модулей

- **[core/network/README.md](core/network/README.md)** - Документация модуля network
- **[core/common/README.md](core/common/README.md)** - Документация модуля common
- ⏸️ **core/license/src/commonTest/README.md *(утерян/в архиве)*** - Документация тестов модуля license (отложено)

### Настройка инструментов

- **[docs/VSCODE_EXTENSIONS.md](docs/VSCODE_EXTENSIONS.md)** - Рекомендуемые расширения VS Code
- **[scripts/install-vscode-extensions.sh](scripts/install-vscode-extensions.sh)** - Скрипт установки расширений VS Code
- **[scripts/install-vscode-extensions.ps1](scripts/install-vscode-extensions.ps1)** - Скрипт установки расширений VS Code (PowerShell)

---

## Быстрые ссылки по задачам

### Я хочу...

- **Понять архитектуру проекта** → [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- **Узнать текущий статус** → [docs/status/PROJECT_STATUS.md](docs/status/PROJECT_STATUS.md) ⭐ ОБНОВЛЕНО | [docs/IMPLEMENTATION_STATUS.md](archive/docs-duplicates-2026-08-08/IMPLEMENTATION_STATUS.md)
- **Проверить video e2e решение (strict/profile-aware/runtime)** → [docs/reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](docs/reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md) | `scripts/video-e2e-go-no-go.ps1`
- **Открыть единый To-Do и чеклисты** → [docs/TODO.md](archive/docs-duplicates-2026-08-08/TODO.md)
- **Узнать статус нативных библиотек** → [docs/status/NATIVE_LIBRARIES_STATUS.md](docs/status/NATIVE_LIBRARIES_STATUS.md) ⭐ НОВОЕ
- **Узнать статус Data Layer** → [docs/status/DATA_LAYER_STATUS.md](docs/status/DATA_LAYER_STATUS.md) ⭐ НОВОЕ
- **Изучить анализ CameraRepositoryImpl** → [docs/analysis/АНАЛИЗ_3.2.1_CameraRepositoryImpl.md](docs/analysis/АНАЛИЗ_3.2.1_CameraRepositoryImpl.md) ⭐ НОВОЕ
- **Установить зависимости** → [docs/installation/INSTALL_INSTRUCTIONS.md](docs/installation/INSTALL_INSTRUCTIONS.md) ⭐ ОБНОВЛЕНО
- **Начать разработку** → [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | [archive/docs-deprecated-2026-09-04/DEVELOPMENT_PLAN.md](archive/docs-deprecated-2026-09-04/DEVELOPMENT_PLAN.md)
- **Реализовать Desktop приложение** → [docs/DESKTOP_DETAILED_PLAN.md](archive/docs/guides/DESKTOP_DETAILED_PLAN.md) ⭐ НОВОЕ | [docs/DESKTOP_REFINEMENT_PLAN.md](archive/docs/guides/DESKTOP_REFINEMENT_PLAN.md) ⭐ НОВОЕ | [platforms/client-desktop-x86_64/IMPLEMENTATION_STATUS.md](platforms/client-desktop-x86_64/IMPLEMENTATION_STATUS.md) ⭐ НОВОЕ
- **Работать с ONVIF** → [docs/ONVIF_CLIENT.md](docs/ONVIF_CLIENT.md) ⭐ ОБНОВЛЕНО | [docs/ONVIF_TROUBLESHOOTING.md](docs/ONVIF_TROUBLESHOOTING.md) ⭐ НОВОЕ
- **План реализации ONVIF** → [docs/ONVIF_CLIENT_IMPLEMENTATION_PLAN.md](archive/docs/onvif/ONVIF_CLIENT_IMPLEMENTATION_PLAN.md) ⭐ НОВОЕ
- **Digest Authentication** → [docs/ONVIF_DIGEST_AUTH.md](docs/ONVIF_DIGEST_AUTH.md) ⭐ НОВОЕ
- **UPnP интеграция** → [docs/ONVIF_UPNP.md](docs/ONVIF_UPNP.md) ⭐ НОВОЕ
- **Работать с RTSP** → [docs/RTSP_CLIENT.md](archive/docs-duplicates-2026-08-08/RTSP_CLIENT.md) | [docs/rtsp/ACTIVATION.md](docs/rtsp/ACTIVATION.md) ⭐ ВЕРСИЯ 2.0 | [docs/rtsp/INSTALLATION.md](docs/rtsp/INSTALLATION.md) ⭐ ВЕРСИЯ 2.0
- **Работать с WebSocket** → [docs/WEBSOCKET_CLIENT.md](archive/docs-duplicates-2026-08-08/WEBSOCKET_CLIENT.md) | [docs/WEBSOCKET_CLIENT_IMPLEMENTATION_PLAN.md](archive/docs-duplicates-2026-08-08/WEBSOCKET_CLIENT_IMPLEMENTATION_PLAN.md) ⭐ НОВОЕ
- **Развернуть систему** → [docs/DEPLOYMENT_GUIDE.md](archive/docs/deployment/DEPLOYMENT_GUIDE.md)
- **Развернуть на NAS** → [docs/NAS_PLATFORMS_ANALYSIS.md](archive/docs/analysis/NAS_PLATFORMS_ANALYSIS.md) | [docs/planning/NAS_PLATFORM_IMPLEMENTATION_PLAN.md](docs/planning/NAS_PLATFORM_IMPLEMENTATION_PLAN.md) | [platforms/nas-x86_64/IMPLEMENTATION_STATUS.md](platforms/nas-x86_64/IMPLEMENTATION_STATUS.md) | [docs/DEPLOYMENT_GUIDE.md](archive/docs/deployment/DEPLOYMENT_GUIDE.md)
- **Настроить систему** → [docs/CONFIGURATION.md](docs/CONFIGURATION.md) ⭐ НОВОЕ | [docs/ENVIRONMENT_VARIABLES.md](docs/ENVIRONMENT_VARIABLES.md) ⭐ НОВОЕ
- **Устранить неполадки** → [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) ⭐ НОВОЕ
- **Оформить отчёт/issue (баг, дизайн, идея)** → [docs/ISSUE_DESCRIPTION_GUIDELINES.md](archive/docs/guides/ISSUE_DESCRIPTION_GUIDELINES.md) ⭐ НОВОЕ
- **Оптимизировать производительность** → [docs/PERFORMANCE.md](docs/PERFORMANCE.md) ⭐ НОВОЕ
- **Изучить безопасность** → [docs/SECURITY_BEST_PRACTICES.md](docs/SECURITY_BEST_PRACTICES.md) ⭐ НОВОЕ
- **Проверить KMP stabilization (Phase 1)** → [docs/kmp-phase1-progress.md](docs/kmp-phase1-progress.md) | [docs/kmp-phase1-dod-checklist.md](docs/kmp-phase1-dod-checklist.md) | [docs/kmp-security-contract.md](docs/kmp-security-contract.md)
- **Запустить one-shot KMP verifier** → `python scripts/ci/verify-kmp-phase1.py` | `.\scripts\ci\verify-kmp-phase1.ps1` | `./scripts/ci/verify-kmp-phase1.sh`
- **Запустить strict KMP verifier (CI-equivalent gates)** → `python scripts/ci/verify-kmp-phase1.py --ci-profile` | `.\scripts\ci\verify-kmp-phase1.ps1 -CiProfile` | `./scripts/ci/verify-kmp-phase1.sh --ci-profile`
- **Посмотреть API** → [docs/API.md](docs/API.md) | [docs/API_EXAMPLES.md](archive/docs/api/API_EXAMPLES.md) ⭐ НОВОЕ
- **Изучить тесты** → [docs/TESTING.md](docs/TESTING.md) | [docs/TESTS_SUMMARY.md](archive/docs-duplicates-2026-08-08/TESTS_SUMMARY.md)
- **Интегрировать библиотеки** → [docs/INTEGRATION_GUIDE.md](docs/INTEGRATION_GUIDE.md)
- ⏸️ **Работать с лицензированием** → [docs/LICENSE_SYSTEM.md](archive/docs-deprecated-2026-09-04/LICENSE_SYSTEM.md) (отложено)
- **Изучить AI-аналитику** → [docs/AI_ANALYTICS.md](docs/AI_ANALYTICS.md) ⭐ НОВОЕ
- **Узнать, какой документации не хватает** → [docs/DOCUMENTATION_GAPS.md](archive/docs/meta/DOCUMENTATION_GAPS.md)
- **Узнать состояние документации** → [docs/DOCUMENTATION_ANALYSIS_REPORT.md](archive/docs/meta/DOCUMENTATION_ANALYSIS_REPORT.md) ⭐ НОВОЕ
- **Узнать, какой функционал можно добавить** → [docs/FUNCTIONALITY_ANALYSIS.md](archive/docs/analysis/FUNCTIONALITY_ANALYSIS.md)
- **Изучить управление пользователями и SSO/Kerberos** → [docs/USER_MANAGEMENT_SSO_KERBEROS_ANALYSIS.md](archive/docs-duplicates-2026-08-08/USER_MANAGEMENT_SSO_KERBEROS_ANALYSIS.md)
- **Руководство для инженеров** → [docs/ENGINEER_GUIDE.md](archive/docs/guides/ENGINEER_GUIDE.md) ⭐ НОВОЕ
- **Руководство для администраторов** → [docs/ADMINISTRATOR_GUIDE.md](docs/ADMINISTRATOR_GUIDE.md)
- **Руководство для операторов** → [docs/OPERATOR_GUIDE.md](docs/OPERATOR_GUIDE.md)
- **Руководство для пользователей** → [docs/USER_GUIDE.md](docs/USER_GUIDE.md)

---

## Карта связей документации

```
README.md (корневой)
  ├── PROJECT_STRUCTURE.md (ручная документация)
  ├── PROJECT_STRUCTURE_AUTO.md (автоматически генерируемая)
  ├── PLATFORM_STRUCTURE.md
  ├── DOCUMENTATION_INDEX.md
  └── docs/
      ├── README.md (навигация)
      ├── ARCHITECTURE.md
      ├── status/
      │   ├── PROJECT_STATUS.md
      │   ├── NATIVE_LIBRARIES_STATUS.md
      │   └── DATA_LAYER_STATUS.md
      ├── implementation/
      │   └── (детали реализации)
      ├── build/
      │   └── (инструкции по сборке)
      ├── installation/
      │   └── (инструкции по установке)
      ├── planning/
      │   └── (планы разработки)
      ├── analysis/
      │   └── (аналитические документы)
      ├── summaries/
      │   └── (сводки и отчеты)
      ├── IMPLEMENTATION_STATUS.md
      ├── DEVELOPMENT.md
      ├── API.md
      └── ...
```

---

## Статистика документации

- **Всего документов:** 500+ (включая архивы, отчеты и платформенные README)
- **Основные руководства:** 20+
- **Техническая документация:** 60+
- **Отчеты и анализ:** 80+
- **Документация модулей/платформ:** 30+

---

**Версия документации:** 2.8
**Последнее обновление:** 27 April 2026 (status lock contour quick-start synced across README/status/index/policy docs)
**Обновлено:**
- **28 January 2026:**
  - ✅ Создано [ENGINEER_GUIDE.md](archive/docs/guides/ENGINEER_GUIDE.md) - полное руководство для инженеров по развертыванию и первичной настройке
  - ✅ Обновлен [IMPLEMENTATION_STATUS.md](archive/docs-duplicates-2026-08-08/IMPLEMENTATION_STATUS.md) - добавлена информация о 23 реализованных сервисах и 11 middleware
  - ✅ Обновлен [INSTALL_INSTRUCTIONS.md](docs/installation/INSTALL_INSTRUCTIONS.md) - объединены все документы по установке
  - ✅ Обновлены все ссылки между документами
  - ✅ Обновлен DOCUMENTATION_INDEX.md с новой структурой
- **28 January 2026:** Добавлена документация Desktop приложения:
  - Создан DESKTOP_DETAILED_PLAN.md - детальный план с полной разбивкой на задачи (8 этапов)
  - Создан DESKTOP_PLAN_SUMMARY.md - краткая сводка плана
  - Создан DESKTOP_REFINEMENT_PLAN.md - план доработки незавершенных задач (574 часа)
  - Обновлены ссылки в DOCUMENTATION_INDEX.md, README.md, IMPLEMENTATION_STATUS.md, PROJECT_STATUS.md
  - Обновлены README файлы в platforms/client-desktop-x86_64
  - Добавлены перекрестные ссылки между всеми документами Desktop
- **27 January 2026:** Актуализация документации и связей:
  - Обновлены даты последнего обновления во всех основных документах
  - Исправлены ссылки на PROJECT_STATUS.md (теперь docs/status/PROJECT_STATUS.md)
  - Добавлены ссылки на NAS платформу в DOCUMENTATION_INDEX.md
  - Обновлены связи между документами NAS платформы
  - Обновлен раздел "Развертывание" с ссылками на NAS документацию
- **January 2026:** Объединены идентичные документы:
  - Статус/роадмап: PROJECT_STATUS.md объединяет PROJECT_ROADMAP.md, DEVELOPMENT_ROADMAP.md, DEVELOPMENT_MAP.md, CURRENT_STATUS.md, DETAILED_DEVELOPMENT_PLAN.md
  - Установка: INSTALL_INSTRUCTIONS.md объединяет INSTALLATION_SUMMARY.md, QUICK_INSTALL.md, LOCAL_BUILD_REQUIREMENTS.md
  - Нативные библиотеки: NATIVE_LIBRARIES_STATUS.md объединяет все NATIVE_LIBRARIES_*.md документы
  - Data Layer: DATA_LAYER_STATUS.md объединяет все DATA_LAYER_*.md документы
- Дубликаты перемещены в `docs/archive/duplicates-2026-01-27/`
- Обновлен DOCUMENTATION_INDEX.md с новой структурой
- Реорганизация документации: созданы подкаталоги docs/rtsp/, docs/reports/, docs/status/, docs/analysis/, docs/security/
- Объединены RTSP документы в 3 файла (ACTIVATION.md, INSTALLATION.md, IMPLEMENTATION.md)
- Старая документация сохранена в OLD-DOC-2026-01-27/
- Добавлена документация для пользователей (администраторы, операторы, пользователи) с учетом всех платформ и архитектур
- Добавлен раздел "Серверные сервисы" в DOCUMENTATION_INDEX.md с упоминанием всех реализованных сервисов и middleware
- Добавлены описания middleware в PROJECT_STRUCTURE.md

