package com.company.ipcamera.shared.test

import com.company.ipcamera.core.network.ApiError
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.data.datasource.remote.NotificationRemoteDataSource
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.repository.PaginatedResult

/**
 * Mock реализация NotificationRemoteDataSource для тестов
 */
class MockNotificationRemoteDataSource(
    private var notifications: MutableList<Notification> = mutableListOf(),
) : NotificationRemoteDataSource {
    var shouldFailOnGet: Boolean = false
    var shouldFailOnCreate: Boolean = false
    var shouldFailOnMarkAsRead: Boolean = false
    var networkError: Exception? = null

    private fun apiErr(): ApiError = (networkError ?: Exception("Mock network error")).let { ApiError.UnknownError(it) }

    override suspend fun getNotifications(
        type: String?,
        priority: String?,
        read: Boolean?,
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int,
    ): ApiResult<PaginatedResult<Notification>> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            var filtered = notifications.toList()

            if (type != null) {
                filtered = filtered.filter { it.type.name == type }
            }
            if (priority != null) {
                filtered = filtered.filter { it.priority.name == priority }
            }
            if (read != null) {
                filtered = filtered.filter { it.read == read }
            }
            if (cameraId != null) {
                filtered = filtered.filter { it.cameraId == cameraId }
            }
            if (startTime != null) {
                filtered = filtered.filter { it.timestamp >= startTime }
            }
            if (endTime != null) {
                filtered = filtered.filter { it.timestamp <= endTime }
            }

            val total = filtered.size
            val offset = (page - 1) * limit
            val paginated = filtered.drop(offset).take(limit)

            ApiResult.Success(
                PaginatedResult(
                    items = paginated,
                    total = total,
                    page = page,
                    limit = limit,
                    hasMore = (offset + limit) < total,
                ),
            )
        }
    }

    override suspend fun getNotificationById(id: String): ApiResult<Notification> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            val notification = notifications.find { it.id == id }
            if (notification != null) {
                ApiResult.Success(notification)
            } else {
                ApiResult.Error(ApiError.UnknownError(Exception("Notification not found: $id")))
            }
        }
    }

    override suspend fun createNotification(notification: Notification): ApiResult<Notification> {
        return if (shouldFailOnCreate) {
            ApiResult.Error(apiErr())
        } else {
            notifications.add(notification)
            ApiResult.Success(notification)
        }
    }

    override suspend fun markNotificationAsRead(id: String): ApiResult<Unit> {
        return if (shouldFailOnMarkAsRead) {
            ApiResult.Error(apiErr())
        } else {
            val notification = notifications.find { it.id == id }
            if (notification != null) {
                val updated = notification.copy(read = true, readAt = System.currentTimeMillis())
                val index = notifications.indexOfFirst { it.id == id }
                notifications[index] = updated
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(ApiError.UnknownError(Exception("Notification not found: $id")))
            }
        }
    }

    override suspend fun markAllNotificationsAsRead(): ApiResult<Unit> {
        return if (shouldFailOnMarkAsRead) {
            ApiResult.Error(apiErr())
        } else {
            notifications.forEachIndexed { index, notification ->
                if (!notification.read) {
                    notifications[index] = notification.copy(read = true, readAt = System.currentTimeMillis())
                }
            }
            ApiResult.Success(Unit)
        }
    }

    override suspend fun deleteNotification(id: String): ApiResult<Unit> {
        notifications.removeIf { it.id == id }
        return ApiResult.Success(Unit)
    }

    fun clear() {
        notifications.clear()
    }

    fun addNotificationDirectly(notification: Notification) {
        notifications.add(notification)
    }
}
