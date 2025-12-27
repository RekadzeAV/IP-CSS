package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.repository.NotificationRepository
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Use case для отправки уведомления
 */
class SendNotificationUseCase(
    private val notificationRepository: NotificationRepository
) {
    /**
     * Отправить уведомление
     *
     * @param title Заголовок уведомления
     * @param message Текст уведомления
     * @param type Тип уведомления
     * @param priority Приоритет уведомления
     * @param userId ID пользователя-получателя (если null, отправляется всем)
     * @param cameraId ID камеры, связанной с уведомлением
     * @param eventId ID события, связанного с уведомлением
     * @param recordingId ID записи, связанной с уведомлением
     * @param extras Дополнительные данные
     */
    suspend operator fun invoke(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFO,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        userId: String? = null,
        cameraId: String? = null,
        eventId: String? = null,
        recordingId: String? = null,
        extras: Map<String, String> = emptyMap()
    ): Result<Notification> {
        return try {
            // Валидация входных данных
            require(title.isNotBlank()) { "Title cannot be empty" }
            require(message.isNotBlank()) { "Message cannot be empty" }

            val notification = Notification(
                id = generateNotificationId(),
                title = title.trim(),
                message = message.trim(),
                type = type,
                priority = priority,
                cameraId = cameraId?.takeIf { it.isNotBlank() },
                eventId = eventId?.takeIf { it.isNotBlank() },
                recordingId = recordingId?.takeIf { it.isNotBlank() },
                extras = extras,
                timestamp = System.currentTimeMillis()
            )

            val result = notificationRepository.addNotification(notification)

            if (result.isSuccess) {
                logger.info { "Notification sent: ${notification.id}, type: ${notification.type}, priority: ${notification.priority}" }
            } else {
                logger.error { "Failed to send notification: ${result.exceptionOrNull()?.message}" }
            }

            result
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Invalid notification parameters" }
            Result.failure(e)
        } catch (e: Exception) {
            logger.error(e) { "Error sending notification" }
            Result.failure(e)
        }
    }

    private fun generateNotificationId(): String {
        return "notification_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}

