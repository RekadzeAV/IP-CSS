package com.company.ipcamera.core.network

/**
 * Конфигурация rate limiting
 */
data class RateLimitConfig(
    /**
     * Лимит на отправку сообщений (сообщений в секунду)
     */
    val messagesPerSecond: Int = 100,

    /**
     * Лимит на подписки/отписки (операций в секунду)
     */
    val subscriptionsPerSecond: Int = 10,

    /**
     * Лимит на размер сообщений (байт в секунду)
     */
    val bytesPerSecond: Long = 10 * 1024 * 1024, // 10MB/сек

    /**
     * Размер окна для расчета лимитов (миллисекунды)
     */
    val windowSizeMillis: Long = 1000, // 1 секунда

    /**
     * Включен ли rate limiting
     */
    val enabled: Boolean = true
) {
    init {
        require(messagesPerSecond > 0) { "messagesPerSecond must be positive" }
        require(subscriptionsPerSecond > 0) { "subscriptionsPerSecond must be positive" }
        require(bytesPerSecond > 0) { "bytesPerSecond must be positive" }
        require(windowSizeMillis > 0) { "windowSizeMillis must be positive" }
    }
}
