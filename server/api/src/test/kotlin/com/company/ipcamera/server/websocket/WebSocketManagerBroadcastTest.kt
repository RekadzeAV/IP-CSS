package com.company.ipcamera.server.websocket

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebSocketManagerBroadcastTest {

    @AfterTest
    fun tearDown() {
        WebSocketManager.sessionManager.clearAll()
        clearAllMocks()
    }

    @Test
    fun `broadcast stream_started delivers frame source and rtsp error payload to cameras subscribers`() = runBlocking {
        val session = mockk<WebSocketSession>(relaxed = true)
        coEvery { session.send(any<Frame>()) } returns Unit

        val sessionId = "ws-test-1"
        WebSocketManager.sessionManager.addSession(sessionId, session)
        assertTrue(WebSocketManager.sessionManager.subscribe(sessionId, WebSocketChannel.CAMERAS))
        assertEquals(1, WebSocketManager.sessionManager.getSubscribersCount(WebSocketChannel.CAMERAS))

        val payload = buildJsonObject {
            put("cameraId", "cam-1")
            put("streamId", "stream-1")
            put("analyticsFrameSource", "NONE")
            put("rtspConnectError", "Native RTSP unavailable or connection failed")
        }

        WebSocketManager.broadcastEvent(
            channel = WebSocketChannel.CAMERAS,
            type = "stream_started",
            data = payload
        )

        coVerify(exactly = 1) { session.send(match<Frame.Text> { frame ->
            val text = frame.readText()
            text.contains("\"type\":\"stream_started\"") &&
                text.contains("\"channel\":\"cameras\"") &&
                text.contains("\"analyticsFrameSource\":\"NONE\"") &&
                text.contains("\"rtspConnectError\":\"Native RTSP unavailable or connection failed\"")
        }) }
    }

    @Test
    fun `broadcast to cameras does not send to sessions subscribed only on analytics`() = runBlocking {
        val analyticsOnly = mockk<WebSocketSession>(relaxed = true)
        coEvery { analyticsOnly.send(any<Frame>()) } returns Unit

        val sid = "ws-test-analytics"
        WebSocketManager.sessionManager.addSession(sid, analyticsOnly)
        assertTrue(WebSocketManager.sessionManager.subscribe(sid, WebSocketChannel.ANALYTICS))
        assertEquals(0, WebSocketManager.sessionManager.getSubscribersCount(WebSocketChannel.CAMERAS))
        assertEquals(1, WebSocketManager.sessionManager.getSubscribersCount(WebSocketChannel.ANALYTICS))

        WebSocketManager.broadcastEvent(
            channel = WebSocketChannel.CAMERAS,
            type = "stream_started",
            data = buildJsonObject {
                put("cameraId", "cam-2")
                put("analyticsFrameSource", "RTSP_DECODED")
            }
        )

        coVerify(exactly = 0) { analyticsOnly.send(any<Frame>()) }
    }
}

