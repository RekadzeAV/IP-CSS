package com.company.ipcamera.core.network.native

import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MockRtspClientTest {

    @Test
    fun `test fallback client creates successfully`() {
        val config = RtspClientConfig(
            url = "rtsp://192.168.1.100:554/stream",
            allowSimulatedFallback = true
        )

        val client = RtspClient(config)

        // Initial state should be DISCONNECTED
        assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)

        client.close()
    }

    @Test
    fun `test fallback client config has correct values`() {
        val config = RtspClientConfig(
            url = "rtsp://192.168.1.100:554/stream",
            username = "admin",
            password = "password",
            allowSimulatedFallback = true,
            enableVideo = true,
            enableAudio = false,
            timeoutMillis = 10000
        )

        assertEquals("rtsp://192.168.1.100:554/stream", config.url)
        assertEquals("admin", config.username)
        assertEquals("password", config.password)
        assertTrue(config.allowSimulatedFallback)
        assertTrue(config.enableVideo)
        assertFalse(config.enableAudio)
        assertEquals(10000, config.timeoutMillis)
    }

    @Test
    fun `test fallback client with minimal config`() {
        val config = RtspClientConfig(
            url = "rtsp://192.168.1.100:554/stream",
            allowSimulatedFallback = true
        )

        val client = RtspClient(config)

        assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)

        client.close()
    }

    @Test
    fun `test fallback client multiple instances`() {
        val clients = listOf(
            RtspClient(RtspClientConfig(url = "rtsp://192.168.1.100:554/stream1", allowSimulatedFallback = true)),
            RtspClient(RtspClientConfig(url = "rtsp://192.168.1.100:554/stream2", allowSimulatedFallback = true)),
            RtspClient(RtspClientConfig(url = "rtsp://192.168.1.100:554/stream3", allowSimulatedFallback = true))
        )

        // All clients should be in DISCONNECTED state initially
        clients.forEach { client ->
            assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)
        }

        // Close all clients
        clients.forEach { it.close() }
    }

    @Test
    fun `test fallback client different URLs`() {
        val urls = listOf(
            "rtsp://192.168.1.100:554/stream",
            "rtsp://192.168.1.101:554/live",
            "rtsp://camera.local:554/h264",
            "rtsp://user:pass@192.168.1.100:554/stream"
        )

        urls.forEach { url ->
            val client = RtspClient(
                RtspClientConfig(url = url, allowSimulatedFallback = true)
            )

            assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)

            client.close()
        }
    }

    @Test
    fun `test fallback client with audio disabled`() {
        val config = RtspClientConfig(
            url = "rtsp://192.168.1.100:554/stream",
            allowSimulatedFallback = true,
            enableAudio = false
        )

        val client = RtspClient(config)

        assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)

        client.close()
    }

    @Test
    fun `test fallback client with video disabled`() {
        val config = RtspClientConfig(
            url = "rtsp://192.168.1.100:554/stream",
            allowSimulatedFallback = true,
            enableVideo = false
        )

        val client = RtspClient(config)

        assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)

        client.close()
    }

    @Test
    fun `test fallback client with custom timeout`() {
        val timeouts = listOf(1000L, 5000L, 10000L, 30000L)

        timeouts.forEach { timeout ->
            val config = RtspClientConfig(
                url = "rtsp://192.168.1.100:554/stream",
                allowSimulatedFallback = true,
                timeoutMillis = timeout
            )

            val client = RtspClient(config)

            assertEquals(timeout, config.timeoutMillis)
            assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)

            client.close()
        }
    }
}
