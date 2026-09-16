package com.company.ipcamera.server.notification

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val logger = KotlinLogging.logger {}

/**
 * Минимальный webhook-канал уведомлений (2.2.4 backend).
 */
class NotificationWebhookSender(
    private val secret: String? = System.getenv("NOTIFICATION_WEBHOOK_SECRET"),
    private val timeoutMs: Long = 5000L,
    private val maxRetries: Int = 3,
    private val retryBaseDelayMs: Long = 250L,
    private val customHttpClient: HttpClient? = null
) {
    private val client = customHttpClient ?: HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = timeoutMs
            socketTimeoutMillis = timeoutMs
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun send(url: String, payload: JsonObject): Result<Unit> {
        if (url.isBlank()) return Result.failure(IllegalArgumentException("Webhook URL must not be blank"))

        val rawPayload = payload.toString()
        val signature = secret?.let { sign(rawPayload, it) }
        val deliveryId = UUID.randomUUID().toString()
        var lastError: Throwable? = null

        repeat(maxRetries) { attemptIndex ->
            val attempt = attemptIndex + 1
            try {
                val response = client.post(url) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-IPCSS-Notification-Delivery-Id", deliveryId)
                    header("X-IPCSS-Notification-Attempt", attempt.toString())
                    signature?.let { header("X-IPCSS-Signature", it) }
                    setBody(payload)
                }
                if (response.status.isSuccess()) {
                    return Result.success(Unit)
                }
                lastError = IllegalStateException("Notification webhook failed: HTTP ${response.status.value}")
                logger.warn { "Notification webhook attempt $attempt/$maxRetries failed: HTTP ${response.status.value}" }
            } catch (e: Exception) {
                lastError = e
                logger.warn(e) { "Notification webhook attempt $attempt/$maxRetries failed" }
            }

            if (attempt < maxRetries) {
                val backoff = (retryBaseDelayMs * (1L shl attemptIndex)).coerceAtMost(5000L)
                delay(backoff)
            }
        }

        return Result.failure(lastError ?: IllegalStateException("Notification webhook delivery failed"))
    }

    private fun sign(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
        return "sha256=" + digest.joinToString("") { "%02x".format(it) }
    }
}
