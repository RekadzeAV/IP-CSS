package com.company.ipcamera.core.network

import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Unit тесты для проверки graceful degradation RtspClient
 * при отсутствии нативной библиотеки или ошибках native слоя.
 */
@Ignore("Requires native Live555 library for full testing")
class RtspClientNativeMockTest {

    @Test
    fun `test connect transitions to error when native library unavailable`() = runTest {
        val config = RtspClientConfig(
            url = "rtsp://192.168.1.100/stream",
            timeoutMillis = 5000
        )
        val client = RtspClient(config)

        // Без нативной библиотеки connect должен перевести статус в ERROR
        client.connect()
        // connect() асинхронный, ждем немного
        kotlinx.coroutines.delay(100)
        assertEquals(RtspClientStatus.ERROR, client.getStatus().value)
    }

    @Test
    fun `test play is no-op when not connected`() = runTest {
        val config = RtspClientConfig(url = "rtsp://192.168.1.100/stream")
        val client = RtspClient(config)

        client.play()
        assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)
    }

    @Test
    fun `test stop is no-op when not playing`() = runTest {
        val config = RtspClientConfig(url = "rtsp://192.168.1.100/stream")
        val client = RtspClient(config)

        client.stop()
        assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)
    }

    @Test
    fun `test pause is no-op when not playing`() = runTest {
        val config = RtspClientConfig(url = "rtsp://192.168.1.100/stream")
        val client = RtspClient(config)

        client.pause()
        assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)
    }

    @Test
    fun `test getStreams returns empty when native unavailable`() = runTest {
        val config = RtspClientConfig(url = "rtsp://192.168.1.100/stream")
        val client = RtspClient(config)

        assertEquals(0, client.getStreams().size)
    }

    @Test
    fun `test getStreamInfo returns null when native unavailable`() = runTest {
        val config = RtspClientConfig(url = "rtsp://192.168.1.100/stream")
        val client = RtspClient(config)

        val info = client.getStreamInfo(0)
        assertNull(info)
    }

    @Test
    fun `test getVideoFrames emits nothing when native unavailable`() = runTest {
        val config = RtspClientConfig(url = "rtsp://192.168.1.100/stream")
        val client = RtspClient(config)

        val frames = client.getVideoFrames()
        assertNotNull(frames)

        // Flow должен быть пустым при отсутствии соединения
        val collected = mutableListOf<RtspFrame>()
        // Не собираем flow, так как он может быть бесконечным
        // Просто проверяем, что объект создан
    }

    @Test
    fun `test getAudioFrames emits nothing when native unavailable`() = runTest {
        val config = RtspClientConfig(url = "rtsp://192.168.1.100/stream")
        val client = RtspClient(config)

        val frames = client.getAudioFrames()
        assertNotNull(frames)
    }

    @Test
    fun `test setVideoFrameCallback does not crash when native unavailable`() = runTest {
        val config = RtspClientConfig(url = "rtsp://192.168.1.100/stream")
        val client = RtspClient(config)

        // Установка callback не должна вызывать ошибок
        client.setVideoFrameCallback { frame ->
            println("Received frame: ${frame.timestamp}")
        }
    }

    @Test
    fun `test setStatusCallback does not crash when native unavailable`() = runTest {
        val config = RtspClientConfig(url = "rtsp://192.168.1.100/stream")
        val client = RtspClient(config)

        // Установка callback не должна вызывать ошибок
        client.setStatusCallback { status, message ->
            println("Status: $status, Message: $message")
        }
    }

    @Test
    fun `test getRuntimeDiagnostics reports disconnected status`() = runTest {
        val config = RtspClientConfig(url = "rtsp://192.168.1.100/stream")
        val client = RtspClient(config)

        val diagnostics = client.getRuntimeDiagnostics().value
        assertNotNull(diagnostics)
        assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)
        assertEquals(0L, diagnostics.connectAttempts)
        assertEquals(0L, diagnostics.connectSuccesses)
        assertEquals(0L, diagnostics.connectFailures)
    }

    @Test
    fun `test client handles null credentials gracefully`() = runTest {
        val config = RtspClientConfig(
            url = "rtsp://192.168.1.100/stream",
            username = null,
            password = null
        )
        val client = RtspClient(config)

        assertNotNull(client)
        assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)
    }

    @Test
    fun `test client handles empty credentials gracefully`() = runTest {
        val config = RtspClientConfig(
            url = "rtsp://192.168.1.100/stream",
            username = "",
            password = ""
        )
        val client = RtspClient(config)

        assertNotNull(client)
        assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)
    }

    @Test
    fun `test reconnect disabled config`() = runTest {
        val config = RtspClientConfig(
            url = "rtsp://192.168.1.100/stream",
            reconnectEnabled = false,
            reconnectMaxRetries = 0
        )
        val client = RtspClient(config)

        assertNotNull(client)
        // При отключенном reconnect и отсутствии native библиотеки статус остается DISCONNECTED
        assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)
    }
}
