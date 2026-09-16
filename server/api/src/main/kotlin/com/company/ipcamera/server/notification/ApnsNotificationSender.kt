package com.company.ipcamera.server.notification

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация Apple Push Notification service (APNs) для iOS push-уведомлений.
 */
data class ApnsConfig(
    val enabled: Boolean = false,
    val environment: ApnsEnvironment = ApnsEnvironment.DEVELOPMENT,
    val teamId: String? = null,
    val keyId: String? = null,
    val privateKeyPath: String? = null,
    val privateKey: String? = null,
    val bundleId: String? = null
) {
    enum class ApnsEnvironment(val host: String, val port: Int) {
        DEVELOPMENT("api.development.push.apple.com", 443),
        PRODUCTION("api.push.apple.com", 443)
    }

    companion object {
        private const val PREFIX = "APNS_"
        
        fun fromEnvironment(): ApnsConfig? {
            val enabled = System.getenv("${PREFIX}ENABLED")?.toBoolean() ?: false
            if (!enabled) return null
            
            val teamId = System.getenv("${PREFIX}TEAM_ID") ?: return null
            val keyId = System.getenv("${PREFIX}KEY_ID") ?: return null
            val bundleId = System.getenv("${PREFIX}BUNDLE_ID") ?: return null
            
            val environment = when (System.getenv("${PREFIX}ENVIRONMENT")?.lowercase()) {
                "production" -> ApnsEnvironment.PRODUCTION
                "development", null -> ApnsEnvironment.DEVELOPMENT
                else -> ApnsEnvironment.DEVELOPMENT
            }
            
            val privateKeyPath = System.getenv("${PREFIX}PRIVATE_KEY_PATH")
            val privateKey = System.getenv("${PREFIX}PRIVATE_KEY")
            
            if (privateKeyPath == null && privateKey == null) {
                logger.warn { "APNS enabled but no private key provided" }
                return null
            }
            
            return ApnsConfig(
                enabled = enabled,
                environment = environment,
                teamId = teamId,
                keyId = keyId,
                privateKeyPath = privateKeyPath,
                privateKey = privateKey,
                bundleId = bundleId
            )
        }
    }
}

/**
 * Отправка APNs push-уведомлений для iOS устройств.
 * Использует HTTP/2 API с JWT авторизацией.
 */
class ApnsNotificationSender(
    private val config: ApnsConfig,
    private val timeoutMs: Long = 5000L,
    private val maxRetries: Int = 3
) {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = timeoutMs
            socketTimeoutMillis = timeoutMs
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private var jwtToken: String? = null
    private var jwtTokenExpiry: Long = 0

    /**
     * Отправить APNs уведомление на одно или несколько устройств.
     */
    suspend fun send(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        badge: Int? = null,
        sound: String? = "default"
    ): Result<Unit> {
        if (!config.enabled) {
            logger.debug { "APNS disabled, skipping push to ${tokens.size} device(s)" }
            return Result.success(Unit)
        }

        val normalizedTokens = tokens.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalizedTokens.isEmpty()) {
            logger.debug { "No valid APNS tokens provided" }
            return Result.success(Unit)
        }

        var lastError: Throwable? = null
        
        for (token in normalizedTokens) {
            var attempt = 0
            while (attempt < maxRetries) {
                try {
                    val success = sendToDevice(token, title, body, data, badge, sound)
                    if (success) return Result.success(Unit)
                    
                    lastError = IllegalStateException("APNS delivery failed for token")
                    logger.warn { "APNS send attempt ${attempt + 1}/$maxRetries failed for token ${token.take(10)}..." }
                } catch (e: Exception) {
                    lastError = e
                    logger.warn(e) { "APNS send attempt ${attempt + 1}/$maxRetries failed" }
                }
                
                attempt++
                if (attempt < maxRetries) {
                    val backoff = (250L * (1L shl (attempt - 1))).coerceAtMost(5000L)
                    delay(backoff)
                }
            }
        }

        return Result.failure(lastError ?: IllegalStateException("APNS delivery failed"))
    }

    /**
     * Отправить APNs уведомление с кастомными данными.
     */
    suspend fun sendWithData(
        tokens: List<String>,
        title: String,
        body: String,
        data: ApnsPayloadData,
        badge: Int? = null,
        sound: String? = "default"
    ): Result<Unit> {
        return send(tokens, title, body, data.toMap(), badge, sound)
    }

    private suspend fun sendToDevice(
        deviceToken: String,
        title: String,
        body: String,
        data: Map<String, String>,
        badge: Int?,
        sound: String?
    ): Boolean {
        val jwt = getJwtToken()
        
        val payload = buildApnsPayload(title, body, data, badge, sound)
        
        try {
            val endpoint = "https://${config.environment.host}/3/device/$deviceToken"
            
            val response = client.post(endpoint) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header("apns-authentication", "Bearer $jwt")
                header("apns-topic", config.bundleId)
                header("apns-priority", "10")
                header("apns-push-type", "alert")
                setBody(payload)
            }
            
            if (response.status.isSuccess()) {
                logger.debug { "APNS notification sent to ${deviceToken.take(10)}..." }
                return true
            } else {
                val errorBody = response.bodyAsText()
                logger.warn { "APNS failed with status ${response.status}: $errorBody" }
                return false
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send APNS notification" }
            return false
        }
    }

    private suspend fun getJwtToken(): String {
        val now = System.currentTimeMillis() / 1000
        if (jwtToken != null && now < jwtTokenExpiry) {
            return jwtToken!!
        }
        val generated = generateJwt(now)
        // APNs принимает exp до 24ч; кэшируем до exp-45сек.
        jwtToken = generated.jwt
        jwtTokenExpiry = generated.exp
        return generated.jwt
    }

    /**
     * Формирует JWT для APNs (ES256) с приватным ключом .p8.
     * header: {alg: ES256, kid: keyId, typ: JWT}
     * claims: {iss: teamId, iat, exp}
     * подпись: SHA256withECDSA над base64url(header).base64url(payload)
     */
    private fun generateJwt(nowSec: Long): JwtResult {
        val teamId = config.teamId ?: throw IllegalStateException("APNS teamId is not configured")
        val keyId = config.keyId ?: throw IllegalStateException("APNS keyId is not configured")
        val privateKey = loadPrivateKey() ?: throw IllegalStateException("APNS private key is not configured")

        val exp = nowSec + 3600 // 1 hour (max allowed 24h)
        val header = ApnsJwtUtil.jsonOf(mapOf("alg" to "ES256", "kid" to keyId, "typ" to "JWT"))
        val payload = ApnsJwtUtil.jsonOf(mapOf("iss" to teamId, "iat" to nowSec, "exp" to exp))

        val signingInput = "${ApnsJwtUtil.b64url(header)}.${ApnsJwtUtil.b64url(payload)}"

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(signingInput.toByteArray(Charsets.US_ASCII))
        val derSignature = signature.sign()
        // Переводим DER в raw R||S (64 байта) — стандарт JWS ECDSA.
        val raw = ApnsJwtUtil.derToRawJws(derSignature)

        return JwtResult(
            jwt = "$signingInput.${ApnsJwtUtil.b64url(raw)}",
            exp = exp
        )
    }

    private fun loadPrivateKey(): java.security.PrivateKey? {
        val bytes: ByteArray? = config.privateKey?.let {
            try { java.util.Base64.getDecoder().decode(it.replace("\\s".toRegex(), "")) }
            catch (e: Exception) { null }
        } ?: config.privateKeyPath?.let { path ->
            try { java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)) }
            catch (e: Exception) { null }
        } ?: return null

        return try {
            val keyFactory = java.security.KeyFactory.getInstance("EC")
            keyFactory.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(bytes))
        } catch (e: Exception) {
            // Возможен SEC1 (некоторые .p8 в SEC1); пробуем секцию BEGIN PRIVATE KEY
            try {
                val pem = bytes!!.toString(Charsets.US_ASCII)
                val base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN EC PRIVATE KEY-----", "")
                    .replace("-----END EC PRIVATE KEY-----", "")
                    .replace("\\s".toRegex(), "")
                val decoded = java.util.Base64.getDecoder().decode(base64)
                java.security.KeyFactory.getInstance("EC")
                    .generatePrivate(java.security.spec.PKCS8EncodedKeySpec(decoded))
            } catch (e2: Exception) {
                logger.error(e2) { "Failed to load APNS EC private key" }
                null
            }
        }
    }

    private data class JwtResult(val jwt: String, val exp: Long)

    private fun buildApnsPayload(
        title: String,
        body: String,
        data: Map<String, String>,
        badge: Int?,
        sound: String?
    ): Any {
        return mapOf(
            "aps" to mapOf(
                "alert" to mapOf(
                    "title" to title,
                    "body" to body
                ),
                "badge" to (badge ?: 0),
                "sound" to (sound ?: "default")
            )
        ) + data
    }
}

/**
 * Данные для APNS уведомления.
 */
data class ApnsPayloadData(
    val type: String? = null,
    val cameraId: String? = null,
    val cameraName: String? = null,
    val eventId: String? = null,
    val recordingId: String? = null,
    val imageUrl: String? = null,
    val deepLink: String? = null,
    val category: String? = null
) {
    fun toMap(): Map<String, String> {
        return buildMap {
            type?.let { put("type", it) }
            cameraId?.let { put("cameraId", it) }
            cameraName?.let { put("cameraName", it) }
            eventId?.let { put("eventId", it) }
            recordingId?.let { put("recordingId", it) }
            imageUrl?.let { put("imageUrl", it) }
            deepLink?.let { put("deepLink", it) }
            category?.let { put("category", it) }
        }
    }
}
