package com.company.ipcamera.core.network.rtsp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Конфигурация для RTSP бенчмарка
 */
@Serializable
data class RtspBenchmarkConfig(
    val rtspUrl: String,
    val duration: Duration = 10.minutes,
    val hardwareDecoding: Boolean = true,
    val bufferSizeMs: Long = 1000,
    val resolution: VideoResolution = VideoResolution.FHD,
    val expectedFps: Int = 30,
    val metricsCollectionInterval: Duration = 1.seconds,
    val enableRecording: Boolean = false,
    val recordingPath: String? = null
)

/**
 * Разрешение видео
 */
@Serializable
enum class VideoResolution(val width: Int, val height: Int) {
    HD(1280, 720),
    FHD(1920, 1080),
    QHD(2560, 1440),
    UHD(3840, 2160)
}

/**
 * Результаты бенчмарка
 */
@Serializable
data class BenchmarkResult(
    val config: RtspBenchmarkConfig,
    val metrics: List<BenchmarkMetrics>,
    val summary: BenchmarkSummary,
    val errors: List<String>,
    val timestamp: Long
)

/**
 * Метрики в момент времени
 */
@Serializable
data class BenchmarkMetrics(
    val timestamp: Long,
    val elapsedSeconds: Double,
    val fps: Double,
    val cpuUsagePercent: Double,
    val memoryUsageBytes: Long,
    val latencyMs: Long,
    val droppedFrames: Long,
    val totalFrames: Long,
    val reconnections: Long,
    val errors: List<String>
)

/**
 * Сводка результатов бенчмарка
 */
@Serializable
data class BenchmarkSummary(
    val duration: Duration,
    val avgFps: Double,
    val minFps: Double,
    val maxFps: Double,
    val avgCpuUsage: Double,
    val maxCpuUsage: Double,
    val avgMemoryUsageBytes: Long,
    val maxMemoryUsageBytes: Long,
    val avgLatencyMs: Double,
    val maxLatencyMs: Long,
    val totalDroppedFrames: Long,
    val totalReconnections: Long,
    val totalErrors: Int,
    val passCriteria: Boolean
)

/**
 * Критерии прохождения теста
 */
@Serializable
data class PassCriteria(
    val minFps: Double = 25.0,
    val maxCpuPercent: Double = 30.0,
    val maxMemoryBytes: Long = 200 * 1024 * 1024, // 200MB
    val maxLatencyMs: Long = 200,
    val maxDroppedFrames: Long = 100,
    val maxReconnections: Long = 5
)

/**
 * Генератор HTML отчета
 */
class BenchmarkReportGenerator {

    fun generateHtmlReport(result: BenchmarkResult): String {
        val summary = result.summary
        return buildString {
            appendLine("<html><head><title>RTSP Benchmark Report</title>")
            appendLine("<style>")
            appendLine("body { font-family: Arial, sans-serif; margin: 20px; }")
            appendLine("h1 { color: #333; }")
            appendLine(".summary { background: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0; }")
            appendLine(".metric { margin: 10px 0; }")
            appendLine(".pass { color: green; font-weight: bold; }")
            appendLine(".fail { color: red; font-weight: bold; }")
            appendLine("table { border-collapse: collapse; width: 100%; }")
            appendLine("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
            appendLine("th { background-color: #4CAF50; color: white; }")
            appendLine("</style>")
            appendLine("</head><body>")

            appendLine("<h1>RTSP Benchmark Report</h1>")
            appendLine("<p><strong>Timestamp:</strong> ${result.timestamp}</p>")
            appendLine("<p><strong>RTSP URL:</strong> ${result.config.rtspUrl}</p>")
            appendLine("<p><strong>Duration:</strong> ${result.summary.duration}</p>")
            appendLine("<p><strong>Hardware Decoding:</strong> ${result.config.hardwareDecoding}</p>")

            appendLine("<div class='summary'>")
            appendLine("<h2>Summary</h2>")
            appendLine(
                "<div class='metric'><strong>Avg FPS:</strong> <span class='${if (summary.avgFps >= result.config.expectedFps) "pass" else "fail"}'>${summary.avgFps.toFixed(
                    2
                )}</span></div>"
            )
            appendLine("<div class='metric'><strong>Avg CPU:</strong> ${summary.avgCpuUsage.toFixed(2)}%</div>")
            appendLine("<div class='metric'><strong>Max CPU:</strong> ${summary.maxCpuUsage.toFixed(2)}%</div>")
            appendLine(
                "<div class='metric'><strong>Avg Memory:</strong> ${(summary.avgMemoryUsageBytes / 1024.0 / 1024.0).toFixed(
                    2
                )} MB</div>"
            )
            appendLine("<div class='metric'><strong>Avg Latency:</strong> ${summary.avgLatencyMs.toFixed(2)} ms</div>")
            appendLine("<div class='metric'><strong>Dropped Frames:</strong> ${summary.totalDroppedFrames}</div>")
            appendLine("<div class='metric'><strong>Reconnections:</strong> ${summary.totalReconnections}</div>")
            appendLine("<div class='metric'><strong>Errors:</strong> ${summary.totalErrors}</div>")
            appendLine(
                "<div class='metric'><strong>Pass Criteria:</strong> <span class='${if (summary.passCriteria) "pass" else "fail"}'>${if (summary.passCriteria) "PASS" else "FAIL"}</span></div>"
            )
            appendLine("</div>")

            if (result.errors.isNotEmpty()) {
                appendLine("<h2>Errors</h2>")
                appendLine("<ul>")
                result.errors.forEach { error ->
                    appendLine("<li>$error</li>")
                }
                appendLine("</ul>")
            }

            appendLine("<h2>Metrics Timeline</h2>")
            appendLine("<table>")
            appendLine("<tr><th>Time (s)</th><th>FPS</th><th>CPU %</th><th>Memory (MB)</th><th>Latency (ms)</th></tr>")
            result.metrics.take(100).forEach { metric ->
                appendLine("<tr>")
                appendLine("<td>${metric.elapsedSeconds.toFixed(2)}</td>")
                appendLine("<td>${metric.fps.toFixed(2)}</td>")
                appendLine("<td>${metric.cpuUsagePercent.toFixed(2)}</td>")
                appendLine("<td>${(metric.memoryUsageBytes / 1024.0 / 1024.0).toFixed(2)}</td>")
                appendLine("<td>${metric.latencyMs}</td>")
                appendLine("</tr>")
            }
            appendLine("</table>")

            appendLine("</body></html>")
        }
    }

    private fun Double.toFixed(digits: Int): String {
        // Локаль-независимое форматирование: всегда точка как десятичный разделитель.
        return java.lang.String.format(java.util.Locale.ROOT, "%.${digits}f", this)
    }
}

// Note: CpuUsageCalculator and MemoryUsageCalculator have been removed from common code.
// Platform-specific implementations are in BenchmarkPlatformStats.* files.
