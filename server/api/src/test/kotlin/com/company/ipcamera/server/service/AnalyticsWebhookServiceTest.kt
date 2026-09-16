package com.company.ipcamera.server.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnalyticsWebhookServiceTest {

    @Test
    fun `logs filtering by success and time range works`() {
        val service = AnalyticsWebhookService(logRetentionMs = 60_000L)

        val now = System.currentTimeMillis()
        val entries = listOf(
            WebhookDeliveryLogEntry(
                timestamp = now - 5_000,
                ruleId = "r1",
                cameraId = "c1",
                url = "https://example.test/1",
                attempt = 1,
                maxAttempts = 3,
                success = false,
                statusCode = 500,
                error = "boom"
            ),
            WebhookDeliveryLogEntry(
                timestamp = now - 3_000,
                ruleId = "r1",
                cameraId = "c1",
                url = "https://example.test/1",
                attempt = 2,
                maxAttempts = 3,
                success = true,
                statusCode = 200,
                error = null
            ),
            WebhookDeliveryLogEntry(
                timestamp = now - 1_000,
                ruleId = "r2",
                cameraId = "c2",
                url = "https://example.test/2",
                attempt = 1,
                maxAttempts = 3,
                success = true,
                statusCode = 204,
                error = null
            )
        )

        entries.forEach { service.recordDeliveryForTest(it) }

        val onlySuccess = service.getDeliveryLogs(success = true, limit = 10)
        assertEquals(2, onlySuccess.size)
        assertTrue(onlySuccess.all { it.success })

        val range = service.getDeliveryLogs(
            fromTimestamp = now - 3_500,
            toTimestamp = now - 500,
            limit = 10
        )
        assertEquals(2, range.size)
        assertTrue(range.all { it.timestamp in (now - 3_500)..(now - 500) })
    }

    @Test
    fun `stats and clear respect filters`() {
        val service = AnalyticsWebhookService(logRetentionMs = 60_000L)
        val now = System.currentTimeMillis()
        listOf(
            WebhookDeliveryLogEntry(now - 4_000, "r1", "c1", "u1", 1, 3, false, 500, "e"),
            WebhookDeliveryLogEntry(now - 3_000, "r1", "c1", "u1", 2, 3, true, 200, null),
            WebhookDeliveryLogEntry(now - 2_000, "r1", "c2", "u2", 1, 3, true, 200, null),
            WebhookDeliveryLogEntry(now - 1_000, "r2", "c2", "u2", 1, 3, false, 429, "rate")
        ).forEach { service.recordDeliveryForTest(it) }

        val statsR1 = service.getDeliveryStats(ruleId = "r1")
        assertEquals(3, statsR1.totalAttempts)
        assertEquals(2, statsR1.successfulAttempts)
        assertEquals(1, statsR1.failedAttempts)

        val removedFailedC2 = service.clearDeliveryLogs(cameraId = "c2", success = false)
        assertEquals(1, removedFailedC2)

        val remaining = service.getDeliveryLogs(limit = 20)
        assertEquals(3, remaining.size)
        assertFalse(remaining.any { it.cameraId == "c2" && !it.success })
    }

    @Test
    fun `retention prunes old records`() {
        val service = AnalyticsWebhookService(logRetentionMs = 1_000L)
        val now = System.currentTimeMillis()
        service.recordDeliveryForTest(
            WebhookDeliveryLogEntry(now - 10_000, "r-old", "c", "u", 1, 3, false, 500, "old")
        )
        service.recordDeliveryForTest(
            WebhookDeliveryLogEntry(now - 100, "r-new", "c", "u", 1, 3, true, 200, null)
        )

        val logs = service.getDeliveryLogs(limit = 10)
        assertEquals(1, logs.size)
        assertEquals("r-new", logs.first().ruleId)
    }

    @Test
    fun `clear all removes every record`() {
        val service = AnalyticsWebhookService(logRetentionMs = 60_000L)
        service.recordDeliveryForTest(
            WebhookDeliveryLogEntry(System.currentTimeMillis(), "r1", "c1", "u1", 1, 3, true, 200, null)
        )
        service.recordDeliveryForTest(
            WebhookDeliveryLogEntry(System.currentTimeMillis(), "r2", "c2", "u2", 1, 3, false, 500, "e")
        )

        val removed = service.clearDeliveryLogs()
        assertEquals(2, removed)
        assertTrue(service.getDeliveryLogs(limit = 10).isEmpty())
    }

    @Test
    fun `summary groups by rule and camera with sorting`() {
        val service = AnalyticsWebhookService(logRetentionMs = 60_000L)
        val now = System.currentTimeMillis()
        listOf(
            WebhookDeliveryLogEntry(now - 4_000, "r1", "c1", "u", 1, 3, false, 500, "e"),
            WebhookDeliveryLogEntry(now - 3_000, "r1", "c1", "u", 2, 3, true, 200, null),
            WebhookDeliveryLogEntry(now - 2_000, "r1", "c2", "u", 1, 3, true, 200, null),
            WebhookDeliveryLogEntry(now - 1_000, "r2", "c1", "u", 1, 3, false, 429, "rate"),
            WebhookDeliveryLogEntry(now - 500, "r2", "c1", "u", 2, 3, false, 500, "e2")
        ).forEach { service.recordDeliveryForTest(it) }

        val summary = service.getDeliverySummary(limit = 10)
        assertEquals(3, summary.size)

        // Sorted by total attempts desc: r1+c1 (2), r2+c1 (2), r1+c2 (1)
        assertEquals("r1", summary[0].ruleId)
        assertEquals("c1", summary[0].cameraId)
        assertEquals(2, summary[0].totalAttempts)
        assertEquals(1, summary[0].successfulAttempts)
        assertEquals(1, summary[0].failedAttempts)

        assertEquals("r2", summary[1].ruleId)
        assertEquals("c1", summary[1].cameraId)
        assertEquals(2, summary[1].totalAttempts)
        assertEquals(0, summary[1].successfulAttempts)
        assertEquals(2, summary[1].failedAttempts)
        assertEquals(now - 500, summary[1].lastAttemptTimestamp)

        assertEquals("r1", summary[2].ruleId)
        assertEquals("c2", summary[2].cameraId)
        assertEquals(1, summary[2].totalAttempts)
    }

    @Test
    fun `summary respects filters and time range`() {
        val service = AnalyticsWebhookService(logRetentionMs = 60_000L)
        val now = System.currentTimeMillis()
        listOf(
            WebhookDeliveryLogEntry(now - 6_000, "r1", "c1", "u", 1, 3, true, 200, null),
            WebhookDeliveryLogEntry(now - 5_000, "r1", "c1", "u", 2, 3, false, 500, "e"),
            WebhookDeliveryLogEntry(now - 4_000, "r1", "c2", "u", 1, 3, true, 200, null),
            WebhookDeliveryLogEntry(now - 3_000, "r2", "c1", "u", 1, 3, true, 200, null)
        ).forEach { service.recordDeliveryForTest(it) }

        val byRule = service.getDeliverySummary(ruleId = "r1", limit = 10)
        assertEquals(2, byRule.size)
        assertTrue(byRule.all { it.ruleId == "r1" })

        val byCamera = service.getDeliverySummary(cameraId = "c1", limit = 10)
        assertEquals(2, byCamera.size)
        assertTrue(byCamera.all { it.cameraId == "c1" })

        val byRange = service.getDeliverySummary(
            fromTimestamp = now - 4_500,
            toTimestamp = now - 2_500,
            limit = 10
        )
        assertEquals(2, byRange.size)
        // In range: r1+c2 and r2+c1
        assertTrue(byRange.any { it.ruleId == "r1" && it.cameraId == "c2" })
        assertTrue(byRange.any { it.ruleId == "r2" && it.cameraId == "c1" })
    }

    @Test
    fun `send retries then succeeds and writes delivery logs`() = runBlocking {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            if (callCount == 1) {
                respond(
                    content = "temporary failure",
                    status = HttpStatusCode.InternalServerError
                )
            } else {
                respond(
                    content = "ok",
                    status = HttpStatusCode.OK
                )
            }
        }
        val service = AnalyticsWebhookService(
            maxAttempts = 3,
            retryBaseDelayMs = 1L,
            customHttpClient = HttpClient(engine) {
                install(ContentNegotiation) {
                    json()
                }
            }
        )

        val result = service.send(
            url = "https://example.test/webhook",
            payload = buildJsonObject { put("event", "motion") },
            ruleId = "rule-1",
            cameraId = "cam-1"
        )

        assertTrue(result.isSuccess)
        assertEquals(2, callCount)
        val logs = service.getDeliveryLogs(limit = 10).sortedBy { it.attempt }
        assertEquals(2, logs.size)
        assertFalse(logs[0].success)
        assertTrue(logs[1].success)
    }

    @Test
    fun `send returns failure after max attempts and logs all tries`() = runBlocking {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(
                content = "still failing",
                status = HttpStatusCode.BadGateway
            )
        }
        val service = AnalyticsWebhookService(
            maxAttempts = 3,
            retryBaseDelayMs = 1L,
            customHttpClient = HttpClient(engine) {
                install(ContentNegotiation) {
                    json()
                }
            }
        )

        val result = service.send(
            url = "https://example.test/webhook",
            payload = buildJsonObject { put("event", "object") },
            ruleId = "rule-2",
            cameraId = "cam-2"
        )

        assertTrue(result.isFailure)
        assertEquals(3, callCount)
        val logs = service.getDeliveryLogs(limit = 10)
        assertEquals(3, logs.size)
        assertTrue(logs.all { !it.success })
    }
}

// Test-only helper to avoid network calls in unit tests.
private fun AnalyticsWebhookService.recordDeliveryForTest(entry: WebhookDeliveryLogEntry) {
    this.appendLogForTests(entry)
}

