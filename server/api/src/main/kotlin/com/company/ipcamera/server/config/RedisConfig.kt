package com.company.ipcamera.server.config

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.api.coroutines.RedisCoroutinesCommandsImpl
import io.lettuce.core.api.reactive.RedisReactiveCommands
import mu.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Proxy

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация и управление подключением к Redis
 */
object RedisConfig {
    private var redisClient: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null
    private var fallbackModeEnabled: Boolean = false
    private val fallbackCommands: RedisCoroutinesCommands<String, String> by lazy {
        @Suppress("UNCHECKED_CAST")
        Proxy.newProxyInstance(
            RedisCoroutinesCommands::class.java.classLoader,
            arrayOf(RedisCoroutinesCommands::class.java)
        ) { _, method, _ ->
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Long.TYPE -> 0L
                java.lang.Integer.TYPE -> 0
                java.lang.Double.TYPE -> 0.0
                java.lang.Float.TYPE -> 0f
                else -> null
            }
        } as RedisCoroutinesCommands<String, String>
    }

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

        return try {
            redisClient = RedisClient.create(redisUri)
            connection = redisClient!!.connect()
            fallbackModeEnabled = false

            logger.info { "Redis connection established: $redisHost:$redisPort (database: $redisDatabase)" }

            val reactive: RedisReactiveCommands<String, String> = connection!!.reactive()
            RedisCoroutinesCommandsImpl(reactive)
        } catch (e: Exception) {
            fallbackModeEnabled = true
            logger.warn(e) {
                "Redis is unavailable ($redisHost:$redisPort). " +
                    "Switching to no-op Redis commands for local smoke/development mode."
            }
            fallbackCommands
        }
    }

    /**
     * Получает команды Redis для корутин
     */
    fun getCommands(): RedisCoroutinesCommands<String, String> {
        if (fallbackModeEnabled) return fallbackCommands
        if (connection == null || !connection!!.isOpen) {
            return initialize()
        }
        val reactive: RedisReactiveCommands<String, String> = connection!!.reactive()
        return RedisCoroutinesCommandsImpl(reactive)
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
        if (fallbackModeEnabled) return@withContext false
        return@withContext try {
            val commands = getCommands()
            val result = commands.ping()
            result == "PONG"
        } catch (e: Exception) {
            logger.error(e) { "Redis ping failed" }
            false
        }
    }

    fun isFallbackModeEnabled(): Boolean = fallbackModeEnabled
}


