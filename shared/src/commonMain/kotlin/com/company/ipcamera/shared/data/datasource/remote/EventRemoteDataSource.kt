package com.company.ipcamera.shared.data.datasource.remote

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.PaginatedResult

/**
 * Удаленный источник данных для событий.
 * Отвечает за работу с REST API.
 */
interface EventRemoteDataSource {
    /**
     * Получить список событий с сервера
     */
    suspend fun getEvents(
        type: EventType? = null,
        cameraId: String? = null,
        severity: EventSeverity? = null,
        acknowledged: Boolean? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        limit: Int = 20
    ): ApiResult<PaginatedResult<Event>>

    /**
     * Получить событие по ID с сервера
     */
    suspend fun getEventById(id: String): ApiResult<Event>

    /**
     * Создать событие на сервере
     */
    suspend fun createEvent(event: Event): ApiResult<Event>

    /**
     * Подтвердить событие на сервере
     */
    suspend fun acknowledgeEvent(id: String, userId: String): ApiResult<Event>

    /**
     * Подтвердить несколько событий на сервере
     */
    suspend fun acknowledgeEvents(ids: List<String>, userId: String): ApiResult<List<Event>>

    /**
     * Удалить событие с сервера
     */
    suspend fun deleteEvent(id: String): ApiResult<Unit>

    /**
     * Получить статистику событий
     */
    suspend fun getEventStatistics(
        cameraId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): ApiResult<Map<String, Any>>
}

