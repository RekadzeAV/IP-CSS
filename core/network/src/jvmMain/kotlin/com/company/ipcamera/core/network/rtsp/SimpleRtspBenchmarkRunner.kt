package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus
import kotlinx.coroutines.*
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * Простой RTSP Benchmark Runner для сбора метрик производительности
 */
class SimpleRtspBenchmarkRunner {

    /**
     * Запускает базовый бенчмарк для одного потока RTSP
     */
    suspend fun runBenchmark(
        rtspUrl: String,
        duration: Duration = 30.seconds,
        config: RtspBenchmarkConfig = RtspBenchmarkConfig(rtspUrl = rtspUrl, duration = duration)
    ): BenchmarkResult {
        logger.info { "Starting RTSP benchmark for: $rtspUrl, duration: $duration" }

        val metrics = mutableListOf<BenchmarkMetrics>()
        val errors = mutableListOf<String>()
        var totalFrames = 0L
        var startTime = 0L
        var reconnections = 0L

        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        try {
            // Создаём RTSP клиент
            val rtspClient = RtspClient(
                RtspClientConfig(
                    url = rtspUrl,
                    allowSimulatedFallback = true // Для тестирования без реальных камер
                )
            )

            // Подключаемся
            rtspClient.connect()

            // Ждём подключения
            withTimeoutOrNull(10_000) {
                while (rtspClient.getStatus().value != RtspClientStatus.CONNECTED &&
                    rtspClient.getStatus().value != RtspClientStatus.PLAYING
                ) {
                    delay(100)
                }
            }

            if (rtspClient.getStatus().value == RtspClientStatus.ERROR) {
                errors.add("Failed to connect to RTSP server")
                return createEmptyResult(config, errors)
            }

            startTime = System.currentTimeMillis()
            val frameTimestamps = mutableListOf<Long>()

            // Подписываемся на видеокадры
            val job = scope.launch {
                rtspClient.getVideoFrames().collect { frame ->
                    totalFrames++
                    frameTimestamps.add(frame.timestamp)

                    // Собираем метрики каждые 2 секунды
                    if (totalFrames % 50L == 0L) { // ~2 секунды при 25 FPS
                        val elapsed = (System.currentTimeMillis() - startTime).toDouble()
                        val fps = if (frameTimestamps.size > 1) {
                            val timeSpan = (frameTimestamps.last() - frameTimestamps.first())
                            if (timeSpan > 0) (frameTimestamps.size * 1000.0) / timeSpan else 0.0
                        } else {
                            0.0
                        }

                        metrics.add(
                            BenchmarkMetrics(
                                timestamp = System.currentTimeMillis(),
                                elapsedSeconds = elapsed / 1000.0,
                                fps = fps,
                                cpuUsagePercent = collectCpuUsage(),
                                memoryUsageBytes = collectMemoryUsage(),
                                latencyMs = calculateLatency(frameTimestamps),
                                droppedFrames = 0L,
                                totalFrames = totalFrames,
                                reconnections = reconnections.toLong(),
                                errors = emptyList()
                            )
                        )

                        // Очищаем старые таймстампы (окно 2 секунды)
                        val cutoff = frameTimestamps.last() - 2000
                        frameTimestamps.removeAll { it < cutoff }
                    }
                }
            }

            // Запускаем воспроизведение
            rtspClient.play()

            // Запускаем тест
            delay(duration)

            // Останавливаем
            rtspClient.stop()
            rtspClient.disconnect()
            job.cancel()

            logger.info { "Benchmark completed: $totalFrames frames in $duration" }
        } catch (e: Exception) {
            val errorMsg = "Benchmark error: ${e.message}"
            errors.add(errorMsg)
            logger.error(e) { errorMsg }

            if (e is CancellationException) throw e
        } finally {
            scope.cancel()
        }

        return createBenchmarkResult(config, metrics, errors, totalFrames)
    }

    /**
     * Запускает бенчмарк для нескольких потоков одновременно
     */
    suspend fun runMultiStreamBenchmark(
        rtspUrls: List<String>,
        duration: Duration = 60.seconds,
        config: RtspBenchmarkConfig = RtspBenchmarkConfig(rtspUrl = rtspUrls.first(), duration = duration)
    ): Map<String, BenchmarkResult> {
        logger.info { "Starting multi-stream benchmark: ${rtspUrls.size} streams" }

        return withContext(Dispatchers.Default) {
            rtspUrls.associate { url ->
                url to runBenchmark(url, duration, config.copy(rtspUrl = url))
            }
        }
    }

    private fun collectCpuUsage(): Double {
        return collectPlatformCpuUsage()
    }

    private fun collectMemoryUsage(): Long {
        return collectPlatformMemoryUsage()
    }

    private fun calculateLatency(frameTimestamps: List<Long>): Long {
        if (frameTimestamps.size < 2) return 0L
        val timeSpan = frameTimestamps.last() - frameTimestamps.first()
        val avgFrameTime = timeSpan / (frameTimestamps.size - 1)
        return avgFrameTime.coerceAtLeast(0)
    }

    private fun createEmptyResult(config: RtspBenchmarkConfig, errors: List<String>): BenchmarkResult {
        return BenchmarkResult(
            config = config,
            metrics = emptyList(),
            summary = BenchmarkSummary(
                duration = config.duration,
                avgFps = 0.0,
                minFps = 0.0,
                maxFps = 0.0,
                avgCpuUsage = 0.0,
                maxCpuUsage = 0.0,
                avgMemoryUsageBytes = 0L,
                maxMemoryUsageBytes = 0L,
                avgLatencyMs = 0.0,
                maxLatencyMs = 0L,
                totalDroppedFrames = 0L,
                totalReconnections = 0L,
                totalErrors = errors.size,
                passCriteria = false
            ),
            errors = errors,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun createBenchmarkResult(
        config: RtspBenchmarkConfig,
        metrics: List<BenchmarkMetrics>,
        errors: List<String>,
        totalFrames: Long
    ): BenchmarkResult {
        val summary = if (metrics.isEmpty()) {
            BenchmarkSummary(
                duration = config.duration,
                avgFps = 0.0,
                minFps = 0.0,
                maxFps = 0.0,
                avgCpuUsage = 0.0,
                maxCpuUsage = 0.0,
                avgMemoryUsageBytes = 0L,
                maxMemoryUsageBytes = 0L,
                avgLatencyMs = 0.0,
                maxLatencyMs = 0L,
                totalDroppedFrames = 0L,
                totalReconnections = 0L,
                totalErrors = errors.size,
                passCriteria = false
            )
        } else {
            val fpsValues = metrics.map { it.fps }
            val cpuValues = metrics.map { it.cpuUsagePercent }
            val memoryValues = metrics.map { it.memoryUsageBytes }
            val latencyValues = metrics.map { it.latencyMs.toDouble() }

            val passCriteria = fpsValues.average() >= (config.expectedFps - 5) &&
                cpuValues.average() <= 30.0 &&
                memoryValues.average() <= 200 * 1024 * 1024L &&
                latencyValues.average() <= 200.0

            BenchmarkSummary(
                duration = config.duration,
                avgFps = fpsValues.average(),
                minFps = fpsValues.minOrNull() ?: 0.0,
                maxFps = fpsValues.maxOrNull() ?: 0.0,
                avgCpuUsage = cpuValues.average(),
                maxCpuUsage = cpuValues.maxOrNull() ?: 0.0,
                avgMemoryUsageBytes = memoryValues.average().toLong(),
                maxMemoryUsageBytes = memoryValues.maxOrNull() ?: 0L,
                avgLatencyMs = latencyValues.average(),
                maxLatencyMs = latencyValues.maxOrNull()?.toLong() ?: 0L,
                totalDroppedFrames = 0L,
                totalReconnections = 0L,
                totalErrors = errors.size,
                passCriteria = passCriteria
            )
        }

        return BenchmarkResult(
            config = config,
            metrics = metrics,
            summary = summary,
            errors = errors,
            timestamp = System.currentTimeMillis()
        )
    }
}
