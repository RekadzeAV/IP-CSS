package com.company.ipcamera.server.security

import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Service для управления blacklist JWT токенов
 * Использует Redis для хранения blacklisted токенов
 */
class TokenBlacklistService(
    private val redisCommands: RedisCoroutinesCommands<String, String>,
    private val blacklistExpirySeconds: Long = 86400 // 24 часа (максимальное время жизни access token)
) {
    
    private val localBlacklist = ConcurrentHashMap<String, Long>()
    
    /**
     * Добавить токен в blacklist
     */
    suspend fun blacklistToken(token: String) {
        val tokenId = hashToken(token)
        val now = System.currentTimeMillis()
        
        // Добавляем в локальный кэш
        localBlacklist[tokenId] = now
        
        // Добавляем в Redis
        try {
            redisCommands.setex(
                "token:blacklist:$tokenId",
                blacklistExpirySeconds,
                "revoked"
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to blacklist token in Redis: $tokenId" }
        }
        
        logger.debug { "Token blacklisted: $tokenId" }
    }
    
    /**
     * Blacklist все токены пользователя
     */
    suspend fun blacklistUserTokens(userId: String) {
        // Примечание: для полноты реализации нужно хранить mapping user -> tokens
        // На данный момент просто логируем
        logger.info { "Blacklisting all tokens for user: $userId (implementation pending)" }
    }
    
    /**
     * Проверить что токен в blacklist
     */
    suspend fun isTokenBlacklisted(token: String): Boolean {
        val tokenId = hashToken(token)
        
        // Проверить локальный кэш
        if (localBlacklist.containsKey(tokenId)) {
            return true
        }
        
        // Проверить Redis
        return try {
            val exists = redisCommands.exists("token:blacklist:$tokenId")
            exists == 1L
        } catch (e: Exception) {
            logger.error(e) { "Failed to check token blacklist: $tokenId" }
            false
        }
    }
    
    /**
     * Добавить токен к набору токенов пользователя
     */
    suspend fun addUserToken(userId: String, token: String) {
        val tokenId = hashToken(token)
        
        try {
            redisCommands.sadd("user:tokens:$userId", tokenId)
        } catch (e: Exception) {
            logger.error(e) { "Failed to add user token: $userId" }
        }
        
        logger.debug { "Added token to user: $userId" }
    }
    
    /**
     * Удалить токен из набора пользователя
     */
    suspend fun removeUserToken(userId: String, token: String) {
        val tokenId = hashToken(token)
        
        try {
            redisCommands.srem("user:tokens:$userId", tokenId)
        } catch (e: Exception) {
            logger.error(e) { "Failed to remove user token: $userId" }
        }
        
        logger.debug { "Removed token from user: $userId" }
    }
    
    /**
     * Очистить истёкшие токены из локального кэша
     */
    fun cleanupExpiredTokens() {
        val now = System.currentTimeMillis()
        localBlacklist.entries.removeIf { it.value < (now - (blacklistExpirySeconds * 1000)) }
        
        logger.debug { "Cleaned up expired tokens from local cache" }
    }
    
    /**
     * Хэшировать токен для хранения
     */
    private fun hashToken(token: String): String {
        return try {
            val bytes = token.toByteArray(Charsets.UTF_8)
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            digest.digest(bytes).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error hashing token" }
            token.hashCode().toString()
        }
    }
}
