# Руководство по использованию WebSocket Client

**Версия:** 2.0
**Дата:** 26 January 2026

---

## 📋 Содержание

1. [Введение](#введение)
2. [Быстрый старт](#быстрый-старт)
3. [Обработка бинарных сообщений](#обработка-бинарных-сообщений)
4. [Очередь сообщений с приоритетами](#очередь-сообщений-с-приоритетами)
5. [Rate Limiting](#rate-limiting)
6. [Метрики и мониторинг](#метрики-и-мониторинг)
7. [Примеры использования](#примеры-использования)

---

## Введение

WebSocket Client предоставляет расширенные возможности для real-time коммуникации:

- ✅ **Бинарные сообщения** - поддержка изображений, видео, файлов
- ✅ **Приоритетная очередь** - управление порядком отправки сообщений
- ✅ **Rate Limiting** - защита от перегрузки сервера
- ✅ **Chunking** - автоматическое разбиение больших сообщений
- ✅ **Метрики** - мониторинг производительности

---

## Быстрый старт

### Базовая настройка

```kotlin
import com.company.ipcamera.core.network.*

val engine = HttpClientEngineFactory.create()
val config = WebSocketClientConfig(
    url = "wss://api.example.com/ws",
    autoReconnect = true,
    queueMaxSize = 100,
    rateLimitConfig = RateLimitConfig(
        messagesPerSecond = 100,
        enabled = true
    )
)

val wsClient = WebSocketClient(engine, config)
```

### Подключение и обработка событий

```kotlin
class MyEventHandler : WebSocketEventHandler {
    override fun onConnected() {
        println("Connected!")
    }

    override fun onMessage(message: WebSocketMessage) {
        when (message) {
            is WebSocketMessage.EventMessage -> handleEvent(message)
            is WebSocketMessage.ImageMessage -> handleImage(message)
            else -> {}
        }
    }

    override fun onRateLimitExceeded(operationType: String, limitType: String) {
        println("Rate limit: $operationType/$limitType")
    }
}

wsClient.addEventHandler(MyEventHandler())
wsClient.connect(token = "your-token")
```

---

## Обработка бинарных сообщений

### Типы бинарных сообщений

```kotlin
// Изображение (JPEG, PNG, WebP)
val imageMessage = WebSocketMessage.ImageMessage(
    data = imageBytes,
    metadata = BinaryMessageMetadata(
        type = BinaryMessageType.IMAGE.name,
        mimeType = BinaryMimeTypes.JPEG,
        size = imageBytes.size.toLong(),
        messageId = UUID.randomUUID().toString()
    )
)

// Видео фрагмент (H.264, H.265)
val videoMessage = WebSocketMessage.VideoChunkMessage(
    data = videoBytes,
    metadata = BinaryMessageMetadata(...)
)

// Файл
val fileMessage = WebSocketMessage.FileMessage(
    data = fileBytes,
    metadata = BinaryMessageMetadata(...),
    fileName = "document.pdf"
)
```

### Автоматический chunking

Сообщения больше 64KB автоматически разбиваются на chunks:

```kotlin
// Большое изображение (>64KB) будет автоматически разбито
val largeImage = ByteArray(100 * 1024) // 100KB
val message = WebSocketMessage.ImageMessage(largeImage, metadata)
wsClient.sendMessage(message) // Автоматически разобьется на chunks
```

---

## Очередь сообщений с приоритетами

### Приоритеты

```kotlin
enum class MessagePriority {
    CRITICAL,  // Аутентификация, ошибки (обходят rate limiting)
    HIGH,      // Подписки, события
    NORMAL,    // Обычные сообщения
    LOW        // Статистика, heartbeat
}
```

### Использование приоритетов

```kotlin
// Критическое сообщение (отправляется первым)
wsClient.sendMessage(
    WebSocketMessage.AuthMessage(token),
    MessagePriority.CRITICAL
)

// Высокий приоритет
wsClient.sendMessage(
    WebSocketMessage.SubscribeMessage(channels),
    MessagePriority.HIGH
)

// Обычный приоритет
wsClient.sendMessage(
    WebSocketMessage.EventMessage(...),
    MessagePriority.NORMAL
)
```

### Стратегии переполнения

```kotlin
val config = WebSocketClientConfig(
    queueMaxSize = 100,
    queueOverflowStrategy = QueueOverflowStrategy.DROP_LOWEST
    // DROP_OLDEST - удалять старые сообщения
    // DROP_LOWEST - удалять сообщения с низким приоритетом
    // REJECT - отклонять новые сообщения
    // BLOCK - блокировать до освобождения места
)
```

---

## Rate Limiting

### Настройка лимитов

```kotlin
val rateLimitConfig = RateLimitConfig(
    messagesPerSecond = 100,      // Лимит сообщений
    subscriptionsPerSecond = 10,   // Лимит подписок
    bytesPerSecond = 10 * 1024 * 1024, // 10MB/сек
    enabled = true
)

val config = WebSocketClientConfig(
    rateLimitConfig = rateLimitConfig
)
```

### Поведение при превышении лимита

При превышении лимита сообщения автоматически добавляются в очередь:

```kotlin
// Если лимит превышен, сообщение будет добавлено в очередь
wsClient.sendMessage(message) // Автоматически обработается rate limiting
```

### CRITICAL приоритет обходит лимиты

```kotlin
// CRITICAL сообщения всегда отправляются, даже при превышении лимита
wsClient.sendMessage(
    WebSocketMessage.AuthMessage(token),
    MessagePriority.CRITICAL // Обходит rate limiting
)
```

---

## Метрики и мониторинг

### Метрики очереди

```kotlin
val metrics = wsClient.getQueueMetrics()

println("Queue size: ${metrics.currentSize}/${metrics.maxSize}")
println("Average wait time: ${metrics.averageWaitTime}ms")
println("Dropped messages: ${metrics.droppedMessages}")
println("Messages by priority:")
metrics.messagesByPriority.forEach { (priority, count) ->
    println("  $priority: $count")
}
```

### Метрики rate limiting

```kotlin
val rateMetrics = wsClient.getRateLimitMetrics()

println("Blocked requests: ${rateMetrics.blockedRequests}")
println("Current message rate: ${rateMetrics.currentMessageRate} msg/s")
println("Current subscription rate: ${rateMetrics.currentSubscriptionRate} ops/s")
println("Current bytes rate: ${rateMetrics.currentBytesRate} bytes/s")
println("Violations: ${rateMetrics.violations.size}")
```

---

## Примеры использования

### Полный пример

```kotlin
class WebSocketManager {
    private val wsClient: WebSocketClient

    init {
        val engine = HttpClientEngineFactory.create()
        val config = WebSocketClientConfig(
            url = "wss://api.example.com/ws",
            autoReconnect = true,
            queueMaxSize = 200,
            queueOverflowStrategy = QueueOverflowStrategy.DROP_LOWEST,
            rateLimitConfig = RateLimitConfig(
                messagesPerSecond = 100,
                subscriptionsPerSecond = 10,
                bytesPerSecond = 10 * 1024 * 1024,
                enabled = true
            )
        )

        wsClient = WebSocketClient(engine, config)
        wsClient.addEventHandler(MyEventHandler())
    }

    suspend fun connect(token: String) {
        wsClient.connect(token)
    }

    suspend fun subscribeToCameraEvents(cameraId: String) {
        wsClient.subscribe(
            channels = listOf("camera_events"),
            filters = mapOf("camera_id" to cameraId)
        )
    }

    suspend fun sendImage(imageBytes: ByteArray) {
        val metadata = BinaryMessageMetadata(
            type = BinaryMessageType.IMAGE.name,
            mimeType = BinaryMimeTypes.JPEG,
            size = imageBytes.size.toLong(),
            messageId = UUID.randomUUID().toString()
        )
        val message = WebSocketMessage.ImageMessage(imageBytes, metadata)
        wsClient.sendMessage(message, MessagePriority.HIGH)
    }

    fun getMetrics(): Pair<QueueMetrics, RateLimitMetrics> {
        return wsClient.getQueueMetrics() to wsClient.getRateLimitMetrics()
    }
}
```

---

## Миграция с предыдущей версии

### Изменения API

1. **Новый параметр priority** в `sendMessage()`:
   ```kotlin
   // Старый код
   wsClient.sendMessage(message)

   // Новый код (обратная совместимость сохранена)
   wsClient.sendMessage(message, MessagePriority.NORMAL)
   ```

2. **Новые методы в WebSocketEventHandler**:
   ```kotlin
   // Опциональные методы (имеют реализации по умолчанию)
   override fun onRateLimitExceeded(operationType: String, limitType: String) {}
   override fun onQueueOverflow(strategy: QueueOverflowStrategy, droppedCount: Long) {}
   ```

3. **Новые типы сообщений**:
   - `WebSocketMessage.ImageMessage`
   - `WebSocketMessage.VideoChunkMessage`
   - `WebSocketMessage.FileMessage`
   - `WebSocketMessage.CustomBinaryMessage`

---

**Последнее обновление:** 26 January 2026
