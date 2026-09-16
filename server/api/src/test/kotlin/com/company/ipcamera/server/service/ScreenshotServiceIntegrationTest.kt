package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration test для ScreenshotService с реальным FFmpeg.
 * Требует: FFMPEG_PATH настроен или ffmpeg в PATH
 */
class ScreenshotServiceIntegrationTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var screenshotService: ScreenshotService

    @BeforeEach
    fun setUp() {
        screenshotService = ScreenshotService(
            screenshotsDirectory = tempDir.toString()
        )
    }

    @Test
    fun `captureFrame with valid H264 frame should not crash`() = runBlocking {
        // Skip if FFmpeg not available
        assumeFfmpegAvailable()

        // Create a minimal valid H264 frame (mock data)
        val mockH264Data = byteArrayOf(
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
            0x67.toByte(), 0x42.toByte(), 0x00.toByte(), 0x1e.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
            0x68.toByte(), 0xce.toByte(), 0x38.toByte(), 0x80.toByte()
        )

        val frame = RtspFrame(
            data = mockH264Data,
            timestamp = System.currentTimeMillis(),
            streamType = RtspStreamType.VIDEO
        )

        val result = screenshotService.captureFrame(frame, cameraId = "test-camera-1")

        // Результат может быть null если FFmpeg не может декодировать mock-данные
        // Главное проверяем что сервис работает без crash
    }

    @Test
    fun `captureFrame with empty data returns null`() = runBlocking {
        val frame = RtspFrame(
            data = ByteArray(0),
            timestamp = 0L,
            streamType = RtspStreamType.VIDEO
        )

        val result = screenshotService.captureFrame(frame, cameraId = "test-camera")
        assertNull(result, "Empty frame should return null")
    }

    @Test
    fun `captureFrame with non-video frame returns null`() = runBlocking {
        val frame = RtspFrame(
            data = byteArrayOf(1, 2, 3),
            timestamp = 0L,
            streamType = RtspStreamType.AUDIO
        )

        val result = screenshotService.captureFrame(frame, cameraId = "test-camera")
        assertNull(result, "Non-video frame should return null")
    }

    @Test
    fun `getScreenshotUrl returns correct API path`() {
        val fullPath = File(tempDir.toFile(), "camera1_1234567890.jpg").absolutePath
        val url = screenshotService.getScreenshotUrl(fullPath)

        assertEquals("/api/v1/screenshots/camera1_1234567890.jpg", url)
    }

    @Test
    fun `cleanupOldScreenshots removes old files and keeps recent ones`() {
        val oldFile = File(tempDir.toFile(), "old.jpg")
        oldFile.writeBytes(byteArrayOf(1, 2, 3))
        val oldTime = System.currentTimeMillis() - (26 * 60 * 60 * 1000)
        assertTrue(oldFile.setLastModified(oldTime), "setLastModified(old) failed")

        val recentFile = File(tempDir.toFile(), "recent.jpg")
        recentFile.writeBytes(byteArrayOf(1, 2, 3))
        val recentTime = System.currentTimeMillis() - (12 * 60 * 60 * 1000)
        assertTrue(recentFile.setLastModified(recentTime), "setLastModified(recent) failed")

        screenshotService.cleanupOldScreenshots(maxAgeHours = 24)

        assertFalse(oldFile.exists(), "Old file should be removed")
        assertTrue(recentFile.exists(), "Recent file should remain")
    }

    @Test
    fun `captureFromRsp handles missing FFmpeg gracefully`() = runBlocking {
        val testRtspUrl = "rtsp://127.0.0.1:8554/test"
        
        val result = screenshotService.captureFromRtsp(
            rtspUrl = testRtspUrl,
            cameraId = "test-camera",
            username = null,
            password = null
        )

        // Если FFmpeg или RTSP сервер недоступны, вернется null
    }

    @Test
    fun `captureFromRtsp with authentication`() = runBlocking {
        val testRtspUrl = "rtsp://127.0.0.1:8554/test"
        
        screenshotService.captureFromRtsp(
            rtspUrl = testRtspUrl,
            cameraId = "test-camera",
            username = "admin",
            password = "password123"
        )
    }

    private fun assumeFfmpegAvailable() {
        val ffmpegPath = System.getenv("FFMPEG_PATH") ?: "ffmpeg"
        try {
            val process = ProcessBuilder(ffmpegPath, "-version")
                .redirectErrorStream(true)
                .start()
            
            val exited = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            if (!exited) process.destroyForcibly()
            
            val exitCode = process.exitValue()
            if (exitCode != 0) {
                throw RuntimeException("FFmpeg not available (exit code: $exitCode)")
            }
        } catch (e: Exception) {
            throw RuntimeException("FFmpeg not available: ${e.message}", e)
        }
    }
}
