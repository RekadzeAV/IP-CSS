package com.company.ipcamera.server.security

import com.company.ipcamera.server.config.EnterpriseAuthConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Хранение state для OAuth2 (CSRF). Redis с TTL.
 */
class OAuth2StateStore(
    private val redis: io.lettuce.core.api.coroutines.RedisCoroutinesCommands<String, String>,
    private val stateTtlSec: Long = EnterpriseAuthConfig.oauth2StateTtlSec
) {
    private val keyPrefix = "oauth2_state:"

    /** @param returnTo опциональный URL фронтенда для редиректа после успеха */
    suspend fun createState(returnTo: String? = null): String = withContext(Dispatchers.IO) {
        val state = UUID.randomUUID().toString().replace("-", "")
        val key = keyPrefix + state
        val value = returnTo ?: ""
        redis.set(key, value)
        redis.expire(key, stateTtlSec)
        state
    }

    /** Возвращает сохранённый returnTo и удаляет state (одноразовый). */
    suspend fun consumeState(state: String): String? = withContext(Dispatchers.IO) {
        val key = keyPrefix + state
        val value = redis.get(key)
        redis.del(key)
        value
    }
}
