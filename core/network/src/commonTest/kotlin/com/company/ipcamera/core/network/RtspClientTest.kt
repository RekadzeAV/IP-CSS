package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.RtspClientStatus.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * Unit тесты для RTSP клиента
 *
 * Примечание: Эти тесты проверяют логику Kotlin обертки.
 * Для полного тестирования требуется нативная библиотека.
 */
class RtspClientTest {
    private fun createClientOrSkip(config: RtspClientConfig): RtspClient? {
        return try {
            RtspClient(config)
        } catch (_: RuntimeException) {
            // Native runtime not available in current environment.
            null
        }
    }

    @Test
    fun testRtspClientCreation() {
        val config = RtspClientConfig(
            url = "rtsp://test.com/stream",
            username = "test",
            password = "test123"
        )
        val client = createClientOrSkip(config) ?: return
        assertNotNull(client)
        assertEquals(DISCONNECTED, client.getStatus().value)
    }

    @Test
    fun testRtspClientConfigDefaults() {
        val config = RtspClientConfig(url = "rtsp://test.com/stream")
        assertEquals("rtsp://test.com/stream", config.url)
        assertNull(config.username)
        assertNull(config.password)
        assertEquals(10000L, config.timeoutMillis)
        assertEquals(1024 * 1024, config.bufferSize)
        assertTrue(config.enableVideo)
        assertTrue(config.enableAudio)
        assertFalse(config.enableMetadata)
        assertFalse(config.allowSimulatedFallback)
        assertTrue(config.reconnectEnabled)
        assertEquals(5, config.reconnectMaxRetries)
        assertEquals(500, config.reconnectInitialDelayMs)
        assertEquals(10_000, config.reconnectMaxDelayMs)
        assertEquals(2.0f, config.reconnectBackoffMultiplier)
        assertEquals(0.15f, config.reconnectJitterRatio)
    }

    @Test
    fun testRtspClientInitialStatus() = runTest {
        val config = RtspClientConfig(url = "rtsp://test.com/stream")
        val client = createClientOrSkip(config) ?: return@runTest

        val status = client.getStatus().first()
        assertEquals(DISCONNECTED, status)
    }

    @Test
    fun testRtspClientGetStreamsBeforeConnect() {
        val config = RtspClientConfig(url = "rtsp://test.com/stream")
        val client = createClientOrSkip(config) ?: return

        val streams = client.getStreams()
        assertTrue(streams.isEmpty())
    }

    @Test
    fun testRtspClientGetStreamInfoBeforeConnect() {
        val config = RtspClientConfig(url = "rtsp://test.com/stream")
        val client = createClientOrSkip(config) ?: return

        val streamInfo = client.getStreamInfo(0)
        assertNull(streamInfo)
    }

    @Test
    fun testRtspClientDetectAudioCodecAAC() {
        val config = RtspClientConfig(url = "rtsp://test.com/stream")
        val client = createClientOrSkip(config) ?: return

        // AAC ADTS заголовок: 0xFF 0xF1
        val aacData = byteArrayOf(0xFF.toByte(), 0xF1.toByte(), 0x00, 0x00)
        val codec = client.detectAudioCodec(aacData)
        assertEquals("AAC", codec)
    }

    @Test
    fun testRtspClientDetectAudioCodecEmpty() {
        val config = RtspClientConfig(url = "rtsp://test.com/stream")
        val client = createClientOrSkip(config) ?: return

        val codec = client.detectAudioCodec(ByteArray(0))
        assertNull(codec)
    }

    @Test
    fun testRtspClientGetAudioCodecsEmpty() {
        val config = RtspClientConfig(url = "rtsp://test.com/stream")
        val client = createClientOrSkip(config) ?: return

        val codecs = client.getAudioCodecs()
        assertTrue(codecs.isEmpty())
    }

    @Test
    fun testRtspClientClose() = runTest {
        val config = RtspClientConfig(url = "rtsp://test.com/stream")
        val client = createClientOrSkip(config) ?: return@runTest

        client.close()

        val status = client.getStatus().first()
        assertEquals(DISCONNECTED, status)
    }

    @Test
    fun testRtspClientPlayWhenDisconnectedIsNoOp() = runBlocking {
        val client = createClientOrSkip(RtspClientConfig(url = "rtsp://127.0.0.1:1/none")) ?: return@runBlocking
        client.play()
        assertEquals(DISCONNECTED, client.getStatus().value)
    }

    /**
     * Явный симуляционный fallback: CONNECTED → PLAYING → CONNECTED → DISCONNECTED.
     */
    @Test
    fun testRtspClientFallbackConnectPlayStopDisconnectLifecycle() = runBlocking {
        val config = RtspClientConfig(
            url = "rtsp://127.0.0.1:1/nosuch",
            enableVideo = true,
            enableAudio = false,
            timeoutMillis = 3000L,
            allowSimulatedFallback = true
        )
        val client = createClientOrSkip(config) ?: return@runBlocking
        client.connect()
        delay(1500)
        when (val st = client.getStatus().value) {
            CONNECTED -> {
                client.play()
                delay(120)
                assertEquals(PLAYING, client.getStatus().value)
                client.stop()
                assertEquals(CONNECTED, client.getStatus().value)
                client.disconnect()
                assertEquals(DISCONNECTED, client.getStatus().value)
                assertTrue(client.getStreams().isEmpty())
            }
            ERROR -> {
                client.disconnect()
                assertEquals(DISCONNECTED, client.getStatus().value)
            }
            else -> fail("unexpected status after connect: $st")
        }
    }

    @Test
    fun testRtspClientDisconnectDuringFallbackConnectLeavesDisconnected() = runBlocking {
        val config = RtspClientConfig(
            url = "rtsp://127.0.0.1:1/nosuch",
            enableVideo = true,
            enableAudio = false,
            timeoutMillis = 60_000L,
            allowSimulatedFallback = true
        )
        val client = createClientOrSkip(config) ?: return@runBlocking
        val connectJob = launch { client.connect() }
        delay(50)
        client.disconnect()
        connectJob.join()
        assertEquals(DISCONNECTED, client.getStatus().value)
        assertTrue(client.getStreams().isEmpty())
    }

    @Test
    fun testRtspClientNativeFailureIsErrorWhenSimulationDisabled() = runBlocking {
        val config = RtspClientConfig(
            url = "rtsp://127.0.0.1:1/nosuch",
            enableVideo = true,
            enableAudio = false,
            timeoutMillis = 3000L,
            allowSimulatedFallback = false
        )
        val client = createClientOrSkip(config) ?: return@runBlocking
        client.connect()
        val deadline = System.currentTimeMillis() + 8000L
        while (System.currentTimeMillis() < deadline) {
            when (client.getStatus().value) {
                ERROR -> {
                    client.disconnect()
                    assertEquals(DISCONNECTED, client.getStatus().value)
                    return@runBlocking
                }
                CONNECTED -> {
                    fail("did not expect CONNECTED without native when allowSimulatedFallback=false")
                }
                else -> delay(50)
            }
        }
        fail("expected ERROR within timeout, got ${client.getStatus().value}")
    }

    @Test
    fun testRtspClientSetCallbacks() = runTest {
        val config = RtspClientConfig(url = "rtsp://test.com/stream")
        val client = createClientOrSkip(config) ?: return@runTest

        var videoFrameReceived = false
        var audioFrameReceived = false
        var statusChanged = false

        client.setVideoFrameCallback { frame ->
            videoFrameReceived = true
        }

        client.setAudioFrameCallback { frame ->
            audioFrameReceived = true
        }

        client.setStatusCallback { status, message ->
            statusChanged = true
        }

        // Callbacks установлены, но не будут вызваны без подключения
        assertFalse(videoFrameReceived)
        assertFalse(audioFrameReceived)
        assertFalse(statusChanged)
    }

    @Test
    fun testRtspClientReconnectWithBackoffReturnsFalseWhenUnreachable() = runBlocking {
        val client = createClientOrSkip(
            RtspClientConfig(
                url = "rtsp://127.0.0.1:1/unreachable",
                timeoutMillis = 800L,
                allowSimulatedFallback = false,
                reconnectMaxRetries = 2,
                reconnectInitialDelayMs = 50,
                reconnectMaxDelayMs = 100,
                reconnectBackoffMultiplier = 2.0f
            )
        ) ?: return@runBlocking

        val ok = client.reconnectWithBackoff(maxAttempts = 2, initialDelayMs = 50, maxDelayMs = 100)
        assertFalse(ok)
        val diagnostics = client.getRuntimeDiagnostics().value
        assertTrue(diagnostics.reconnectAttempts >= 2)
        assertTrue(diagnostics.reconnectFailures >= 2)
        client.disconnect()
        assertEquals(DISCONNECTED, client.getStatus().value)
    }

    @Test
    fun testRtspClientFallbackProducesFrameTimestampDiagnostics() = runBlocking {
        val client = createClientOrSkip(
            RtspClientConfig(
                url = "rtsp://127.0.0.1:1/fallback",
                allowSimulatedFallback = true,
                enableVideo = true,
                enableAudio = false,
                timeoutMillis = 1200L
            )
        ) ?: return@runBlocking

        client.connect()
        val connectDeadline = System.currentTimeMillis() + 5000L
        while (System.currentTimeMillis() < connectDeadline && client.getStatus().value == CONNECTING) {
            delay(50)
        }
        if (client.getStatus().value != CONNECTED) {
            client.disconnect()
            fail("expected CONNECTED for fallback path, got ${client.getStatus().value}")
        }

        client.play()
        delay(200)
        val diagnostics = client.getRuntimeDiagnostics().value
        assertNotNull(diagnostics.lastPlayingAt)
        assertNotNull(diagnostics.lastFrameAt)
        assertTrue(diagnostics.lastFrameAt!! >= (diagnostics.lastConnectedAt ?: 0L))

        client.disconnect()
        assertEquals(DISCONNECTED, client.getStatus().value)
    }

    @Test
    fun testRtspClientClearCallbacks() = runTest {
        val config = RtspClientConfig(url = "rtsp://test.com/stream")
        val client = createClientOrSkip(config) ?: return@runTest

        client.setVideoFrameCallback { }
        client.setAudioFrameCallback { }
        client.setStatusCallback { _, _ -> }

        // Очистка callbacks
        client.setVideoFrameCallback(null)
        client.setAudioFrameCallback(null)
        client.setStatusCallback(null)

        // Должно работать без ошибок
        assertTrue(true)
    }

    @Test
    fun testRtspFrameDataClass() {
        val frame = RtspFrame(
            data = byteArrayOf(1, 2, 3, 4),
            timestamp = 12345L,
            streamType = RtspStreamType.VIDEO,
            width = 1920,
            height = 1080
        )

        assertEquals(4, frame.data.size)
        assertEquals(12345L, frame.timestamp)
        assertEquals(RtspStreamType.VIDEO, frame.streamType)
        assertEquals(1920, frame.width)
        assertEquals(1080, frame.height)
    }

    @Test
    fun testRtspStreamInfoDataClass() {
        val resolution = com.company.ipcamera.core.common.model.Resolution(1920, 1080)
        val streamInfo = RtspStreamInfo(
            index = 0,
            type = RtspStreamType.VIDEO,
            resolution = resolution,
            fps = 25,
            codec = "H.264"
        )

        assertEquals(0, streamInfo.index)
        assertEquals(RtspStreamType.VIDEO, streamInfo.type)
        assertEquals(resolution, streamInfo.resolution)
        assertEquals(25, streamInfo.fps)
        assertEquals("H.264", streamInfo.codec)
    }

    @Test
    fun testRtspStreamInfoWithAudio() {
        val streamInfo = RtspStreamInfo(
            index = 1,
            type = RtspStreamType.AUDIO,
            resolution = null,
            fps = 0,
            codec = "AAC",
            audioCodec = "AAC",
            sampleRate = 44100,
            channels = 2
        )

        assertEquals(RtspStreamType.AUDIO, streamInfo.type)
        assertEquals("AAC", streamInfo.codec)
        assertEquals("AAC", streamInfo.audioCodec)
        assertEquals(44100, streamInfo.sampleRate)
        assertEquals(2, streamInfo.channels)
    }

    @Test
    fun testRtspClientStatusEnum() {
        val statuses = listOf(
            DISCONNECTED,
            CONNECTING,
            CONNECTED,
            PLAYING,
            ERROR
        )

        assertEquals(5, statuses.size)
        assertTrue(statuses.contains(DISCONNECTED))
        assertTrue(statuses.contains(ERROR))
    }

    @Test
    fun testRtspStreamTypeEnum() {
        val types = listOf(
            RtspStreamType.VIDEO,
            RtspStreamType.AUDIO,
            RtspStreamType.METADATA
        )

        assertEquals(3, types.size)
        assertTrue(types.contains(RtspStreamType.VIDEO))
        assertTrue(types.contains(RtspStreamType.AUDIO))
    }
}
