package com.company.ipcamera.shared.data.datasource.local.impl

import com.company.ipcamera.shared.data.datasource.local.NotificationLocalDataSource
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.NotificationEntityMapper
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.model.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация NotificationLocalDataSource с использованием SQLDelight
 */
class NotificationLocalDataSourceImpl(
    private val databaseFactory: DatabaseFactory
) : NotificationLocalDataSource {

    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = NotificationEntityMapper()

    override suspend fun getNotifications(): List<Notification> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectAllNotifications()
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting notifications from local database" }
            emptyList()
        }
    }

    override suspend fun getNotificationById(id: String): Notification? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectNotificationById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting notification by id from local database: $id" }
            null
        }
    }

    override suspend fun getUnreadNotifications(): List<Notification> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectUnreadNotifications()
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting unread notifications from local database" }
            emptyList()
        }
    }

    override suspend fun getNotificationsByType(type: String): List<Notification> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectNotificationsByType(type)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting notifications by type from local database: $type" }
            emptyList()
        }
    }

    override suspend fun getNotificationsByPriority(priority: String): List<Notification> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectNotificationsByPriority(priority)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting notifications by priority from local database: $priority" }
            emptyList()
        }
    }

    override suspend fun getNotificationsByCameraId(cameraId: String): List<Notification> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectNotificationsByCameraId(cameraId)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting notifications by camera id from local database: $cameraId" }
            emptyList()
        }
    }

    override suspend fun getNotificationsByDateRange(startTime: Long, endTime: Long): List<Notification> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectNotificationsByDateRange(startTime, endTime)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting notifications by date range from local database" }
            emptyList()
        }
    }

    override suspend fun saveNotification(notification: Notification): Result<Notification> = withContext(Dispatchers.Default) {
        try {
            val dbNotification = mapper.toDatabase(notification)
            database.cameraDatabaseQueries.insertNotification(
                id = dbNotification.id,
                title = dbNotification.title,
                message = dbNotification.message,
                type = dbNotification.type,
                priority = dbNotification.priority,
                camera_id = dbNotification.camera_id,
                event_id = dbNotification.event_id,
                recording_id = dbNotification.recording_id,
                channel_id = dbNotification.channel_id,
                icon = dbNotification.icon,
                sound = dbNotification.sound,
                vibration = dbNotification.vibration,
                read = dbNotification.read,
                read_at = dbNotification.read_at,
                timestamp = dbNotification.timestamp,
                extras = dbNotification.extras
            )
            Result.success(notification)
        } catch (e: Exception) {
            logger.error(e) { "Error saving notification to local database: ${notification.id}" }
            Result.failure(e)
        }
    }

    override suspend fun saveNotifications(notifications: List<Notification>): Result<List<Notification>> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.transaction {
                notifications.forEach { notification ->
                    val dbNotification = mapper.toDatabase(notification)
                    database.cameraDatabaseQueries.insertNotification(
                        id = dbNotification.id,
                        title = dbNotification.title,
                        message = dbNotification.message,
                        type = dbNotification.type,
                        priority = dbNotification.priority,
                        camera_id = dbNotification.camera_id,
                        event_id = dbNotification.event_id,
                        recording_id = dbNotification.recording_id,
                        channel_id = dbNotification.channel_id,
                        icon = dbNotification.icon,
                        sound = dbNotification.sound,
                        vibration = dbNotification.vibration,
                        read = dbNotification.read,
                        read_at = dbNotification.read_at,
                        timestamp = dbNotification.timestamp,
                        extras = dbNotification.extras
                    )
                }
            }
            Result.success(notifications)
        } catch (e: Exception) {
            logger.error(e) { "Error saving notifications to local database" }
            Result.failure(e)
        }
    }

    override suspend fun updateNotification(notification: Notification): Result<Notification> = withContext(Dispatchers.Default) {
        try {
            val dbNotification = mapper.toDatabase(notification)
            database.cameraDatabaseQueries.updateNotification(
                title = dbNotification.title,
                message = dbNotification.message,
                type = dbNotification.type,
                priority = dbNotification.priority,
                camera_id = dbNotification.camera_id,
                event_id = dbNotification.event_id,
                recording_id = dbNotification.recording_id,
                channel_id = dbNotification.channel_id,
                icon = dbNotification.icon,
                sound = dbNotification.sound,
                vibration = dbNotification.vibration,
                read = dbNotification.read,
                read_at = dbNotification.read_at,
                timestamp = dbNotification.timestamp,
                extras = dbNotification.extras,
                id = dbNotification.id
            )
            Result.success(notification)
        } catch (e: Exception) {
            logger.error(e) { "Error updating notification in local database: ${notification.id}" }
            Result.failure(e)
        }
    }

    override suspend fun markNotificationAsRead(id: String, timestamp: Long): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.markNotificationAsRead(timestamp, id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error marking notification as read in local database: $id" }
            Result.failure(e)
        }
    }

    override suspend fun markAllNotificationsAsRead(timestamp: Long): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.markAllNotificationsAsRead(timestamp)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error marking all notifications as read in local database" }
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteNotification(id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting notification from local database: $id" }
            Result.failure(e)
        }
    }

    override suspend fun deleteReadNotifications(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteReadNotifications()
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting read notifications from local database" }
            Result.failure(e)
        }
    }

    override suspend fun deleteOldNotifications(beforeTimestamp: Long): Result<Int> = withContext(Dispatchers.Default) {
        try {
            // Сначала получаем количество удаляемых уведомлений
            val count = database.cameraDatabaseQueries
                .selectNotificationsByDateRange(0, beforeTimestamp)
                .executeAsList()
                .size

            database.cameraDatabaseQueries.deleteOldNotifications(beforeTimestamp)
            Result.success(count)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting old notifications from local database" }
            Result.failure(e)
        }
    }

    override suspend fun deleteAllNotifications(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val allNotifications = database.cameraDatabaseQueries.selectAllNotifications().executeAsList()
            database.cameraDatabaseQueries.transaction {
                allNotifications.forEach { notification ->
                    database.cameraDatabaseQueries.deleteNotification(notification.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting all notifications from local database" }
            Result.failure(e)
        }
    }

    override suspend fun notificationExists(id: String): Boolean = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectNotificationById(id).executeAsOneOrNull() != null
        } catch (e: Exception) {
            logger.error(e) { "Error checking notification existence in local database: $id" }
            false
        }
    }
}

