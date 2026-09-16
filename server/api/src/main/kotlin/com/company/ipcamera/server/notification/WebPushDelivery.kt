package com.company.ipcamera.server.notification

import com.company.ipcamera.server.service.PushTokenPurger
import com.company.ipcamera.shared.domain.model.Notification
import mu.KotlinLogging
import java.util.Base64

private val logger = KotlinLogging.logger {}

/**
 * Адаптер доставки push-уведомлений через Web Push (браузеры, platform="web").
 *
 * Токен устройства для web-платформы — base64url JSON-объект подписки
 * [WebPushSubscription] (endpoint/p256dh/auth), как его отдаёт
 * `PushSubscription.toJSON()` в браузере.
 *
 * При 404/410 от push-сервиса ([WebPushResult.subscriptionInvalid]) подписка
 * мертва — токен автоматически удаляется через [PushTokenPurger], и ретраи не выполняются.
 */
class WebPushDelivery(
    private val sender: WebPushSender,
    private val tokenPurger: PushTokenPurger? = null
) : PushNotificationDelivery {

    override val platform: String = "web"

    override fun supports(platform: String): Boolean = platform == "web"

    override suspend fun send(deviceTokens: List<String>, notification: Notification): Result<Unit> {
        if (deviceTokens.isEmpty()) return Result.success(Unit)

        var purged = 0
        var failures = 0
        for (rawToken in deviceTokens) {
            val subscription = parseSubscription(rawToken)
            if (subscription == null) {
                logger.warn { "Web Push token is not a valid subscription JSON; purging token" }
                purged += tokenPurger?.purgeTokens(listOf(rawToken)) ?: 0
                failures++
                continue
            }

            val payload = WebPushPayload(
                title = notification.title,
                body = notification.message,
                data = notification.extras
            )
            val result = sender.send(subscription, payload)

            when {
                result.isSuccess && result.getOrNull()?.success == true -> { /* delivered */ }
                result.isSuccess && result.getOrNull()?.subscriptionInvalid == true -> {
                    logger.info { "Web Push subscription invalid (404/410); purging token" }
                    purged += tokenPurger?.purgeTokens(listOf(rawToken)) ?: 0
                    failures++
                }
                result.isFailure -> {
                    logger.warn { "Web Push delivery failed: ${result.exceptionOrNull()?.message}" }
                    failures++
                }
                else -> failures++
            }
        }

        if (purged > 0) {
            logger.info { "Web Push auto-purged $purged invalid subscription token(s)" }
        }

        return if (failures < deviceTokens.size) Result.success(Unit) else Result.failure(
            IllegalStateException("Web Push delivery failed for all ${deviceTokens.size} token(s)")
        )
    }

    /**
     * Парсинг токена в [WebPushSubscription].
     * Принимает base64url(JSON) или plain JSON (для обратной совместимости с существующими токенами),
     * в обеих формах: плоской `{endpoint, p256dh, auth}` и браузерной `{endpoint, keys: {p256dh, auth}}`.
     */
    internal fun parseSubscription(rawToken: String): WebPushSubscription? {
        val raw = rawToken.trim()
        if (raw.isEmpty()) return null

        val candidates = mutableListOf(raw)
        runCatching {
            val decoded = Base64.getUrlDecoder().decode(raw)
            candidates += String(decoded, Charsets.UTF_8)
        }

        candidates.forEach { candidate ->
            // Плоская форма: {endpoint, p256dh, auth}
            val flat = runCatching {
                json.decodeFromString(FlatSubscriptionDto.serializer(), candidate)
            }.getOrNull()
            if (flat != null && flat.endpoint.startsWith("http") &&
                flat.p256dh.isNotBlank() && flat.auth.isNotBlank()
            ) {
                return WebPushSubscription(endpoint = flat.endpoint, p256dh = flat.p256dh, auth = flat.auth)
            }
            // Браузерная форма (PushSubscription.toJSON): {endpoint, keys: {p256dh, auth}}
            val nested = runCatching {
                json.decodeFromString(NestedSubscriptionDto.serializer(), candidate)
            }.getOrNull()
            if (nested != null && nested.endpoint.startsWith("http")) {
                val keys = nested.keys
                if (keys != null && keys.p256dh.isNotBlank() && keys.auth.isNotBlank()) {
                    return WebPushSubscription(endpoint = nested.endpoint, p256dh = keys.p256dh, auth = keys.auth)
                }
            }
        }
        return null
    }

    /** DTO плоской формы подписки. */
    @kotlinx.serialization.Serializable
    private data class FlatSubscriptionDto(
        val endpoint: String = "",
        val p256dh: String = "",
        val auth: String = ""
    )

    /** DTO браузерной формы подписки (PushSubscription.toJSON). */
    @kotlinx.serialization.Serializable
    private data class NestedSubscriptionDto(
        val endpoint: String = "",
        val keys: KeysDto? = null
    )

    @kotlinx.serialization.Serializable
    private data class KeysDto(val p256dh: String = "", val auth: String = "")

    private companion object {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    }
}

/**
 * Фабрика для создания push-канала Web Push из окружения (WEBPUSH_*).
 */
object WebPushDeliveryFactory {
    fun createFromEnvironment(tokenPurger: PushTokenPurger? = null): WebPushDelivery? {
        val config = WebPushConfig.fromEnvironment() ?: return null
        logger.info { "Web Push delivery channel enabled" }
        return WebPushDelivery(WebPushSender(config), tokenPurger)
    }
}
