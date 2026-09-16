package com.company.ipcamera.server.middleware

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.server.request.queryString
import io.ktor.server.response.*
import io.ktor.server.routing.routing
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Middleware для принудительного перенаправления HTTP → HTTPS (порт 80 → 443 или заданный httpsPort).
 *
 * Использование:
 * ```kotlin
 * installHttpsRedirect(enabled = config.forceHttps, httpsPort = config.httpsPort)
 * ```
 */
class HttpsRedirectMiddleware {
    var enabled: Boolean = true
    /** Целевой порт HTTPS для редиректа (по умолчанию 443). */
    var httpsPort: Int = 443

    fun install(application: Application) {
        // Перехват в дереве Routing, а не в Application.call: иначе маршрут может ответить раньше редиректа.
        application.routing {
            intercept(ApplicationCallPipeline.Call) {
                if (!enabled) {
                    proceed()
                    return@intercept
                }
                val call = context
                val scheme = call.effectiveRequestScheme()

                // Проверяем, является ли запрос HTTP (не HTTPS)
                if (scheme.equals("http", ignoreCase = true)) {
                    val forwardedHost = call.request.headers["X-Forwarded-Host"]?.trim()
                    val host = if (!forwardedHost.isNullOrBlank()) {
                        forwardedHost.substringBefore(",").trim()
                    } else {
                        call.request.local.serverHost
                    }
                    val path = call.request.path()
                    val queryString = call.request.queryString()

                    // Формируем HTTPS URL с целевым портом из конфига (80 → 443 или httpsPort)
                    val httpsUrl = buildString {
                        append("https://")
                        append(host)
                        if (httpsPort != 443) {
                            append(":$httpsPort")
                        }
                        append(path)
                        if (queryString.isNotEmpty()) {
                            append("?$queryString")
                        }
                    }

                    logger.info { "Redirecting HTTP request to HTTPS: ${call.request.uri} → $httpsUrl" }

                    // Перенаправляем с кодом 301 (Moved Permanently)
                    call.respondRedirect(httpsUrl, permanent = true)
                    return@intercept
                }
                proceed()
            }
        }
    }
}

/**
 * Устанавливает редирект HTTP → HTTPS.
 *
 * @param enabled включить редирект (опция «только HTTPS» из конфига FORCE_HTTPS)
 * @param httpsPort целевой порт HTTPS (по умолчанию 443; из конфига HTTPS_PORT)
 */
fun Application.installHttpsRedirect(enabled: Boolean = true, httpsPort: Int = 443) {
    val middleware = HttpsRedirectMiddleware().apply {
        this.enabled = enabled
        this.httpsPort = httpsPort
    }
    middleware.install(this)
}
