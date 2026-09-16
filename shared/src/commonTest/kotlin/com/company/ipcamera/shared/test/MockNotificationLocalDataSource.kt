package com.company.ipcamera.shared.test

import com.company.ipcamera.shared.data.datasource.local.NotificationLocalDataSource
import com.company.ipcamera.shared.domain.model.Notification

/**
 * Mock реализация NotificationLocalDataSource для тестов
 */
class MockNotificationLocalDataSource(
    private var notifications: MutableList<Notification> = mutableListOf(),
) : NotificationLocalDataSource {
    var shouldFailOnSave: Boolean = false
    var shouldFailOnUpdate: Boolean = false
    var shouldFailOnDelete: Boolean = false

    override suspend fun getNotifications(): List<Notification> = notifications.toList()

    override suspend fun getNotificationById(id: String): Notification? = notifications.find { it.id == id }

    override suspend fun getUnreadNotifications(): List<Notification> {
        return notifications.filter { !it.read }
    }

    override suspend fun getNotificationsByType(type: String): List<Notification> {
        return notifications.filter { it.type.name == type }
    }

    override suspend fun getNotificationsByPriority(priority: String): List<Notification> {
        return notifications.filter { it.priority.name == priority }
    }

    override suspend fun getNotificationsByCameraId(cameraId: String): List<Notification> {
        return notifications.filter { it.cameraId == cameraId }
    }

    override suspend fun getNotificationsByDateRange(
        startTime: Long,
        endTime: Long,
    ): List<Notification> {
        return notifications.filter { it.timestamp >= startTime && it.timestamp <= endTime }
    }

    override suspend fun saveNotification(notification: Notification): Result<Notification> {
        return if (shouldFailOnSave) {
            Result.failure(Exception("Mock save failure"))
        } else {
            notifications.add(notification)
            Result.success(notification)
        }
    }

    override suspend fun saveNotifications(notifications: List<Notification>): Result<List<Notification>> {
        return if (shouldFailOnSave) {
            Result.failure(Exception("Mock save failure"))
        } else {
            this.notifications.addAll(notifications)
            Result.success(notifications)
        }
    }

    override suspend fun updateNotification(notification: Notification): Result<Notification> {
        return if (shouldFailOnUpdate) {
            Result.failure(Exception("Mock update failure"))
        } else {
            val index = notifications.indexOfFirst { it.id == notification.id }
            if (index >= 0) {
                notifications[index] = notification
                Result.success(notification)
            } else {
                Result.failure(Exception("Notification not found: ${notification.id}"))
            }
        }
    }

    override suspend fun markNotificationAsRead(
        id: String,
        timestamp: Long,
    ): Result<Unit> {
        val notification = notifications.find { it.id == id }
        return if (notification != null) {
            val updated = notification.copy(read = true, readAt = timestamp)
            val index = notifications.indexOfFirst { it.id == id }
            notifications[index] = updated
            Result.success(Unit)
        } else {
            Result.failure(Exception("Notification not found: $id"))
        }
    }

    override suspend fun markAllNotificationsAsRead(timestamp: Long): Result<Unit> {
        notifications.forEachIndexed { index, notification ->
            if (!notification.read) {
                notifications[index] = notification.copy(read = true, readAt = timestamp)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteNotification(id: String): Result<Unit> {
        return if (shouldFailOnDelete) {
            Result.failure(Exception("Mock delete failure"))
        } else {
            notifications.removeIf { it.id == id }
            Result.success(Unit)
        }
    }

    override suspend fun deleteReadNotifications(): Result<Unit> {
        notifications.removeIf { it.read }
        return Result.success(Unit)
    }

    override suspend fun deleteOldNotifications(beforeTimestamp: Long): Result<Int> {
        val count = notifications.count { it.timestamp < beforeTimestamp }
        notifications.removeIf { it.timestamp < beforeTimestamp }
        return Result.success(count)
    }

    override suspend fun deleteAllNotifications(): Result<Unit> {
        notifications.clear()
        return Result.success(Unit)
    }

    override suspend fun notificationExists(id: String): Boolean {
        return notifications.any { it.id == id }
    }

    fun clear() {
        notifications.clear()
    }

    fun addNotificationDirectly(notification: Notification) {
        notifications.add(notification)
    }
}
