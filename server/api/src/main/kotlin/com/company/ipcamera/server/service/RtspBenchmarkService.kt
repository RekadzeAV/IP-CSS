package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.rtsp.SimpleRtspBenchmarkRunner
import com.company.ipcamera.server.dto.RtspBenchmarkRequest
import com.company.ipcamera.server.dto.RtspBenchmarkResponse
import mu.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * Сервис для управления RTSP бенчмарками
 */
class RtspBenchmarkService {
    
    private val benchmarkRunner = SimpleRtspBenchmarkRunner()
    private val activeTests = mutableMapOf<String, BenchmarkTestState>()
    
    data class BenchmarkTestState(
        val id: String,
        val status: String, // RUNNING, COMPLETED, FAILED
        val request: RtspBenchmarkRequest,
        val result: RtspBenchmarkResponse?,
        val progress: Double,
        val job: Job?,
        val errorMessage: String?
    )
    
    /**
     * Запустить бенчмарк асинхронно
     */
    suspend fun startBenchmark(request: RtspBenchmarkRequest): String {
        val testId = UUID.randomUUID().toString()
        
        val state = BenchmarkTestState(
            id = testId,
            status = "RUNNING",
            request = request,
            result = null,
            progress = 0.0,
            job = null,
            errorMessage = null
        )
        
        activeTests[testId] = state
        
        // Запуск в фоновом режиме
        val job = CoroutineScope(Dispatchers.Default).launch {
            try {
                logger.info { "Starting RTSP benchmark: testId=$testId url=${request.rtspUrl}" }
                
                val result = benchmarkRunner.runBenchmark(
                    rtspUrl = request.rtspUrl,
                    duration = (request.durationSeconds.toLong()).seconds,
                    config = com.company.ipcamera.core.network.rtsp.RtspBenchmarkConfig(
                        rtspUrl = request.rtspUrl,
                        duration = (request.durationSeconds.toLong()).seconds,
                        expectedFps = request.expectedFps
                    )
                )
                
                val response = RtspBenchmarkResponse(
                    status = "COMPLETED",
                    testId = testId,
                    durationSeconds = result.summary.duration.inWholeSeconds.toDouble(),
                    avgFps = result.summary.avgFps,
                    minFps = result.summary.minFps,
                    maxFps = result.summary.maxFps,
                    avgCpuUsage = result.summary.avgCpuUsage,
                    maxCpuUsage = result.summary.maxCpuUsage,
                    avgMemoryMb = result.summary.avgMemoryUsageBytes / (1024.0 * 1024.0),
                    maxMemoryMb = result.summary.maxMemoryUsageBytes / (1024.0 * 1024.0),
                    avgLatencyMs = result.summary.avgLatencyMs,
                    maxLatencyMs = result.summary.maxLatencyMs.toDouble(),
                    totalFrames = result.metrics.sumOf { it.totalFrames }.toLong().coerceAtLeast(result.summary.totalDroppedFrames),
                    droppedFrames = result.summary.totalDroppedFrames,
                    reconnections = result.summary.totalReconnections,
                    errors = result.summary.totalErrors,
                    errorMessages = result.errors,
                    passed = result.summary.passCriteria,
                    timestamp = result.timestamp
                )
                
                activeTests[testId] = activeTests[testId]?.copy(
                    status = "COMPLETED",
                    result = response,
                    progress = 1.0
                ) ?: state.copy(
                    status = "COMPLETED",
                    result = response,
                    progress = 1.0
                )
                
                logger.info { "Benchmark completed: testId=$testId avgFps=${response.avgFps} passed=${response.passed}" }
                
            } catch (e: Exception) {
                val errorMsg = "Benchmark failed: ${e.message}"
                logger.error(e) { errorMsg }
                
                activeTests[testId] = activeTests[testId]?.copy(
                    status = "FAILED",
                    errorMessage = e.message,
                    progress = activeTests[testId]?.progress ?: 0.0
                ) ?: state.copy(
                    status = "FAILED",
                    errorMessage = e.message,
                    progress = 0.0
                )
            }
        }
        
        activeTests[testId] = state.copy(job = job)
        
        return testId
    }
    
    /**
     * Запустить несколько бенчмарков одновременно
     */
    suspend fun startMultiStreamBenchmark(request: RtspBenchmarkRequest): Map<String, String> {
        // Парсинг URL с паттерном {1,2,3}
        val urls = parseMultiStreamUrls(request.rtspUrl)
        
        val results = mutableMapOf<String, String>()
        
        urls.forEach { url ->
            val subRequest = request.copy(rtspUrl = url)
            val testId = startBenchmark(subRequest)
            results[url] = testId
        }
        
        return results
    }
    
    /**
     * Получить статус теста
     */
    fun getTestStatus(testId: String): BenchmarkTestState? {
        return activeTests[testId]
    }
    
    /**
     * Получить результат теста
     */
    fun getTestResult(testId: String): RtspBenchmarkResponse? {
        return activeTests[testId]?.result
    }
    
    /**
     * Отменить тест
     */
    fun cancelTest(testId: String) {
        val state = activeTests[testId]
        if (state?.status == "RUNNING") {
            state.job?.cancel()
            activeTests[testId] = state.copy(status = "CANCELLED")
            logger.info { "Benchmark cancelled: testId=$testId" }
        }
    }
    
    /**
     * Получить все активные тесты
     */
    fun getAllTests(): List<BenchmarkTestState> {
        return activeTests.values.toList()
    }
    
    /**
     * Очистить завершенные тесты старше N часов
     */
    fun cleanupOldTests(hours: Int = 24) {
        val cutoffTime = System.currentTimeMillis() - (hours * 60 * 60 * 1000L)
        
        val toRemove = activeTests.entries.filter { entry ->
            entry.value.result?.timestamp?.let { it < cutoffTime } == true ||
            entry.value.status == "CANCELLED"
        }.map { it.key }
        
        toRemove.forEach { activeTests.remove(it) }
        
        if (toRemove.isNotEmpty()) {
            logger.info { "Cleaned up ${toRemove.size} old benchmark tests" }
        }
    }
    
    private fun parseMultiStreamUrls(template: String): List<String> {
        // Пример: rtsp://server/stream{1,2,3} -> [rtsp://server/stream1, rtsp://server/stream2, rtsp://server/stream3]
        val pattern = Regex("""\{(\d+(?:,\d+)*)\}""")
        val match = pattern.find(template) ?: return listOf(template)
        
        val numbers = match.groupValues[1].split(",").map { it.toInt() }
        val prefix = template.substring(0, match.range.start)
        val suffix = template.substring(match.range.endInclusive + 1)
        
        return numbers.map { num -> "$prefix$num$suffix" }
    }
}
