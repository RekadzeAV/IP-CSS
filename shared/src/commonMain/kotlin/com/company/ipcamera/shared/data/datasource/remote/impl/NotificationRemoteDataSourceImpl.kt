package com.company.ipcamera.shared.data.datasource.remote.impl

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.data.datasource.remote.NotificationRemoteDataSource
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация NotificationRemoteDataSource с использованием ApiClient напрямую
 * (так как NotificationApiService не существует, используем ApiClient)
 */
class NotificationRemoteDataSourceImpl(
    private val apiClient: ApiClient
) : NotificationRemoteDataSource {

    private val basePath = "/api/v1/notifications"

    override suspend fun getNotifications(
        type: String?,
        priority: String?,
        read: Boolean?,
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int
    ): ApiResult<PaginatedResult<Notification>> = withContext(Dispatchers.IO) {
        try {
            val queryParams = mutableMapOf<String, String>()
            queryParams["page"] = page.toString()
            queryParams["limit"] = limit.toString()
            type?.let { queryParams["type"] = it }
            priority?.let { queryParams["priority"] = it }
            read?.let { queryParams["read"] = it.toString() }
            cameraId?.let { queryParams["camera_id"] = it }
            startTime?.let { queryParams["start_time"] = it.toString() }
            endTime?.let { queryParams["end_time"] = it.toString() }

            // API возвращает ApiResponse<PaginatedNotificationResponse>
            @kotlinx.serialization.Serializable
            data class PaginatedNotificationResponse(
                val items: List<NotificationResponse>,
                val total: Int,
                val page: Int,
                val limit: Int,
                val hasMore: Boolean
            )

            val response = apiClient.get<ApiResponse<PaginatedNotificationResponse>>(
                path = basePath,
                queryParameters = queryParams
            )

            response.fold(
                onSuccess = { apiResponse ->
                    val paginatedResponse = apiResponse.data
                        ?: throw IllegalArgumentException("Paginated response data is null")

                    val notifications = paginatedResponse.items.map { it.toDomain() }
                    val paginatedResult = PaginatedResult(
                        items = notifications,
                        total = paginatedResponse.total,
                        page = paginatedResponse.page,
                        limit = paginatedResponse.limit,
                        hasMore = paginatedResponse.hasMore
                    )
                    ApiResult.Success(paginatedResult)
                },
                onError = { error ->
                    logger.error(error) { "Error getting notifications from remote API" }
                    ApiResult.Error(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting notifications from remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun getNotificationById(id: String): ApiResult<Notification> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.get<ApiResponse<NotificationResponse>>(
                path = "$basePath/$id"
            )

            response.fold(
                onSuccess = { apiResponse ->
                    val notificationResponse = apiResponse.data
                        ?: throw IllegalArgumentException("Notification data is null")
                    ApiResult.Success(notificationResponse.toDomain())
                },
                onError = { error ->
                    logger.error(error) { "Error getting notification by id from remote API: $id" }
                    ApiResult.Error(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting notification by id from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun createNotification(notification: Notification): ApiResult<Notification> = withContext(Dispatchers.IO) {
        try {
            // Уведомления обычно создаются на сервере автоматически
            // Этот метод может быть использован для ручного создания
            val request = notification.toDto()
            val response = apiClient.post<ApiResponse<NotificationResponse>>(
                path = basePath,
                body = request
            )

            response.fold(
                onSuccess = { apiResponse ->
                    val notificationResponse = apiResponse.data
                        ?: throw IllegalArgumentException("Notification data is null")
                    ApiResult.Success(notificationResponse.toDomain())
                },
                onError = { error ->
                    logger.error(error) { "Error creating notification on remote API: ${notification.id}" }
                    ApiResult.Error(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error creating notification on remote API: ${notification.id}" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun markNotificationAsRead(id: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.post<ApiResponse<Unit>>(
                path = "$basePath/$id/read"
            )

            response.fold(
                onSuccess = {
                    ApiResult.Success(Unit)
                },
                onError = { error ->
                    logger.error(error) { "Error marking notification as read on remote API: $id" }
                    ApiResult.Error(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error marking notification as read on remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun markAllNotificationsAsRead(): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            @kotlinx.serialization.Serializable
            data class MarkAllReadRequest(val ids: List<String>? = null)

            val response = apiClient.post<ApiResponse<Unit>>(
                path = "$basePath/read",
                body = MarkAllReadRequest()
            )

            response.fold(
                onSuccess = {
                    ApiResult.Success(Unit)
                },
                onError = { error ->
                    logger.error(error) { "Error marking all notifications as read on remote API" }
                    ApiResult.Error(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error marking all notifications as read on remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun deleteNotification(id: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.delete<ApiResponse<Unit>>(
                path = "$basePath/$id"
            )

            response.fold(
                onSuccess = {
                    ApiResult.Success(Unit)
                },
                onError = { error ->
                    logger.error(error) { "Error deleting notification from remote API: $id" }
                    ApiResult.Error(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error deleting notification from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    /**
     * Маппинг NotificationResponse в Domain модель Notification
     */
    private fun NotificationResponse.toDomain(): Notification {
        return Notification(
            id = id,
            title = title,
            message = message,
            type = NotificationType.valueOf(type),
            priority = NotificationPriority.valueOf(priority),
            cameraId = cameraId,
            eventId = eventId,
            recordingId = recordingId,
            channelId = channelId,
            icon = icon,
            sound = sound,
            vibration = vibration,
            read = read,
            readAt = readAt,
            timestamp = timestamp,
            extras = extras
        )
    }

    /**
     * Маппинг Notification в DTO для создания
     */
    private fun Notification.toDto(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "message" to message,
            "type" to type.name,
            "priority" to priority.name,
            "cameraId" to (cameraId ?: ""),
            "eventId" to (eventId ?: ""),
            "recordingId" to (recordingId ?: ""),
            "channelId" to (channelId ?: ""),
            "icon" to (icon ?: ""),
            "sound" to sound,
            "vibration" to vibration,
            "timestamp" to timestamp,
            "extras" to extras
        )
    }
}

