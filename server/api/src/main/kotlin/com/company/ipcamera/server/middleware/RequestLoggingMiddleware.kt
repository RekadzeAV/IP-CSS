package com.company.ipcamera.server.middleware

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.path
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import mu.KotlinLogging
import org.slf4j.event.Level as LogLevel

private val logger = KotlinLogging.logger {}
private val noisyProtectedPaths = setOf(
    "/api/v1/auth/ws-token",
    "/api/v1/users/me",
    "/api/v1/cameras",
    "/api/v1/events",
    "/api/v1/events/statistics"
)

private data class RequestInfo(
    val method: String,
    val path: String,
    val fullPath: String,
    val clientIp: String,
    val userAgent: String
)

private data class UserInfo(
    val userId: String?,
    val username: String?
)

/**
 * Middleware для логирования HTTP запросов
 *
 * Логирует:
 * - Метод и путь запроса
 * - IP адрес клиента
 * - User-Agent
 * - Время выполнения запроса
 * - HTTP статус ответа
 * - Размер ответа (если доступен)
 */
fun Application.configureRequestLogging() {
    intercept(ApplicationCallPipeline.Call) {
        val call: io.ktor.server.application.ApplicationCall = context
        val startTime = System.currentTimeMillis()
        
        val requestInfo = extractRequestInfo(call)
        val userInfo = extractUserInfo(call)

        var statusCode: HttpStatusCode? = null
        var responseSize: Long? = null

        try {
            proceed()
            statusCode = call.response.status()
            responseSize = call.response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        } catch (e: Exception) {
            statusCode = call.response.status()
            throw e
        } finally {
            val duration = System.currentTimeMillis() - startTime
            val logMessage = buildLogMessage(requestInfo, userInfo, statusCode, duration, responseSize)
            val level = determineLogLevel(statusCode, requestInfo.path)

            logAt(level) { logMessage }
            logSlowRequest(requestInfo, duration)
        }
    }
}

private fun extractRequestInfo(call: io.ktor.server.application.ApplicationCall): RequestInfo {
    val path = call.request.path()
    val queryString = call.request.queryString()
    val fullPath = if (queryString.isNotEmpty()) "$path?$queryString" else path
    return RequestInfo(
        method = call.request.httpMethod.value,
        path = path,
        fullPath = fullPath,
        clientIp = call.request.local.remoteHost,
        userAgent = call.request.headers["User-Agent"] ?: "Unknown"
    )
}

private fun extractUserInfo(call: io.ktor.server.application.ApplicationCall): UserInfo {
    val jwtPrincipal = call.principal<JWTPrincipal>()
    return UserInfo(
        userId = jwtPrincipal?.payload?.subject,
        username = jwtPrincipal?.payload?.getClaim("username")?.asString()
    )
}

private fun buildLogMessage(
    requestInfo: RequestInfo,
    userInfo: UserInfo,
    statusCode: HttpStatusCode?,
    duration: Long,
    responseSize: Long?
): String = buildString {
    append("${requestInfo.method} ${requestInfo.fullPath}")
    append(" | IP: ${requestInfo.clientIp}")
    userInfo.userId?.let { append(" | User: $it") }
    userInfo.username?.let { append(" ($it)") }
    append(" | Status: ${statusCode?.value ?: "Unknown"}")
    append(" | Duration: ${duration}ms")
    responseSize?.let { append(" | Size: ${formatBytes(it)}") }
    append(" | UA: ${requestInfo.userAgent}")
}

private fun determineLogLevel(statusCode: HttpStatusCode?, path: String): LogLevel {
    val isNoisyAuthProbe = isNoisyAuthProbe(statusCode, path)
    if (isNoisyAuthProbe) return LogLevel.DEBUG
    
    return when (statusCode?.value) {
        in 200..299 -> LogLevel.DEBUG
        in 300..399 -> LogLevel.INFO
        in 400..499 -> LogLevel.WARN
        in 500..599 -> LogLevel.ERROR
        else -> LogLevel.INFO
    }
}

private fun logAt(level: LogLevel, messageBuilder: () -> String) {
    when (level) {
        LogLevel.DEBUG -> logger.debug { messageBuilder() }
        LogLevel.INFO -> logger.info { messageBuilder() }
        LogLevel.WARN -> logger.warn { messageBuilder() }
        LogLevel.ERROR -> logger.error { messageBuilder() }
        else -> logger.info { messageBuilder() }
    }
}

private fun isNoisyAuthProbe(statusCode: HttpStatusCode?, path: String): Boolean {
    return statusCode?.value == 401 && noisyProtectedPaths.any { path.startsWith(it) }
}

private fun logSlowRequest(requestInfo: RequestInfo, duration: Long) {
    if (duration > 1000 && !requestInfo.path.startsWith("/api/v1/ws")) {
        logger.warn { "Slow request detected: ${requestInfo.method} ${requestInfo.fullPath} took ${duration}ms" }
    }
}

/**
 * Форматирует байты в читаемый формат
 */

/**
 * Форматирует байты в читаемый формат
 */
private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}
