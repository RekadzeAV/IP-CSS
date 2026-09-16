# RTSP Client - Quick Start Guide

**Версия:** 0.1.2-beta  
**Дата:** 11 June 2026  
**Статус:** ✅ **PRODUCTION READY**

---

## Быстрый старт (5 минут)

### 1. Подготовка окружения

**Установка MediaMTX (RTSP сервер):**
```powershell
docker run -d --name ip-camera-mediamtx --restart=always `
  -p 8554:8554 `
  -p 8000:8000 `
  -p 8001:8001 `
  -e RTSP_PROTOCOL=udp `
  iting1103/rtsp-simple-server:latest
```

**Публикация тестового потока:**
```powershell
# Вариант 1: Генерация тестового паттерна
ffmpeg -re -f lavfi -i testsrc=duration=60:size=1920x1080:rate=25 `
  -c:v libx264 -preset ultrafast -f rtsp `
  rtsp://localhost:8554/test

# Вариант 2: Публикация видеофайла
ffmpeg -re -i test.mp4 -c copy -f rtsp `
  rtsp://localhost:8554/test
```

### 2. Подключение RTSP client

**Базовое использование:**
```kotlin
import com.ipcss.core.network.rtsp.RtspClient
import com.ipcss.core.network.rtsp.RtspConfig

// Создаем клиент
val client = RtspClient()

// Настраиваем конфигурацию
val config = RtspConfig(
    username = "admin",
    password = "password123",
    timeoutMs = 10000
)
client.configure(config)

// Подключаемся
val success = client.connect("rtsp://192.168.1.100:554/test")
if (success) {
    println("Подключено успешно!")
    client.play()
}
```

### 3. Получение видео и аудио

**Обработка кадров:**
```kotlin
import com.ipcss.core.network.rtsp.RtspFrameType

client.statusCallback = { status, message, _ ->
    when (status) {
        RtspStatus.CONNECTED -> println("Подключено: $message")
        RtspStatus.STREAMING -> println("Стриминг...")
        RtspStatus.ERROR -> println("Ошибка: $message")
        RtspStatus.DISCONNECTED -> println("Отключено")
    }
}

client.videoCallback = { frame, _ ->
    when (frame.type) {
        RtspFrameType.VIDEO -> {
            // H.264/H.265 видео кадры
            val h264Data = frame.data
            // Отправить на декодер
            sendToVideoDecoder(h264Data)
        }
        RtspFrameType.AUDIO -> {
            // AAC/PCMU/PCMA аудио кадры
            val audioData = frame.data
            // Отправить на аудио декодер
            sendToAudioDecoder(audioData)
        }
    }
}

// Запуск стриминга
client.play()
```

---

## Примеры использования

### Пример 1: Desktop Application (Compose Desktop)

```kotlin
class CameraViewModel(cameraUrl: String) : ViewModel() {
    
    private val rtspClient = RtspClient()
    private val _videoFrames = MutableStateFlow<List<Bitmap>>(emptyList())
    val videoFrames: StateFlow<List<Bitmap>> = _videoFrames
    
    init {
        initCamera(cameraUrl)
    }
    
    private fun initCamera(url: String) {
        val config = RtspConfig(
            timeoutMs = 10000,
            enableReconnect = true,
            maxReconnectAttempts = 5
        )
        rtspClient.configure(config)
        
        rtspClient.statusCallback = { status, message, _ ->
            when (status) {
                RtspStatus.CONNECTED -> {
                    println("Камера подключена")
                    rtspClient.play()
                }
                RtspStatus.ERROR -> {
                    println("Ошибка камеры: $message")
                    // Показать уведомление пользователю
                }
            }
        }
        
        rtspClient.videoCallback = { frame, _ ->
            if (frame.type == RtspFrameType.VIDEO) {
                // Преобразовать H.264 в Bitmap
                val bitmap = decodeH264ToBitmap(frame.data)
                _videoFrames.value = _videoFrames.value + bitmap
            }
        }
        
        // Подключение
        viewModelScope.launch {
            rtspClient.connect(url)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        rtspClient.disconnect()
    }
}
```

### Пример 2: Обработка ошибок и reconnect

```kotlin
class RobustCameraClient(url: String) {
    
    private val rtspClient = RtspClient()
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    
    fun start() {
        val config = RtspConfig(
            timeoutMs = 15000,
            enableReconnect = true,
            reconnectDelayMs = 3000,
            maxReconnectAttempts = maxReconnectAttempts
        )
        rtspClient.configure(config)
        
        rtspClient.statusCallback = { status, message, _ ->
            when (status) {
                RtspStatus.CONNECTED -> {
                    reconnectAttempts = 0
                    rtspClient.play()
                }
                RtspStatus.ERROR -> {
                    reconnectAttempts++
                    
                    if (reconnectAttempts < maxReconnectAttempts) {
                        println("Попытка переподключения $reconnectAttempts/$maxReconnectAttempts")
                        // Автоматический reconnect встроен в клиент
                    } else {
                        println("Максимум попыток переподключения")
                        // Показать ошибку пользователю
                        showConnectionErrorDialog()
                    }
                }
                RtspStatus.DISCONNECTED -> {
                    println("Отключение от камеры")
                }
            }
        }
        
        rtspClient.connect(url)
    }
    
    fun stop() {
        rtspClient.disconnect()
    }
}
```

### Пример 3: Множественные камеры

```kotlin
class MultiCameraManager {
    
    private val cameras = mutableMapOf<String, RtspClient>()
    
    fun addCamera(id: String, url: String) {
        val client = RtspClient()
        
        val config = RtspConfig(
            timeoutMs = 10000,
            enableReconnect = true
        )
        client.configure(config)
        
        client.statusCallback = { status, message, _ ->
            println("Камера $id: $status - $message")
        }
        
        client.videoCallback = { frame, _ ->
            // Обрабатываем кадры для конкретной камеры
            processFrame(id, frame)
        }
        
        cameras[id] = client
        client.connect(url)
    }
    
    fun removeCamera(id: String) {
        cameras[id]?.disconnect()
        cameras.remove(id)
    }
    
    fun pauseCamera(id: String) {
        cameras[id]?.pause()
    }
    
    fun resumeCamera(id: String) {
        cameras[id]?.play()
    }
    
    fun stopAll() {
        cameras.values.forEach { it.disconnect() }
        cameras.clear()
    }
}
```

---

## Конфигурация

### RtspConfig параметры

```kotlin
data class RtspConfig(
    // Аутентификация
    val username: String? = null,
    val password: String? = null,
    
    // Таймауты
    val timeoutMs: Long = 10000L,
    
    // Reconnect
    val enableReconnect: Boolean = true,
    val reconnectDelayMs: Long = 3000L,
    val maxReconnectAttempts: Int = 5,
    
    // Настройки кодеков (опционально)
    val enableAudio: Boolean = true,
    val enableVideo: Boolean = true
)
```

### Рекомендуемые конфигурации

**Для локальной сети (стабильное подключение):**
```kotlin
val localNetworkConfig = RtspConfig(
    timeoutMs = 5000,
    enableReconnect = true,
    reconnectDelayMs = 1000,
    maxReconnectAttempts = 10
)
```

**Для нестабильных сетей:**
```kotlin
val unstableNetworkConfig = RtspConfig(
    timeoutMs = 15000,
    enableReconnect = true,
    reconnectDelayMs = 5000,
    maxReconnectAttempts = 20
)
```

**Для мобильного интернета:**
```kotlin
val mobileNetworkConfig = RtspConfig(
    timeoutMs = 20000,
    enableReconnect = true,
    reconnectDelayMs = 10000,
    maxReconnectAttempts = 10
)
```

---

## Troubleshooting

### Проблема: "Connection timeout"

**Причина:** Камера недоступна или firewall блокирует

**Решение:**
```powershell
# Проверка доступности камеры
Test-NetConnection -ComputerName 192.168.1.100 -Port 554

# Проверка RTSP URL
ffplay rtsp://192.168.1.100:554/test
```

### Проблема: "Authentication failed"

**Причина:** Неправильные credentials

**Решение:**
```kotlin
val config = RtspConfig(
    username = "admin",
    password = "correct_password"
)
client.configure(config)
```

### Проблема: "No stream available"

**Причина:** Поток не опубликован на сервере

**Решение:**
```powershell
# Проверка MediaMTX логов
docker logs ip-camera-mediamtx --tail 20

# Публикация потока
ffmpeg -re -f lavfi -i testsrc -c:v libx264 -f rtsp rtsp://localhost:8554/test
```

### Проблема: Высокая задержка

**Причина:** Большое время буферизации

**Решение:**
```kotlin
val config = RtspConfig(
    timeoutMs = 3000,  // Уменьшить таймаут
    enableReconnect = true
)
```

---

## Поддержка

**Документация:**
- Full Documentation *(утерян/в архиве)*
- [Deployment Guide](../../archive/docs/deployment/DEPLOYMENT_PHASE2_2026-06-11.md)
- [Code Review Report](../reports/CODE_REVIEW_PHASE2_2026-06-11.md)
- [Changelog](../../_to_be_archived/ROOT_FILES_2026-06-21/CHANGELOG_PHASE2.md)

**Инструменты:**
- test_rtsp_integration.ps1 *(утерян/в архиве)* - Автоматизированное тестирование
- tools/rtsp_monitor.ps1 *(утерян/в архиве)* - Мониторинг

**Issues:** https://github.com/RekadzeAV/IP-CSS/issues

---

**Создан:** 11 June 2026  
**Автор:** Koda AI Assistant  
**Версия:** 0.1.2-beta
