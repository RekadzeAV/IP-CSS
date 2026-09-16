package com.company.ipcamera.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Класс зоны наблюдения (обзор / стандарт / событийная) для политики FPS и целевой плотности пикселей.
 */
@Serializable
enum class ObservationZoneClass {
    OVERVIEW,
    STANDARD,
    EVENT,
}

/**
 * Параметры сцены и целей наблюдения (IEC 62676-4 / px·m⁻¹ — упрощённая модель для ТЗ).
 */
@Serializable
data class ObservationSettings(
    val zoneClass: ObservationZoneClass? = null,
    /** Целевой FPS для зоны (подсказка оператору / политика); null — без явного целевого значения. */
    val targetStreamFps: Int? = null,
    /** Ширина контролируемой сцены в метрах (горизонталь в кадре). */
    val sceneWidthMeters: Double? = null,
    /** Характерная дистанция до объекта интереса, м (опционально). */
    val observationDistanceMeters: Double? = null,
    /** Ручной override px/m, если расчёт из разрешения и ширины сцены не используется. */
    val pixelsPerMeterOverride: Double? = null,
    /** Минимально желаемые px/m для зоны (если null — берётся из [ObservationZoneClass]). */
    val targetPixelsPerMeterMin: Double? = null,
)
