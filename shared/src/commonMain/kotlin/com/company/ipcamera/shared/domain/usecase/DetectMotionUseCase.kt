package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.DetectionZone
import com.company.ipcamera.shared.domain.service.AnalyticsService
import com.company.ipcamera.shared.domain.service.MotionDetectionResult
import com.company.ipcamera.shared.domain.service.MotionZone

/**
 * Use case для детекции движения на кадре видео
 */
class DetectMotionUseCase(
    private val analyticsService: AnalyticsService
) {
    /**
     * Обнаружить движение на кадре
     *
     * @param camera Камера
     * @param frameData Данные кадра (байты изображения)
     * @param previousFrameData Данные предыдущего кадра для сравнения (опционально)
     * @param zones Зоны детекции движения (если пусто, детекция по всему кадру)
     * @param threshold Порог чувствительности (0.0 - 1.0)
     * @return результат детекции движения
     */
    suspend operator fun invoke(
        camera: Camera,
        frameData: ByteArray,
        previousFrameData: ByteArray? = null,
        zones: List<DetectionZone> = emptyList(),
        threshold: Float = 0.5f
    ): Result<MotionDetectionResult> {
        return try {
            val result = analyticsService.detectMotion(
                camera = camera,
                frameData = frameData,
                previousFrameData = previousFrameData,
                zones = zones,
                threshold = threshold
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

