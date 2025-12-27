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
     * @param page Номер страницы
     * @param limit Количество элементов на странице
     * @return Результат пагинации с записями
     */
    suspend operator fun invoke(
        cameraId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        limit: Int = 20
    ): PaginatedResult<Recording> {
        return recordingRepository.getRecordings(
            cameraId = cameraId,
            startTime = startTime,
            endTime = endTime,
            page = page,
            limit = limit
        )
    }
}

