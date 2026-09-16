package com.company.ipcamera.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.company.ipcamera.server.config.JwtConfig
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Service для управления JWT токенами
 * Предоставляет функциональность для generation, verification и rotation токенов
 */
class JwtService(
    private val secret: String = System.getenv("JWT_SECRET") ?: "your-secret-key-change-in-production-min-32-chars",
    private val issuer: String = "ip-camera-server",
    private val audience: String = "ip-camera-client"
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(secret)
    private val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    /**
     * Генерирует access token для пользователя
     */
    fun generateAccessToken(
        userId: String,
        username: String,
        role: String,
        permissions: List<String> = emptyList(),
        expirationMillis: Long = JwtConfig.accessTokenExpiration
    ): String {
        val now = Date()
        val expiration = Date(now.time + expirationMillis)

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId)
            .withClaim("username", username)
            .withClaim("role", role)
            .withClaim("permissions", permissions)
            .withClaim("type", "access")
            .withIssuedAt(now)
            .withExpiresAt(expiration)
            .sign(algorithm)
    }

    /**
     * Генерирует refresh token для пользователя
     */
    fun generateRefreshToken(
        userId: String,
        expirationMillis: Long = JwtConfig.refreshTokenExpiration
    ): String {
        val now = Date()
        val expiration = Date(now.time + expirationMillis)

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId)
            .withClaim("type", "refresh")
            .withIssuedAt(now)
            .withExpiresAt(expiration)
            .sign(algorithm)
    }

    /**
     * Генерирует временный токен для 2FA
     */
    fun generate2FaTempToken(
        userId: String,
        expirationMillis: Long = JwtConfig.twoFaTempTokenExpiration
    ): String {
        val now = Date()
        val expiration = Date(now.time + expirationMillis)

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId)
            .withClaim("type", "2fa_temp")
            .withClaim("pending_2fa", true)
            .withIssuedAt(now)
            .withExpiresAt(expiration)
            .sign(algorithm)
    }

    /**
     * Верифицирует JWT токен
     */
    fun verifyToken(token: String): DecodedJWT {
        return try {
            verifier.verify(token)
        } catch (e: Exception) {
            logger.warn { "Invalid token: ${e.message}" }
            throw InvalidTokenException("Invalid token", e)
        }
    }

    /**
     * Проверяет, что токен является access token
     */
    fun validateAccessToken(token: String): DecodedJWT {
        val decodedJWT = verifyToken(token)
        val tokenType = decodedJWT.getClaim("type").asString()

        if (tokenType != "access") {
            throw InvalidTokenException("Token is not an access token")
        }

        return decodedJWT
    }

    /**
     * Проверяет, что токен является refresh token
     */
    fun validateRefreshToken(token: String): DecodedJWT {
        val decodedJWT = verifyToken(token)
        val tokenType = decodedJWT.getClaim("type").asString()

        if (tokenType != "refresh") {
            throw InvalidTokenException("Token is not a refresh token")
        }

        return decodedJWT
    }

    /**
     * Проверяет, что токен является 2FA temp token
     */
    fun validate2FaTempToken(token: String): DecodedJWT {
        val decodedJWT = verifyToken(token)
        val tokenType = decodedJWT.getClaim("type").asString()

        if (tokenType != "2fa_temp") {
            throw InvalidTokenException("Token is not a 2FA temp token")
        }

        return decodedJWT
    }

    /**
     * Извлекает userId из токена
     */
    fun extractUserId(token: String): String {
        val decodedJWT = verifyToken(token)
        return decodedJWT.subject
    }

    /**
     * Извлекает username из токена
     */
    fun extractUsername(token: String): String {
        val decodedJWT = verifyToken(token)
        return decodedJWT.getClaim("username").asString()
    }

    /**
     * Извлекает роль из токена
     */
    fun extractRole(token: String): String {
        val decodedJWT = verifyToken(token)
        return decodedJWT.getClaim("role").asString()
    }

    /**
     * Извлекает permissions из токена
     */
    fun extractPermissions(token: String): List<String> {
        val decodedJWT = verifyToken(token)
        return decodedJWT.getClaim("permissions").asList(String::class.java) ?: emptyList()
    }

    /**
     * Проверяет, истёк ли токен
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            val decodedJWT = verifyToken(token)
            decodedJWT.expiresAt?.before(Date()) ?: true
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Получает информацию о токене
     */
    fun getTokenInfo(token: String): TokenInfo {
        val decodedJWT = verifyToken(token)
        return TokenInfo(
            userId = decodedJWT.subject,
            username = decodedJWT.getClaim("username").asString(),
            role = decodedJWT.getClaim("role").asString(),
            permissions = decodedJWT.getClaim("permissions").asList(String::class.java) ?: emptyList(),
            tokenType = decodedJWT.getClaim("type").asString(),
            issuedAt = decodedJWT.issuedAt,
            expiresAt = decodedJWT.expiresAt,
            isExpired = isTokenExpired(token)
        )
    }
}

/**
 * Информация о токене
 */
data class TokenInfo(
    val userId: String,
    val username: String,
    val role: String,
    val permissions: List<String>,
    val tokenType: String?,
    val issuedAt: Date?,
    val expiresAt: Date?,
    val isExpired: Boolean
)

/**
 * Исключение для невалидных токенов
 */
class InvalidTokenException(message: String, cause: Throwable? = null) : Exception(message, cause)
