package com.company.ipcamera.core.network.onvif

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnvifAnalyticsServiceTest {

    @Test
    fun testOnvifAnalyticsEngineCreation() {
        val engine = OnvifAnalyticsEngine(
            token = "engine-001",
            name = "Motion Detector",
            type = "MotionDetector",
            status = AnalyticsEngineStatus.ACTIVE
        )

        assertEquals("engine-001", engine.token)
        assertEquals("Motion Detector", engine.name)
        assertEquals("MotionDetector", engine.type)
        assertEquals(AnalyticsEngineStatus.ACTIVE, engine.status)
    }

    @Test
    fun testAnalyticsEngineStatus() {
        assertEquals(AnalyticsEngineStatus.ACTIVE, AnalyticsEngineStatus.valueOf("ACTIVE"))
        assertEquals(AnalyticsEngineStatus.INACTIVE, AnalyticsEngineStatus.valueOf("INACTIVE"))
        assertEquals(AnalyticsEngineStatus.ERROR, AnalyticsEngineStatus.valueOf("ERROR"))
    }

    @Test
    fun testOnvifAnalyticsEngineConfiguration() {
        val config = OnvifAnalyticsEngineConfiguration(
            parameters = mapOf(
                "Sensitivity" to "80",
                "Threshold" to "0.5"
            ),
            enabled = true
        )

        assertEquals(2, config.parameters.size)
        assertEquals("80", config.parameters["Sensitivity"])
        assertTrue(config.enabled)
    }

    @Test
    fun testOnvifAnalyticsEngineInput() {
        val input = OnvifAnalyticsEngineInput(
            token = "input-001",
            type = "VideoSource",
            sourceToken = "video-source-001"
        )

        assertEquals("input-001", input.token)
        assertEquals("VideoSource", input.type)
        assertEquals("video-source-001", input.sourceToken)
    }

    @Test
    fun testOnvifAnalyticsEngineInputConfiguration() {
        val config = OnvifAnalyticsEngineInputConfiguration(
            parameters = mapOf("Enabled" to "true"),
            enabled = true
        )

        assertTrue(config.enabled)
        assertEquals("true", config.parameters["Enabled"])
    }
}
