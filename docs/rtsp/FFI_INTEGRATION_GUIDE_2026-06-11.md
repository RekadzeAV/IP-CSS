# RTSP Client Kotlin FFI Integration Guide

**Дата:** 11 June 2026  
**Версия:** 1.0  
**Статус:** ✅ Complete

---

## Overview

Этот документ описывает, как использовать нативный RTSP client из Kotlin Multiplatform кода через FFI bindings.

---

## Quick Start

### 1. Добавление зависимостей

В `build.gradle.kts` модуля:

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:network"))
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(project(":core:network"))
                // Нативная библиотека автоматически включается
            }
        }
    }
}
```

### 2. Базовое использование

```kotlin
import com.ipcss.core.network.rtsp.RtspClient
import com.ipcss.core.network.rtsp.RtspConfig
import com.ipcss.core.network.rtsp.RtspFrame
import com.ipcss.core.network.rtsp.RtspStatus

class RtspCameraConnection(
    private val rtspUrl: String
) {
    private val client = RtspClient()
    private var isConnected = false
    
    fun connect() {
        val config = RtspConfig(
            username = null,
            password = null,
            timeoutMs = 10000,
            enableReconnect = true,
            maxReconnectAttempts = 3
        )
        
        client.configure(config)
        
        // Установить статус callback
        client.statusCallback = { status, message, userData ->
            when (status) {
                RtspStatus.CONNECTING -> println("Connecting...")
                RtspStatus.CONNECTED -> {
                    println("Connected: $message")
                    isConnected = true
                    // Запустить поток
                    client.play()
                }
                RtspStatus.STREAMING -> println("Streaming: $message")
                RtspStatus.ERROR -> {
                    println("Error: $message")
                    isConnected = false
                }
                RtspStatus.DISCONNECTED -> {
                    println("Disconnected")
                    isConnected = false
                }
            }
        }
        
        // Установить видео callback
        client.videoCallback = { frame, userData ->
            when (frame.type) {
                RtspFrameType.VIDEO -> {
                    // Обработать видео кадр
                    processVideoFrame(frame)
                }
                RtspFrameType.AUDIO -> {
                    // Обработать аудио кадр
                    processAudioFrame(frame)
                }
            }
        }
        
        // Подключиться
        val success = client.connect(rtspUrl)
        if (!success) {
            throw RuntimeException("Failed to connect to RTSP stream")
        }
    }
    
    fun disconnect() {
        client.disconnect()
        isConnected = false
    }
    
    private fun processVideoFrame(frame: RtspFrame) {
        // Обработка видео кадров (H264/H265)
        val naluData = frame.data
        
        // Здесь можно:
        // 1. Отправить на декодер (FFmpeg/OpenH264)
        // 2. Сохранить в файл
        // 3. Отправить через WebSocket
        // 4. Показать в UI
    }
    
    private fun processAudioFrame(frame: RtspFrame) {
        // Обработка аудио кадров (AAC/PCMU/PCMA)
        val audioData = frame.data
        
        // Здесь можно:
        // 1. Отправить на аудио декодер
        // 2. Воспроизвести через AudioTrack
        // 3. Сохранить в файл
    }
    
    fun pause() {
        client.pause()
    }
    
    fun resume() {
        client.play()
    }
    
    fun isStreaming(): Boolean = isConnected && client.isPlaying()
}
```

---

## API Reference

### RtspClient Class

#### Constructors

```kotlin
// Создать новый RTSP client
val client = RtspClient()
```

#### Configuration

```kotlin
// Конфигурация RTSP клиента
data class RtspConfig(
    val username: String?,           // Опциональное имя пользователя
    val password: String?,           // Опциональный пароль
    val timeoutMs: Long,             // Таймаут подключения (мс)
    val enableReconnect: Boolean,    // Включить авто-переподключение
    val reconnectDelayMs: Long,      // Задержка между попытками (мс)
    val maxReconnectAttempts: Int    // Максимум попыток переподключения
)

// Применить конфигурацию
client.configure(config)
```

#### Connection Methods

```kotlin
// Подключиться к RTSP потоку
fun connect(url: String): Boolean

// Отключиться от RTSP потока
fun disconnect()

// Проверить статус подключения
fun isConnected(): Boolean

// Проверить статус потока
fun isPlaying(): Boolean
```

#### Stream Control

```kotlin
// Запустить поток (PLAY)
fun play(): Boolean

// Приостановить поток (PAUSE)
fun pause(): Boolean

// Остановить поток (TEARDOWN)
fun stop(): Boolean
```

#### Callbacks

```kotlin
// Статус callback
var statusCallback: ((status: RtspStatus, message: String, userData: Pointer?) -> Unit)? = null

// Фрейм callback (видео + аудио)
var frameCallback: ((frame: RtspFrame, userData: Pointer?) -> Unit)? = null

// Альтернативные специализированные callbacks
var videoCallback: ((frame: RtspFrame, userData: Pointer?) -> Unit)? = null
var audioCallback: ((frame: RtspFrame, userData: Pointer?) -> Unit)? = null
```

#### Stream Information

```kotlin
// Получить список доступных потоков
fun getStreams(): List<RtspStream>

data class RtspStream(
    val type: RtspStreamType,      // VIDEO или AUDIO
    val codec: String,             // H264, H265, AAC, PCMU, PCMA
    val width: Int,                // Ширина (для видео)
    val height: Int,               // Высота (для видео)
    val fps: Int,                  // FPS (для видео)
    val trackId: String?           // Track ID потока
)

enum class RtspStreamType {
    VIDEO,
    AUDIO
}
```

#### Statistics

```kotlin
// Получить статистику
fun getStatistics(): RtspStatistics

data class RtspStatistics(
    val packetsReceived: Int,
    val packetsLost: Int,
    val bytesReceived: Long,
    val jitter: Int,
    val fps: Float
)
```

---

## Advanced Usage

### 1. Обработка различных кодеков

```kotlin
client.frameCallback = { frame, userData ->
    when {
        frame.isVideo -> {
            when (frame.codec) {
                "H264" -> processH264Frame(frame)
                "H265" -> processH265Frame(frame)
                else -> Log.w("RTSP", "Unknown video codec: ${frame.codec}")
            }
        }
        frame.isAudio -> {
            when (frame.codec) {
                "AAC" -> processAACFrame(frame)
                "PCMU" -> processPCMUFrame(frame)
                "PCMA" -> processPCMAFrame(frame)
                else -> Log.w("RTSP", "Unknown audio codec: ${frame.codec}")
            }
        }
    }
}

private fun processH264Frame(frame: RtspFrame) {
    // H264 NAL unit data
    val naluData = frame.data
    
    // Проверка на SPS/PPS (первый NAL unit обычно содержит SPS/PPS)
    if (frame.isKeyFrame) {
        // Извлечь SPS/PPS из первого кадра
        val spsPps = extractSpsPps(naluData)
        // Инициализировать декодер с SPS/PPS
        videoDecoder.init(spsPps.sps, spsPps.pps)
    }
    
    // Декодировать кадр
    val yuvData = videoDecoder.decode(naluData)
    
    // Показать в UI
    displayFrame(yuvData)
}

private fun processAACFrame(frame: RtspFrame) {
    // AAC ADTS payload
    val aacData = frame.data
    
    // Инициализировать AAC декодер при первом кадре
    if (!aacDecoderInitialized) {
        aacDecoder.init(aacConfig)
        aacDecoderInitialized = true
    }
    
    // Декодировать PCM
    val pcmData = aacDecoder.decode(aacData)
    
    // Воспроизвести аудио
    audioTrack.write(pcmData)
}
```

### 2. Авто-переподключение

```kotlin
class ResilientRtspConnection(
    private val rtspUrl: String,
    private val maxReconnectAttempts: Int = 5
) {
    private val client = RtspClient()
    private var reconnectCount = 0
    
    init {
        val config = RtspConfig(
            enableReconnect = true,
            reconnectDelayMs = 5000,
            maxReconnectAttempts = maxReconnectAttempts
        )
        client.configure(config)
        
        client.statusCallback = { status, message, _ ->
            when (status) {
                RtspStatus.ERROR -> {
                    Log.e("RTSP", "Connection error: $message")
                    if (reconnectCount < maxReconnectAttempts) {
                        reconnectCount++
                        Log.i("RTSP", "Reconnecting... ($reconnectCount/$maxReconnectAttempts)")
                        client.connect(rtspUrl)
                    } else {
                        Log.e("RTSP", "Max reconnect attempts reached")
                    }
                }
                RtspStatus.CONNECTED -> {
                    reconnectCount = 0 // Сброс счетчика при успехе
                    Log.i("RTSP", "Connected successfully")
                }
                RtspStatus.DISCONNECTED -> {
                    Log.i("RTSP", "Disconnected")
                }
            }
        }
    }
    
    fun start() {
        client.connect(rtspUrl)
    }
    
    fun stop() {
        client.disconnect()
    }
}
```

### 3. Интеграция с VideoPlayer

```kotlin
class CameraVideoPlayer(
    private val rtspUrl: String
) {
    private val rtspClient = RtspClient()
    private val videoDecoder = H264Decoder()
    private val audioDecoder = AACDecoder()
    private val videoSurface: Surface? = null
    private val audioTrack: AudioTrack? = null
    
    fun initialize() {
        val config = RtspConfig(
            timeoutMs = 10000,
            enableReconnect = true
        )
        rtspClient.configure(config)
        
        rtspClient.videoCallback = { frame, _ ->
            if (frame.isKeyFrame) {
                // Инициализировать декодер
                val spsPps = extractSpsPps(frame.data)
                videoDecoder.init(spsPps.sps, spsPps.pps)
            }
            
            // Декодировать и показать
            val yuvData = videoDecoder.decode(frame.data)
            videoSurface?.let { surface ->
                renderToSurface(yuvData, surface)
            }
        }
        
        rtspClient.audioCallback = { frame, _ ->
            val pcmData = audioDecoder.decode(frame.data)
            audioTrack?.write(pcmData)
        }
        
        rtspClient.statusCallback = { status, message, _ ->
            when (status) {
                RtspStatus.CONNECTED -> {
                    rtspClient.play()
                }
                RtspStatus.ERROR -> {
                    // Показать ошибку пользователю
                    showError(message)
                }
            }
        }
    }
    
    fun startStream() {
        rtspClient.connect(rtspUrl)
    }
    
    fun stopStream() {
        rtspClient.disconnect()
    }
    
    fun pause() {
        rtspClient.pause()
    }
    
    fun resume() {
        rtspClient.play()
    }
}
```

### 4. Запись потока в файл

```kotlin
class RtspStreamRecorder(
    private val rtspUrl: String,
    private val outputPath: String
) {
    private val rtspClient = RtspClient()
    private val videoFile = FileOutputStream(outputPath)
    private val videoWriter = H264FileWriter(videoFile)
    
    fun startRecording() {
        rtspClient.videoCallback = { frame, _ ->
            if (frame.isVideo) {
                videoWriter.writeFrame(frame.data, frame.timestamp)
            }
        }
        
        rtspClient.connect(rtspUrl)
    }
    
    fun stopRecording() {
        rtspClient.disconnect()
        videoFile.close()
    }
}
```

---

## Error Handling

### Обработка ошибок подключения

```kotlin
try {
    val success = rtspClient.connect(rtspUrl)
    if (!success) {
        when {
            rtspUrl.contains("invalid") -> {
                // Неверный URL
                showError("Invalid RTSP URL")
            }
            else -> {
                // Общая ошибка подключения
                showError("Connection failed: ${rtspClient.lastErrorMessage}")
            }
        }
    }
} catch (e: Exception) {
    showError("Exception: ${e.message}")
}
```

### Обработка ошибок потока

```kotlin
rtspClient.statusCallback = { status, message, _ ->
    when (status) {
        RtspStatus.ERROR -> {
            when {
                message.contains("no stream is available") -> {
                    // Поток не опубликован
                    showWarning("Stream not available on server")
                }
                message.contains("authentication") -> {
                    // Ошибка аутентификации
                    showCredentialsDialog()
                }
                message.contains("timeout") -> {
                    // Таймаут соединения
                    showRetryButton()
                }
                else -> {
                    // Общая ошибка
                    showError("Stream error: $message")
                }
            }
        }
    }
}
```

---

## Performance Tips

### 1. Оптимизация использования памяти

```kotlin
// Используйте object pooling для фреймов
class FramePool {
    private val pool = ArrayDeque<RtspFrame>(capacity = 10)
    
    fun acquire(): RtspFrame {
        return pool.pollFirst() ?: RtspFrame()
    }
    
    fun release(frame: RtspFrame) {
        frame.clear()
        pool.offerLast(frame)
    }
}
```

### 2. Балансировка нагрузки

```kotlin
// Обрабатывайте фреймы в отдельном потоке
private val frameProcessingExecutor = Executors.newFixedThreadPool(2)

rtspClient.frameCallback = { frame, _ ->
    frameProcessingExecutor.submit {
        processFrame(frame)
    }
}
```

### 3. Мониторинг производительности

```kotlin
class RtspPerformanceMonitor(
    private val rtspClient: RtspClient
) {
    fun getMetrics(): RtspMetrics {
        val stats = rtspClient.statistics
        
        return RtspMetrics(
            fps = calculateFps(),
            latency = calculateLatency(),
            packetLossRate = stats.packetsLost.toFloat() / stats.packetsReceived,
            jitter = stats.jitter,
            bandwidth = stats.bytesReceived * 8 / 1000 // kbps
        )
    }
}
```

---

## Troubleshooting

### Проблема: "no stream is available"

**Решение:** Убедитесь что поток опубликован на RTSP сервере
```bash
# Проверка что поток доступен
ffplay rtsp://localhost:8554/test
```

### Проблема: "Connection timeout"

**Решение:** Проверьте сетевое подключение и Firewall
```powershell
# Проверка доступности порта
Test-NetConnection -ComputerName localhost -Port 554
```

### Проблема: "Authentication failed"

**Решение:** Проверьте credentials
```kotlin
val config = RtspConfig(
    username = "admin",
    password = "password123"
)
```

### Проблема: Высокая задержка

**Решение:** Уменьшите буферизацию
```kotlin
val config = RtspConfig(
    timeoutMs = 5000,  // Уменьшить таймаут
    enableReconnect = true
)
```

---

## Examples

### Полный пример: Camera Monitor App

```kotlin
class CameraMonitorApp {
    private val cameras = mutableListOf<Camera>()
    private val rtspClients = mutableMapOf<Camera, RtspClient>()
    
    data class Camera(
        val id: String,
        val name: String,
        val rtspUrl: String
    )
    
    fun addCamera(camera: Camera) {
        cameras.add(camera)
        
        val client = RtspClient()
        client.configure(RtspConfig(timeoutMs = 10000))
        
        client.statusCallback = { status, message, _ ->
            when (status) {
                RtspStatus.CONNECTED -> {
                    client.play()
                    cameraOnline(camera)
                }
                RtspStatus.ERROR -> {
                    cameraOffline(camera, message)
                }
            }
        }
        
        client.videoCallback = { frame, _ ->
            displayFrame(camera, frame)
        }
        
        rtspClients[camera] = client
        client.connect(camera.rtspUrl)
    }
    
    fun removeCamera(camera: Camera) {
        rtspClients[camera]?.disconnect()
        rtspClients.remove(camera)
        cameras.remove(camera)
    }
    
    fun startAllCameras() {
        cameras.forEach { camera ->
            rtspClients[camera]?.connect(camera.rtspUrl)
        }
    }
    
    fun stopAllCameras() {
        rtspClients.values.forEach { it.disconnect() }
    }
}
```

---

## API Summary

| Method | Description |
|--------|-------------|
| `configure(config)` | Настроить клиент |
| `connect(url)` | Подключиться к потоку |
| `disconnect()` | Отключиться от потока |
| `play()` | Запустить поток |
| `pause()` | Приостановить поток |
| `stop()` | Остановить поток |
| `isConnected()` | Проверить подключение |
| `isPlaying()` | Проверить статус потока |
| `getStreams()` | Получить список потоков |
| `getStatistics()` | Получить статистику |

---

**Создан:** 11 June 2026  
**Автор:** Koda AI Assistant  
**Версия:** 1.0
