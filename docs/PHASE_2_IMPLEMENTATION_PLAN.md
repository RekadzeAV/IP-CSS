# Phase 2 Implementation Plan

**Дата:** 2026-01-28  
**Статус:** 🔥 Начинаем  
**Версия:** 1.0

---

## 📋 Обзор

Фаза 2 фокусируется на реализации основного функционала: AI-аналитика, уведомления, экспорт записей.

### Цели Фазы 2:

1. ✅ **Motion Detection** — детекция движения с настраиваемыми зонами
2. ✅ **Object Detection** — YOLOv8 интеграция (люди, авто, животные)
3. ✅ **Timeline View** — визуализация событий на временной шкале
4. ✅ **Export Recordings** — экспорт в MP4/MKV с метаданными
5. ✅ **Email Notifications** — SMTP уведомления
6. ✅ **Telegram Bot** — Telegram push уведомления
7. ✅ **Desktop UI** — завершение Desktop клиента
8. ✅ **Web Polish** — финальная полировка веб-интерфейса

---

## 🎯 Задача 1: Motion Detection

**Приоритет:** Критический  
**Оценка:** 2-3 недели  
**Статус:** 🔥 В работе

### Требования:

- [ ] Детекция движения по зонам
- [ ] Настройка чувствительности
- [ ] События при детекции
- [ ] Интеграция с записью
- [ ] Настройка через UI

### Архитектура:

```
┌─────────────┐      ┌──────────────────┐      ┌─────────────────┐
│  RTSP Stream│─────▶│ MotionDetector   │─────▶│ Event Service   │
│  (H.264)    │      │ (OpenCV)         │      │ (Save Event)    │
└─────────────┘      └──────────────────┘      └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │  Zone Config     │
                       │  (JSON)          │
                       └──────────────────┘
```

### Компоненты:

1. **MotionDetectorService** (Kotlin)
   - Анализ кадров
   - Сравнение с фоном
   - Применение зон

2. **ZoneConfig** (Data Class)
   - Маска зон
   - Чувствительность
   - Минимальная площадь

3. **MotionEvent** (Domain Model)
   - Timestamp
   - Camera ID
   - Zone ID
   - Confidence

4. **API Endpoints**
   - `POST /api/v1/motion/config` — настройка
   - `GET /api/v1/motion/events` — получение событий

### Файлы:

- `server/api/src/main/kotlin/com/company/ipcamera/server/service/MotionDetectorService.kt` (новый)
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/model/MotionConfig.kt` (новый)
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/model/MotionEvent.kt` (новый)
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/MotionRoutes.kt` (новый)

---

## 🎯 Задача 2: Object Detection (YOLO)

**Приоритет:** Критический  
**Оценка:** 3-4 недели  
**Статус:** 📋 Запланировано

### Требования:

- [ ] YOLOv8 интеграция
- [ ] Детекция: люди, авто, животные
- [ ] Трекинг объектов
- [ ] События при детекции
- [ ] Статистика по объектам

### Архитектура:

```
┌─────────────┐      ┌──────────────────┐      ┌─────────────────┐
│  RTSP Stream│─────▶│ ObjectDetector   │─────▶│ Event Service   │
│  (H.264)    │      │ (YOLOv8)         │      │ (Save Event)    │
└─────────────┘      └──────────────────┘      └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │  Detected Objects│
                       │  - Person        │
                       │  - Car           │
                       │  - Animal        │
                       └──────────────────┘
```

### Компоненты:

1. **ObjectDetectorService** (Kotlin + Python)
   - YOLOv8 модель
   - Inference на GPU/CPU
   - Постобработка

2. **DetectedObject** (Data Class)
   - Class (person, car, animal)
   - Confidence
   - Bounding Box
   - Track ID

3. **Integration**
   - JNI/FFI для Python
   - Или ONNX Runtime (Kotlin)

---

## 🎯 Задача 3: Timeline View

**Приоритет:** Высокий  
**Оценка:** 1-2 недели  
**Статус:** 📋 Запланировано

### Требования:

- [ ] Визуализация событий
- [ ] Фильтрация по типу
- [ ] Быстрая навигация
- [ ] Интеграция с плеером
- [ ] Масштабирование

### Компоненты:

1. **TimelineComponent** (React/Compose)
2. **EventAggregator** (Backend)
3. **API Endpoints** для агрегации

---

## 🎯 Задача 4: Export Recordings

**Приоритет:** Высокий  
**Оценка:** 1 неделя  
**Статус:** 📋 Запланировано

### Требования:

- [ ] Экспорт в MP4/MKV
- [ ] Добавление метаданных
- [ ] Пакетный экспорт
- [ ] Прогресс экспорта
- [ ] Скачать через HTTP

### API:

```http
POST /api/v1/recordings/export
{
  "recordingIds": ["rec-1", "rec-2"],
  "format": "mp4",
  "includeMetadata": true
}

Response:
{
  "exportId": "exp-123",
  "status": "processing",
  "downloadUrl": null
}

GET /api/v1/recordings/export/exp-123/status

Response:
{
  "exportId": "exp-123",
  "status": "completed",
  "progress": 100,
  "downloadUrl": "/api/v1/recordings/export/exp-123/download"
}
```

---

## 🎯 Задача 5: Email Notifications

**Приоритет:** Средний  
**Оценка:** 3-5 дней  
**Статус:** 📋 Запланировано

### Требования:

- [ ] SMTP интеграция
- [ ] HTML шаблоны
- [ ] Триггеры событий
- [ ] Настройка получателей
- [ ] Rate limiting

### Конфигурация:

```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=notifications@ipcss.com
SMTP_PASSWORD=app_password
SMTP_FROM=IP-CSS Alerts <alerts@ipcss.com>
```

---

## 🎯 Задача 6: Telegram Bot

**Приоритет:** Средний  
**Оценка:** 3-5 дней  
**Статус:** 📋 Запланировано

### Требования:

- [ ] Bot API интеграция
- [ ] Push уведомления
- [ ] Команды управления
- [ ] Подписка на камеры
- [ ] Скриншоты по запросу

### Команды:

```
/start — Начало работы
/status — Статус системы
/cameras — Список камер
/subscribe — Подписка на события
/unsubscribe — Отписка
/snapshot — Скриншот камеры
```

---

## 📊 Прогресс

| Задача | Статус | Прогресс |
|--------|--------|----------|
| 1. Motion Detection | 🔥 | 0% |
| 2. Object Detection | 📋 | 0% |
| 3. Timeline View | 📋 | 0% |
| 4. Export Recordings | 📋 | 0% |
| 5. Email Notifications | 📋 | 0% |
| 6. Telegram Bot | 📋 | 0% |
| 7. Desktop UI | 📋 | 0% |
| 8. Web Polish | 📋 | 0% |

**Общий прогресс Фазы 2:** 0%

---

## 🚀 Следующие шаги

1. ✅ Завершить Фазу 1 (100% DONE)
2. 🔥 Начать Motion Detection (Task 1/8)
3. 📋 Подготовить инфраструктуру для YOLO
4. 📋 Создать Timeline компонент

---

**Подготовлено:** NLP-Core-Team  
**Дата:** 2026-01-28  
**Статус:** 🔥 Начинаем
