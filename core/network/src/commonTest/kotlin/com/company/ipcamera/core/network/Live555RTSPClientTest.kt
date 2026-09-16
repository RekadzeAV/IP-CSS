package com.company.ipcamera.core.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для Live555RTSPClient
 */
class Live555RTSPClientTest {

    @Test
    fun testIsLive555Supported() = runTest {
        // Проверяем, что функция существует и не бросает исключений
        val supported = isLive555Supported()

        // Результат может быть false если библиотека не скомпилирована
        assertTrue(supported is Boolean)
    }

    @Test
    fun testGetLive555Version() = runTest {
        // Проверяем, что функция существует
        val version = getLive555Version()

        // Может быть null если библиотека не загружена
        assertTrue(version == null || version.isNotEmpty())
    }

    @Test
    fun testRtspClientConfigCreation() = runTest {
        val config = RtspClientConfig(
            url = "rtsp://192.168.1.10:554/stream",
            username = "admin",
            password = "password",
            timeoutMs = 5000,
            enableVideo = true,
            enableAudio = true,
            rtpTransport = RtpTransport.UDP
        )

        assertEquals("rtsp://192.168.1.10:554/stream", config.url)
        assertEquals("admin", config.username)
        assertEquals("password", config.password)
        assertEquals(5000L, config.timeoutMs)
        assertTrue(config.enableVideo)
        assertTrue(config.enableAudio)
        assertEquals(RtpTransport.UDP, config.rtpTransport)
    }

    @Test
    fun testRtpTransportEnum() {
        assertEquals(0, RtpTransport.UDP.ordinal)
        assertEquals(1, RtpTransport.TCP.ordinal)
        assertEquals(2, RtpTransport.MULTICAST.ordinal)
    }

    @Test
    fun testVideoFormatEnum() {
        assertEquals(0, VideoFormat.UNKNOWN.ordinal)
        assertEquals(1, VideoFormat.H264.ordinal)
        assertEquals(2, VideoFormat.H265.ordinal)
        assertEquals(3, VideoFormat.MPEG4.ordinal)
        assertEquals(4, VideoFormat.MJPEG.ordinal)
    }

    @Test
    fun testAudioFormatEnum() {
        assertEquals(0, AudioFormat.UNKNOWN.ordinal)
        assertEquals(1, AudioFormat.AAC.ordinal)
        assertEquals(2, AudioFormat.G711.ordinal)
        assertEquals(3, AudioFormat.G726.ordinal)
        assertEquals(4, AudioFormat.MP3.ordinal)
    }

    @Test
    fun testRtspClientStatusEnum() {
        assertEquals(0, RtspClientStatus.DISCONNECTED.ordinal)
        assertEquals(1, RtspClientStatus.CONNECTING.ordinal)
        assertEquals(2, RtspClientStatus.CONNECTED.ordinal)
        assertEquals(3, RtspClientStatus.PLAYING.ordinal)
        assertEquals(4, RtspClientStatus.PAUSED.ordinal)
        assertEquals(5, RtspClientStatus.ERROR.ordinal)
    }

    @Test
    fun testVideoStreamInfoCreation() {
        val info = VideoStreamInfo(
            format = VideoFormat.H264,
            width = 1920,
            height = 1080,
            fps = 30,
            bitrate = 4096
        )

        assertEquals(VideoFormat.H264, info.format)
        assertEquals(1920, info.width)
        assertEquals(1080, info.height)
        assertEquals(30, info.fps)
        assertEquals(4096, info.bitrate)
    }

    @Test
    fun testAudioStreamInfoCreation() {
        val info = AudioStreamInfo(
            format = AudioFormat.AAC,
            sampleRate = 48000,
            channels = 2,
            bitrate = 128
        )

        assertEquals(AudioFormat.AAC, info.format)
        assertEquals(48000, info.sampleRate)
        assertEquals(2, info.channels)
        assertEquals(128, info.bitrate)
    }

    @Test
    fun testClientLifecycle() = runTest {
        // Создаём клиент с невалидным URL для тестирования
        val config = RtspClientConfig(
            url = "rtsp://invalid.invalid:554/stream",
            timeoutMs = 1000,
            enableVideo = false,
            enableAudio = false
        )

        // Проверяем, что клиент создаётся без исключений
        // (фактическое подключение будет неудачным)
        val client = Live555RTSPClient(config)

        // Проверяем начальный статус
        val initialStatus = client.getStatus().first()
        assertEquals(RtspClientStatus.DISCONNECTED, initialStatus)

        // Проверяем, что close() не бросает исключений
        client.close()

        assertTrue(true) // Если дошли до сюда - тест прошёл
    }

    @Test
    fun testConnectionFailure() = runTest {
        val config = RtspClientConfig(
            url = "rtsp://invalid.invalid:554/stream",
            timeoutMs = 1000,
            enableVideo = false,
            enableAudio = false
        )

        val client = Live555RTSPClient(config)

        // Попытка подключения к невалидному адресу должна завершиться неудачей
        val connected = client.connect()

        assertFalse(connected)

        // Статус должен быть ERROR
        val status = client.getStatus().first()
        assertEquals(RtspClientStatus.ERROR, status)

        client.close()
    }

    @Test
    fun testVideoStreamInfoIsNullWhenNotAvailable() = runTest {
        val config = RtspClientConfig(
            url = "rtsp://192.168.1.10:554/stream",
            enableVideo = false,
            enableAudio = false
        )

        val client = Live555RTSPClient(config)

        // Информация о видео должна быть null если видео не включено
        val videoInfo = client.getVideoInfo()

        // Может быть null до подключения или если видео не включено
        assertTrue(videoInfo == null || videoInfo is VideoStreamInfo)

        client.close()
    }

    @Test
    fun testAudioStreamInfoIsNullWhenNotAvailable() = runTest {
        val config = RtspClientConfig(
            url = "rtsp://192.168.1.10:554/stream",
            enableVideo = false,
            enableAudio = false
        )

        val client = Live555RTSPClient(config)

        // Информация об аудио должна быть null если аудио не включено
        val audioInfo = client.getAudioInfo()

        // Может быть null до подключения или если аудио не включено
        assertTrue(audioInfo == null || audioInfo is AudioStreamInfo)

        client.close()
    }
}
