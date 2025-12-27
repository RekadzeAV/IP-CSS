package com.company.ipcamera.shared.data.datasource.remote.impl

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.api.EventApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.data.datasource.remote.EventRemoteDataSource
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация EventRemoteDataSource с использованием EventApiService
 */
class EventRemoteDataSourceImpl(
    private val eventApiService: EventApiService
) : EventRemoteDataSource {

    override suspend fun getEvents(
        type: EventType?,
        cameraId: String?,
        severity: EventSeverity?,
        acknowledged: Boolean?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int
    ): ApiResult<PaginatedResult<Event>> = withContext(Dispatchers.IO) {
        try {
            val paginatedResponse = eventApiService.getEvents(
                type = type?.name,
                cameraId = cameraId,
                severity = severity?.name,
                acknowledged = acknowledged,
                startTime = startTime,
                endTime = endTime,
                page = page,
                limit = limit
            )

            val events = paginatedResponse.items.map { it.toDomain() }
            val paginatedResult = PaginatedResult(
                items = events,
                total = paginatedResponse.total,
                page = paginatedResponse.page,
                limit = paginatedResponse.limit,
                hasMore = paginatedResponse.hasMore
            )

            ApiResult.Success(paginatedResult)
        } catch (e: Exception) {
            logger.error(e) { "Error getting events from remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun getEventById(id: String): ApiResult<Event> = withContext(Dispatchers.IO) {
        try {
            val response = eventApiService.getEventById(id)
            ApiResult.Success(response.toDomain())
        } catch (e: Exception) {
            logger.error(e) { "Error getting event by id from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun createEvent(event: Event): ApiResult<Event> = withContext(Dispatchers.IO) {
        try {
            // EventApiService не имеет метода createEvent, события обычно создаются автоматически
            // Возвращаем ошибку, так как это не поддерживается через API
            ApiResult.Error(
                com.company.ipcamera.core.network.ApiError.UnknownError(
                    UnsupportedOperationException("Creating events via API is not supported")
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error creating event on remote API: ${event.id}" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun acknowledgeEvent(id: String, userId: String): ApiResult<Event> = withContext(Dispatchers.IO) {
        try {
            val response = eventApiService.acknowledgeEvent(id)
            if (response.success) {
                // Получаем обновленное событие
                val eventResponse = eventApiService.getEventById(id)
                ApiResult.Success(eventResponse.toDomain())
            } else {
                ApiResult.Error(
                    com.company.ipcamera.core.network.ApiError.UnknownError(
                        Exception(response.message ?: "Failed to acknowledge event")
                    )
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error acknowledging event on remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun acknowledgeEvents(ids: List<String>, userId: String): ApiResult<List<Event>> = withContext(Dispatchers.IO) {
        try {
            val response = eventApiService.acknowledgeEvents(ids)
            // Получаем все обновленные события
            val events = ids.mapNotNull { id ->
                try {
                    eventApiService.getEventById(id).toDomain()
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to get event after acknowledgment: $id" }
                    null
                }
            }
            ApiResult.Success(events)
        } catch (e: Exception) {
            logger.error(e) { "Error acknowledging events on remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun deleteEvent(id: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            eventApiService.deleteEvent(id)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting event from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun getEventStatistics(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?
    ): ApiResult<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = eventApiService.getEventStatistics(cameraId, startTime, endTime)
            ApiResult.Success(response.data ?: emptyMap())
        } catch (e: Exception) {
            logger.error(e) { "Error getting event statistics from remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    /**
     * Маппинг EventResponse в Domain модель Event
     */
    private fun EventResponse.toDomain(): Event {
        return Event(
            id = id,
            cameraId = cameraId,
            cameraName = cameraName,
            type = EventType.valueOf(type),
            severity = EventSeverity.valueOf(severity),
            timestamp = timestamp,
            description = description,
            metadata = metadata,
            acknowledged = acknowledged,
            acknowledgedAt = acknowledgedAt,
            acknowledgedBy = acknowledgedBy,
            thumbnailUrl = thumbnailUrl,
            videoUrl = videoUrl
        )
    }
}

