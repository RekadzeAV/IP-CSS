# RTSP Client Documentation

## Обзор

RTSP клиент обеспечивает подключение к камерам по протоколу RTSP (Real Time Streaming Protocol) для получения видеопотоков. Клиент поддерживает множественные потоки (видео, аудио), автоматическое переподключение и работу с различными кодеками.

## Архитектура

RTSP клиент состоит из двух частей:
1. **Нативная C++ библиотека** (`native/video-processing/src/rtsp_client.cpp`) - реализация RTSP протокола
2. **Kotlin обертка** (`core/network/src/.../RtspClient.kt`) - высокоуровневый API для использования в Kotlin коде

## Использование

### Создание клиента

```kotlin
import com.company.ipcamera.core.network.*

val config = RtspClientConfig(
    url = "rtsp://192.168.1.100:554/stream1",
    username = "admin",
    password = "password123",
    enableVideo = true,
    enableAudio = true,
    timeoutMillis = 10000
)

val rtspClient = RtspClient(config)
```

### Подключение и воспроизведение

```kotlin
// Подключение
rtspClient.connect()

// Отслеживание статуса
rtspClient.getStatus().collect { status ->
    when (status) {
        RtspClientStatus.CONNECTED -> println("Connected")
        RtspClientStatus.PLAYING -> println("Playing")
        RtspClientStatus.ERROR -> println("Error occurred")
        else -> {}
    }
}

// Начало воспроизведения
rtspClient.play()

// Получение видеокадров
rtspClient.getVideoFrames().collect { frame ->
    // Обработка видеокадра
    val imageData = frame.data
    val width = frame.width
    val height = frame.height
    val timestamp = frame.timestamp
}

// Получение аудиокадров
rtspClient.getAudioFrames().collect { frame ->
    // Обработка аудиокадра
    val audioData = frame.data
}
```

### Callback обработка

```kotlin
rtspClient.setVideoFrameCallback { frame ->
    // Обработка видеокадра
    displayFrame(frame.data, frame.width, frame.height)
}

rtspClient.setAudioFrameCallback { frame ->
    // Обработка аудиокадра
    playAudio(frame.data)
}

rtspClient.setStatusCallback { status, message ->
    println("Status changed: $status - $message")
}
```

### Управление воспроизведением

```kotlin
// Пауза
rtspClient.pause()

// Возобновление
rtspClient.play()

// Остановка
rtspClient.stop()

// Отключение
rtspClient.disconnect()
```

### Получение информации о потоках

```kotlin
val streams = rtspClient.getStreams()
streams.forEach { stream ->
    println("Stream ${stream.index}:")
    println("  Type: ${stream.type}")
    println("  Resolution: ${stream.resolution}")
    println("  FPS: ${stream.fps}")
    println("  Codec: ${stream.codec}")
}

// Получение информации о конкретном потоке
val videoStream = rtspClient.getStreamInfo(0)
videoStream?.let {
    println("Video stream: ${it.resolution} @ ${it.fps} fps")
}
```

## API

### RtspClientConfig

Конфигурация RTSP клиента:

- `url: String` - RTSP URL камеры (например: `rtsp://192.168.1.100:554/stream1`)
- `username: String?` - имя пользователя (опционально)
- `password: String?` - пароль (опционально)
- `timeoutMillis: Long` - таймаут подключения (по умолчанию: 10000)
- `bufferSize: Int` - размер буфера для кадров (по умолчанию: 1MB)
- `enableVideo: Boolean` - включить видеопоток (по умолчанию: true)
- `enableAudio: Boolean` - включить аудиопоток (по умолчанию: true)
- `enableMetadata: Boolean` - включить метаданные (по умолчанию: false)

### Методы

- `connect()` - подключиться к RTSP серверу
- `play()` - начать воспроизведение
- `pause()` - приостановить воспроизведение
- `stop()` - остановить воспроизведение
- `disconnect()` - отключиться от сервера
- `getStatus(): StateFlow<RtspClientStatus>` - получить статус клиента
- `getVideoFrames(): SharedFlow<RtspFrame>` - получить поток видеокадров
- `getAudioFrames(): SharedFlow<RtspFrame>` - получить поток аудиокадров
- `getStreams(): List<RtspStreamInfo>` - получить список потоков
- `getStreamInfo(index: Int): RtspStreamInfo?` - получить информацию о потоке
- `setVideoFrameCallback(callback: RtspFrameCallback?)` - установить callback для видео
- `setAudioFrameCallback(callback: RtspFrameCallback?)` - установить callback для аудио
- `setStatusCallback(callback: RtspStatusCallback?)` - установить callback для статуса
- `close()` - закрыть клиент и освободить ресурсы

### Типы данных

#### RtspFrame
Кадр потока:
```kotlin
data class RtspFrame(
    val data: ByteArray,          // Данные кадра
    val timestamp: Long,          // Временная метка
    val streamType: RtspStreamType, // Тип потока
    val width: Int = 0,           // Ширина (для видео)
    val height: Int = 0           // Высота (для видео)
)
```

#### RtspStreamInfo
Информация о потоке:
```kotlin
data class RtspStreamInfo(
    val index: Int,               // Индекс потока
    val type: RtspStreamType,     // Тип потока
    val resolution: Resolution?,  // Разрешение (для видео)
    val fps: Int,                 // FPS (для видео)
    val codec: String             // Кодек
)
```

#### RtspStreamType
Тип потока:
- `VIDEO` - видеопоток
- `AUDIO` - аудиопоток
- `METADATA` - метаданные

#### RtspClientStatus
Статус клиента:
- `DISCONNECTED` - отключен
- `CONNECTING` - подключение
- `CONNECTED` - подключен
- `PLAYING` - воспроизведение
- `ERROR` - ошибка

## Примеры использования

### Базовое использование

```kotlin
class CameraStreamPlayer {
    private val rtspClient: RtspClient

    init {
        val config = RtspClientConfig(
            url = "rtsp://camera.example.com/stream",
            username = "admin",
            password = "password"
        )
        rtspClient = RtspClient(config)

        rtspClient.setVideoFrameCallback { frame ->
            updateVideoView(frame)
        }
    }

    suspend fun start() {
        rtspClient.connect()
        rtspClient.play()
    }

    fun stop() {
        rtspClient.stop()
        rtspClient.disconnect()
    }

    private fun updateVideoView(frame: RtspFrame) {
        // Обновление UI с новым кадром
    }
}
```

### Множественные потоки

```kotlin
val config = RtspClientConfig(
    url = "rtsp://camera.example.com/stream",
    enableVideo = true,
    enableAudio = true
)

val rtspClient = RtspClient(config)

// Обработка видео
rtspClient.getVideoFrames().collect { frame ->
    processVideoFrame(frame)
}

// Обработка аудио
rtspClient.getAudioFrames().collect { frame ->
    processAudioFrame(frame)
}
```

### Обработка ошибок

```kotlin
rtspClient.setStatusCallback { status, message ->
    when (status) {
        RtspClientStatus.ERROR -> {
            println("Error: $message")
            // Попытка переподключения
            rtspClient.disconnect()
            delay(5000)
            rtspClient.connect()
        }
        else -> {}
    }
}
```

## Нативная библиотека

### C++ API

Нативная библиотека предоставляет следующие функции:

- `rtsp_client_create()` - создание клиента
- `rtsp_client_destroy()` - уничтожение клиента
- `rtsp_client_connect()` - подключение
- `rtsp_client_disconnect()` - отключение
- `rtsp_client_play()` - воспроизведение
- `rtsp_client_stop()` - остановка
- `rtsp_client_pause()` - пауза
- `rtsp_client_set_frame_callback()` - установка callback для кадров
- `rtsp_client_set_status_callback()` - установка callback для статуса

### Компиляция

Нативная библиотека компилируется с помощью CMake:

```bash
cd native/video-processing
mkdir build && cd build
cmake ..
make
```

## Примечания

- В текущей реализации RTSP клиент использует упрощенную версию протокола
- Для продакшена рекомендуется интеграция с библиотеками типа Live555 или libVLC
- Поддержка различных кодеков (H.264, H.265, MJPEG) зависит от нативной реализации
- Аудиопотоки требуют дополнительной обработки для декодирования
- FFI биндинги для Kotlin/Native должны быть настроены для каждой платформы отдельно

## Статус реализации

**Текущий прогресс:** ~10%

**Реализовано:**
- ✅ Kotlin обертка с базовой структурой
- ✅ Нативная C++ библиотека с заголовками и базовой структурой

**Не реализовано:**
- ❌ Интеграция Kotlin ↔ C++ (FFI биндинги)
- ❌ Реальная реализация RTSP протокола (все функции - заглушки)
- ❌ RTP/RTCP обработка
- ❌ Декодирование видео/аудио (H.264, H.265, AAC)
- ❌ Аутентификация (Basic, Digest)

**Детальный анализ нереализованного функционала:** [MISSING_FUNCTIONALITY.md](MISSING_FUNCTIONALITY.md#rtspclient)
**Руководство по интеграции:** [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md#2-rtsp-клиент---интеграция-live555)
**Разделение разработки по платформам:** [PLATFORMS.md](PLATFORMS.md)

