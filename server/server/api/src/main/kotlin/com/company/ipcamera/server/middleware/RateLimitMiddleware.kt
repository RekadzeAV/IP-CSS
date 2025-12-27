package com.company.ipcamera.server.middleware

import com.company.ipcamera.server.config.RedisConfig
import com.company.ipcamera.server.security.SecurityLogger
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

/**
 * Rate limiter для защиты от брутфорса и DDoS атак
 *
 * Использует Redis для распределенного rate limiting в кластерных системах
 */
class RateLimitMiddleware(
    private val redisCommands: RedisCoroutinesCommands<String, String> = RedisConfig.getCommands()
) {
    companion object {
        private const val REDIS_KEY_PREFIX = "rate_limit:"
    }

    /**
     * Конфигурация лимитов для разных типов endpoints
     */
    data class RateLimitConfig(
        val maxAttempts: Int,
        val windowDuration: Duration,
        val blockDuration: Duration
    )

    // Различные конфигурации для разных endpoints
    val loginConfig = RateLimitConfig(
        maxAttempts = 5,
        windowDuration = 15.minutes,
        blockDuration = 30.minutes
    )

    val generalConfig = RateLimitConfig(
        maxAttempts = 100,
        windowDuration = 1.minutes,
        blockDuration = 5.minutes
    )

    val registrationConfig = RateLimitConfig(
        maxAttempts = 3,
        windowDuration = 60.minutes,
        blockDuration = 120.minutes
    )

    /**
     * Проверяет, не превышен ли лимит запросов
     * Использует sliding window алгоритм с Redis Sorted Set
     *
     * @param identifier Идентификатор (IP адрес или user ID)
     * @param config Конфигурация лимита (по умолчанию общая)
     * @return Pair<Boolean, RateLimitInfo> - разрешен ли запрос и информация о лимите
     */
    suspend fun checkLimit(
        identifier: String,
        config: RateLimitConfig = generalConfig
    ): Pair<Boolean, RateLimitInfo> {
        return try {
            val now = System.currentTimeMillis()
            val windowStart = now - config.windowDuration.inWholeMilliseconds
            val redisKey = "$REDIS_KEY_PREFIX$identifier"

            // Используем Redis Sorted Set для хранения временных меток попыток
            // Score = timestamp, Member = unique identifier (timestamp + random)
            val member = "$now:${System.nanoTime()}"

            // Добавляем текущую попытку
            redisCommands.zadd(redisKey, now.toDouble(), member)

            // Удаляем старые попытки (старше окна)
            // Используем Range для удаления элементов с score от -inf до windowStart
            val range = io.lettuce.core.Range.create(
                io.lettuce.core.Range.Boundary.unbounded(),
                io.lettuce.core.Range.Boundary.including(windowStart.toDouble())
            )
            redisCommands.zremrangebyscore(redisKey, range)

            // Подсчитываем количество попыток в окне
            val attemptCount = redisCommands.zcard(redisKey)?.toInt() ?: 0

            // Устанавливаем TTL для ключа (чтобы он автоматически удалился после blockDuration)
            val ttlSeconds = config.blockDuration.inWholeSeconds.toInt()
            redisCommands.expire(redisKey, ttlSeconds.toLong())

            val rateLimitInfo = RateLimitInfo(
                limit = config.maxAttempts,
                remaining = (config.maxAttempts - attemptCount).coerceAtLeast(0),
                resetTime = now + config.windowDuration.inWholeMilliseconds
            )

            if (attemptCount > config.maxAttempts) {
                logger.warn {
                    "Rate limit exceeded for identifier: $identifier (${attemptCount}/${config.maxAttempts} attempts)"
                }
                // Логирование в security logger будет выполнено из checkRateLimit с информацией об endpoint
                return Pair(false, rateLimitInfo)
            }

            Pair(true, rateLimitInfo.copy(remaining = rateLimitInfo.remaining - 1))
        } catch (e: Exception) {
            logger.error(e) { "Error checking rate limit for identifier: $identifier" }
            // В случае ошибки Redis разрешаем запрос (fail-open для доступности)
            // В продакшене можно использовать fail-closed для большей безопасности
            val now = System.currentTimeMillis()
            Pair(true, RateLimitInfo(
                limit = config.maxAttempts,
                remaining = config.maxAttempts,
                resetTime = now + config.windowDuration.inWholeMilliseconds
            ))
        }
    }

    /**
     * Информация о текущем состоянии rate limit
     */
    data class RateLimitInfo(
        val limit: Int,
        val remaining: Int,
        val resetTime: Long
    )

    /**
     * Сбрасывает счетчик для идентификатора (например, после успешного входа)
     */
    suspend fun resetLimit(identifier: String) {
        try {
            val redisKey = "$REDIS_KEY_PREFIX$identifier"
            redisCommands.del(redisKey)
            logger.debug { "Rate limit reset for identifier: $identifier" }
        } catch (e: Exception) {
            logger.error(e) { "Error resetting rate limit for identifier: $identifier" }
        }
    }
}

/**
 * Extension функция для проверки rate limit в маршрутах
 * @param identifier Идентификатор (IP адрес или user ID)
 * @param rateLimiter Экземпляр RateLimitMiddleware
 * @param config Конфигурация лимита (по умолчанию общая)
 * @return true, если запрос разрешен, false если превышен лимит
 */
suspend fun ApplicationCall.checkRateLimit(
    identifier: String,
    rateLimiter: RateLimitMiddleware,
    config: RateLimitMiddleware.RateLimitConfig = rateLimiter.generalConfig
): Boolean {
    val (allowed, rateLimitInfo) = rateLimiter.checkLimit(identifier, config)

    // Добавляем заголовки rate limit
    response.headers.append("X-RateLimit-Limit", rateLimitInfo.limit.toString())
    response.headers.append("X-RateLimit-Remaining", rateLimitInfo.remaining.toString())
    response.headers.append("X-RateLimit-Reset", (rateLimitInfo.resetTime / 1000).toString())

    if (!allowed) {
        val ipAddress = request.origin.remoteHost
        val endpoint = request.path()
        SecurityLogger.logRateLimitExceeded(identifier, ipAddress, endpoint)

        respond(
            io.ktor.http.HttpStatusCode.TooManyRequests,
            com.company.ipcamera.server.dto.ApiResponse<Unit>(
                success = false,
                data = null,
                message = "Too many requests. Please try again later."
            )
        )
        return false
    }
    return true
}

