package com.company.ipcamera.server.notification

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
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
private data class TelegramSendMessageRequest(
    val chat_id: String,
    val text: String,
    val parse_mode: String? = null,
    val disable_notification: Boolean = false
)

open class TelegramNotificationSender(
    private val config: TelegramConfig,
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 1500L,
    private val client: HttpClient? = null
) {
    private val httpClient: HttpClient = client ?: HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
            connectTimeoutMillis = 5000
            socketTimeoutMillis = 5000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    open suspend fun send(text: String): Result<Unit> {
        if (!config.enabled) {
            logger.debug { "Telegram notifications disabled, skip send" }
            return Result.success(Unit)
        }
        if (text.isBlank()) return Result.success(Unit)

        for (chatId in config.chatIds) {
            val result = sendToChat(chatId = chatId, text = text)
            if (result.isFailure) {
                return result
            }
        }
        return Result.success(Unit)
    }

    private suspend fun sendToChat(chatId: String, text: String): Result<Unit> {
        var lastError: Throwable? = null
        repeat(maxRetries) { attempt ->
            try {
                val response = httpClient.post("https://api.telegram.org/bot${config.botToken}/sendMessage") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        TelegramSendMessageRequest(
                            chat_id = chatId,
                            text = text
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    return Result.success(Unit)
                }
                val body = runCatching { response.body<String>() }.getOrDefault("")
                lastError = IllegalStateException("Telegram API error ${response.status.value}: $body")
            } catch (e: Exception) {
                lastError = e
            }

            if (attempt < maxRetries - 1) {
                delay(retryDelayMs)
            }
        }
        logger.error(lastError) { "Failed to send Telegram notification to chatId=$chatId" }
        return Result.failure(lastError ?: IllegalStateException("Telegram send failed"))
    }
}
