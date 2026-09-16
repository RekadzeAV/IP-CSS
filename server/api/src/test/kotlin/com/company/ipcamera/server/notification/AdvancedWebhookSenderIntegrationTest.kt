package com.company.ipcamera.server.notification

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdvancedWebhookSenderIntegrationTest {

    @Test
    fun `Webhook sender should send notification successfully`() = runTest {
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("https://example.com/webhook", request.url.toString())
                    val ct = (request.body as? OutgoingContent)?.contentType
                    assertEquals(ContentType.Application.Json, ct, "Content-Type should be application/json")
                    
                    respond(
                        content = "OK",
                        status = HttpStatusCode.OK
                    )
                }
            }
        }

        val config = WebhookConfig(
            enabled = true,
            defaultUrl = "https://example.com/webhook",
            maxRetries = 3
        )

        val sender = AdvancedWebhookSender(config, customHttpClient = mockClient)

        val payload = WebhookPayload(
            type = "EVENT_NOTIFICATION",
            timestamp = System.currentTimeMillis(),
            event = EventPayload(
                id = "evt-123",
                type = "MOTION_DETECTION",
                cameraId = "camera-001",
                cameraName = "Test Camera",
                severity = "WARNING",
                message = "Motion detected"
            )
        )

        val result = sender.send("https://example.com/webhook", payload)

        assertTrue(result.isSuccess, "Webhook send should succeed")
        val webhookResult = result.getOrNull()!!
        assertTrue(webhookResult.success, "Result should be successful")
        assertEquals(200, webhookResult.statusCode)
    }

    @Test
    fun `Webhook sender should send signed notification with HMAC`() = runTest {
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    // Проверяем HMAC заголовки
                    assertTrue { request.headers.contains("X-IPCSS-Timestamp") }
                    assertTrue { request.headers.contains("X-IPCSS-Signature") }
                    assertEquals("1", request.headers["X-IPCSS-Signature-Ver"])
                    
                    respond(
                        content = "OK",
                        status = HttpStatusCode.OK
                    )
                }
            }
        }

        val config = WebhookConfig(
            enabled = true,
            defaultUrl = "https://example.com/webhook",
            hmacSecret = "test-secret-key"
        )

        val sender = AdvancedWebhookSender(config, customHttpClient = mockClient)

        val payload = WebhookPayload(
            type = "EVENT_NOTIFICATION",
            timestamp = System.currentTimeMillis(),
            event = EventPayload(
                id = "evt-123",
                type = "TEST",
                cameraId = "camera-001",
                cameraName = "Test",
                severity = "INFO",
                message = "Test message"
            )
        )

        val result = sender.sendSigned("https://example.com/webhook", payload)

        assertTrue(result.isSuccess, "Signed webhook should succeed")
        val webhookResult = result.getOrNull()!!
        assertTrue(webhookResult.success, "Result should be successful")
    }

    @Test
    fun `Webhook sender should retry on failure`() = runTest {
        var attemptCount = 0
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    attemptCount++
                    if (attemptCount < 3) {
                        respond(
                            content = "Service Unavailable",
                            status = HttpStatusCode.ServiceUnavailable
                        )
                    } else {
                        respond(
                            content = "OK",
                            status = HttpStatusCode.OK
                        )
                    }
                }
            }
        }

        val config = WebhookConfig(
            enabled = true,
            defaultUrl = "https://example.com/webhook",
            maxRetries = 3,
            retryBaseDelayMs = 10 // Быстрая retry для тестов
        )

        val sender = AdvancedWebhookSender(config, customHttpClient = mockClient)

        val payload = WebhookPayload(
            type = "TEST",
            timestamp = System.currentTimeMillis()
        )

        val result = sender.send("https://example.com/webhook", payload)

        assertTrue(result.isSuccess, "Should succeed after retries")
        assertEquals(3, attemptCount, "Should have made 3 attempts")
    }

    @Test
    fun `Webhook sender should handle disabled config`() = runTest {
        val config = WebhookConfig(enabled = false)
        val sender = AdvancedWebhookSender(config)

        val payload = WebhookPayload(type = "TEST", timestamp = System.currentTimeMillis())

        val result = sender.send("https://example.com", payload)

        assertTrue(result.isSuccess, "Should succeed when disabled")
        val webhookResult = result.getOrNull()!!
        assertTrue(webhookResult.success, "Result should be successful")
    }

    @Test
    fun `Webhook sendEventNotification should construct proper payload`() = runTest {
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val body = (request.body as TextContent).text
                    assertTrue { body.contains("EVENT_NOTIFICATION") }
                    assertTrue { body.contains("MOTION_DETECTION") }
                    assertTrue { body.contains("camera-001") }
                    
                    respond(
                        content = "OK",
                        status = HttpStatusCode.OK
                    )
                }
            }
        }

        val config = WebhookConfig(
            enabled = true,
            defaultUrl = "https://example.com/webhook",
            hmacSecret = "secret"
        )

        val sender = AdvancedWebhookSender(config, customHttpClient = mockClient)

        val result = sender.sendEventNotification(
            url = "https://example.com/webhook",
            eventId = "evt-123",
            eventType = "MOTION_DETECTION",
            cameraId = "camera-001",
            cameraName = "Test Camera",
            severity = "WARNING",
            message = "Motion detected",
            metadata = mapOf("zone" to "front")
        )

        assertTrue(result.isSuccess, "Event notification should succeed")
    }
}
