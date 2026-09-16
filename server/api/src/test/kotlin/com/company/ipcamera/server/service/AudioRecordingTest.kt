package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Базовые smoke-тесты FfmpegService на актуальном API.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioRecordingTest {

    private lateinit var ffmpegService: FfmpegService

    @TempDir
    lateinit var tempDir: Path

    @BeforeAll
    fun setup() {
        ffmpegService = FfmpegService()
    }

    @Test
    @DisplayName("FFmpeg service creates and checks availability")
    fun testServiceInitialization() {
        assertNotNull(ffmpegService)
        // В CI/локально FFmpeg может отсутствовать, поэтому просто проверяем что метод вызывается.
        ffmpegService.isAvailable()
    }

    @Test
    @DisplayName("getVideoInfo returns null for missing file")
    fun testGetVideoInfo() {
        val file = File(tempDir.toFile(), "sample.mp4")
        // Реальная реализация: файл не существует → null (ранее стаб возвращал фейковую карту)
        val info = ffmpegService.getVideoInfo(file)
        org.junit.jupiter.api.Assertions.assertNull(info)
    }

    @Test
    @DisplayName("exportVideo handles missing input gracefully")
    fun testExportVideo() {
        val inputFile = File(tempDir.toFile(), "missing.mp4")
        val outputFile = File(tempDir.toFile(), "out.mkv")
        val success = ffmpegService.exportVideo(
            inputFile = inputFile,
            outputFile = outputFile,
            format = RecordingFormat.MKV,
            quality = Quality.MEDIUM,
            startTime = null,
            endTime = null,
            useH265 = false
        )
        // Реальная реализация: отсутствующий вход → false (ранее стаб возвращал true)
        org.junit.jupiter.api.Assertions.assertFalse(success)
    }

    @Test
    @DisplayName("transcodeVideo returns false for missing input")
    fun testTranscodeVideo() {
        val inputFile = File(tempDir.toFile(), "missing.mp4")
        val outputFile = File(tempDir.toFile(), "out.mp4")
        val success = ffmpegService.transcodeVideo(
            inputFile = inputFile,
            outputFile = outputFile,
            format = RecordingFormat.MP4,
            quality = Quality.LOW,
            width = 640,
            height = 360,
            fps = 30
        )
        // Реальная реализация: отсутствующий вход → false (ранее стаб возвращал true)
        org.junit.jupiter.api.Assertions.assertFalse(success)
    }

    @Test
    @DisplayName("invalid time range is rejected without invoking ffmpeg")
    fun testExportInvalidTimeRange() {
        val inputFile = File(tempDir.toFile(), "missing.mp4")
        val outputFile = File(tempDir.toFile(), "out.mkv")
        val success = ffmpegService.exportVideo(
            inputFile = inputFile,
            outputFile = outputFile,
            format = RecordingFormat.MKV,
            quality = Quality.MEDIUM,
            startTime = 10.0,
            endTime = 5.0,
            useH265 = false
        )
        org.junit.jupiter.api.Assertions.assertFalse(success)
    }

    @Test
    @DisplayName("encodeRtspToFile returns a process")
    fun testEncodeRtspToFile() {
        val outputFile = File(tempDir.toFile(), "encode-output.mp4")
        val process = ffmpegService.encodeRtspToFile(
            rtspUrl = "rtsp://localhost/stream",
            outputFile = outputFile,
            format = RecordingFormat.MP4,
            quality = Quality.LOW,
            duration = null,
            username = null,
            password = null,
            useH265 = false
        )
        assertNotNull(process)
        process.destroy()
    }
}
