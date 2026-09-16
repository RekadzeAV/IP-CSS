# HLS & DASH Streaming

## Обзор

Модуль предоставляет поддержку альтернативных протоколов стриминга HLS и MPEG-DASH для совместимости с современными IP камерами и стриминговыми серверами.

## Features

- ✅ HLS (.m3u8) поддержка
- ✅ MPEG-DASH (.mpd) поддержка
- ✅ Adaptive Bitrate Streaming (ABR)
- ✅ Автовоспроизведение
- ✅ Интеграция с Video Decoder
- ✅ Буферизация и кэширование

## Быстрый старт

### HLS Playback

```kotlin
import com.company.ipcamera.core.streaming.*

// Создание HLS клиента
val config = HLSClientConfig(
    playlistUrl = "http://192.168.1.10:8080/stream.m3u8",
    enableAdaptiveBitrate = true,
    maxBufferSeconds = 30
)

val hlsClient = HLSClient(config)

// Подключение
val connected = hlsClient.connect()
if (!connected) {
    println("Failed to connect")
    return
}

// Воспроизведение
hlsClient.play()

// Обработка кадров
hlsClient.getStatus().collect { status ->
    when (status) {
        HLSStatus.PLAYING -> {
            val frame = hlsClient.getNextFrame()
            if (frame != null) {
                displayFrame(frame.data)
            }
        }
        HLSStatus.ERROR -> println("Error!")
        else -> {}
    }
}

// Закрытие
hlsClient.close()
```

### DASH Playback

```kotlin
// Создание DASH клиента
val config = DASHClientConfig(
    manifestUrl = "http://192.168.1.10:8080/stream.mpd",
    enableAdaptiveBitrate = true
)

val dashClient = DASHClient(config)

// Подключение
dashClient.connect()
dashClient.play()

// Обработка кадров
while (playing) {
    val frame = dashClient.getNextFrame()
    if (frame != null) {
        displayFrame(frame.data)
    }
}

dashClient.close()
```

### Unified Client Factory

```kotlin
val factory = StreamingClientFactory()

// Автоматическое определение типа потока
val client = factory.createClient(
    url = "http://192.168.1.10:8080/stream.m3u8",
    decoder = myDecoder
)

when (client) {
    is HLSClient -> println("HLS stream")
    is DASHClient -> println("DASH stream")
    is RtspClient -> println("RTSP stream")
}
```

## API Reference

### HLSClient

**Методы:**
- `suspend fun connect(): Boolean` - Подключение
- `suspend fun play()` - Воспроизведение
- `suspend fun pause()` - Приостановка
- `suspend fun stop()` - Остановка
- `fun getStatus(): StateFlow<HLSStatus>` - Статус
- `fun getStreamInfo(): HLSStreamInfo?` - Информация о потоке
- `suspend fun getNextFrame(): DecodedFrame?` - Следующий кадр
- `fun close()` - Закрытие

### HLSClientConfig

**Поля:**
- `playlistUrl: String` - URL плейлиста
- `timeoutMs: Long` - Таймаут
- `maxBufferSeconds: Int` - Макс буфер (сек)
- `minBufferSeconds: Int` - Мин буфер (сек)
- `enableAdaptiveBitrate: Boolean` - ABR
- `preferredVideoCodec: VideoCodec` - Предпочтительный видео кодек
- `preferredAudioCodec: AudioCodec` - Предпочтительный аудио кодек
- `decoder: VideoDecoder?` - Декодер
- `userAgent: String` - User-Agent

### DASHClient

Аналогично HLSClient с DASH-специфичными методами.

### HLSStatus / DASHStatus

**Значения:**
- `DISCONNECTED` - Не подключён
- `CONNECTING` - Подключение
- `CONNECTED` - Подключён
- `PLAYING` - Воспроизведение
- `PAUSED` - Приостановка
- `BUFFERING` - Буферизация
- `ERROR` - Ошибка
- `CLOSED` - Закрыт

## HLS Playlist Parser

### Базовое использование

```kotlin
val parser = HLSPlaylistParser()

// Парсинг плейлиста
val playlist = parser.parsePlaylist(m3u8Content)

// Информация
println("Duration: ${playlist.duration}s")
println("Target Duration: ${playlist.targetDuration}s")
println("Segments: ${playlist.segments.size}")
println("Live: ${playlist.isLive}")
```

### Парсинг вариативного плейлиста

```kotlin
val variants = parser.parseVariantPlaylist(masterPlaylistContent)

variants.forEach { variant ->
    println("Bitrate: ${variant.bandwidth} bps")
    println("Resolution: ${variant.resolution}")
    println("Codecs: ${variant.codecs}")
}
```

## DASH Manifest Parser

### Базовое использование

```kotlin
val parser = DASHManifestParser()

// Парсинг манифеста
val manifest = parser.parseManifest(mpdContent)

// Информация
println("Duration: ${manifest.duration}s")
println("Live: ${manifest.isLive}")
println("Profiles: ${manifest.profiles}")
```

## Adaptive Bitrate Streaming

### Автоматическое переключение

```kotlin
val abrManager = AdaptiveBitrateManager()

// Обновить доступные битрейты
abrManager.updateAvailableBitrates(
    listOf(500_000, 1_000_000, 2_000_000, 4_000_000)
)

// Обновить уровень буфера
abrManager.updateBufferLevel(15.0) // 15 секунд

// Получить оптимальный битрейт
val optimalBitrate = abrManager.selectOptimalBitrate()
println("Selected: ${optimalBitrate / 1000} kbps")
```

### Ручное управление

```kotlin
// Мониторинг буфера
streamStatus.collect { status ->
    if (status is HLSEvent.Buffering) {
        // Буферизация - снизить битрейт
        abrManager.updateBufferLevel(status.bufferSeconds)
    }
}
```

## Integration with Live555

```kotlin
// Подключение RTSP с fallback на HLS
fun connectStream(url: String, decoder: VideoDecoder): RtspClient? {
    return try {
        if (url.isSecureRtspUrl) {
            createSecureRtspClient(RTSPSClientConfig(url = url))
        } else if (url.isHlsUrl) {
            val hls = HLSClient(HLSClientConfig(url))
            // Обернуть в RtspClient интерфейс
            null
        } else if (url.isDashUrl) {
            val dash = DASHClient(DASHClientConfig(url))
            null
        } else {
            Live555RTSPClient(RtspClientConfig(url))
        }
    } catch (e: Exception) {
        null
    }
}
```

## Buffer Management

### Настройка буфера

```kotlin
val config = HLSClientConfig(
    playlistUrl = "http://...",
    maxBufferSeconds = 60,  // Максимум 60 секунд
    minBufferSeconds = 5    // Минимум 5 секунд
)
```

### Мониторинг буфера

```kotlin
streamStatus.collect { event ->
    when (event) {
        is HLSEvent.Buffering -> {
            println("Buffering: ${event.bufferSeconds}s")
        }
        is HLSEvent.BitrateChanged -> {
            println("Bitrate: ${event.newBitrate / 1000} kbps")
        }
        else -> {}
    }
}
```

## Troubleshooting

### Плейлист не загружается

**Проблема:** `connect()` возвращает false

**Решения:**
1. Проверьте URL плейлиста
2. Убедитесь что сервер доступен
3. Проверьте CORS настройки
4. Проверьте User-Agent

### Высокая буферизация

**Проблема:** Частая буферизация

**Решения:**
1. Увеличьте `maxBufferSeconds`
2. Уменьшите битрейт потока
3. Проверьте скорость сети
4. Включите ABR

### Artefacts на изображении

**Проблема:** Искажения видео

**Решения:**
1. Проверьте поддержку кодеков
2. Убедитесь что декодер инициализирован
3. Попробуйте программный декодер

### HLS не работает на iOS

**Проблема:** Ошибка на iOS

**Решения:**
1. Используйте AVPlayer нативно
2. Проверьте формат плейлиста
3. Убедитесь что сегменты доступны

## Performance

### Бенчмарки

| Протокол | Разрешение | FPS | Buffer | CPU |
|----------|------------|-----|--------|-----|
| HLS | 1920x1080 | 30 | 10s | ~15% |
| DASH | 1920x1080 | 30 | 10s | ~15% |
| RTSP | 1920x1080 | 30 | 1s | ~10% |

### Рекомендации

1. **HLS лучше для** публичных сетей и CDN
2. **DASH лучше для** адаптивного битрейта
3. **RTSP лучше для** локальных камер с низкой задержкой

## См. также

- [Video Decoder API](VIDEO_DECODER_API.md)
- [Live555 Integration](LIVE555_CINTEROP_INTEGRATION.md)
- [RTSPS Client](RTSPS_CLIENT.md)

---

**Версия:** 1.0  
**Дата:** 2025-01-15  
**Автор:** NLP-Core-Team
