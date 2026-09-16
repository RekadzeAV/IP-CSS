# WebSocket Client Documentation

**Версия проекта:** Alfa-0.0.1
**Последнее обновление:** 26 January 2026

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

## Обзор

WebSocket клиент обеспечивает real-time коммуникацию между клиентскими приложениями и сервером. Он поддерживает автоматическое переподключение, подписки на события и обработку различных типов сообщений.

## Использование

### Создание клиента

```kotlin
import com.company.ipcamera.core.network.*

val engine = HttpClientEngineFactory.create() // Платформо-специфичный engine
val config = WebSocketClientConfig(
    url = "wss://api.company.com/v1/ws",
    autoReconnect = true,
    reconnectDelayMillis = 5000,
    maxReconnectAttempts = 10,
    pingIntervalMillis = 30000,
    queueMaxSize = 100,
    queueOverflowStrategy = QueueOverflowStrategy.DROP_LOWEST,
    rateLimitConfig = RateLimitConfig(
        messagesPerSecond = 100,
        subscriptionsPerSecond = 10,
        bytesPerSecond = 10 * 1024 * 1024, // 10MB/сек
        enabled = true
    )
)

val wsClient = WebSocketClient(engine, config)
```

### Подключение

```kotlin
wsClient.connect(token = "jwt-token-here")

// Отслеживание состояния подключения
wsClient.getConnectionState().collect { state ->
    when (state) {
        WebSocketConnectionState.CONNECTED -> println("Connected")
        WebSocketConnectionState.DISCONNECTED -> println("Disconnected")
        WebSocketConnectionState.RECONNECTING -> println("Reconnecting...")
        WebSocketConnectionState.FAILED -> println("Connection failed")
        else -> {}
    }
}
```

### Подписка на события

```kotlin
wsClient.subscribe(
    channels = listOf("camera_events", "camera_status"),
    filters = mapOf("camera_ids" to listOf("cam-001", "cam-002"))
)
```

### Обработка сообщений

```kotlin
class MyEventHandler : WebSocketEventHandler {
    override fun onConnected() {
        println("WebSocket connected")
    }

    override fun onDisconnected(cause: Throwable?) {
        println("WebSocket disconnected: ${cause?.message}")
    }

    override fun onMessage(message: WebSocketMessage) {
        when (message) {
            is WebSocketMessage.EventMessage -> {
                println("Received event: ${message.type} on channel ${message.channel}")
                println("Data: ${message.data}")
            }
            is WebSocketMessage.ImageMessage -> {
                println("Received image: ${message.metadata.mimeType}, size: ${message.metadata.size} bytes")
                // Обработка изображения
            }
            is WebSocketMessage.VideoChunkMessage -> {
                println("Received video chunk: ${message.metadata.mimeType}, size: ${message.metadata.size} bytes")
                // Обработка видео фрагмента
            }
            is WebSocketMessage.ErrorMessage -> {
                println("Error: ${message.error}")
            }
            else -> {}
        }
    }

    override fun onRateLimitExceeded(operationType: String, limitType: String) {
        println("Rate limit exceeded: $operationType, limit: $limitType")
    }

    override fun onQueueOverflow(strategy: QueueOverflowStrategy, droppedCount: Long) {
        println("Queue overflow: strategy=$strategy, dropped=$droppedCount messages")
    }

    override fun onError(error: Throwable) {
        println("Error: ${error.message}")
    }
}

val handler = MyEventHandler()
wsClient.addEventHandler(handler)
```

### Отключение

```kotlin
wsClient.disconnect()
// или
wsClient.close() // Полное закрытие с освобождением ресурсов
```

## API

### WebSocketClientConfig

Конфигурация WebSocket клиента:

- `url: String` - URL WebSocket сервера
- `autoReconnect: Boolean` - автоматическое переподключение (по умолчанию: true)
- `reconnectDelayMillis: Long` - задержка перед переподключением (по умолчанию: 5000)
- `maxReconnectAttempts: Int` - максимальное количество попыток (по умолчанию: Int.MAX_VALUE)
- `pingIntervalMillis: Long` - интервал ping сообщений (по умолчанию: 30000)
- `timeoutMillis: Long` - таймаут подключения (по умолчанию: 10000)
- `enableLogging: Boolean` - включить логирование (по умолчанию: true)

### Методы

- `connect(token: String?)` - подключиться к серверу
- `disconnect()` - отключиться от сервера
- `subscribe(channels: List<String>, filters: Map<String, Any>?)` - подписаться на каналы
- `unsubscribe(channels: List<String>)` - отписаться от каналов
- `sendMessage(message: WebSocketMessage)` - отправить сообщение
- `getConnectionState(): StateFlow<WebSocketConnectionState>` - получить состояние подключения
- `addEventHandler(handler: WebSocketEventHandler)` - добавить обработчик событий
- `removeEventHandler(handler: WebSocketEventHandler)` - удалить обработчик событий
- `close()` - закрыть клиент и освободить ресурсы

### Типы сообщений

#### AuthMessage
Аутентификация на сервере:
```kotlin
WebSocketMessage.AuthMessage(token = "jwt-token")
```

#### SubscribeMessage
Подписка на события:
```kotlin
WebSocketMessage.SubscribeMessage(
    channels = listOf("camera_events"),
    filters = mapOf("camera_ids" to listOf("cam-001"))
)
```

#### UnsubscribeMessage
Отписка от событий:
```kotlin
WebSocketMessage.UnsubscribeMessage(
    channels = listOf("camera_events")
)
```

#### EventMessage
Событие от сервера:
```kotlin
WebSocketMessage.EventMessage(
    type = "camera_status_changed",
    channel = "camera_status",
    data = mapOf("camera_id" to "cam-001", "status" to "online")
)
```

#### ErrorMessage
Ошибка от сервера:
```kotlin
WebSocketMessage.ErrorMessage(
    error = "Invalid subscription",
    code = "INVALID_REQUEST"
)
```

## Примеры использования

### Полная интеграция

```kotlin
class CameraMonitor {
    private val wsClient: WebSocketClient

    init {
        val engine = HttpClientEngineFactory.create()
        val config = WebSocketClientConfig(
            url = "wss://api.company.com/v1/ws",
            autoReconnect = true
        )
        wsClient = WebSocketClient(engine, config)

        wsClient.addEventHandler(object : WebSocketEventHandler {
            override fun onMessage(message: WebSocketMessage) {
                if (message is WebSocketMessage.EventMessage) {
                    handleCameraEvent(message)
                }
            }
        })
    }

    suspend fun start(token: String) {
        wsClient.connect(token)
        wsClient.subscribe(
            channels = listOf("camera_events", "camera_status")
        )
    }

    private fun handleCameraEvent(event: WebSocketMessage.EventMessage) {
        // Обработка события камеры
    }

    fun stop() {
        wsClient.disconnect()
    }
}
```

### Обработка переподключений

```kotlin
wsClient.getConnectionState().collect { state ->
    when (state) {
        WebSocketConnectionState.RECONNECTING -> {
            // Показать индикатор переподключения
        }
        WebSocketConnectionState.CONNECTED -> {
            // Восстановить подписки
            wsClient.subscribe(channels = listOf("camera_events"))
        }
        else -> {}
    }
}
```

## Примечания

- Клиент автоматически переподключается при разрыве соединения
- Подписки автоматически восстанавливаются после переподключения
- Токен аутентификации автоматически отправляется при подключении
- Ping/Pong сообщения обрабатываются автоматически для поддержания соединения

## Статус реализации

**Текущий прогресс:** ~80%

**Реализовано:**
- ✅ Подключение/отключение
- ✅ Автоматическое переподключение
- ✅ Подписки на каналы
- ✅ Обработка текстовых сообщений
- ✅ Обработка событий через WebSocketEventHandler

**Не реализовано:**
- ❌ Обработка бинарных сообщений (игнорируются)
- ❌ Очередь сообщений при отключении
- ❌ Rate limiting
- ❌ Метрики и мониторинг (latency, reconnect count)

**Детальный анализ нереализованного функционала:** [MISSING_FUNCTIONALITY.md](MISSING_FUNCTIONALITY.md#websocketclient)
**Разделение разработки по платформам:** [PLATFORMS.md](PLATFORMS.md)

---

## Связанные документы

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

### Основные документы
- **[DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)** - Полный индекс документации
- **[README.md](../README.md)** - Обзор проекта

### Статус и анализ
- **[MISSING_FUNCTIONALITY.md](MISSING_FUNCTIONALITY.md#websocketclient)** - Детальный анализ нереализованного функционала WebSocket клиента
- **[IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)** - Статус реализации компонентов

### API и сервер
- **[API.md](API.md)** - REST API документация (WebSocket API раздел)
- **[DEVELOPMENT.md](DEVELOPMENT.md)** - Руководство по разработке
- **[PLATFORMS.md](PLATFORMS.md)** - Разделение разработки по платформам

---

**Последнее обновление:** 26 January 2026

