package com.company.ipcamera.core.network.rtsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Unit тесты для RTSP BenchmarkConfig
 */
class RtspBenchmarkConfigTest {

    @Test
    fun `test default config values`() {
        val config = RtspBenchmarkConfig(
            rtspUrl = "rtsp://localhost:554/test"
        )

        assertEquals("rtsp://localhost:554/test", config.rtspUrl)
        assertEquals(10.minutes, config.duration)
        assertTrue(config.hardwareDecoding)
        assertEquals(1000, config.bufferSizeMs)
        assertEquals(30, config.expectedFps)
        assertEquals(1.seconds, config.metricsCollectionInterval)
    }

    @Test
    fun `test custom config values`() {
        val config = RtspBenchmarkConfig(
            rtspUrl = "rtsp://camera.local/stream",
            duration = 5.minutes,
            hardwareDecoding = false,
            bufferSizeMs = 2000,
            expectedFps = 25
        )

        assertEquals("rtsp://camera.local/stream", config.rtspUrl)
        assertEquals(5.minutes, config.duration)
        assertFalse(config.hardwareDecoding)
        assertEquals(2000, config.bufferSizeMs)
        assertEquals(25, config.expectedFps)
    }

    @Test
    fun `test video resolution enum`() {
        assertEquals(1280, VideoResolution.HD.width)
        assertEquals(720, VideoResolution.HD.height)

        assertEquals(1920, VideoResolution.FHD.width)
        assertEquals(1080, VideoResolution.FHD.height)

        assertEquals(2560, VideoResolution.QHD.width)
        assertEquals(1440, VideoResolution.QHD.height)

        assertEquals(3840, VideoResolution.UHD.width)
        assertEquals(2160, VideoResolution.UHD.height)
    }
}

/**
 * Unit тесты для BenchmarkReportGenerator
 */
class BenchmarkReportGeneratorTest {

    @Test
    fun `test HTML report generation`() {
        val generator = BenchmarkReportGenerator()

        val config = RtspBenchmarkConfig(
            rtspUrl = "rtsp://localhost:554/test",
            duration = 1.minutes,
            hardwareDecoding = true,
            expectedFps = 30
        )

        val metrics = listOf(
            BenchmarkMetrics(
                timestamp = System.currentTimeMillis(),
                elapsedSeconds = 1.0,
                fps = 29.5,
                cpuUsagePercent = 10.0,
                memoryUsageBytes = 100 * 1024 * 1024,
                latencyMs = 150,
                droppedFrames = 0,
                totalFrames = 30,
                reconnections = 0,
                errors = emptyList()
            )
        )

        val summary = BenchmarkSummary(
            duration = 1.minutes,
            avgFps = 29.5,
            minFps = 29.0,
            maxFps = 30.0,
            avgCpuUsage = 10.0,
            maxCpuUsage = 12.0,
            avgMemoryUsageBytes = 100 * 1024 * 1024,
            maxMemoryUsageBytes = 110 * 1024 * 1024,
            avgLatencyMs = 150.0,
            maxLatencyMs = 160,
            totalDroppedFrames = 0,
            totalReconnections = 0,
            totalErrors = 0,
            passCriteria = true
        )

        val result = BenchmarkResult(
            config = config,
            metrics = metrics,
            summary = summary,
            errors = emptyList(),
            timestamp = System.currentTimeMillis()
        )

        val html = generator.generateHtmlReport(result)

        assertTrue(html.contains("<html>"))
        assertTrue(html.contains("<title>RTSP Benchmark Report</title>"))
        assertTrue(html.contains("rtsp://localhost:554/test"))
        // avgFps форматируется как "29.50" (%.2f); принимаем оба варианта.
        assertTrue(html.contains("29.50") || html.contains("29.5"))
        assertTrue(html.contains("PASS"))
        assertTrue(html.contains("</html>"))
    }

    @Test
    fun `test HTML report with failures`() {
        val generator = BenchmarkReportGenerator()

        val config = RtspBenchmarkConfig(
            rtspUrl = "rtsp://localhost:554/test",
            expectedFps = 30
        )

        val summary = BenchmarkSummary(
            duration = 1.minutes,
            avgFps = 15.0, // Below expected
            minFps = 10.0,
            maxFps = 20.0,
            avgCpuUsage = 50.0,
            maxCpuUsage = 60.0,
            avgMemoryUsageBytes = 300 * 1024 * 1024,
            maxMemoryUsageBytes = 350 * 1024 * 1024,
            avgLatencyMs = 300.0,
            maxLatencyMs = 400,
            totalDroppedFrames = 500,
            totalReconnections = 10,
            totalErrors = 2,
            passCriteria = false
        )

        val result = BenchmarkResult(
            config = config,
            metrics = emptyList(),
            summary = summary,
            errors = listOf("Connection timeout", "Frame drop detected"),
            timestamp = System.currentTimeMillis()
        )

        val html = generator.generateHtmlReport(result)

        assertTrue(html.contains("FAIL"))
        assertTrue(html.contains("Connection timeout"))
        assertTrue(html.contains("Frame drop detected"))
    }
}
