# План реорганизации документации в корне проекта

**Дата создания:** 27 января 2026
**Статус:** В процессе выполнения

---

## 📊 Анализ текущего состояния

### Файлы в корне проекта (19 MD файлов)

#### ✅ Оставить в корне (стандартные файлы проекта)
1. **README.md** - главный файл проекта ✅
2. **CHANGELOG.md** - история изменений ✅
3. **CONTRIBUTING.md** - руководство для контрибьюторов ✅
4. **LICENSE** - лицензия ✅
5. **DOCUMENTATION_INDEX.md** - индекс документации ✅
6. **PROJECT_PROMPT.md** - промпт для AI (используется активно) ✅
7. **PROJECT_STRUCTURE.md** - структура проекта (важная справочная документация) ✅

#### 📁 Переместить в docs/summaries/ (сводки и завершения)
1. **AI_ANALYTICS_IMPLEMENTATION_SUMMARY.md** → `docs/summaries/AI_ANALYTICS_IMPLEMENTATION_SUMMARY.md`
   - Резюме реализации AI-аналитики
   - Статус: Завершено (январь 2026)

2. **BUILD_SUCCESS_SUMMARY.md** → `docs/summaries/BUILD_SUCCESS_SUMMARY.md`
   - Сводка успешной сборки нативной библиотеки
   - Статус: Завершено (январь 2026)

3. **CMAKE_SETUP_COMPLETE.md** → `docs/summaries/CMAKE_SETUP_COMPLETE.md`
   - Завершение настройки CMake
   - Статус: Завершено (январь 2026)

4. **DOCUMENTATION_REVISION_SUMMARY.md** → `docs/summaries/DOCUMENTATION_REVISION_SUMMARY.md`
   - Отчет о ревизии документации
   - Статус: Завершено (26 января 2026)

5. **MODELS_INTEGRATION_COMPLETE.md** → `docs/summaries/MODELS_INTEGRATION_COMPLETE.md`
   - Интеграция дополнительных AI моделей
   - Статус: Завершено (26 января 2026)

6. **OBJECT_DETECTOR_COMPLETE.md** → `docs/summaries/OBJECT_DETECTOR_COMPLETE.md`
   - Завершение реализации Object Detector
   - Статус: Завершено (26 января 2026)

#### 📁 Переместить в docs/implementation/ (прогресс реализации)
1. **RTSP_CLIENT_INTEGRATION_PROGRESS.md** → `docs/implementation/RTSP_CLIENT_INTEGRATION_PROGRESS.md`
   - Прогресс интеграции RTSP клиента
   - **Примечание:** Файл уже существует в `docs/implementation/`, но версия в корне может отличаться
   - **Действие:** Сравнить содержимое, оставить более актуальную версию

#### 📁 Переместить в docs/reports/ (отчеты)
1. **DUPLICATES_AND_GARBAGE_REPORT.md** → `docs/reports/DUPLICATES_AND_GARBAGE_REPORT.md`
   - Отчет о дубликатах и мусорных файлах
   - Статус: Очистка выполнена (январь 2026)
   - **Примечание:** Отчет устаревший, но полезен для истории

#### 📁 Переместить в docs/analysis/ (аналитические документы)
1. **STRUCTURE_ANALYSIS_AND_RECOMMENDATIONS.md** → `docs/analysis/STRUCTURE_ANALYSIS_AND_RECOMMENDATIONS.md`
   - Анализ структуры проекта и рекомендации
   - **Примечание:** Файл уже существует в `docs/analysis/`, но версия в корне может отличаться
   - **Действие:** Сравнить содержимое, оставить более актуальную версию

2. **АНАЛИЗ_3.2.1_CameraRepositoryImpl.md** → `docs/analysis/АНАЛИЗ_3.2.1_CameraRepositoryImpl.md`
   - Анализ состояния разработки CameraRepositoryImpl
   - Статус: ~90% готовности

3. **АНАЛИЗ_VIDEOPLAYER_6.3.3.md** → `docs/analysis/АНАЛИЗ_VIDEOPLAYER_6.3.3.md`
   - Анализ состояния разработки VideoPlayer
   - Статус: Критический блокер MVP

#### 📁 Переместить в docs/status/ или docs/reports/ (статусы и отчеты)
1. **PROJECT_STATUS.md** → `docs/status/PROJECT_STATUS.md`
   - Статус и план разработки проекта
   - **Примечание:** Файл уже существует в `docs/status/`, но версия в корне может отличаться
   - **Действие:** Сравнить содержимое, оставить более актуальную версию

2. **ТАБЛИЦА_ГОТОВНОСТИ_ПРОЕКТА.md** → `docs/status/ТАБЛИЦА_ГОТОВНОСТИ_ПРОЕКТА.md`
   - Таблица готовности проекта IP-CSS
   - Статус: Общий прогресс ~73%

3. **ФАЗА_1_MVP_ДЕТАЛЬНЫЙ_ОТЧЕТ.md** → `docs/reports/ФАЗА_1_MVP_ДЕТАЛЬНЫЙ_ОТЧЕТ.md`
   - Фаза 1: MVP - Детальный отчет по этапам
   - Статус: Общий прогресс Фазы 1 ~77%

#### 📁 Переместить в docs/ (общая документация)
1. **PLATFORM_STRUCTURE.md** → `docs/PLATFORM_STRUCTURE.md`
   - Структура платформ проекта
   - **Примечание:** Важная документация, но не должна быть в корне

---

## 📋 План выполнения

### Этап 1: Проверка дубликатов
- [ ] Сравнить `RTSP_CLIENT_INTEGRATION_PROGRESS.md` (корень) с `docs/implementation/RTSP_CLIENT_INTEGRATION_PROGRESS.md`
- [ ] Сравнить `STRUCTURE_ANALYSIS_AND_RECOMMENDATIONS.md` (корень) с `docs/analysis/STRUCTURE_ANALYSIS_AND_RECOMMENDATIONS.md`
- [ ] Сравнить `PROJECT_STATUS.md` (корень) с `docs/status/PROJECT_STATUS.md`
- [ ] Определить, какая версия более актуальна для каждого файла

### Этап 2: Перемещение файлов-сводок
- [ ] Переместить `AI_ANALYTICS_IMPLEMENTATION_SUMMARY.md` → `docs/summaries/`
- [ ] Переместить `BUILD_SUCCESS_SUMMARY.md` → `docs/summaries/`
- [ ] Переместить `CMAKE_SETUP_COMPLETE.md` → `docs/summaries/`
- [ ] Переместить `DOCUMENTATION_REVISION_SUMMARY.md` → `docs/summaries/`
- [ ] Переместить `MODELS_INTEGRATION_COMPLETE.md` → `docs/summaries/`
- [ ] Переместить `OBJECT_DETECTOR_COMPLETE.md` → `docs/summaries/`

### Этап 3: Перемещение файлов реализации
- [ ] Обработать `RTSP_CLIENT_INTEGRATION_PROGRESS.md` (сравнить и объединить или удалить дубликат)

### Этап 4: Перемещение отчетов
- [ ] Переместить `DUPLICATES_AND_GARBAGE_REPORT.md` → `docs/reports/`
- [ ] Переместить `ФАЗА_1_MVP_ДЕТАЛЬНЫЙ_ОТЧЕТ.md` → `docs/reports/`

### Этап 5: Перемещение аналитических документов
- [ ] Обработать `STRUCTURE_ANALYSIS_AND_RECOMMENDATIONS.md` (сравнить и объединить или удалить дубликат)
- [ ] Переместить `АНАЛИЗ_3.2.1_CameraRepositoryImpl.md` → `docs/analysis/`
- [ ] Переместить `АНАЛИЗ_VIDEOPLAYER_6.3.3.md` → `docs/analysis/`

### Этап 6: Перемещение статусов
- [ ] Обработать `PROJECT_STATUS.md` (сравнить и объединить или удалить дубликат)
- [ ] Переместить `ТАБЛИЦА_ГОТОВНОСТИ_ПРОЕКТА.md` → `docs/status/`

### Этап 7: Перемещение общей документации
- [ ] Переместить `PLATFORM_STRUCTURE.md` → `docs/`

### Этап 8: Обновление ссылок
- [ ] Обновить ссылки в `DOCUMENTATION_INDEX.md`
- [ ] Обновить ссылки в `README.md`
- [ ] Обновить ссылки в других документах, которые ссылаются на перемещенные файлы

---

## 📊 Итоговая структура корня проекта

После реорганизации в корне останется:
- `README.md` ✅
- `CHANGELOG.md` ✅
- `CONTRIBUTING.md` ✅
- `LICENSE` ✅
- `DOCUMENTATION_INDEX.md` ✅
- `PROJECT_PROMPT.md` ✅
- `PROJECT_STRUCTURE.md` ✅

**Итого:** 7 файлов (вместо 19)

---

## ⚠️ Важные замечания

1. **Дубликаты:** Перед перемещением необходимо сравнить файлы, которые уже существуют в `docs/`, чтобы не потерять актуальную информацию.

2. **Ссылки:** После перемещения нужно обновить все ссылки на перемещенные файлы в других документах.

3. **Git:** После перемещения нужно проверить, что Git правильно отслеживает перемещение файлов (использовать `git mv` для сохранения истории).

4. **PROJECT_PROMPT.md:** Этот файл оставлен в корне, так как активно используется для AI-ассистентов и должен быть легко доступен.

---

**Дата создания плана:** 27 января 2026
**Статус:** Готов к выполнению
