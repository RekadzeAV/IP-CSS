package com.company.ipcamera.server.service.analytics

import com.company.ipcamera.shared.domain.model.MotionZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Тесты геометрического фильтра зон детекции движения ([MotionZoneFilter]). */
class MotionZoneFilterTest {

    /** Полный кадр как зона: центр кадра всегда попадает. */
    private fun fullFrameZone(id: String = "z-full", sensitivity: Double = 0.5) = MotionZone(
        id = id,
        name = "Full frame",
        polygon = listOf(
            MotionZone.Point(0.0, 0.0),
            MotionZone.Point(1.0, 0.0),
            MotionZone.Point(1.0, 1.0),
            MotionZone.Point(0.0, 1.0),
        ),
        sensitivity = sensitivity,
    )

    /** Зона — правая половина кадра (x от 0.5 до 1.0). */
    private fun rightHalfZone() = MotionZone(
        id = "z-right",
        name = "Right half",
        polygon = listOf(
            MotionZone.Point(0.5, 0.0),
            MotionZone.Point(1.0, 0.0),
            MotionZone.Point(1.0, 1.0),
            MotionZone.Point(0.5, 1.0),
        ),
        sensitivity = 0.5,
    )

    @Test
    fun `point inside full-frame polygon`() {
        assertTrue(MotionZoneFilter.containsPoint(fullFrameZone().polygon, 0.5, 0.5))
    }

    @Test
    fun `point outside polygon returns false`() {
        // Полигон «правая половина», точка — левый край кадра.
        assertFalse(MotionZoneFilter.containsPoint(rightHalfZone().polygon, 0.1, 0.5))
    }

    @Test
    fun `degenerate polygon (fewer than 3 points) never contains point`() {
        val line = listOf(MotionZone.Point(0.0, 0.0), MotionZone.Point(1.0, 1.0))
        assertFalse(MotionZoneFilter.containsPoint(line, 0.5, 0.5))
    }

    @Test
    fun `zonesContaining matches center of rect to zone`() {
        // Кадр 1920x1080, bbox в правой половине (центр x = 1500/1920 ≈ 0.78).
        val matched = MotionZoneFilter.zonesContaining(
            rectX = 1400, rectY = 400, rectW = 200, rectH = 200,
            frameWidth = 1920, frameHeight = 1080,
            zones = listOf(fullFrameZone(), rightHalfZone()),
        )
        assertEquals(setOf("z-full", "z-right"), matched.map { it.id }.toSet())
    }

    @Test
    fun `zonesContaining excludes zone that does not contain rect center`() {
        // bbox в левой половине: центр x = 300/1920 ≈ 0.16 → только full-frame.
        val matched = MotionZoneFilter.zonesContaining(
            rectX = 200, rectY = 400, rectW = 200, rectH = 200,
            frameWidth = 1920, frameHeight = 1080,
            zones = listOf(fullFrameZone(), rightHalfZone()),
        )
        assertEquals(listOf("z-full"), matched.map { it.id })
    }

    @Test
    fun `zonesContaining filters out disabled zones`() {
        val disabled = fullFrameZone(id = "z-off").copy(enabled = false)
        val matched = MotionZoneFilter.zonesContaining(
            rectX = 0, rectY = 0, rectW = 100, rectH = 100,
            frameWidth = 1920, frameHeight = 1080,
            zones = listOf(disabled),
        )
        assertTrue(matched.isEmpty())
    }

    @Test
    fun `zonesContaining with empty zones returns empty list (no implicit full frame)`() {
        val matched = MotionZoneFilter.zonesContaining(
            rectX = 0, rectY = 0, rectW = 100, rectH = 100,
            frameWidth = 1920, frameHeight = 1080,
            zones = emptyList(),
        )
        assertTrue(matched.isEmpty())
    }

    @Test
    fun `concave polygon point-inclusion is parity based`() {
        // Невыпуклый полигон: квадрат с вырезом (Г-образная ниша) в левом нижнем углу.
        val lShape = MotionZone(
            id = "z-l",
            name = "L-shape",
            polygon = listOf(
                MotionZone.Point(0.0, 0.0),
                MotionZone.Point(1.0, 0.0),
                MotionZone.Point(1.0, 1.0),
                MotionZone.Point(0.5, 1.0),
                MotionZone.Point(0.5, 0.5),
                MotionZone.Point(0.0, 0.5),
            ),
        )
        // Точка в вырезе (0.25, 0.75) — вне L-фигуры.
        assertFalse(MotionZoneFilter.containsPoint(lShape.polygon, 0.25, 0.75))
        // Точка в теле фигуры (0.75, 0.75) — внутри.
        assertTrue(MotionZoneFilter.containsPoint(lShape.polygon, 0.75, 0.75))
    }
}
