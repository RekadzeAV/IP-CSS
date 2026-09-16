package com.company.ipcamera.core.network

import kotlinx.serialization.json.buildJsonObject
import kotlin.test.*

/**
 * Тесты для MessageQueue
 */
class MessageQueueTest {

    @Test
    fun testEnqueueDequeue() {
        val queue = MessageQueue(maxSize = 10)

        val message1 = WebSocketMessage.EventMessage("test1", "channel", buildJsonObject { })
        val message2 = WebSocketMessage.EventMessage("test2", "channel", buildJsonObject { })

        assertTrue(queue.enqueue(message1, MessagePriority.NORMAL))
        assertTrue(queue.enqueue(message2, MessagePriority.NORMAL))

        assertEquals(2, queue.size())

        val dequeued1 = queue.dequeue()
        assertNotNull(dequeued1)
        assertTrue(dequeued1 is WebSocketMessage.EventMessage)

        val dequeued2 = queue.dequeue()
        assertNotNull(dequeued2)

        assertTrue(queue.isEmpty())
    }

    @Test
    fun testPriorityOrder() {
        val queue = MessageQueue(maxSize = 10)

        // Добавляем сообщения с разными приоритетами
        queue.enqueue(WebSocketMessage.EventMessage("low", "channel", buildJsonObject { }), MessagePriority.LOW)
        queue.enqueue(WebSocketMessage.AuthMessage("token"), MessagePriority.CRITICAL)
        queue.enqueue(WebSocketMessage.SubscribeMessage(listOf("channel")), MessagePriority.HIGH)
        queue.enqueue(WebSocketMessage.EventMessage("normal", "channel", buildJsonObject { }), MessagePriority.NORMAL)

        // Извлекаем - должны идти по приоритету
        val first = queue.dequeue()
        assertTrue(first is WebSocketMessage.AuthMessage, "CRITICAL should be first")

        val second = queue.dequeue()
        assertTrue(second is WebSocketMessage.SubscribeMessage, "HIGH should be second")

        val third = queue.dequeue()
        assertTrue(
            third is WebSocketMessage.EventMessage && (third as WebSocketMessage.EventMessage).type == "normal",
            "NORMAL should be third"
        )

        val fourth = queue.dequeue()
        assertTrue(
            fourth is WebSocketMessage.EventMessage && (fourth as WebSocketMessage.EventMessage).type == "low",
            "LOW should be last"
        )
    }

    @Test
    fun testOverflowDropOldest() {
        val queue = MessageQueue(
            maxSize = 3,
            overflowStrategy = QueueOverflowStrategy.DROP_OLDEST
        )

        // Заполняем очередь
        repeat(3) {
            queue.enqueue(
                WebSocketMessage.EventMessage("msg$it", "channel", buildJsonObject { }),
                MessagePriority.NORMAL
            )
        }
        assertEquals(3, queue.size())

        // Добавляем еще одно - должно удалить самое старое
        queue.enqueue(WebSocketMessage.EventMessage("msg3", "channel", buildJsonObject { }), MessagePriority.NORMAL)
        assertEquals(3, queue.size())

        // Первое сообщение должно быть удалено
        val first = queue.dequeue()
        assertTrue(first is WebSocketMessage.EventMessage)
        assertNotEquals("msg0", (first as WebSocketMessage.EventMessage).type)
    }

    @Test
    fun testOverflowDropLowest() {
        val queue = MessageQueue(
            maxSize = 2,
            overflowStrategy = QueueOverflowStrategy.DROP_LOWEST
        )

        // Добавляем CRITICAL и LOW
        queue.enqueue(WebSocketMessage.AuthMessage("token"), MessagePriority.CRITICAL)
        queue.enqueue(WebSocketMessage.EventMessage("low", "channel", buildJsonObject { }), MessagePriority.LOW)

        // Добавляем HIGH - должно удалить LOW
        queue.enqueue(WebSocketMessage.SubscribeMessage(listOf("channel")), MessagePriority.HIGH)

        assertEquals(2, queue.size())

        // CRITICAL должно остаться
        val first = queue.dequeue()
        assertTrue(first is WebSocketMessage.AuthMessage)

        // HIGH должно остаться (LOW удалено)
        val second = queue.dequeue()
        assertTrue(second is WebSocketMessage.SubscribeMessage)
    }

    @Test
    fun testOverflowReject() {
        val queue = MessageQueue(
            maxSize = 2,
            overflowStrategy = QueueOverflowStrategy.REJECT
        )

        // Заполняем очередь
        queue.enqueue(WebSocketMessage.EventMessage("msg1", "channel", buildJsonObject { }), MessagePriority.NORMAL)
        queue.enqueue(WebSocketMessage.EventMessage("msg2", "channel", buildJsonObject { }), MessagePriority.NORMAL)

        // Попытка добавить еще одно - должно быть отклонено
        val enqueued = queue.enqueue(
            WebSocketMessage.EventMessage("msg3", "channel", buildJsonObject { }),
            MessagePriority.NORMAL
        )
        assertFalse(enqueued, "Message should be rejected when queue is full")
        assertEquals(2, queue.size())
    }

    @Test
    fun testClear() {
        val queue = MessageQueue(maxSize = 10)

        repeat(5) {
            queue.enqueue(
                WebSocketMessage.EventMessage("msg$it", "channel", buildJsonObject { }),
                MessagePriority.NORMAL
            )
        }

        assertEquals(5, queue.size())
        queue.clear()
        assertTrue(queue.isEmpty())
    }

    @Test
    fun testMetrics() {
        val queue = MessageQueue(maxSize = 10)

        // Добавляем сообщения
        queue.enqueue(WebSocketMessage.AuthMessage("token"), MessagePriority.CRITICAL)
        queue.enqueue(WebSocketMessage.SubscribeMessage(listOf("channel")), MessagePriority.HIGH)
        queue.enqueue(WebSocketMessage.EventMessage("normal", "channel", buildJsonObject { }), MessagePriority.NORMAL)
        queue.enqueue(WebSocketMessage.EventMessage("low", "channel", buildJsonObject { }), MessagePriority.LOW)

        val metrics = queue.getMetrics()

        assertEquals(4, metrics.currentSize)
        assertEquals(10, metrics.maxSize)
        assertTrue(metrics.messagesByPriority.containsKey(MessagePriority.CRITICAL))
        assertTrue(metrics.messagesByPriority.containsKey(MessagePriority.HIGH))
        assertTrue(metrics.messagesByPriority.containsKey(MessagePriority.NORMAL))
        assertTrue(metrics.messagesByPriority.containsKey(MessagePriority.LOW))
    }

    @Test
    fun testClearOldMessages() {
        var now = 10_000L
        val queue = MessageQueue(maxSize = 10, nowProvider = { now })

        // Добавляем сообщение
        queue.enqueue(WebSocketMessage.EventMessage("test", "channel", buildJsonObject { }), MessagePriority.NORMAL)

        // Очищаем старые сообщения (старше 1 секунды) без реального сна.
        now += 1_100
        queue.clearOldMessages(1000)

        // Сообщение должно быть удалено
        assertTrue(queue.isEmpty())
    }

    @Test
    fun testSamePriorityOrder() {
        var now = 20_000L
        val queue = MessageQueue(maxSize = 10, nowProvider = { now })

        // Добавляем сообщения с одинаковым приоритетом в разное время
        queue.enqueue(WebSocketMessage.EventMessage("first", "channel", buildJsonObject { }), MessagePriority.NORMAL)
        now += 10
        queue.enqueue(WebSocketMessage.EventMessage("second", "channel", buildJsonObject { }), MessagePriority.NORMAL)

        // Первое должно быть извлечено первым (FIFO для одинакового приоритета)
        val first = queue.dequeue()
        assertTrue(first is WebSocketMessage.EventMessage)
        assertEquals("first", (first as WebSocketMessage.EventMessage).type)
    }

    @Test
    fun testStressBurstWithDropLowestKeepsCriticalMessages() {
        val queue = MessageQueue(
            maxSize = 50,
            overflowStrategy = QueueOverflowStrategy.DROP_LOWEST
        )

        // Имитируем burst-нагрузку: много LOW/NORMAL и периодические CRITICAL.
        repeat(5000) { index ->
            when {
                index % 250 == 0 -> {
                    queue.enqueue(
                        WebSocketMessage.AuthMessage("token-$index"),
                        MessagePriority.CRITICAL
                    )
                }
                index % 3 == 0 -> {
                    queue.enqueue(
                        WebSocketMessage.EventMessage("normal-$index", "channel", buildJsonObject { }),
                        MessagePriority.NORMAL
                    )
                }
                else -> {
                    queue.enqueue(
                        WebSocketMessage.EventMessage("low-$index", "channel", buildJsonObject { }),
                        MessagePriority.LOW
                    )
                }
            }
        }

        val metrics = queue.getMetrics()
        assertEquals(50, metrics.currentSize, "Queue should stay capped at max size")
        assertTrue(metrics.droppedMessages > 0, "Burst load should produce dropped messages")

        // При DROP_LOWEST в хвосте очереди должны сохраняться сообщения высокого приоритета.
        val snapshot = mutableListOf<WebSocketMessage>()
        while (!queue.isEmpty()) {
            queue.dequeue()?.let { snapshot.add(it) }
        }
        assertTrue(
            snapshot.any { it is WebSocketMessage.AuthMessage },
            "At least one CRITICAL message must survive burst"
        )
    }
}
