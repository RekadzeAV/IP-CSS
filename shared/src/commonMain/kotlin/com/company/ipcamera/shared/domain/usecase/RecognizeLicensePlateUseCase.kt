package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.service.AnalyticsService
import com.company.ipcamera.shared.domain.service.LicensePlateRecognitionResult

/**
 * Use case для распознавания номерных знаков (ANPR - Automatic Number Plate Recognition)
 */
class RecognizeLicensePlateUseCase(
    private val analyticsService: AnalyticsService
) {
    /**
     * Распознать номерные знаки на кадре
     *
     * @param camera Камера
     * @param frameData Данные кадра (байты изображения)
     * @param minConfidence Минимальная уверенность (0.0 - 1.0)
     * @param country Код страны для ограничения распознавания (опционально)
     * @return результат распознавания номерных знаков
     */
    suspend operator fun invoke(
        camera: Camera,
        frameData: ByteArray,
        minConfidence: Float = 0.7f,
        country: String? = null
    ): Result<LicensePlateRecognitionResult> {
        return try {
            val result = analyticsService.recognizeLicensePlates(
                camera = camera,
                frameData = frameData,
                minConfidence = minConfidence,
                country = country
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

