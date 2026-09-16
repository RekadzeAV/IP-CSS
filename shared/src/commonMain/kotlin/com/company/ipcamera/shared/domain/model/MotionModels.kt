package com.company.ipcamera.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Конфигурация детекции движения для камеры
 */
@Serializable
data class MotionConfig(
    val id: String,
    val cameraId: String,
    val enabled: Boolean = true,
    val sensitivity: Double = 0.5, // 0.0 - 1.0
    val minArea: Double = 0.01, // Минимальная площадь изменения (0.0 - 1.0)
    val zones: List<MotionZone> = emptyList(),
    val cooldownSeconds: Int = 10, // Задержка между событиями
    val recordOnMotion: Boolean = true,
    val notifyOnMotion: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Зона детекции движения
 */
@Serializable
data class MotionZone(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val polygon: List<Point>, // Координаты вершин многоугольника
    val sensitivity: Double = 0.5, // Индивидуальная чувствительность зоны
) {
    @Serializable
    data class Point(
        val x: Double, // 0.0 - 1.0 (относительно ширины кадра)
        val y: Double, // 0.0 - 1.0 (относительно высоты кадра)
    )
}

/**
 * Событие детекции движения
 */
@Serializable
data class MotionEvent(
    val id: String,
    val cameraId: String,
    val zoneId: String?, // null если вне зон
    val timestamp: Long,
    val confidence: Double, // 0.0 - 1.0
    val area: Double, // Площадь изменения (0.0 - 1.0)
    val snapshotPath: String?, // Путь к скриншоту
    val recordingId: String?, // Связанная запись
    val processed: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
)
