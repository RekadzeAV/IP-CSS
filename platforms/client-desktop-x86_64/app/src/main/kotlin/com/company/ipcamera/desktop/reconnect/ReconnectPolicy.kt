package com.company.ipcamera.desktop.reconnect

import kotlin.math.pow

/**
 * Стратегия экспоненциального backoff для reconnect RTSP потоков.
 */
enum class ReconnectBackoffStrategy {
    /**
     * Линейный backoff: delay = initialDelay * attempt
     */
    LINEAR,

    /**
     * Экспоненциальный backoff: delay = initialDelay * (2 ^ (attempt - 1))
     */
    EXPONENTIAL,

    /**
     * Фиксированная задержка между попытками
     */
    FIXED
}

/**
 * Конфигурация политики reconnect для RTSP потока.
 */
data class ReconnectPolicy(
    val enabled: Boolean = true,
    val maxAttempts: Int = 5,
    val initialDelayMs: Long = 1_000L,
    val maxDelayMs: Long = 30_000L,
    val backoffStrategy: ReconnectBackoffStrategy = ReconnectBackoffStrategy.EXPONENTIAL,
    val backoffMultiplier: Double = 2.0,
    val jitterRatio: Double = 0.1,
    val retryOnTimeout: Boolean = true,
    val retryOnConnectionRefused: Boolean = true,
    val retryOnServerUnavailable: Boolean = true
) {
    companion object {
        /**
         * Консервативная политика: больше задержки, меньше попыток.
         */
        val CONSERVATIVE = ReconnectPolicy(
            enabled = true,
            maxAttempts = 3,
            initialDelayMs = 3_000L,
            maxDelayMs = 60_000L,
            backoffStrategy = ReconnectBackoffStrategy.EXPONENTIAL,
            backoffMultiplier = 2.5,
            jitterRatio = 0.2
        )

        /**
         * Сбалансированная политика: умеренные задержки и попытки.
         */
        val BALANCED = ReconnectPolicy(
            enabled = true,
            maxAttempts = 5,
            initialDelayMs = 1_500L,
            maxDelayMs = 30_000L,
            backoffStrategy = ReconnectBackoffStrategy.EXPONENTIAL,
            backoffMultiplier = 2.0,
            jitterRatio = 0.1
        )

        /**
         * Агрессивная политика: быстрые повторные попытки.
         */
        val AGGRESSIVE = ReconnectPolicy(
            enabled = true,
            maxAttempts = 10,
            initialDelayMs = 500L,
            maxDelayMs = 15_000L,
            backoffStrategy = ReconnectBackoffStrategy.EXPONENTIAL,
            backoffMultiplier = 1.5,
            jitterRatio = 0.05
        )

        /**
         * Политика без повторных попыток.
         */
        val NONE = ReconnectPolicy(
            enabled = false,
            maxAttempts = 0,
            initialDelayMs = 0L,
            maxDelayMs = 0L,
            backoffStrategy = ReconnectBackoffStrategy.FIXED,
            backoffMultiplier = 1.0,
            jitterRatio = 0.0
        )

        /**
         * Создать политику с валидацией.
         */
        fun create(
            enabled: Boolean = true,
            maxAttempts: Int = 5,
            initialDelayMs: Long = 1_500L,
            maxDelayMs: Long = 30_000L,
            backoffStrategy: ReconnectBackoffStrategy = ReconnectBackoffStrategy.EXPONENTIAL,
            backoffMultiplier: Double = 2.0,
            jitterRatio: Double = 0.1,
            retryOnTimeout: Boolean = true,
            retryOnConnectionRefused: Boolean = true,
            retryOnServerUnavailable: Boolean = true
        ): ReconnectPolicy {
            require(!enabled || maxAttempts >= 1) { "maxAttempts must be >= 1 when enabled" }
            require(!enabled || initialDelayMs >= 100) { "initialDelayMs must be >= 100ms when enabled" }
            require(!enabled || maxDelayMs >= initialDelayMs) { "maxDelayMs must be >= initialDelayMs when enabled" }
            require(backoffMultiplier in 1.0..10.0) { "backoffMultiplier must be in range [1.0, 10.0]" }
            require(jitterRatio in 0.0..0.5) { "jitterRatio must be in range [0.0, 0.5]" }

            return ReconnectPolicy(
                enabled = enabled,
                maxAttempts = maxAttempts,
                initialDelayMs = initialDelayMs,
                maxDelayMs = maxDelayMs,
                backoffStrategy = backoffStrategy,
                backoffMultiplier = backoffMultiplier,
                jitterRatio = jitterRatio,
                retryOnTimeout = retryOnTimeout,
                retryOnConnectionRefused = retryOnConnectionRefused,
                retryOnServerUnavailable = retryOnServerUnavailable
            )
        }

        /**
         * Создать политику из переменных окружения.
         */
        fun fromEnvironment(): ReconnectPolicy {
            val enabled = System.getenv("IPCSS_RECONNECT_ENABLED")?.toBoolean() ?: true
            val maxAttempts = System.getenv("IPCSS_RECONNECT_MAX_ATTEMPTS")?.toIntOrNull()?.coerceIn(1, 20) ?: 5
            val initialDelayMs = System.getenv("IPCSS_RECONNECT_INITIAL_DELAY_MS")?.toLongOrNull()?.coerceIn(100L, 10_000L) ?: 1_500L
            val maxDelayMs = System.getenv("IPCSS_RECONNECT_MAX_DELAY_MS")?.toLongOrNull()?.coerceIn(1000L, 120_000L) ?: 30_000L
            
            val strategy = when (System.getenv("IPCSS_RECONNECT_BACKOFF_STRATEGY")?.uppercase()) {
                "LINEAR" -> ReconnectBackoffStrategy.LINEAR
                "FIXED" -> ReconnectBackoffStrategy.FIXED
                else -> ReconnectBackoffStrategy.EXPONENTIAL
            }

            val multiplier = System.getenv("IPCSS_RECONNECT_BACKOFF_MULTIPLIER")?.toDoubleOrNull()?.coerceIn(1.0, 10.0) ?: 2.0
            val jitter = System.getenv("IPCSS_RECONNECT_JITTER_RATIO")?.toDoubleOrNull()?.coerceIn(0.0, 0.5) ?: 0.1

            return create(
                enabled = enabled,
                maxAttempts = maxAttempts,
                initialDelayMs = initialDelayMs,
                maxDelayMs = maxDelayMs.coerceAtLeast(initialDelayMs),
                backoffStrategy = strategy,
                backoffMultiplier = multiplier,
                jitterRatio = jitter
            )
        }
    }
}

/**
 * Рассчитать задержку перед следующей попыткой reconnect.
 */
fun calculateReconnectDelay(
    attempt: Int,
    policy: ReconnectPolicy
): Long {
    if (!policy.enabled || attempt <= 0) {
        return 0L
    }

    val baseDelay = when (policy.backoffStrategy) {
        ReconnectBackoffStrategy.FIXED -> policy.initialDelayMs
        ReconnectBackoffStrategy.LINEAR -> policy.initialDelayMs * attempt
        ReconnectBackoffStrategy.EXPONENTIAL -> {
            // delay = initialDelay * (multiplier ^ (attempt - 1))
            val exponent = kotlin.math.min(attempt - 1, 30) // Ограничить exponent для предотвращения переполнения
            (policy.initialDelayMs * policy.backoffMultiplier.pow(exponent.toDouble())).toLong()
        }
    }

    val clampedDelay = baseDelay.coerceAtMost(policy.maxDelayMs)

    // Добавляем jitter для предотвращения thundering herd
    if (policy.jitterRatio > 0.0) {
        val jitterRange = (clampedDelay * policy.jitterRatio).toLong()
        if (jitterRange > 0) {
            val randomJitter = (0..jitterRange * 2).random() - jitterRange
            return (clampedDelay + randomJitter).coerceAtLeast(policy.initialDelayMs)
        }
    }

    return clampedDelay
}

/**
 * Определить тип ошибки из сообщения.
 */
enum class StreamErrorType {
    TIMEOUT,
    CONNECTION_REFUSED,
    SERVER_UNAVAILABLE,
    NETWORK_ERROR,
    UNKNOWN
}

fun classifyStreamError(error: String?): StreamErrorType {
    if (error == null) return StreamErrorType.UNKNOWN
    
    val lower = error.lowercase()
    return when {
        lower.contains("timeout") || lower.contains("timed out") -> StreamErrorType.TIMEOUT
        lower.contains("connection refused") || lower.contains("connection reset") -> StreamErrorType.CONNECTION_REFUSED
        lower.contains("unavailable") || lower.contains("503") || lower.contains("service unavailable") -> StreamErrorType.SERVER_UNAVAILABLE
        lower.contains("network") || lower.contains("socket") || lower.contains("io") -> StreamErrorType.NETWORK_ERROR
        else -> StreamErrorType.UNKNOWN
    }
}

/**
 * Определить, нужно ли пытаться переподключиться для данной ошибки.
 */
fun shouldRetryError(error: String?, policy: ReconnectPolicy): Boolean {
    if (!policy.enabled) return false
    
    val errorType = classifyStreamError(error)
    return when (errorType) {
        StreamErrorType.TIMEOUT -> policy.retryOnTimeout
        StreamErrorType.CONNECTION_REFUSED -> policy.retryOnConnectionRefused
        StreamErrorType.SERVER_UNAVAILABLE -> policy.retryOnServerUnavailable
        StreamErrorType.NETWORK_ERROR -> true // Всегда пытаемся при сетевых ошибках
        StreamErrorType.UNKNOWN -> true // Пытаемся по умолчанию
    }
}
