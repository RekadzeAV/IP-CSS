package com.company.ipcamera.shared.test

import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

/**
 * Результат валидации RTSP камеры
 */
data class RtspValidationResult(
    val cameraId: String,
    val cameraName: String,
    val rtspUrl: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationSeconds: Long,
    // Подключение
    val connectionSuccess: Boolean,
    val totalConnections: Int,
    val successfulConnections: Int,
    val failedConnections: Int,
    val reconnections: Int,
    val avgConnectionTimeMs: Long,
    // Видео поток
    val totalFramesReceived: Long,
    val avgFps: Double,
    val minFps: Double,
    val maxFps: Double,
    val droppedFrames: Long,
    // Аудио поток
    val audioFramesReceived: Long,
    val audioDecodingErrors: Int,
    // Ошибки
    val connectionErrors: Int,
    val decodingErrors: Int,
    val timeoutErrors: Int,
    val otherErrors: Int,
    val errorMessages: List<String>,
    // Память
    val initialMemoryMb: Double,
    val peakMemoryMb: Double,
    val finalMemoryMb: Double,
    val memoryLeakSuspected: Boolean,
    // Статус
    val testPassed: Boolean,
    val failureReason: String?,
)

/**
 * Валидация RTSP клиента с реальными камерами
 */
class RtspValidationTest(
    private val cameraId: String,
    private val cameraName: String,
    private val rtspUrl: String,
    private val durationMinutes: Int = 10,
    private val outputDir: String = "docs/reports/rtsp-validation",
    private val memoryCheckIntervalMs: Long = 60_000,
    private val maxReconnectAttempts: Int = 5,
    private val reconnectDelayMs: Long = 5000,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var connectionCount = 0
    private var successfulConnections = 0
    private var failedConnections = 0
    private var reconnections = 0
    private val connectionTimes = mutableListOf<Long>()

    private var totalFramesReceived = 0L
    private var audioFramesReceived = 0L
    private var droppedFrames = 0L
    private val fpsSamples = mutableListOf<Double>()

    private var connectionErrors = 0
    private var decodingErrors = 0
    private var timeoutErrors = 0
    private var otherErrors = 0
    private val errorMessages = mutableListOf<String>()

    private var audioDecodingErrors = 0

    private var initialMemoryMb = 0.0
    private var peakMemoryMb = 0.0
    private var finalMemoryMb = 0.0

    private val startTime = LocalDateTime.now()
    private var endTime: LocalDateTime? = null

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Запустить валидацию
     */
    suspend fun run(): RtspValidationResult {
        logger.info { "Starting RTSP validation for $cameraName" }
        logger.info { "  ID: $cameraId" }
        logger.info { "  URL: $rtspUrl" }
        logger.info { "  Duration: $durationMinutes minutes" }

        // Инициализация
        initialMemoryMb = getMemoryUsageMb()
        peakMemoryMb = initialMemoryMb

        // Создаем директорию для отчетов
        File(outputDir).mkdirs()

        val durationMillis = durationMinutes * 60 * 1000L
        val testStartTime = System.currentTimeMillis()
        var lastFrameTime = System.currentTimeMillis()

        try {
            // Главный цикл теста
            while (System.currentTimeMillis() - testStartTime < durationMillis) {
                try {
                    val connectStartTime = System.currentTimeMillis()
                    connectAndStream()
                    val connectEndTime = System.currentTimeMillis()
                    connectionTimes.add(connectEndTime - connectStartTime)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error(e) { "Error in test loop for $cameraName" }
                    handleError(e)
                    delay(reconnectDelayMs)
                }

                // Проверка FPS
                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = (currentTime - lastFrameTime) / 1000.0
                if (elapsedSeconds > 0 && totalFramesReceived > 0) {
                    val currentFps = totalFramesReceived / elapsedSeconds
                    fpsSamples.add(currentFps)
                }
            }
        } catch (e: CancellationException) {
            logger.info { "Test cancelled for $cameraName" }
        } finally {
            endTime = LocalDateTime.now()
            finalMemoryMb = getMemoryUsageMb()
            peakMemoryMb = peakMemoryMb.coerceAtLeast(finalMemoryMb)
        }

        // Вычисляем результат
        val durationSeconds = (System.currentTimeMillis() - testStartTime) / 1000
        val avgFps = if (fpsSamples.isNotEmpty()) fpsSamples.average() else 0.0
        val minFps = fpsSamples.minOrNull() ?: 0.0
        val maxFps = fpsSamples.maxOrNull() ?: 0.0
        val avgConnectionTime = if (connectionTimes.isNotEmpty()) connectionTimes.average().toLong() else 0L

        // Проверка на утечку памяти
        val memoryLeakSuspected =
            if (initialMemoryMb > 0) {
                val growth = (finalMemoryMb - initialMemoryMb) / initialMemoryMb
                growth > 0.2
            } else {
                false
            }

        // Критерии прохождения теста
        val connectionSuccess = failedConnections == 0 || (successfulConnections > 0 && failedConnections.toDouble() / successfulConnections < 0.1)
        val testPassed = (
            connectionSuccess && reconnections <= 3 && !memoryLeakSuspected &&
                totalFramesReceived > 0 &&
                (decodingErrors + timeoutErrors) <= 5
        )

        val result =
            RtspValidationResult(
                cameraId = cameraId,
                cameraName = cameraName,
                rtspUrl = rtspUrl,
                startTime = startTime,
                endTime = endTime ?: LocalDateTime.now(),
                durationSeconds = durationSeconds,
                connectionSuccess = connectionSuccess,
                totalConnections = connectionCount,
                successfulConnections = successfulConnections,
                failedConnections = failedConnections,
                reconnections = reconnections,
                avgConnectionTimeMs = avgConnectionTime,
                totalFramesReceived = totalFramesReceived,
                avgFps = avgFps,
                minFps = minFps,
                maxFps = maxFps,
                droppedFrames = droppedFrames,
                audioFramesReceived = audioFramesReceived,
                audioDecodingErrors = audioDecodingErrors,
                connectionErrors = connectionErrors,
                decodingErrors = decodingErrors,
                timeoutErrors = timeoutErrors,
                otherErrors = otherErrors,
                errorMessages = errorMessages.toList(),
                initialMemoryMb = initialMemoryMb,
                peakMemoryMb = peakMemoryMb,
                finalMemoryMb = finalMemoryMb,
                memoryLeakSuspected = memoryLeakSuspected,
                testPassed = testPassed,
                failureReason = if (!testPassed) generateFailureReason() else null,
            )

        // Генерируем отчёт
        generateReport(result)

        logger.info { "Validation completed for $cameraName: ${if (testPassed) "PASSED" else "FAILED"}" }

        return result
    }

    /**
     * Подключение и получение потока
     */
    private suspend fun connectAndStream() {
        connectionCount++
        logger.info { "Connection #$connectionCount to $cameraName" }

        val config =
            RtspClientConfig(
                url = rtspUrl,
                reconnectEnabled = true,
                reconnectMaxRetries = maxReconnectAttempts,
                reconnectInitialDelayMs = 500,
                reconnectMaxDelayMs = reconnectDelayMs.toInt(),
                connectionTimeoutMs = 10000,
                readTimeoutMs = 5000,
            )

        val rtspClient = RtspClient(config)

        try {
            rtspClient.connect()

            // Получаем видео и аудио потоки
            val videoJob =
                scope.launch {
                    rtspClient.getVideoFrames().collect { frame ->
                        totalFramesReceived++
                    }
                }

            val audioJob =
                scope.launch {
                    rtspClient.getAudioFrames().collect { frame ->
                        audioFramesReceived++
                    }
                }

            successfulConnections++

            // Ожидаем завершения (должно быть отменено извне)
            while (currentCoroutineContext().isActive) {
                delay(1000)
            }

            videoJob.cancel()
            audioJob.cancel()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failedConnections++
            handleError(e)
        }
    }

    /**
     * Обработка ошибки
     */
    private fun handleError(error: Throwable) {
        val message = error.message ?: "Unknown error"
        val timestamp = LocalDateTime.now().format(formatter)
        val logEntry = "[$timestamp] ERROR: $message"
        errorMessages.add(logEntry)

        when {
            message.contains("timeout", ignoreCase = true) -> timeoutErrors++
            message.contains("decode", ignoreCase = true) || message.contains("codec", ignoreCase = true) -> decodingErrors++
            message.contains("audio", ignoreCase = true) -> audioDecodingErrors++
            else -> connectionErrors++
        }

        logger.error(error) { "Error in RTSP connection: $message" }
    }

    /**
     * Получение использования памяти
     */
    private fun getMemoryUsageMb(): Double {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0)
    }

    /**
     * Генерация причины неудачи
     */
    private fun generateFailureReason(): String {
        val reasons = mutableListOf<String>()

        if (failedConnections > 0) {
            reasons.add("Failed connections: $failedConnections")
        }
        if (reconnections > 3) {
            reasons.add("Too many reconnections: $reconnections")
        }
        if (totalFramesReceived == 0L) {
            reasons.add("No frames received")
        }
        if (memoryLeakSuspected) {
            reasons.add("Memory leak suspected")
        }
        if (decodingErrors > 5) {
            reasons.add("Too many decoding errors: $decodingErrors")
        }

        return reasons.joinToString("; ")
    }

    /**
     * Генерация отчёта
     */
    private fun generateReport(result: RtspValidationResult) {
        val reportFile = File(outputDir, "validation-$cameraId-${result.startTime.format(formatter)}.md")

        val report =
            buildString {
                appendLine("# RTSP Validation Report: $cameraName")
                appendLine()
                appendLine("## Summary")
                appendLine()
                appendLine("| Metric | Value |")
                appendLine("|--------|-------|")
                appendLine("| **Camera ID** | $cameraId |")
                appendLine("| **Status** | ${if (result.testPassed) "✅ PASSED" else "❌ FAILED"} |")
                appendLine("| **Duration** | ${result.durationSeconds}s |")
                appendLine("| **RTSP URL** | ${result.rtspUrl} |")
                appendLine()

                appendLine("## Connection Statistics")
                appendLine()
                appendLine("| Metric | Value |")
                appendLine("|--------|-------|")
                appendLine("| Total Connections | ${result.totalConnections} |")
                appendLine("| Successful | ${result.successfulConnections} |")
                appendLine("| Failed | ${result.failedConnections} |")
                appendLine("| Reconnections | ${result.reconnections} |")
                appendLine("| Avg Connection Time | ${result.avgConnectionTimeMs}ms |")
                appendLine()

                appendLine("## Video Statistics")
                appendLine()
                appendLine("| Metric | Value |")
                appendLine("|--------|-------|")
                appendLine("| Total Frames | ${result.totalFramesReceived} |")
                appendLine("| Average FPS | ${String.format("%.2f", result.avgFps)} |")
                appendLine("| Min FPS | ${String.format("%.2f", result.minFps)} |")
                appendLine("| Max FPS | ${String.format("%.2f", result.maxFps)} |")
                appendLine("| Dropped Frames | ${result.droppedFrames} |")
                appendLine()

                appendLine("## Audio Statistics")
                appendLine()
                appendLine("| Metric | Value |")
                appendLine("|--------|-------|")
                appendLine("| Audio Frames | ${result.audioFramesReceived} |")
                appendLine("| Decoding Errors | ${result.audioDecodingErrors} |")
                appendLine()

                appendLine("## Error Statistics")
                appendLine()
                appendLine("| Metric | Value |")
                appendLine("|--------|-------|")
                appendLine("| Connection Errors | ${result.connectionErrors} |")
                appendLine("| Decoding Errors | ${result.decodingErrors} |")
                appendLine("| Timeout Errors | ${result.timeoutErrors} |")
                appendLine("| Other Errors | ${result.otherErrors} |")
                appendLine()

                appendLine("## Memory Statistics")
                appendLine()
                appendLine("| Metric | Value |")
                appendLine("|--------|-------|")
                appendLine("| Initial | ${String.format("%.2f", result.initialMemoryMb)} MB |")
                appendLine("| Peak | ${String.format("%.2f", result.peakMemoryMb)} MB |")
                appendLine("| Final | ${String.format("%.2f", result.finalMemoryMb)} MB |")
                appendLine("| Memory Leak | ${if (result.memoryLeakSuspected) "❌ YES" else "✅ NO"} |")
                appendLine()

                if (result.failureReason != null) {
                    appendLine("## Failure Reason")
                    appendLine()
                    appendLine(result.failureReason)
                    appendLine()
                }

                if (result.errorMessages.isNotEmpty()) {
                    appendLine("## Error Log (last 10 entries)")
                    appendLine()
                    appendLine("```")
                    result.errorMessages.takeLast(10).forEach { appendLine(it) }
                    appendLine("```")
                    appendLine()
                }

                appendLine("## Test Environment")
                appendLine()
                appendLine("- **OS**: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
                appendLine("- **JVM**: ${System.getProperty("java.version")}")
                appendLine(
                    "- **Test Time**: ${result.startTime.format(formatter)} - ${result.endTime.format(formatter)}",
                )
            }

        reportFile.writeText(report)
        logger.info { "Report generated: ${reportFile.absolutePath}" }
    }
}

/**
 * Точка входа для запуска из консоли
 */
fun main() =
    runBlocking {
        val cameraId = "emulator_aac"
        val cameraName = "Emulator_AAC"
        val rtspUrl = "rtsp://127.0.0.1:8554/stream/"
        val durationMinutes = 10

        val test =
            RtspValidationTest(
                cameraId = cameraId,
                cameraName = cameraName,
                rtspUrl = rtspUrl,
                durationMinutes = durationMinutes,
            )

        val result = test.run()

        println("\n=== Validation Result ===")
        println("Camera: $cameraName")
        println("Status: ${if (result.testPassed) "PASSED" else "FAILED"}")
        println("Duration: ${result.durationSeconds}s")
        println("Frames: ${result.totalFramesReceived}")
        println("Avg FPS: ${String.format("%.2f", result.avgFps)}")
        println("Reconnects: ${result.reconnections}")
        println("Memory Leak: ${if (result.memoryLeakSuspected) "YES" else "NO"}")

        if (result.failureReason != null) {
            println("\nFailure Reason:")
            println(result.failureReason)
        }
    }
