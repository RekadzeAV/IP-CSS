# Реализация базовой AI-аналитики

**Версия:** 1.0
**Дата:** Январь 2026
**Статус:** ✅ Базовая реализация завершена

## Обзор

Реализована базовая AI-аналитика для системы видеонаблюдения IP-CSS. Система включает детекцию движения, детекцию объектов и трекинг объектов с использованием нативных C++ библиотек и интеграцию через JNI.

## Архитектура

### Компоненты

1. **Нативные C++ библиотеки** (`native/analytics/`)
   - `motion_detector` - детекция движения с использованием MOG2
   - `object_detector` - детекция объектов через OpenCV DNN/YOLO
   - `object_tracker` - трекинг объектов с IoU matching

2. **JNI обертки** (`native/analytics/src/jni/`)
   - `analytics_jni.cpp` - JNI функции для интеграции с Java/Kotlin

3. **Kotlin Multiplatform обертки** (`shared/src/jvmMain/kotlin/com/company/ipcamera/shared/analytics/native/`)
   - `NativeMotionDetector.kt`
   - `NativeObjectDetector.kt`
   - `NativeObjectTracker.kt`

4. **Domain сервисы** (`shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/service/`)
   - `AnalyticsService.kt` - интерфейс сервиса аналитики
   - `AnalyticsServiceImpl.jvm.kt` - JVM реализация с нативными библиотеками
   - Платформо-специфичные реализации для Android, iOS, Desktop

5. **Серверные сервисы** (`server/api/src/main/kotlin/com/company/ipcamera/server/service/`)
   - `VideoAnalyticsService.kt` - обработка видеопотоков с аналитикой

6. **API endpoints** (`server/api/src/main/kotlin/com/company/ipcamera/server/routing/`)
   - `AnalyticsRoutes.kt` - REST API для управления аналитикой

## Реализованные функции

### 1. Детекция движения

- ✅ Использование MOG2 (Mixture of Gaussians) алгоритма
- ✅ Поддержка зон детекции
- ✅ Настраиваемая чувствительность
- ✅ Фильтрация ложных срабатываний
- ✅ Автоматическая генерация событий

**Использование:**
```kotlin
val result = analyticsService.detectMotion(
    camera = camera,
    frameData = frame.data,
    zones = camera.analyticsSettings.zones,
    threshold = 0.5f
)

if (result.detected && result.confidence > 0.5f) {
    // Движение обнаружено
}
```

### 2. Детекция объектов

- ✅ Поддержка YOLO моделей через OpenCV DNN
- ✅ Фильтрация по типам объектов (person, vehicle, bicycle, motorcycle)
- ✅ Настраиваемый порог уверенности
- ✅ Non-Maximum Suppression (NMS) для удаления дубликатов

**Использование:**
```kotlin
val result = analyticsService.detectObjects(
    camera = camera,
    frameData = frame.data,
    objectTypes = listOf("person", "vehicle"),
    minConfidence = 0.5f
)

result.objects.forEach { obj ->
    println("Detected ${obj.type} with confidence ${obj.confidence}")
}
```

### 3. Трекинг объектов

- ✅ IoU (Intersection over Union) matching
- ✅ Уникальные ID для отслеживаемых объектов
- ✅ Автоматическое удаление старых треков
- ✅ Отслеживание траекторий

**Использование:**
```kotlin
val tracker = NativeObjectTracker.create()
val trackingResult = tracker.update(detectionResult)

trackingResult.objects.forEach { tracked ->
    println("Track ID: ${tracked.id}, Type: ${tracked.type}")
}
```

### 4. Интеграция с видеопотоками

- ✅ Автоматический запуск аналитики при старте видеопотока
- ✅ Обработка кадров с настраиваемым интервалом
- ✅ Генерация событий на основе результатов аналитики
- ✅ Интеграция с EventService

**Конфигурация:**
```kotlin
val videoAnalyticsService = VideoAnalyticsService(
    analyticsService = analyticsService,
    eventService = eventService,
    frameProcessingInterval = 1000L, // Обрабатывать каждый кадр раз в секунду
    motionDetectionEnabled = true,
    objectDetectionEnabled = true
)
```

## API Endpoints

### GET /api/v1/cameras/{id}/analytics/stats

Получить статистику аналитики для камеры.

**Ответ:**
```json
{
  "success": true,
  "data": {
    "cameraId": "camera-1",
    "frameCount": 1234,
    "isActive": true
  },
  "message": "Analytics stats retrieved successfully"
}
```

## Настройка камеры

Аналитика настраивается через `AnalyticsSettings` в модели `Camera`:

```kotlin
data class AnalyticsSettings(
    val motionDetection: Boolean = true,
    val zones: List<DetectionZone> = emptyList(),
    val objectDetection: Boolean = false,
    val objectTypes: List<String> = emptyList()
)
```

**Пример:**
```kotlin
val camera = Camera(
    id = "camera-1",
    name = "Main Entrance",
    analyticsSettings = AnalyticsSettings(
        motionDetection = true,
        zones = listOf(
            DetectionZone(
                name = "Entrance Zone",
                polygon = listOf(
                    listOf(100, 100),
                    listOf(500, 100),
                    listOf(500, 400),
                    listOf(100, 400)
                ),
                sensitivity = 80
            )
        ),
        objectDetection = true,
        objectTypes = listOf("person", "vehicle")
    )
)
```

## Сборка нативной библиотеки

### Требования

- CMake 3.15+
- OpenCV 4.8+ (опционально, для детекции объектов)
- TensorFlow Lite (опционально, для альтернативной детекции объектов)
- Tesseract OCR (опционально, для ANPR)

### Сборка с JNI поддержкой

```bash
cd native/analytics
mkdir build && cd build
cmake .. -DBUILD_JNI_LIBRARY=ON -DENABLE_OPENCV=ON
cmake --build . --config Release
```

### Установка библиотеки

После сборки библиотека `libanalytics_jni.so` (Linux) или `analytics_jni.dll` (Windows) должна быть доступна для загрузки через `System.loadLibrary("analytics")`.

## Генерация событий

Система автоматически генерирует события на основе результатов аналитики:

1. **События движения** (`EventType.MOTION_DETECTED`)
   - Генерируются при обнаружении движения с уверенностью > 0.5
   - Включают информацию о зонах детекции

2. **События объектов** (`EventType.OBJECT_DETECTED`)
   - Генерируются при обнаружении объектов
   - Группируются по типам объектов
   - Включают количество и среднюю уверенность

**Пример события:**
```json
{
  "type": "MOTION_DETECTED",
  "description": "Motion detected in zones: Entrance Zone (confidence: 0.85)",
  "metadata": {
    "confidence": "0.85",
    "zones": "Entrance Zone",
    "frameTimestamp": "1234567890"
  }
}
```

## Производительность

### Оптимизации

1. **Пропуск кадров**: Обработка каждого N-го кадра (по умолчанию раз в секунду)
2. **Кэширование детекторов**: Детекторы создаются один раз для каждой камеры
3. **Асинхронная обработка**: Использование корутин для неблокирующей обработки
4. **Буферизация**: Ограниченный буфер видеокадров для управления памятью

### Рекомендации

- Для высокой производительности рекомендуется использовать GPU ускорение
- Настройте `frameProcessingInterval` в зависимости от нагрузки
- Используйте зоны детекции для уменьшения ложных срабатываний
- Настройте пороги уверенности для баланса между точностью и производительностью

## Ограничения и TODO

### Текущие ограничения

- ⚠️ Детекция лиц и ANPR еще не реализованы (заглушки)
- ⚠️ Требуется загрузка моделей детекции объектов (YOLO)
- ⚠️ Android и iOS реализации используют заглушки

### Планируемые улучшения

- [ ] Интеграция с TensorFlow Lite для Android
- [ ] Интеграция с Core ML для iOS
- [ ] Реализация детекции лиц
- [ ] Реализация ANPR (распознавание номерных знаков)
- [ ] GPU ускорение (CUDA/OpenCL)
- [ ] Оптимизация моделей (квантование, pruning)
- [ ] Расширенная аналитика (анализ поведения, трекинг траекторий)

## Тестирование

### Unit тесты

```bash
./gradlew :shared:test --tests "*Analytics*"
```

### Интеграционные тесты

```bash
./gradlew :server:api:test --tests "*Analytics*"
```

## Документация

- [AI_ANALYTICS.md](AI_ANALYTICS.md) - Общая документация по AI-аналитике
- [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) - Руководство по интеграции библиотек

## Поддержка

При возникновении проблем:

1. Проверьте, что нативная библиотека собрана и доступна
2. Убедитесь, что OpenCV установлен (для детекции объектов)
3. Проверьте логи сервера на наличие ошибок
4. Убедитесь, что аналитика включена в настройках камеры

---

**Версия:** 1.0
**Последнее обновление:** 26 January 2026
