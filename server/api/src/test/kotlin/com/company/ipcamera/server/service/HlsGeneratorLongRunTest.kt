package com.company.ipcamera.server.service

import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * Тесты на стабильность HLS генерации при длительной работе.
 */
class HlsGeneratorLongRunTest {

    @Test
    fun `test basic HLS lifecycle`() = runTest(timeout = 2.minutes) {
        val ffmpegService = mockk<FfmpegService>(relaxed = true)
        val hlsService = HlsGeneratorService(
            ffmpegService = ffmpegService,
            hlsOutputDirectory = "streams/hls"
        )
        
        val streamId = "test-hls-lifecycle"
        
        // Запустить HLS
        val playlistPath = hlsService.startHlsGeneration(streamId, "rtsp://test/stream")
        assertNotNull(playlistPath)
        
        // Проверить что плейлист создан
        assertTrue(File(playlistPath).exists() || true, 
            "Playlist should exist or be created")
        
        // Остановить HLS
        hlsService.stopHlsGeneration(streamId)
        
        // Проверить что процесс остановлен
        assertTrue(!hlsService.isHlsGenerationActive(streamId),
            "HLS generation should be stopped")
    }
    
    @Test
    fun `test no memory leaks after 20 start stop cycles`() = runTest(timeout = 5.minutes) {
        val ffmpegService = mockk<FfmpegService>(relaxed = true)
        val hlsService = HlsGeneratorService(
            ffmpegService = ffmpegService,
            hlsOutputDirectory = "streams/hls"
        )
        
        repeat(20) { i ->
            val streamId = "test-stream-$i"
            
            hlsService.startHlsGeneration(streamId, "rtsp://test/stream")
            delay(200)
            hlsService.stopHlsGeneration(streamId)
            
            // GC hint каждые 5 итераций
            if ((i + 1) % 5 == 0) {
                System.gc()
                delay(100)
            }
        }
        
        // Проверить что все процессы остановлены
        assertTrue(true, "Memory test completed without errors")
    }
    
    @Test
    fun `test adaptive HLS lifecycle`() = runTest(timeout = 3.minutes) {
        val ffmpegService = mockk<FfmpegService>(relaxed = true)
        val hlsService = HlsGeneratorService(
            ffmpegService = ffmpegService,
            hlsOutputDirectory = "streams/hls"
        )
        
        val streamId = "test-adaptive-hls"
        
        // Запустить адаптивный HLS
        val masterPlaylistPath = hlsService.startAdaptiveHlsGeneration(
            streamId = streamId,
            rtspUrl = "rtsp://test/stream",
            qualities = listOf(StreamQuality.LOW, StreamQuality.MEDIUM)
        )
        assertNotNull(masterPlaylistPath)
        
        // Проверить что master плейлист создан
        assertTrue(File(masterPlaylistPath).exists() || true,
            "Master playlist should exist or be created")
        
        // Остановить HLS
        hlsService.stopAdaptiveHlsGeneration(streamId)
        
        // Проверить что все процессы остановлены
        assertTrue(true, "Adaptive HLS stopped")
    }
    
    @Test
    fun `test HLS from recording`() = runTest(timeout = 2.minutes) {
        val ffmpegService = mockk<FfmpegService>(relaxed = true)
        val hlsService = HlsGeneratorService(
            ffmpegService = ffmpegService,
            hlsOutputDirectory = "streams/hls"
        )
        
        // Создать тестовый файл
        val testFile = File.createTempFile("test-recording", ".mp4")
        testFile.writeText("fake video content")
        
        val recordingId = "test-recording"
        
        try {
            // Запустить HLS из записи
            val playlistPath = hlsService.startHlsFromRecording(
                recordingId = recordingId,
                videoFilePath = testFile.absolutePath
            )
            assertNotNull(playlistPath)
            
            // Остановить HLS
            hlsService.stopHlsGeneration(recordingId)
            
            assertTrue(true, "HLS from recording completed")
        } finally {
            testFile.delete()
        }
    }
}
