package com.company.ipcamera.server.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StreamQualityTest {

    @Test
    fun `api values include high tier h264 fallback variants`() {
        val apiValues = allStreamQualityApiValues()

        assertTrue("qhd1440_h264" in apiValues, "QHD H264 fallback quality must be exposed")
        assertTrue("uhd4k_h264" in apiValues, "4K H264 fallback quality must be exposed")
    }

    @Test
    fun `toApiQualityString stays lowercase and consistent with enum names`() {
        StreamQuality.entries.forEach { quality ->
            assertEquals(quality.name.lowercase(), quality.toApiQualityString())
        }
    }
}
