# Сквозная интеграция AI с видеопотоками (2.1.7)

**Версия:** 1.0  
**Дата:** 27 April 2026  
**Статус:** Реализовано с production monitoring

---

## 📋 Содержание

1. [Обзор](#обзор)
2. [Архитектура](#архитектура)
3. [Сценарии приёмки](#сценарии-приёмки)
4. [Метрики и мониторинг](#метрики-и-мониторинг)
5. [Field Calibration](#field-calibration)
6. [Troubleshooting](#troubleshooting)

---

## Обзор

**Цель 2.1.7:** Связать устойчивый поток кадров (RTSP/HLS) с аналитикой и доменными событиями.

**Текущий статус:** ✅ Реализовано

**Что реализовано:**
- ✅ `AnalyticsFrameInput` / `AnalyticsFrameSourceKind` (RTSP_DECODED, HLS_DERIVED)
- ✅ `VideoAnalyticsService` с поддержкой multi-source
- ✅ `AnalyticsProductionMonitor` для production monitoring
- ✅ WebSocket события аналитики
- ✅ Метрики `/analytics/metrics` endpoint
- ✅ Field calibration для детекторов

**Остаётся:**
- ⚠️ HLS-only frame source для аналитики (бэклог)
- ⚠️ Field validation на реальных камерах

---

## Архитектура

```
┌─────────────────┐
│   RTSP Stream   │
│   (Camera)      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  RtspClient     │
│  (decode)       │
└────────┬────────┘
         │
         ▼ RtspFrame
┌─────────────────┐
│ AnalyticsFrame  │
│ Input           │
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────────┐
│VideoAnalytics   │─────▶│ Production       │
│Service          │      │ Monitor          │
└────────┬────────┘      └──────────────────┘
         │
         ▼
┌─────────────────┐
│ DetectMotion    │
│ DetectObjects   │
│ DetectFaces     │
│ RecognizeANPR   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ EventGenerator  │
└────────┬────────┘
         │
         ▼ WebSocket
┌─────────────────┐
│  Clients (Web/  │
│  Desktop/Mobile)│
└─────────────────┘
```

### Ключевые компоненты

**1. AnalyticsFrameInput**
```kotlin
sealed class AnalyticsFrameInput {
    data class RtspDecoded(val frames: Flow<RtspFrame>) : AnalyticsFrameInput()
    data class HlsDerived(val frames: Flow<HlsFrame>) : AnalyticsFrameInput()
    
    val kind: AnalyticsFrameSourceKind
        get() = when (this) {
            is RtspDecoded -> AnalyticsFrameSourceKind.RTSP_DECODED
            is HlsDerived -> AnalyticsFrameSourceKind.HLS_DERIVED
        }
}
```

**2. VideoAnalyticsService**
- Запускает аналитику для потока кадров
- Обрабатывает motion/object/face/anpr детекции
- Генерирует события и отправляет через WebSocket
- Интегрируется с Production Monitor

**3. AnalyticsProductionMonitor**
- Метрики по камерам (processed/skipped frames, errors)
- Глобальные метрики (total detections)
- История ошибок
- Рекомендации по оптимизации
- Health check пайплайна

---

## Сценарии приёмки

### S-RTSP-1: RTSP-decoded frames → аналитика → события ✅

**Предусловия:**
- Камера подключена и доступна по RTSP
- RTSP client успешно декодирует кадры
- Детекторы включены в настройках камеры

**Шаги:**
1. Запустить аналитику для камеры:
   ```kotlin
   videoAnalyticsService.startAnalytics(
       cameraId = "cam-1",
       camera = camera,
       frameInput = AnalyticsFrameInput.rtspDecoded(rtspFrames)
   )
   ```

2. Подождать обработки кадров (1-5 секунд)

3. Проверить WebSocket события:
   ```json
   {
     "type": "motion_detected",
     "cameraId": "cam-1",
     "timestamp": 1234567890,
     "data": {
       "motionDetected": true,
       "confidence": 0.85,
       "zonesCount": 1
     }
   }
   ```

4. Проверить метрики:
   ```bash
   curl http://localhost:8080/api/v1/analytics/metrics?cameraId=cam-1
   ```

**Критерии прохождения:**
- ✅ События приходят через WebSocket
- ✅ Метрики обновляются (processed frames > 0)
- ✅ Нет критичных ошибок (total errors = 0)
- ✅ FPS > 1 кадр/сек

**Тест:**
```kotlin
@Test
fun `RTSP frames should trigger analytics events`() = runBlocking {
    // Arrange
    val camera = createTestCamera()
    val rtspFrames = mockk<Flow<RtspFrame>>()
    
    // Act
    analyticsService.startAnalytics("cam-1", camera, rtspFrames)
    
    // Assert
    verify(timeout = 5000) {
        websocketManager.broadcastEvent(
            eq(WebSocketChannel.ANALYTICS),
            any(),
            any()
        )
    }
}
```

### S-RTSP-2: Ошибка потока → корректная обработка ✅

**Предусловия:**
- Аналитика запущена
- RTSP поток прерывается

**Шаги:**
1. Имитировать ошибку потока
2. Проверить что аналитика корректно обрабатывает ошибку
3. Проверить что события об ошибках отправляются
4. Проверить что при восстановлении потока аналитика продолжает работу

**Критерии прохождения:**
- ✅ Ошибка фиксируется в Production Monitor
- ✅ Событие ошибки отправляется через WebSocket
- ✅ Аналитика не крашится
- ✅ При восстановлении потока обработка продолжается

### S-HLS-1: HLS-only → аналитика ⚠️ (бэклог)

**Описание:** Аналогично S-RTSP-1, но кадры извлекаются из HLS потока.

**Статус:** В планах (приоритет низкий для MVP)

---

## Метрики и мониторинг

### Метрики по камере

**Endpoint:** `GET /api/v1/analytics/metrics?cameraId={id}`

**Пример ответа:**
```json
{
  "cameraId": "cam-1",
  "cameraName": "Test Camera",
  "frameSourceKind": "RTSP_DECODED",
  "isRunning": true,
  "totalFramesProcessed": 3600,
  "totalFramesSkipped": 12,
  "totalErrors": 0,
  "averageProcessingTimeMs": 45.2,
  "currentFps": 1.0,
  "lastFrameProcessedAt": 1234567890,
  "lastError": null,
  "errorHistory": [],
  "detectorStats": {
    "motionDetections": 15,
    "objectDetections": 8,
    "faceDetections": 3,
    "anprDetections": 0,
    "lastMotionDetectionAt": 1234567800,
    "lastObjectDetectionAt": 1234567700,
    "lastFaceDetectionAt": 1234567600,
    "lastAnprDetectionAt": 0
  }
}
```

### Глобальные метрики

**Endpoint:** `GET /api/v1/analytics/metrics`

**Пример ответа:**
```json
{
  "totalCameras": 10,
  "activeCameras": 8,
  "totalFramesProcessed": 36000,
  "totalFramesSkipped": 120,
  "totalErrors": 5,
  "totalMotionDetections": 150,
  "totalObjectDetections": 80,
  "totalFaceDetections": 30,
  "totalAnprDetections": 5,
  "lastFrameProcessedAt": 1234567890,
  "lastErrorAt": 1234567000
}
```

### Статус пайплайна (health check)

**Endpoint:** `GET /api/v1/analytics/health?cameraId={id}`

**Пример ответа:**
```json
{
  "cameraId": "cam-1",
  "healthy": true,
  "recommendations": []
}
```

**Возможные рекомендации:**
```json
{
  "cameraId": "cam-1",
  "healthy": false,
  "recommendations": [
    "Высокая задержка обработки: 1500мс (целевая: 1000мс). Рекомендуется: уменьшить разрешение",
    "Высокий процент пропущенных кадров: 15.2%. Рекомендуется: увеличить frameProcessingInterval",
    "Частые ошибки: 8 за последние 10 минут. Рекомендуется: проверить логи и конфигурацию"
  ]
}
```

---

## Field Calibration

### Детекция движения

**Параметры калибровки:**
- `motionThreshold` (0.0-1.0): порог детекции (default: 0.5)
- `motionMinArea` (int): минимальная область движения в пикселях (default: 100)
- `motionEventCooldownMs` (long): минимальный интервал между событиями (default: 5000)

**Процедура калибровки:**
1. Настроить начальные значения в настройках камеры
2. Запустить аналитику
3. Проверить метрики:
   ```bash
   curl http://localhost:8080/api/v1/analytics/metrics?cameraId=cam-1
   ```
4. Отрегулировать `motionThreshold`:
   - Слишком много ложных срабатываний → увеличить threshold
   - Мало детекций → уменьшить threshold
5. Отрегулировать `motionMinArea`:
   - Слишком много мелких объектов → увеличить minArea
   - Пропускаются мелкие объекты → уменьшить minArea
6. Повторить до достижения приемлемого баланса

**Пример настройки для улицы:**
```json
{
  "motionThreshold": 0.7,
  "motionMinArea": 500,
  "motionEventCooldownMs": 10000
}
```

**Пример настройки для помещения:**
```json
{
  "motionThreshold": 0.3,
  "motionMinArea": 50,
  "motionEventCooldownMs": 3000
}
```

### Детекция объектов

**Параметры калибровки:**
- `objectDetectionConfidenceThreshold` (0.0-1.0): порог уверенности (default: 0.5)
- `objectTypes`: список типов объектов для детекции

**Процедура калибровки:**
1. Загрузить модель объектов (YOLO/SSD)
2. Настроить `confidenceThreshold` на 0.5
3. Протестировать с известными объектами
4. Отрегулировать threshold:
   - Много ложных срабатываний → увеличить до 0.7
   - Мало детекций → уменьшить до 0.3
5. Настроить `objectTypes` в зависимости от сценария

### Детекция лиц

**Параметры калибровки:**
- `faceRecognitionConfidenceThreshold` (0.0-1.0): порог уверенности (default: 0.7)

**Процедура калибровки:**
1. Загрузить каскад Haar или модель DNN
2. Настроить threshold на 0.7
3. Протестировать с известными лицами
4. Отрегулировать threshold по результатам

### ANPR

**Параметры калибровки:**
- `anprConfidenceThreshold` (0.0-1.0): порог уверенности (default: 0.7)
- `anprLanguage`: язык распознавания (default: "eng")

**Процедура калибровки:**
1. Загрузить ANPR модель
2. Настроить threshold на 0.7
3. Протестировать с известными номерами
4. Отрегулировать threshold и language по результатам

---

## Troubleshooting

### Проблема: Нет событий через WebSocket

**Причины:**
1. Аналитика не запущена
2. Детекторы отключены в настройках
3. Нет движения/объектов в кадре

**Решение:**
```bash
# Проверить статус аналитики
curl http://localhost:8080/api/v1/analytics/status?cameraId=cam-1

# Проверить настройки камеры
curl http://localhost:8080/api/v1/cameras/cam-1

# Проверить метрики
curl http://localhost:8080/api/v1/analytics/metrics?cameraId=cam-1
```

### Проблема: Высокая задержка обработки

**Признаки:**
- `averageProcessingTimeMs` > `frameProcessingInterval`
- Много пропущенных кадров

**Решение:**
1. Уменьшить разрешение потока
2. Отключить ненужные детекторы
3. Увеличить `frameProcessingInterval`
4. Проверить доступность GPU (если используется)

### Проблема: Частые ошибки

**Признаки:**
- `totalErrors` быстро растёт
- Много записей в `errorHistory`

**Решение:**
```bash
# Посмотреть последние ошибки
curl http://localhost:8080/api/v1/analytics/metrics?cameraId=cam-1 | jq .errorHistory

# Проверить логи
docker compose logs surveillance | grep "Analytics error"

# Проверить доступность модели
ls -la /app/models/
```

### Проблема: Утечка памяти

**Признаки:**
- Memory usage растёт со временем
- OOM errors

**Решение:**
1. Уменьшить `maxStoredResults`
2. Уменьшить `maxTrajectoryPointsPerTrack`
3. Проверить что `stopAnalytics()` вызывается при остановке камеры
4. Проверить что детекторы очищаются

---

## Checklists

### Для S-RTSP-1 приёмки

**Подготовка:**
- [ ] Камера подключена и доступна
- [ ] RTSP URL корректен
- [ ] Детекторы включены в настройках
- [ ] WebSocket клиент подключён

**Тестирование:**
- [ ] Запущена аналитика
- [ ] Поступают кадры (processed frames > 0)
- [ ] Приходят события через WebSocket
- [ ] Метрики обновляются
- [ ] Нет критичных ошибок

**После теста:**
- [ ] Аналитика корректно остановлена
- [ ] Ресурсы освобождены
- [ ] Логи проверены

### Field Calibration

**Детекция движения:**
- [ ] motionThreshold настроен
- [ ] motionMinArea настроен
- [ ] motionEventCooldownMs настроен
- [ ] Протестировано на реальной камере
- [ ] Ложные срабатывания приемлемы
- [ ] Пропуски приемлемы

**Детекция объектов:**
- [ ] Модель загружена
- [ ] confidenceThreshold настроен
- [ ] objectTypes настроены
- [ ] Протестировано с известными объектами

**Детекция лиц:**
- [ ] Каскад/модель загружена
- [ ] confidenceThreshold настроен
- [ ] Протестировано с известными лицами

**ANPR:**
- [ ] Модель загружена
- [ ] confidenceThreshold настроен
- [ ] language настроен
- [ ] Протестировано с известными номерами

---

## Связанные документы

- [VideoAnalyticsService](../../server/api/src/main/kotlin/com/company/ipcamera/server/service/VideoAnalyticsService.kt)
- [AnalyticsProductionMonitor](../../server/api/src/main/kotlin/com/company/ipcamera/server/service/analytics/AnalyticsProductionMonitor.kt)
- [NativeAnalytics](../../core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/analytics/NativeAnalytics.kt)
- [PHASE2_1_7_ACCEPTANCE_MATRIX.md](../planning/PHASE2_1_7_ACCEPTANCE_MATRIX.md)

---

## История изменений

| Версия | Дата | Изменение | Автор |
|--------|------|-----------|-------|
| 1.0 | 2026-04-27 | Initial version с production monitoring | NLP-Core-Team |

---

**Последнее обновление:** 27 April 2026  
**Следующая проверка:** Field validation на реальных камерах
