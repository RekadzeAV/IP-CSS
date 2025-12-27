package com.company.ipcamera.server.middleware

import io.ktor.http.HttpHeaders
import io.ktor.server.application.*
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
    install(DefaultHeaders) {
        header(HttpHeaders.Server, "IP-CSS")
    }

    intercept(ApplicationCallPipeline.Call) {
        call.response.headers.append(
            HttpHeaders.ContentSecurityPolicy,
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

        call.response.headers.append(HttpHeaders.XFrameOptions, "SAMEORIGIN")
        call.response.headers.append(HttpHeaders.XContentTypeOptions, "nosniff")

        // HSTS (только для HTTPS)
        val scheme = call.request.origin.scheme
        if (scheme == "https") {
            call.response.headers.append(
                HttpHeaders.StrictTransportSecurity,
                "max-age=31536000; includeSubDomains; preload"
            )
        }

        call.response.headers.append(HttpHeaders.ReferrerPolicy, "strict-origin-when-cross-origin")
        call.response.headers.append(
            "Permissions-Policy",
            "geolocation=(), microphone=(), camera=()"
        )

        // X-XSS-Protection (устаревший, но для совместимости)
        call.response.headers.append("X-XSS-Protection", "1; mode=block")
    }

    logger.info { "Security headers middleware configured" }
}

