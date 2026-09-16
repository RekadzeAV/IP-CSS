package com.company.ipcamera.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.*

/**
 * Интеграционные тесты для WebSocketClient
 */
class WebSocketClientIntegrationTest {

    @Test
    fun testMessageQueueWithRateLimiting() = runTest {
        val config = WebSocketClientConfig(
            url = "ws://test.example.com",
            queueMaxSize = 10,
            queueOverflowStrategy = QueueOverflowStrategy.DROP_LOWEST,
            rateLimitConfig = RateLimitConfig(
                messagesPerSecond = 5,
                enabled = true
            )
        )

        // Создаем mock engine (в реальности нужен тестовый WebSocket сервер)
        // Для упрощения теста проверяем только логику интеграции

        val messageQueue = MessageQueue(
            maxSize = config.queueMaxSize,
            overflowStrategy = config.queueOverflowStrategy
        )
        val rateLimiter = RateLimiter(config.rateLimitConfig)

        // Отправляем сообщения быстрее лимита
        var queuedCount = 0
        repeat(10) {
            if (!rateLimiter.checkMessageLimit()) {
                val enqueued = messageQueue.enqueue(
                    WebSocketMessage.EventMessage("test", "channel", buildJsonObject { }),
                    MessagePriority.NORMAL
                )
                if (enqueued) queuedCount++
            }
        }

        assertTrue(queuedCount > 0, "Some messages should be queued when rate limit exceeded")

        // Проверяем, что очередь работает
        assertTrue(messageQueue.size() > 0, "Queue should contain messages")

        // Проверяем метрики
        val queueMetrics = messageQueue.getMetrics()
        assertTrue(queueMetrics.currentSize > 0, "Queue should have messages")

        val rateMetrics = rateLimiter.getMetrics()
        assertTrue(rateMetrics.blockedRequests > 0, "Should have blocked requests")
    }

    @Test
    fun testPriorityQueueWithRateLimiting() = runTest {
        val messageQueue = MessageQueue(maxSize = 10)

        // Добавляем сообщения с разными приоритетами
        messageQueue.enqueue(
            WebSocketMessage.EventMessage("test", "channel", buildJsonObject { }),
            MessagePriority.LOW
        )
        messageQueue.enqueue(
            WebSocketMessage.AuthMessage("token"),
            MessagePriority.CRITICAL
        )
        messageQueue.enqueue(
            WebSocketMessage.SubscribeMessage(listOf("channel")),
            MessagePriority.HIGH
        )

        // Извлекаем сообщения - должны идти по приоритету
        val first = messageQueue.dequeue()
        assertTrue(first is WebSocketMessage.AuthMessage, "CRITICAL should be first")

        val second = messageQueue.dequeue()
        assertTrue(second is WebSocketMessage.SubscribeMessage, "HIGH should be second")

        val third = messageQueue.dequeue()
        assertTrue(third is WebSocketMessage.EventMessage, "LOW should be last")
    }

    @Test
    fun testBinaryMessageWithQueue() = runTest {
        val messageQueue = MessageQueue(maxSize = 10)
        val binaryHandler = BinaryMessageHandler()

        // Создаем JPEG данные
        val jpegData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())

        // Определяем тип
        val messageType = binaryHandler.detectMessageType(jpegData)
        assertEquals(BinaryMessageType.IMAGE, messageType)

        // Создаем метаданные
        val metadata = BinaryMessageMetadata(
            type = messageType?.name ?: "CUSTOM",
            mimeType = binaryHandler.detectMimeType(jpegData),
            size = jpegData.size.toLong(),
            messageId = "test-id"
        )

        // Создаем сообщение
        val imageMessage = WebSocketMessage.ImageMessage(jpegData, metadata)

        // Добавляем в очередь
        val enqueued = messageQueue.enqueue(imageMessage, MessagePriority.HIGH)
        assertTrue(enqueued, "Image message should be queued")

        // Извлекаем
        val dequeued = messageQueue.dequeue()
        assertTrue(dequeued is WebSocketMessage.ImageMessage, "Should get ImageMessage")
        assertEquals(imageMessage.data.size, (dequeued as WebSocketMessage.ImageMessage).data.size)
    }

    @Test
    fun testQueueOverflowStrategies() = runTest {
        // Тест DROP_OLDEST
        val queueOldest = MessageQueue(
            maxSize = 3,
            overflowStrategy = QueueOverflowStrategy.DROP_OLDEST
        )

        repeat(5) {
            queueOldest.enqueue(
                WebSocketMessage.EventMessage("test", "channel", buildJsonObject { }),
                MessagePriority.NORMAL
            )
        }

        assertEquals(3, queueOldest.size(), "Queue should have max size")

        // Тест DROP_LOWEST
        val queueLowest = MessageQueue(
            maxSize = 2,
            overflowStrategy = QueueOverflowStrategy.DROP_LOWEST
        )

        queueLowest.enqueue(
            WebSocketMessage.AuthMessage("token"),
            MessagePriority.CRITICAL
        )
        queueLowest.enqueue(
            WebSocketMessage.EventMessage("test", "channel", buildJsonObject { }),
            MessagePriority.LOW
        )

        // Попытка добавить еще одно LOW должна быть отклонена:
        // в очереди уже нет более низкого приоритета, который можно вытеснить.
        val enqueued = queueLowest.enqueue(
            WebSocketMessage.EventMessage("test2", "channel", buildJsonObject { }),
            MessagePriority.LOW
        )

        assertFalse(enqueued, "LOW message should be rejected when queue contains CRITICAL+LOW")
        assertEquals(2, queueLowest.size(), "Queue should still have max size")

        // CRITICAL должно остаться
        val first = queueLowest.dequeue()
        assertTrue(first is WebSocketMessage.AuthMessage, "CRITICAL should remain")
    }

    @Test
    fun testRateLimiterWithDifferentOperations() = runTest {
        val config = RateLimitConfig(
            messagesPerSecond = 10,
            subscriptionsPerSecond = 5,
            bytesPerSecond = 1000,
            enabled = true
        )
        val limiter = RateLimiter(config)

        // Проверяем, что разные операции имеют независимые лимиты
        repeat(10) {
            assertTrue(limiter.checkMessageLimit(), "Message limit should work")
        }

        repeat(5) {
            assertTrue(limiter.checkSubscriptionLimit(), "Subscription limit should work")
        }

        assertTrue(limiter.checkBytesLimit(500), "Bytes limit should work")
        assertTrue(limiter.checkBytesLimit(400), "More bytes should work")

        // Превышаем лимиты
        assertFalse(limiter.checkMessageLimit(), "Message limit should be exceeded")
        assertFalse(limiter.checkSubscriptionLimit(), "Subscription limit should be exceeded")
        assertFalse(limiter.checkBytesLimit(200), "Bytes limit should be exceeded")

        val metrics = limiter.getMetrics()
        assertTrue(metrics.blockedRequests > 0, "Should have blocked requests")
        assertTrue(metrics.violations.isNotEmpty(), "Should have violations")
    }

    @Test
    fun testChunkingWithLargeMessage() = runTest {
        val binaryHandler = BinaryMessageHandler()

        // Создаем большое сообщение (>64KB)
        val largeData = ByteArray(ChunkingConstants.CHUNK_SIZE_THRESHOLD + 1000)
        largeData.fill(0x42)

        assertTrue(binaryHandler.needsChunking(largeData), "Large message should need chunking")

        val messageId = "test-large-message"
        val chunks = binaryHandler.splitIntoChunks(largeData, messageId)

        assertTrue(chunks.size > 1, "Should be split into multiple chunks")

        // Проверяем размер каждого chunk
        chunks.forEach { chunk ->
            assertTrue(
                chunk.size <= ChunkingConstants.MAX_CHUNK_SIZE,
                "Each chunk should not exceed MAX_CHUNK_SIZE"
            )
        }

        // Собираем обратно
        val reassembled = ByteArray(largeData.size)
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(reassembled, offset)
            offset += chunk.size
        }

        assertTrue(reassembled.contentEquals(largeData), "Reassembled should equal original")
    }
}
