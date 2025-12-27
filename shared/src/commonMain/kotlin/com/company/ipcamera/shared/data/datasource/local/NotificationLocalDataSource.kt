package com.company.ipcamera.shared.data.datasource.local

import com.company.ipcamera.shared.domain.model.Notification

/**
 * Локальный источник данных для уведомлений.
 * Отвечает за работу с локальной базой данных SQLite.
 */
interface NotificationLocalDataSource {
    /**
     * Получить все уведомления из локальной БД
     */
    suspend fun getNotifications(): List<Notification>

    /**
     * Получить уведомление по ID из локальной БД
     */
    suspend fun getNotificationById(id: String): Notification?

    /**
     * Получить непрочитанные уведомления
     */
    suspend fun getUnreadNotifications(): List<Notification>

    /**
     * Получить уведомления по типу
     */
    suspend fun getNotificationsByType(type: String): List<Notification>

    /**
     * Получить уведомления по приоритету
     */
    suspend fun getNotificationsByPriority(priority: String): List<Notification>

    /**
     * Получить уведомления по ID камеры
     */
    suspend fun getNotificationsByCameraId(cameraId: String): List<Notification>

    /**
     * Получить уведомления в диапазоне дат
     */
    suspend fun getNotificationsByDateRange(startTime: Long, endTime: Long): List<Notification>

    /**
     * Сохранить уведомление в локальную БД
     */
    suspend fun saveNotification(notification: Notification): Result<Notification>

    /**
     * Сохранить список уведомлений в локальную БД (batch операция)
     */
    suspend fun saveNotifications(notifications: List<Notification>): Result<List<Notification>>

    /**
     * Обновить уведомление в локальной БД
     */
    suspend fun updateNotification(notification: Notification): Result<Notification>

    /**
     * Отметить уведомление как прочитанное
     */
    suspend fun markNotificationAsRead(id: String, timestamp: Long): Result<Unit>

    /**
     * Отметить все уведомления как прочитанные
     */
    suspend fun markAllNotificationsAsRead(timestamp: Long): Result<Unit>

    /**
     * Удалить уведомление из локальной БД
     */
    suspend fun deleteNotification(id: String): Result<Unit>

    /**
     * Удалить прочитанные уведомления
     */
    suspend fun deleteReadNotifications(): Result<Unit>

    /**
     * Удалить старые уведомления (до указанной даты)
     */
    suspend fun deleteOldNotifications(beforeTimestamp: Long): Result<Int>

    /**
     * Удалить все уведомления из локальной БД
     */
    suspend fun deleteAllNotifications(): Result<Unit>

    /**
     * Проверить существование уведомления
     */
    suspend fun notificationExists(id: String): Boolean
}

