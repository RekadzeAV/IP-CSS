package com.company.ipcamera.server.notification

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация Web Push (VAPID).
 */
data class WebPushConfig(
    val enabled: Boolean = false,
    val publicKey: String? = null,
    val privateKey: String? = null,
    val subject: String = "mailto:your-email@example.com",
    val timeoutMs: Long = 10000L,
    val maxRetries: Int = 3
) {
    companion object {
        private const val PREFIX = "WEBPUSH_"
        
        fun fromEnvironment(): WebPushConfig? {
            val enabled = System.getenv("${PREFIX}ENABLED")?.toBoolean() ?: false
            if (!enabled) return null
            
            val publicKey = System.getenv("${PREFIX}PUBLIC_KEY")
            val privateKey = System.getenv("${PREFIX}PRIVATE_KEY")
            
            if (publicKey == null || privateKey == null) {
                logger.warn { "WebPush enabled but keys not configured" }
                return null
            }
            
            return WebPushConfig(
                enabled = enabled,
                publicKey = publicKey,
                privateKey = privateKey,
                subject = System.getenv("${PREFIX}SUBJECT") ?: "mailto:admin@example.com",
                timeoutMs = System.getenv("${PREFIX}TIMEOUT_MS")?.toLongOrNull() ?: 10000L,
                maxRetries = System.getenv("${PREFIX}MAX_RETRIES")?.toIntOrNull() ?: 3
            )
        }
        
        /**
         * Генерация VAPID ключей (P-256): publicKey — uncompressed point (base64url),
         * privateKey — raw 32 байта (base64url).
         */
        fun generateVapidKeys(): VapidKeys {
            val pair = com.company.ipcamera.server.notification.WebPushCrypto.generateVapidKeyPair()
            return VapidKeys(
                publicKey = com.company.ipcamera.server.notification.WebPushCrypto
                    .b64UrlEncode(com.company.ipcamera.server.notification.WebPushCrypto.rawPublicKey(pair.public)),
                privateKey = com.company.ipcamera.server.notification.WebPushCrypto
                    .b64UrlEncode(com.company.ipcamera.server.notification.WebPushCrypto.rawPrivateKey(pair.private))
            )
        }
    }
}

/**
 * VAPID ключи.
 */
data class VapidKeys(
    val publicKey: String,
    val privateKey: String
)

/**
 * Web Push endpoint для браузера.
 */
data class WebPushSubscription(
    val endpoint: String,
    val p256dh: String,
    val auth: String
)

/**
 * Отправка Web Push уведомлений через VAPID.
 */
class WebPushSender(
    private val config: WebPushConfig,
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
     * Отправить Web Push уведомление.
     */
    suspend fun send(
        subscription: WebPushSubscription,
        payload: WebPushPayload,
        ttl: Int = 60 * 60 * 24 // 24 часа по умолчанию
    ): Result<WebPushResult> {
        if (!config.enabled) {
            return Result.success(WebPushResult(success = true, messageId = null))
        }

        var lastError: Throwable? = null
        
        repeat(config.maxRetries) { attempt ->
                        try {
                val result = sendInternal(subscription, payload, ttl)
                if (result.success) return Result.success(result)
                if (result.subscriptionInvalid) {
                    // 404/410: подписка удалена на стороне push-сервиса — ретраить бессмысленно.
                    logger.info { "Web Push subscription expired (invalid): ${subscription.endpoint}" }
                    return Result.success(result)
                }
                lastError = IllegalStateException("Web Push returned error: ${result.error}")
            } catch (e: Exception) {
                lastError = e
                logger.warn(e) { "Web Push send attempt ${attempt + 1}/${config.maxRetries} failed" }
                
                if (attempt < config.maxRetries - 1) {
                    val backoff = (1000L * (1L shl attempt)).coerceAtMost(10000L)
                    delay(backoff)
                }
            }
        }
        
        return Result.failure(lastError ?: IllegalStateException("Web Push delivery failed"))
    }

    /**
     * Отправить нескольким подписчикам.
     */
    suspend fun sendBulk(
        subscriptions: List<WebPushSubscription>,
        payload: WebPushPayload,
        ttl: Int = 60 * 60 * 24
    ): Result<Map<String, WebPushResult>> {
        val results = mutableMapOf<String, WebPushResult>()
        
        for (sub in subscriptions) {
            val result = send(sub, payload, ttl)
            results[sub.endpoint] = result.getOrNull() ?: WebPushResult(
                success = false,
                messageId = null,
                error = result.exceptionOrNull()?.message
            )
        }
        
        val successCount = results.count { it.value.success }
        logger.info { "Web Push bulk: $successCount/${subscriptions.size} successful" }
        
        return Result.success(results)
    }

    private suspend fun sendInternal(
        subscription: WebPushSubscription,
        payload: WebPushPayload,
        ttl: Int
    ): WebPushResult {
        val endpoint = subscription.endpoint
        
        // VAPID JWT (aud = origin endpoint) + шифрование payload по RFC 8291
        val audience = runCatching {
            val uri = java.net.URI(endpoint)
            "${uri.scheme}://${uri.host}" + if (uri.port > 0) ":${uri.port}" else ""
        }.getOrDefault("https://fcm.googleapis.com")

        val vapidPrivateKey = decodeVapidPrivateKey(config.privateKey)
        val jwt = WebPushCrypto.signVapidJwt(
            privateKey = vapidPrivateKey,
            audience = audience,
            subject = config.subject,
            nowEpochSecond = Instant.now().epochSecond
        )
        val vapidPublicKeyRaw = WebPushCrypto.b64UrlDecode(config.publicKey!!)

        val encryptedPayload = encryptPayload(payload, subscription)
        
                        val response = client.post(endpoint) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header("Authorization", WebPushCrypto.vapidAuthorizationHeader(jwt, vapidPublicKeyRaw))
            header("TTL", ttl.toString())
            header("Content-Encoding", "aes128gcm")
            setBody(encryptedPayload)
        }

        if (response.status.value in 200..299) {
            logger.debug { "Web Push sent successfully: ${response.status}" }
            return WebPushResult(success = true, messageId = null)
        } else {
            val errorBody = response.bodyAsText()
            logger.warn { "Web Push failed: ${response.status} - $errorBody" }
            val invalid = response.status.value == 404 || response.status.value == 410
            return WebPushResult(
                success = false,
                messageId = null,
                error = "HTTP ${response.status.value}: $errorBody",
                subscriptionInvalid = invalid
            )
        }
    }

    /**
     * Декодирование VAPID приватного ключа: поддерживает raw 32 байта (base64url)
     * и PKCS#8 (стандартный base64).
     */
    private fun decodeVapidPrivateKey(encoded: String?): java.security.PrivateKey {
        require(!encoded.isNullOrBlank()) { "WebPush private key is not configured" }
        val urlDecoded = try { WebPushCrypto.b64UrlDecode(encoded) } catch (_: Exception) { ByteArray(0) }
        return when {
            urlDecoded.size == 32 -> WebPushCrypto.privateKeyFromRaw(urlDecoded)
            else -> {
                val der = java.util.Base64.getMimeDecoder().decode(encoded)
                KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(der))
            }
        }
    }

    private fun generateVapidJwt(audience: String): String {
        val privateKey = decodeVapidPrivateKey(config.privateKey)
        return WebPushCrypto.signVapidJwt(
            privateKey = privateKey,
            audience = audience,
            subject = config.subject,
            nowEpochSecond = Instant.now().epochSecond
        )
    }

    /** RFC 8291 aes128gcm шифрование payload ключами подписки. */
    private fun encryptPayload(payload: WebPushPayload, subscription: WebPushSubscription): ByteArray {
        val userPublicKey = WebPushCrypto.b64UrlDecode(subscription.p256dh)
        val authSecret = WebPushCrypto.b64UrlDecode(subscription.auth)
        val plainJson = Json.encodeToString(WebPushPayload.serializer(), payload).toByteArray()
        return WebPushCrypto.encryptAes128Gcm(userPublicKey, authSecret, plainJson)
    }

    private fun base64UrlEncode(data: ByteArray): String =
        WebPushCrypto.b64UrlEncode(data)
}

/**
 * Payload Web Push уведомления.
 */
@Serializable
data class WebPushPayload(
    val title: String,
    val body: String,
    val icon: String? = null,
    val badge: String? = null,
    val data: Map<String, String> = emptyMap(),
    val actions: List<WebPushAction> = emptyList()
)

@Serializable
data class WebPushAction(
    val action: String,
    val title: String,
    val icon: String? = null
)

/**
 * Результат Web Push отправки.
 */
data class WebPushResult(
    val success: Boolean,
    val messageId: String?,
    val error: String? = null,
    /** true, если push-сервис вернул 404/410 — подписка отозвана/не существует, ретраи бессмысленны. */
    val subscriptionInvalid: Boolean = false
)

/**
 * Тело запроса для отправки.
 */
// ByteArrayContent removed - using inline Content instead
