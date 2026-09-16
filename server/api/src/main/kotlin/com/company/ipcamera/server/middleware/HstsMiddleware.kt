package com.company.ipcamera.server.middleware

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Middleware для установки HSTS (HTTP Strict Transport Security) headers
 *
 * HSTS заставляет браузеры использовать только HTTPS для данного домена.
 *
 * Использование:
 * ```kotlin
 * install(HstsMiddleware) {
 *     maxAge = 31536000 // 1 год в секундах
 *     includeSubDomains = true
 *     preload = false
 * }
 * ```
 */
class HstsMiddleware {
    /**
     * Максимальное время действия HSTS в секундах (по умолчанию 1 год)
     */
    var maxAge: Long = 31536000

    /**
     * Включать ли поддомены
     */
    var includeSubDomains: Boolean = true

    /**
     * Включить ли preload (для добавления в HSTS preload list)
     */
    var preload: Boolean = false

    /**
     * Включить ли middleware (можно отключить для development)
     */
    var enabled: Boolean = true

    fun install(application: Application) {
        application.intercept(ApplicationCallPipeline.Call) {
            if (!enabled) {
                proceed()
                return@intercept
            }
            val call: ApplicationCall = context
            val scheme = call.effectiveRequestScheme()

            // Устанавливаем HSTS только для HTTPS (в т.ч. за TLS-терминацией по X-Forwarded-Proto)
            if (scheme.equals("https", ignoreCase = true)) {
                val hstsValue = buildString {
                    append("max-age=$maxAge")
                    if (includeSubDomains) {
                        append("; includeSubDomains")
                    }
                    if (preload) {
                        append("; preload")
                    }
                }

                call.response.headers.append(
                    HttpHeaders.StrictTransportSecurity,
                    hstsValue
                )

                logger.debug { "HSTS header set: $hstsValue" }
            }
            proceed()
        }
    }
}

/**
 * Функция для установки HstsMiddleware
 */
fun Application.installHsts(
    maxAge: Long = 31536000,
    includeSubDomains: Boolean = true,
    preload: Boolean = false,
    enabled: Boolean = true
) {
    val middleware = HstsMiddleware().apply {
        this.maxAge = maxAge
        this.includeSubDomains = includeSubDomains
        this.preload = preload
        this.enabled = enabled
    }
    middleware.install(this)
}
