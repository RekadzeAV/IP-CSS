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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Serializable
private data class SmsRequest(
    val to: List<String>,
    val text: String,
    val from: String? = null
)

open class SmsNotificationSender(
    private val config: SmsConfig,
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

    open suspend fun send(text: String, to: List<String>): Result<Unit> {
        if (!config.enabled) return Result.success(Unit)
        if (text.isBlank()) return Result.success(Unit)
        val recipients = (to + config.defaultTo).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (recipients.isEmpty()) return Result.success(Unit)

        var lastError: Throwable? = null
        repeat(maxRetries) { attemptIndex ->
            val attempt = attemptIndex + 1
            try {
                val response = client.post(config.endpointUrl) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-API-Key", config.apiKey)
                    setBody(
                        SmsRequest(
                            to = recipients,
                            text = text,
                            from = config.from
                        )
                    )
                }
                if (response.status.isSuccess()) return Result.success(Unit)
                lastError = IllegalStateException("SMS provider HTTP ${response.status.value}")
                logger.warn { "SMS send attempt $attempt/$maxRetries failed: HTTP ${response.status.value}" }
            } catch (e: Exception) {
                lastError = e
                logger.warn(e) { "SMS send attempt $attempt/$maxRetries failed" }
            }
            if (attempt < maxRetries) {
                val backoff = (retryBaseDelayMs * (1L shl attemptIndex)).coerceAtMost(5000L)
                delay(backoff)
            }
        }
        return Result.failure(lastError ?: IllegalStateException("SMS delivery failed"))
    }
}
