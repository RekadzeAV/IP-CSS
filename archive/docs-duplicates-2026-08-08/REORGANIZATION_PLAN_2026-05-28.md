# План реорганизации документации

**Дата:** 28 May 2026  
**Статус:** В ожидании подтверждения

---

## 📊 Текущее состояние

- **Файлов в корне:** 58 MD файлов
- **Файлов для перемещения:** 45
- **Файлов для объединения:** ~20 в 5 итоговых документов
- **Файлов останется в корне:** 13 (основные документы проекта)

---

## 🎯 Цели

1. Очистить корень проекта от отчетов и технической документации
2. Объединить дублирующиеся и однотипные документы
3. Создать единую логичную структуру в docs/
4. Сохранить все исходные файлы в _to_be_archived/
5. Обновить все перекрестные ссылки

---

## 📁 План перемещения файлов из корня

### Папка: docs/reports/ (отчеты о фиксах и реализации)

| Файл | Новый путь | Примечание |
|------|------------|------------|
| APICLIENT_FIX_REPORT.md | docs/reports/APICLIENT_FIX_REPORT.md | |
| CERTIFICATE_PINNER_NATIVE_ANALYTICS_FIX.md | docs/reports/CERTIFICATE_PINNER_NATIVE_ANALYTICS_FIX.md | |
| COMPILATION_FIXES_FINAL.md | _to_be_archived/ | Для объединения |
| COMPILATION_FIXES_REPORT.md | _to_be_archived/ | Для объединения |
| COMPILATION_FIXES_SUMMARY.md | _to_be_archived/ | Для объединения |
| EXPECT_ACTUAL_PROBLEM_FINAL_REPORT.md | docs/reports/EXPECT_ACTUAL_PROBLEM_FINAL_REPORT.md | |
| FINAL_VERIFICATION_REPORT.md | docs/reports/FINAL_VERIFICATION_REPORT.md | |
| FIXES_APPLIED_REPORT.md | docs/reports/FIXES_APPLIED_REPORT.md | |
| GRADLE_INSTALLATION_SUCCESS.md | _to_be_archived/ | Для объединения |
| IMPLEMENTATION_STATUS_REPORT.md | docs/reports/IMPLEMENTATION_STATUS_REPORT.md | |
| JAVACPP_FFMPEG_FINAL_STATUS.md | _to_be_archived/ | Для объединения |
| JAVACPP_FFMPEG_UPDATE_COMPLETE.md | _to_be_archived/ | Для объединения |
| JAVACPP_FFMPEG_VERSION_CHECK.md | _to_be_archived/ | Для объединения |
| JAVACPP_FFMPEG_VERSION_CHECK_RESULT.md | _to_be_archived/ | Для объединения |
| JAVACPP_FFMPEG_VERSION_UPDATE.md | _to_be_archived/ | Для объединения |
| JAVACPP_TYPES_FIXED.md | _to_be_archived/ | Для объединения |
| JAVACPP_TYPES_FIXED_FINAL.md | _to_be_archived/ | Для объединения |
| KOTLIN_CONFIG_FIX_REPORT.md | docs/reports/KOTLIN_CONFIG_FIX_REPORT.md | |
| PLAN_SYNOLOGY_RELEASE_FIXES_SUMMARY.md | _to_be_archived/ | Для объединения |
| RELEASE_SUMMARY.md | docs/reports/RELEASE_SUMMARY.md | |
| REMAINING_ISSUES_FIXED_REPORT.md | docs/reports/REMAINING_ISSUES_FIXED_REPORT.md | |
| SYNOLOGY_RELEASE_FIXES_COMPLETED.md | _to_be_archived/ | Для объединения |
| RTSP_CLIENT_INTEGRATION_REPORT.md | _to_be_archived/ | Для объединения |
| RTSP_CLIENT_QUICK_START.md | _to_be_archived/ | Для объединения |
| RTSP_INTEGRATION_FINAL_SUMMARY.md | _to_be_archived/ | Для объединения |

### Папка: docs/analysis/ (аналитические документы)

| Файл | Новый путь | Примечание |
|------|------------|------------|
| EXPECT_ACTUAL_INVESTIGATION.md | docs/analysis/EXPECT_ACTUAL_INVESTIGATION.md | |
| PROJECT_REVIEW.md | docs/analysis/PROJECT_REVIEW.md | |

### Папка: docs/installation/ (инструкции по установке)

| Файл | Новый путь | Примечание |
|------|------------|------------|
| GRADLE_INSTALLATION_GUIDE.md | docs/installation/GRADLE_INSTALLATION_GUIDE.md | |

### Папка: docs/implementation/ (документы по реализации)

| Файл | Новый путь | Примечание |
|------|------------|------------|
| IMPLEMENTATION_CONTINUATION.md | docs/implementation/IMPLEMENTATION_CONTINUATION.md | |

### Папка: docs/status/ (статус проекта)

| Файл | Новый путь | Примечание |
|------|------------|------------|
| FINAL_IMPLEMENTATION_STATUS.md | docs/status/FINAL_IMPLEMENTATION_STATUS.md | |
| PROJECT_STATUS.md | _to_be_archived/ | Дубликат docs/status/PROJECT_STATUS.md |

### Папка: docs/guides/ (руководства)

| Файл | Новый путь | Примечание |
|------|------------|------------|
| README_PRODUCTION_SETUP.md | docs/guides/README_PRODUCTION_SETUP.md | |

### Папка: docs/ (общая документация)

| Файл | Новый путь | Примечание |
|------|------------|------------|
| STRUCTURE.md | docs/STRUCTURE.md | |
| VERSION_MANAGEMENT.md | docs/VERSION_MANAGEMENT.md | |
| TASK_LIST.md | docs/TASK_LIST.md | |
| PHASE1_BLOCKER_REMEDIATION_SUMMARY_2026-05-25.md | docs/reports/PHASE1_BLOCKER_REMEDIATION_SUMMARY_2026-05-25.md | Отчет |
| PLAN_SYNOLOGY_RELEASE_FIXES.md | _to_be_archived/ | Для объединения |
| DEVELOPMENT_ENVIRONMENT_SUMMARY.md | docs/reports/DEVELOPMENT_ENVIRONMENT_SUMMARY.md | Отчет |
| COMPLETION_SUMMARY.md | docs/reports/COMPLETION_SUMMARY.md | Отчет |
| FILES_CREATED.md | docs/reports/FILES_CREATED.md | Отчет |
| FINAL_REPORT.md | docs/reports/FINAL_REPORT.md | Отчет |
| SCRIPTS_README.md | docs/guides/SCRIPTS_README.md | Руководство |
| INTEGRATION_TESTING_README.md | docs/guides/INTEGRATION_TESTING_README.md | Руководство |

---

## 🔀 Документы для объединения

### Группа 1: Compilation Fixes
**Исходные файлы (переместить в _to_be_archived/):**
- COMPILATION_FIXES_FINAL.md
- COMPILATION_FIXES_REPORT.md
- COMPILATION_FIXES_SUMMARY.md

**Итоговый документ:** `docs/reports/COMPILE_FIXES_CONSOLIDATED.md`

### Группа 2: JavaCPP FFMPEG
**Исходные файлы (переместить в _to_be_archived/):**
- JAVACPP_FFMPEG_FINAL_STATUS.md
- JAVACPP_FFMPEG_UPDATE_COMPLETE.md
- JAVACPP_FFMPEG_VERSION_CHECK.md
- JAVACPP_FFMPEG_VERSION_CHECK_RESULT.md
- JAVACPP_FFMPEG_VERSION_UPDATE.md
- JAVACPP_TYPES_FIXED.md
- JAVACPP_TYPES_FIXED_FINAL.md

**Итоговый документ:** `docs/reports/JAVACPP_FFMPEG_IMPLEMENTATION.md`

### Группа 3: Gradle Installation
**Исходные файлы (переместить в _to_be_archived/):**
- GRADLE_INSTALLATION_COMPLETE.md
- GRADLE_INSTALLATION_GUIDE.md
- GRADLE_INSTALLATION_SUCCESS.md

**Итоговый документ:** `docs/installation/GRADLE_SETUP_GUIDE.md`

### Группа 4: Synology Release
**Исходные файлы (переместить в _to_be_archived/):**
- PLAN_SYNOLOGY_RELEASE_FIXES.md
- PLAN_SYNOLOGY_RELEASE_FIXES_SUMMARY.md
- SYNOLOGY_RELEASE_FIXES_COMPLETED.md

**Итоговый документ:** `docs/reports/SYNOLOGY_RELEASE_IMPLEMENTATION.md`

### Группа 5: RTSP Integration
**Исходные файлы (переместить в _to_be_archived/):**
- RTSP_CLIENT_INTEGRATION_REPORT.md
- RTSP_CLIENT_QUICK_START.md
- RTSP_INTEGRATION_FINAL_SUMMARY.md

**Итоговый документ:** `docs/reports/RTSP_INTEGRATION_CONSOLIDATED.md`

---

## 📋 Файлы, которые останутся в корне

| Файл | Причина |
|------|---------|
| README.md | Основной документ проекта |
| CHANGELOG.md | История изменений |
| CONTRIBUTING.md | Руководство для контрибьюторов |
| DOCUMENTATION_INDEX.md | Индекс документации |
| PROJECT_PROMPT.md | Исходный промпт проекта |
| PROJECT_STRUCTURE.md | Структура проекта |
| ARCHIVE_MANIFEST.md | Манифест архива (результат реорганизации) |
| DOC_REFACTOR_LOG.md | Лог реорганизации |
| DOC_REFACTOR_FINAL_REPORT.md | Финальный отчет реорганизации |
| DOCS_REFACTOR_SUMMARY.md | Сводка реорганизации |
| DOCS_REFACTOR_TASKS_COMPLETE.md | Отчет о выполненных задачах |

---

## 📊 Статистика

| Действие | Количество |
|----------|------------|
| Файлов переместить в docs/ | ~25 |
| Файлов переместить в _to_be_archived/ (для объединения) | ~20 |
| Файлов объединить в | 5 |
| Файлов останется в корне | 13 |
| **Итого уменьшение в корне:** | **45 файлов** (с 58 до 13) |

---

## ✅ Шаги выполнения

1. **Создать папку _to_be_archived/**
2. **Переместить файлы в _to_be_archived/ для объединения**
3. **Создать 5 объединенных документов**
4. **Переместить остальные файлы в docs/**
5. **Обновить ссылки во всех документах**
6. **Создать ARCHIVE_MANIFEST.md**
7. **Обновить DOCUMENTATION_INDEX.md**

---

## ⚠️ Важные примечания

- **Ничего не удаляется безвозвратно** — все файлы перемещаются в _to_be_archived/ или docs/
- **Все ссылки будут проверены и обновлены**
- **Архив сохранит оригинальную структуру для отслеживания истории**

---

**Продолжить выполнение? y/n**
