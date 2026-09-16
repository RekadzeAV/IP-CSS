package com.company.ipcamera.desktop.performance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}

/**
 * Система мониторинга производительности приложения
 * Отслеживает метрики производительности, память, FPS и другие показатели
 */
class PerformanceMonitor {
    private val metrics = ConcurrentHashMap<String, PerformanceMetric>()
    private val _performanceData = MutableStateFlow<PerformanceData>(PerformanceData())
    val performanceData: StateFlow<PerformanceData> = _performanceData.asStateFlow()

    private val timeSource = TimeSource.Monotonic
    private val frameCount = AtomicLong(0)
    private var lastFpsUpdate = timeSource.markNow()

    /**
     * Начать измерение производительности операции
     */
    fun startMeasurement(operation: String): MeasurementHandle {
        val startTime = timeSource.markNow()
        return MeasurementHandle(operation, startTime) { duration ->
            recordMetric(operation, duration)
        }
    }

    /**
     * Записать метрику производительности
     */
    fun recordMetric(operation: String, duration: kotlin.time.Duration) {
        val metric = metrics.getOrPut(operation) {
            PerformanceMetric(operation)
        }
        metric.addSample(duration)

        // Обновляем общие данные производительности
        updatePerformanceData()
    }

    /**
     * Записать использование памяти
     */
    fun recordMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()

        val memoryMetric = metrics.getOrPut("memory") {
            PerformanceMetric("memory")
        }

        // Обновляем данные производительности
        _performanceData.value = _performanceData.value.copy(
            memoryUsed = usedMemory,
            memoryTotal = totalMemory,
            memoryMax = maxMemory,
            memoryUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble() * 100).toFloat()
        )
    }

    /**
     * Записать кадр (для расчета FPS)
     */
    fun recordFrame() {
        frameCount.incrementAndGet()
        val now = timeSource.markNow()
        val elapsed = now - lastFpsUpdate

        // Обновляем FPS каждую секунду
        if (elapsed.inWholeSeconds >= 1) {
            val fps = frameCount.get().toFloat() / elapsed.inWholeSeconds.toFloat()
            _performanceData.value = _performanceData.value.copy(
                fps = fps
            )
            frameCount.set(0)
            lastFpsUpdate = now
        }
    }

    /**
     * Получить метрику по имени операции
     */
    fun getMetric(operation: String): PerformanceMetric? {
        return metrics[operation]
    }

    /**
     * Получить все метрики
     */
    fun getAllMetrics(): Map<String, PerformanceMetric> {
        return metrics.toMap()
    }

    /**
     * Очистить все метрики
     */
    fun clearMetrics() {
        metrics.clear()
        _performanceData.value = PerformanceData()
    }

    /**
     * Обновить общие данные производительности
     */
    private fun updatePerformanceData() {
        val avgDurations = metrics.values
            .filter { it.operation != "memory" }
            .mapNotNull { it.getAverageDuration() }

        val totalAvg = if (avgDurations.isNotEmpty()) {
            avgDurations.average()
        } else {
            0.0
        }

        _performanceData.value = _performanceData.value.copy(
            averageOperationTime = totalAvg.toFloat()
        )
    }

    /**
     * Запустить периодический мониторинг памяти
     */
    fun startMemoryMonitoring(intervalMs: Long = 5000) {
        // Это должно быть вызвано из корутины
        // В реальной реализации можно использовать coroutineScope.launch
    }
}

/**
 * Данные производительности
 */
data class PerformanceData(
    val fps: Float = 0f,
    val memoryUsed: Long = 0L,
    val memoryTotal: Long = 0L,
    val memoryMax: Long = 0L,
    val memoryUsagePercent: Float = 0f,
    val averageOperationTime: Float = 0f
)

/**
 * Метрика производительности операции
 */
class PerformanceMetric(
    val operation: String
) {
    private val samples = mutableListOf<kotlin.time.Duration>()
    private val maxSamples = 100 // Храним последние 100 измерений

    fun addSample(duration: kotlin.time.Duration) {
        synchronized(samples) {
            samples.add(duration)
            if (samples.size > maxSamples) {
                samples.removeAt(0)
            }
        }
    }

    fun getAverageDuration(): Double? {
        synchronized(samples) {
            return if (samples.isEmpty()) null else {
                samples.map { it.inWholeMilliseconds.toDouble() }.average()
            }
        }
    }

    fun getMinDuration(): kotlin.time.Duration? {
        synchronized(samples) {
            return samples.minOrNull()
        }
    }

    fun getMaxDuration(): kotlin.time.Duration? {
        synchronized(samples) {
            return samples.maxOrNull()
        }
    }

    fun getSampleCount(): Int {
        synchronized(samples) {
            return samples.size
        }
    }
}

/**
 * Handle для измерения производительности операции
 */
class MeasurementHandle(
    private val operation: String,
    private val startTime: TimeSource.Monotonic.ValueTimeMark,
    private val onComplete: (kotlin.time.Duration) -> Unit
) {
    fun complete() {
        val duration = startTime.elapsedNow()
        onComplete(duration)
    }
}

/**
 * Глобальный экземпляр монитора производительности
 */
object GlobalPerformanceMonitor {
    private val monitor = PerformanceMonitor()

    fun getInstance(): PerformanceMonitor = monitor
}
