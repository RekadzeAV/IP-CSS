package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.common.nowMillis
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
 * Реализация NotificationRepository
 *
 * Note: API сервис для уведомлений еще не реализован.
 * Эта реализация использует временное хранилище в памяти и должна быть доработана
 * когда будет готов API сервис для уведомлений.
 *
 * Будущая реализация должна принимать NotificationApiService:
 * ```
 * class NotificationRepositoryImpl(
 *     private val notificationApiService: NotificationApiService
 * ) : NotificationRepository { ... }
 * ```
 */
class NotificationRepositoryImpl() : NotificationRepository {
    // Временное хранилище в памяти
    private val notifications = mutableListOf<Notification>()

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
                var filtered = notifications.asSequence()

                type?.let { t -> filtered = filtered.filter { it.type == t } }
                priority?.let { p -> filtered = filtered.filter { it.priority == p } }
                read?.let { r -> filtered = filtered.filter { it.read == r } }

                val sorted = filtered.sortedByDescending { it.timestamp }.toList()
                val total = sorted.size
                val startIndex = (page - 1) * limit
                val endIndex = minOf(startIndex + limit, sorted.size)
                val items = sorted.subList(startIndex, endIndex)

                PaginatedResult(
                    items = items,
                    total = total,
                    page = page,
                    limit = limit,
                    hasMore = endIndex < total,
                )
            } catch (e: Exception) {
                logger.error(e) { "Error getting notifications" }
                PaginatedResult(emptyList(), 0, page, limit, false)
            }
        }

    override suspend fun getNotificationById(id: String): Notification? =
        withContext(Dispatchers.Default) {
            try {
                notifications.find { it.id == id }
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
                notifications.add(notification)
                Result.success(notification)
            } catch (e: Exception) {
                logger.error(e) { "Error adding notification: ${notification.id}" }
                Result.failure(e)
            }
        }

    override suspend fun markAsRead(id: String): Result<Notification> =
        withContext(Dispatchers.Default) {
            try {
                val index = notifications.indexOfFirst { it.id == id }
                if (index >= 0) {
                    val updated =
                        notifications[index].copy(
                            read = true,
                            readAt = nowMillis(),
                        )
                    notifications[index] = updated
                    Result.success(updated)
                } else {
                    Result.failure(IllegalArgumentException("Notification not found: $id"))
                }
            } catch (e: Exception) {
                logger.error(e) { "Error marking notification as read: $id" }
                Result.failure(e)
            }
        }

    override suspend fun markAsRead(ids: List<String>): Result<List<Notification>> =
        withContext(Dispatchers.Default) {
            try {
                val updated = mutableListOf<Notification>()
                ids.forEach { id ->
                    val index = notifications.indexOfFirst { it.id == id }
                    if (index >= 0) {
                        val notification =
                            notifications[index].copy(
                                read = true,
                                readAt = nowMillis(),
                            )
                        notifications[index] = notification
                        updated.add(notification)
                    }
                }
                Result.success(updated)
            } catch (e: Exception) {
                logger.error(e) { "Error marking notifications as read" }
                Result.failure(e)
            }
        }

    override suspend fun markAllAsRead(userId: String?): Result<Int> =
        withContext(Dispatchers.Default) {
            try {
                var count = 0
                val now = nowMillis()
                notifications.forEachIndexed { index, notification ->
                    if (!notification.read) {
                        count++
                        notifications[index] = notification.copy(read = true, readAt = now)
                    }
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
                val removed = notifications.removeAll { it.id == id }
                if (removed) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalArgumentException("Notification not found: $id"))
                }
            } catch (e: Exception) {
                logger.error(e) { "Error deleting notification: $id" }
                Result.failure(e)
            }
        }

    override suspend fun getUnreadCount(userId: String?): Int =
        withContext(Dispatchers.Default) {
            try {
                notifications.count { !it.read }
            } catch (e: Exception) {
                logger.error(e) { "Error getting unread count" }
                0
            }
        }
}
