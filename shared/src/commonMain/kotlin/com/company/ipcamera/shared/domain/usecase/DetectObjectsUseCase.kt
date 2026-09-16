package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.common.newRandomId
import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.service.AnalyticsService
import com.company.ipcamera.shared.domain.service.ObjectDetectionResult

/**
 * Use case для детекции объектов на кадре видео
 *
 * Выполняет детекцию объектов различных типов (люди, транспортные средства и т.д.)
 * с возможностью создания событий при обнаружении объектов.
 */
class DetectObjectsUseCase(
    private val analyticsService: AnalyticsService,
    private val eventRepository: EventRepository? = null,
) {
    /**
     * Обнаружить объекты на кадре
     *
     * @param camera Камера
     * @param frameData Данные кадра (байты изображения)
     * @param objectTypes Типы объектов для детекции (если пусто, детекция всех типов)
     * @param minConfidence Минимальная уверенность (0.0 - 1.0)
     * @param createEvent Создавать ли событие при обнаружении объектов (по умолчанию true, если eventRepository предоставлен)
     * @return результат детекции объектов
     */
    suspend operator fun invoke(
        camera: Camera,
        frameData: ByteArray,
        objectTypes: List<String> = emptyList(),
        minConfidence: Float = 0.5f,
        createEvent: Boolean = true,
    ): Result<ObjectDetectionResult> {
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

        // Валидация типов объектов
        val validObjectTypes = setOf("person", "vehicle", "bicycle", "motorcycle", "unknown")
        if (objectTypes.isNotEmpty()) {
            val invalidTypes = objectTypes.filter { it !in validObjectTypes }
            if (invalidTypes.isNotEmpty()) {
                return Result.failure(
                    IllegalArgumentException(
                        "Invalid object types: ${invalidTypes.joinToString()}. Valid types: ${validObjectTypes.joinToString()}",
                    ),
                )
            }
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
                analyticsService.detectObjects(
                    camera = camera,
                    frameData = frameData,
                    objectTypes = objectTypes,
                    minConfidence = minConfidence,
                )

            // Создание события при обнаружении объектов
            if (result.objects.isNotEmpty() && createEvent && eventRepository != null) {
                try {
                    createObjectDetectionEvent(camera, result, objectTypes)
                } catch (e: Exception) {
                    // Логируем ошибку, но не прерываем выполнение
                    println("Warning: Failed to create object detection event: ${e.message}")
                }
            }

            Result.success(result)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                RuntimeException("Failed to detect objects: ${e.message}", e),
            )
        }
    }

    /**
     * Создать событие детекции объектов
     */
    private suspend fun createObjectDetectionEvent(
        camera: Camera,
        result: ObjectDetectionResult,
        requestedObjectTypes: List<String>,
    ) {
        val eventId = newRandomId()
        val timestamp = nowMillis()

        // Группировка объектов по типам
        val objectsByType = result.objects.groupBy { it.type }
        val totalCount = result.objects.size

        val metadata =
            mutableMapOf<String, String>().apply {
                put("totalCount", totalCount.toString())
                objectsByType.forEach { (type, objects) ->
                    put("${type}Count", objects.size.toString())
                    val maxConfidence = objects.maxOfOrNull { it.confidence } ?: 0.0f
                    put("${type}MaxConfidence", maxConfidence.toString())
                }
                if (requestedObjectTypes.isNotEmpty()) {
                    put("requestedTypes", requestedObjectTypes.joinToString(","))
                }
            }

        val description =
            buildString {
                append("Обнаружено объектов: $totalCount")
                if (objectsByType.size == 1) {
                    val (type, objects) = objectsByType.entries.first()
                    val pct = ((objects.maxOfOrNull { it.confidence }?.times(1000) ?: 0.0f).toInt() / 10.0)
                    append(" (тип: $type, уверенность: $pct%)")
                } else {
                    append(" (типы: ${objectsByType.keys.joinToString(", ")})")
                }
            }

        val event =
            Event(
                id = eventId,
                cameraId = camera.id,
                cameraName = camera.name,
                type = EventType.OBJECT_DETECTION,
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
                println("Object detection event created: ${createdEvent.id}")
            },
            onFailure = { error ->
                // Ошибка при создании события
                println("Error creating object detection event: ${error.message}")
            },
        )
    }
}
