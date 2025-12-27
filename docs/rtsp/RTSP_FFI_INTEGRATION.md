# RTSP Client FFI Integration

## Обзор

Реализована интеграция RTSP клиента с нативной C++ библиотекой через FFI биндинги (Kotlin/Native cinterop).

## Структура

### 1. C Interop Definition

Файл: `core/network/src/nativeInterop/cinterop/rtsp_client.def`

Определяет конфигурацию для генерации Kotlin биндингов из C заголовочного файла `rtsp_client.h`.

### 2. Native Implementation

Файл: `core/network/src/nativeMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.native.kt`

Реализует интеграцию с нативной библиотекой через cinterop:
- Создание и уничтожение RTSP клиента
- Подключение/отключение от сервера
- Управление воспроизведением (play/stop/pause)
- Получение информации о потоках
- Обработка callbacks для кадров и статуса

### 3. Build Configuration

Файл: `core/network/build.gradle.kts`

Настроены cinterop для следующих платформ:
- **Linux x64** (`linuxX64`)
- **macOS x64** (`macosX64`)
- **macOS ARM64** (`macosArm64`)
- **iOS x64** (`iosX64`)
- **iOS ARM64** (`iosArm64`)
- **iOS Simulator ARM64** (`iosSimulatorArm64`)
- **Windows x64** (`mingwX64`)
- **Android Native ARM32** (`androidNativeArm32`)
- **Android Native ARM64** (`androidNativeArm64`)
- **Android Native x86** (`androidNativeX86`)
- **Android Native x64** (`androidNativeX64`)

## Использование

### Создание клиента

```kotlin
val nativeClient = NativeRtspClient()
val handle = nativeClient.create()
```

### Подключение

```kotlin
val success = nativeClient.connect(
    handle = handle,
    url = "rtsp://192.168.1.100:554/stream",
    username = "admin",
    password = "password",
    timeoutMs = 10000
)
```

### Установка callbacks

```kotlin
// Callback для видеокадров
nativeClient.setFrameCallback(handle, RtspStreamType.VIDEO) { frame ->
    // Обработка кадра
    val imageData = frame.data
    val width = frame.width
    val height = frame.height
}

// Callback для изменения статуса
nativeClient.setStatusCallback(handle) { status, message ->
    println("Status: $status, Message: $message")
}
```

### Воспроизведение

```kotlin
nativeClient.play(handle)
```

### Получение информации о потоках

```kotlin
val streamCount = nativeClient.getStreamCount(handle)
for (i in 0 until streamCount) {
    val streamInfo = nativeClient.getStreamInfo(handle, i)
    println("Stream $i: ${streamInfo?.codec}, ${streamInfo?.resolution}")
}
```

### Освобождение ресурсов

```kotlin
nativeClient.destroy(handle)
```

## Важные замечания

1. **Callbacks**: Используются `StableRef` для безопасной передачи Kotlin функций в нативный код
2. **Память**: Кадры автоматически освобождаются после обработки через `rtsp_frame_release()`
3. **Thread Safety**: Все операции выполняются в соответствующих контекстах корутин
4. **Ошибки**: Ошибки в callbacks перехватываются и игнорируются, чтобы не нарушить работу нативной библиотеки

## Требования

- Нативная библиотека `libvideo_processing` должна быть скомпилирована для каждой целевой платформы
- Заголовочный файл `rtsp_client.h` должен быть доступен во время компиляции
- Для работы RTSP клиента требуется FFmpeg (если включен `ENABLE_FFMPEG`)

## Сборка нативной библиотеки

Нативная библиотека компилируется через CMake:

```bash
cd native
mkdir build
cd build
cmake ..
make
```

Библиотека должна быть размещена в соответствующих директориях:
- `native/video-processing/lib/linux/x64/`
- `native/video-processing/lib/macos/x64/`
- `native/video-processing/lib/macos/arm64/`
- и т.д.

## Следующие шаги

1. Компиляция нативной библиотеки для всех целевых платформ
2. Тестирование интеграции на каждой платформе
3. Оптимизация производительности callbacks
4. Добавление поддержки дополнительных функций RTSP протокола

