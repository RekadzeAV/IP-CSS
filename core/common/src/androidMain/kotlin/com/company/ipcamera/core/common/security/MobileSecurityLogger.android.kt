package com.company.ipcamera.core.common.security

import mu.KotlinLogging

private val logger = KotlinLogging.logger("MOBILE_SECURITY")

/**
 * Android реализация логирования событий безопасности
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

        // Критические события дублируются на сервер (REMAINING_TASKS 2.6 / этап 5.5).
        // Приёмник регистрирует приложение (SecurityEventUploader) — транспорт
        // остаётся за пределами core:common, чтобы не тянуть okhttp/koin в KMP-модуль.
        if (event.severity == MobileSecurityEventSeverity.CRITICAL) {
            remoteSink?.invoke(event)
        }
    }

    companion object {
        /**
         * Приёмник критических событий (регистрируется приложением при старте).
         * Вызывается синхронно из [log] — реализация должна быть неблокирующей.
         */
        @Volatile
        var remoteSink: ((MobileSecurityEvent) -> Unit)? = null
    }
}
