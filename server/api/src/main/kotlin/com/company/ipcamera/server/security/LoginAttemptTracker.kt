package com.company.ipcamera.server.security

import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.delay
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * Трекер неудачных попыток входа для защиты от перечисления пользователей
 * и брутфорс атак
 */
class LoginAttemptTracker(
    private val redisCommands: RedisCoroutinesCommands<String, String>
) {
    companion object {
        private const val REDIS_KEY_PREFIX = "login_attempts:"
        private const val MAX_FAILED_ATTEMPTS = 5 // После 5 неудачных попыток требуется CAPTCHA
        private const val CAPTCHA_REQUIRED_ATTEMPTS = 3 // После 3 неудачных попыток требуется CAPTCHA
        private const val ATTEMPT_WINDOW_MINUTES = 15L // Окно для подсчета попыток
        private const val BASE_DELAY_MS = 1000L // Базовая задержка 1 секунда
    }

    /**
     * Регистрирует неудачную попытку входа
     *
     * @param identifier Идентификатор (IP адрес или комбинация IP + username)
     * @return Количество неудачных попыток и требуется ли CAPTCHA
     */
    suspend fun recordFailedAttempt(identifier: String): LoginAttemptInfo {
        return try {
            val redisKey = "$REDIS_KEY_PREFIX$identifier"
            val now = System.currentTimeMillis()

            // Добавляем текущую попытку в sorted set
            val member = "$now:${System.nanoTime()}"
            redisCommands.zadd(redisKey, now.toDouble(), member)

            // Удаляем старые попытки (старше окна)
            val windowStart = now - (ATTEMPT_WINDOW_MINUTES * 60 * 1000)
            val range = io.lettuce.core.Range.create(Double.NEGATIVE_INFINITY, windowStart.toDouble())
            redisCommands.zremrangebyscore(redisKey, range)

            // Подсчитываем количество попыток
            val attemptCount = (redisCommands.zcard(redisKey)?.toInt() ?: 0)

            // Устанавливаем TTL (30 минут)
            redisCommands.expire(redisKey, 30 * 60)

            val requiresCaptcha = attemptCount >= CAPTCHA_REQUIRED_ATTEMPTS
            val isBlocked = attemptCount >= MAX_FAILED_ATTEMPTS

            LoginAttemptInfo(
                attemptCount = attemptCount,
                requiresCaptcha = requiresCaptcha,
                isBlocked = isBlocked,
                delayMs = calculateDelay(attemptCount)
            )
        } catch (e: Exception) {
            logger.error(e) { "Error recording failed login attempt for identifier: $identifier" }
            // В случае ошибки возвращаем безопасные значения
            LoginAttemptInfo(
                attemptCount = 0,
                requiresCaptcha = false,
                isBlocked = false,
                delayMs = 0
            )
        }
    }

    /**
     * Сбрасывает счетчик неудачных попыток (после успешного входа)
     */
    suspend fun resetAttempts(identifier: String) {
        try {
            val redisKey = "$REDIS_KEY_PREFIX$identifier"
            redisCommands.del(redisKey)
            logger.debug { "Login attempts reset for identifier: $identifier" }
        } catch (e: Exception) {
            logger.error(e) { "Error resetting login attempts for identifier: $identifier" }
        }
    }

    /**
     * Получает информацию о текущих попытках без регистрации новой
     */
    suspend fun getAttemptInfo(identifier: String): LoginAttemptInfo {
        return try {
            val redisKey = "$REDIS_KEY_PREFIX$identifier"
            val now = System.currentTimeMillis()

            // Удаляем старые попытки
            val windowStart = now - (ATTEMPT_WINDOW_MINUTES * 60 * 1000)
            val range = io.lettuce.core.Range.create(Double.NEGATIVE_INFINITY, windowStart.toDouble())
            redisCommands.zremrangebyscore(redisKey, range)

            val attemptCount = (redisCommands.zcard(redisKey)?.toInt() ?: 0)
            val requiresCaptcha = attemptCount >= CAPTCHA_REQUIRED_ATTEMPTS
            val isBlocked = attemptCount >= MAX_FAILED_ATTEMPTS

            LoginAttemptInfo(
                attemptCount = attemptCount,
                requiresCaptcha = requiresCaptcha,
                isBlocked = isBlocked,
                delayMs = calculateDelay(attemptCount)
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting login attempt info for identifier: $identifier" }
            LoginAttemptInfo(
                attemptCount = 0,
                requiresCaptcha = false,
                isBlocked = false,
                delayMs = 0
            )
        }
    }

    /**
     * Вычисляет задержку на основе количества неудачных попыток (exponential backoff)
     *
     * Формула: delay = BASE_DELAY * 2^(attemptCount - 1)
     * Максимальная задержка: 10 секунд
     */
    private fun calculateDelay(attemptCount: Int): Long {
        if (attemptCount <= 0) return 0

        val delay = BASE_DELAY_MS * (1 shl (attemptCount - 1).coerceAtMost(10))
        return delay.coerceAtMost(10000) // Максимум 10 секунд
    }

    /**
     * Применяет задержку перед ответом (для защиты от timing атак)
     */
    suspend fun applyDelay(attemptCount: Int) {
        val delayMs = calculateDelay(attemptCount)
        if (delayMs > 0) {
            delay(delayMs.milliseconds)
        }
    }
}

/**
 * Информация о попытках входа
 */
data class LoginAttemptInfo(
    val attemptCount: Int,
    val requiresCaptcha: Boolean,
    val isBlocked: Boolean,
    val delayMs: Long
)
