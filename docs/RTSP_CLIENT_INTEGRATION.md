# Руководство по интеграции RTSP клиента

**Дата создания:** Декабрь 2025  
**Версия:** 1.0

## Обзор

RTSP клиент интегрирован с нативной C++ библиотекой через FFI (Foreign Function Interface). Структура готова к использованию после компиляции нативной библиотеки.

## Архитектура

```
┌─────────────────────────────────┐
│      RtspClient (Kotlin)       │
│   (High-level API для клиентов) │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│   NativeRtspClient (expect)     │
│   (Платформо-независимый API)   │
└──────────────┬──────────────────┘
               │
       ┌───────┴───────┐
       │               │
       ▼               ▼
┌─────────────┐  ┌─────────────┐
│  Android    │  │    iOS      │
│  (JNI)      │  │  (cinterop) │
└─────────────┘  └─────────────┘
       │               │
       └───────┬───────┘
               ▼
┌─────────────────────────────────┐
│   rtsp_client (C++ библиотека)  │
│   (Нативная реализация)         │
└─────────────────────────────────┘
```

## Структура файлов

### Kotlin код

- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/RtspClient.kt`
  - Высокоуровневый API для работы с RTSP потоками
  - Использует `NativeRtspClient` для вызова нативных функций

- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.kt`
  - Expect класс для платформо-независимого API

- `core/network/src/nativeMain/kotlin/.../NativeRtspClient.native.kt`
  - Native реализация через cinterop (Linux, macOS)

- `core/network/src/androidMain/kotlin/.../NativeRtspClient.android.kt`
  - Android реализация через JNI

- `core/network/src/iosMain/kotlin/.../NativeRtspClient.ios.kt`
  - iOS реализация через cinterop

- `core/network/src/jvmMain/kotlin/.../NativeRtspClient.jvm.kt`
  - JVM реализация (Desktop)

### Нативный код

- `native/video-processing/include/rtsp_client.h`
  - C заголовочный файл с определениями функций

- `native/video-processing/src/rtsp_client.cpp`
  - C++ реализация RTSP клиента

### Конфигурация

- `core/network/src/nativeInterop/cinterop/rtsp_client.def`
  - Определение cinterop для генерации Kotlin биндингов

## Использование

### Базовое использование

```kotlin
// Создание клиента
val config = RtspClientConfig(
    url = "rtsp://camera.example.com:554/stream",
    username = "admin",
    password = "password",
    timeoutMillis = 10000
)
val client = RtspClient(config)

// Подключение
client.connect()

// Подписка на видеокадры
client.getVideoFrames().collect { frame ->
    // Обработка кадра
    processFrame(frame.data, frame.width, frame.height)
}

// Воспроизведение
client.play()

// Остановка
client.stop()

// Отключение
client.disconnect()
client.close()
```

### Использование callbacks

```kotlin
client.setVideoFrameCallback { frame ->
    // Обработка видеокадра
    displayFrame(frame)
}

client.setStatusCallback { status, message ->
    when (status) {
        RtspClientStatus.CONNECTED -> println("Connected")
        RtspClientStatus.PLAYING -> println("Playing")
        RtspClientStatus.ERROR -> println("Error: $message")
        else -> {}
    }
}
```

## Настройка сборки

### Компиляция нативной библиотеки

1. Установите зависимости:
   - FFmpeg (libavformat, libavcodec, libavutil, libswscale)
   - OpenCV (опционально, для обработки кадров)

2. Соберите нативную библиотеку:
```bash
cd native
mkdir build && cd build
cmake ..
make
```

3. Библиотека будет создана в `native/video-processing/lib/`

### Настройка cinterop

Cinterop уже настроен в `build.gradle.kts`:

```kotlin
linuxX64("native") {
    compilations.getByName("main") {
        cinterops {
            val rtspClient by creating {
                defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                packageName("com.company.ipcamera.core.network.rtsp")
                compilerOpts("-I${project.rootDir}/../native/video-processing/include")
            }
        }
    }
}
```

### Android (JNI)

Для Android требуется:

1. Скомпилировать нативную библиотеку для Android (armeabi-v7a, arm64-v8a, x86, x86_64)
2. Добавить библиотеку в `android/app/src/main/jniLibs/`
3. Загрузить библиотеку в коде:
```kotlin
System.loadLibrary("video_processing")
```

### iOS (cinterop)

Для iOS требуется:

1. Скомпилировать нативную библиотеку для iOS (arm64, x86_64 для симулятора)
2. Добавить библиотеку в Xcode проект
3. Настроить cinterop в `build.gradle.kts` для iOS targets

## Текущий статус

### ✅ Реализовано

- Структура expect/actual для платформо-независимого API
- Интеграция `RtspClient` с `NativeRtspClient`
- Конфигурация cinterop (.def файл)
- Обработка callbacks для кадров и статусов
- Управление жизненным циклом (connect, play, stop, disconnect)

### ⚠️ Требуется доработка

- Реализация нативных вызовов в actual классах (сейчас заглушки)
- Компиляция нативной библиотеки для всех платформ
- Тестирование интеграции на реальных устройствах
- Обработка ошибок и таймаутов
- Оптимизация производительности

## Следующие шаги

1. **Компиляция нативной библиотеки:**
   - Собрать библиотеку для каждой целевой платформы
   - Протестировать на реальных RTSP камерах

2. **Реализация нативных вызовов:**
   - Раскомментировать и реализовать вызовы в actual классах
   - Добавить обработку ошибок

3. **Тестирование:**
   - Unit тесты для `RtspClient`
   - Integration тесты с реальными камерами
   - Тесты производительности

4. **Оптимизация:**
   - Оптимизация буферизации кадров
   - Аппаратное ускорение декодирования
   - Управление памятью

## Известные проблемы

- Callbacks могут вызываться из нативных потоков - требуется синхронизация
- Утечки памяти при неправильном освобождении ресурсов
- Таймауты могут не работать корректно на некоторых платформах

## Связанные документы

- [RTSP_CLIENT.md](RTSP_CLIENT.md) - Общее описание RTSP клиента
- [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) - Общее руководство по интеграции
- [ARCHITECTURE.md](ARCHITECTURE.md) - Архитектура системы

---

**Последнее обновление:** Декабрь 2025

