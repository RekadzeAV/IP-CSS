# RTSP Client Documentation

**Версия проекта:** Alfa-0.0.1
**Последнее обновление:** Декабрь 2025

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

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

## Установка зависимостей

Перед использованием RTSP клиента необходимо установить зависимости для компиляции нативной библиотеки.

### Требуемые зависимости

- **CMake** (≥ 3.15) - для сборки нативной библиотеки
- **FFmpeg** (libavformat, libavcodec, libavutil, libswscale, libswresample) - для обработки видео и аудио потоков
- **pkg-config** - для обнаружения FFmpeg

### macOS

```bash
# Установка Homebrew (если не установлен)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Установка зависимостей
brew install cmake ffmpeg pkg-config

# Проверка установки
cmake --version
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "FFmpeg NOT FOUND"
```

### Linux (Ubuntu/Debian)

```bash
sudo apt-get update
sudo apt-get install -y \
    cmake \
    build-essential \
    pkg-config \
    libavformat-dev \
    libavcodec-dev \
    libavutil-dev \
    libswscale-dev \
    libswresample-dev

# Проверка установки
cmake --version
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "FFmpeg NOT FOUND"
```

## Компиляция и установка

### Шаг 1: Компиляция нативной библиотеки

```bash
# Автоматическая сборка (рекомендуется)
./scripts/build-native-lib.sh

# Или вручную:
cd native/video-processing
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

**Проверка успешной компиляции:**

```bash
# Linux
ls -lh native/video-processing/build/libvideo_processing.so

# macOS
ls -lh native/video-processing/build/libvideo_processing.dylib
```

### Шаг 2: Проверка экспорта символов

**Linux:**
```bash
nm -D native/video-processing/build/libvideo_processing.so | grep rtsp_client
```

**macOS:**
```bash
nm -gU native/video-processing/build/libvideo_processing.dylib | grep rtsp_client
```

Должны быть видны символы:
- `rtsp_client_create`
- `rtsp_client_connect`
- `rtsp_client_play`
- `rtsp_client_stop`
- `rtsp_client_pause`

### Шаг 3: Генерация cinterop биндингов

```bash
# Сборка для всех native платформ
./gradlew :core:network:compileKotlinNative

# Или для конкретных платформ:
./gradlew :core:network:compileKotlinLinuxX64
./gradlew :core:network:compileKotlinMacosX64
./gradlew :core:network:compileKotlinMacosArm64
```

## Активация кода

После установки зависимостей и компиляции библиотеки, необходимо активировать код в `NativeRtspClient.native.kt`.

### Чек-лист активации

#### 1. Раскомментировать импорты

Найти строку:
```kotlin
// import com.company.ipcamera.core.network.rtsp.rtsp_client.*
```

Изменить на:
```kotlin
import com.company.ipcamera.core.network.rtsp.rtsp_client.*
```

#### 2. Раскомментировать реализацию методов

Для каждого метода с комментариями `// TODO: После компиляции cinterop...`, раскомментировать реализацию:

- `create()`
- `connect()`
- `disconnect()`
- `getStatus()`
- `play()`
- `stop()`
- `pause()`
- `getStreamCount()`
- `getStreamType()`
- `getStreamInfo()`

#### 3. Раскомментировать вспомогательные функции

В конце файла раскомментировать все вспомогательные функции:
- `handleToPointer()`
- `convertNativeStatus()`
- `convertNativeStreamType()`
- `convertStreamType()`

#### 4. Реализовать callbacks

Реализовать методы `setFrameCallback()` и `setStatusCallback()` с использованием StableRef для thread-safety.

#### 5. Проверка компиляции

```bash
./gradlew :core:network:compileKotlinNative
```

## Интеграция с видеоплеером

### HLS генерация

Реализована полная интеграция видеоплеера с RTSP клиентом для веб-интерфейса и Android приложения:

- **HlsGeneratorService** - генерация HLS сегментов через FFmpeg
- **VideoStreamService** - управление трансляцией RTSP потоков
- **ScreenshotService** - создание снимков кадров
- Поддержка разных уровней качества (LOW, MEDIUM, HIGH, ULTRA)

### API Endpoints для видеоплеера

- `POST /api/v1/cameras/{id}/stream/start` - запустить трансляцию
- `POST /api/v1/cameras/{id}/stream/stop` - остановить трансляцию
- `GET /api/v1/cameras/{id}/stream/status` - получить статус
- `GET /api/v1/cameras/streams/{streamId}/hls/playlist.m3u8` - HLS плейлист
- `POST /api/v1/cameras/{id}/stream/screenshot` - создать снимок

Подробная документация по интеграции видеоплеера: [RTSP_VIDEO_PLAYER_INTEGRATION.md](RTSP_VIDEO_PLAYER_INTEGRATION.md)

## Статус реализации

**Текущий прогресс:** ~50% (инфраструктура готова, требуется активация)

### ✅ Реализовано

- ✅ Конфигурация cinterop настроена
- ✅ Конфигурация build.gradle.kts обновлена
- ✅ Конфигурация CMakeLists.txt обновлена
- ✅ Структура кода подготовлена
- ✅ Документация создана
- ✅ Скрипты автоматизации созданы
- ✅ Kotlin обертка с базовой структурой
- ✅ Нативная C++ библиотека с заголовками и базовой структурой

### ⚠️ Требуется активация

- ⚠️ Компиляция библиотеки (требует установки зависимостей)
- ⚠️ Активация кода (раскомментирование в NativeRtspClient.native.kt)

### ❌ Не реализовано

- ❌ Тестирование
- ❌ Android/iOS платформо-специфичные реализации
- ❌ Полная реализация RTSP протокола (частично)
- ❌ RTP/RTCP обработка (частично)
- ❌ Декодирование видео/аудио (H.264, H.265, AAC) - частично через FFmpeg
- ❌ Аутентификация (Basic, Digest) - частично

**Детальный анализ нереализованного функционала:** [MISSING_FUNCTIONALITY.md](MISSING_FUNCTIONALITY.md#rtspclient)

---

## Связанные документы

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

### Основные документы
- **[DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)** - Полный индекс документации
- **[README.md](../README.md)** - Обзор проекта

### Статус и анализ
- **[MISSING_FUNCTIONALITY.md](MISSING_FUNCTIONALITY.md#rtspclient)** - Детальный анализ нереализованного функционала RTSP клиента
- **[IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)** - Статус реализации компонентов

### Интеграция и разработка
- **[INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md#2-rtsp-клиент---интеграция-live555)** - Руководство по интеграции библиотек (RTSP клиент - интеграция Live555)
- **[NATIVE_LIBRARIES_INTEGRATION.md](NATIVE_LIBRARIES_INTEGRATION.md)** - Интеграция нативных C++ библиотек
- **[PLATFORMS.md](PLATFORMS.md)** - Разделение разработки по платформам
- **[DEVELOPMENT.md](DEVELOPMENT.md)** - Руководство по разработке

---

**Последнее обновление:** Декабрь 2025

