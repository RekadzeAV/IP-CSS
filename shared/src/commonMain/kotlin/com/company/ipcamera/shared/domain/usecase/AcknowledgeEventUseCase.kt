package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.repository.EventRepository

/**
 * Use case для подтверждения события
 */
class AcknowledgeEventUseCase(
    private val eventRepository: EventRepository
) {
    /**
     * Подтвердить событие
     *
     * @param eventId ID события
     * @param userId ID пользователя (по умолчанию "system")
     * @return Результат операции
     */
    suspend operator fun invoke(eventId: String, userId: String = "system"): Result<Event> {
        if (eventId.isBlank()) {
            return Result.failure(IllegalArgumentException("Event ID cannot be blank"))
        }

        return eventRepository.acknowledgeEvent(eventId, userId)
    }
}

