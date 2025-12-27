package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.service.AnalyticsService
import com.company.ipcamera.shared.domain.service.FaceDetectionResult

/**
 * Use case для детекции лиц на кадре видео
 */
class DetectFacesUseCase(
    private val analyticsService: AnalyticsService
) {
    /**
     * Обнаружить лица на кадре
     *
     * @param camera Камера
     * @param frameData Данные кадра (байты изображения)
     * @param minConfidence Минимальная уверенность (0.0 - 1.0)
     * @param includeLandmarks Включать ли координаты точек лица
     * @param includeEmbeddings Включать ли векторные представления лиц
     * @return результат детекции лиц
     */
    suspend operator fun invoke(
        camera: Camera,
        frameData: ByteArray,
        minConfidence: Float = 0.5f,
        includeLandmarks: Boolean = false,
        includeEmbeddings: Boolean = false
    ): Result<FaceDetectionResult> {
        return try {
            val result = analyticsService.detectFaces(
                camera = camera,
                frameData = frameData,
                minConfidence = minConfidence,
                includeLandmarks = includeLandmarks,
                includeEmbeddings = includeEmbeddings
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

