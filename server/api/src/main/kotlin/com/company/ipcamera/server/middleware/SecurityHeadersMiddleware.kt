package com.company.ipcamera.server.middleware

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.path
import io.ktor.server.request.host
import io.ktor.server.request.queryString
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.response.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Middleware для добавления Security Headers
 *
 * Добавляет следующие заголовки безопасности:
 * - Content-Security-Policy
 * - X-Frame-Options
 * - X-Content-Type-Options
 * - Strict-Transport-Security
 * - Referrer-Policy
 * - Permissions-Policy
 */
fun Application.configureSecurityHeaders() {
    install(io.ktor.server.plugins.defaultheaders.DefaultHeaders) {
        header("Server", "IP-CSS")
    }

    // Принудительный HTTPS redirect
    val forceHttps = System.getenv("FORCE_HTTPS")?.toBoolean()
        ?: (System.getenv("NODE_ENV") == "production")
    val isProduction = System.getenv("NODE_ENV") == "production" || forceHttps

    intercept(ApplicationCallPipeline.Call) {
        val call: io.ktor.server.application.ApplicationCall = context
        // Принудительный HTTPS redirect
        if (forceHttps) {
            val scheme = call.effectiveRequestScheme()

            if (scheme.equals("http", ignoreCase = true)) {
                val host = call.request.headers["X-Forwarded-Host"]
                    ?: call.request.local.localHost
                val path = call.request.path()
                val query = call.request.queryString()

                val httpsUrl = buildString {
                    append("https://")
                    append(host)
                    val port = call.request.local.serverPort
                    if (!host.contains(":") && port != 80 && port != 443) {
                        append(":")
                        append(port)
                    }
                    append(path)
                    if (query.isNotEmpty()) {
                        append("?")
                        append(query)
                    }
                }

                logger.info { "Redirecting HTTP to HTTPS: $httpsUrl" }
                call.respondRedirect(httpsUrl, permanent = true)
                return@intercept
            }
        }

        // Для preflight/ранне-завершённых ответов заголовки уже могут быть зафиксированы.
        if (call.response.isCommitted) {
            return@intercept
        }

        call.response.headers.append(
            "Content-Security-Policy",
            "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' data:; " +
                "connect-src 'self' ws: wss:; " +
                "frame-ancestors 'self'; " +
                "base-uri 'self'; " +
                "form-action 'self'"
        )

        // X-Frame-Options - DENY для максимальной защиты от clickjacking
        call.response.headers.append("X-Frame-Options", "DENY")
        call.response.headers.append("X-Content-Type-Options", "nosniff")

        // HSTS задаётся в [installHsts] (Application.module), с учётом X-Forwarded-Proto — см. HstsMiddleware

        call.response.headers.append("Referrer-Policy", "strict-origin-when-cross-origin")
        call.response.headers.append(
            "Permissions-Policy",
            "geolocation=(), microphone=(), camera=()"
        )

        // X-XSS-Protection (устаревший, но для совместимости)
        call.response.headers.append("X-XSS-Protection", "1; mode=block")

        // Дополнительные security headers для улучшенной защиты
        // Cross-Origin-Embedder-Policy (COEP) - изоляция контекста выполнения
        if (isProduction) {
            call.response.headers.append("Cross-Origin-Embedder-Policy", "require-corp")
        }

        // Cross-Origin-Opener-Policy (COOP) - изоляция окон
        call.response.headers.append("Cross-Origin-Opener-Policy", "same-origin")

        // Cross-Origin-Resource-Policy (CORP) - контроль доступа к ресурсам
        call.response.headers.append("Cross-Origin-Resource-Policy", "same-origin")

        // DNS Prefetch Control
        call.response.headers.append("X-DNS-Prefetch-Control", "on")
    }

    logger.info { "Security headers middleware configured" }
}

