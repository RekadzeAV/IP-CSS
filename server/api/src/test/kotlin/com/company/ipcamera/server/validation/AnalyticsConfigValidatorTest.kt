package com.company.ipcamera.server.validation

import com.company.ipcamera.server.dto.AnalyticsConfigDto
import com.company.ipcamera.server.dto.DetectionZoneDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsConfigValidatorTest {

    @Test
    fun `accepts default config`() {
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto())
        assertTrue(res is ValidationResult.Success)
    }

    @Test
    fun `rejects invalid motion threshold`() {
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto(motionThreshold = 1.5f))
        assertTrue(res is ValidationResult.Error)
        assertEquals("motionThreshold", (res as ValidationResult.Error).field)
    }

    @Test
    fun `rejects negative motion min area`() {
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto(motionMinArea = -1))
        assertTrue(res is ValidationResult.Error)
        assertEquals("motionMinArea", (res as ValidationResult.Error).field)
    }

    @Test
    fun `rejects negative cooldown`() {
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto(motionEventCooldownMs = -5L))
        assertTrue(res is ValidationResult.Error)
        assertEquals("motionEventCooldownMs", (res as ValidationResult.Error).field)
    }

    @Test
    fun `rejects invalid object detection threshold`() {
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto(objectDetectionConfidenceThreshold = 1.5f))
        assertTrue(res is ValidationResult.Error)
        assertEquals("objectDetectionConfidenceThreshold", (res as ValidationResult.Error).field)
    }

    @Test
    fun `rejects invalid object detection max objects`() {
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto(objectDetectionMaxObjects = 101))
        assertTrue(res is ValidationResult.Error)
        assertEquals("objectDetectionMaxObjects", (res as ValidationResult.Error).field)
    }

    @Test
    fun `rejects blank object type`() {
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto(objectTypes = listOf("person", "  ")))
        assertTrue(res is ValidationResult.Error)
        assertEquals("objectTypes", (res as ValidationResult.Error).field)
    }

    @Test
    fun `rejects object type too long`() {
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto(objectTypes = listOf("x".repeat(51))))
        assertTrue(res is ValidationResult.Error)
        assertEquals("objectTypes", (res as ValidationResult.Error).field)
    }

    @Test
    fun `rejects invalid zone polygon`() {
        val zone = DetectionZoneDto(name = "z1", polygon = listOf(listOf(0, 0), listOf(1, 1)))
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto(zones = listOf(zone)))
        assertTrue(res is ValidationResult.Error)
        assertEquals("zones", (res as ValidationResult.Error).field)
    }

    @Test
    fun `accepts valid zone polygon`() {
        val zone = DetectionZoneDto(
            name = "z1",
            polygon = listOf(listOf(0, 0), listOf(0, 100), listOf(100, 100), listOf(100, 0))
        )
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto(zones = listOf(zone)))
        assertTrue(res is ValidationResult.Success)
    }

    @Test
    fun `rejects blank zone name`() {
        val zone = DetectionZoneDto(
            name = "  ",
            polygon = listOf(listOf(0, 0), listOf(0, 1), listOf(1, 1), listOf(1, 0))
        )
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto(zones = listOf(zone)))
        assertTrue(res is ValidationResult.Error)
        assertEquals("zones", (res as ValidationResult.Error).field)
    }

    @Test
    fun `rejects invalid anpr threshold`() {
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto(anprConfidenceThreshold = -0.5f))
        assertTrue(res is ValidationResult.Error)
        assertEquals("anprConfidenceThreshold", (res as ValidationResult.Error).field)
    }

    @Test
    fun `rejects invalid face recognition threshold`() {
        val res = RequestValidator.validateAnalyticsConfig(AnalyticsConfigDto(faceRecognitionConfidenceThreshold = 1.1f))
        assertTrue(res is ValidationResult.Error)
        assertEquals("faceRecognitionConfidenceThreshold", (res as ValidationResult.Error).field)
    }
}