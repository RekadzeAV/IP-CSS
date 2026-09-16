package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.data.datasource.local.NotificationLocalDataSource
import com.company.ipcamera.shared.data.datasource.remote.NotificationRemoteDataSource
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
 * Реализация NotificationRepository с использованием Data Sources (новая архитектура)
 *
 * Использует стратегию local-first: сначала проверяет локальную БД,
 * затем синхронизирует с удаленным API при необходимости
 */
class NotificationRepositoryImpl(
    private val localDataSource: NotificationLocalDataSource,
    private val remoteDataSource: NotificationRemoteDataSource? = null,
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
                // Получаем уведомления локально с фильтрацией
                // Примечание: userId фильтрация не применяется, так как уведомления не имеют userId поля
                // В будущем можно добавить userId в модель Notification, если потребуется
                val allNotifications =
                    localDataSource.getNotifications()
                        .filter { notification ->
                            var matches = true
                            // userId фильтрация пропущена - уведомления не имеют userId поля
                            if (type != null && notification.type != type) matches = false
                            if (priority != null && notification.priority != priority) matches = false
                            if (read != null && notification.read != read) matches = false
                            matches
                        }

                // Применяем пагинацию
                val offset = (page - 1) * limit
                val paginatedItems = allNotifications.drop(offset).take(limit)
                val localResult =
                    PaginatedResult(
                        items = paginatedItems,
                        total = allNotifications.size,
                        page = page,
                        limit = limit,
                        hasMore = (offset + limit) < allNotifications.size,
                    )

                // Local-first: remote только если локально нет уведомлений по фильтрам
                if (remoteDataSource != null && allNotifications.isEmpty()) {
                    remoteDataSource.getNotifications(
                        type = type?.name,
                        priority = priority?.name,
                        read = read,
                        cameraId = null,
                        startTime = null,
                        endTime = null,
                        page = page,
                        limit = limit,
                    ).fold(
                        onSuccess = { remoteResult ->
                            // Сохраняем в локальную БД для кэширования
                            if (remoteResult.items.isNotEmpty()) {
                                localDataSource.saveNotifications(remoteResult.items).getOrNull()
                            }
                            remoteResult
                        },
                        onError = { error ->
                            logger.warn(error) { "Failed to get notifications from remote, using local" }
                            localResult
                        },
                    )
                } else {
                    localResult
                }
            } catch (e: Exception) {
                logger.error(e) { "Error getting notifications" }
                PaginatedResult(emptyList(), 0, page, limit, false)
            }
        }

    override suspend fun getNotificationById(id: String): Notification? =
        withContext(Dispatchers.Default) {
            try {
                // Сначала проверяем локально
                localDataSource.getNotificationById(id) ?: run {
                    // Если не найдено локально, пытаемся получить с сервера
                    remoteDataSource?.getNotificationById(id)?.fold(
                        onSuccess = { notification ->
                            // Сохраняем в локальную БД
                            localDataSource.saveNotification(notification).getOrNull()
                            notification
                        },
                        onError = {
                            logger.warn(it) { "Failed to get notification from remote: $id" }
                            null
                        },
                    )
                }
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
                // Сохраняем локально
                val localResult = localDataSource.saveNotification(notification)

                // Если есть удаленный источник, синхронизируем (опционально, уведомления обычно создаются на сервере)
                if (remoteDataSource != null && localResult.isSuccess) {
                    remoteDataSource.createNotification(notification).fold(
                        onSuccess = { remoteNotification ->
                            localDataSource.updateNotification(remoteNotification).getOrNull()
                            Result.success(remoteNotification)
                        },
                        onError = { error ->
                            logger.warn(
                                error,
                            ) { "Failed to sync notification to remote, but saved locally: ${notification.id}" }
                            localResult
                        },
                    )
                } else {
                    localResult
                }
            } catch (e: Exception) {
                logger.error(e) { "Error adding notification: ${notification.id}" }
                Result.failure(e)
            }
        }

    override suspend fun markAsRead(id: String): Result<Notification> =
        withContext(Dispatchers.Default) {
            try {
                // Проверяем существование уведомления
                val localNotification =
                    localDataSource.getNotificationById(id)
                        ?: return@withContext Result.failure(IllegalArgumentException("Notification not found: $id"))

                // Обновляем локально используя метод data source
                val timestamp = nowMillis()
                localDataSource.markNotificationAsRead(id, timestamp).getOrNull()

                // Если есть удаленный источник, синхронизируем
                if (remoteDataSource != null) {
                    remoteDataSource.markNotificationAsRead(id).fold(
                        onSuccess = {
                            // Получаем обновленное уведомление с сервера
                            remoteDataSource.getNotificationById(id).fold(
                                onSuccess = { remoteNotification ->
                                    localDataSource.updateNotification(remoteNotification).getOrNull()
                                    Result.success(remoteNotification)
                                },
                                onError = {
                                    // Если не удалось получить с сервера, возвращаем локально обновленное
                                    val updatedLocal =
                                        localNotification.copy(
                                            read = true,
                                            readAt = timestamp,
                                        )
                                    Result.success(updatedLocal)
                                },
                            )
                        },
                        onError = { error ->
                            logger.warn(
                                error,
                            ) { "Failed to mark notification as read on remote, but updated locally: $id" }
                            // Возвращаем локально обновленное уведомление
                            val updatedLocal =
                                localNotification.copy(
                                    read = true,
                                    readAt = timestamp,
                                )
                            Result.success(updatedLocal)
                        },
                    )
                } else {
                    // Только локальное обновление
                    val updatedLocal =
                        localNotification.copy(
                            read = true,
                            readAt = timestamp,
                        )
                    Result.success(updatedLocal)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error marking notification as read: $id" }
                Result.failure(e)
            }
        }

    override suspend fun markAsRead(ids: List<String>): Result<List<Notification>> =
        withContext(Dispatchers.Default) {
            try {
                // Обновляем локально
                val localNotifications = ids.mapNotNull { localDataSource.getNotificationById(it) }
                val updatedNotifications =
                    localNotifications.map { notification ->
                        notification.copy(
                            read = true,
                            readAt = nowMillis(),
                        )
                    }
                updatedNotifications.forEach { localDataSource.updateNotification(it).getOrNull() }

                // Если есть удаленный источник, синхронизируем
                if (remoteDataSource != null) {
                    ids.forEach { id ->
                        remoteDataSource.markNotificationAsRead(id).fold(
                            onSuccess = {
                                remoteDataSource.getNotificationById(id).fold(
                                    onSuccess = { notification ->
                                        localDataSource.updateNotification(notification).getOrNull()
                                    },
                                    onError = { },
                                )
                            },
                            onError = { },
                        )
                    }
                }
                // Итог — фактическое состояние в local после batch (не только ответ remote mapNotNull)
                Result.success(ids.mapNotNull { localDataSource.getNotificationById(it) })
            } catch (e: Exception) {
                logger.error(e) { "Error marking notifications as read" }
                Result.failure(e)
            }
        }

    override suspend fun markAllAsRead(userId: String?): Result<Int> =
        withContext(Dispatchers.Default) {
            try {
                // Получаем количество непрочитанных уведомлений
                val unreadNotifications = localDataSource.getUnreadNotifications()
                val count = unreadNotifications.size

                // Отмечаем все локальные уведомления как прочитанные используя метод data source
                val timestamp = nowMillis()
                localDataSource.markAllNotificationsAsRead(timestamp).getOrNull()

                // Если есть удаленный источник, синхронизируем
                if (remoteDataSource != null) {
                    remoteDataSource.markAllNotificationsAsRead().fold(
                        onSuccess = {
                            Result.success(count)
                        },
                        onError = { error ->
                            logger.warn(
                                error,
                            ) { "Failed to mark all notifications as read on remote, but updated locally" }
                            Result.success(count)
                        },
                    )
                } else {
                    Result.success(count)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error marking all notifications as read" }
                Result.failure(e)
            }
        }

    override suspend fun deleteNotification(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                // Удаляем локально
                val localResult = localDataSource.deleteNotification(id)

                // Если есть удаленный источник, синхронизируем
                if (remoteDataSource != null && localResult.isSuccess) {
                    remoteDataSource.deleteNotification(id).fold(
                        onSuccess = {
                            Result.success(Unit)
                        },
                        onError = { error ->
                            logger.warn(error) { "Failed to delete notification from remote, but deleted locally: $id" }
                            localResult
                        },
                    )
                } else {
                    localResult
                }
            } catch (e: Exception) {
                logger.error(e) { "Error deleting notification: $id" }
                Result.failure(e)
            }
        }

    override suspend fun getUnreadCount(userId: String?): Int =
        withContext(Dispatchers.Default) {
            try {
                // Используем оптимизированный метод для получения непрочитанных уведомлений
                // Примечание: userId фильтрация не применяется, так как уведомления не имеют userId поля
                localDataSource.getUnreadNotifications().size
            } catch (e: Exception) {
                logger.error(e) { "Error getting unread count" }
                0
            }
        }
}
