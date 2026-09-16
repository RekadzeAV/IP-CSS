package com.company.ipcamera.server.service.analytics

import com.company.ipcamera.shared.domain.model.MotionZone

/**
 * Геометрическая фильтрация зон детекции движения.
 *
 * Зона — выпуклый/невыпуклый полигон с вершинами в нормализованных координатах
 * (0.0–1.0 относительно ширины/высоты кадра). Попадание точки в полигон определяется
 * методом чётности пересечений (ray casting).
 */
object MotionZoneFilter {

    /**
     * Вернуть включённые зоны, содержащие центр заданного прямоугольника.
     *
     * @param rectX/rectY/rectW/rectH — прямоугольник движения в пикселях кадра
     * @param frameWidth/frameHeight — размер кадра в пикселях
     * @param zones — сконфигурированные зоны камеры
     */
    fun zonesContaining(
        rectX: Int,
        rectY: Int,
        rectW: Int,
        rectH: Int,
        frameWidth: Int,
        frameHeight: Int,
        zones: List<MotionZone>,
    ): List<MotionZone> {
        if (zones.isEmpty()) return zones.filter { it.enabled }
        val cx = (rectX + rectW / 2.0) / frameWidth
        val cy = (rectY + rectH / 2.0) / frameHeight
        return zones.filter { zone ->
            zone.enabled && containsPoint(zone.polygon, cx, cy)
        }
    }

    /** Точка (x, y) в нормализованных координатах внутри полигона? */
    fun containsPoint(polygon: List<MotionZone.Point>, x: Double, y: Double): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            if (((pi.y > y) != (pj.y > y)) &&
                (x < (pj.x - pi.x) * (y - pi.y) / (pj.y - pi.y) + pi.x)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}
