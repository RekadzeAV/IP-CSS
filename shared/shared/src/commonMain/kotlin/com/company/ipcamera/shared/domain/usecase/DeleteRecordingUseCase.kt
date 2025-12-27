package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.repository.RecordingRepository

/**
 * Use case для удаления записи
 */
class DeleteRecordingUseCase(
    private val recordingRepository: RecordingRepository
) {
    /**
     * Удалить запись
     *
     * @param recordingId ID записи для удаления
     * @return Результат операции
     */
    suspend operator fun invoke(recordingId: String): Result<Unit> {
        // Валидация входных параметров
        if (recordingId.isBlank()) {
            return Result.failure(IllegalArgumentException("Recording ID cannot be blank"))
        }

        // Проверяем существование записи
        val recording = recordingRepository.getRecordingById(recordingId)
            ?: return Result.failure(IllegalArgumentException("Recording not found: $recordingId"))

        // Примечание: Удаление файла записи и thumbnail должно быть реализовано
        // в RecordingRepository.deleteRecording() или в соответствующем сервисе

        return recordingRepository.deleteRecording(recordingId)
    }
}

