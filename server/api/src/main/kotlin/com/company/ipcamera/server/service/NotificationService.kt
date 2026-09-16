package com.company.ipcamera.server.service

import com.company.ipcamera.server.notification.BroadcastNotificationChannel
import com.company.ipcamera.server.notification.PushNotificationDelivery
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.repository.NotificationRepository
import com.company.ipcamera.shared.domain.service.NotificationService as NotificationServiceInterface
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Сервис уведомлений.
 *
 * Сохраняет уведомление в репозиторий (persistence) и, при наличии настроенных push-каналов
 * ([PushNotificationDelivery]), доставляет их по зарегистрированным токенам устройств.
 *
 * Все зависимости push-доставки опциональны — service корректно работает и без них
 * (возвращает сохранённое уведомление, логируя отсутствие каналов).
 */
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val pushTokenService: PushTokenService? = null,
    private val deliveries: List<PushNotificationDelivery> = emptyList(),
    private val broadcastChannels: List<BroadcastNotificationChannel> = emptyList(),
    private val defaultUserId: String? = null
) : NotificationServiceInterface {

    override suspend fun sendNotification(
        title: String,
        message: String,
        type: NotificationType,
        priority: NotificationPriority,
        userId: String?,
        cameraId: String?,
        eventId: String?,
        recordingId: String?,
        extras: Map<String, String>
    ): Result<Notification> {
        val notification = Notification(
            id = "${System.currentTimeMillis()}-${kotlin.random.Random.nextLong(0, 1_000_000)}",
            title = title,
            message = message,
            type = type,
            priority = priority,
            cameraId = cameraId,
            eventId = eventId,
            recordingId = recordingId,
            extras = extras
        )

        // Persistence: сохраняем в репозиторий, если возможно.
        val saved = notificationRepository.addNotification(notification)
        saved.onFailure { e ->
            logger.warn(e) { "Failed to persist notification: $title" }
        }

        // Доставка push (graceful): не роняем основной поток, если канал недоступен.
        try {
            dispatchPush(notification, userId)
        } catch (e: Exception) {
            logger.warn(e) { "Push delivery failed for notification: $title" }
        }

        // Широковещательные каналы (Telegram/Email/SMS): graceful, ошибки не роняют поток.
        broadcastChannels.forEach { channel ->
            try {
                channel.broadcast(notification).onFailure { e ->
                    logger.warn(e) { "Broadcast channel '${channel.name}' failed for notification: $title" }
                }
            } catch (e: Exception) {
                logger.warn(e) { "Broadcast channel '${channel.name}' threw for notification: $title" }
            }
        }

        return saved
    }

    override suspend fun sendEventNotification(
        eventId: String,
        title: String,
        message: String,
        cameraId: String?,
        priority: NotificationPriority,
        userId: String?
    ): Result<Notification> {
        return sendNotification(title, message, NotificationType.EVENT, priority, userId, cameraId, eventId, null)
    }

    /**
     * Доставка уведомления по push-токенам (маршрутизация по платформе).
     */
    private suspend fun dispatchPush(notification: Notification, userId: String?) {
        if (deliveries.isEmpty()) {
            logger.debug { "No push delivery channels configured for notification: ${notification.title}" }
            return
        }
        if (pushTokenService == null) {
            logger.debug { "PushTokenService not configured; skipping push delivery" }
            return
        }

        val targetUserId = userId ?: defaultUserId ?: return
        val tokens = pushTokenService.list(targetUserId)
        if (tokens.isEmpty()) {
            logger.debug { "No device tokens registered for user '$targetUserId'" }
            return
        }

        // Группируем токены по платформе и передаём нужному каналу.
        tokens.groupBy { it.platform.lowercase() }.forEach { (platform, entries) ->
            val deviceTokens = entries.map { it.token }.distinct()
            val delivery = deliveries.firstOrNull { it.supports(platform) }
            if (delivery == null) {
                logger.debug { "No delivery channel for platform '$platform'" }
                return@forEach
            }
            val result = delivery.send(deviceTokens, notification)
            if (result.isFailure) {
                logger.warn { "Push delivery failed for platform '$platform': ${result.exceptionOrNull()?.message}" }
            }
        }
    }
}