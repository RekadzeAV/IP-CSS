package com.company.ipcamera.core.security

import kotlinx.serialization.Serializable

/**
 * Интерфейс для защиты от брутфорс атак
 * Отслеживает попытки входа и блокирует подозрительных пользователей/IP
 *
 * Семантика (согласована между всеми платформенными реализациями):
 * - неудачные попытки учитываются в скользящем окне [BruteForceConfig.timeWindowMinutes];
 * - при достижении [BruteForceConfig.maxFailedAttempts] пользователь/IP блокируется
 *   на [BruteForceConfig.lockoutDurationMinutes];
 * - успешный вход сбрасывает счётчики; активная блокировка снимается только по истечении срока
 *   (защита от «успешного» запроса во время блокировки).
 */
interface BruteForceProtection {
    /**
     * Зарегистрировать попытку входа
     */
    fun recordLoginAttempt(username: String?, ipAddress: String?, success: Boolean)

    /**
     * Проверить, заблокирован ли пользователь
     */
    fun isUserBlocked(username: String): Boolean

    /**
     * Проверить, заблокирован ли IP
     */
    fun isIpBlocked(ipAddress: String): Boolean

    /**
     * Получить время разблокировки пользователя (null — не заблокирован)
     */
    fun getUserUnlockTime(username: String): Long?

    /**
     * Получить время разблокировки IP (null — не заблокирован)
     */
    fun getIpUnlockTime(ipAddress: String): Long?

    /**
     * Сбросить счетчик неудачных попыток
     */
    fun resetFailedAttempts(username: String)

    /**
     * Сбросить счетчик неудачных попыток для IP
     */
    fun resetIpFailedAttempts(ipAddress: String)

    /**
     * Получить количество неудачных попыток (в текущем окне)
     */
    fun getFailedAttempts(username: String): Int
}

/**
 * expect класс для защиты от брутфорс
 */
expect class BruteForceProtectionManager : BruteForceProtection {
    constructor()
    constructor(config: BruteForceConfig)
}

/**
 * Фабрика для создания BruteForceProtection
 */
object BruteForceProtectionFactory {
    fun create(): BruteForceProtection {
        return BruteForceProtectionManager()
    }

    /**
     * Создание с кастомной конфигурацией (конфиг применяется реализацией)
     */
    fun create(config: BruteForceConfig): BruteForceProtection {
        return BruteForceProtectionManager(config)
    }
}

/**
 * Конфигурация защиты от брутфорс
 */
@Serializable
data class BruteForceConfig(
    /**
     * Максимальное количество неудачных попыток
     */
    val maxFailedAttempts: Int = 5,

    /**
     * Время блокировки в минутах
     */
    val lockoutDurationMinutes: Long = 30,

    /**
     * Окно времени для подсчета попыток в минутах
     */
    val timeWindowMinutes: Long = 15,

    /**
     * Включить блокировку по IP
     */
    val enableIpBlocking: Boolean = true,

    /**
     * Включить блокировку по username
     */
    val enableUsernameBlocking: Boolean = true
) {
    init {
        require(maxFailedAttempts > 0) { "maxFailedAttempts must be positive" }
        require(lockoutDurationMinutes >= 0) { "lockoutDurationMinutes must be non-negative" }
        require(timeWindowMinutes > 0) { "timeWindowMinutes must be positive" }
    }
}
