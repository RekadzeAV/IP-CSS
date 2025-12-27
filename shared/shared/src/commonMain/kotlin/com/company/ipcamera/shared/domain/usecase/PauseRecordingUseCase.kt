package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.RecordingRepository

/**
 * Use case для приостановки записи
 */
class PauseRecordingUseCase(
    private val recordingRepository: RecordingRepository
) {
    /**
     * Приостановить запись
     * 
     * @param recordingId ID записи для приостановки
     * @return Результат с обновленной записью
     */
    suspend operator fun invoke(recordingId: String): Result<Recording> {
        val recording = recordingRepository.getRecordingById(recordingId)
            ?: return Result.failure(IllegalArgumentException("Recording not found: $recordingId"))
        
        // Проверяем, что запись активна
        if (!recording.isActive()) {
            return Result.failure(IllegalStateException("Recording is not active: $recordingId"))
        }
        
        // Обновляем статус записи
        val updatedRecording = recording.copy(
            status = com.company.ipcamera.shared.domain.model.RecordingStatus.PAUSED
        )
        
        return recordingRepository.updateRecording(updatedRecording)
    }
}

