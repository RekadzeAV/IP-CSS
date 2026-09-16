package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import com.company.ipcamera.shared.platform.hwaccel.VideoRecordingService

/**
 * Use case для начала записи видео с камеры с поддержкой аппаратного ускорения
 */
class StartRecordingUseCase(
    private val cameraRepository: CameraRepository,
    private val recordingRepository: RecordingRepository,
    private val videoRecordingService: VideoRecordingService? = null,
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
        duration: Long? = null,
    ): Result<Recording> {
        // Валидация входных параметров
        if (cameraId.isBlank()) {
            return Result.failure(IllegalArgumentException("Camera ID cannot be blank"))
        }

        if (duration != null && duration <= 0) {
            return Result.failure(IllegalArgumentException("Duration must be positive"))
        }

        // Получаем камеру
        val camera =
            cameraRepository.getCameraById(cameraId)
                ?: return Result.failure(IllegalArgumentException("Camera not found: $cameraId"))

        // Проверяем, что камера онлайн
        if (camera.status != CameraStatus.ONLINE) {
            return Result.failure(
                IllegalStateException("Camera is not online. Current status: ${camera.status}"),
            )
        }

        // Если доступен VideoRecordingService с HW ускорением — используем его
        val service = videoRecordingService
        if (service != null) {
            return service.startRecording(
                camera = camera,
                format = format,
                duration = duration,
            )
        }

        // Fallback: базовая запись без HW ускорения
        return startBasicRecording(camera, format, quality)
    }

    /**
     * Базовая запись без аппаратного ускорения (legacy fallback)
     */
    private suspend fun startBasicRecording(
        camera: com.company.ipcamera.shared.domain.model.Camera,
        format: RecordingFormat,
        quality: Quality,
    ): Result<Recording> {
        val recordingId = com.company.ipcamera.shared.common.newRandomId()
        val startTime = com.company.ipcamera.shared.common.nowMillis()

        val recording =
            Recording(
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
                status = RecordingStatus.ACTIVE,
                thumbnailUrl = null,
                createdAt = startTime,
            )

        return recordingRepository.addRecording(recording)
    }
}