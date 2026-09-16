package com.company.ipcamera.server.notification

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.util.Properties

private val logger = KotlinLogging.logger {}

/**
 * Отправка email-уведомлений по SMTP (B.1).
 * Шаблон: тема = title, тело = message (поддержка плейсхолдеров {{title}}, {{message}}, {{type}}, {{priority}}).
 * Повторные попытки при ошибке (до maxRetries раз с задержкой).
 */
class EmailNotificationSender(
    private val config: SmtpConfig,
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 2000L
) {
    private val session: Session by lazy {
        val props = Properties().apply {
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port.toString())
            put("mail.smtp.auth", (config.username != null).toString())
            put("mail.smtp.starttls.enable", config.useTls.toString())
        }
        val auth = if (config.username != null && config.password != null) {
            object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(config.username, config.password)
            }
        } else null
        Session.getInstance(props, auth)
    }

    /**
     * Отправить email на указанные адреса.
     * @param toEmails список адресов получателей
     * @param subject тема (шаблон: подставляются {{title}}, {{type}}, {{priority}})
     * @param bodyHtml тело письма HTML (шаблон: {{message}}, {{title}}, {{type}}, {{priority}}, {{cameraId}}, {{eventId}})
     */
    suspend fun send(
        toEmails: List<String>,
        subject: String,
        bodyHtml: String,
        context: Map<String, String> = emptyMap()
    ): Result<Unit> {
        if (toEmails.isEmpty()) return Result.success(Unit)
        if (!config.enabled) {
            logger.debug { "SMTP disabled, skipping email to ${toEmails.size} recipient(s)" }
            return Result.success(Unit)
        }

        val filledSubject = fillTemplate(subject, context)
        val filledBody = fillTemplate(bodyHtml, context)

        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            val result = doSend(toEmails, filledSubject, filledBody)
            result.fold(
                onSuccess = { return Result.success(Unit) },
                onFailure = { e ->
                    lastException = e as? Exception ?: Exception(e)
                    logger.warn(lastException) { "SMTP send attempt ${attempt + 1}/$maxRetries failed" }
                    if (attempt < maxRetries - 1) {
                        kotlinx.coroutines.delay(retryDelayMs)
                    }
                }
            )
        }
        return Result.failure(lastException ?: RuntimeException("SMTP send failed after $maxRetries attempts"))
    }

    private fun fillTemplate(template: String, context: Map<String, String>): String {
        var result = template
        context.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
        }
        return result
    }

    private suspend fun doSend(toEmails: List<String>, subject: String, bodyHtml: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(config.fromAddress, config.fromName ?: "IP Camera"))
                    setRecipients(Message.RecipientType.TO, toEmails.map { InternetAddress(it) }.toTypedArray())
                    setSubject(subject, "UTF-8")
                    setContent(bodyHtml, "text/html; charset=UTF-8")
                }
                Transport.send(message)
                logger.debug { "Email sent to ${toEmails.size} recipient(s): $subject" }
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Failed to send email via ${config.host}:${config.port}" }
                Result.failure(e)
            }
        }
}
