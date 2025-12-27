package com.company.ipcamera.server.config

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.api.coroutines.RedisCoroutinesCommandsImpl
import mu.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация и управление подключением к Redis
 */
object RedisConfig {
    private var redisClient: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null

    /**
     * Инициализирует подключение к Redis
     */
    fun initialize(): RedisCoroutinesCommands<String, String> {
        val redisHost = System.getenv("REDIS_HOST") ?: "localhost"
        val redisPort = System.getenv("REDIS_PORT")?.toIntOrNull() ?: 6379
        val redisPassword = System.getenv("REDIS_PASSWORD")
        val redisDatabase = System.getenv("REDIS_DATABASE")?.toIntOrNull() ?: 0

        val redisUri = RedisURI.Builder
            .redis(redisHost, redisPort)
            .apply {
                if (redisPassword != null) {
                    withPassword(redisPassword.toCharArray())
                }
                withDatabase(redisDatabase)
            }
            .build()

        redisClient = RedisClient.create(redisUri)
        connection = redisClient!!.connect()

        logger.info { "Redis connection established: $redisHost:$redisPort (database: $redisDatabase)" }

        return RedisCoroutinesCommandsImpl(connection!!.async(), Dispatchers.IO)
    }

    /**
     * Получает команды Redis для корутин
     */
    fun getCommands(): RedisCoroutinesCommands<String, String> {
        if (connection == null || !connection!!.isOpen) {
            return initialize()
        }
        return RedisCoroutinesCommandsImpl(connection!!.async(), Dispatchers.IO)
    }

    /**
     * Закрывает подключение к Redis
     */
    suspend fun close() = withContext(Dispatchers.IO) {
        try {
            connection?.close()
            redisClient?.shutdown()
            logger.info { "Redis connection closed" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing Redis connection" }
        }
    }

    /**
     * Проверяет доступность Redis
     */
    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val commands = getCommands()
            val result = commands.ping()
            result == "PONG"
        } catch (e: Exception) {
            logger.error(e) { "Redis ping failed" }
            false
        }
    }
}

