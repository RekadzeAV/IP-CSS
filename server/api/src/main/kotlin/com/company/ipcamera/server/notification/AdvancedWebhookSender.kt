package com.company.ipcamera.server.notification

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.security.Signature
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Расширенная конфигурация webhook уведомлений.
 */
data class WebhookConfig(
    val enabled: Boolean = false,
    val defaultUrl: String? = null,
    val timeoutMs: Long = 10000L,
    val maxRetries: Int = 3,
    val retryBaseDelayMs: Long = 1000L,
    val hmacSecret: String? = null,
    val customHeaders: Map<String, String> = emptyMap()
) {
    companion object {
        private const val PREFIX = "WEBHOOK_"
        
        fun fromEnvironment(): WebhookConfig? {
            val enabled = System.getenv("${PREFIX}ENABLED")?.toBoolean() ?: false
            if (!enabled) return null
            
            val defaultUrl = System.getenv("${PREFIX}DEFAULT_URL")
            if (defaultUrl == null || defaultUrl.isBlank()) {
                logger.warn { "Webhook enabled but no URL provided" }
                return null
            }
            
            return WebhookConfig(
                enabled = enabled,
                defaultUrl = defaultUrl.trim(),
                timeoutMs = System.getenv("${PREFIX}TIMEOUT_MS")?.toLongOrNull() ?: 10000L,
                maxRetries = System.getenv("${PREFIX}MAX_RETRIES")?.toIntOrNull() ?: 3,
                retryBaseDelayMs = System.getenv("${PREFIX}RETRY_BASE_DELAY_MS")?.toLongOrNull() ?: 1000L,
                hmacSecret = System.getenv("NOTIFICATION_WEBHOOK_SECRET"),
                customHeaders = emptyMap()
            )
        }
    }
}

/**
 * Продвинутый webhook sender с HMAC подписью и retry логикой.
 */
class AdvancedWebhookSender(
    private val config: WebhookConfig,
    private val customHttpClient: HttpClient? = null
) {
    private val client = customHttpClient ?: HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMs
            connectTimeoutMillis = config.timeoutMs
            socketTimeoutMillis = config.timeoutMs
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * Отправить webhook уведомление.
     */
    suspend fun send(
        url: String,
        payload: WebhookPayload,
        headers: Map<String, String> = emptyMap()
    ): Result<WebhookResult> {
        if (!config.enabled) {
            logger.debug { "Webhook disabled, skipping notification to $url" }
            return Result.success(WebhookResult(success = true, statusCode = null))
        }

                val targetUrl = url.trim()
        if (targetUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid webhook URL"))
        }

        // Автоматическая подпись HMAC, если секрет задан, и вызывающий не передал собственные заголовки
        if (config.hmacSecret != null && headers.isEmpty()) {
            return sendSigned(targetUrl, payload)
        }

        var lastError: Throwable? = null
        
        repeat(config.maxRetries) { attempt ->
            try {
                val result = sendInternal(targetUrl, payload, headers)
                if (result.success) {
                    return Result.success(result)
                }
                
                lastError = IllegalStateException("Webhook returned error: ${result.error}")
                logger.warn { "Webhook send attempt ${attempt + 1}/${config.maxRetries} failed: ${result.error}" }
            } catch (e: Exception) {
                lastError = e
                logger.warn(e) { "Webhook send attempt ${attempt + 1}/${config.maxRetries} failed" }
            }
            
            if (attempt < config.maxRetries - 1) {
                val backoff = (config.retryBaseDelayMs * (1L shl attempt)).coerceAtMost(30000L)
                logger.debug { "Retrying webhook in ${backoff}ms..." }
                delay(backoff)
            }
        }
        
        return Result.failure(lastError ?: IllegalStateException("Webhook delivery failed"))
    }

    /**
     * Отправить webhook с подписью HMAC-SHA256.
     */
    suspend fun sendSigned(
        url: String,
        payload: WebhookPayload
    ): Result<WebhookResult> {
        if (config.hmacSecret == null) {
            return send(url, payload)
        }

        val timestamp = System.currentTimeMillis()
        val bodyJson = Json.encodeToString(WebhookPayload.serializer(), payload)
        val signature = generateHmacSignature(bodyJson, timestamp)

        val headers = mapOf(
            "X-IPCSS-Timestamp" to timestamp.toString(),
            "X-IPCSS-Signature" to signature,
            "X-IPCSS-Signature-Ver" to "1"
        )

        return send(url, payload, headers)
    }

    /**
     * Отправить уведомление о событии.
     */
    suspend fun sendEventNotification(
        url: String,
        eventId: String,
        eventType: String,
        cameraId: String?,
        cameraName: String?,
        severity: String,
        message: String,
        metadata: Map<String, String> = emptyMap()
    ): Result<WebhookResult> {
        val payload = WebhookPayload(
            type = "EVENT_NOTIFICATION",
            timestamp = System.currentTimeMillis(),
            event = EventPayload(
                id = eventId,
                type = eventType,
                cameraId = cameraId,
                cameraName = cameraName,
                severity = severity,
                message = message,
                metadata = metadata
            )
        )

        return sendSigned(url, payload)
    }

    private suspend fun sendInternal(
        url: String,
        payload: WebhookPayload,
        extraHeaders: Map<String, String>
    ): WebhookResult {
        try {
            val bodyJson = Json.encodeToString(WebhookPayload.serializer(), payload)
            
                                    val response = client.post(url) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                config.customHeaders.forEach { (key, value) -> header(key, value) }
                extraHeaders.forEach { (key, value) -> header(key, value) }
                setBody(bodyJson)
            }

            if (response.status.isSuccess()) {
                logger.debug { "Webhook sent successfully to $url: ${response.status}" }
                return WebhookResult(
                    success = true,
                    statusCode = response.status.value,
                    responseBody = response.bodyAsText()
                )
            } else {
                val errorBody = response.bodyAsText()
                logger.warn { "Webhook failed with status ${response.status}: $errorBody" }
                return WebhookResult(
                    success = false,
                    statusCode = response.status.value,
                    error = "HTTP ${response.status.value}: $errorBody"
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send webhook to $url" }
            return WebhookResult(
                success = false,
                statusCode = null,
                error = e.message ?: "Unknown error"
            )
        }
    }

    private fun generateHmacSignature(body: String, timestamp: Long): String {
        val data = "$timestamp.$body"
        val key = config.hmacSecret!!.toByteArray()
        val message = data.toByteArray()

        val hmacInstance: javax.crypto.Mac = javax.crypto.Mac.getInstance("HmacSHA256")
        val keySpec = javax.crypto.spec.SecretKeySpec(key, "HmacSHA256")
        hmacInstance.init(keySpec)
        val signature = hmacInstance.doFinal(message)

        return signature.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Webhook payload.
 */
@Serializable
data class WebhookPayload(
    val type: String,
    val timestamp: Long,
    val event: EventPayload? = null,
    val notification: NotificationPayload? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Payload события.
 */
@Serializable
data class EventPayload(
    val id: String,
    val type: String,
    val cameraId: String?,
    val cameraName: String?,
    val severity: String,
    val message: String,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Payload уведомления.
 */
@Serializable
data class NotificationPayload(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val priority: String
)

/**
 * Результат webhook отправки.
 */
data class WebhookResult(
    val success: Boolean,
    val statusCode: Int?,
    val error: String? = null,
    val responseBody: String? = null
)
