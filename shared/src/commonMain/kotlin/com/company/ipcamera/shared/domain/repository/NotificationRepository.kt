package com.company.ipcamera.shared.domain.repository

import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType

/**
 * Репозиторий для работы с уведомлениями
 */
interface NotificationRepository {
    /**
     * Получить все уведомления
     */
    suspend fun getNotifications(
        userId: String? = null,
        type: NotificationType? = null,
        priority: NotificationPriority? = null,
        read: Boolean? = null,
        page: Int = 1,
        limit: Int = 20
    ): PaginatedResult<Notification>

    /**
     * Получить уведомление по ID
     */
    suspend fun getNotificationById(id: String): Notification?

    /**
     * Добавить новое уведомление
     */
    suspend fun addNotification(notification: Notification): Result<Notification>

    /**
     * Отметить уведомление как прочитанное
     */
    suspend fun markAsRead(id: String): Result<Notification>

    /**
     * Отметить несколько уведомлений как прочитанные
     */
    suspend fun markAsRead(ids: List<String>): Result<List<Notification>>

    /**
     * Отметить все уведомления как прочитанные
     */
    suspend fun markAllAsRead(userId: String? = null): Result<Int>

    /**
     * Удалить уведомление
     */
    suspend fun deleteNotification(id: String): Result<Unit>

    /**
     * Получить количество непрочитанных уведомлений
     */
    suspend fun getUnreadCount(userId: String? = null): Int
}


