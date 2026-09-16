package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.common.newRandomId
import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.BoundingBox
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.DetectedObject
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.service.TrackInfo

/**
 * Отслеживаемый объект
 */
data class TrackedObject(
    val id: String, // Уникальный ID для трекинга
    val objectType: String,
    val currentBoundingBox: BoundingBox,
    val trajectory: List<BoundingBox>, // История позиций
    val confidence: Float,
    val startTime: Long,
    val lastSeenTime: Long,
    val lostFrames: Int = 0, // Количество кадров без обнаружения
    val attributes: Map<String, String> = emptyMap(),
)

/**
 * Результат трекинга объектов
 */
data class ObjectTrackingResult(
    val trackedObjects: List<TrackedObject>,
    val newObjects: List<DetectedObject>, // Новые объекты, которые еще не отслеживаются
    val lostObjects: List<String>, // ID объектов, которые потеряны
    val timestamp: Long = nowMillis(),
)

/**
 * Use case для трекинга объектов между кадрами
 *
 * Отслеживает объекты между кадрами, используя простой алгоритм IoU matching.
 * В будущем можно интегрировать более сложные алгоритмы (Kalman filter, DeepSORT).
 */
class TrackObjectsUseCase(
    private val eventRepository: EventRepository? = null,
) {
    // Хранилище активных треков по камерам
    private val activeTracksByCamera = mutableMapOf<String, MutableMap<String, TrackedObject>>()

    // Счетчик для генерации уникальных ID треков
    private var trackIdCounter = 0L

    /**
     * Отследить объекты на кадре
     *
     * @param camera Камера
     * @param detectedObjects Обнаруженные объекты на текущем кадре
     * @param frameTimestamp Временная метка кадра
     * @param maxLostFrames Максимальное количество кадров без обнаружения перед удалением трека
     * @param createEvents Создавать ли события при входе/выходе объектов (по умолчанию true, если eventRepository предоставлен)
     * @return результат трекинга
     */
    suspend operator fun invoke(
        camera: Camera,
        detectedObjects: List<DetectedObject>,
        frameTimestamp: Long = nowMillis(),
        maxLostFrames: Int = 5,
        createEvents: Boolean = true,
    ): Result<ObjectTrackingResult> {
        // Валидация входных параметров
        if (camera.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Camera ID cannot be blank"))
        }

        if (maxLostFrames < 0) {
            return Result.failure(IllegalArgumentException("Max lost frames must be non-negative"))
        }

        return try {
            val activeTracks = activeTracksByCamera.getOrPut(camera.id) { mutableMapOf() }
            val matchedTracks = mutableSetOf<String>()
            val newObjects = mutableListOf<DetectedObject>()
            val lostObjects = mutableListOf<String>()

            // Обновляем существующие треки и находим новые объекты
            for (detectedObject in detectedObjects) {
                var bestMatch: Pair<String, TrackedObject>? = null
                var bestIoU = 0.0

                // Ищем лучший match по IoU (Intersection over Union)
                for ((trackId, trackedObject) in activeTracks) {
                    if (trackedObject.objectType == detectedObject.type &&
                        trackedObject.lostFrames < maxLostFrames
                    ) {
                        val iou = calculateIoU(detectedObject.boundingBox, trackedObject.currentBoundingBox)
                        if (iou > bestIoU && iou > 0.3) { // Порог IoU для матчинга
                            bestIoU = iou
                            bestMatch = trackId to trackedObject
                        }
                    }
                }

                if (bestMatch != null) {
                    // Обновляем существующий трек
                    val (trackId, oldTrack) = bestMatch
                    val updatedTrajectory = oldTrack.trajectory + detectedObject.boundingBox

                    activeTracks[trackId] =
                        oldTrack.copy(
                            currentBoundingBox = detectedObject.boundingBox,
                            trajectory = updatedTrajectory,
                            confidence = detectedObject.confidence,
                            lastSeenTime = frameTimestamp,
                            lostFrames = 0,
                        )
                    matchedTracks.add(trackId)
                } else {
                    // Новый объект
                    newObjects.add(detectedObject)
                }
            }

            // Отмечаем потерянные треки
            for ((trackId, trackedObject) in activeTracks) {
                if (trackId !in matchedTracks) {
                    val updatedLostFrames = trackedObject.lostFrames + 1
                    if (updatedLostFrames > maxLostFrames) {
                        // Трек потерян
                        lostObjects.add(trackId)
                        activeTracks.remove(trackId)

                        // Создаем событие о потере объекта
                        if (createEvents && eventRepository != null) {
                            createObjectLostEvent(camera, trackedObject)
                        }
                    } else {
                        // Обновляем счетчик потерянных кадров
                        activeTracks[trackId] =
                            trackedObject.copy(
                                lostFrames = updatedLostFrames,
                            )
                    }
                }
            }

            // Создаем новые треки для новых объектов
            for (newObject in newObjects) {
                val trackId = generateTrackId()
                val newTrack =
                    TrackedObject(
                        id = trackId,
                        objectType = newObject.type,
                        currentBoundingBox = newObject.boundingBox,
                        trajectory = listOf(newObject.boundingBox),
                        confidence = newObject.confidence,
                        startTime = frameTimestamp,
                        lastSeenTime = frameTimestamp,
                        lostFrames = 0,
                        attributes = mapOf("cameraId" to camera.id),
                    )
                activeTracks[trackId] = newTrack

                // Создаем событие о новом объекте
                if (createEvents && eventRepository != null) {
                    createObjectEnteredEvent(camera, newTrack)
                }
            }

            Result.success(
                ObjectTrackingResult(
                    trackedObjects = activeTracks.values.toList(),
                    newObjects = newObjects,
                    lostObjects = lostObjects,
                ),
            )
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                RuntimeException("Failed to track objects: ${e.message}", e),
            )
        }
    }

    /**
     * Вычислить IoU (Intersection over Union) между двумя bounding boxes
     */
    private fun calculateIoU(
        box1: BoundingBox,
        box2: BoundingBox,
    ): Double {
        val x1 = maxOf(box1.x, box2.x)
        val y1 = maxOf(box1.y, box2.y)
        val x2 = minOf(box1.x + box1.width, box2.x + box2.width)
        val y2 = minOf(box1.y + box1.height, box2.y + box2.height)

        val intersection =
            if (x2 > x1 && y2 > y1) {
                (x2 - x1) * (y2 - y1)
            } else {
                0
            }

        val area1 = box1.width * box1.height
        val area2 = box2.width * box2.height
        val union = area1 + area2 - intersection

        return if (union > 0) intersection.toDouble() / union else 0.0
    }

    /**
     * Генерировать уникальный ID трека
     */
    private fun generateTrackId(): String {
        return "track_${++trackIdCounter}_${nowMillis()}"
    }

    /**
     * Создать событие о входе объекта
     */
    private suspend fun createObjectEnteredEvent(
        camera: Camera,
        trackedObject: TrackedObject,
    ) {
        try {
            val eventId = newRandomId()
            val event =
                Event(
                    id = eventId,
                    cameraId = camera.id,
                    cameraName = camera.name,
                    type = EventType.OBJECT_DETECTION,
                    severity = EventSeverity.INFO,
                    timestamp = nowMillis(),
                    description = "Объект ${trackedObject.objectType} появился (ID: ${trackedObject.id})",
                    metadata =
                        mapOf(
                            "trackId" to trackedObject.id,
                            "objectType" to trackedObject.objectType,
                            "confidence" to trackedObject.confidence.toString(),
                            "event" to "object_entered",
                        ),
                    acknowledged = false,
                    acknowledgedAt = null,
                    acknowledgedBy = null,
                    thumbnailUrl = null,
                    videoUrl = null,
                )

            eventRepository?.addEvent(event)?.fold(
                onSuccess = { /* Событие создано */ },
                onFailure = { error ->
                    println("Error creating object entered event: ${error.message}")
                },
            )
        } catch (e: Exception) {
            println("Warning: Failed to create object entered event: ${e.message}")
        }
    }

    /**
     * Создать событие о потере объекта
     */
    private suspend fun createObjectLostEvent(
        camera: Camera,
        trackedObject: TrackedObject,
    ) {
        try {
            val eventId = newRandomId()
            val duration = nowMillis() - trackedObject.startTime
            val event =
                Event(
                    id = eventId,
                    cameraId = camera.id,
                    cameraName = camera.name,
                    type = EventType.OBJECT_DETECTION,
                    severity = EventSeverity.INFO,
                    timestamp = nowMillis(),
                    description = "Объект ${trackedObject.objectType} потерян (ID: ${trackedObject.id}, длительность: ${duration}ms)",
                    metadata =
                        mapOf(
                            "trackId" to trackedObject.id,
                            "objectType" to trackedObject.objectType,
                            "duration" to duration.toString(),
                            "trajectoryLength" to trackedObject.trajectory.size.toString(),
                            "event" to "object_lost",
                        ),
                    acknowledged = false,
                    acknowledgedAt = null,
                    acknowledgedBy = null,
                    thumbnailUrl = null,
                    videoUrl = null,
                )

            eventRepository?.addEvent(event)?.fold(
                onSuccess = { /* Событие создано */ },
                onFailure = { error ->
                    println("Error creating object lost event: ${error.message}")
                },
            )
        } catch (e: Exception) {
            println("Warning: Failed to create object lost event: ${e.message}")
        }
    }

    /**
     * Очистить все активные треки
     */
    fun clearTracks() {
        activeTracksByCamera.clear()
        trackIdCounter = 0L
    }

    /**
     * Очистить треки для конкретной камеры
     */
    fun clearTracksForCamera(cameraId: String) {
        activeTracksByCamera.remove(cameraId)
    }
}

/**
 * Сведение доменного трека к [TrackInfo] для API и траекторий (числовой [TrackInfo.trackId] из id вида `track_<n>_<ts>`).
 */
fun TrackedObject.toTrackInfo(): TrackInfo {
    val numericId =
        id.removePrefix("track_").substringBefore('_').toIntOrNull()
            ?: (id.hashCode() and 0x7FFF_FFFF)
    return TrackInfo(
        trackId = numericId,
        objectType = objectType,
        boundingBox = currentBoundingBox,
        confidence = confidence,
        lastSeen = lastSeenTime,
    )
}
