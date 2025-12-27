package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.RecordingRepository

/**
 * Use case для остановки записи видео
 */
class StopRecordingUseCase(
    private val recordingRepository: RecordingRepository
) {
    /**
     * Остановить запись
     * 
     * @param recordingId ID записи для остановки
     * @return Результат с обновленной записью
     */
    suspend operator fun invoke(recordingId: String): Result<Recording> {
        val recording = recordingRepository.getRecordingById(recordingId)
            ?: return Result.failure(IllegalArgumentException("Recording not found: $recordingId"))
        
        // Проверяем, что запись активна
        if (!recording.isActive()) {
            return Result.failure(IllegalStateException("Recording is not active: $recordingId"))
        }
        
        // Обновляем запись
        val endTime = System.currentTimeMillis()
        val duration = endTime - recording.startTime
        
        val updatedRecording = recording.copy(
            endTime = endTime,
            duration = duration,
            status = com.company.ipcamera.shared.domain.model.RecordingStatus.COMPLETED
        )
        
        return recordingRepository.updateRecording(updatedRecording)
    }
}

