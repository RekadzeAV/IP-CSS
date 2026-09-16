package com.company.ipcamera.core.network.onvif

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnvifImagingServiceTest {

    @Test
    fun testOnvifImagingSettingsCreation() {
        val settings = OnvifImagingSettings(
            brightness = 0.8f,
            contrast = 0.7f,
            colorSaturation = 0.9f,
            sharpness = 0.6f
        )

        assertEquals(0.8f, settings.brightness)
        assertEquals(0.7f, settings.contrast)
        assertEquals(0.9f, settings.colorSaturation)
        assertEquals(0.6f, settings.sharpness)
    }

    @Test
    fun testOnvifExposureSettings() {
        val exposure = OnvifExposureSettings(
            mode = ExposureMode.AUTO,
            priority = ExposurePriority.LowNoise,
            exposureTime = 0.033f,
            iris = 2.8f,
            gain = 1.5f
        )

        assertEquals(ExposureMode.AUTO, exposure.mode)
        assertEquals(ExposurePriority.LowNoise, exposure.priority)
        assertEquals(0.033f, exposure.exposureTime)
        assertEquals(2.8f, exposure.iris)
    }

    @Test
    fun testOnvifFocusSettings() {
        val focus = OnvifFocusSettings(
            mode = FocusMode.AUTO,
            position = null,
            autoFocusSpeed = AutoFocusSpeed.NORMAL
        )

        assertEquals(FocusMode.AUTO, focus.mode)
        assertEquals(AutoFocusSpeed.NORMAL, focus.autoFocusSpeed)

        val manualFocus = OnvifFocusSettings(
            mode = FocusMode.MANUAL,
            position = 0.5f
        )

        assertEquals(FocusMode.MANUAL, manualFocus.mode)
        assertEquals(0.5f, manualFocus.position)
    }

    @Test
    fun testOnvifWhiteBalanceSettings() {
        val whiteBalance = OnvifWhiteBalanceSettings(
            mode = WhiteBalanceMode.AUTO,
            colorTemperature = null
        )

        assertEquals(WhiteBalanceMode.AUTO, whiteBalance.mode)

        val manualWB = OnvifWhiteBalanceSettings(
            mode = WhiteBalanceMode.MANUAL,
            colorTemperature = 5500f
        )

        assertEquals(WhiteBalanceMode.MANUAL, manualWB.mode)
        assertEquals(5500f, manualWB.colorTemperature)
    }

    @Test
    fun testOnvifWideDynamicRangeSettings() {
        val wdr = OnvifWideDynamicRangeSettings(
            enabled = true,
            level = 0.7f
        )

        assertTrue(wdr.enabled)
        assertEquals(0.7f, wdr.level)
    }

    @Test
    fun testOnvifBacklightCompensationSettings() {
        val backlight = OnvifBacklightCompensationSettings(
            enabled = true,
            mode = BacklightCompensationMode.ON
        )

        assertTrue(backlight.enabled)
        assertEquals(BacklightCompensationMode.ON, backlight.mode)
    }

    @Test
    fun testOnvifImagingOptions() {
        val options = OnvifImagingOptions(
            brightnessRange = FloatRange(0.0f, 1.0f),
            contrastRange = FloatRange(0.0f, 1.0f),
            supportedExposureModes = listOf(ExposureMode.AUTO, ExposureMode.MANUAL),
            supportedFocusModes = listOf(FocusMode.AUTO, FocusMode.MANUAL)
        )

        assertNotNull(options.brightnessRange)
        assertNotNull(options.contrastRange)
        assertEquals(2, options.supportedExposureModes.size)
        assertEquals(2, options.supportedFocusModes.size)
    }

    @Test
    fun testOnvifImagingPreset() {
        val preset = OnvifImagingPreset(
            token = "preset-001",
            name = "Day Mode",
            settings = OnvifImagingSettings(
                brightness = 0.8f,
                contrast = 0.7f
            )
        )

        assertEquals("preset-001", preset.token)
        assertEquals("Day Mode", preset.name)
        assertNotNull(preset.settings)
    }

    @Test
    fun testOnvifImagingStatus() {
        val status = OnvifImagingStatus(
            videoSourceToken = "video-source-001",
            focusStatus = FocusStatus.IDLE,
            focusPosition = 0.5f
        )

        assertEquals("video-source-001", status.videoSourceToken)
        assertEquals(FocusStatus.IDLE, status.focusStatus)
        assertEquals(0.5f, status.focusPosition)
    }

    @Test
    fun testExposureModeEnum() {
        assertEquals(ExposureMode.AUTO, ExposureMode.valueOf("AUTO"))
        assertEquals(ExposureMode.MANUAL, ExposureMode.valueOf("MANUAL"))
    }

    @Test
    fun testFocusModeEnum() {
        assertEquals(FocusMode.AUTO, FocusMode.valueOf("AUTO"))
        assertEquals(FocusMode.MANUAL, FocusMode.valueOf("MANUAL"))
    }

    @Test
    fun testWhiteBalanceModeEnum() {
        assertEquals(WhiteBalanceMode.AUTO, WhiteBalanceMode.valueOf("AUTO"))
        assertEquals(WhiteBalanceMode.MANUAL, WhiteBalanceMode.valueOf("MANUAL"))
    }

    @Test
    fun testFocusStatusEnum() {
        assertEquals(FocusStatus.IDLE, FocusStatus.valueOf("IDLE"))
        assertEquals(FocusStatus.MOVING, FocusStatus.valueOf("MOVING"))
        assertEquals(FocusStatus.FAILED, FocusStatus.valueOf("FAILED"))
    }
}
