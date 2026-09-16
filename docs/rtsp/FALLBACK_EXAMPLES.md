# Примеры использования fallback режима

**Дата:** 2026-05-25  
**Версия:** 1.0

---

## 📖 Содержание

1. [Базовое использование](#базовое-использование)
2. [Интеграция с UI](#интеграция-с-ui)
3. [Тестирование](#тестирование)
4. [Множественные камеры](#множественные-камеры)
5. [Обработка ошибок](#обработка-ошибок)
6. [Производительность](#производительность)

---

## Базовое использование

### Простое подключение

```kotlin
import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus

// Создание клиента с fallback режимом
val config = RtspClientConfig(
    url = "rtsp://192.168.1.100:554/stream",
    allowSimulatedFallback = true  // Включить fallback
)

val client = RtspClient(config)

// Подключение
client.connect()

// Подписка на статус
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

// Получение видео кадров
client.getVideoFrames().collect { frame ->
    println("Получен кадр: ${frame.width}x${frame.height}")
}

// Остановка
client.stop()
client.disconnect()
client.close()
```

### Конфигурация с параметрами

```kotlin
val config = RtspClientConfig(
    url = "rtsp://192.168.1.100:554/stream",
    username = "admin",
    password = "password",
    allowSimulatedFallback = true,
    enableVideo = true,
    enableAudio = false,
    timeoutMillis = 10000,
    reconnectEnabled = true,
    reconnectMaxRetries = 5,
    reconnectInitialDelayMs = 500,
    reconnectMaxDelayMs = 10000
)

val client = RtspClient(config)
```

---

## Интеграция с UI

### В ViewModel

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus

class CameraViewModel(
    private val cameraUrl: String
) : ViewModel() {
    
    private val client = RtspClient(
        RtspClientConfig(
            url = cameraUrl,
            allowSimulatedFallback = BuildConfig.DEBUG
        )
    )
    
    private val _videoFrames = MutableStateFlow<ByteArray?>(null)
    val videoFrames: StateFlow<ByteArray?> = _videoFrames
    
    private val _connectionStatus = MutableStateFlow<RtspClientStatus>(RtspClientStatus.DISCONNECTED)
    val connectionStatus: StateFlow<RtspClientStatus> = _connectionStatus
    
    fun connect() {
        viewModelScope.launch {
            client.connect()
            
            // Подписка на статус
            client.getStatus().collect { status ->
                _connectionStatus.value = status
            }
            
            // Подписка на видео кадры
            client.getVideoFrames().collect { frame ->
                _videoFrames.value = frame.data
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
    
    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
```

### В Compose UI

```kotlin
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

@Composable
fun CameraView(viewModel: CameraViewModel) {
    val videoFrames by viewModel.videoFrames.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    
    // Подписка на обновления при старте
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.connect()
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STOPPED) {
            viewModel.disconnect()
        }
    }
    
    // Отображение
    when {
        connectionStatus == RtspClientStatus.PLAYING && videoFrames != null -> {
            // Отображение видео
            VideoPlayer(frame = videoFrames!!)
        }
        connectionStatus == RtspClientStatus.CONNECTING -> {
            CircularProgressIndicator()
        }
        connectionStatus == RtspClientStatus.ERROR -> {
            Text("Ошибка подключения")
        }
        else -> {
            Text("Ожидание...")
        }
    }
}
```

---

## Тестирование

### Unit тест

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus

class CameraViewModelTest {
    
    @Test
    fun `test camera connection with fallback`() {
        val viewModel = CameraViewModel("rtsp://test/stream")
        
        // Simulate connection
        viewModel.connect()
        
        // Verify initial state
        assertEquals(RtspClientStatus.CONNECTING, viewModel.connectionStatus.value)
        
        // Cleanup
        viewModel.disconnect()
    }
}
```

### Интеграционный тест

```kotlin
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig

class RtspClientIntegrationTest {
    
    @Test
    fun `test fallback stream receives frames`() = runTest {
        val client = RtspClient(
            RtspClientConfig(
                url = "rtsp://test/stream",
                allowSimulatedFallback = true
            )
        )
        
        client.connect()
        client.play()
        
        var frameCount = 0
        
        // Collect frames for 500ms
        val job = launch {
            client.getVideoFrames().collect { _ ->
                frameCount++
            }
        }
        
        delay(500)
        job.cancel()
        
        assertTrue(frameCount > 0, "Should receive at least one frame")
        
        client.stop()
        client.disconnect()
        client.close()
    }
}
```

---

## Множественные камеры

### Управление несколькими клиентами

```kotlin
class MultiCameraManager {
    private val clients = mutableMapOf<String, RtspClient>()
    
    fun startCamera(cameraId: String, url: String) {
        val client = RtspClient(
            RtspClientConfig(
                url = url,
                allowSimulatedFallback = true
            )
        )
        
        clients[cameraId] = client
        
        // Подключение
        viewModelScope.launch {
            client.connect()
            client.play()
        }
    }
    
    fun stopCamera(cameraId: String) {
        clients[cameraId]?.let { client ->
            viewModelScope.launch {
                client.stop()
                client.disconnect()
            }
            clients.remove(cameraId)
        }
    }
    
    fun stopAllCameras() {
        clients.forEach { (_, client) ->
            viewModelScope.launch {
                client.stop()
                client.disconnect()
            }
        }
        clients.clear()
    }
    
    fun getCameraClient(cameraId: String): RtspClient? {
        return clients[cameraId]
    }
}
```

### GridLayout с несколькими камерами

```kotlin
@Composable
fun MultiCameraGrid(cameras: List<CameraViewModel>) {
    val gridSize = when {
        cameras.size == 1 -> 1
        cameras.size <= 4 -> 2
        cameras.size <= 9 -> 3
        else -> 4
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridSize),
        modifier = Modifier.fillMaxSize()
    ) {
        items(cameras.size) { index ->
            CameraView(viewModel = cameras[index])
        }
    }
}
```

---

## Обработка ошибок

### Graceful degradation

```kotlin
fun createRtspClient(
    url: String,
    isDebug: Boolean,
    username: String? = null,
    password: String? = null
): RtspClient {
    return RtspClient(
        RtspClientConfig(
            url = url,
            username = username,
            password = password,
            allowSimulatedFallback = isDebug,
            timeoutMillis = 10000,
            reconnectEnabled = true,
            reconnectMaxRetries = 3
        )
    )
}

// Использование
val client = createRtspClient(
    url = camera.url,
    isDebug = BuildConfig.DEBUG,
    username = camera.username,
    password = camera.password
)
```

### Обработка ошибок подключения

```kotlin
client.getStatus().collect { status ->
    when (status) {
        RtspClientStatus.ERROR -> {
            // Показать сообщение пользователю
            showError("Не удалось подключиться к камере")
            
            // Автоматическое переподключение
            viewModelScope.launch {
                delay(3000)
                client.connect()
            }
        }
        else -> {}
    }
}
```

### Timeout handling

```kotlin
import kotlinx.coroutines.withTimeoutOrNull

// Подключение с таймаутом
val connected = withTimeoutOrNull(10_000) {
    client.connect()
    client.getStatus().first { it == RtspClientStatus.CONNECTED }
}

if (connected == null) {
    // Таймаут подключения
    showError("Таймаут подключения к камере")
}
```

---

## Производительность

### Мониторинг FPS

```kotlin
class FpsMonitor {
    private var frameCount = 0
    private var lastFpsUpdate = System.currentTimeMillis()
    private var currentFps = 0.0
    
    fun onFrameReceived() {
        frameCount++
        
        val now = System.currentTimeMillis()
        if (now - lastFpsUpdate >= 1000) {
            currentFps = frameCount.toDouble() * 1000 / (now - lastFpsUpdate)
            frameCount = 0
            lastFpsUpdate = now
            
            println("Current FPS: $currentFps")
        }
    }
    
    fun getFps(): Double = currentFps
}

// Использование
val fpsMonitor = FpsMonitor()

client.getVideoFrames().collect { frame ->
    fpsMonitor.onFrameReceived()
    // Обработка кадра
}
```

### Memory monitoring

```kotlin
class MemoryMonitor {
    private val runtime = Runtime.getRuntime()
    
    fun getMemoryUsage(): Long {
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory
    }
    
    fun getMemoryUsageMB(): Int {
        return (getMemoryUsage() / (1024 * 1024)).toInt()
    }
    
    fun logMemoryUsage() {
        println("Memory usage: ${getMemoryUsageMB()} MB")
    }
}

// Использование
val memoryMonitor = MemoryMonitor()

// Периодическая проверка
timer(period = 5000) {
    memoryMonitor.logMemoryUsage()
}
```

---

## 📚 Дополнительные ресурсы

- [Fallback Mode Usage](../rtsp/FALLBACK_MODE_USAGE.md) - Полное руководство
- [Quick Start](../QUICKSTART_FALLBACK_MODE.md) - Быстрый старт
- API Reference *(утерян/в архиве)* - API документация

---

**Последнее обновление:** 2026-05-25 15:35  
**Поддерживается:** NLP-Core-Team
