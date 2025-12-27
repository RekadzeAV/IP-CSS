package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult

/**
 * Use case для получения списка событий
 */
class GetEventsUseCase(
    private val eventRepository: EventRepository
) {
    /**
     * Получить список событий с фильтрацией и пагинацией
     */
    suspend operator fun invoke(
        type: EventType? = null,
        cameraId: String? = null,
        severity: EventSeverity? = null,
        acknowledged: Boolean? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        limit: Int = 20
    ): PaginatedResult<Event> {
        val validPage = if (page < 1) 1 else page
        val validLimit = when {
            limit <= 0 -> 20
            limit > 100 -> 100
            else -> limit
        }

        if (startTime != null && endTime != null && startTime > endTime) {
            throw IllegalArgumentException("Start time cannot be greater than end time")
        }

        return eventRepository.getEvents(
            type = type,
            cameraId = cameraId?.takeIf { it.isNotBlank() },
            severity = severity,
            acknowledged = acknowledged,
            startTime = startTime?.takeIf { it > 0 },
            endTime = endTime?.takeIf { it > 0 },
            page = validPage,
            limit = validLimit
        )
    }
}

