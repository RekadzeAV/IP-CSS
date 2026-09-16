package com.company.ipcamera.server.security

import com.company.ipcamera.server.config.JwtConfig
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Service для управления JWT Token Rotation
 * Реализует безопасную ротацию токенов с проверкой blacklist
 */
class TokenRotationService(
    val jwtService: JwtService,
    val tokenBlacklistService: TokenBlacklistService
) {
    /**
     * Ротирует access token - создаёт новый access token и добавляет старый в blacklist
     */
    suspend fun rotateAccessToken(
        oldToken: String,
        userId: String,
        username: String,
        role: String,
        permissions: List<String>
    ): String {
        logger.info { "Rotating access token for user: $userId" }

        try {
            // 1. Верифицируем старый токен
            jwtService.validateAccessToken(oldToken)

            // 2. Добавляем старый токен в blacklist
            tokenBlacklistService.blacklistToken(oldToken)
            logger.debug { "Old access token blacklisted" }

            // 3. Генерируем новый access token
            val newToken = jwtService.generateAccessToken(userId, username, role, permissions)
            logger.info { "New access token generated for user: $userId" }

            return newToken
        } catch (e: InvalidTokenException) {
            logger.warn { "Failed to rotate access token: ${e.message}" }
            throw e
        }
    }

    /**
     * Ротирует refresh token - создаёт новый refresh token и добавляет старый в blacklist
     */
    suspend fun rotateRefreshToken(
        oldToken: String,
        userId: String
    ): String {
        logger.info { "Rotating refresh token for user: $userId" }

        try {
            // 1. Верифицируем старый токен
            jwtService.validateRefreshToken(oldToken)

            // 2. Проверяем, что токен не в blacklist
            if (tokenBlacklistService.isTokenBlacklisted(oldToken)) {
                logger.warn { "Refresh token already blacklisted: $userId" }
                throw InvalidTokenException("Refresh token is blacklisted")
            }

            // 3. Добавляем старый токен в blacklist
            tokenBlacklistService.blacklistToken(oldToken)
            logger.debug { "Old refresh token blacklisted" }

            // 4. Генерируем новый refresh token
            val newToken = jwtService.generateRefreshToken(userId)
            logger.info { "New refresh token generated for user: $userId" }

            return newToken
        } catch (e: InvalidTokenException) {
            logger.warn { "Failed to rotate refresh token: ${e.message}" }
            throw e
        }
    }

    /**
     * Полная ротация токенов при refresh (создаёт новые access + refresh)
     */
    suspend fun fullTokenRotation(
        oldRefreshToken: String,
        userId: String,
        username: String,
        role: String,
        permissions: List<String>
    ): RefreshTokenResult {
        logger.info { "Performing full token rotation for user: $userId" }

        return try {
            // 1. Ротируем refresh token
            val newRefreshToken = rotateRefreshToken(oldRefreshToken, userId)

            // 2. Генерируем новый access token
            val newAccessToken = jwtService.generateAccessToken(userId, username, role, permissions)

            logger.info { "Full token rotation completed for user: $userId" }

            RefreshTokenResult(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                accessTokenExpiresIn = JwtConfig.accessTokenExpiration / 1000,
                refreshTokenExpiresIn = JwtConfig.refreshTokenExpiration / 1000
            )
        } catch (e: Exception) {
            logger.error(e) { "Full token rotation failed for user: $userId" }
            throw e
        }
    }

    /**
     * Проверяет, может ли токен быть ротирован
     */
    suspend fun canTokenBeRotated(token: String): Boolean {
        return try {
            val tokenInfo = jwtService.getTokenInfo(token)
            
            // Токен не должен быть истёк
            !tokenInfo.isExpired &&
            // Токен не должен быть в blacklist
            !tokenBlacklistService.isTokenBlacklisted(token)
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Результат ротации токенов
 */
data class RefreshTokenResult(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long,
    val refreshTokenExpiresIn: Long
)
