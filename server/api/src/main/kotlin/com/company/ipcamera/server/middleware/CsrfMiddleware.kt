package com.company.ipcamera.server.middleware

import com.company.ipcamera.server.security.SecurityLogger
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.*
import mu.KotlinLogging
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * CSRF Token Manager
 * Управляет генерацией и валидацией CSRF токенов
 */
object CsrfTokenManager {
    // Хранилище CSRF токенов (в production рекомендуется использовать Redis)
    private val tokens = ConcurrentHashMap<String, CsrfTokenData>()

    // Время жизни токена (1 час)
    private const val TOKEN_EXPIRATION_MS = 60 * 60 * 1000L

    data class CsrfTokenData(
        val token: String,
        val createdAt: Long,
        val expiresAt: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    }

    /**
     * Генерирует новый CSRF токен
     */
    fun generateToken(sessionId: String? = null): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        val now = System.currentTimeMillis()
        val expiresAt = now + TOKEN_EXPIRATION_MS

        val key = sessionId ?: "anonymous"
        tokens[key] = CsrfTokenData(token, now, expiresAt)

        // Очистка истекших токенов
        cleanupExpiredTokens()

        logger.debug { "Generated CSRF token for session: $key" }
        return token
    }

    /**
     * Валидирует CSRF токен
     */
    fun validateToken(token: String, sessionId: String? = null): Boolean {
        val key = sessionId ?: "anonymous"
        val tokenData = tokens[key]

        if (tokenData == null) {
            logger.warn { "CSRF token not found for session: $key" }
            return false
        }

        if (tokenData.isExpired()) {
            logger.warn { "CSRF token expired for session: $key" }
            tokens.remove(key)
            return false
        }

        if (tokenData.token != token) {
            logger.warn { "CSRF token mismatch for session: $key" }
            return false
        }

        return true
    }

    /**
     * Удаляет токен (используется при logout)
     */
    fun revokeToken(sessionId: String?) {
        val key = sessionId ?: "anonymous"
        tokens.remove(key)
        logger.debug { "Revoked CSRF token for session: $key" }
    }

    /**
     * Очистка истекших токенов
     */
    private fun cleanupExpiredTokens() {
        val now = System.currentTimeMillis()
        tokens.entries.removeIf { it.value.isExpired() }
    }
}

/**
 * Middleware для CSRF защиты
 *
 * Генерирует CSRF токены и валидирует их для state-changing операций
 */
fun Application.configureCsrfProtection() {
    intercept(ApplicationCallPipeline.Call) {
        val call: io.ktor.server.application.ApplicationCall = context
        val method = call.request.httpMethod
        val path = call.request.path()

        // Пропускаем GET, HEAD, OPTIONS запросы (не изменяют состояние)
        if (method == HttpMethod.Get || method == HttpMethod.Head || method == HttpMethod.Options) {
            // Генерируем и устанавливаем CSRF токен в cookie для GET запросов
            val existingToken = call.request.cookies["csrf_token"]
            if (existingToken == null) {
                val sessionId = (call.request.cookies["session_id"] as? io.ktor.http.Cookie)?.value
                val csrfToken = CsrfTokenManager.generateToken(sessionId)

                val isProduction = System.getenv("ENVIRONMENT") == "production"
                call.response.cookies.append(
                    name = "csrf_token",
                    value = csrfToken,
                    maxAge = 3600L,
                    path = "/",
                    secure = isProduction,
                    httpOnly = false,
                    extensions = mapOf("SameSite" to "Strict")
                )
            }
            proceed()
            return@intercept
        }

        // Для state-changing операций (POST, PUT, DELETE, PATCH) проверяем CSRF токен
        if (method == HttpMethod.Post || method == HttpMethod.Put ||
            method == HttpMethod.Delete || method == HttpMethod.Patch) {

            // Исключаем некоторые endpoints из проверки CSRF
            val excludedPaths = listOf(
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/logout"
            )

            if (excludedPaths.any { path.startsWith(it) }) {
                proceed()
                return@intercept
            }

            // Получаем CSRF токен из заголовка
            val csrfToken = call.request.headers["X-CSRF-Token"]
            val cookieToken: String? = (call.request.cookies["csrf_token"] as? io.ktor.http.Cookie)?.value
            val ipAddress = call.request.local.remoteHost
            val endpoint = call.request.path()

            if (csrfToken.isNullOrEmpty()) {
                logger.warn { "CSRF token missing in request: $endpoint from $ipAddress" }
                SecurityLogger.logCsrfAttack(ipAddress, endpoint, "CSRF token missing")
                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "success" to false,
                        "message" to "CSRF token is required",
                        "error" to "CSRF_TOKEN_MISSING"
                    )
                )
                return@intercept
            }

            // Валидируем CSRF токен
            val sessionId = (call.request.cookies["session_id"] as? io.ktor.http.Cookie)?.value
            if (!CsrfTokenManager.validateToken(csrfToken, sessionId)) {
                // Также проверяем токен из cookie (для дополнительной защиты)
                if (cookieToken == null || !CsrfTokenManager.validateToken(cookieToken, sessionId)) {
                    logger.warn { "Invalid CSRF token in request: $endpoint from $ipAddress" }
                    SecurityLogger.logCsrfAttack(ipAddress, endpoint, "Invalid CSRF token")
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf(
                            "success" to false,
                            "message" to "Invalid CSRF token",
                            "error" to "CSRF_TOKEN_INVALID"
                        )
                    )
                    return@intercept
                }
            }

            logger.debug { "CSRF token validated successfully for: ${call.request.path()}" }
        }

        proceed()
    }

    logger.info { "CSRF protection middleware configured" }
}
