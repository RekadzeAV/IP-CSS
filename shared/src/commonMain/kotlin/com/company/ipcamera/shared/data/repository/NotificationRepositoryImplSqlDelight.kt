package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.NotificationEntityMapper
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.repository.NotificationRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация NotificationRepository с использованием SQLDelight
 */
class NotificationRepositoryImplSqlDelight(
    private val databaseFactory: DatabaseFactory
) : NotificationRepository {

    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = NotificationEntityMapper()

    override suspend fun getNotifications(
        userId: String?,
        type: NotificationType?,
        priority: NotificationPriority?,
        read: Boolean?,
        page: Int,
        limit: Int
    ): PaginatedResult<Notification> = withContext(Dispatchers.Default) {
        try {
            val offset = (page - 1) * limit

            // Получаем все уведомления в зависимости от фильтров
            val allNotifications = when {
                read == false -> {
                    // Непрочитанные уведомления
                    val notifications = database.cameraDatabaseQueries
                        .selectUnreadNotifications()
                        .executeAsList()
                    when {
                        type != null && priority != null -> {
                            notifications.filter {
                                it.type == type.name && it.priority == priority.name
                            }
                        }
                        type != null -> {
                            notifications.filter { it.type == type.name }
                        }
                        priority != null -> {
                            notifications.filter { it.priority == priority.name }
                        }
                        else -> notifications
                    }
                }
                read == true -> {
                    // Прочитанные уведомления
                    val all = database.cameraDatabaseQueries
                        .selectAllNotifications()
                        .executeAsList()
                        .filter { it.read == 1L }
                    when {
                        type != null && priority != null -> {
                            all.filter {
                                it.type == type.name && it.priority == priority.name
                            }
                        }
                        type != null -> {
                            all.filter { it.type == type.name }
                        }
                        priority != null -> {
                            all.filter { it.priority == priority.name }
                        }
                        else -> all
                    }
                }
                type != null && priority != null -> {
                    val byType = database.cameraDatabaseQueries
                        .selectNotificationsByType(type.name)
                        .executeAsList()
                    byType.filter { it.priority == priority.name }
                }
                type != null -> {
                    database.cameraDatabaseQueries
                        .selectNotificationsByType(type.name)
                        .executeAsList()
                }
                priority != null -> {
                    database.cameraDatabaseQueries
                        .selectNotificationsByPriority(priority.name)
                        .executeAsList()
                }
                else -> {
                    database.cameraDatabaseQueries
                        .selectAllNotifications()
                        .executeAsList()
                }
            }

            val total = allNotifications.size
            val paginatedNotifications = allNotifications.drop(offset).take(limit)

            PaginatedResult(
                items = paginatedNotifications.map { mapper.toDomain(it) },
                total = total,
                page = page,
                limit = limit,
                hasMore = offset + limit < total
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting notifications" }
            PaginatedResult(emptyList(), 0, page, limit, false)
        }
    }

    override suspend fun getNotificationById(id: String): Notification? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectNotificationById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting notification by id: $id" }
            null
        }
    }

    override suspend fun addNotification(notification: Notification): Result<Notification> = withContext(Dispatchers.Default) {
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
            logger.error(e) { "Error adding notification: ${notification.id}" }
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(id: String): Result<Notification> = withContext(Dispatchers.Default) {
        try {
            val now = System.currentTimeMillis()
            database.cameraDatabaseQueries.markNotificationAsRead(
                read_at = now,
                id = id
            )
            getNotificationById(id)?.let { notification ->
                Result.success(notification)
            } ?: Result.failure(IllegalArgumentException("Notification not found: $id"))
        } catch (e: Exception) {
            logger.error(e) { "Error marking notification as read: $id" }
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(ids: List<String>): Result<List<Notification>> = withContext(Dispatchers.Default) {
        try {
            val now = System.currentTimeMillis()
            ids.forEach { id ->
                database.cameraDatabaseQueries.markNotificationAsRead(
                    read_at = now,
                    id = id
                )
            }
            val notifications = ids.mapNotNull { getNotificationById(it) }
            Result.success(notifications)
        } catch (e: Exception) {
            logger.error(e) { "Error marking notifications as read" }
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(userId: String?): Result<Int> = withContext(Dispatchers.Default) {
        try {
            val now = System.currentTimeMillis()
            // Получаем все непрочитанные уведомления
            val unreadNotifications = database.cameraDatabaseQueries
                .selectUnreadNotifications()
                .executeAsList()

            val count = unreadNotifications.size
            database.cameraDatabaseQueries.markAllNotificationsAsRead(
                read_at = now
            )
            Result.success(count)
        } catch (e: Exception) {
            logger.error(e) { "Error marking all notifications as read" }
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteNotification(id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting notification: $id" }
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(userId: String?): Int = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectUnreadNotifications()
                .executeAsList()
                .size
        } catch (e: Exception) {
            logger.error(e) { "Error getting unread count" }
            0
        }
    }
}


