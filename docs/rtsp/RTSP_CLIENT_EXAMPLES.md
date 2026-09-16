# Примеры использования RTSP клиента

**Дата создания:** 26 January 2026
**Версия:** 1.0

## Базовое использование

### Простое подключение и воспроизведение

```kotlin
import com.company.ipcamera.core.network.*
import kotlinx.coroutines.*

suspend fun basicExample() {
    // Создание конфигурации
    val config = RtspClientConfig(
        url = "rtsp://192.168.1.100:554/stream1",
        username = "admin",
        password = "password123"
    )

    // Создание клиента
    val client = RtspClient(config)

    // Подключение
    client.connect()

    // Ожидание подключения
    client.getStatus().collect { status ->
        when (status) {
            RtspClientStatus.CONNECTED -> {
                println("Connected!")
                // Начать воспроизведение
                client.play()
            }
            RtspClientStatus.ERROR -> {
                println("Connection failed")
            }
            else -> {}
        }
    }

    // Получение видеокадров
    client.getVideoFrames().collect { frame ->
        println("Received frame: ${frame.width}x${frame.height}, size: ${frame.data.size}")
        // Обработка кадра
    }
}
```

## Использование с автоматическим переподключением

```kotlin
suspend fun reconnectExample() {
    val config = RtspClientConfig(
        url = "rtsp://192.168.1.100:554/stream1",
        username = "admin",
        password = "password123",
        reconnectConfig = RtspReconnectConfig(
            enabled = true,
            maxRetries = 10, // Максимум 10 попыток (0 = бесконечно)
            initialDelayMs = 2000, // Начальная задержка 2 секунды
            maxDelayMs = 60000, // Максимальная задержка 60 секунд
            backoffMultiplier = 1.5f // Множитель для экспоненциальной задержки
        )
    )

    val client = RtspClient(config)

    // Установка callback для отслеживания статуса
    client.setStatusCallback { status, message ->
        when (status) {
            RtspClientStatus.CONNECTING -> {
                println("Connecting... $message")
            }
            RtspClientStatus.CONNECTED -> {
                println("Connected: $message")
            }
            RtspClientStatus.PLAYING -> {
                println("Playing: $message")
            }
            RtspClientStatus.ERROR -> {
                println("Error: $message")
            }
            RtspClientStatus.DISCONNECTED -> {
                println("Disconnected: $message")
            }
        }
    }

    client.connect()
    client.play()

    // Клиент автоматически переподключится при ошибках
}
```

## Обработка видеокадров

```kotlin
suspend fun videoFrameExample() {
    val config = RtspClientConfig(
        url = "rtsp://192.168.1.100:554/stream1"
    )

    val client = RtspClient(config)

    // Установка callback для видеокадров
    client.setVideoFrameCallback { frame ->
        println("Video frame received:")
        println("  Size: ${frame.width}x${frame.height}")
        println("  Data size: ${frame.data.size} bytes")
        println("  Timestamp: ${frame.timestamp}")
        println("  Stream type: ${frame.streamType}")

        // Обработка кадра (например, декодирование, отображение и т.д.)
        processVideoFrame(frame.data)
    }

    client.connect()
    client.play()

    // Или использование Flow
    client.getVideoFrames().collect { frame ->
        processVideoFrame(frame.data)
    }
}

fun processVideoFrame(data: ByteArray) {
    // Обработка данных кадра
    // Например, декодирование H.264/H.265, отображение и т.д.
}
```

## Обработка аудиокадров

```kotlin
suspend fun audioFrameExample() {
    val config = RtspClientConfig(
        url = "rtsp://192.168.1.100:554/stream1",
        enableAudio = true
    )

    val client = RtspClient(config)

    // Установка callback для аудиокадров
    client.setAudioFrameCallback { frame ->
        println("Audio frame received:")
        println("  Data size: ${frame.data.size} bytes")
        println("  Timestamp: ${frame.timestamp}")

        // Определение кодека
        val codec = client.detectAudioCodec(frame.data)
        println("  Detected codec: $codec")

        // Обработка аудиокадра
        processAudioFrame(frame.data, codec)
    }

    client.connect()
    client.play()
}

fun processAudioFrame(data: ByteArray, codec: String?) {
    when (codec) {
        "AAC" -> {
            // Обработка AAC
        }
        "PCMU", "PCMA" -> {
            // Обработка G.711
        }
        else -> {
            // Обработка других кодеков
        }
    }
}
```

## Получение информации о потоках

```kotlin
suspend fun streamInfoExample() {
    val config = RtspClientConfig(
        url = "rtsp://192.168.1.100:554/stream1"
    )

    val client = RtspClient(config)

    client.connect()

    // Получение списка потоков
    val streams = client.getStreams()
    println("Available streams: ${streams.size}")

    streams.forEach { stream ->
        println("Stream ${stream.index}:")
        println("  Type: ${stream.type}")
        println("  Codec: ${stream.codec}")

        when (stream.type) {
            RtspStreamType.VIDEO -> {
                stream.resolution?.let { resolution ->
                    println("  Resolution: ${resolution.width}x${resolution.height}")
                }
                println("  FPS: ${stream.fps}")
            }
            RtspStreamType.AUDIO -> {
                println("  Audio codec: ${stream.audioCodec}")
                stream.sampleRate?.let { println("  Sample rate: $it Hz") }
                stream.channels?.let { println("  Channels: $it") }
            }
            RtspStreamType.METADATA -> {
                println("  Metadata stream")
            }
        }
    }
}
```

## Управление воспроизведением

```kotlin
suspend fun playbackControlExample() {
    val config = RtspClientConfig(
        url = "rtsp://192.168.1.100:554/stream1"
    )

    val client = RtspClient(config)

    client.connect()

    // Начать воспроизведение
    client.play()

    delay(10000) // Воспроизведение 10 секунд

    // Приостановить воспроизведение
    client.pause()

    delay(5000) // Пауза 5 секунд

    // Возобновить воспроизведение
    client.play()

    delay(10000) // Еще 10 секунд

    // Остановить воспроизведение
    client.stop()

    // Отключиться
    client.disconnect()
}
```

## Использование с корутинами

```kotlin
class RtspStreamManager {
    private var client: RtspClient? = null
    private var frameJob: Job? = null

    suspend fun startStreaming(url: String) {
        val config = RtspClientConfig(
            url = url,
            reconnectConfig = RtspReconnectConfig(enabled = true)
        )

        client = RtspClient(config)

        // Запуск обработки кадров в отдельной корутине
        frameJob = CoroutineScope(Dispatchers.Default).launch {
            client?.getVideoFrames()?.collect { frame ->
                processFrame(frame)
            }
        }

        client?.connect()
        client?.play()
    }

    suspend fun stopStreaming() {
        frameJob?.cancel()
        client?.stop()
        client?.disconnect()
        client?.close()
        client = null
    }

    private fun processFrame(frame: RtspFrame) {
        // Обработка кадра
    }
}
```

## Обработка ошибок

```kotlin
suspend fun errorHandlingExample() {
    val config = RtspClientConfig(
        url = "rtsp://invalid-url:554/stream"
    )

    val client = RtspClient(config)

    // Установка callback для обработки ошибок
    client.setStatusCallback { status, message ->
        when (status) {
            RtspClientStatus.ERROR -> {
                println("Error occurred: $message")
                // Обработка ошибки
                handleError(message)
            }
            else -> {}
        }
    }

    try {
        client.connect()
        client.play()
    } catch (e: Exception) {
        println("Exception: ${e.message}")
        e.printStackTrace()
    }
}

fun handleError(message: String?) {
    when {
        message?.contains("Invalid RTSP URL") == true -> {
            println("Invalid URL format")
        }
        message?.contains("Failed to connect") == true -> {
            println("Connection failed - check network and credentials")
        }
        message?.contains("Authentication failed") == true -> {
            println("Invalid credentials")
        }
        else -> {
            println("Unknown error: $message")
        }
    }
}
```

## Использование в Android Activity/Fragment

```kotlin
class CameraViewActivity : AppCompatActivity() {
    private var rtspClient: RtspClient? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra("RTSP_URL") ?: return

        val config = RtspClientConfig(
            url = url,
            reconnectConfig = RtspReconnectConfig(enabled = true)
        )

        rtspClient = RtspClient(config)

        // Установка callbacks
        rtspClient?.setVideoFrameCallback { frame ->
            // Обновление UI на главном потоке
            runOnUiThread {
                updateVideoView(frame.data)
            }
        }

        rtspClient?.setStatusCallback { status, message ->
            runOnUiThread {
                updateStatus(status, message)
            }
        }

        // Подключение и воспроизведение
        scope.launch {
            rtspClient?.connect()
            rtspClient?.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.launch {
            rtspClient?.disconnect()
            rtspClient?.close()
        }
        scope.cancel()
    }

    private fun updateVideoView(data: ByteArray) {
        // Обновление видеоплеера
    }

    private fun updateStatus(status: RtspClientStatus, message: String?) {
        // Обновление статуса в UI
    }
}
```

## Множественные потоки

```kotlin
suspend fun multipleStreamsExample() {
    val streams = listOf(
        "rtsp://192.168.1.100:554/stream1",
        "rtsp://192.168.1.101:554/stream1",
        "rtsp://192.168.1.102:554/stream1"
    )

    val clients = streams.map { url ->
        val config = RtspClientConfig(
            url = url,
            reconnectConfig = RtspReconnectConfig(enabled = true)
        )
        RtspClient(config)
    }

    // Подключение всех клиентов
    clients.forEach { client ->
        client.connect()
        client.play()
    }

    // Обработка кадров от всех клиентов
    clients.forEachIndexed { index, client ->
        CoroutineScope(Dispatchers.Default).launch {
            client.getVideoFrames().collect { frame ->
                println("Stream $index: frame ${frame.width}x${frame.height}")
            }
        }
    }
}
```

---

**Связанные документы:**
- [RTSP_CLIENT.md](../../archive/docs-duplicates-2026-08-08/RTSP_CLIENT.md) - Полная документация RTSP клиента
- [RTSP_NATIVE_INTEGRATION.md](RTSP_NATIVE_INTEGRATION.md) - Интеграция с нативной библиотекой
