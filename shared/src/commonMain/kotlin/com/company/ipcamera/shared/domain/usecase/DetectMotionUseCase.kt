package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.common.newRandomId
import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.DetectionZone
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.service.AnalyticsService
import com.company.ipcamera.shared.domain.service.MotionDetectionResult

/**
 * Use case для детекции движения на кадре видео
 *
 * Выполняет детекцию движения на видеокадре с возможностью создания событий
 * при обнаружении движения.
 */
class DetectMotionUseCase(
    private val analyticsService: AnalyticsService,
    private val eventRepository: EventRepository? = null,
) {
    /**
     * Обнаружить движение на кадре
     *
     * @param camera Камера
     * @param frameData Данные кадра (байты изображения)
     * @param previousFrameData Данные предыдущего кадра для сравнения (опционально)
     * @param zones Зоны детекции движения (если пусто, детекция по всему кадру)
     * @param threshold Порог чувствительности (0.0 - 1.0)
     * @param minArea Минимальная площадь области движения (пиксели). Если null — из настроек камеры или 0.1% кадра.
     * @param createEvent Создавать ли событие при обнаружении движения (по умолчанию true, если eventRepository предоставлен)
     * @return результат детекции движения
     */
    suspend operator fun invoke(
        camera: Camera,
        frameData: ByteArray,
        previousFrameData: ByteArray? = null,
        zones: List<DetectionZone> = emptyList(),
        threshold: Float = 0.5f,
        minArea: Int? = null,
        createEvent: Boolean = true,
    ): Result<MotionDetectionResult> {
        // Валидация входных параметров
        if (camera.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Camera ID cannot be blank"))
        }

        if (frameData.isEmpty()) {
            return Result.failure(IllegalArgumentException("Frame data cannot be empty"))
        }

        if (threshold < 0.0f || threshold > 1.0f) {
            return Result.failure(
                IllegalArgumentException("Threshold must be between 0.0 and 1.0, got: $threshold"),
            )
        }

        // Проверка размера данных кадра
        val expectedSize =
            camera.resolution?.let { res ->
                res.width * res.height * 3 // RGB24
            }

        if (expectedSize != null && frameData.size < expectedSize) {
            return Result.failure(
                IllegalArgumentException(
                    "Frame data size (${frameData.size}) is less than expected ($expectedSize) for resolution ${camera.resolution}",
                ),
            )
        }

        return try {
            val effectiveMinArea = minArea ?: camera.settings.analytics.motionMinArea
            val result =
                analyticsService.detectMotion(
                    camera = camera,
                    frameData = frameData,
                    previousFrameData = previousFrameData,
                    zones = zones,
                    threshold = threshold,
                    minArea = effectiveMinArea,
                )

            // Создание события при обнаружении движения
            if (result.detected && createEvent && eventRepository != null) {
                try {
                    createMotionEvent(camera, result, zones)
                } catch (e: Exception) {
                    // Логируем ошибку, но не прерываем выполнение
                    // В production можно использовать logger
                    println("Warning: Failed to create motion event: ${e.message}")
                }
            }

            Result.success(result)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                RuntimeException("Failed to detect motion: ${e.message}", e),
            )
        }
    }

    /**
     * Создать событие детекции движения
     */
    private suspend fun createMotionEvent(
        camera: Camera,
        result: MotionDetectionResult,
        zones: List<DetectionZone>,
    ) {
        val eventId = newRandomId()
        val timestamp = nowMillis()

        val metadata =
            mutableMapOf<String, String>().apply {
                put("confidence", result.confidence.toString())
                put("zonesCount", zones.size.toString())
                if (result.zones.isNotEmpty()) {
                    put("activeZones", result.zones.joinToString(",") { it.zone.name })
                    put("maxIntensity", result.zones.maxOfOrNull { it.intensity }?.toString() ?: "0.0")
                }
            }

        val description =
            buildString {
                append("Обнаружено движение")
                if (zones.isNotEmpty()) {
                    append(" в ${zones.size} зоне(ах)")
                }
                append(" (уверенность: ${((result.confidence * 1000).toInt() / 10.0)}%)")
            }

        val event =
            Event(
                id = eventId,
                cameraId = camera.id,
                cameraName = camera.name,
                type = EventType.MOTION_DETECTION,
                severity = EventSeverity.WARNING,
                timestamp = timestamp,
                description = description,
                metadata = metadata,
                acknowledged = false,
                acknowledgedAt = null,
                acknowledgedBy = null,
                thumbnailUrl = null,
                videoUrl = null,
            )

        eventRepository?.addEvent(event)?.fold(
            onSuccess = { createdEvent ->
                // Событие успешно создано
                // В production можно использовать logger
                println("Motion event created: ${createdEvent.id}")
            },
            onFailure = { error ->
                // Ошибка при создании события
                println("Error creating motion event: ${error.message}")
            },
        )
    }
}
