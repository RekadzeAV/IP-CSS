package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.RecordingRepository

/**
 * Use case для получения списка записей
 */
class GetRecordingsUseCase(
    private val recordingRepository: RecordingRepository
) {
    /**
     * Получить список записей с фильтрацией и пагинацией
     *
     * @param cameraId Фильтр по ID камеры (опционально)
     * @param startTime Начальное время (опционально)
     * @param endTime Конечное время (опционально)
     * @param page Номер страницы (должен быть >= 1)
     * @param limit Количество элементов на странице (должен быть > 0 и <= 100)
     * @return Результат пагинации с записями
     */
    suspend operator fun invoke(
        cameraId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        limit: Int = 20
    ): PaginatedResult<Recording> {
        // Валидация параметров пагинации
        val validPage = if (page < 1) 1 else page
        val validLimit = when {
            limit <= 0 -> 20
            limit > 100 -> 100
            else -> limit
        }

        // Валидация временного диапазона
        if (startTime != null && endTime != null && startTime > endTime) {
            throw IllegalArgumentException("Start time cannot be greater than end time")
        }

        return recordingRepository.getRecordings(
            cameraId = cameraId?.takeIf { it.isNotBlank() },
            startTime = startTime?.takeIf { it > 0 },
            endTime = endTime?.takeIf { it > 0 },
            page = validPage,
            limit = validLimit
        )
    }
}

