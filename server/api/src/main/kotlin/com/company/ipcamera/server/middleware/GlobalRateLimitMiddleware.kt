package com.company.ipcamera.server.middleware

import io.ktor.server.application.*
import io.ktor.server.request.path
import io.ktor.util.pipeline.PipelineContext
import mu.KotlinLogging
import org.koin.ktor.ext.getKoin

private val logger = KotlinLogging.logger {}

private fun PipelineContext<Unit, ApplicationCall>.globalRateLimiter(): RateLimitMiddleware =
    application.getKoin().get()

/**
 * Глобальный rate limit по IP для REST API (`generalConfig` + Redis), чтобы лимиты были
 * согласованы между инстансами. Отключение: `API_GLOBAL_RATE_LIMIT_ENABLED=false`.
 *
 * Исключены высокочастотные и служебные пути (HLS, health, WS, ingress ONVIF и т.д.).
 */
fun Application.configureGlobalApiRateLimit() {
    val raw = System.getenv("API_GLOBAL_RATE_LIMIT_ENABLED")?.trim()?.lowercase()
    if (raw == "false" || raw == "0" || raw == "off") {
        logger.info { "Global API rate limit disabled (API_GLOBAL_RATE_LIMIT_ENABLED)" }
        return
    }

    intercept(ApplicationCallPipeline.Call) {
        val call = context
        val path = call.request.path()
        if (!path.startsWith("/api/v1")) {
            proceed()
            return@intercept
        }
        if (shouldSkipGlobalRateLimit(path)) {
            proceed()
            return@intercept
        }

        val forwarded = call.request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
        val ip = forwarded?.takeIf { it.isNotBlank() } ?: call.request.local.remoteHost
        val identifier = "api_global:$ip"

        val rateLimiter = globalRateLimiter()
        if (!call.checkRateLimit(identifier, rateLimiter, rateLimiter.generalConfig)) {
            return@intercept
        }
        proceed()
    }

    logger.info { "Global API rate limit enabled (Redis, generalConfig per IP)" }
}

internal fun shouldSkipGlobalRateLimit(path: String): Boolean = when {
    path.startsWith("/api/v1/health") -> true
    path.startsWith("/api/v1/database/health") -> true
    path == "/api/v1/cluster/me" || path.startsWith("/api/v1/cluster/me/") -> true
    // Auth service/probe endpoints can spike during token rotation and WS reconnect.
    path == "/api/v1/auth/refresh" -> true
    path == "/api/v1/auth/ws-token" -> true
    path.startsWith("/api/v1/ws") -> true
    path.startsWith("/api/v1/screenshots/") -> true
    path.contains("/hls") -> true
    path.startsWith("/api/v1/onvif/") -> true
    else -> false
}
