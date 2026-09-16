package com.company.ipcamera.core.network.onvif

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnvifAnalyticsParserTest {

    @Test
    fun testParseGetAnalyticsEnginesResponse() {
        val xml = """
            <tana:GetAnalyticsEnginesResponse>
                <tana:AnalyticsEngine Token="engine-001">
                    <tana:Name>Motion Detector</tana:Name>
                    <tana:Type>MotionDetector</tana:Type>
                </tana:AnalyticsEngine>
            </tana:GetAnalyticsEnginesResponse>
        """

        val result = OnvifAnalyticsParser.parseGetAnalyticsEnginesResponse(xml)
        assertTrue(result.isSuccess)
        val engines = result.getOrNull()
        assertNotNull(engines)
        assertTrue(engines!!.isNotEmpty())
        assertEquals("engine-001", engines.first().token)
    }

    @Test
    fun testParseGetAnalyticsEngineResponse() {
        val xml = """
            <tana:GetAnalyticsEngineResponse>
                <tana:AnalyticsEngine Token="engine-001">
                    <tana:Name>Motion Detector</tana:Name>
                    <tana:Type>MotionDetector</tana:Type>
                </tana:AnalyticsEngine>
            </tana:GetAnalyticsEngineResponse>
        """

        val result = OnvifAnalyticsParser.parseGetAnalyticsEngineResponse(xml)
        assertTrue(result.isSuccess)
        val engine = result.getOrNull()
        assertNotNull(engine)
        assertEquals("engine-001", engine!!.token)
    }

    @Test
    fun testParseGetAnalyticsEngineInputsResponse() {
        val xml = """
            <tana:GetAnalyticsEngineInputsResponse>
                <tana:AnalyticsEngineInput Token="input-001">
                    <tana:Type>VideoSource</tana:Type>
                    <tana:SourceToken>video-source-001</tana:SourceToken>
                </tana:AnalyticsEngineInput>
            </tana:GetAnalyticsEngineInputsResponse>
        """

        val result = OnvifAnalyticsParser.parseGetAnalyticsEngineInputsResponse(xml)
        assertTrue(result.isSuccess)
        val inputs = result.getOrNull()
        assertNotNull(inputs)
        assertTrue(inputs!!.isNotEmpty())
        assertEquals("input-001", inputs.first().token)
    }

    @Test
    fun testParseGetAnalyticsEngineInputResponse() {
        val xml = """
            <tana:GetAnalyticsEngineInputResponse>
                <tana:AnalyticsEngineInput Token="input-001">
                    <tana:Type>VideoSource</tana:Type>
                </tana:AnalyticsEngineInput>
            </tana:GetAnalyticsEngineInputResponse>
        """

        val result = OnvifAnalyticsParser.parseGetAnalyticsEngineInputResponse(xml)
        assertTrue(result.isSuccess)
        val input = result.getOrNull()
        assertNotNull(input)
        assertEquals("input-001", input!!.token)
    }
}
