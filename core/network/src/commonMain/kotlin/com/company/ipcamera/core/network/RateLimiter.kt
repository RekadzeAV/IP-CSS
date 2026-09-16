package com.company.ipcamera.core.network

import kotlinx.datetime.Clock
import mu.KotlinLogging
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * Нарушение rate limit
 */
data class RateLimitViolation(
    val timestamp: Long,
    val operationType: String,
    val limitType: String,
    val currentRate: Double,
    val limit: Double
)

/**
 * Метрики rate limiting
 */
data class RateLimitMetrics(
    val blockedRequests: Long,
    val currentMessageRate: Double, // Текущая скорость (сообщений/сек)
    val currentSubscriptionRate: Double, // Текущая скорость (подписок/сек)
    val currentBytesRate: Double, // Текущая скорость (байт/сек)
    val violations: List<RateLimitViolation>
)

/**
 * Token Bucket для rate limiting
 */
private class TokenBucket(
    private val capacity: Double,
    private val refillRate: Double, // Токенов в секунду
    private val windowSizeMillis: Long
) {
    private var tokens: Double = capacity
    private var lastRefillTime: Long = Clock.System.now().toEpochMilliseconds()
    private var consumed: Long = 0
    private var blocked: Long = 0

    /**
     * Попытаться потребить токены
     */
    fun tryConsume(amount: Double = 1.0): Boolean {
        refill()
        return if (tokens >= amount) {
            tokens -= amount
            consumed++
            true
        } else {
            blocked++
            false
        }
    }

    /**
     * Пополнить токены
     */
    private fun refill() {
        val now = Clock.System.now().toEpochMilliseconds()
        val elapsed = (now - lastRefillTime) / 1000.0 // в секундах

        if (elapsed > 0) {
            val tokensToAdd = elapsed * refillRate
            tokens = min(capacity, tokens + tokensToAdd)
            lastRefillTime = now
        }
    }

    /**
     * Получить текущее количество токенов
     */
    fun getCurrentTokens(): Double {
        refill()
        return tokens
    }

    /**
     * Получить текущую скорость потребления
     */
    fun getCurrentRate(): Double {
        return consumed.toDouble()
    }

    /**
     * Сбросить счетчики
     */
    fun reset() {
        tokens = capacity
        lastRefillTime = Clock.System.now().toEpochMilliseconds()
        consumed = 0
        blocked = 0
    }

    fun getBlockedCount(): Long = blocked
}

/**
 * Rate Limiter с алгоритмом Token Bucket
 */
class RateLimiter(
    private val config: RateLimitConfig
) {
    private val messageBucket = TokenBucket(
        capacity = config.messagesPerSecond.toDouble(),
        refillRate = config.messagesPerSecond.toDouble(),
        windowSizeMillis = config.windowSizeMillis
    )

    private val subscriptionBucket = TokenBucket(
        capacity = config.subscriptionsPerSecond.toDouble(),
        refillRate = config.subscriptionsPerSecond.toDouble(),
        windowSizeMillis = config.windowSizeMillis
    )

    private val bytesBucket = TokenBucket(
        capacity = config.bytesPerSecond.toDouble(),
        refillRate = config.bytesPerSecond.toDouble(),
        windowSizeMillis = config.windowSizeMillis
    )

    private val violations = mutableListOf<RateLimitViolation>()

    /**
     * Проверить лимит для отправки сообщения
     */
    fun checkMessageLimit(): Boolean {
        if (!config.enabled) return true

        val allowed = messageBucket.tryConsume(1.0)
        if (!allowed) {
            recordViolation(
                "message",
                "messages_per_second",
                messageBucket.getCurrentRate(),
                config.messagesPerSecond.toDouble()
            )
            logger.warn { "Message rate limit exceeded: ${messageBucket.getCurrentRate()}/${config.messagesPerSecond} msg/s" }
        }
        return allowed
    }

    /**
     * Проверить лимит для подписки/отписки
     */
    fun checkSubscriptionLimit(): Boolean {
        if (!config.enabled) return true

        val allowed = subscriptionBucket.tryConsume(1.0)
        if (!allowed) {
            recordViolation(
                "subscription",
                "subscriptions_per_second",
                subscriptionBucket.getCurrentRate(),
                config.subscriptionsPerSecond.toDouble()
            )
            logger.warn { "Subscription rate limit exceeded: ${subscriptionBucket.getCurrentRate()}/${config.subscriptionsPerSecond} ops/s" }
        }
        return allowed
    }

    /**
     * Проверить лимит для размера сообщения
     */
    fun checkBytesLimit(size: Long): Boolean {
        if (!config.enabled) return true

        val allowed = bytesBucket.tryConsume(size.toDouble())
        if (!allowed) {
            recordViolation("bytes", "bytes_per_second", bytesBucket.getCurrentRate(), config.bytesPerSecond.toDouble())
            logger.warn { "Bytes rate limit exceeded: ${bytesBucket.getCurrentRate()}/${config.bytesPerSecond} bytes/s" }
        }
        return allowed
    }

    /**
     * Записать нарушение лимита
     */
    private fun recordViolation(operationType: String, limitType: String, currentRate: Double, limit: Double) {
        violations.add(
            RateLimitViolation(
                timestamp = Clock.System.now().toEpochMilliseconds(),
                operationType = operationType,
                limitType = limitType,
                currentRate = currentRate,
                limit = limit
            )
        )

        // Ограничиваем размер списка нарушений
        if (violations.size > 1000) {
            violations.removeAt(0)
        }
    }

    /**
     * Получить метрики
     */
    fun getMetrics(): RateLimitMetrics {
        return RateLimitMetrics(
            blockedRequests = messageBucket.getBlockedCount() +
                subscriptionBucket.getBlockedCount() +
                bytesBucket.getBlockedCount(),
            currentMessageRate = messageBucket.getCurrentRate(),
            currentSubscriptionRate = subscriptionBucket.getCurrentRate(),
            currentBytesRate = bytesBucket.getCurrentRate(),
            violations = violations.toList()
        )
    }

    /**
     * Сбросить лимиты
     */
    fun reset() {
        messageBucket.reset()
        subscriptionBucket.reset()
        bytesBucket.reset()
        violations.clear()
        logger.debug { "Rate limiter reset" }
    }
}
