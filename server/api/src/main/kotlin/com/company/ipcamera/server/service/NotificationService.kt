package com.company.ipcamera.server.service

import com.company.ipcamera.server.websocket.WebSocketChannel
import com.company.ipcamera.server.websocket.WebSocketManager
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.repository.NotificationRepository
import com.company.ipcamera.shared.domain.usecase.SendNotificationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Сервис для отправки уведомлений
 *
 * Управляет созданием, отправкой и доставкой уведомлений пользователям.
 * Интегрирован с WebSocket для real-time доставки уведомлений.
 */
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val sendNotificationUseCase: SendNotificationUseCase
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Отправить уведомление пользователю или всем пользователям
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
    suspend fun sendNotification(
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
            // Используем Use Case для создания уведомления
            val result = sendNotificationUseCase(
                title = title,
                message = message,
                type = type,
                priority = priority,
                userId = userId,
                cameraId = cameraId,
                eventId = eventId,
                recordingId = recordingId,
                extras = extras
            )

            if (result.isSuccess) {
                val notification = result.getOrThrow()

                // Отправляем уведомление через WebSocket в реальном времени
                scope.launch {
                    sendNotificationViaWebSocket(notification)
                }

                logger.info { "Notification sent: ${notification.id}, type: ${notification.type}, priority: ${notification.priority}" }
            }

            result
        } catch (e: Exception) {
            logger.error(e) { "Error sending notification" }
            Result.failure(e)
        }
    }

    /**
     * Отправить уведомление об ошибке
     */
    suspend fun sendErrorNotification(
        title: String,
        message: String,
        userId: String? = null,
        cameraId: String? = null,
        extras: Map<String, String> = emptyMap()
    ): Result<Notification> {
        return sendNotification(
            title = title,
            message = message,
            type = NotificationType.ERROR,
            priority = NotificationPriority.HIGH,
            userId = userId,
            cameraId = cameraId,
            extras = extras
        )
    }

    /**
     * Отправить уведомление о предупреждении
     */
    suspend fun sendWarningNotification(
        title: String,
        message: String,
        userId: String? = null,
        cameraId: String? = null,
        extras: Map<String, String> = emptyMap()
    ): Result<Notification> {
        return sendNotification(
            title = title,
            message = message,
            type = NotificationType.WARNING,
            priority = NotificationPriority.HIGH,
            userId = userId,
            cameraId = cameraId,
            extras = extras
        )
    }

    /**
     * Отправить уведомление о событии
     */
    suspend fun sendEventNotification(
        eventId: String,
        title: String,
        message: String,
        cameraId: String? = null,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        userId: String? = null
    ): Result<Notification> {
        return sendNotification(
            title = title,
            message = message,
            type = NotificationType.EVENT,
            priority = priority,
            userId = userId,
            cameraId = cameraId,
            eventId = eventId
        )
    }

    /**
     * Отправить уведомление о записи
     */
    suspend fun sendRecordingNotification(
        recordingId: String,
        title: String,
        message: String,
        cameraId: String? = null,
        userId: String? = null
    ): Result<Notification> {
        return sendNotification(
            title = title,
            message = message,
            type = NotificationType.RECORDING,
            priority = NotificationPriority.NORMAL,
            userId = userId,
            cameraId = cameraId,
            recordingId = recordingId
        )
    }

    /**
     * Отправить уведомление через WebSocket
     */
    private suspend fun sendNotificationViaWebSocket(notification: Notification) {
        try {
            val jsonData = buildJsonObject {
                put("id", notification.id)
                put("title", notification.title)
                put("message", notification.message)
                put("type", notification.type.name)
                put("priority", notification.priority.name)
                put("timestamp", notification.timestamp)
                put("read", notification.read)

                notification.cameraId?.let { put("cameraId", it) }
                notification.eventId?.let { put("eventId", it) }
                notification.recordingId?.let { put("recordingId", it) }

                if (notification.extras.isNotEmpty()) {
                    val extrasObject = buildJsonObject {
                        notification.extras.forEach { (key, value) ->
                            put(key, value)
                        }
                    }
                    put("extras", extrasObject)
                }
            }

            // Отправляем уведомление через WebSocket в канал notifications
            WebSocketManager.broadcastEvent(
                channel = WebSocketChannel.NOTIFICATIONS,
                type = "notification.created",
                data = jsonData
            )

            logger.debug { "Notification sent via WebSocket: ${notification.id}" }
        } catch (e: Exception) {
            logger.error(e) { "Error sending notification via WebSocket: ${notification.id}" }
        }
    }
}

