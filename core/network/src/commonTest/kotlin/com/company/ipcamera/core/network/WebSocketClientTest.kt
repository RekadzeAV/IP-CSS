package com.company.ipcamera.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebSocketClientTest {

    @Test
    fun testWebSocketClientConfigDefault() {
        val config = WebSocketClientConfig(url = "ws://example.com/ws")

        assertEquals("ws://example.com/ws", config.url)
        assertTrue(config.autoReconnect)
        assertEquals(5000L, config.reconnectDelayMillis)
        assertEquals(Int.MAX_VALUE, config.maxReconnectAttempts)
        assertEquals(30000L, config.pingIntervalMillis)
        assertTrue(config.enableLogging)
        assertFalse(config.enableCompression)
    }

    @Test
    fun testWebSocketMessageTypes() {
        val authMessage = WebSocketMessage.AuthMessage("token123")
        assertEquals("token123", authMessage.token)

        val subscribeMessage = WebSocketMessage.SubscribeMessage(
            channels = listOf("camera1", "camera2"),
            filters = mapOf("type" to "motion")
        )
        assertEquals(2, subscribeMessage.channels.size)
        assertNotNull(subscribeMessage.filters)

        val unsubscribeMessage = WebSocketMessage.UnsubscribeMessage(
            channels = listOf("camera1")
        )
        assertEquals(1, unsubscribeMessage.channels.size)

        val eventMessage = WebSocketMessage.EventMessage(
            type = "motion",
            channel = "camera1",
            data = mapOf("timestamp" to "1234567890")
        )
        assertEquals("motion", eventMessage.type)
        assertEquals("camera1", eventMessage.channel)

        val errorMessage = WebSocketMessage.ErrorMessage(
            error = "Connection failed",
            code = "CONNECTION_ERROR"
        )
        assertEquals("Connection failed", errorMessage.error)
        assertEquals("CONNECTION_ERROR", errorMessage.code)

        val binaryMessage = WebSocketMessage.BinaryMessage(byteArrayOf(1, 2, 3, 4))
        assertEquals(4, binaryMessage.data.size)
    }

    @Test
    fun testWebSocketConnectionState() {
        val states = WebSocketConnectionState.values()
        assertTrue(states.contains(WebSocketConnectionState.DISCONNECTED))
        assertTrue(states.contains(WebSocketConnectionState.CONNECTING))
        assertTrue(states.contains(WebSocketConnectionState.CONNECTED))
        assertTrue(states.contains(WebSocketConnectionState.RECONNECTING))
        assertTrue(states.contains(WebSocketConnectionState.FAILED))
    }
}

