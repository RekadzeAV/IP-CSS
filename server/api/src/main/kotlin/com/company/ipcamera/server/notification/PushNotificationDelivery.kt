package com.company.ipcamera.server.notification

import com.company.ipcamera.shared.domain.model.Notification

/**
 * Абстракция push-канала доставки уведомлений.
 *
 * Каждый канал объявляет поддерживаемую платформу (например, "ios", "android", "web")
 * и умеет разослать [Notification] по токенам устройств.
 */
interface PushNotificationDelivery {
    /** Возвращает true, если канал обслуживает указанную платформу токена. */
    fun supports(platform: String): Boolean

    /** Основной канал платформы (для логирования/приоритета). */
    val platform: String

    /** Разослать уведомление по токенам устройств. */
    suspend fun send(deviceTokens: List<String>, notification: Notification): Result<Unit>
}