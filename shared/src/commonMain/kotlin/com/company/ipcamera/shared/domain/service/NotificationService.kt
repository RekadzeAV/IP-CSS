package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType

/**
 * Интерфейс для сервиса уведомлений
 *
 * Определяет методы для отправки уведомлений пользователям.
 * Реализация может быть платформо-специфичной (например, в server/api).
 */
interface NotificationService {
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
     * @return результат отправки уведомления
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
        extras: Map<String, String> = emptyMap(),
    ): Result<Notification>

    /**
     * Отправить уведомление о событии
     *
     * @param eventId ID события
     * @param title Заголовок уведомления
     * @param message Текст уведомления
     * @param cameraId ID камеры
     * @param priority Приоритет уведомления
     * @param userId ID пользователя-получателя
     * @return результат отправки уведомления
     */
    suspend fun sendEventNotification(
        eventId: String,
        title: String,
        message: String,
        cameraId: String? = null,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        userId: String? = null,
    ): Result<Notification>
}
