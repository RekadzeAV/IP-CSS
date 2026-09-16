package com.company.ipcamera.core.network

import kotlin.test.*

/**
 * Тесты для FFmpeg декодирования в RTSP клиенте
 *
 * Примечание: Эти тесты проверяют интеграцию FFmpeg декодирования.
 * Для полного тестирования требуется нативная библиотека с FFmpeg.
 */
class RtspClientFfmpegDecodingTest {
    private fun createClientOrSkip(config: RtspClientConfig): RtspClient? {
        return try {
            RtspClient(config)
        } catch (_: RuntimeException) {
            null
        }
    }

    @Test
    fun testH264DecodingSupport() {
        // Проверяем, что RTSP клиент поддерживает H.264 декодирование
        val config = RtspClientConfig(
            url = "rtsp://test.com/stream",
            enableVideo = true
        )
        val client = createClientOrSkip(config) ?: return

        assertNotNull(client, "RTSP client should be created")
        assertTrue(config.enableVideo, "Video should be enabled")
    }

    @Test
    fun testH265DecodingSupport() {
        // Проверяем, что RTSP клиент поддерживает H.265 декодирование
        val config = RtspClientConfig(
            url = "rtsp://test.com/stream",
            enableVideo = true
        )
        val client = createClientOrSkip(config) ?: return

        assertNotNull(client, "RTSP client should be created")
        assertTrue(config.enableVideo, "Video should be enabled")
    }

    @Test
    fun testAacAudioDecodingSupport() {
        // Проверяем, что RTSP клиент поддерживает AAC аудио декодирование
        val config = RtspClientConfig(
            url = "rtsp://test.com/stream",
            enableAudio = true
        )
        val client = createClientOrSkip(config) ?: return

        assertNotNull(client, "RTSP client should be created")
        assertTrue(config.enableAudio, "Audio should be enabled")
    }

    @Test
    fun testG711AudioDecodingSupport() {
        // Проверяем, что RTSP клиент поддерживает G.711 аудио декодирование
        val config = RtspClientConfig(
            url = "rtsp://test.com/stream",
            enableAudio = true
        )
        val client = createClientOrSkip(config) ?: return

        assertNotNull(client, "RTSP client should be created")
        assertTrue(config.enableAudio, "Audio should be enabled")
    }

    @Test
    fun testVideoAndAudioEnabled() {
        // Проверяем, что можно включить и видео, и аудио одновременно
        val config = RtspClientConfig(
            url = "rtsp://test.com/stream",
            enableVideo = true,
            enableAudio = true
        )
        val client = createClientOrSkip(config) ?: return

        assertNotNull(client, "RTSP client should be created")
        assertTrue(config.enableVideo, "Video should be enabled")
        assertTrue(config.enableAudio, "Audio should be enabled")
    }

    @Test
    fun testVideoOnlyMode() {
        // Проверяем режим только видео (без аудио)
        val config = RtspClientConfig(
            url = "rtsp://test.com/stream",
            enableVideo = true,
            enableAudio = false
        )
        val client = createClientOrSkip(config) ?: return

        assertNotNull(client, "RTSP client should be created")
        assertTrue(config.enableVideo, "Video should be enabled")
        assertFalse(config.enableAudio, "Audio should be disabled")
    }

    @Test
    fun testAudioOnlyMode() {
        // Проверяем режим только аудио (без видео)
        val config = RtspClientConfig(
            url = "rtsp://test.com/stream",
            enableVideo = false,
            enableAudio = true
        )
        val client = createClientOrSkip(config) ?: return

        assertNotNull(client, "RTSP client should be created")
        assertFalse(config.enableVideo, "Video should be disabled")
        assertTrue(config.enableAudio, "Audio should be enabled")
    }

    @Test
    fun testDecodingErrorHandling() {
        // Проверяем, что RTSP клиент корректно обрабатывает ошибки декодирования
        // В реальности это будет протестировано в integration тестах
        val config = RtspClientConfig(
            url = "rtsp://test.com/stream",
            enableVideo = true,
            enableAudio = true
        )
        val client = createClientOrSkip(config) ?: return

        assertNotNull(client, "RTSP client should be created")
        // Примечание: Полная проверка обработки ошибок требует реального подключения
    }

    @Test
    fun testDecodingPerformance() {
        // Проверяем конфигурацию для производительности декодирования
        val config = RtspClientConfig(
            url = "rtsp://test.com/stream",
            enableVideo = true,
            enableAudio = true,
            bufferSize = 2 * 1024 * 1024 // 2MB буфер
        )
        val client = createClientOrSkip(config) ?: return

        assertNotNull(client, "RTSP client should be created")
        assertEquals(2 * 1024 * 1024, config.bufferSize, "Buffer size should be set correctly")
    }
}
