package com.company.ipcamera.core.common.security

import mu.KotlinLogging

private val logger = KotlinLogging.logger("MOBILE_SECURITY")

/**
 * iOS реализация логирования событий безопасности
 */
actual class SecureMobileSecurityLogger : MobileSecurityLogger {

    actual override fun log(event: MobileSecurityEvent) {
        val message = buildString {
            append("[${event.type.name}]")
            if (event.details.isNotEmpty()) {
                append(" ${event.details.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
            }
        }

        when (event.severity) {
            MobileSecurityEventSeverity.INFO -> logger.info { message }
            MobileSecurityEventSeverity.WARNING -> logger.warn { message }
            MobileSecurityEventSeverity.ERROR -> logger.error { message }
            MobileSecurityEventSeverity.CRITICAL -> logger.error { "[CRITICAL] $message" }
        }

        // В production можно также отправлять критические события на сервер
        if (event.severity == MobileSecurityEventSeverity.CRITICAL) {
            // TODO: Отправить критическое событие на сервер для мониторинга
            // sendToServer(event)
        }
    }
}
