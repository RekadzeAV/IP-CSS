package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.service.AnalyticsService
import com.company.ipcamera.shared.domain.service.ObjectDetectionResult

/**
 * Use case для детекции объектов на кадре видео
 */
class DetectObjectsUseCase(
    private val analyticsService: AnalyticsService
) {
    /**
     * Обнаружить объекты на кадре
     *
     * @param camera Камера
     * @param frameData Данные кадра (байты изображения)
     * @param objectTypes Типы объектов для детекции (если пусто, детекция всех типов)
     * @param minConfidence Минимальная уверенность (0.0 - 1.0)
     * @return результат детекции объектов
     */
    suspend operator fun invoke(
        camera: Camera,
        frameData: ByteArray,
        objectTypes: List<String> = emptyList(),
        minConfidence: Float = 0.5f
    ): Result<ObjectDetectionResult> {
        return try {
            val result = analyticsService.detectObjects(
                camera = camera,
                frameData = frameData,
                objectTypes = objectTypes,
                minConfidence = minConfidence
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

