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
        val recording = recordingRepository.getRecordingById(recordingId)
            ?: return Result.failure(IllegalArgumentException("Recording not found: $recordingId"))
        
        // TODO: Удалить файл записи и thumbnail, если они существуют
        // Это должно быть реализовано в сервисе или репозитории
        
        return recordingRepository.deleteRecording(recordingId)
    }
}

