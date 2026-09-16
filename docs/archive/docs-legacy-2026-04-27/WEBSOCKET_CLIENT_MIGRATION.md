# Руководство по миграции WebSocketClient

**Версия:** 2.0
**Дата:** 26 January 2026

---

## Обзор изменений

WebSocketClient был значительно улучшен с добавлением новых возможностей:
- Обработка бинарных сообщений (изображения, видео, файлы)
- Приоритетная очередь сообщений
- Rate limiting
- Chunking для больших сообщений

---

## Изменения API

### 1. Новый параметр `priority` в `sendMessage()`

**Старый код:**
```kotlin
wsClient.sendMessage(WebSocketMessage.EventMessage(...))
```

**Новый код:**
```kotlin
// Обратная совместимость сохранена (по умолчанию MessagePriority.NORMAL)
wsClient.sendMessage(WebSocketMessage.EventMessage(...))

// Или с явным указанием приоритета
wsClient.sendMessage(
    WebSocketMessage.EventMessage(...),
    MessagePriority.HIGH
)
```

**Breaking changes:** Нет - параметр опциональный

---

### 2. Новые методы в `WebSocketEventHandler`

**Старый код:**
```kotlin
class MyEventHandler : WebSocketEventHandler {
    override fun onConnected() {}
    override fun onDisconnected(cause: Throwable?) {}
    override fun onMessage(message: WebSocketMessage) {}
    override fun onError(error: Throwable) {}
}
```

**Новый код:**
```kotlin
class MyEventHandler : WebSocketEventHandler {
    override fun onConnected() {}
    override fun onDisconnected(cause: Throwable?) {}
    override fun onMessage(message: WebSocketMessage) {}
    override fun onError(error: Throwable) {}

    // Новые опциональные методы (имеют реализации по умолчанию)
    override fun onRateLimitExceeded(operationType: String, limitType: String) {
        // Обработка превышения rate limit
    }

    override fun onQueueOverflow(strategy: QueueOverflowStrategy, droppedCount: Long) {
        // Обработка переполнения очереди
    }
}
```

**Breaking changes:** Нет - методы опциональные

---

### 3. Новые типы сообщений

**Добавлены новые типы бинарных сообщений:**
- `WebSocketMessage.ImageMessage`
- `WebSocketMessage.VideoChunkMessage`
- `WebSocketMessage.FileMessage`
- `WebSocketMessage.CustomBinaryMessage`

**Пример использования:**
```kotlin
override fun onMessage(message: WebSocketMessage) {
    when (message) {
        is WebSocketMessage.ImageMessage -> {
            // Обработка изображения
            val imageData = message.data
            val mimeType = message.metadata.mimeType
        }
        is WebSocketMessage.VideoChunkMessage -> {
            // Обработка видео фрагмента
        }
        // ... остальные типы
    }
}
```

**Breaking changes:** Нет - старые типы сохранены

---

### 4. Обновление конфигурации

**Старый код:**
```kotlin
val config = WebSocketClientConfig(
    url = "wss://api.example.com/ws",
    autoReconnect = true
)
```

**Новый код:**
```kotlin
val config = WebSocketClientConfig(
    url = "wss://api.example.com/ws",
    autoReconnect = true,
    queueMaxSize = 100,  // Новый параметр
    queueOverflowStrategy = QueueOverflowStrategy.DROP_LOWEST,  // Новый параметр
    rateLimitConfig = RateLimitConfig(  // Новый параметр
        messagesPerSecond = 100,
        subscriptionsPerSecond = 10,
        bytesPerSecond = 10 * 1024 * 1024,
        enabled = true
    )
)
```

**Breaking changes:** Нет - все параметры опциональные с значениями по умолчанию

---

### 5. Новые методы

**Добавлены методы для получения метрик:**
```kotlin
// Получить метрики очереди
val queueMetrics = wsClient.getQueueMetrics()
println("Queue size: ${queueMetrics.currentSize}")

// Получить метрики rate limiting
val rateMetrics = wsClient.getRateLimitMetrics()
println("Blocked requests: ${rateMetrics.blockedRequests}")
```

**Breaking changes:** Нет - только новые методы

---

## Миграция по шагам

### Шаг 1: Обновление зависимостей

Убедитесь, что используете последнюю версию проекта.

### Шаг 2: Обновление обработчиков событий (опционально)

Если хотите использовать новые возможности:

```kotlin
class MyEventHandler : WebSocketEventHandler {
    // ... существующие методы ...

    // Добавьте обработку новых событий
    override fun onRateLimitExceeded(operationType: String, limitType: String) {
        // Ваша логика
    }

    override fun onQueueOverflow(strategy: QueueOverflowStrategy, droppedCount: Long) {
        // Ваша логика
    }
}
```

### Шаг 3: Обновление обработки сообщений (опционально)

Добавьте обработку новых типов бинарных сообщений:

```kotlin
override fun onMessage(message: WebSocketMessage) {
    when (message) {
        // Существующие типы
        is WebSocketMessage.EventMessage -> { /* ... */ }

        // Новые типы бинарных сообщений
        is WebSocketMessage.ImageMessage -> {
            // Обработка изображения
        }
        is WebSocketMessage.VideoChunkMessage -> {
            // Обработка видео
        }
        // ...
    }
}
```

### Шаг 4: Настройка конфигурации (опционально)

Настройте очередь и rate limiting по необходимости:

```kotlin
val config = WebSocketClientConfig(
    url = "wss://api.example.com/ws",
    // ... существующие параметры ...

    // Новые параметры (опционально)
    queueMaxSize = 200,
    queueOverflowStrategy = QueueOverflowStrategy.DROP_OLDEST,
    rateLimitConfig = RateLimitConfig(
        messagesPerSecond = 50,  // Настройте под ваши нужды
        enabled = true
    )
)
```

---

## Обратная совместимость

✅ **Все изменения обратно совместимы:**
- Старый код продолжит работать без изменений
- Новые параметры имеют значения по умолчанию
- Новые методы опциональны
- Старые типы сообщений сохранены

---

## Рекомендации

1. **Постепенная миграция:** Обновляйте код постепенно, начиная с обработчиков событий
2. **Тестирование:** Протестируйте обновленный код перед развертыванием
3. **Мониторинг:** Используйте новые метрики для мониторинга производительности
4. **Настройка:** Настройте rate limiting и очередь под ваши требования

---

## Примеры миграции

### Пример 1: Простое обновление

**До:**
```kotlin
val wsClient = WebSocketClient(engine, WebSocketClientConfig(url = "wss://api.example.com/ws"))
wsClient.connect()
```

**После:** Код работает без изменений ✅

### Пример 2: С обработкой бинарных сообщений

**До:**
```kotlin
override fun onMessage(message: WebSocketMessage) {
    if (message is WebSocketMessage.BinaryMessage) {
        // Обработка бинарных данных
    }
}
```

**После:**
```kotlin
override fun onMessage(message: WebSocketMessage) {
    when (message) {
        is WebSocketMessage.ImageMessage -> {
            // Обработка изображения с метаданными
            val imageData = message.data
            val mimeType = message.metadata.mimeType
        }
        is WebSocketMessage.BinaryMessage -> {
            // Старый тип все еще работает
        }
    }
}
```

---

**Последнее обновление:** 26 January 2026
