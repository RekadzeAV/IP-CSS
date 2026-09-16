# ARCHIVE_MANIFEST.md

**Дата архивации:** 21 June 2026  
**Исполнитель:** AI Assistant (Cline)  
**Причина:** Полная реорганизация и очистка корня проекта

---

## 📋 Обзор

Данный файл содержит список всех файлов, перемещённых в ходе реорганизации проекта IP-CSS.

**Цель реорганизации:**
- Очистить корень проекта от дублирующихся и устаревших файлов
- Организовать документы по тематическим папкам
- Облегчить навигацию и поиск информации
- Подготовить проект к релизному циклу

---

## 📁 Перемещённые файлы (21 June 2026)

### В `_to_be_archived/ROOT_FILES_2026-06-21/` (68 файлов)

Все файлы, которые дублировали документацию в `docs/` или были устаревшими отчётами:

| Категория | Файлы | Описание |
|-----------|-------|----------|
| **Отчёты о фиксах** | APICLIENT_FIX_REPORT.md, CERTIFICATE_PINNER_NATIVE_ANALYTICS_FIX.md, COMPILATION_FIXES_*.md (3), FIXES_APPLIED_REPORT.md, KOTLIN_CONFIG_FIX_REPORT.md, REMAINING_ISSUES_FIXED_REPORT.md | Отчёты об исправлениях |
| **Отчёты о сборке** | COMPLETION_SUMMARY.md, DEVELOPMENT_ENVIRONMENT_SUMMARY.md, FINAL_IMPLEMENTATION_STATUS.md, FINAL_REPORT.md, FINAL_SESSION_REPORT.md, FINAL_VERIFICATION_REPORT.md | Финальные отчёты |
| **JavaCPP/FFMPEG** | JAVACPP_FFMPEG_*.md (5), JAVACPP_TYPES_FIXED.md, JAVACPP_TYPES_FIXED_FINAL.md | Интеграция JavaCPP |
| **Gradle** | GRADLE_INSTALLATION_COMPLETE.md, GRADLE_INSTALLATION_GUIDE.md, GRADLE_INSTALLATION_SUCCESS.md | Установка Gradle |
| **Phase 1-2** | PHASE1_*.md (3), PHASE2_*.md (4), SESSION_SUMMARY_*.md (4) | Отчёты по фазам |
| **Synology** | PLAN_SYNOLOGY_RELEASE_FIXES.md, PLAN_SYNOLOGY_RELEASE_FIXES_SUMMARY.md, SYNOLOGY_RELEASE_FIXES_COMPLETED.md | Релиз для Synology |
| **RTSP** | RTSP_CLIENT_INTEGRATION_REPORT.md, RTSP_CLIENT_QUICK_START.md, RTSP_INTEGRATION_FINAL_SUMMARY.md | RTSP интеграция |
| **Планы/Статусы** | IMPLEMENTATION_CONTINUATION.md, IMPLEMENTATION_STATUS_2025-01-15.md, IMPLEMENTATION_STATUS_REPORT.md, NEXT_TASKS_SUMMARY.md, PROJECT_REVIEW.md, PROJECT_STATUS.md, TASK_LIST.md | Планы и статусы |
| **Документация** | DOC_REFACTOR_LOG.md, DOC_REFACTOR_FINAL_REPORT.md, DOCS_REFACTOR_SUMMARY.md, DOCS_REFACTOR_TASKS_COMPLETE.md, DOCUMENTATION_REORGANIZATION_PLAN.md, REORGANIZATION_SUMMARY.md | Реорганизация документации |
| **Этап 3** (текущий) | 57 файлов в `archive/docs-duplicates-2026-08-08/` | 08 August 2026 |
| **Всего архивировано** | **159 файлов** | |
| **README/Release** | README_EN.md, README_PRODUCTION_SETUP.md, RELEASE_NOTES.md, RELEASE_SUMMARY.md, VERSION_MANAGEMENT.md | Дополнительные README |
| **Прочее** | CHANGELOG_PHASE1.4_2026-05-27.md, CHANGELOG_PHASE2.md, EXPECT_ACTUAL_*.md (2), FILES_CREATED.md, INFRASTRUCTURE_CREDENTIALS_REPORT.md, INTEGRATION_TESTING_README.md, SCRIPTS_README.md, STRUCTURE.md | Разное |

---

## 📊 Статистика

| Этап | Количество файлов | Дата |
|------|------------------|------|
| **Этап 1** (предыдущий) | 34 файла в `docs/` | 17 May 2026 |
| **Этап 2** (текущий) | 68 файлов в `_to_be_archived/ROOT_FILES_2026-06-21/` | 21 June 2026 |
| **Этап 3** (docs duplicates) | 57 файлов в `archive/docs-duplicates-2026-08-08/` | 08 August 2026 |
| **Этап 4** (kmp duplicates) | 2 файла в `docs/archive/core-network-discovery-duplicates-2026-08-09/` | 09 August 2026 |
| **Этап 5** (deprecated docs) | 54 файла в `archive/docs-deprecated-2026-09-04/` | 04 September 2026 |
| **Этап 6** (Android duplicate) | исходники `platforms/client-android/app` в `archive/client-android-dup-2026-09-04/` (git mv, история сохранена) | 04 September 2026 |
| **Всего архивировано** | **234 файла** | |

## 📦 Этап 5: устаревшая документация (`docs/` → `archive/docs-deprecated-2026-09-04/`)

**Дата:** 04 September 2026
**Причина:** аудит актуальности документации; статусы/планы перенесены в `PLAN_EXECUTION_MASTER.md` и актуализированные `PLAN_*.md`

| Категория | Файлы (примеры) |
|-----------|-----------------|
| Сессионные отчёты | SESSION_COMPLETION_REPORT_2026-01-27*.md (4) |
| Завершённые фазы/планы | SERVER_IMPLEMENTATION_*.md (2), MVP_*.md (3), ПЛАН_РЕАЛИЗАЦИИ.md, ДЕТАЛИЗАЦИЯ_ЭТАПА_6.3_КОМПОНЕНТЫ.md |
| Устаревшие планы исправлений | MIGRATION_IMPLEMENTATION_PLAN, TECHNICAL_ISSUES_REMEDIATION_PLAN* (2), SECURITY_REMEDIATION_PLAN* (2), TASKS_FOR_REFINEMENT_AND_DEBUGGING |
| Реализованные интеграции | ACTIVATE_NATIVE_DECODER, AI_ANALYTICS_IMPLEMENTATION* (2), ANALYTICS_FFI_* (2), ANALYTICS_OPTIMIZATION, ADAPTIVE_BITRATE_AND_WEBRTC_IMPLEMENTATION |
| Видео/плеер | VIDEOPLAYER_DEVELOPMENT_PLAN, VIDEO_RECORDING_ENHANCEMENTS, VIDEO_RECORDING_IMPLEMENTATION |
| Дубли | STRUCTURE (→ PROJECT_STRUCTURE.md), VERSION_MANAGEMENT (→ VERSIONING_GUIDE.md), TECHNICAL_DEBT (→ PLAN_TECH_DEBT_TODO.md), AI_MODELS_RECOMMENDATIONS (→ MODELS.md), WORKING_DOCUMENTS_INDEX, RULES_QUICK_REFERENCE (→ RULES_AND_AUTOMATION.md) |
| Удалённые подсистемы | LICENSE_SYSTEM (core:license вынесен за рамки) |
| Тестовые шаблоны/разовое | TESTING_RESULTS_* (2), TESTING_MANUAL_EXECUTION, TESTING_EXECUTION_RESULT_PHASE0, TOKEN_STORAGE_IMPLEMENTATION, TYPESCRIPT_NAVIGATION_EXTENSIONS, VARIABLE_AND_SECURITY_AUDIT_REPORT, devops-build-agent-prompt |
| **Корневые** | WORK_PLAN.md (→ PLAN_EXECUTION_MASTER.md), REFACTORING_PLAN.md, REFACTORING_PLAN_2026-06-29.md, REFACTORING_COMPLETED.md, CLEANUP_SUMMARY.md |

## 📱 Дубликат Android-приложения (`platforms/client-android/app` → `archive/client-android-dup-2026-09-04/app/`)

**Дата:** 04 September 2026
**Причина:** устаревший дубль главного модуля `android/app` (монолитная ранняя архитектура: ApiClient, Room AppDatabase, CacheManager, FCM PushNotificationService, Login/Home/экраны). Главный модуль — `:android:app` (settings.gradle.kts). Уникальная функциональность дубля (Room/FCM/логин) переносится в главный модуль в рамках Этапа 5 мастер-плана.
**Метаданные платформы** (`platforms/client-android/README.md`) сохранены на месте — на них ссылаются активные документы.

**Итого:** 19 файлов (git mv, история сохранена).
**Итого:** 49 файлов из `docs/` + 5 из корня = **54 файла** (перемещены `git mv`, история сохранена).

## 📚 Дубли документации (`docs/` → `archive/docs-duplicates-2026-08-08/`)

Перенесены устаревшие/дублирующие документы:
- статусные отчёты: `IMPLEMENTATION_STATUS.md`, `MVP_PRIORITY_TASKS_STATUS.md`, `PROJECT_STATUS_BASELINE.md`, `STATUS_BY_BLOCK.md`
- планы/задачи: `TASK_LIST.md`, `TODO.md`, `TODO_CONSOLIDATED.md`, `PHASE_*`, `REORGANIZATION_PLAN_2026-05-28.md`
- тестовые отчёты/планы: `TESTS_SUMMARY.md`, `TESTING_PLAN_ANALYSIS.md`, `TESTING_PLAN_VIDEO_DECODER.md`, `TESTING_IMPLEMENTATION_*.md`
- RTSP/VideoDecoder/WebSocket интеграции: `RTSP_*.md`, `WEBSOCKET_*.md`, `VIDEO_DECODER_*.md`, `VIDEO_PLAYER_RTSP_HLS_INTEGRATION.md`
- отчёты по security/release/analysis: `SECURITY_*.md`, `RELEASE_*.md`, `PROJECT_ANALYSIS_*.md`, `PROMPT_ANALYSIS.md`, `USER_MANAGEMENT_SSO_KERBEROS_ANALYSIS.md`

---

---

## ✅ Оставшиеся файлы в корне (7 файлов)

| Файл | Размер | Причина |
|------|--------|---------|
| README.md | 29 KB | Главный файл проекта (обновлён) |
| CHANGELOG.md | 28 KB | История изменений |
| CONTRIBUTING.md | 14 KB | Руководство для контрибьюторов |
| LICENSE | 0 B | Лицензия GPLv3 |
| DOCUMENTATION_INDEX.md | 56 KB | Индекс документации |
| PROJECT_PROMPT.md | 95 KB | Промпт для AI-ассистентов |
| PROJECT_STRUCTURE.md | 31 KB | Структура проекта |
| ARCHIVE_MANIFEST.md | новый | Данный файл |

---

## 📦 Инфраструктура сборок

### Локальные сборки для тестирования

| Платформа | Путь вывода | Статус |
|-----------|-------------|--------|
| Android APK (debug) | `androidApp/build/outputs/apk/debug/` | ✅ Готово |
| Android APK (release) | `androidApp/build/outputs/apk/release/` | 🔧 Требуется настройка |
| Android AAB (release) | `androidApp/build/outputs/bundle/release/` | 🔧 Требуется настройка |
| Desktop Windows | `platforms/client-desktop-x86_64/build/compose/binaries/` | 🔧 Требуется сборка |
| Desktop Linux | `platforms/client-desktop-x86_64/build/compose/binaries/` | 🔧 Требуется сборка |
| Desktop macOS | `platforms/client-desktop-x86_64/build/compose/binaries/` | 🔧 Требуется сборка |
| Server JAR | `server/build/libs/` | ✅ Готово |
| NAS Synology SPK | `platforms/nas-x86_64/build/distributions/` | 🔧 Требуется сборка |
| NAS QNAP QPKG | `platforms/nas-x86_64/build/distributions/` | 🔧 Требуется сборка |
| Docker Image | Локальный registry | ✅ Готово |

### Release-сборки

| Тип | Расположение | Описание |
|-----|-------------|----------|
| Beta (Docker) | `release-builds/beta/` | Docker Compose + Dockerfile для Linux |
| Release | `release-build/` | Конфигурации релизных сборок |
| GitHub Releases | Репозиторий GitHub | Тегнутые версии |

---

## ⚠️ Примечания

1. **Все исходные файлы сохранены** — файлы перемещены, а не удалены
2. **Ссылки в документации** могут быть битыми — требуется обновление
3. **Релизные бинарные артефакты** пока не созданы — только конфигурации
4. **Для первого тестового релиза** необходимо выполнить сборку для каждой платформы

---

## 🔄 Следующие шаги

1. ✅ Очистка корня проекта (68 файлов → архив)
2. ✅ Обновление README.md
3. ⬜ Обновление DOCUMENTATION_INDEX.md (ссылки на перемещённые файлы)
4. ⬜ Создание тестовых сборок для всех платформ
5. ⬜ Настройка CI/CD для автоматических сборок
6. ⬜ Подписание артефактов для релиза

---

**Дата создания:** 21 June 2026  
**Версия:** 2.0

---

## 📦 Архив 15.09.2026 — NAS Git-выкладка (отключена)

**Запись:** `archive/nas-git-deployment-2026-09-15/`

| Объект | Было | Стало | Причина |
|---|---|---|---|
| Git-remote `origin` | `Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git` | **удалён** (`git remote remove origin`) | Канал недоступен (интерактивный пароль), дублирует GitHub, риск утечки секрета |
| `NAS_GIT_OPERATIONS_REPORT.md` | `docs/` | `archive/nas-git-deployment-2026-09-15/` | Git-операции на NAS более не выполняются |
| `NAS_SSH_KEYS_REPORT.md` | `docs/` | `archive/nas-git-deployment-2026-09-15/` | SSH-доступ к NAS для git-выкладки не используется |

**Дата архивации:** 15.09.2026 · **Точка истины:** `github/main`

**НЕ затронуто:** платформенная поддержка NAS как продукта (`scripts/build-nas-packages.sh`, `platforms/nas-*`, `build-nas-packages.yml`, Docker TrueNAS), справочные NAS-документы по сборке/установке.

**Полное описание и процедура восстановления:** `archive/nas-git-deployment-2026-09-15/README.md`

---

## 📦 Архив 15.09.2026 — Git-ветки GitHub (удалены, с бэкапом)

**Запись:** `archive/git-branches-backup-2026-09-15/`

| Объект | Действие |
|---|---|
| 32 ветки GitHub (`develop/*`, `test/*`, `dev/*`, `feature/*`, `refactor/*`, `chore/*`, `main-old`, `old_prodject_alfa_0.0.1_defeat`, `commit`) | удалены с remote `github`; tip-refs сохранены локально в `refs/archive/branches-2026-09-15/*` и в `old-branches.bundle` (полная история) |
| Dependabot-ветки (23, активные PR) | **не затронуты** |
| 4 локальные ветки (ранее слиты в `main`) | удалены локально, история в `main` |

**Причина:** устаревшие ветки (коммиты 2025-12…2026-03), шум в ветках/Actions; точка истины — `main`.
**Проверка/восстановление:** `archive/git-branches-backup-2026-09-15/README.md`
**Дата:** 15.09.2026


---

## 📦 Архив 15.09.2026 — GitHub Actions workflows (актуализация каталога)

**Запись:** `archive/workflows-2026-09-15/`

| Объект | Действие |
|---|---|
| 10 workflows (`build-native.yml`, `build-native-libraries.yml`, `native-windows-build.yml`, `rtsp-client-ci.yml`, `cd.yml`, `python-ci-gates.yml`, `phase1-mvp-verify.yml`, `postgres-finalization-one-command-gate.yml`, `postgres-finalization-report-validation.yml`, `nightly-live-integration.yml`) | удалены из `.github/workflows/`, оригиналы в архиве |
| 13 workflows + README | сохранены и актуализированы (триггеры → `main`, заменены устаревшие actions) |

**Причина:** 8 из 10 — дубли функциональности `ci.yml`/`nightly.yml`/нативных сборок; 2 — одноразовые staging-инструменты завершённого этапа PostgreSQL-финализации; 1 — требовал живых камер (порт A1). Все находились в `disabled_manually` после массовых `startup_failure` (корень: `allowed_actions: local_only` — исправлено на `all` 15.09 через REST API).

**Полное описание и восстановление:** `archive/workflows-2026-09-15/README.md` · актуальный состав: `.github/workflows/README.md`
**Дата:** 15.09.2026

