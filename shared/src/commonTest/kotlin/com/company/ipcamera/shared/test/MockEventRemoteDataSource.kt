package com.company.ipcamera.shared.test

import com.company.ipcamera.core.network.ApiError
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.data.datasource.remote.EventRemoteDataSource
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.PaginatedResult

/**
 * Mock реализация EventRemoteDataSource для тестов
 */
class MockEventRemoteDataSource(
    private var events: MutableList<Event> = mutableListOf(),
) : EventRemoteDataSource {
    var shouldFailOnGet: Boolean = false
    var shouldFailOnCreate: Boolean = false
    var shouldFailOnAcknowledge: Boolean = false
    var networkError: Exception? = null

    private fun apiErr(): ApiError = (networkError ?: Exception("Mock network error")).let { ApiError.UnknownError(it) }

    override suspend fun getEvents(
        type: EventType?,
        cameraId: String?,
        severity: EventSeverity?,
        acknowledged: Boolean?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int,
    ): ApiResult<PaginatedResult<Event>> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            var filtered = events.toList()

            if (type != null) {
                filtered = filtered.filter { it.type == type }
            }
            if (cameraId != null) {
                filtered = filtered.filter { it.cameraId == cameraId }
            }
            if (severity != null) {
                filtered = filtered.filter { it.severity == severity }
            }
            if (acknowledged != null) {
                filtered = filtered.filter { it.acknowledged == acknowledged }
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

    override suspend fun getEventById(id: String): ApiResult<Event> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            val event = events.find { it.id == id }
            if (event != null) {
                ApiResult.Success(event)
            } else {
                ApiResult.Error(ApiError.UnknownError(Exception("Event not found: $id")))
            }
        }
    }

    override suspend fun createEvent(event: Event): ApiResult<Event> {
        return if (shouldFailOnCreate) {
            ApiResult.Error(apiErr())
        } else {
            events.add(event)
            ApiResult.Success(event)
        }
    }

    override suspend fun acknowledgeEvent(
        id: String,
        userId: String,
    ): ApiResult<Event> {
        return if (shouldFailOnAcknowledge) {
            ApiResult.Error(apiErr())
        } else {
            val event = events.find { it.id == id }
            if (event != null) {
                val updated =
                    event.copy(
                        acknowledged = true,
                        acknowledgedAt = System.currentTimeMillis(),
                        acknowledgedBy = userId,
                    )
                val index = events.indexOfFirst { it.id == id }
                events[index] = updated
                ApiResult.Success(updated)
            } else {
                ApiResult.Error(ApiError.UnknownError(Exception("Event not found: $id")))
            }
        }
    }

    override suspend fun acknowledgeEvents(
        ids: List<String>,
        userId: String,
    ): ApiResult<List<Event>> {
        return if (shouldFailOnAcknowledge) {
            ApiResult.Error(apiErr())
        } else {
            val updatedEvents = mutableListOf<Event>()
            ids.forEach { id ->
                val event = events.find { it.id == id }
                if (event != null) {
                    val updated =
                        event.copy(
                            acknowledged = true,
                            acknowledgedAt = System.currentTimeMillis(),
                            acknowledgedBy = userId,
                        )
                    val index = events.indexOfFirst { it.id == id }
                    events[index] = updated
                    updatedEvents.add(updated)
                }
            }
            ApiResult.Success(updatedEvents)
        }
    }

    override suspend fun deleteEvent(id: String): ApiResult<Unit> {
        events.removeIf { it.id == id }
        return ApiResult.Success(Unit)
    }

    override suspend fun getEventStatistics(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
    ): ApiResult<Map<String, Any>> {
        var filtered = events.toList()

        if (cameraId != null) {
            filtered = filtered.filter { it.cameraId == cameraId }
        }
        if (startTime != null) {
            filtered = filtered.filter { it.timestamp >= startTime }
        }
        if (endTime != null) {
            filtered = filtered.filter { it.timestamp <= endTime }
        }

        val stats =
            mapOf(
                "total" to filtered.size,
                "byType" to filtered.groupBy { it.type }.mapValues { it.value.size },
                "bySeverity" to filtered.groupBy { it.severity }.mapValues { it.value.size },
                "acknowledged" to filtered.count { it.acknowledged },
                "unacknowledged" to filtered.count { !it.acknowledged },
            )

        return ApiResult.Success(stats)
    }

    fun clear() {
        events.clear()
    }

    fun addEventDirectly(event: Event) {
        events.add(event)
    }
}
