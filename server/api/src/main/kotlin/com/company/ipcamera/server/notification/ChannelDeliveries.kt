package com.company.ipcamera.server.notification

import com.company.ipcamera.shared.domain.model.Notification
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Широковещательный канал Telegram: рассылка в сконфигурированные chatIds бота.
 */
class TelegramBroadcastChannel(
    private val sender: TelegramNotificationSender
) : BroadcastNotificationChannel {
    override val name: String = "telegram"

    override suspend fun broadcast(notification: Notification): Result<Unit> =
        sender.send(format(notification))
}

/**
 * Широковещательный канал Email: рассылка на список адресов получателей.
 *
 * @param recipients email-адреса (env: SMTP_RECIPIENTS, comma-separated)
 */
class EmailBroadcastChannel(
    private val sender: EmailNotificationSender,
    private val recipients: List<String>
) : BroadcastNotificationChannel {
    override val name: String = "email"

    override suspend fun broadcast(notification: Notification): Result<Unit> {
        if (recipients.isEmpty()) return Result.success(Unit)
        return sender.send(
            toEmails = recipients,
            subject = notification.title,
            bodyHtml = buildHtmlBody(notification),
            context = mapOf(
                "title" to notification.title,
                "message" to notification.message,
                "type" to notification.type.name,
                "priority" to notification.priority.name
            )
        )
    }

    private fun buildHtmlBody(n: Notification): String = buildString {
        append("<html><body>")
        append("<h2>").append(escape(n.title)).append("</h2>")
        append("<p>").append(escape(n.message)).append("</p>")
        append("<p><small>")
        append("Type: ").append(n.type.name).append(" &middot; ")
        append("Priority: <b>").append(n.priority.name).append("</b>")
        n.cameraId?.let { append(" &middot; Camera: ").append(escape(it)) }
        append(" &middot; ").append(java.time.Instant.ofEpochMilli(n.timestamp))
        append("</small></p>")
        append("</body></html>")
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

/**
 * Широковещательный канал SMS: отправка на defaultTo из конфига провайдера.
 */
class SmsBroadcastChannel(
    private val sender: SmsNotificationSender
) : BroadcastNotificationChannel {
    override val name: String = "sms"

    override suspend fun broadcast(notification: Notification): Result<Unit> =
        sender.send(text = format(notification), to = emptyList())
}

private fun format(n: Notification): String = buildString {
    append("[").append(n.priority.name).append("] ").append(n.title)
    if (n.message.isNotBlank() && n.message != n.title) {
        append("\n").append(n.message)
    }
    n.cameraId?.let { append("\nCamera: ").append(it) }
}

/**
 * Фабрика широковещательных каналов из окружения.
 * Возвращает только те каналы, чья конфигурация присутствует в env.
 */
object BroadcastChannelsFactory {
    fun createFromEnvironment(): List<BroadcastNotificationChannel> = buildList {
        // Telegram (TELEGRAM_BOT_TOKEN + TELEGRAM_CHAT_ID(S))
        TelegramConfig.fromEnvironment()?.let { config ->
            add(TelegramBroadcastChannel(TelegramNotificationSender(config)))
            logger.info { "Telegram broadcast channel enabled (${config.chatIds.size} chat(s))" }
        }
        // Email (SMTP_HOST + SMTP_RECIPIENTS)
        SmtpConfig.fromEnvironment()?.let { config ->
            val recipients = System.getenv("SMTP_RECIPIENTS")
                ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
            if (recipients.isNotEmpty()) {
                add(EmailBroadcastChannel(EmailNotificationSender(config), recipients))
                logger.info { "Email broadcast channel enabled (${recipients.size} recipient(s))" }
            } else {
                logger.warn { "SMTP configured but SMTP_RECIPIENTS empty; email channel skipped" }
            }
        }
        // SMS (SMS_ENDPOINT_URL + SMS_API_KEY, получатели — SMS_DEFAULT_TO)
        SmsConfig.fromEnvironment()?.let { config ->
            add(SmsBroadcastChannel(SmsNotificationSender(config)))
            logger.info { "SMS broadcast channel enabled (${config.defaultTo.size} default recipient(s))" }
        }
    }
}
