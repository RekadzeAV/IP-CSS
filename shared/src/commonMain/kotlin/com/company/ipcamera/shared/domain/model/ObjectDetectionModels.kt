package com.company.ipcamera.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Конфигурация детекции объектов
 */
@Serializable
data class ObjectDetectionConfig(
    val id: String,
    val cameraId: String,
    val enabled: Boolean = true,
    val modelType: ModelType = ModelType.YOLOV8N,
    val confidence: Double = 0.5, // 0.0 - 1.0
    val iouThreshold: Double = 0.45, // Non-maximum suppression
    val detectClasses: List<String> = listOf("person", "car", "animal"),
    val trackEnabled: Boolean = true,
    val recordOnDetection: Boolean = true,
    val notifyOnDetection: Boolean = true,
    val cooldownSeconds: Int = 30,
    val createdAt: Long,
    val updatedAt: Long,
) {
    @Serializable
    enum class ModelType {
        YOLOV8N, // Nano - fastest
        YOLOV8S, // Small
        YOLOV8M, // Medium
        YOLOV8L, // Large
        YOLOV8X, // Extra large - most accurate
    }
}

/**
 * Событие детекции объекта
 */
@Serializable
data class ObjectDetectionEvent(
    val id: String,
    val cameraId: String,
    val timestamp: Long,
    val detectedObjects: List<DetectedObject>,
    val zoneId: String?,
    val snapshotPath: String?,
    val recordingId: String?,
    val processed: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * Статистика по объектам
 */
@Serializable
data class ObjectStats(
    val cameraId: String,
    val timeRange: TimeRange,
    val totalCount: Int,
    val byClass: Map<String, Int>, // className -> count
    val uniqueTracks: Int,
    val peakTime: Long?, // Timestamp peak activity
) {
    @Serializable
    data class TimeRange(
        val startTime: Long,
        val endTime: Long,
    )
}
