# Интеграция аналитики с Kotlin через FFI

## Обзор

Реализована интеграция блока аналитики с Kotlin через FFI (Foreign Function Interface) используя Kotlin/Native cinterop. Это позволяет использовать нативные C++ библиотеки аналитики из Kotlin кода на всех поддерживаемых платформах.

## Архитектура

```
┌─────────────────────────────────────────┐
│      AnalyticsService (Kotlin)          │
│   (High-level API для клиентов)         │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│   NativeAnalytics (expect/actual)      │
│   (Платформо-независимый API)          │
└──────────────┬──────────────────────────┘
               │
       ┌───────┴───────┐
       │               │
       ▼               ▼
┌─────────────┐  ┌─────────────┐
│  Native     │  │  Android    │
│  (cinterop) │  │  (JNI)      │
└──────┬──────┘  └──────┬───────┘
       │               │
       └───────┬───────┘
               ▼
┌─────────────────────────────────────────┐
│   analytics (C++ библиотека)           │
│   (Нативная реализация)                │
└─────────────────────────────────────────┘
```

## Структура файлов

### Expect класс

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/analytics/NativeAnalytics.kt`

Определяет платформо-независимый API для работы с аналитикой:
- `createMotionDetector()` - создание детектора движения
- `createObjectDetector()` - создание детектора объектов
- `createFaceDetector()` - создание детектора лиц
- `createANPREngine()` - создание движка ANPR
- `createObjectTracker()` - создание трекера объектов

### Native реализация

**Файл:** `core/network/src/nativeMain/kotlin/com/company/ipcamera/core/network/analytics/NativeAnalytics.native.kt`

Реализует интеграцию с нативной библиотекой через cinterop для нативных платформ (Linux, macOS, Windows):
- Использует `kotlinx.cinterop` для вызова C функций
- Управляет памятью через `memScoped` и `StableRef`
- Конвертирует между нативными и Kotlin типами
- Освобождает ресурсы после использования

### Android реализация

**Файл:** `core/network/src/androidMain/kotlin/com/company/ipcamera/core/network/analytics/NativeAnalytics.android.kt`

Заглушка для Android платформы. Для полной интеграции требуется:
1. Скомпилировать нативную библиотеку для Android (armeabi-v7a, arm64-v8a, x86, x86_64)
2. Добавить библиотеку в `android/app/src/main/jniLibs/`
3. Загрузить библиотеку через `System.loadLibrary("analytics")`
4. Реализовать JNI методы или использовать существующий `analytics_jni.cpp`

### JVM реализация

**Файл:** `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/analytics/NativeAnalytics.jvm.kt`

Заглушка для JVM платформ (Desktop). Для полной интеграции требуется:
1. Скомпилировать нативную библиотеку для целевой платформы
2. Загрузить библиотеку через `System.loadLibrary("analytics")`
3. Реализовать JNI методы

### C Interop Definition

**Файл:** `core/network/src/nativeInterop/cinterop/analytics.def`

Определяет конфигурацию для генерации Kotlin биндингов из C заголовочных файлов:
- Указывает заголовочные файлы (`object_detector.h`, `motion_detector.h`, и т.д.)
- Определяет пакет для биндингов
- Указывает пути к библиотекам и заголовочным файлам
- Явно определяет функции для правильной генерации биндингов

## Использование

### Создание детектора движения

```kotlin
val analytics = NativeAnalytics()
val motionDetector = analytics.createMotionDetector(
    width = 1920,
    height = 1080,
    threshold = 0.5f,
    minArea = 100
)

if (motionDetector != null) {
    val result = analytics.detectMotion(
        handle = motionDetector,
        frameData = frameBytes,
        width = 1920,
        height = 1080
    )

    if (result?.motionDetected == true) {
        println("Motion detected with confidence: ${result.confidence}")
    }

    analytics.destroyMotionDetector(motionDetector)
}
```

### Создание детектора объектов

```kotlin
val analytics = NativeAnalytics()
val objectDetector = analytics.createObjectDetector(
    confidenceThreshold = 0.5f,
    maxObjects = 10,
    useGPU = false
)

if (objectDetector != null) {
    // Загружаем модель
    val modelLoaded = analytics.loadObjectDetectorModel(
        handle = objectDetector,
        modelPath = "/path/to/model.tflite"
    )

    if (modelLoaded) {
        val result = analytics.detectObjects(
            handle = objectDetector,
            frameData = frameBytes,
            width = 1920,
            height = 1080
        )

        result?.objects?.forEach { obj ->
            println("Detected ${obj.type} with confidence ${obj.confidence}")
        }
    }

    analytics.destroyObjectDetector(objectDetector)
}
```

### Распознавание номерных знаков

```kotlin
val analytics = NativeAnalytics()
val anprEngine = analytics.createANPREngine(
    confidenceThreshold = 0.7f,
    language = "eng"
)

if (anprEngine != null) {
    val result = analytics.recognizeLicensePlates(
        handle = anprEngine,
        frameData = frameBytes,
        width = 1920,
        height = 1080
    )

    result?.plates?.forEach { plate ->
        println("Recognized plate: ${plate.text} (confidence: ${plate.confidence})")
    }

    analytics.destroyANPREngine(anprEngine)
}
```

## Настройка сборки

### Компиляция нативной библиотеки

1. Установите зависимости:
   - OpenCV (для обработки изображений)
   - TensorFlow Lite (для детекции объектов)
   - Tesseract OCR (для ANPR)

2. Соберите нативную библиотеку:
```bash
cd native/analytics
mkdir build && cd build
cmake ..
make
```

3. Библиотека будет создана в `native/analytics/lib/`

### Настройка cinterop

Cinterop уже настроен в `core/network/build.gradle.kts` для следующих платформ:
- **Linux x64** (`linuxX64`)
- **macOS x64** (`macosX64`)
- **macOS ARM64** (`macosArm64`)
- **Windows x64** (`mingwX64`)
- **Android Native** (`androidNativeArm32`, `androidNativeArm64`, `androidNativeX86`, `androidNativeX64`)

Пример конфигурации:
```kotlin
linuxX64("native") {
    compilations.getByName("main") {
        cinterops {
            val analytics by creating {
                defFile(project.file("src/nativeInterop/cinterop/analytics.def"))
                compilerOpts("-I${project.rootDir}/../native/analytics/include")
                includeDirs("${project.rootDir}/../native/analytics/include")
                linkerOpts("-L${project.rootDir}/../native/analytics/lib/linux/x64 -lanalytics")
            }
        }
    }
}
```

## Поддерживаемые детекторы

### Motion Detector (Детектор движения)
- Алгоритм: MOG2, Frame Difference
- Параметры: threshold, minArea, useGaussianBlur
- Результат: motionDetected, confidence, bounding box

### Object Detector (Детектор объектов)
- Алгоритм: TensorFlow Lite, YOLO
- Параметры: confidenceThreshold, maxObjects, useGPU
- Результат: список объектов с типами (person, vehicle, bicycle, motorcycle)

### Face Detector (Детектор лиц)
- Алгоритм: Haar Cascades, DNN
- Параметры: scaleFactor, minNeighbors, minSize, maxSize
- Результат: список лиц с landmarks

### ANPR Engine (Распознавание номеров)
- Алгоритм: Tesseract OCR
- Параметры: confidenceThreshold, language
- Результат: список распознанных номеров с текстом и bounding box

### Object Tracker (Трекер объектов)
- Алгоритм: IoU Matching, Kalman Filter, DeepSORT
- Параметры: iouThreshold, maxAge, minConfidence
- Результат: список отслеживаемых объектов с ID и временными метками

## Управление памятью

Все нативные ресурсы должны быть освобождены после использования:

```kotlin
val analytics = NativeAnalytics()
val detector = analytics.createMotionDetector(...)

try {
    // Использование детектора
    val result = analytics.detectMotion(detector, ...)
} finally {
    // Обязательно освобождаем ресурсы
    analytics.destroyMotionDetector(detector)
}
```

## Производительность

- Все операции детекции выполняются в `Dispatchers.Default` для неблокирующей работы
- Память управляется через `memScoped` для автоматического освобождения
- Callbacks используют `StableRef` для безопасной передачи между нативным и Kotlin кодом

## Следующие шаги

1. Реализовать полную интеграцию для Android через JNI
2. Реализовать полную интеграцию для JVM через JNI
3. Добавить поддержку iOS через cinterop
4. Интегрировать с `AnalyticsService` в модуле `shared`
5. Добавить unit-тесты для FFI интеграции

## Связанные документы

- [AI_ANALYTICS.md](AI_ANALYTICS.md) - Общая документация по AI-аналитике
- [RTSP_CLIENT.md](RTSP_CLIENT.md) - Пример интеграции RTSP через FFI
- [NATIVE_LIBRARIES_INTEGRATION.md](NATIVE_LIBRARIES_INTEGRATION.md) - Общая документация по интеграции нативных библиотек
