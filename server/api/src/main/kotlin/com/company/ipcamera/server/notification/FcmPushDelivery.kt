package com.company.ipcamera.server.notification

import com.company.ipcamera.server.service.PushTokenPurger
import com.company.ipcamera.shared.domain.model.Notification
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Адаптер доставки push-уведомлений через FCM (Android, platform="android").
 *
 * Оборачивает [FcmNotificationSender] (FCM HTTP v1 API); активен, если FCM сконфигурирован.
 *
 * Особенность: `FcmNotificationSender.send` возвращает failure на **первом** мёртвом токене
 * (404/410 — «FCM token invalid»), прерывая обработку остальных. Для корректной
 * автоочистки и доставки остальным адаптер отправляет **по-токенно**: каждый токен —
 * отдельный вызов sender'а, поэтому ретраи/backoff внутри sender'а остаются на месте.
 *
 * При «FCM token invalid» токен автоматически удаляется через [PushTokenPurger].
 */
class FcmPushDelivery(
    private val sender: FcmNotificationSender,
    private val tokenPurger: PushTokenPurger? = null
) : PushNotificationDelivery {

    override val platform: String = "android"

    override fun supports(platform: String): Boolean = platform == "android"

    override suspend fun send(deviceTokens: List<String>, notification: Notification): Result<Unit> {
        if (deviceTokens.isEmpty()) return Result.success(Unit)

        val title = notification.title
        val body = notification.message
        val data = notification.extras
        val priority = if (notification.requiresImmediateAttention()) FcmPriority.HIGH else FcmPriority.NORMAL

        var delivered = 0
        var purged = 0
        var failed = 0
        var lastError: Throwable? = null

        for (token in deviceTokens) {
            val result = sender.send(tokens = listOf(token), title = title, body = body, data = data, priority = priority)
            when {
                result.isSuccess -> delivered++
                else -> {
                    val message = result.exceptionOrNull()?.message.orEmpty()
                    if (TOKEN_INVALID_MARKER.containsMatchIn(message)) {
                        // 404/410 — подписка отозвана; ретраи внутри sender уже отработали.
                        purged += tokenPurger?.purgeTokens(listOf(token)) ?: 0
                        logger.info { "FCM token invalid (404/410); purged token" }
                    } else {
                        failed++
                        lastError = result.exceptionOrNull()
                    }
                }
            }
        }

        if (purged > 0) logger.info { "FCM auto-purged $purged invalid token(s)" }

        // success — если доставлен хотя бы один токен; failure — если все невалидны/ошибка.
        return if (delivered > 0) {
            Result.success(Unit)
        } else {
            Result.failure(
                lastError
                    ?: IllegalStateException("No FCM token delivered (${deviceTokens.size} token(s) invalid or failed)")
            )
        }
    }

    companion object {
        /** Сообщение об отзыве токена из FcmNotificationSender: «FCM token invalid (404)». */
        private val TOKEN_INVALID_MARKER = Regex("FCM token invalid", option = RegexOption.IGNORE_CASE)
    }
}

/**
 * Фабрика для создания push-канала FCM из окружения (FCM_*).
 */
object FcmPushDeliveryFactory {
    fun createFromEnvironment(tokenPurger: PushTokenPurger? = null): FcmPushDelivery? {
        val config = FcmConfig.fromEnvironment() ?: return null
        logger.info { "FCM push channel enabled (project: ${config.projectId})" }
        return FcmPushDelivery(FcmNotificationSender(config), tokenPurger)
    }
}
