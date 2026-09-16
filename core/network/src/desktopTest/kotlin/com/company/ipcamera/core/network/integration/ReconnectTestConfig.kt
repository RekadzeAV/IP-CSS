package com.company.ipcamera.core.network.integration

/**
 * Конфигурация для Reconnect Integration тестов
 */
data class ReconnectTestConfig(
    val serverUrl: String = "rtsp://localhost:8554/test",
    val reconnectEnabled: Boolean = true,
    val reconnectMaxRetries: Int = 5,
    val reconnectInitialDelayMs: Int = 1000,
    val reconnectMaxDelayMs: Int = 10000,
    val reconnectBackoffMultiplier: Float = 2.0f,
    val connectionTimeoutMs: Int = 5000,
    val playbackTimeoutMs: Int = 10000
) {
    companion object {
        /**
         * Агрессивная политика reconnect (много попыток, короткие задержки)
         */
        fun aggressive(): ReconnectTestConfig = ReconnectTestConfig(
            reconnectMaxRetries = 10,
            reconnectInitialDelayMs = 500,
            reconnectMaxDelayMs = 5000,
            reconnectBackoffMultiplier = 1.5f
        )

        /**
         * Консервативная политика reconnect (мало попыток, длинные задержки)
         */
        fun conservative(): ReconnectTestConfig = ReconnectTestConfig(
            reconnectMaxRetries = 3,
            reconnectInitialDelayMs = 5000,
            reconnectMaxDelayMs = 30000,
            reconnectBackoffMultiplier = 2.0f
        )

        /**
         * Тестовая конфигурация по умолчанию
         */
        fun test(): ReconnectTestConfig = ReconnectTestConfig(
            serverUrl = "rtsp://localhost:8554/test",
            reconnectMaxRetries = 5,
            reconnectInitialDelayMs = 1000
        )
    }
}

/**
 * Результат теста reconnect
 */
data class ReconnectTestResult(
    val success: Boolean,
    val reconnectAttempts: Int,
    val totalReconnectTimeMs: Int,
    val finalStatus: String,
    val errorMessage: String? = null
)

/**
 * Статус тестовой среды RTSP
 */
enum class ServerHealthStatus {
    HEALTHY,
    UNHEALTHY,
    STOPPED,
    UNKNOWN
}
