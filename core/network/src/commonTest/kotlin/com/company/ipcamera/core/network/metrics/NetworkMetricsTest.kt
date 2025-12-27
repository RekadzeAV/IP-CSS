package com.company.ipcamera.core.network.metrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration

class NetworkMetricsTest {

    @Test
    fun testRequestMetrics() {
        val metric = RequestMetrics(
            url = "/api/test",
            method = "GET",
            statusCode = 200,
            duration = Duration.parse("100ms"),
            requestSize = 100,
            responseSize = 500,
            success = true
        )

        assertEquals("/api/test", metric.url)
        assertEquals("GET", metric.method)
        assertEquals(200, metric.statusCode)
        assertEquals(100, metric.requestSize)
        assertEquals(500, metric.responseSize)
        assertTrue(metric.success)
    }

    @Test
    fun testNetworkMetricsCollector() {
        val collector = NetworkMetricsCollector()

        collector.recordMetric(
            RequestMetrics(
                url = "/api/test1",
                method = "GET",
                statusCode = 200,
                duration = Duration.parse("100ms"),
                success = true
            )
        )

        collector.recordMetric(
            RequestMetrics(
                url = "/api/test2",
                method = "POST",
                statusCode = 500,
                duration = Duration.parse("200ms"),
                success = false
            )
        )

        val aggregated = collector.getAggregatedMetrics()

        assertEquals(2, aggregated.totalRequests)
        assertEquals(1, aggregated.successfulRequests)
        assertEquals(1, aggregated.failedRequests)
        assertTrue(aggregated.requestsByStatusCode.containsKey(200))
        assertTrue(aggregated.requestsByStatusCode.containsKey(500))
    }

    @Test
    fun testNetworkMetricsCollectorClear() {
        val collector = NetworkMetricsCollector()

        collector.recordMetric(
            RequestMetrics(
                url = "/api/test",
                method = "GET",
                statusCode = 200,
                duration = Duration.parse("100ms"),
                success = true
            )
        )

        assertEquals(1, collector.metrics.value.size)

        collector.clear()

        assertEquals(0, collector.metrics.value.size)
    }

    @Test
    fun testGetRecentMetrics() {
        val collector = NetworkMetricsCollector()

        for (i in 1..150) {
            collector.recordMetric(
                RequestMetrics(
                    url = "/api/test$i",
                    method = "GET",
                    statusCode = 200,
                    duration = Duration.parse("100ms"),
                    success = true
                )
            )
        }

        val recent = collector.getRecentMetrics(50)
        assertEquals(50, recent.size)
    }
}

