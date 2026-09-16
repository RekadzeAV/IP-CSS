package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.network.api.EventApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация EventRepository с использованием API сервиса
 */
class EventRepositoryImpl(
    private val eventApiService: EventApiService,
) : EventRepository {
    override suspend fun getEvents(
        type: EventType?,
        cameraId: String?,
        severity: EventSeverity?,
        acknowledged: Boolean?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int,
    ): PaginatedResult<Event> =
        withContext(Dispatchers.Default) {
            try {
                val result =
                    eventApiService.getEvents(
                        type = type?.name?.lowercase(),
                        cameraId = cameraId,
                        severity = severity?.name,
                        acknowledged = acknowledged,
                        startTime = startTime,
                        endTime = endTime,
                        page = page,
                        limit = limit,
                    )
                PaginatedResult(
                    items = result.items.map { it.toDomain() },
                    total = result.total,
                    page = result.page,
                    limit = result.limit,
                    hasMore = result.hasMore,
                )
            } catch (e: Exception) {
                logger.error(e) { "Error getting events" }
                PaginatedResult(emptyList(), 0, page, limit, false)
            }
        }

    override suspend fun getEventById(id: String): Event? =
        withContext(Dispatchers.Default) {
            try {
                eventApiService.getEventById(id).toDomain()
            } catch (e: Exception) {
                logger.error(e) { "Error getting event by id: $id" }
                null
            }
        }

    override suspend fun addEvent(event: Event): Result<Event> =
        withContext(Dispatchers.Default) {
            try {
                // Note: API doesn't have direct addEvent endpoint
                // This would need to be implemented based on actual API
                Result.failure(UnsupportedOperationException("Adding event directly is not supported by API"))
            } catch (e: Exception) {
                logger.error(e) { "Error adding event: ${event.id}" }
                Result.failure(e)
            }
        }

    override suspend fun updateEvent(event: Event): Result<Event> =
        withContext(Dispatchers.Default) {
            try {
                // Note: API doesn't have direct updateEvent endpoint
                // This would need to be implemented based on actual API
                Result.failure(UnsupportedOperationException("Updating event is not supported by API"))
            } catch (e: Exception) {
                logger.error(e) { "Error updating event: ${event.id}" }
                Result.failure(e)
            }
        }

    override suspend fun acknowledgeEvent(
        id: String,
        userId: String,
    ): Result<Event> =
        withContext(Dispatchers.Default) {
            try {
                eventApiService.acknowledgeEvent(id)
                getEventById(id)?.let { Result.success(it) }
                    ?: Result.failure(Exception("Event not found after acknowledgment"))
            } catch (e: Exception) {
                logger.error(e) { "Error acknowledging event: $id" }
                Result.failure(e)
            }
        }

    override suspend fun acknowledgeEvents(
        ids: List<String>,
        userId: String,
    ): Result<List<Event>> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                eventApiService.acknowledgeEvents(ids)
                val events = ids.mapNotNull { getEventById(it) }
                Result.success(events)
            } catch (e: Exception) {
                logger.error(e) { "Error acknowledging events" }
                Result.failure(e)
            }
        }

    override suspend fun deleteEvent(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                eventApiService.deleteEvent(id)
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error deleting event: $id" }
                Result.failure(e)
            }
        }

    override suspend fun getEventStatistics(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
    ): Result<Map<String, Any>> =
        withContext(Dispatchers.Default) {
            try {
                val response = eventApiService.getEventStatistics(cameraId, startTime, endTime)
                Result.success(response.data ?: emptyMap())
            } catch (e: Exception) {
                logger.error(e) { "Error getting event statistics" }
                Result.failure(e)
            }
        }

    private fun EventResponse.toDomain(): Event {
        return Event(
            id = id,
            cameraId = cameraId,
            cameraName = cameraName,
            type = EventType.valueOf(type.uppercase().replace("-", "_")),
            severity = EventSeverity.valueOf(severity.uppercase()),
            timestamp = timestamp,
            description = description,
            metadata = metadata,
            acknowledged = acknowledged,
            acknowledgedAt = acknowledgedAt,
            acknowledgedBy = acknowledgedBy,
            thumbnailUrl = thumbnailUrl,
            videoUrl = videoUrl,
        )
    }
}
