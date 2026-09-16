# Android RTSP Integration Guide

**Дата:** 2026-01-28  
**Статус:** ✅ Завершено  
**Версия:** 1.0

---

## 📋 Обзор

Android интеграция для IP-CSS системы с поддержкой RTSP/HLS видеопотоков.

### Реализованные компоненты:

1. ✅ **ExoPlayer Integration** — видеоплеер с поддержкой RTSP/HLS
2. ✅ **Low-Latency Mode** — оптимизация для низкой задержки
3. ✅ **HLS Fallback** — автоматическое переключение при ошибках RTSP
4. ✅ **Auto-Reconnect** — экспоненциальное переподключение
5. ✅ **MediaSession** — фоновое воспроизведение и уведомления
6. ✅ **Picture-in-Picture** — режим PiP для многозадачности
7. ✅ **Frame Analytics** — захват кадров для локальной аналитики

---

## 🏗️ Архитектура

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│  Android App    │─────▶│  ExoVideoPlayer  │─────▶│  RTSP Camera    │
│  (Jetpack Compose)│    │  (ExoPlayer)     │      │  (H.264/H.265)  │
└─────────────────┘      └──────────────────┘      └─────────────────┘
                                │
                                ▼
                         ┌──────────────────┐
                         │  HLS Fallback    │
                         │  (Server-side)   │
                         └──────────────────┘
                                │
                                ▼
                         ┌──────────────────┐
                         │  MediaSession    │
                         │  (Background)    │
                         └──────────────────┘
```

---

## 🚀 Быстрый старт

### 1. Добавление зависимости

```kotlin
// android/app/build.gradle.kts
dependencies {
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-session:1.2.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.2.0")
    
    // RTSP support (через okhttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
```

### 2. Базовое использование

```kotlin
@Composable
fun CameraStreamScreen(cameraUrl: String) {
    ExoVideoPlayer(
        videoUrl = cameraUrl,
        hlsFallbackUrl = "https://server.com/hls/camera1/playlist.m3u8",
        streamType = StreamType.RTSP,
        autoPlay = true,
        enableLowLatency = true,
        modifier = Modifier.fillMaxSize(),
        onPlayerReady = { player ->
            Log.d("CameraStream", "Player ready")
        },
        onError = { error ->
            Log.e("CameraStream", "Error: ${error.message}")
        },
        onRetry = {
            Log.d("CameraStream", "Retrying...")
        }
    )
}
```

### 3. Расширенная конфигурация

```kotlin
@Composable
fun AdvancedCameraStream(cameraUrl: String) {
    ExoVideoPlayer(
        videoUrl = cameraUrl,
        hlsFallbackUrl = "https://server.com/hls/camera1/playlist.m3u8",
        streamType = StreamType.RTSP,
        autoPlay = true,
        enableLowLatency = true,
        enableBackgroundPlayback = true,
        mediaTitle = "Camera 1",
        mediaSubtitle = "Live Stream",
        localFrameAnalyticsEnabled = true,
        localFrameAnalyticsIntervalMs = 1000L,
        onLocalAnalyticsRgbFrame = { width, height, rgbData ->
            // Обработка кадра для аналитики
            processFrame(width, height, rgbData)
        },
        onStatsUpdate = { stats ->
            Log.d("CameraStream", """
                Buffer: ${stats.bufferHealth}%
                Position: ${stats.playbackPosition}ms
                Buffered: ${stats.bufferedPosition}ms
            """.trimIndent())
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

---

## 📊 Компоненты

### 1. ExoVideoPlayer

**Файл:** `android/app/src/main/java/com/company/ipcamera/android/ui/components/ExoVideoPlayer.kt`

**Параметры:**

| Параметр | Тип | По умолчанию | Описание |
|----------|-----|--------------|----------|
| `videoUrl` | String? | null | URL видеопотока (RTSP/HLS) |
| `hlsFallbackUrl` | String? | null | Резервный HLS URL |
| `streamType` | StreamType? | null | Тип потока (RTSP/HLS) |
| `autoPlay` | Boolean | true | Автовоспроизведение |
| `enableLowLatency` | Boolean | true | Низкая задержка |
| `enableBackgroundPlayback` | Boolean | false | Фоновое воспроизведение |
| `mediaTitle` | String? | null | Заголовок для MediaSession |
| `mediaSubtitle` | String? | null | Подзаголовок для MediaSession |
| `localFrameAnalyticsEnabled` | Boolean | false | Захват кадров |
| `localFrameAnalyticsIntervalMs` | Long | 1000L | Интервал захвата (мс) |

**Callbacks:**
- `onPlayerReady` — плеер готов
- `onError` — ошибка воспроизведения
- `onRetry` — повторная попытка
- `onStatsUpdate` — обновление статистики
- `onLocalAnalyticsRgbFrame` — захваченный кадр (RGB24)

---

### 2. StreamType

```kotlin
enum class StreamType {
    RTSP,  // Прямой RTSP поток (низкая задержка)
    HLS    // HLS поток (более стабильный)
}
```

**Автоматическое определение:**
- `rtsp://` → RTSP
- `.m3u8` или `/hls/` → HLS
- По умолчанию → HLS

---

### 3. PlaybackStats

```kotlin
data class PlaybackStats(
    val videoFormat: Format? = null,
    val videoBitrate: Int = 0,
    val videoFrameRate: Float = 0f,
    val audioFormat: Format? = null,
    val audioBitrate: Int = 0,
    val bufferHealth: Long = 0,
    val playbackPosition: Long = 0,
    val bufferedPosition: Long = 0
)
```

---

## 🔧 Конфигурация

### 1. Low-Latency Mode (RTSP)

```kotlin
ExoVideoPlayer(
    videoUrl = "rtsp://192.168.1.100:554/stream1",
    enableLowLatency = true,
    // Внутренние настройки буфера:
    // minBufferMs: 1000
    // maxBufferMs: 2000
    // bufferForPlaybackMs: 500
    // bufferForPlaybackAfterRebufferMs: 500
)
```

**Задержка:** 1-3 секунды (vs 5-10 сек в стандартном режиме)

### 2. HLS Fallback

```kotlin
ExoVideoPlayer(
    videoUrl = "rtsp://192.168.1.100:554/stream1",
    hlsFallbackUrl = "https://server.com/hls/camera1/playlist.m3u8",
    // Автоматическое переключение при ошибках:
    // - NETWORK (connection failed/timeout)
    // - PARSING (malformed container/manifest)
    // - DECODER (init failed/decoding failed)
)
```

### 3. Auto-Reconnect

```kotlin
// Встроенная логика:
// - Максимум попыток: 3
// - Задержка: 2s * retryCount (экспоненциальная)
// - Сброс при успешном подключении
```

### 4. Background Playback

```kotlin
ExoVideoPlayer(
    videoUrl = cameraUrl,
    enableBackgroundPlayback = true,
    mediaTitle = "Camera 1",
    mediaSubtitle = "Live Stream",
    // MediaSession Manager автоматически:
    // - Создает notification
    // - Обрабатывает media buttons
    // - Поддерживает Picture-in-Picture
)
```

---

## 🎯 Функции

### 1. Автоматическое переключение RTSP → HLS

**Логика:**
```
RTSP Stream
    ↓
[Error: NETWORK/PARSING/DECODER]
    ↓
Check hlsFallbackUrl
    ↓
[HLS Available] → Switch to HLS
    ↓
[HLS Not Available] → Retry RTSP (3 attempts)
    ↓
[All Retries Failed] → Show Error
```

### 2. Экспоненциальное переподключение

**Алгоритм:**
```
Attempt 1: Wait 2s
Attempt 2: Wait 4s
Attempt 3: Wait 6s
Max Attempts: 3
```

### 3. Захват кадров для аналитики

```kotlin
ExoVideoPlayer(
    localFrameAnalyticsEnabled = true,
    localFrameAnalyticsIntervalMs = 1000L, // 1 кадр/сек
    onLocalAnalyticsRgbFrame = { width, height, rgbData ->
        // rgbData: ByteArray (width * height * 3)
        // Формат: RGB24, порядок R,G,B
        // Использование: object detection, motion detection, etc.
        
        processFrameForAnalytics(width, height, rgbData)
    }
)
```

---

## 📱 Picture-in-Picture (PiP)

### 1. Разрешение в Manifest

```xml
<!-- android/app/src/main/AndroidManifest.xml -->
<activity
    android:name=".MainActivity"
    android:configChanges="|screenSize|smallestScreenSize|screenLayout|orientation"
    android:resizeableActivity="true"
    android:supportsPictureInPicture="true"
    android:launchMode="singleTask">
</activity>
```

### 2. Вход в PiP режим

```kotlin
@Composable
fun VideoViewScreen(viewModel: VideoViewViewModel = viewModel()) {
    val context = LocalContext.current
    
    ExoVideoPlayer(
        videoUrl = viewModel.cameraUrl,
        enableBackgroundPlayback = true,
        modifier = Modifier.fillMaxSize()
    )
    
    // Кнопка для входа в PiP
    FloatingActionButton(
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val activity = context as? Activity
                activity?.enterPictureInPictureMode(
                    PictureInPictureParams.Builder().build()
                )
            }
        }
    ) {
        Icon(Icons.Default.PictureInPicture, "PiP")
    }
}
```

---

## 🐛 Troubleshooting

### Проблема: RTSP не подключается

**Решение:**
1. Проверить доступность камеры: `ping 192.168.1.100`
2. Проверить порт: `telnet 192.168.1.100 554`
3. Использовать HLS fallback
4. Включить debug логи:
```kotlin
android.util.Log.setLevel(Log.DEBUG)
```

### Проблема: Высокая задержка (10+ сек)

**Решение:**
1. Включить `enableLowLatency = true`
2. Проверить ключевые кадры камеры (GOP < 2s)
3. Использовать TCP вместо UDP:
```kotlin
// Через okhttp interceptor
val client = OkHttpClient.Builder()
    .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
    .build()
```

### Проблема: Плеер не восстанавливается после ошибки

**Решение:**
1. Проверить `onRetry` callback
2. Увеличить `maxRetries` (по умолчанию 3)
3. Проверить `hlsFallbackUrl`

### Проблема: Фоновое воспроизведение не работает

**Решение:**
1. Проверить `enableBackgroundPlayback = true`
2. Проверить MediaSession в логах
3. Проверить разрешения в Manifest:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

---

## 📊 Метрики производительности

### Задержка (End-to-End Latency)

| Режим | Задержка | Буфер |
|-------|----------|-------|
| RTSP (Low-Latency) | 1-3 сек | 1-2 сек |
| RTSP (Standard) | 5-10 сек | 5-15 сек |
| HLS (Standard) | 10-15 сек | 5-15 сек |
| LL-HLS | 2-4 сек | 2-4 сек |

### Использование памяти

| Компонент | Память |
|-----------|--------|
| ExoPlayer Instance | ~20-50 MB |
| Video Buffer | ~5-15 MB |
| Audio Buffer | ~1-3 MB |
| Frame Analytics | ~10-20 MB (дополнительно) |

### CPU Usage

| Режим | CPU |
|-------|-----|
| RTSP Decode (1080p) | ~15-25% |
| HLS Decode (1080p) | ~10-20% |
| Background Playback | ~5-10% |
| Frame Analytics (1fps) | ~5-10% (дополнительно) |

---

## 📚 Связанные документы

- [ExoVideoPlayer.kt](android/app/src/main/java/com/company/ipcamera/android/ui/components/ExoVideoPlayer.kt)
- [MediaSessionManager.kt](android/app/src/main/java/com/company/ipcamera/android/media/MediaSessionManager.kt)
- [HLS_LOW_LATENCY_OPTIMIZATION.md](docs/HLS_LOW_LATENCY_OPTIMIZATION.md)
- [RtspClient.kt](core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/RtspClient.kt)

---

## 🎯 Следующие шаги

1. ✅ ExoPlayer Integration — завершено
2. ✅ Low-Latency Mode — завершено
3. ✅ HLS Fallback — завершено
4. ✅ MediaSession/PiP — завершено
5. ⏳ Hardware Decoder Optimization — запланировано
6. ⏳ Adaptive Bitrate Streaming — запланировано

---

**Подготовлено:** NLP-Core-Team  
**Дата:** 2026-01-28  
**Статус:** ✅ Завершено
