package com.company.ipcamera.server.config

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация подписок ONVIF Event Service.
 * Загружается из переменных окружения.
 */
data class OnvifEventsConfig(
    /** Включены ли авто-подписки на события ONVIF при старте API и при добавлении камеры */
    val enabled: Boolean = true,
    /** Время жизни подписки в секундах (для Renew) */
    val subscriptionTimeSeconds: Int = 3600,
    /** Интервал опроса PullPoint в миллисекундах */
    val pullIntervalMs: Long = 5000L,
    /** Таймаут одного запроса PullMessages в миллисекундах */
    val pullTimeoutMs: Int = 500,
    /** Интервал проверки продления подписки в миллисекундах */
    val renewalCheckIntervalMs: Long = 50 * 60 * 1000L,
    /** Использовать PullPoint по умолчанию (true рекомендуется для надёжности) */
    val usePullPointByDefault: Boolean = true
) {
    companion object {
        fun fromEnvironment(): OnvifEventsConfig {
            val enabled = System.getenv("ONVIF_EVENTS_ENABLED")?.lowercase()?.let {
                it == "true" || it == "1" || it == "yes"
            } ?: true

            val subscriptionTimeSeconds = System.getenv("ONVIF_SUBSCRIPTION_TIME_SEC")?.toIntOrNull() ?: 3600
            val pullIntervalMs = System.getenv("ONVIF_PULL_INTERVAL_MS")?.toLongOrNull() ?: 5000L
            val pullTimeoutMs = System.getenv("ONVIF_PULL_TIMEOUT_MS")?.toIntOrNull() ?: 500
            val renewalCheckIntervalMs = System.getenv("ONVIF_RENEW_CHECK_INTERVAL_MS")?.toLongOrNull() ?: 50 * 60 * 1000L
            val usePullPointByDefault = System.getenv("ONVIF_USE_PULL_POINT")?.lowercase()?.let {
                it != "false" && it != "0" && it != "no"
            } ?: true

            val config = OnvifEventsConfig(
                enabled = enabled,
                subscriptionTimeSeconds = subscriptionTimeSeconds.coerceAtLeast(60),
                pullIntervalMs = pullIntervalMs.coerceAtLeast(1000L),
                pullTimeoutMs = pullTimeoutMs.coerceIn(100, 30_000),
                renewalCheckIntervalMs = renewalCheckIntervalMs.coerceAtLeast(1000L),
                usePullPointByDefault = usePullPointByDefault
            )

            logger.info {
                "ONVIF Events config: enabled=$enabled, subscriptionTime=${config.subscriptionTimeSeconds}s, " +
                    "pullInterval=${config.pullIntervalMs}ms, renewalCheckInterval=${config.renewalCheckIntervalMs}ms, " +
                    "usePullPoint=${config.usePullPointByDefault}"
            }
            return config
        }
    }
}
