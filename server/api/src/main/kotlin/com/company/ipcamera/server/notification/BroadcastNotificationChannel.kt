package com.company.ipcamera.server.notification

import com.company.ipcamera.shared.domain.model.Notification

/**
 * Абстракция широковещательного канала уведомлений (без device-токенов).
 *
 * В отличие от [PushNotificationDelivery] (адресация по токенам платформы),
 * широковещательные каналы доставляют уведомление по списку получателей,
 * сконфигурированному на уровне канала (chatIds бота, email-адреса, номера SMS).
 */
interface BroadcastNotificationChannel {
    /** Имя канала (для логирования). */
    val name: String

    /** Разослать уведомление всем получателям канала. */
    suspend fun broadcast(notification: Notification): Result<Unit>
}
