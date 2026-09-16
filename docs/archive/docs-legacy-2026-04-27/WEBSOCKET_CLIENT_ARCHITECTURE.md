# Архитектура WebSocketClient - Детальное проектирование

**Дата создания:** 26 January 2026
**Версия:** 1.0
**Статус:** ✅ Завершено

---

## 📋 Анализ текущей реализации

### Kotlin WebSocketClient

**Текущее состояние:**
- ✅ Подключение/отключение работает
- ✅ Автоматическое переподключение реализовано
- ✅ Подписки на каналы работают
- ✅ Обработка текстовых сообщений работает
- 🟡 Бинарные сообщения обрабатываются, но:
  - Только как простой `BinaryMessage` без типизации
  - Нет chunking для больших сообщений
  - Нет валидации бинарных данных
  - Нет определения типа данных (изображение, видео, файл)
- 🟡 Очередь сообщений есть (`messageBuffer`), но:
  - Простой список без приоритетов
  - Нет стратегий обработки переполнения
  - Нет метрик
- ❌ Rate limiting отсутствует

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/WebSocketClient.kt`

### TypeScript WebSocketClient

**Текущее состояние:**
- ✅ Подключение/отключение работает
- ✅ Автоматическое переподключение реализовано
- ✅ Подписки на каналы работают
- ✅ Обработка текстовых сообщений работает
- ❌ Бинарные сообщения не обрабатываются (только JSON)
- ❌ Очередь сообщений отсутствует
- ❌ Rate limiting отсутствует

**Файл:** `server/web/src/utils/websocket.ts`

---

## 🏗️ Проектирование архитектуры

### 1. Обработка бинарных сообщений

#### 1.1 Типы бинарных сообщений

```kotlin
sealed class BinaryMessageType {
    object Image : BinaryMessageType()      // JPEG, PNG, WebP
    object VideoChunk : BinaryMessageType() // H.264, H.265 фрагменты
    object File : BinaryMessageType()       // Общие файлы
    object Custom : BinaryMessageType()     // Пользовательские форматы
}

data class BinaryMessageMetadata(
    val type: BinaryMessageType,
    val mimeType: String,
    val size: Long,
    val messageId: String,
    val chunkIndex: Int? = null,  // Для chunking
    val totalChunks: Int? = null, // Для chunking
    val timestamp: Long = System.currentTimeMillis()
)
```

#### 1.2 Расширенные типы сообщений

```kotlin
sealed class WebSocketMessage {
    // Существующие типы...

    @Serializable
    data class ImageMessage(
        val data: ByteArray,
        val metadata: BinaryMessageMetadata
    ) : WebSocketMessage()

    @Serializable
    data class VideoChunkMessage(
        val data: ByteArray,
        val metadata: BinaryMessageMetadata
    ) : WebSocketMessage()

    @Serializable
    data class FileMessage(
        val data: ByteArray,
        val metadata: BinaryMessageMetadata,
        val fileName: String
    ) : WebSocketMessage()

    @Serializable
    data class CustomBinaryMessage(
        val data: ByteArray,
        val metadata: BinaryMessageMetadata,
        val format: String
    ) : WebSocketMessage()
}
```

#### 1.3 Chunking для больших сообщений

**Порог:** 64KB (65536 байт)

**Протокол chunking:**
1. Большое сообщение разбивается на фрагменты по 64KB
2. Каждый фрагмент содержит:
   - `messageId` - уникальный ID сообщения
   - `chunkIndex` - индекс фрагмента (0-based)
   - `totalChunks` - общее количество фрагментов
   - `data` - данные фрагмента
3. На клиенте фрагменты собираются в полное сообщение
4. Таймаут для неполных сообщений: 30 секунд

#### 1.4 Валидация бинарных данных

**Magic bytes для определения типа:**
- JPEG: `FF D8 FF`
- PNG: `89 50 4E 47 0D 0A 1A 0A`
- WebP: `52 49 46 46` + `57 45 42 50`
- H.264 NAL: `00 00 00 01` или `00 00 01`
- H.265 NAL: `00 00 00 01` или `00 00 01`

**Проверки:**
1. Размер данных (максимум: 100MB)
2. Формат (magic bytes)
3. Целостность (checksum для chunking)

---

### 2. Система очереди сообщений с приоритетами

#### 2.1 Приоритеты сообщений

```kotlin
enum class MessagePriority(val value: Int) {
    CRITICAL(0),  // Аутентификация, ошибки
    HIGH(1),      // Подписки, события
    NORMAL(2),    // Обычные сообщения
    LOW(3)        // Статистика, heartbeat
}
```

#### 2.2 Структура очереди

```kotlin
data class QueuedMessage(
    val message: WebSocketMessage,
    val priority: MessagePriority,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
```

#### 2.3 Стратегии обработки переполнения

```kotlin
enum class QueueOverflowStrategy {
    DROP_OLDEST,  // Удалять старые сообщения
    DROP_LOWEST,  // Удалять сообщения с низким приоритетом
    REJECT,       // Отклонять новые сообщения
    BLOCK         // Блокировать до освобождения места
}
```

#### 2.4 Метрики очереди

```kotlin
data class QueueMetrics(
    val currentSize: Int,
    val maxSize: Int,
    val averageWaitTime: Long,  // мс
    val droppedMessages: Long,
    val messagesByPriority: Map<MessagePriority, Int>
)
```

---

### 3. Механизм Rate Limiting

#### 3.1 Типы лимитов

```kotlin
data class RateLimitConfig(
    val messagesPerSecond: Int = 100,      // Лимит на отправку сообщений
    val subscriptionsPerSecond: Int = 10,   // Лимит на подписки/отписки
    val bytesPerSecond: Long = 10 * 1024 * 1024, // 10MB/сек
    val windowSizeMillis: Long = 1000      // Размер окна (1 секунда)
)
```

#### 3.2 Алгоритм Token Bucket

**Принцип работы:**
1. Каждый тип операции имеет свой bucket с токенами
2. Токены пополняются с заданной скоростью
3. Операция требует токен(ы) для выполнения
4. Если токенов нет - операция блокируется или ставится в очередь

**Преимущества:**
- Гибкость (разные лимиты для разных операций)
- Эффективность (O(1) проверка)
- Плавное ограничение (burst допускается)

#### 3.3 Метрики rate limiting

```kotlin
data class RateLimitMetrics(
    val blockedRequests: Long,
    val currentRate: Double,  // Текущая скорость (операций/сек)
    val violations: List<RateLimitViolation>
)

data class RateLimitViolation(
    val timestamp: Long,
    val operationType: String,
    val limitType: String
)
```

---

## 🔗 Интеграция компонентов

### Схема взаимодействия

```
WebSocketClient
    ├── BinaryMessageHandler
    │   ├── ChunkingManager
    │   ├── BinaryValidator
    │   └── MessageTypeDetector
    ├── MessageQueue
    │   ├── PriorityQueue
    │   ├── OverflowStrategy
    │   └── QueueMetrics
    └── RateLimiter
        ├── TokenBucket (для сообщений)
        ├── TokenBucket (для подписок)
        └── TokenBucket (для размера)
```

### Порядок обработки сообщения

1. **Отправка:**
   - Проверка rate limit → Очередь (если превышен) → Отправка

2. **Получение:**
   - Получение фрейма → Определение типа → Chunking (если нужно) → Валидация → Обработчик

---

## 📝 Интерфейсы и контракты

### BinaryMessageHandler

```kotlin
interface BinaryMessageHandler {
    suspend fun handleBinaryFrame(data: ByteArray): WebSocketMessage?
    suspend fun sendBinaryMessage(message: WebSocketMessage): Boolean
    fun detectMessageType(data: ByteArray): BinaryMessageType?
    fun validateBinaryData(data: ByteArray, type: BinaryMessageType): Boolean
}
```

### MessageQueue

```kotlin
interface MessageQueue {
    fun enqueue(message: WebSocketMessage, priority: MessagePriority): Boolean
    fun dequeue(): WebSocketMessage?
    fun size(): Int
    fun clear()
    fun getMetrics(): QueueMetrics
}
```

### RateLimiter

```kotlin
interface RateLimiter {
    fun checkLimit(operation: String): Boolean
    fun recordOperation(operation: String, size: Long = 0)
    fun reset()
    fun getMetrics(): RateLimitMetrics
}
```

---

## ✅ Критерии приемки

### Обработка бинарных сообщений
- ✅ Поддержка JPEG, PNG, WebP, H.264, H.265
- ✅ Chunking для сообщений > 64KB
- ✅ Валидация через magic bytes
- ✅ Обработка ошибок chunking

### Очередь сообщений
- ✅ Приоритеты работают корректно
- ✅ Стратегии переполнения работают
- ✅ Метрики собираются

### Rate Limiting
- ✅ Лимиты применяются корректно
- ✅ Разные лимиты для разных операций
- ✅ События превышения лимитов

---

**Статус:** ✅ Проектирование завершено
**Следующий шаг:** Начать реализацию Фазы 2 (Доработка бинарных сообщений)
