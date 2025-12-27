package com.company.ipcamera.shared.data.datasource.remote

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.repository.PaginatedResult

/**
 * Удаленный источник данных для уведомлений.
 * Отвечает за работу с REST API.
 */
interface NotificationRemoteDataSource {
    /**
     * Получить список уведомлений с сервера
     */
    suspend fun getNotifications(
        type: String? = null,
        priority: String? = null,
        read: Boolean? = null,
        cameraId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        limit: Int = 20
    ): ApiResult<PaginatedResult<Notification>>

    /**
     * Получить уведомление по ID с сервера
     */
    suspend fun getNotificationById(id: String): ApiResult<Notification>

    /**
     * Создать уведомление на сервере
     */
    suspend fun createNotification(notification: Notification): ApiResult<Notification>

    /**
     * Отметить уведомление как прочитанное
     */
    suspend fun markNotificationAsRead(id: String): ApiResult<Unit>

    /**
     * Отметить все уведомления как прочитанные
     */
    suspend fun markAllNotificationsAsRead(): ApiResult<Unit>

    /**
     * Удалить уведомление с сервера
     */
    suspend fun deleteNotification(id: String): ApiResult<Unit>
}

