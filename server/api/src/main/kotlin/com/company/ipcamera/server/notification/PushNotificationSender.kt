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
private data class PushRequest(
    val tokens: List<String>,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap()
)

class PushNotificationSender(
    private val config: PushConfig,
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

    suspend fun send(tokens: List<String>, title: String, body: String, data: Map<String, String>): Result<Unit> {
        if (!config.enabled) return Result.success(Unit)
        val normalizedTokens = tokens.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalizedTokens.isEmpty()) return Result.success(Unit)

        var lastError: Throwable? = null
        repeat(maxRetries) { attemptIndex ->
            val attempt = attemptIndex + 1
            try {
                val response = client.post(config.endpointUrl) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-API-Key", config.apiKey)
                    setBody(
                        PushRequest(
                            tokens = normalizedTokens,
                            title = title,
                            body = body,
                            data = data
                        )
                    )
                }
                if (response.status.isSuccess()) return Result.success(Unit)
                lastError = IllegalStateException("Push provider HTTP ${response.status.value}")
                logger.warn { "Push send attempt $attempt/$maxRetries failed: HTTP ${response.status.value}" }
            } catch (e: Exception) {
                lastError = e
                logger.warn(e) { "Push send attempt $attempt/$maxRetries failed" }
            }
            if (attempt < maxRetries) {
                val backoff = (retryBaseDelayMs * (1L shl attemptIndex)).coerceAtMost(5000L)
                delay(backoff)
            }
        }

        return Result.failure(lastError ?: IllegalStateException("Push delivery failed"))
    }
}
