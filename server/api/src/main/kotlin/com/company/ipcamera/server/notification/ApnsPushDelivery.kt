package com.company.ipcamera.server.notification

import com.company.ipcamera.shared.domain.model.Notification
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Адаптер доставки push-уведомлений через APNs (iOS).
 *
 * Оборачивает [ApnsNotificationSender]; активен, если APNs сконфигурирован.
 */
class ApnsPushDelivery(
    private val sender: ApnsNotificationSender
) : PushNotificationDelivery {

    override val platform: String = "ios"

    override fun supports(platform: String): Boolean = platform == "ios"

    override suspend fun send(deviceTokens: List<String>, notification: Notification): Result<Unit> {
        if (deviceTokens.isEmpty()) return Result.success(Unit)
        return sender.send(
            tokens = deviceTokens,
            title = notification.title,
            body = notification.message,
            data = notification.extras,
            badge = if (notification.requiresImmediateAttention()) 1 else null,
            sound = if (notification.sound) "default" else null
        )
    }
}

/**
 * Фабрика для создания push-канала APNs из окружения (APNS_*).
 */
object ApnsPushDeliveryFactory {
    fun createFromEnvironment(): ApnsPushDelivery? {
        val config = ApnsConfig.fromEnvironment() ?: return null
        logger.info { "APNs push channel enabled (env: ${config.environment.name})" }
        return ApnsPushDelivery(ApnsNotificationSender(config))
    }
}