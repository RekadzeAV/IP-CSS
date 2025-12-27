package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.BoundingBox
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.DetectedObject

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
    val attributes: Map<String, String> = emptyMap()
)

/**
 * Результат трекинга объектов
 */
data class ObjectTrackingResult(
    val trackedObjects: List<TrackedObject>,
    val newObjects: List<DetectedObject>, // Новые объекты, которые еще не отслеживаются
    val lostObjects: List<String>, // ID объектов, которые потеряны
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Use case для трекинга объектов между кадрами
 */
class TrackObjectsUseCase {
    private val activeTracks = mutableMapOf<String, TrackedObject>()

    /**
     * Отследить объекты на кадре
     *
     * @param camera Камера
     * @param detectedObjects Обнаруженные объекты на текущем кадре
     * @param frameTimestamp Временная метка кадра
     * @param maxLostFrames Максимальное количество кадров без обнаружения перед удалением трека
     * @return результат трекинга
     */
    suspend operator fun invoke(
        camera: Camera,
        detectedObjects: List<DetectedObject>,
        frameTimestamp: Long = System.currentTimeMillis(),
        maxLostFrames: Int = 5
    ): Result<ObjectTrackingResult> {
        // TODO: Интеграция с алгоритмом трекинга (Kalman filter, DeepSORT и т.д.)
        // Сейчас возвращаем заглушку
        val newObjects = detectedObjects
        val lostObjects = emptyList<String>()

        return Result.success(
            ObjectTrackingResult(
                trackedObjects = activeTracks.values.toList(),
                newObjects = newObjects,
                lostObjects = lostObjects
            )
        )
    }

    /**
     * Очистить все активные треки
     */
    fun clearTracks() {
        activeTracks.clear()
    }

    /**
     * Очистить треки для конкретной камеры
     */
    fun clearTracksForCamera(cameraId: String) {
        activeTracks.entries.removeIf { it.value.attributes["cameraId"] == cameraId }
    }
}

