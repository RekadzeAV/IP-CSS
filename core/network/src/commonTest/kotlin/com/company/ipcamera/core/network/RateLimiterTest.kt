package com.company.ipcamera.core.network

import kotlinx.coroutines.*
import kotlin.test.*

/**
 * Тесты для RateLimiter
 */
class RateLimiterTest {

    @Test
    fun testMessageLimit() {
        val config = RateLimitConfig(
            messagesPerSecond = 10,
            enabled = true
        )
        val limiter = RateLimiter(config)

        // Отправляем 10 сообщений - должны пройти
        repeat(10) {
            assertTrue(limiter.checkMessageLimit(), "Message $it should be allowed")
        }

        // 11-е сообщение должно быть заблокировано
        assertFalse(limiter.checkMessageLimit(), "11th message should be blocked")
    }

    @Test
    fun testMessageLimitDisabled() {
        val config = RateLimitConfig(
            messagesPerSecond = 1,
            enabled = false
        )
        val limiter = RateLimiter(config)

        // Даже при лимите 1, если disabled, все должны проходить
        repeat(10) {
            assertTrue(limiter.checkMessageLimit(), "Message should be allowed when disabled")
        }
    }

    @Test
    fun testSubscriptionLimit() {
        val config = RateLimitConfig(
            subscriptionsPerSecond = 5,
            enabled = true
        )
        val limiter = RateLimiter(config)

        // Отправляем 5 подписок - должны пройти
        repeat(5) {
            assertTrue(limiter.checkSubscriptionLimit(), "Subscription $it should be allowed")
        }

        // 6-я подписка должна быть заблокирована
        assertFalse(limiter.checkSubscriptionLimit(), "6th subscription should be blocked")
    }

    @Test
    fun testBytesLimit() {
        val config = RateLimitConfig(
            bytesPerSecond = 1000, // 1KB/сек
            enabled = true
        )
        val limiter = RateLimiter(config)

        // Отправляем 500 байт - должно пройти
        assertTrue(limiter.checkBytesLimit(500), "500 bytes should be allowed")

        // Отправляем еще 400 байт - должно пройти (всего 900)
        assertTrue(limiter.checkBytesLimit(400), "400 bytes should be allowed")

        // Отправляем еще 200 байт - должно быть заблокировано (превысит лимит)
        assertFalse(limiter.checkBytesLimit(200), "200 bytes should be blocked (exceeds limit)")
    }

    @Test
    fun testTokenRefill() {
        val config = RateLimitConfig(
            messagesPerSecond = 10,
            enabled = true
        )
        val limiter = RateLimiter(config)

        // Исчерпываем лимит
        repeat(10) {
            limiter.checkMessageLimit()
        }
        assertFalse(limiter.checkMessageLimit(), "Should be blocked after limit")

        // Ждем пополнения токенов (в реальности нужно ждать 1 секунду)
        // Для теста просто проверяем, что механизм работает
        runBlocking {
            delay(1100) // Ждем больше 1 секунды
        }

        // После пополнения должно снова работать
        assertTrue(limiter.checkMessageLimit(), "Should work after refill")
    }

    @Test
    fun testMetrics() {
        val config = RateLimitConfig(
            messagesPerSecond = 5,
            subscriptionsPerSecond = 3,
            bytesPerSecond = 1000,
            enabled = true
        )
        val limiter = RateLimiter(config)

        // Выполняем операции
        repeat(3) { limiter.checkMessageLimit() }
        repeat(2) { limiter.checkSubscriptionLimit() }
        limiter.checkBytesLimit(500)

        // Пытаемся превысить лимиты
        limiter.checkMessageLimit() // 4-е сообщение
        limiter.checkMessageLimit() // 5-е сообщение
        limiter.checkMessageLimit() // 6-е - заблокировано
        limiter.checkSubscriptionLimit() // 3-я - заблокировано

        val metrics = limiter.getMetrics()

        assertTrue(metrics.blockedRequests > 0, "Should have blocked requests")
        assertTrue(metrics.currentMessageRate > 0, "Should have message rate")
        assertTrue(metrics.violations.isNotEmpty(), "Should have violations")
    }

    @Test
    fun testReset() {
        val config = RateLimitConfig(
            messagesPerSecond = 5,
            enabled = true
        )
        val limiter = RateLimiter(config)

        // Исчерпываем лимит
        repeat(5) {
            limiter.checkMessageLimit()
        }
        assertFalse(limiter.checkMessageLimit(), "Should be blocked")

        // Сбрасываем
        limiter.reset()

        // После сброса должно снова работать
        assertTrue(limiter.checkMessageLimit(), "Should work after reset")
    }

    @Test
    fun testDifferentLimits() {
        val config = RateLimitConfig(
            messagesPerSecond = 100,
            subscriptionsPerSecond = 10,
            bytesPerSecond = 1024 * 1024, // 1MB
            enabled = true
        )
        val limiter = RateLimiter(config)

        // Проверяем, что разные лимиты работают независимо
        assertTrue(limiter.checkMessageLimit(), "Message limit should work")
        assertTrue(limiter.checkSubscriptionLimit(), "Subscription limit should work")
        assertTrue(limiter.checkBytesLimit(512 * 1024), "Bytes limit should work")
    }
}
