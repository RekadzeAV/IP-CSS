package com.company.ipcamera.core.network.rtsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Basic RTSP Client Tests
 * Tests for RTSP client functionality
 */
class RtspClientTest {

    @Test
    fun `test RTSP URL parsing - Hikvision format`() {
        val url = "rtsp://admin:password123@192.168.1.64:554/Streaming/channels/101"
        val parsed = RtspUrlParser.parse(url)

        assertNotNull(parsed)
        assertEquals("192.168.1.64", parsed?.host)
        assertEquals(554, parsed?.port)
        assertEquals("/Streaming/channels/101", parsed?.path)
    }

    @Test
    fun `test RTSP URL parsing - Dahua format`() {
        val url = "rtsp://admin:password@192.168.1.108:554/cam/realmonitor?channel=1&subtype=1"
        val parsed = RtspUrlParser.parse(url)

        assertNotNull(parsed)
        assertEquals("192.168.1.108", parsed?.host)
        assertEquals(554, parsed?.port)
    }

    @Test
    fun `test RTSP URL parsing - Axis format`() {
        val url = "rtsp://192.168.1.90:554/axis-media/media.amp?videostream=raw"
        val parsed = RtspUrlParser.parse(url)

        assertNotNull(parsed)
        assertEquals("192.168.1.90", parsed?.host)
        assertEquals(554, parsed?.port)
    }

    @Test
    fun `test default RTSP port`() {
        val url = "rtsp://192.168.1.64/Streaming/channels/101"
        val parsed = RtspUrlParser.parse(url)

        assertNotNull(parsed)
        assertEquals(554, parsed?.port) // Default RTSP port
    }

    @Test
    fun `test RTSP credentials extraction`() {
        val url = "rtsp://admin:secret123@10.0.0.1:554/stream"
        val parsed = RtspUrlParser.parse(url)

        assertNotNull(parsed)
        assertEquals("admin", parsed?.username)
        assertEquals("secret123", parsed?.password)
    }

    @Test
    fun `test invalid RTSP URL`() {
        val url = "http://invalid-url"
        val parsed = RtspUrlParser.parse(url)

        // Should return null or throw exception for invalid RTSP URL
        assertEquals(null, parsed)
    }
}
