package com.company.ipcamera.shared.domain.model

/**
 * Расширенный PTZ (блок 9.2): паттерн — последовательность позиций/пресетов для камеры.
 */
@kotlinx.serialization.Serializable
data class PtzPattern(
    val id: String,
    val cameraId: String,
    val name: String,
    val steps: List<PtzPatternStep>,
    val durationMs: Int = 0,
    val createdAt: Long,
)

@kotlinx.serialization.Serializable
data class PtzPatternStep(
    val presetId: String? = null,
    val pan: Float? = null,
    val tilt: Float? = null,
    val zoom: Float? = null,
    val holdMs: Int = 0,
)

/**
 * Тур — последовательность паттернов или пресетов с временными интервалами.
 */
@kotlinx.serialization.Serializable
data class PtzTour(
    val id: String,
    val cameraId: String,
    val name: String,
    val patternIds: List<String>,
    val repeat: Boolean = false,
    val scheduleCron: String? = null,
    val createdAt: Long,
)

/**
 * Геозона для PTZ: область в кадре (полигон), привязка к пресету или действию.
 */
@kotlinx.serialization.Serializable
data class PtzGeozone(
    val id: String,
    val cameraId: String,
    val name: String,
    val polygon: List<List<Int>>,
    val presetIdOnEnter: String? = null,
    val presetIdOnExit: String? = null,
    val priority: Int = 0,
    val createdAt: Long,
)
