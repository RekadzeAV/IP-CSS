# Отчет о реализации Use Cases аналитики (2.3.7)

**Дата:** 2026-01-27
**Статус:** ✅ **ЗАВЕРШЕНО** (100%)

## Обзор

Реализованы все 6 Use Cases для аналитики видео, которые обеспечивают детекцию движения, объектов, лиц, распознавание номерных знаков, трекинг объектов и комплексный анализ видео.

## Реализованные Use Cases

### 1. ✅ DetectMotionUseCase
**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/usecase/DetectMotionUseCase.kt`

**Функциональность:**
- Детекция движения на видеокадре
- Поддержка зон детекции (DetectionZone)
- Настраиваемый порог чувствительности (threshold)
- Автоматическое создание событий при обнаружении движения
- Интеграция с EventRepository

**Параметры:**
- `camera: Camera` - камера
- `frameData: ByteArray` - данные кадра
- `previousFrameData: ByteArray?` - предыдущий кадр (опционально)
- `zones: List<DetectionZone>` - зоны детекции
- `threshold: Float` - порог чувствительности (0.0-1.0)
- `createEvent: Boolean` - создавать ли событие

**Возвращает:** `Result<MotionDetectionResult>`

---

### 2. ✅ DetectObjectsUseCase
**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/usecase/DetectObjectsUseCase.kt`

**Функциональность:**
- Детекция объектов различных типов (person, vehicle, bicycle, motorcycle)
- Фильтрация по типам объектов
- Настраиваемый минимальный уровень уверенности (confidence)
- Автоматическое создание событий при обнаружении объектов
- Интеграция с EventRepository

**Параметры:**
- `camera: Camera` - камера
- `frameData: ByteArray` - данные кадра
- `objectTypes: List<String>` - типы объектов для детекции
- `minConfidence: Float` - минимальная уверенность (0.0-1.0)
- `createEvent: Boolean` - создавать ли событие

**Возвращает:** `Result<ObjectDetectionResult>`

---

### 3. ✅ TrackObjectsUseCase
**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/usecase/TrackObjectsUseCase.kt`

**Функциональность:**
- Трекинг объектов между кадрами
- Алгоритм IoU (Intersection over Union) для матчинга объектов
- Управление жизненным циклом треков (создание, обновление, удаление)
- Расчет траекторий объектов
- События входа/выхода объектов
- Интеграция с EventRepository

**Параметры:**
- `camera: Camera` - камера
- `detectedObjects: List<DetectedObject>` - обнаруженные объекты
- `frameTimestamp: Long` - временная метка кадра
- `maxLostFrames: Int` - максимальное количество кадров без обнаружения
- `createEvents: Boolean` - создавать ли события

**Возвращает:** `Result<ObjectTrackingResult>`

**Структуры данных:**
- `TrackedObject` - отслеживаемый объект с траекторией
- `ObjectTrackingResult` - результат трекинга

---

### 4. ✅ DetectFacesUseCase
**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/usecase/DetectFacesUseCase.kt`

**Функциональность:**
- Детекция лиц на видеокадре
- Опциональное извлечение landmarks (68 точек)
- Опциональное извлечение embeddings для распознавания
- Настраиваемый минимальный уровень уверенности
- Автоматическое создание событий при обнаружении лиц
- Интеграция с EventRepository

**Параметры:**
- `camera: Camera` - камера
- `frameData: ByteArray` - данные кадра
- `minConfidence: Float` - минимальная уверенность (0.0-1.0)
- `includeLandmarks: Boolean` - извлекать ли landmarks
- `includeEmbeddings: Boolean` - извлекать ли embeddings
- `createEvent: Boolean` - создавать ли событие

**Возвращает:** `Result<FaceDetectionResult>`

---

### 5. ✅ RecognizeLicensePlateUseCase
**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/usecase/RecognizeLicensePlateUseCase.kt`

**Функциональность:**
- Распознавание номерных знаков (ANPR - Automatic Number Plate Recognition)
- Поддержка различных форматов номеров по странам
- Настраиваемый минимальный уровень уверенности
- Автоматическое создание событий при обнаружении номеров
- Интеграция с EventRepository

**Параметры:**
- `camera: Camera` - камера
- `frameData: ByteArray` - данные кадра
- `minConfidence: Float` - минимальная уверенность (0.0-1.0)
- `country: String?` - код страны для валидации формата
- `createEvent: Boolean` - создавать ли событие

**Возвращает:** `Result<LicensePlateRecognitionResult>`

---

### 6. ✅ AnalyzeVideoUseCase
**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/usecase/AnalyzeVideoUseCase.kt`

**Функциональность:**
- Комплексный анализ видео, объединяющий все аналитические Use Cases
- Параллельная обработка для оптимизации производительности
- Интеграция с DetectMotionUseCase, DetectObjectsUseCase, DetectFacesUseCase, RecognizeLicensePlateUseCase
- Обработка настроек аналитики из Camera.settings.analytics
- Автоматическое создание событий на основе результатов
- Интеграция с NotificationService для уведомлений

**Параметры:**
- `camera: Camera` - камера
- `frameData: ByteArray` - данные кадра
- `previousFrameData: ByteArray?` - предыдущий кадр (опционально)
- `settings: VideoAnalysisSettings?` - настройки анализа (опционально)

**Возвращает:** `Result<VideoAnalysisResult>`

**Структуры данных:**
- `VideoAnalysisResult` - результат комплексного анализа
- `AnalysisEvent` - событие анализа
- `VideoAnalysisSettings` - настройки анализа

---

## Dependency Injection

### Модуль: AnalyticsUseCasesModule
**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/di/AnalyticsUseCasesModule.kt`

Все Use Cases зарегистрированы в Koin модуле `analyticsUseCasesModule` и доступны через dependency injection.

**Зависимости:**
- `AnalyticsService` - сервис аналитики (обязательно)
- `EventRepository` - репозиторий событий (опционально)
- `NotificationService` - сервис уведомлений (опционально, только для AnalyzeVideoUseCase)

**Интеграция:**
Модуль подключен во всех платформах:
- ✅ Android (`android/app/src/main/java/com/company/ipcamera/android/di/AppModule.kt`)
- ✅ Desktop x86_64 (`platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/di/AppModule.kt`)
- ✅ Desktop ARM (`platforms/client-desktop-arm/app/src/main/kotlin/com/company/ipcamera/desktop/di/AppModule.kt`)
- ✅ Server (`server/api/src/main/kotlin/com/company/ipcamera/server/di/AppModule.kt`)

---

## Интеграция с другими компонентами

### EventRepository
Все Use Cases интегрированы с `EventRepository` для автоматического создания событий:
- `EventType.MOTION_DETECTION` - при обнаружении движения
- `EventType.OBJECT_DETECTION` - при обнаружении объектов
- `EventType.FACE_DETECTION` - при обнаружении лиц
- `EventType.LICENSE_PLATE_RECOGNITION` - при распознавании номеров

### NotificationService
`AnalyzeVideoUseCase` интегрирован с `NotificationService` для отправки уведомлений о важных событиях.

### AnalyticsService
Все Use Cases используют `AnalyticsService` для выполнения аналитических операций:
- `detectMotion()` - детекция движения
- `detectObjects()` - детекция объектов
- `detectFaces()` - детекция лиц
- `recognizeLicensePlates()` - распознавание номеров

---

## Валидация и обработка ошибок

Все Use Cases включают:
- ✅ Валидацию входных параметров
- ✅ Проверку размера данных кадра
- ✅ Проверку диапазонов значений (confidence, threshold)
- ✅ Обработку ошибок с информативными сообщениями
- ✅ Graceful degradation (продолжение работы при ошибках создания событий)

---

## Производительность

- ✅ Параллельная обработка в `AnalyzeVideoUseCase` через Kotlin Coroutines
- ✅ Оптимизация алгоритмов трекинга (IoU matching)
- ✅ Эффективное управление памятью для треков объектов

---

## Статус реализации

| Use Case | Статус | Файл |
|----------|--------|------|
| DetectMotionUseCase | ✅ Реализовано | `DetectMotionUseCase.kt` |
| DetectObjectsUseCase | ✅ Реализовано | `DetectObjectsUseCase.kt` |
| TrackObjectsUseCase | ✅ Реализовано | `TrackObjectsUseCase.kt` |
| DetectFacesUseCase | ✅ Реализовано | `DetectFacesUseCase.kt` |
| RecognizeLicensePlateUseCase | ✅ Реализовано | `RecognizeLicensePlateUseCase.kt` |
| AnalyzeVideoUseCase | ✅ Реализовано | `AnalyzeVideoUseCase.kt` |

**Итого:** 6/6 Use Cases реализовано (100%)

---

## Следующие шаги

### Рекомендуемые улучшения:
1. ⚠️ Интеграция с нативными библиотеками для улучшения производительности
2. ⚠️ Реализация более сложных алгоритмов трекинга (Kalman filter, DeepSORT)
3. ⚠️ Интеграция с видеопотоками для автоматического анализа кадров
4. ⚠️ Добавление поддержки дополнительных типов объектов
5. ⚠️ Оптимизация для GPU ускорения

### Интеграция:
- ⚠️ Интеграция с RTSP клиентом для автоматического анализа потоков
- ⚠️ Интеграция с VideoStreamService для обработки кадров в реальном времени

---

## Заключение

Все 6 Use Cases аналитики успешно реализованы и интегрированы в систему. Модуль готов к использованию на всех платформах (Android, Desktop, Server). Use Cases обеспечивают полный функционал для детекции движения, объектов, лиц, распознавания номерных знаков, трекинга объектов и комплексного анализа видео.

**Статус:** ✅ **ГОТОВО К ИСПОЛЬЗОВАНИЮ**
