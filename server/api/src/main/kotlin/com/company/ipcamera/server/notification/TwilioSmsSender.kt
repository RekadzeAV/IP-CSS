package com.company.ipcamera.server.notification

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация SMS уведомлений через Twilio.
 */
data class TwilioConfig(
    val enabled: Boolean = false,
    val accountId: String? = null,
    val authToken: String? = null,
    val fromNumber: String? = null,
    val region: String = "us1"
) {
    companion object {
        private const val PREFIX = "TWILIO_"
        
        fun fromEnvironment(): TwilioConfig? {
            val enabled = System.getenv("${PREFIX}ENABLED")?.toBoolean() ?: false
            if (!enabled) return null
            
            val accountId = System.getenv("${PREFIX}ACCOUNT_SID") ?: return null
            val authToken = System.getenv("${PREFIX}AUTH_TOKEN") ?: return null
            val fromNumber = System.getenv("${PREFIX}FROM_NUMBER") ?: return null
            
            val region = System.getenv("${PREFIX}REGION") ?: "us1"
            
            return TwilioConfig(
                enabled = enabled,
                accountId = accountId,
                authToken = authToken,
                fromNumber = fromNumber,
                region = region
            )
        }
    }
}

/**
 * Отправка SMS через Twilio API.
 */
class TwilioSmsSender(
    private val config: TwilioConfig,
    private val timeoutMs: Long = 10000L,
    private val maxRetries: Int = 3,
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

    private val authHeader: String by lazy {
        val auth = "${config.accountId}:${config.authToken}"
        "Basic ${auth.encodeToBase64()}"
    }

    /**
     * Отправить SMS через Twilio.
     */
    suspend fun send(to: String, message: String): Result<SmsResult> {
        if (!config.enabled) {
            logger.debug { "Twilio disabled, skipping SMS to $to" }
            return Result.success(SmsResult(success = true, messageId = null))
        }

        val normalizedTo = to.trim()
        if (normalizedTo.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid recipient number"))
        }

        var lastError: Throwable? = null
        
        repeat(maxRetries) { attempt ->
                        try {
                val result = sendInternal(normalizedTo, message)
                if (result.success) return Result.success(result)
                lastError = IllegalStateException("Twilio API error: ${result.error}")
            } catch (e: Exception) {
                lastError = e
                logger.warn(e) { "Twilio send attempt ${attempt + 1}/$maxRetries failed" }
                
                if (attempt < maxRetries - 1) {
                    val backoff = (1000L * (1L shl attempt)).coerceAtMost(10000L)
                    delay(backoff)
                }
            }
        }
        
        return Result.failure(lastError ?: IllegalStateException("Twilio SMS delivery failed"))
    }

    /**
     * Отправить SMS нескольким получателям.
     */
    suspend fun sendBulk(to: List<String>, message: String): Result<Map<String, SmsResult>> {
        val results = mutableMapOf<String, SmsResult>()
        
        for (phoneNumber in to) {
            val result = send(phoneNumber, message)
            results[phoneNumber] = result.getOrNull() ?: SmsResult(
                success = false,
                messageId = null,
                error = result.exceptionOrNull()?.message
            )
        }
        
        val successCount = results.count { it.value.success }
        logger.info { "Twilio bulk SMS: $successCount/${to.size} successful" }
        
        return Result.success(results)
    }

    private suspend fun sendInternal(to: String, message: String): SmsResult {
        val url = "https://api.twilio.com/2010-04-01/Accounts/${config.accountId}/Messages.json"
        
        val body = buildString {
            append("To=").append(URLEncoder.encode(to, StandardCharsets.UTF_8.toString()))
            append("&From=").append(URLEncoder.encode(config.fromNumber!!, StandardCharsets.UTF_8.toString()))
            append("&Body=").append(URLEncoder.encode(message, StandardCharsets.UTF_8.toString()))
        }

        val response = client.post(url) {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
                append(HttpHeaders.Authorization, authHeader)
            }
            setBody(body)
        }

        if (response.status.isSuccess()) {
            val json = response.bodyAsText()
            val sid = Json.parseToJsonElement(json).jsonObject["sid"]?.toString()?.trim('"')
            
            logger.debug { "Twilio SMS sent to $to: $sid" }
            return SmsResult(success = true, messageId = sid)
        } else {
            val errorBody = response.bodyAsText()
            logger.error { "Twilio API error: ${response.status} - $errorBody" }
            return SmsResult(
                success = false,
                messageId = null,
                error = "HTTP ${response.status.value}: $errorBody"
            )
        }
    }
}

/**
 * Результат отправки SMS.
 */
data class SmsResult(
    val success: Boolean,
    val messageId: String?,
    val error: String? = null
)

/**
 * Расширение для кодирования в Base64.
 */
private fun String.encodeToBase64(): String {
    return java.util.Base64.getEncoder().encodeToString(this.toByteArray())
}
