package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.data.datasource.local.NotificationLocalDataSource
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
 * Реализация NotificationRepository с использованием SQLDelight через [NotificationLocalDataSource],
 * без второго подключения к БД и расхождения с
 * [com.company.ipcamera.shared.data.datasource.local.impl.NotificationLocalDataSourceImpl].
 */
class NotificationRepositoryImplSqlDelight(
    private val notificationLocalDataSource: NotificationLocalDataSource,
) : NotificationRepository {
    override suspend fun getNotifications(
        userId: String?,
        type: NotificationType?,
        priority: NotificationPriority?,
        read: Boolean?,
        page: Int,
        limit: Int,
    ): PaginatedResult<Notification> =
        withContext(Dispatchers.Default) {
            try {
                val offset = (page - 1) * limit

                val allNotifications =
                    when {
                        read == false -> {
                            val notifications = notificationLocalDataSource.getUnreadNotifications()
                            when {
                                type != null && priority != null -> {
                                    notifications.filter {
                                        it.type == type && it.priority == priority
                                    }
                                }
                                type != null -> {
                                    notifications.filter { it.type == type }
                                }
                                priority != null -> {
                                    notifications.filter { it.priority == priority }
                                }
                                else -> notifications
                            }
                        }
                        read == true -> {
                            val all =
                                notificationLocalDataSource.getNotifications()
                                    .filter { it.read }
                            when {
                                type != null && priority != null -> {
                                    all.filter {
                                        it.type == type && it.priority == priority
                                    }
                                }
                                type != null -> {
                                    all.filter { it.type == type }
                                }
                                priority != null -> {
                                    all.filter { it.priority == priority }
                                }
                                else -> all
                            }
                        }
                        type != null && priority != null -> {
                            val byType = notificationLocalDataSource.getNotificationsByType(type.name)
                            byType.filter { it.priority == priority }
                        }
                        type != null -> {
                            notificationLocalDataSource.getNotificationsByType(type.name)
                        }
                        priority != null -> {
                            notificationLocalDataSource.getNotificationsByPriority(priority.name)
                        }
                        else -> {
                            notificationLocalDataSource.getNotifications()
                        }
                    }

                val total = allNotifications.size
                val paginatedNotifications = allNotifications.drop(offset).take(limit)

                PaginatedResult(
                    items = paginatedNotifications,
                    total = total,
                    page = page,
                    limit = limit,
                    hasMore = offset + limit < total,
                )
            } catch (e: Exception) {
                logger.error(e) { "Error getting notifications" }
                PaginatedResult(emptyList(), 0, page, limit, false)
            }
        }

    override suspend fun getNotificationById(id: String): Notification? =
        withContext(Dispatchers.Default) {
            try {
                notificationLocalDataSource.getNotificationById(id)
            } catch (e: Exception) {
                logger.error(e) { "Error getting notification by id: $id" }
                null
            }
        }

    override suspend fun addNotification(notification: Notification): Result<Notification> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                notificationLocalDataSource.saveNotification(notification)
            } catch (e: Exception) {
                logger.error(e) { "Error adding notification: ${notification.id}" }
                Result.failure(e)
            }
        }

    override suspend fun markAsRead(id: String): Result<Notification> =
        withContext(Dispatchers.Default) {
            try {
                val now = nowMillis()
                notificationLocalDataSource.markNotificationAsRead(id, now).getOrElse {
                    return@withContext Result.failure(
                        it,
                    )
                }
                notificationLocalDataSource.getNotificationById(id)?.let { notification ->
                    Result.success(notification)
                } ?: Result.failure(IllegalArgumentException("Notification not found: $id"))
            } catch (e: Exception) {
                logger.error(e) { "Error marking notification as read: $id" }
                Result.failure(e)
            }
        }

    override suspend fun markAsRead(ids: List<String>): Result<List<Notification>> =
        withContext(Dispatchers.Default) {
            try {
                val now = nowMillis()
                ids.forEach { id ->
                    notificationLocalDataSource.markNotificationAsRead(id, now).getOrElse {
                        return@withContext Result.failure(
                            it,
                        )
                    }
                }
                val notifications = ids.mapNotNull { notificationLocalDataSource.getNotificationById(it) }
                Result.success(notifications)
            } catch (e: Exception) {
                logger.error(e) { "Error marking notifications as read" }
                Result.failure(e)
            }
        }

    override suspend fun markAllAsRead(userId: String?): Result<Int> =
        withContext(Dispatchers.Default) {
            try {
                val now = nowMillis()
                val unreadNotifications = notificationLocalDataSource.getUnreadNotifications()
                val count = unreadNotifications.size
                notificationLocalDataSource.markAllNotificationsAsRead(now).getOrElse {
                    return@withContext Result.failure(
                        it,
                    )
                }
                Result.success(count)
            } catch (e: Exception) {
                logger.error(e) { "Error marking all notifications as read" }
                Result.failure(e)
            }
        }

    override suspend fun deleteNotification(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                notificationLocalDataSource.deleteNotification(id)
            } catch (e: Exception) {
                logger.error(e) { "Error deleting notification: $id" }
                Result.failure(e)
            }
        }

    override suspend fun getUnreadCount(userId: String?): Int =
        withContext(Dispatchers.Default) {
            try {
                notificationLocalDataSource.getUnreadNotifications().size
            } catch (e: Exception) {
                logger.error(e) { "Error getting unread count" }
                0
            }
        }
}
