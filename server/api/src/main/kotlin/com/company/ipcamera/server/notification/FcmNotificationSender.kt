package com.company.ipcamera.server.notification

import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/** Ответ Google OAuth2 token endpoint. */
@Serializable
internal data class GoogleTokenResponse(
    val access_token: String? = null,
    val expires_in: Int? = null,
    val token_type: String? = null
)

/**
 * Конфигурация Firebase Cloud Messaging (FCM) для Android push-уведомлений.
 */
data class FcmConfig(
    val enabled: Boolean = false,
    val projectId: String? = null,
    val serviceAccountPath: String? = null,
    val serviceAccountJson: String? = null
) {
    companion object {
        private const val PREFIX = "FCM_"
        
        fun fromEnvironment(): FcmConfig? {
            val enabled = System.getenv("${PREFIX}ENABLED")?.toBoolean() ?: false
            if (!enabled) return null
            
            val projectId = System.getenv("${PREFIX}PROJECT_ID") ?: return null
            val serviceAccountPath = System.getenv("${PREFIX}SERVICE_ACCOUNT_PATH")
            val serviceAccountJson = System.getenv("${PREFIX}SERVICE_ACCOUNT_JSON")
            
            if (serviceAccountPath == null && serviceAccountJson == null) {
                logger.warn { "FCM enabled but no service account provided" }
                return null
            }
            
            return FcmConfig(
                enabled = enabled,
                projectId = projectId,
                serviceAccountPath = serviceAccountPath,
                serviceAccountJson = serviceAccountJson
            )
        }
    }
}

/**
 * Отправка FCM push-уведомлений для Android устройств через FCM HTTP v1 API
 * (OAuth2 service-account JWT, без Firebase Admin SDK).
 */
class FcmNotificationSender(
    private val config: FcmConfig,
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 1000L,
    private val client: HttpClient? = null,
    /** Инъекция учётных данных для тестов; по умолчанию читается из config.serviceAccount* */
    private val credentialsOverride: FcmServiceAccountCredentials? = null,
    private val clockMillis: () -> Long = System::currentTimeMillis
) {
    private val httpClient: HttpClient = client ?: HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val credentials: FcmServiceAccountCredentials? by lazy { loadCredentials() }

    private fun loadCredentials(): FcmServiceAccountCredentials? = try {
        when {
            config.serviceAccountJson != null ->
                FcmServiceAccountCredentials.fromJson(config.serviceAccountJson)
            config.serviceAccountPath != null ->
                FcmServiceAccountCredentials.fromJson(java.io.File(config.serviceAccountPath).readText())
            else -> null
        }
    } catch (e: Exception) {
        logger.error(e) { "Failed to load FCM service account credentials" }
        null
    }

    // Кэш access token (живёт ~1 час; обновляем за минуту до истечения)
    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var cachedTokenExpiresAt: Long = 0

    /** Обмен JWT-assertion на OAuth2 access token (с ретраями). */
    private suspend fun getAccessToken(credentials: FcmServiceAccountCredentials): Result<String> {
        val now = clockMillis()
        cachedToken?.takeIf { now < cachedTokenExpiresAt }?.let { return Result.success(it) }

        var lastError: Throwable? = null
        repeat(maxRetries) { attempt ->
            try {
                val jwt = credentials.buildAssertionJwt(now)
                val response = httpClient.post("https://oauth2.googleapis.com/token") {
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(
                        "grant_type=" +
                            java.net.URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", Charsets.UTF_8) +
                            "&assertion=" + java.net.URLEncoder.encode(jwt, Charsets.UTF_8)
                    )
                }
                val body = runCatching { response.body<String>() }.getOrDefault("")
                if (!response.status.isSuccess()) {
                    lastError = IllegalStateException("Google OAuth error ${response.status.value}: $body")
                } else {
                    val parsed = Json { ignoreUnknownKeys = true }
                        .decodeFromString(GoogleTokenResponse.serializer(), body)
                    if (parsed.access_token.isNullOrBlank()) {
                        lastError = IllegalStateException("Google OAuth returned empty access_token")
                    } else {
                        cachedToken = parsed.access_token
                        cachedTokenExpiresAt =
                            now + (parsed.expires_in?.toLong()?.times(1000L)?.minus(60_000L) ?: 300_000L)
                        return Result.success(parsed.access_token)
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
            if (attempt < maxRetries - 1) delay(retryDelayMs)
        }
        return Result.failure(lastError ?: IllegalStateException("FCM auth failed"))
    }

    /**
     * Отправить FCM push-уведомления на указанные токены устройств через HTTP v1 API.
     * Возвращает failure, если хотя бы одна отправка не удалась после ретраев.
     */
    suspend fun send(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        priority: FcmPriority = FcmPriority.NORMAL
    ): Result<Unit> {
        if (!config.enabled || tokens.isEmpty()) {
            logger.debug { "FCM disabled or no tokens, skipping push to ${tokens.size} device(s)" }
            return Result.success(Unit)
        }

        val credentials = credentials
            ?: return Result.failure(IllegalStateException("FCM service account credentials not available"))

        val accessToken = getAccessToken(credentials).getOrElse { return Result.failure(it) }

        var lastError: Throwable? = null
        for (token in tokens) {
            var delivered = false
            var attempt = 0
            while (attempt < maxRetries && !delivered) {
                try {
                    val payload = buildJsonObject {
                        putJsonObject("message") {
                            put("token", token)
                            putJsonObject("notification") {
                                put("title", title)
                                put("body", body)
                            }
                            if (data.isNotEmpty()) {
                                putJsonObject("data") { data.forEach { (k, v) -> put(k, v) } }
                            }
                            putJsonObject("android") {
                                put("priority", if (priority == FcmPriority.HIGH) "HIGH" else "NORMAL")
                            }
                        }
                    }.toString()

                    val response = httpClient.post(
                        "https://fcm.googleapis.com/v1/projects/${credentials.projectId}/messages:send"
                    ) {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(payload)
                    }
                    val respBody = runCatching { response.body<String>() }.getOrDefault("")
                    when {
                        response.status.isSuccess() -> delivered = true
                        response.status.value == 404 || response.status.value == 410 -> {
                            // Токен отозван — ретраить бессмысленно, прерываем сразу.
                            lastError = IllegalStateException("FCM token invalid (${response.status.value}): $respBody")
                            break
                        }
                        else -> lastError =
                            IllegalStateException("FCM API error ${response.status.value}: $respBody")
                    }
                } catch (e: Exception) {
                    lastError = e
                }
                if (!delivered && attempt < maxRetries - 1) delay(retryDelayMs)
                attempt++
            }
            if (!delivered) return Result.failure(lastError ?: IllegalStateException("FCM delivery failed"))
        }
        return Result.success(Unit)
    }

    /**
     * Отправить уведомление с кастомными данными.
     * (Disabled - Firebase SDK not available)
     */
    suspend fun sendWithData(
        tokens: List<String>,
        title: String,
        body: String,
        data: FcmPayloadData
    ): Result<Unit> {
        return send(tokens, title, body, data.toMap())
    }
}

/**
 * Данные для FCM уведомления.
 */
data class FcmPayloadData(
    val type: String? = null,
    val cameraId: String? = null,
    val cameraName: String? = null,
    val eventId: String? = null,
    val recordingId: String? = null,
    val imageUrl: String? = null,
    val deepLink: String? = null
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
        }
    }
}

/**
 * Приоритет FCM уведомления.
 */
enum class FcmPriority {
    NORMAL,
    HIGH;

    // fun toFirebasePriority(): String {
    //     return when (this) {
    //         NORMAL -> MessagePriority.NORMAL.value()
    //         HIGH -> MessagePriority.HIGH.value()
    //     }
    // }
}
