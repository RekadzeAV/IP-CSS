package com.company.ipcamera.core.network.ratelimit

import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * Rate limiter для ограничения частоты запросов
 */
interface RateLimiter {
    /**
     * Проверить, можно ли выполнить запрос
     * @return true если запрос разрешен, false если нужно подождать
     */
    suspend fun acquire(): Boolean

    /**
     * Получить время ожидания до следующего разрешенного запроса
     */
    fun getWaitTime(): Duration
}

/**
 * Простой rate limiter на основе токенов
 */
class TokenBucketRateLimiter(
    private val maxTokens: Int = 10,
    private val refillRate: Duration = Duration.parse("1s"),
    private val tokensPerRefill: Int = 1
) : RateLimiter {
    private var tokens: Int = maxTokens
    private var lastRefillTime: Long = System.currentTimeMillis()

    override suspend fun acquire(): Boolean {
        refillTokens()

        if (tokens > 0) {
            tokens--
            return true
        }

        return false
    }

    override fun getWaitTime(): Duration {
        refillTokens()

        if (tokens > 0) {
            return Duration.ZERO
        }

        // Вычисляем время до следующего пополнения
        val timeSinceLastRefill = System.currentTimeMillis() - lastRefillTime
        val refillInterval = refillRate.inWholeMilliseconds

        if (timeSinceLastRefill >= refillInterval) {
            return Duration.ZERO
        }

        return Duration.parse("${refillInterval - timeSinceLastRefill}ms")
    }

    private fun refillTokens() {
        val now = System.currentTimeMillis()
        val timeSinceLastRefill = now - lastRefillTime
        val refillInterval = refillRate.inWholeMilliseconds

        if (timeSinceLastRefill >= refillInterval) {
            val refills = (timeSinceLastRefill / refillInterval).toInt()
            tokens = (tokens + refills * tokensPerRefill).coerceAtMost(maxTokens)
            lastRefillTime = now
        }
    }
}

/**
 * Rate limiter на основе фиксированного интервала
 */
class FixedIntervalRateLimiter(
    private val interval: Duration,
    private val maxRequests: Int = 1
) : RateLimiter {
    private val requestTimes = mutableListOf<Long>()

    override suspend fun acquire(): Boolean {
        val now = System.currentTimeMillis()
        val intervalMs = interval.inWholeMilliseconds

        // Удаляем старые запросы
        requestTimes.removeAll { now - it > intervalMs }

        if (requestTimes.size < maxRequests) {
            requestTimes.add(now)
            return true
        }

        return false
    }

    override fun getWaitTime(): Duration {
        val now = System.currentTimeMillis()
        val intervalMs = interval.inWholeMilliseconds

        // Удаляем старые запросы
        requestTimes.removeAll { now - it > intervalMs }

        if (requestTimes.size < maxRequests) {
            return Duration.ZERO
        }

        // Время до освобождения слота
        val oldestRequest = requestTimes.minOrNull() ?: return Duration.ZERO
        val waitMs = intervalMs - (now - oldestRequest)
        return Duration.parse("${waitMs.coerceAtLeast(0)}ms")
    }
}

