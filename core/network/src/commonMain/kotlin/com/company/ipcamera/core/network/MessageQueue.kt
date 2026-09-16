package com.company.ipcamera.core.network

import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Приоритет сообщения
 */
enum class MessagePriority(val value: Int) {
    CRITICAL(0), // Аутентификация, ошибки
    HIGH(1), // Подписки, события
    NORMAL(2), // Обычные сообщения
    LOW(3) // Статистика, heartbeat
}

/**
 * Стратегия обработки переполнения очереди
 */
enum class QueueOverflowStrategy {
    DROP_OLDEST, // Удалять старые сообщения
    DROP_LOWEST, // Удалять сообщения с низким приоритетом
    REJECT, // Отклонять новые сообщения
    BLOCK // Блокировать до освобождения места
}

/**
 * Сообщение в очереди с приоритетом
 */
data class QueuedMessage(
    val message: WebSocketMessage,
    val priority: MessagePriority,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val retryCount: Int = 0
) : Comparable<QueuedMessage> {
    override fun compareTo(other: QueuedMessage): Int {
        // Сначала сравниваем по приоритету (меньше значение = выше приоритет)
        val priorityCompare = priority.value.compareTo(other.priority.value)
        if (priorityCompare != 0) return priorityCompare

        // Если приоритеты равны, сравниваем по времени (старые сообщения первыми)
        return timestamp.compareTo(other.timestamp)
    }
}

/**
 * Метрики очереди сообщений
 */
data class QueueMetrics(
    val currentSize: Int,
    val maxSize: Int,
    val averageWaitTime: Long, // мс
    val droppedMessages: Long,
    val messagesByPriority: Map<MessagePriority, Int>
)

/**
 * Очередь сообщений с приоритетами
 */
class MessageQueue(
    private val maxSize: Int = 100,
    private val overflowStrategy: QueueOverflowStrategy = QueueOverflowStrategy.DROP_LOWEST,
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private val queue = mutableListOf<QueuedMessage>()
    private var droppedMessagesCount = 0L
    private val waitTimes = mutableListOf<Long>()

    /**
     * Добавить сообщение в очередь
     */
    fun enqueue(message: WebSocketMessage, priority: MessagePriority = MessagePriority.NORMAL): Boolean {
        // Проверяем, есть ли место в очереди
        if (queue.size >= maxSize) {
            return when (overflowStrategy) {
                QueueOverflowStrategy.DROP_OLDEST -> {
                    val removed = queue.minByOrNull { it.timestamp }
                    if (removed != null) {
                        queue.remove(removed)
                        logger.debug { "Dropped oldest message (priority: ${removed.priority})" }
                    }
                    droppedMessagesCount++
                    queue.add(QueuedMessage(message, priority, timestamp = nowProvider()))
                    true
                }
                QueueOverflowStrategy.DROP_LOWEST -> {
                    // Находим сообщение с самым низким приоритетом
                    val lowestPriorityMessage = queue.maxByOrNull { it.priority.value }
                    if (lowestPriorityMessage != null && lowestPriorityMessage.priority.value > priority.value) {
                        queue.remove(lowestPriorityMessage)
                        logger.debug { "Dropped lowest priority message (priority: ${lowestPriorityMessage.priority})" }
                        droppedMessagesCount++
                        queue.add(QueuedMessage(message, priority, timestamp = nowProvider()))
                        true
                    } else {
                        logger.warn { "Cannot enqueue message: queue full and priority not high enough" }
                        droppedMessagesCount++
                        false
                    }
                }
                QueueOverflowStrategy.REJECT -> {
                    logger.warn { "Cannot enqueue message: queue full (REJECT strategy)" }
                    droppedMessagesCount++
                    false
                }
                QueueOverflowStrategy.BLOCK -> {
                    // В реальной реализации здесь была бы блокировка
                    // Для упрощения используем REJECT
                    logger.warn { "Cannot enqueue message: queue full (BLOCK strategy - using REJECT)" }
                    droppedMessagesCount++
                    false
                }
            }
        }

        // Есть место, добавляем сообщение
        queue.add(QueuedMessage(message, priority, timestamp = nowProvider()))
        logger.debug { "Enqueued message (priority: $priority, queue size: ${queue.size})" }
        return true
    }

    /**
     * Извлечь сообщение из очереди (с наивысшим приоритетом)
     */
    fun dequeue(): WebSocketMessage? {
        val queuedMessage = queue.minOrNull() ?: return null
        queue.remove(queuedMessage)
        val waitTime = nowProvider() - queuedMessage.timestamp
        waitTimes.add(waitTime)

        // Ограничиваем размер списка waitTimes для памяти
        if (waitTimes.size > 1000) {
            waitTimes.removeAt(0)
        }

        logger.debug { "Dequeued message (priority: ${queuedMessage.priority}, wait time: ${waitTime}ms)" }
        return queuedMessage.message
    }

    /**
     * Получить размер очереди
     */
    fun size(): Int = queue.size

    /**
     * Проверить, пуста ли очередь
     */
    fun isEmpty(): Boolean = queue.isEmpty()

    /**
     * Очистить очередь
     */
    fun clear() {
        queue.clear()
        waitTimes.clear()
        logger.debug { "Queue cleared" }
    }

    /**
     * Получить метрики очереди
     */
    fun getMetrics(): QueueMetrics {
        val messagesByPriority = queue.groupingBy { it.priority }.eachCount()
        val averageWaitTime = if (waitTimes.isNotEmpty()) {
            waitTimes.average().toLong()
        } else {
            0L
        }

        return QueueMetrics(
            currentSize = queue.size,
            maxSize = maxSize,
            averageWaitTime = averageWaitTime,
            droppedMessages = droppedMessagesCount,
            messagesByPriority = messagesByPriority
        )
    }

    /**
     * Очистить старые сообщения (старше указанного времени)
     */
    fun clearOldMessages(maxAgeMillis: Long) {
        val now = nowProvider()
        val beforeSize = queue.size
        queue.removeAll { now - it.timestamp > maxAgeMillis }
        if (queue.size != beforeSize) {
            logger.debug { "Cleared old messages (max age: ${maxAgeMillis}ms)" }
        }
    }
}
