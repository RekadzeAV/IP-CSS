package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.common.newRandomId
import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.service.AnalyticsService
import com.company.ipcamera.shared.domain.service.FaceDetectionResult

/**
 * Use case для детекции лиц на кадре видео
 *
 * Выполняет детекцию лиц с возможностью извлечения landmarks и embeddings
 * для последующего распознавания, а также создания событий при обнаружении лиц.
 */
class DetectFacesUseCase(
    private val analyticsService: AnalyticsService,
    private val eventRepository: EventRepository? = null,
) {
    /**
     * Обнаружить лица на кадре
     *
     * @param camera Камера
     * @param frameData Данные кадра (байты изображения)
     * @param minConfidence Минимальная уверенность (0.0 - 1.0)
     * @param includeLandmarks Извлекать ли landmarks (68 точек) для каждого лица
     * @param includeEmbeddings Извлекать ли embeddings для распознавания лиц
     * @param createEvent Создавать ли событие при обнаружении лиц (по умолчанию true, если eventRepository предоставлен)
     * @return результат детекции лиц
     */
    suspend operator fun invoke(
        camera: Camera,
        frameData: ByteArray,
        minConfidence: Float = 0.5f,
        includeLandmarks: Boolean = false,
        includeEmbeddings: Boolean = false,
        createEvent: Boolean = true,
    ): Result<FaceDetectionResult> {
        // Валидация входных параметров
        if (camera.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Camera ID cannot be blank"))
        }

        if (frameData.isEmpty()) {
            return Result.failure(IllegalArgumentException("Frame data cannot be empty"))
        }

        if (minConfidence < 0.0f || minConfidence > 1.0f) {
            return Result.failure(
                IllegalArgumentException("Min confidence must be between 0.0 and 1.0, got: $minConfidence"),
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
            val result =
                analyticsService.detectFaces(
                    camera = camera,
                    frameData = frameData,
                    minConfidence = minConfidence,
                    includeLandmarks = includeLandmarks,
                    includeEmbeddings = includeEmbeddings,
                )

            // Создание события при обнаружении лиц
            if (result.faces.isNotEmpty() && createEvent && eventRepository != null) {
                try {
                    createFaceDetectionEvent(camera, result, includeLandmarks, includeEmbeddings)
                } catch (e: Exception) {
                    // Логируем ошибку, но не прерываем выполнение
                    println("Warning: Failed to create face detection event: ${e.message}")
                }
            }

            Result.success(result)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                RuntimeException("Failed to detect faces: ${e.message}", e),
            )
        }
    }

    /**
     * Создать событие детекции лиц
     */
    private suspend fun createFaceDetectionEvent(
        camera: Camera,
        result: FaceDetectionResult,
        includeLandmarks: Boolean,
        includeEmbeddings: Boolean,
    ) {
        val eventId = newRandomId()
        val timestamp = nowMillis()

        val metadata =
            mutableMapOf<String, String>().apply {
                put("facesCount", result.faces.size.toString())
                val maxConfidence = result.faces.maxOfOrNull { it.confidence } ?: 0.0f
                put("maxConfidence", maxConfidence.toString())
                val avgConfidence =
                    if (result.faces.isNotEmpty()) {
                        result.faces.map { it.confidence }.average().toFloat()
                    } else {
                        0.0f
                    }
                put("avgConfidence", avgConfidence.toString())
                put("withLandmarks", includeLandmarks.toString())
                put("withEmbeddings", includeEmbeddings.toString())

                // Статистика по landmarks
                val facesWithLandmarks = result.faces.count { it.landmarks != null }
                if (facesWithLandmarks > 0) {
                    put("facesWithLandmarks", facesWithLandmarks.toString())
                }
            }

        val description =
            buildString {
                append("Обнаружено лиц: ${result.faces.size}")
                if (result.faces.isNotEmpty()) {
                    val maxConfidence = result.faces.maxOfOrNull { it.confidence } ?: 0.0f
                    append(" (макс. уверенность: ${((maxConfidence * 1000).toInt() / 10.0)}%)")
                }
                if (includeLandmarks) {
                    append(" [landmarks включены]")
                }
                if (includeEmbeddings) {
                    append(" [embeddings включены]")
                }
            }

        val event =
            Event(
                id = eventId,
                cameraId = camera.id,
                cameraName = camera.name,
                type = EventType.FACE_DETECTION,
                severity = EventSeverity.INFO,
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
                println("Face detection event created: ${createdEvent.id}")
            },
            onFailure = { error ->
                // Ошибка при создании события
                println("Error creating face detection event: ${error.message}")
            },
        )
    }
}
