# Использование fallback механизма RtspClient

**Версия:** 1.0  
**Дата:** 2026-05-25  
**Статус:** ✅ Готово к использованию

---

## 📖 Описание

RtspClient имеет встроенный fallback механизм для работы в режиме разработки и тестирования без:
- Реальных RTSP камер
- Нативной библиотеки (FFmpeg)
- FFI биндингов (Kotlin/Native)

В fallback режиме клиент эмулирует:
- Успешное подключение к серверу
- Видео поток (H.264, 1920x1080, 25 FPS)
- Аудио поток (AAC, 48kHz, 2 канала)
- Callback вызовы для кадров

---

## 🚀 Быстрый старт

### 1. Включение fallback режима

```kotlin
val config = RtspClientConfig(
    url = "rtsp://192.168.1.100:554/stream",
    username = "admin",
    password = "password",
    allowSimulatedFallback = true,  // Включить fallback
    enableVideo = true,
    enableAudio = true,
    timeoutMillis = 10000
)

val client = RtspClient(config)
```

### 2. Подключение и воспроизведение

```kotlin
// Подключение
client.connect()

// Ждем подключения (StateFlow)
client.getStatus().collect { status ->
    when (status) {
        RtspClientStatus.CONNECTING -> println("Подключение...")
        RtspClientStatus.CONNECTED -> println("Подключено")
        RtspClientStatus.PLAYING -> println("Воспроизведение")
        RtspClientStatus.ERROR -> println("Ошибка: $status")
        else -> {}
    }
}

// Воспроизведение
client.play()

// Получение кадров
client.getVideoFrames().collect { frame ->
    println("Получен кадр: ${frame.width}x${frame.height}")
}

// Остановка
client.stop()
client.disconnect()
```

---

## 📊 Что эмулируется в fallback режиме

### Видео поток:
- **Кодек:** H.264
- **Разрешение:** 1920x1080
- **FPS:** 25
- **Размер кадра:** ~100 байт (mock data)
- **Timestamp:** текущее время в мс

### Аудио поток:
- **Кодек:** AAC
- **Частота:** 48000 Hz
- **Каналы:** 2 (стерео)
- **Размер кадра:** 1024 байт (mock data)

### Статусы:
```
DISCONNECTED → CONNECTING → CONNECTED → PLAYING
                              ↓
                        DISCONNECTED
```

---

## 🎯 Сценарии использования

### 1. Разработка UI без камер

```kotlin
class CameraViewModel(
    private val cameraId: String
) : ViewModel() {
    
    private val client = RtspClient(
        RtspClientConfig(
            url = "rtsp://mock-camera/stream",
            allowSimulatedFallback = true  // Для разработки
        )
    )
    
    val videoFrames = MutableStateFlow<ByteArray?>(null)
    val connectionStatus = MutableStateFlow<RtspClientStatus>(RtspClientStatus.DISCONNECTED)
    
    fun connect() {
        viewModelScope.launch {
            client.connect()
            
            client.getStatus().collect { status ->
                connectionStatus.value = status
            }
            
            client.getVideoFrames().collect { frame ->
                videoFrames.value = frame.data
            }
            
            client.play()
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            client.stop()
            client.disconnect()
        }
    }
}
```

### 2. Тестирование компонент

```kotlin
@Test
fun `test video player with fallback stream`() = runTest {
    val client = RtspClient(
        RtspClientConfig(
            url = "rtsp://test/stream",
            allowSimulatedFallback = true
        )
    )
    
    client.connect()
    delay(100) // Ждем подключения
    
    assertEquals(RtspClientStatus.CONNECTED, client.getStatus().value)
    
    client.play()
    delay(100) // Ждем несколько кадров
    
    val frames = mutableListOf<RtspFrame>()
    client.getVideoFrames().take(5).collect { frame ->
        frames.add(frame)
    }
    
    assertTrue(frames.size >= 2, "Ожидалось минимум 2 кадра")
    assertEquals(1920, frames.first().width)
    assertEquals(1080, frames.first().height)
}
```

### 3. Benchmark тестирование

```kotlin
val runner = SimpleRtspBenchmarkRunner()

// Бенчмарк с fallback
val result = runner.runBenchmark(
    rtspUrl = "rtsp://mock/stream",
    duration = 30.seconds,
    config = RtspBenchmarkConfig(
        rtspUrl = "rtsp://mock/stream",
        expectedFps = 25.0,  // Ожидаемый FPS
        allowSimulatedFallback = true
    )
)

println("Средний FPS: ${result.summary.avgFps}")
println("CPU использование: ${result.summary.avgCpuUsage}%")
println("Память: ${result.summary.avgMemoryUsageBytes / 1024 / 1024} MB")
```

### 4. Интеграция с CamerasViewModel

```kotlin
class CamerasViewModel(
    private val getCamerasUseCase: GetCamerasUseCase,
    // ... другие use cases
) : ViewModel() {
    
    private val cameraClients = mutableMapOf<String, RtspClient>()
    
    fun startCameraPreview(cameraId: String) {
        val camera = getCameraById(cameraId) ?: return
        
        val client = RtspClient(
            RtspClientConfig(
                url = camera.url,
                username = camera.username,
                password = camera.password,
                allowSimulatedFallback = BuildConfig.DEBUG,  // Только в debug
                timeoutMillis = 10000
            )
        )
        
        cameraClients[cameraId] = client
        
        viewModelScope.launch {
            client.connect()
            client.play()
            
            client.getVideoFrames().collect { frame ->
                // Обновить UI видео
                updateVideoFrame(cameraId, frame.data)
            }
        }
    }
    
    fun stopCameraPreview(cameraId: String) {
        cameraClients[cameraId]?.let { client ->
            viewModelScope.launch {
                client.stop()
                client.disconnect()
            }
            cameraClients.remove(cameraId)
        }
    }
}
```

---

## ⚙️ Конфигурация

### RtspClientConfig параметры:

```kotlin
data class RtspClientConfig(
    // Обязательные
    val url: String,
    
    // Опциональные
    val username: String? = null,
    val password: String? = null,
    
    // Fallback режим
    val allowSimulatedFallback: Boolean = false,
    
    // Потоки
    val enableVideo: Boolean = true,
    val enableAudio: Boolean = true,
    val enableMetadata: Boolean = false,
    
    // Таймауты
    val timeoutMillis: Long = 10000,
    val bufferSize: Int = 1024 * 1024,  // 1MB
    
    // Переподключение
    val reconnectEnabled: Boolean = true,
    val reconnectMaxRetries: Int = 5,
    val reconnectInitialDelayMs: Int = 500,
    val reconnectMaxDelayMs: Int = 10_000,
    val reconnectBackoffMultiplier: Float = 2.0f,
    val reconnectJitterRatio: Float = 0.15f
)
```

### Рекомендации по настройке:

| Сценарий | allowSimulatedFallback | enableVideo | enableAudio |
|----------|----------------------|-------------|-------------|
| Debug UI | `true` | `true` | `true` |
| Test | `true` | `true` | `false` |
| Benchmark | `true` | `true` | `true` |
| Production | `false` | `true` | `true` |
| Audio only | `false` | `false` | `true` |

---

## 🐛 Обработка ошибок

### Fallback не включен, native недоступен:

```kotlin
val client = RtspClient(
    RtspClientConfig(
        url = "rtsp://192.168.1.100/stream",
        allowSimulatedFallback = false  // Production mode
    )
)

client.connect()

client.getStatus().collect { status ->
    when (status) {
        RtspClientStatus.ERROR -> {
            // Native недоступен
            // Показать сообщение пользователю
            // Или предложить переключиться в fallback режим
        }
        else -> {}
    }
}
```

### Graceful degradation:

```kotlin
fun createClient(url: String, isDebug: Boolean): RtspClient {
    return RtspClient(
        RtspClientConfig(
            url = url,
            allowSimulatedFallback = isDebug,
            reconnectEnabled = true,
            reconnectMaxRetries = 3
        )
    )
}

// Использование
val client = createClient(
    url = camera.url,
    isDebug = BuildConfig.DEBUG
)
```

---

## 📈 Мониторинг и диагностика

### Runtime diagnostics:

```kotlin
val diagnostics = client.getRuntimeDiagnosticsSnapshot()

println("Connect attempts: ${diagnostics.connectAttempts}")
println("Connect successes: ${diagnostics.connectSuccesses}")
println("Connect failures: ${diagnostics.connectFailures}")
println("Last error: ${diagnostics.lastError}")
println("Last frame at: ${diagnostics.lastFrameAt}")
```

### StateFlow статусов:

```kotlin
// Статус подключения
client.getStatus()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), RtspClientStatus.DISCONNECTED)

// Runtime diagnostics
client.getRuntimeDiagnostics()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), RtspRuntimeDiagnostics())

// Видео кадры
client.getVideoFrames()
    .onEach { frame ->
        // Обработка кадра
    }
    .catch { e ->
        // Обработка ошибки
    }
```

---

## 🔄 Переключение режимов

### Runtime переключение:

```kotlin
class CameraManager {
    private var useFallback = BuildConfig.DEBUG
    
    fun setFallbackMode(enabled: Boolean) {
        useFallback = enabled
        // Пересоздать клиентов с новым режимом
        restartAllClients()
    }
    
    private fun restartAllClients() {
        // Остановить текущие клиенты
        // Создать новые с новым режимом
    }
}
```

### Conditional creation:

```kotlin
fun createRtspClient(camera: Camera, environment: Environment): RtspClient {
    val fallback = when (environment) {
        Environment.DEVELOPMENT -> true
        Environment.TEST -> true
        Environment.PRODUCTION -> false
    }
    
    return RtspClient(
        RtspClientConfig(
            url = camera.url,
            username = camera.username,
            password = camera.password,
            allowSimulatedFallback = fallback
        )
    )
}
```

---

## 📝 Best Practices

### 1. Не включать fallback в production:

```kotlin
// ❌ Плохо
val config = RtspClientConfig(
    url = camera.url,
    allowSimulatedFallback = true  // Всегда true - опасно!
)

// ✅ Хорошо
val config = RtspClientConfig(
    url = camera.url,
    allowSimulatedFallback = BuildConfig.DEBUG
)
```

### 2. Обрабатывать ошибку fallback:

```kotlin
client.connect()

client.getStatus().collect { status ->
    if (status == RtspClientStatus.ERROR) {
        // Fallback не включен или native недоступен
        // Предложить пользователю проверить подключение
        showError("Не удалось подключиться к камере")
    }
}
```

### 3. Использовать timeout:

```kotlin
// Подключение с таймаутом
withTimeoutOrNull(10_000) {
    client.connect()
    client.play()
} ?: run {
    // Таймаут подключения
    showError("Таймаут подключения к камере")
}
```

### 4. Освобождать ресурсы:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    client.close()  // Важно!
}
```

---

## 🔗 Ссылки

- RtspClient API *(утерян/в архиве)*
- Benchmark Runner *(утерян/в архиве)*
- [Test Plan](../../docs/testing/RTSP_INTEGRATION_TEST_PLAN.md)
- [RTSP README](../../docs/rtsp/README.md)

---

**Поддерживается:** NLP-Core-Team  
**Последнее обновление:** 2026-05-25
