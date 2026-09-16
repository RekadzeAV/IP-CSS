package com.company.ipcamera.core.network

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
            filters = buildJsonObject {
                put("type", JsonPrimitive("motion"))
            }
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
            data = buildJsonObject {
                put("timestamp", JsonPrimitive("1234567890"))
            }
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

    @Test
    fun testBinaryMessageTypes() {
        val metadata = BinaryMessageMetadata(
            type = "IMAGE",
            mimeType = "image/jpeg",
            size = 3L,
            messageId = "msg-1"
        )
        val imageMessage = WebSocketMessage.ImageMessage(
            data = byteArrayOf(1, 2, 3),
            metadata = metadata
        )
        assertEquals("IMAGE", imageMessage.metadata.type)
        assertEquals("image/jpeg", imageMessage.metadata.mimeType)
        assertEquals(3L, imageMessage.metadata.size)

        val videoChunkMessage = WebSocketMessage.VideoChunkMessage(
            data = byteArrayOf(1, 2, 3),
            metadata = metadata.copy(type = "VIDEO_CHUNK", mimeType = "video/h264")
        )
        assertEquals("VIDEO_CHUNK", videoChunkMessage.metadata.type)
        assertEquals("video/h264", videoChunkMessage.metadata.mimeType)

        val fileMessage = WebSocketMessage.FileMessage(
            data = byteArrayOf(1, 2, 3),
            metadata = metadata.copy(type = "FILE"),
            fileName = "test.jpg"
        )
        assertEquals("test.jpg", fileMessage.fileName)
        assertEquals("FILE", fileMessage.metadata.type)
    }

    @Test
    fun testCustomBinaryMessage() {
        val customMessage = WebSocketMessage.CustomBinaryMessage(
            data = byteArrayOf(1, 2, 3),
            metadata = BinaryMessageMetadata(
                type = "CUSTOM",
                mimeType = "application/octet-stream",
                size = 3L,
                messageId = "msg-custom"
            ),
            format = "custom"
        )
        assertEquals("custom", customMessage.format)
        assertEquals("CUSTOM", customMessage.metadata.type)
    }
}
