package com.company.ipcamera.shared.test

import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.shared.platform.NasPlatformDetectorImpl
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

/**
 * Результат стабильности RTSP подключения
 */
data class RtspStabilityResult(
    val testId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationSeconds: Long,
    val rtspUrl: String,
    // Метрики подключения
    val totalConnections: Int,
    val successfulConnections: Int,
    val failedConnections: Int,
    val reconnections: Int,
    // Метрики потока
    val totalFramesReceived: Long,
    val averageFps: Double,
    val minFps: Double,
    val maxFps: Double,
    // Метрики ошибок
    val connectionErrors: Int,
    val decodingErrors: Int,
    val timeoutErrors: Int,
    val otherErrors: Int,
    // Метрики памяти (если доступны)
    val initialMemoryMb: Double,
    val peakMemoryMb: Double,
    val finalMemoryMb: Double,
    val memoryLeakSuspected: Boolean,
    // Статус теста
    val testPassed: Boolean,
    val failureReason: String? = null,
    // Детальные логи
    val errorLog: List<String> = emptyList(),
)

/**
 * Долгосрочный тест стабильности RTSP клиента
 * * Запускает длительное подключение к RTSP камере и мониторит:
 * - Стабильность подключения (reconnects)
 * - Производительность (FPS)
 * - Использование памяти
 * - Ошибки всех типов
 */
class RtspLongRunStabilityTest(
    private val rtspUrl: String,
    private val durationMinutes: Int = 60,
    private val outputDir: String = "docs/reports/rtsp-stability-tests",
    private val memoryCheckIntervalMs: Long = 30_000, // Проверка памяти каждые 30 секунд
    private val fpsCheckIntervalMs: Long = 1000, // FPS измерение каждую секунду
    private val maxReconnectAttempts: Int = 5,
    private val reconnectDelayMs: Long = 5000,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var connectionCount = 0
    private var successfulConnections = 0
    private var failedConnections = 0
    private var reconnections = 0

    private var totalFramesReceived = 0L
    private val fpsSamples = mutableListOf<Double>()

    private var connectionErrors = 0
    private var decodingErrors = 0
    private var timeoutErrors = 0
    private var otherErrors = 0

    private var initialMemoryMb = 0.0
    private var peakMemoryMb = 0.0
    private var finalMemoryMb = 0.0

    private val errorLog = mutableListOf<String>()

    private val startTime = LocalDateTime.now()
    private var endTime: LocalDateTime? = null

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Запустить тест стабильности
     */
    suspend fun run(): RtspStabilityResult {
        logger.info { "Starting RTSP long-run stability test" }
        logger.info { "  URL: $rtspUrl" }
        logger.info { "  Duration: $durationMinutes minutes" }
        logger.info { "  Output: $outputDir" }

        val testId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))

        // Инициализация
        initialMemoryMb = getMemoryUsageMb()
        peakMemoryMb = initialMemoryMb

        // Создаем директорию для отчетов
        File(outputDir).mkdirs()

        val durationMillis = durationMinutes * 60 * 1000L
        val testStartTime = System.currentTimeMillis()

        try {
            // Главный цикл теста
            while (System.currentTimeMillis() - testStartTime < durationMillis) {
                try {
                    connectAndStream()
                } catch (e: Exception) {
                    logger.error(e) { "Unexpected error in test loop" }
                    logError("Unexpected error: ${e.message}", e)
                    otherErrors++
                    delay(reconnectDelayMs)
                }
            }
        } catch (e: CancellationException) {
            logger.info { "Test cancelled" }
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

        // Проверка на утечку памяти (>20% рост считается подозрительным)
        val memoryLeakSuspected =
            if (initialMemoryMb > 0) {
                val growth = (finalMemoryMb - initialMemoryMb) / initialMemoryMb
                growth > 0.2
            } else {
                false
            }

        // Критерии прохождения теста
        val testPassed = (
            failedConnections <= 3 && reconnections <= 5 && !memoryLeakSuspected &&
                totalFramesReceived > 0
        )

        val result =
            RtspStabilityResult(
                testId = testId,
                startTime = startTime,
                endTime = endTime ?: LocalDateTime.now(),
                durationSeconds = durationSeconds,
                rtspUrl = rtspUrl,
                totalConnections = connectionCount,
                successfulConnections = successfulConnections,
                failedConnections = failedConnections,
                reconnections = reconnections,
                totalFramesReceived = totalFramesReceived,
                averageFps = avgFps,
                minFps = minFps,
                maxFps = maxFps,
                connectionErrors = connectionErrors,
                decodingErrors = decodingErrors,
                timeoutErrors = timeoutErrors,
                otherErrors = otherErrors,
                initialMemoryMb = initialMemoryMb,
                peakMemoryMb = peakMemoryMb,
                finalMemoryMb = finalMemoryMb,
                memoryLeakSuspected = memoryLeakSuspected,
                testPassed = testPassed,
                failureReason = if (!testPassed) generateFailureReason() else null,
                errorLog = errorLog.toList(),
            )

        // Генерируем отчет
        generateReport(result)

        logger.info { "Test completed: ${if (testPassed) "PASSED" else "FAILED"}" }

        return result
    }

    /**
     * Подключиться к RTSP и получать поток
     */
    private suspend fun connectAndStream() {
        connectionCount++
        logger.info { "Connection attempt #$connectionCount" }

        var rtspClient: RtspClient? = null

        try {
            // Создаем RTSP клиент
            rtspClient =
                RtspClient(
                    url = rtspUrl,
                    platformDetector = NasPlatformDetectorImpl,
                    onFrame = { frame ->
                        handleFrame(frame)
                    },
                    onError = { error ->
                        handleError(error)
                    },
                )

            connectionCount++
            successfulConnections++

            // Подключаемся
            rtspClient.start()

            // Мониторим поток до завершения теста
            var lastFrameTime = System.currentTimeMillis()
            val streamStartTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - streamStartTime < (durationMinutes * 60 * 1000L)) {
                // Мониторинг FPS
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastFrameTime >= fpsCheckIntervalMs) {
                    val elapsedSeconds = (currentTime - lastFrameTime) / 1000.0
                    val currentFps = totalFramesReceived / elapsedSeconds
                    fpsSamples.add(currentFps)
                    lastFrameTime = currentTime

                    if (fpsSamples.size % 10 == 0) {
                        logger.info { "FPS: $currentFps, Total frames: $totalFramesReceived, Reconnects: $reconnections" }
                    }
                }

                // Мониторинг памяти
                if (currentTime - lastFrameTime >= memoryCheckIntervalMs) {
                    val currentMemory = getMemoryUsageMb()
                    if (currentMemory > peakMemoryMb) {
                        peakMemoryMb = currentMemory
                    }
                    logger.debug { "Memory: ${currentMemory.toFixed(2)} MB, Peak: ${peakMemoryMb.toFixed(2)} MB" }
                }

                delay(100)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in RTSP connection/stream" }
            failedConnections++
            handleError(RealtimeErrorType.CONNECTION, e)

            // Попытки reconnect
            var reconnectAttempt = 0
            while (reconnectAttempt < maxReconnectAttempts && System.currentTimeMillis() - streamStartTime < (durationMinutes * 60 * 1000L)) {
                reconnectAttempt++
                reconnections++
                logger.warn { "Reconnect attempt #$reconnectAttempt" }

                delay(reconnectDelayMs)

                try {
                    rtspClient?.disconnect()
                    rtspClient?.close()
                } catch (e2: Exception) {
                    logger.warn(e2) { "Error closing client during reconnect" }
                }

                // Рекурсивный вызов для нового подключения
                connectAndStream()
                return
            }

            logger.error { "Max reconnect attempts reached" }
        } finally {
            try {
                rtspClient?.disconnect()
                rtspClient?.close()
            } catch (e: Exception) {
                logger.warn(e) { "Error closing RTSP client" }
            }
        }
    }

    /**
     * Обработка полученного кадра
     */
    private fun handleFrame(frame: RtspFrame) {
        totalFramesReceived++
    }

    /**
     * Обработка ошибки
     */
    private fun handleError(
        errorType: RealtimeErrorType,
        error: Throwable,
    ) {
        val message = error.message ?: "Unknown error"
        logError("$errorType: $message", error)

        when (errorType) {
            RealtimeErrorType.CONNECTION -> connectionErrors++
            RealtimeErrorType.DECODING -> decodingErrors++
            RealtimeErrorType.TIMEOUT -> timeoutErrors++
            RealtimeErrorType.OTHER -> otherErrors++
        }
    }

    /**
     * Логирование ошибки
     */
    private fun logError(
        message: String,
        error: Throwable? = null,
    ) {
        val timestamp = LocalDateTime.now().format(formatter)
        val logEntry = "[$timestamp] ERROR: $message${error?.message?.let { " - $it" } ?: ""}"
        errorLog.add(logEntry)
        logger.error { logEntry }
    }

    /**
     * Получение использования памяти в MB
     */
    private fun getMemoryUsageMb(): Double {
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0)
        return usedMb
    }

    /**
     * Генерация причины неудачи теста
     */
    private fun generateFailureReason(): String {
        val reasons = mutableListOf<String>()

        if (failedConnections > 3) {
            reasons.add("Too many failed connections: $failedConnections")
        }
        if (reconnections > 5) {
            reasons.add("Too many reconnections: $reconnections")
        }
        if (totalFramesReceived == 0L) {
            reasons.add("No frames received")
        }

        return reasons.joinToString("; ")
    }

    /**
     * Генерация отчета
     */
    private fun generateReport(result: RtspStabilityResult) {
        val reportFile = File(outputDir, "stability-report-${result.testId}.md")

        val report =
            buildString {
                appendLine("# RTSP Long-Run Stability Test Report")
                appendLine()
                appendLine("## Summary")
                appendLine()
                appendLine("| Metric | Value |")
                appendLine("|--------|-------|")
                appendLine("| **Test ID** | ${result.testId} |")
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
                appendLine()

                appendLine("## Frame Statistics")
                appendLine()
                appendLine("| Metric | Value |")
                appendLine("|--------|-------|")
                appendLine("| Total Frames | ${result.totalFramesReceived} |")
                appendLine("| Average FPS | ${result.averageFps.toFixed(2)} |")
                appendLine("| Min FPS | ${result.minFps.toFixed(2)} |")
                appendLine("| Max FPS | ${result.maxFps.toFixed(2)} |")
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
                appendLine("| Initial | ${result.initialMemoryMb.toFixed(2)} MB |")
                appendLine("| Peak | ${result.peakMemoryMb.toFixed(2)} MB |")
                appendLine("| Final | ${result.finalMemoryMb.toFixed(2)} MB |")
                appendLine("| Memory Leak Suspected | ${if (result.memoryLeakSuspected) "❌ YES" else "✅ NO"} |")
                appendLine()

                if (result.failureReason != null) {
                    appendLine("## Failure Reason")
                    appendLine()
                    appendLine(result.failureReason)
                    appendLine()
                }

                if (result.errorLog.isNotEmpty()) {
                    appendLine("## Error Log (last 20 entries)")
                    appendLine()
                    appendLine("```")
                    result.errorLog.takeLast(20).forEach { appendLine(it) }
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

    /**
     * Утилита для форматирования double
     */
    private fun Double.toFixed(digits: Int): String {
        return String.format("%.${digits}f", this)
    }

    /**
     * Типы ошибок RTSP
     */
    private enum class RealtimeErrorType {
        CONNECTION,
        DECODING,
        TIMEOUT,
        OTHER,
    }
}

/**
 * Точка входа для запуска теста из консоли
 */
fun main() =
    runBlocking {
        val rtspUrl = "rtsp://demo:demo@192.168.1.100:554/stream" // Заменить на реальную камеру
        val durationMinutes = 60 // 1 час для теста (24 часа для production)

        val test =
            RtspLongRunStabilityTest(
                rtspUrl = rtspUrl,
                durationMinutes = durationMinutes,
                outputDir = "docs/reports/rtsp-stability-tests",
            )

        val result = test.run()

        println("\n=== Test Result ===")
        println("Status: ${if (result.testPassed) "PASSED" else "FAILED"}")
        println("Duration: ${result.durationSeconds}s")
        println("Frames: ${result.totalFramesReceived}")
        println("Avg FPS: ${result.averageFps.toFixed(2)}")
        println("Reconnects: ${result.reconnections}")
        println("Memory Leak: ${if (result.memoryLeakSuspected) "YES" else "NO"}")

        if (result.failureReason != null) {
            println("\nFailure Reason:")
            println(result.failureReason)
        }
    }
