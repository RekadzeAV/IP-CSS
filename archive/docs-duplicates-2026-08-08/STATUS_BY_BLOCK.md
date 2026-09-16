# Статус по блокам

**Дата:** 2026-03-01  
Сводка по блокам MVP и AI-аналитики.

---

## 1. MVP — поэтапный план (`MVP_PHASED_IMPLEMENTATION_PLAN.md`)

| Блок | Статус | Комментарий |
|------|--------|-------------|
| **Текущее состояние (аудит)** | ⚠️ Частично | ONVIF/безопасность/RTSP/CameraRepository/миграции — есть база, детали по плану не все закрыты |
| **Фаза 0: Подготовка** | ❌ Не начато | MVP_DEFINITION.md, проверка сборки/тестов, ветки |
| **Фаза 1: ONVIF Event** | ⚠️ Частично | Сервисы и интеграция есть; подписка при старте, тесты на реальных камерах, WebSocket→UI — по плану не завершены |
| **Фаза 2: Безопасность** | ⚠️ Частично | Pinning на платформах есть; конфиг pins, принудительный HTTPS, тесты — по плану не все |
| **Фаза 3: RTSP/FFmpeg** | ⚠️ Частично | JVM декодер есть; стабильность, тесты на реальных потоках, реконнект — в работе |
| **Фаза 4: CameraRepository кэш** | ❌ Не начато | DiscoveryCache и StatusCache в V2 по плану не внедрены |
| **Фаза 5: Миграции БД** | ✅ В работе | MigrationManager, 1.sqm, 2.sqm (license_plate), версия 3; процесс описан в DATABASE_MIGRATIONS.md |

---

## 2. AI-аналитика — план по фазам (`AI_ANALYTICS_IMPLEMENTATION_PLAN.md`)

| Блок | Статус | Комментарий |
|------|--------|-------------|
| **Фаза 1: Инфраструктура** | ✅ Готово | Ошибки, image_utils, model_manager, logger, JNI, getVersion, DEPENDENCIES/MODELS |
| **Фаза 2: Детекция движения** | ✅ Готово | Нативный motion_detector, Use Case, AnalyticsFrameProcessor, троттлинг |
| **Фаза 3: Детекция объектов** | ✅ Готово | object_detector (input size), Use Case, кэш модели по камере, скрипты загрузки моделей |
| **Фаза 4: Трекинг объектов** | ✅ Готово | lastSeen в object_tracker, trackObjects в AnalyticsService, TrackInfo, REST /tracks, getCurrentTracks, VideoAnalyticsService |
| **Фаза 5: ANPR** | ✅ Готово | Таблица license_plate, LicensePlateRepository, сохранение в Use Case, постобработка текста (uppercase, 0/O) в anpr_engine |
| **Фаза 6: Детекция лиц** | ⚠️ База есть | face_detector (cascade), detectFaces(), Use Case; БД лиц, REST по лицам, embeddings — не делались |
| **Фаза 7: Интеграция** | ⚠️ Частично | GET /analytics/status, пайплайн в VideoAnalyticsService, REST results/stats/tracks, WebSocket; таблица analytics_result, RuleEngine — не делались |
| **Фаза 8: Оптимизация** | ⚠️ Частично | GET /analytics/metrics, skippedFramesCount, ANALYTICS_OPTIMIZATION.md; профилирование, очередь кадров — частично |

---

## 3. База данных и миграции

| Блок | Статус | Комментарий |
|------|--------|-------------|
| **Версия схемы** | 3 | camera, recording, event, user, setting, notification, schema_version, **license_plate** |
| **Миграция 1.sqm** | ✅ | 1 → 2: recording, event, user, setting, notification, schema_version |
| **Миграция 2.sqm** | ✅ | 2 → 3: license_plate |
| **MigrationManager** | ✅ | CURRENT_SCHEMA_VERSION = 3 |
| **Документация** | ✅ | DATABASE_MIGRATIONS.md обновлён |

---

## 4. API аналитики (REST)

| Endpoint | Статус |
|----------|--------|
| GET /cameras/{id}/analytics | ✅ Конфиг |
| POST /cameras/{id}/analytics | ✅ Настройки |
| GET /cameras/{id}/analytics/status | ✅ isRunning, activeDetectors |
| GET /cameras/{id}/analytics/tracks | ✅ Текущие треки |
| GET /cameras/{id}/analytics/metrics | ✅ processedFrames, skippedFrames |
| GET /cameras/{id}/analytics/stats | ✅ Статистика |
| GET /cameras/{id}/analytics/results | ✅ Результаты (пагинация) |
| POST /cameras/{id}/analytics/start | ✅ |
| POST /cameras/{id}/analytics/stop | ✅ |
| DELETE /cameras/{id}/analytics/results | ✅ |

---

## 5. Легенда

- **✅ Готово** — блок реализован по плану.
- **⚠️ Частично** — есть реализация, но не все пункты плана закрыты.
- **❌ Не начато** — по плану не делалось.
