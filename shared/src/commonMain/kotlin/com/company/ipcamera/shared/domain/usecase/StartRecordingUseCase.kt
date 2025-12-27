package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.RecordingRepository

/**
 * Use case для начала записи видео с камеры
 */
class StartRecordingUseCase(
    private val cameraRepository: CameraRepository,
    private val recordingRepository: RecordingRepository
) {
    /**
     * Начать запись с камеры
     * 
     * @param cameraId ID камеры
     * @param format Формат записи (MP4, MKV, etc.)
     * @param quality Качество записи
     * @param duration Длительность записи в миллисекундах (null = бесконечная запись)
     * @return Результат с созданной записью
     */
    suspend operator fun invoke(
        cameraId: String,
        format: RecordingFormat = RecordingFormat.MP4,
        quality: Quality = Quality.HIGH,
        duration: Long? = null
    ): Result<Recording> {
        // Получаем камеру
        val camera = cameraRepository.getCameraById(cameraId)
            ?: return Result.failure(IllegalArgumentException("Camera not found: $cameraId"))
        
        // Проверяем, что камера онлайн
        if (camera.status.name != "ONLINE") {
            return Result.failure(IllegalStateException("Camera is not online: $cameraId"))
        }
        
        // Создаем запись
        val recordingId = java.util.UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()
        
        val recording = Recording(
            id = recordingId,
            cameraId = camera.id,
            cameraName = camera.name,
            startTime = startTime,
            endTime = null,
            duration = 0,
            filePath = null,
            fileSize = null,
            format = format,
            quality = quality,
            status = com.company.ipcamera.shared.domain.model.RecordingStatus.ACTIVE,
            thumbnailUrl = null,
            createdAt = startTime
        )
        
        // Сохраняем запись в репозиторий
        return recordingRepository.addRecording(recording)
    }
}

