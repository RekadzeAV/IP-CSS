package com.company.ipcamera.core.network.metrics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration

/**
 * Метрики сетевых запросов
 */
data class RequestMetrics(
    val url: String,
    val method: String,
    val statusCode: Int?,
    val duration: Duration,
    val requestSize: Long = 0,
    val responseSize: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean
)

/**
 * Агрегированные метрики
 */
data class AggregatedMetrics(
    val totalRequests: Long = 0,
    val successfulRequests: Long = 0,
    val failedRequests: Long = 0,
    val averageResponseTime: Duration = Duration.ZERO,
    val totalBytesSent: Long = 0,
    val totalBytesReceived: Long = 0,
    val requestsByStatusCode: Map<Int, Long> = emptyMap()
)

/**
 * Сборщик метрик для сетевых запросов
 */
class NetworkMetricsCollector {
    private val _metrics = MutableStateFlow<List<RequestMetrics>>(emptyList())
    val metrics: StateFlow<List<RequestMetrics>> = _metrics.asStateFlow()

    private val maxMetricsHistory = 1000

    /**
     * Добавить метрику запроса
     */
    fun recordMetric(metric: RequestMetrics) {
        val current = _metrics.value.toMutableList()
        current.add(metric)

        // Ограничиваем историю
        if (current.size > maxMetricsHistory) {
            current.removeAt(0)
        }

        _metrics.value = current
    }

    /**
     * Получить агрегированные метрики
     */
    fun getAggregatedMetrics(): AggregatedMetrics {
        val allMetrics = _metrics.value
        if (allMetrics.isEmpty()) {
            return AggregatedMetrics()
        }

        val successful = allMetrics.count { it.success }
        val failed = allMetrics.size - successful
        val totalDuration = allMetrics.sumOf { it.duration.inWholeMilliseconds }
        val avgDuration = if (allMetrics.isNotEmpty()) {
            Duration.parse("${totalDuration / allMetrics.size}ms")
        } else {
            Duration.ZERO
        }

        val totalSent = allMetrics.sumOf { it.requestSize }
        val totalReceived = allMetrics.sumOf { it.responseSize }

        val statusCodeCounts = allMetrics
            .filter { it.statusCode != null }
            .groupingBy { it.statusCode!! }
            .eachCount()
            .mapValues { it.value.toLong() }

        return AggregatedMetrics(
            totalRequests = allMetrics.size.toLong(),
            successfulRequests = successful.toLong(),
            failedRequests = failed.toLong(),
            averageResponseTime = avgDuration,
            totalBytesSent = totalSent,
            totalBytesReceived = totalReceived,
            requestsByStatusCode = statusCodeCounts
        )
    }

    /**
     * Очистить метрики
     */
    fun clear() {
        _metrics.value = emptyList()
    }

    /**
     * Получить метрики за последние N запросов
     */
    fun getRecentMetrics(count: Int = 100): List<RequestMetrics> {
        return _metrics.value.takeLast(count)
    }
}

