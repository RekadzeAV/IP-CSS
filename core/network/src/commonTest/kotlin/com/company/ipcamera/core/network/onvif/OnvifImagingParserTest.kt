package com.company.ipcamera.core.network.onvif

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnvifImagingParserTest {

    @Test
    fun testParseGetImagingSettingsResponse() {
        val xml = """
            <timg:GetImagingSettingsResponse>
                <timg:ImagingSettings>
                    <timg:Brightness>0.8</timg:Brightness>
                    <timg:Contrast>0.7</timg:Contrast>
                    <timg:ColorSaturation>0.9</timg:ColorSaturation>
                    <timg:Sharpness>0.6</timg:Sharpness>
                </timg:ImagingSettings>
            </timg:GetImagingSettingsResponse>
        """

        val result = OnvifImagingParser.parseGetImagingSettingsResponse(xml)
        assertTrue(result.isSuccess)
        val settings = result.getOrNull()
        assertNotNull(settings)
        assertEquals(0.8f, settings!!.brightness)
        assertEquals(0.7f, settings.contrast)
    }

    @Test
    fun testParseGetOptionsResponse() {
        val xml = """
            <timg:GetOptionsResponse>
                <timg:ImagingOptions>
                    <timg:Brightness>
                        <timg:Min>0.0</timg:Min>
                        <timg:Max>1.0</timg:Max>
                    </timg:Brightness>
                </timg:ImagingOptions>
            </timg:GetOptionsResponse>
        """

        val result = OnvifImagingParser.parseGetOptionsResponse(xml)
        assertTrue(result.isSuccess)
        val options = result.getOrNull()
        assertNotNull(options)
        assertNotNull(options!!.brightnessRange)
    }

    @Test
    fun testParseGetStatusResponse() {
        val xml = """
            <timg:GetStatusResponse>
                <timg:Status>
                    <timg:VideoSourceToken>video-source-001</timg:VideoSourceToken>
                    <timg:FocusStatus>IDLE</timg:FocusStatus>
                    <timg:FocusPosition>0.5</timg:FocusPosition>
                </timg:Status>
            </timg:GetStatusResponse>
        """

        val result = OnvifImagingParser.parseGetStatusResponse(xml)
        assertTrue(result.isSuccess)
        val status = result.getOrNull()
        assertNotNull(status)
        assertEquals("video-source-001", status!!.videoSourceToken)
        assertEquals(FocusStatus.IDLE, status.focusStatus)
    }

    @Test
    fun testParseGetPresetsResponse() {
        val xml = """
            <timg:GetPresetsResponse>
                <timg:Preset Token="preset-001">
                    <timg:Name>Day Mode</timg:Name>
                </timg:Preset>
            </timg:GetPresetsResponse>
        """

        val result = OnvifImagingParser.parseGetPresetsResponse(xml)
        assertTrue(result.isSuccess)
        val presets = result.getOrNull()
        assertNotNull(presets)
        assertTrue(presets!!.isNotEmpty())
        assertEquals("preset-001", presets.first().token)
    }

    @Test
    fun testParseSetPresetResponse() {
        val xml = """
            <timg:SetPresetResponse>
                <timg:PresetToken>preset-001</timg:PresetToken>
            </timg:SetPresetResponse>
        """

        val result = OnvifImagingParser.parseSetPresetResponse(xml)
        assertTrue(result.isSuccess)
        val token = result.getOrNull()
        assertNotNull(token)
        assertEquals("preset-001", token)
    }
}
