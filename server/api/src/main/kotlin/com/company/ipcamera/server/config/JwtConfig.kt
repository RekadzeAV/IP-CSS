package com.company.ipcamera.server.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

/**
 * Конфигурация JWT для аутентификации
 */
object JwtConfig {
    // Получаем секретный ключ из переменной окружения или используем дефолтный (только для разработки!)
    private val secret = System.getenv("JWT_SECRET") ?: "your-secret-key-change-in-production-min-32-chars"
    
    // Алгоритм подписи
    val algorithm = Algorithm.HMAC256(secret)
    
    // Срок жизни access token (15 минут)
    val accessTokenExpiration = 15 * 60 * 1000L // 15 минут в миллисекундах
    
    // Срок жизни refresh token (7 дней)
    val refreshTokenExpiration = 7 * 24 * 60 * 60 * 1000L // 7 дней в миллисекундах
    
    // Realm для JWT
    val realm = "ip-camera-system"
    
    /**
     * Создает JWT verifier для проверки токенов
     */
    fun createVerifier(): com.auth0.jwt.JWTVerifier {
        return JWT
            .require(algorithm)
            .withIssuer("ip-camera-server")
            .withAudience("ip-camera-client")
            .build()
    }
    
    /**
     * Генерирует access token для пользователя
     */
    fun generateAccessToken(userId: String, username: String, role: String, permissions: List<String> = emptyList()): String {
        val now = Date()
        val expiration = Date(now.time + accessTokenExpiration)
        
        return JWT.create()
            .withIssuer("ip-camera-server")
            .withAudience("ip-camera-client")
            .withSubject(userId)
            .withClaim("username", username)
            .withClaim("role", role)
            .withClaim("permissions", permissions)
            .withIssuedAt(now)
            .withExpiresAt(expiration)
            .sign(algorithm)
    }
    
    /** Время жизни временного токена для шага 2FA (5 минут) */
    val twoFaTempTokenExpiration = 5 * 60 * 1000L

    /**
     * Генерирует временный токен для завершения входа после проверки 2FA.
     * Используется только для POST /auth/2fa/verify.
     */
    fun generate2FaTempToken(userId: String): String {
        val now = Date()
        val expiration = Date(now.time + twoFaTempTokenExpiration)
        return JWT.create()
            .withIssuer("ip-camera-server")
            .withAudience("ip-camera-client")
            .withSubject(userId)
            .withClaim("type", "2fa_temp")
            .withClaim("pending_2fa", true)
            .withIssuedAt(now)
            .withExpiresAt(expiration)
            .sign(algorithm)
    }

    /**
     * Генерирует refresh token для пользователя
     */
    fun generateRefreshToken(userId: String): String {
        val now = Date()
        val expiration = Date(now.time + refreshTokenExpiration)
        
        return JWT.create()
            .withIssuer("ip-camera-server")
            .withAudience("ip-camera-client")
            .withSubject(userId)
            .withClaim("type", "refresh")
            .withIssuedAt(now)
            .withExpiresAt(expiration)
            .sign(algorithm)
    }
}

