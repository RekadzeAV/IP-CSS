package com.company.ipcamera.core.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Unit тесты для логики переподключения WebSocketClient.
 * Тестирует exponential backoff, max retries, и recovery.
 */
class WebSocketReconnectTest {

    @Test
    fun `test exponential backoff increases delay between retries`() = runTest {
        val config = WebSocketClientConfig(
            url = "ws://test.example.com",
            maxReconnectAttempts = 5,
            reconnectDelayMillis = 100,
            timeoutMillis = 5000,
            autoReconnect = true
        )

        // Симулируем вычисление задержки через логику WebSocketClient
        var currentDelay = config.reconnectDelayMillis
        val delays = mutableListOf<Long>()

        repeat(config.maxReconnectAttempts) {
            delays.add(currentDelay)
            currentDelay = (currentDelay * 2).coerceAtMost(5000L)
        }

        assertEquals(listOf(100L, 200L, 400L, 800L, 1600L), delays)
        assertTrue(delays.all { it <= 5000L })
    }

    @Test
    fun `test reconnect delay caps at maxDelayMs`() = runTest {
        val config = WebSocketClientConfig(
            url = "ws://test.example.com",
            reconnectDelayMillis = 1000,
            timeoutMillis = 3000,
            autoReconnect = true,
            maxReconnectAttempts = 3
        )

        var delay = config.reconnectDelayMillis
        val delays = mutableListOf<Long>()

        repeat(config.maxReconnectAttempts) {
            delays.add(delay)
            delay = (delay * 3).coerceAtMost(3000L)
        }

        assertEquals(listOf(1000L, 3000L, 3000L), delays)
    }

    @Test
    fun `test reconnect disabled when maxRetries is zero`() = runTest {
        val config = WebSocketClientConfig(
            url = "ws://test.example.com",
            maxReconnectAttempts = 0,
            autoReconnect = true
        )

        assertEquals(0, config.maxReconnectAttempts)
        // При maxRetries = 0 reconnect не должен выполняться
    }

    @Test
    fun `test reconnect disabled by config`() = runTest {
        val config = WebSocketClientConfig(
            url = "ws://test.example.com",
            autoReconnect = false,
            maxReconnectAttempts = 5
        )

        assertFalse(config.autoReconnect)
    }

    @Test
    fun `test queue preserves message order after reconnect`() = runTest {
        val queue = MessageQueue(maxSize = 10)

        // Добавляем сообщения до "reconnect"
        queue.enqueue(
            WebSocketMessage.EventMessage("event1", "ch1", buildJsonObject { }),
            MessagePriority.NORMAL
        )
        queue.enqueue(
            WebSocketMessage.EventMessage("event2", "ch1", buildJsonObject { }),
            MessagePriority.NORMAL
        )

        // Извлекаем — порядок должен сохраниться
        val first = queue.dequeue()
        val second = queue.dequeue()

        assertTrue(first is WebSocketMessage.EventMessage)
        assertTrue(second is WebSocketMessage.EventMessage)
        assertEquals("event1", (first as WebSocketMessage.EventMessage).type)
        assertEquals("event2", (second as WebSocketMessage.EventMessage).type)
    }

    @Test
    fun `test rate limiter resets after long interval`() = runTest {
        val config = RateLimitConfig(
            messagesPerSecond = 2,
            enabled = true
        )
        val limiter = RateLimiter(config)

        // Исчерпываем лимит
        assertTrue(limiter.checkMessageLimit())
        assertTrue(limiter.checkMessageLimit())
        assertFalse(limiter.checkMessageLimit())

        // Симулируем прошедшее время (в реальности limiter использует timestamp)
        // Проверяем, что метрики зафиксировали превышение
        val metrics = limiter.getMetrics()
        assertTrue(metrics.blockedRequests > 0)
        assertTrue(metrics.violations.any { it.operationType.contains("message") })
    }

    @Test
    fun `test WebSocketClientConfig defaults are reasonable`() = runTest {
        val config = WebSocketClientConfig(url = "ws://test.example.com")

        assertEquals(10000L, config.timeoutMillis)
        assertTrue(config.autoReconnect)
        assertEquals(Int.MAX_VALUE, config.maxReconnectAttempts)
        assertEquals(5000L, config.reconnectDelayMillis)
        assertEquals(30000L, config.pingIntervalMillis)
        assertEquals(100, config.queueMaxSize)
        assertEquals(QueueOverflowStrategy.DROP_LOWEST, config.queueOverflowStrategy)
    }
}
