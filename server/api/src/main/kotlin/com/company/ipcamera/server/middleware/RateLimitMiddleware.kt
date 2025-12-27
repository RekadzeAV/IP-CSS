package com.company.ipcamera.server.middleware

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

/**
 * Простой rate limiter для защиты от брутфорса
 * 
 * TODO: Мигрировать на Redis для распределенных систем
 * TODO: Добавить поддержку различных лимитов для разных endpoints
 */
class RateLimitMiddleware {
    // Хранилище попыток: IP -> (timestamp, count)
    private val attempts = ConcurrentHashMap<String, MutableList<Long>>()
    private val mutex = Mutex()
    
    // Конфигурация
    private val maxAttempts = 5 // Максимальное количество попыток
    private val windowDuration: Duration = 15.minutes // Окно времени
    private val blockDuration: Duration = 30.minutes // Время блокировки
    
    /**
     * Проверяет, не превышен ли лимит запросов
     * @param identifier Идентификатор (IP адрес или user ID)
     * @return true, если запрос разрешен, false если превышен лимит
     */
    suspend fun checkLimit(identifier: String): Boolean = mutex.withLock {
        val now = System.currentTimeMillis()
        val windowStart = now - windowDuration.inWholeMilliseconds
        val blockStart = now - blockDuration.inWholeMilliseconds
        
        val userAttempts = attempts.getOrPut(identifier) { mutableListOf() }
        
        // Удаляем старые попытки (старше окна)
        userAttempts.removeAll { it < windowStart }
        
        // Проверяем, не заблокирован ли пользователь
        val recentAttempts = userAttempts.filter { it > blockStart }
        if (recentAttempts.size >= maxAttempts) {
            logger.warn { "Rate limit exceeded for identifier: $identifier (${recentAttempts.size} attempts)" }
            return false
        }
        
        // Добавляем текущую попытку
        userAttempts.add(now)
        
        // Очищаем старые записи (оптимизация памяти)
        if (attempts.size > 10000) {
            cleanupOldEntries(now)
        }
        
        return true
    }
    
    /**
     * Очищает старые записи из памяти
     */
    private fun cleanupOldEntries(now: Long) {
        val cutoff = now - blockDuration.inWholeMilliseconds
        attempts.entries.removeIf { entry ->
            entry.value.removeAll { it < cutoff }
            entry.value.isEmpty()
        }
    }
    
    /**
     * Сбрасывает счетчик для идентификатора (например, после успешного входа)
     */
    suspend fun resetLimit(identifier: String) = mutex.withLock {
        attempts.remove(identifier)
    }
}

/**
 * Extension функция для проверки rate limit в маршрутах
 */
suspend fun ApplicationCall.checkRateLimit(identifier: String, rateLimiter: RateLimitMiddleware): Boolean {
    if (!rateLimiter.checkLimit(identifier)) {
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

