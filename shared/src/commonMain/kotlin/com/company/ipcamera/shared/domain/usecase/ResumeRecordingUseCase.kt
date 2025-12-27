package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.RecordingRepository

/**
 * Use case для возобновления записи
 */
class ResumeRecordingUseCase(
    private val recordingRepository: RecordingRepository
) {
    /**
     * Возобновить запись
     * 
     * @param recordingId ID записи для возобновления
     * @return Результат с обновленной записью
     */
    suspend operator fun invoke(recordingId: String): Result<Recording> {
        val recording = recordingRepository.getRecordingById(recordingId)
            ?: return Result.failure(IllegalArgumentException("Recording not found: $recordingId"))
        
        // Проверяем, что запись приостановлена
        if (recording.status != com.company.ipcamera.shared.domain.model.RecordingStatus.PAUSED) {
            return Result.failure(IllegalStateException("Recording is not paused: $recordingId"))
        }
        
        // Обновляем статус записи
        val updatedRecording = recording.copy(
            status = com.company.ipcamera.shared.domain.model.RecordingStatus.ACTIVE
        )
        
        return recordingRepository.updateRecording(updatedRecording)
    }
}

