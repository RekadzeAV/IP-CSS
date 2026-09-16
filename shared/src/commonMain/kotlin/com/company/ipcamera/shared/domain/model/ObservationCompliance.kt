package com.company.ipcamera.shared.domain.model

/**
 * Расчёты px/m и предупреждений о недостаточном разрешении (упрощённо: по горизонтали кадра).
 */
object ObservationCompliance {
    /** Рекомендуемые минимумы px/m по классу зоны (ориентир для ТЗ, не нормативный акт). */
    fun defaultTargetPixelsPerMeter(zone: ObservationZoneClass?): Double? =
        when (zone) {
            ObservationZoneClass.OVERVIEW -> 5.0
            ObservationZoneClass.STANDARD -> 20.0
            ObservationZoneClass.EVENT -> 50.0
            null -> null
        }

    /**
     * px/m по горизонтали: [frameWidthPx] / [sceneWidthMeters].
     */
    fun horizontalPixelsPerMeter(
        frameWidthPx: Int,
        sceneWidthMeters: Double,
    ): Double? {
        if (frameWidthPx <= 0 || sceneWidthMeters <= 0.0) return null
        return frameWidthPx / sceneWidthMeters
    }

    /**
     * Сводка для UI/API: вычисленный px/m, целевой минимум и флаг «разрешение может быть недостаточным».
     */
    fun summarize(camera: Camera): CameraObservationSummary {
        val obs = camera.settings.observation
        val width = camera.resolution?.width
        val computedPxM =
            obs.pixelsPerMeterOverride
                ?: if (width != null && obs.sceneWidthMeters != null) {
                    horizontalPixelsPerMeter(width, obs.sceneWidthMeters)
                } else {
                    null
                }
        val targetMin = obs.targetPixelsPerMeterMin ?: defaultTargetPixelsPerMeter(obs.zoneClass)
        val warning =
            if (computedPxM != null && targetMin != null) {
                computedPxM + 1e-6 < targetMin
            } else {
                false
            }
        return CameraObservationSummary(
            computedPixelsPerMeter = computedPxM,
            targetPixelsPerMeterMin = targetMin,
            lowResolutionWarning = warning,
        )
    }
}

/**
 * Производные показатели наблюдения для камеры.
 */
data class CameraObservationSummary(
    val computedPixelsPerMeter: Double?,
    val targetPixelsPerMeterMin: Double?,
    val lowResolutionWarning: Boolean,
)
