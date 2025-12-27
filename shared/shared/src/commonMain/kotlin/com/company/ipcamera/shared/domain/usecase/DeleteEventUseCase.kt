package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.repository.EventRepository

/**
 * Use case для удаления события
 */
class DeleteEventUseCase(
    private val eventRepository: EventRepository
) {
    /**
     * Удалить событие
     *
     * @param eventId ID события для удаления
     * @return Результат операции
     */
    suspend operator fun invoke(eventId: String): Result<Unit> {
        if (eventId.isBlank()) {
            return Result.failure(IllegalArgumentException("Event ID cannot be blank"))
        }

        val event = eventRepository.getEventById(eventId)
            ?: return Result.failure(IllegalArgumentException("Event not found: $eventId"))

        return eventRepository.deleteEvent(eventId)
    }
}

