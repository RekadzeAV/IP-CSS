package com.company.ipcamera.server.config

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.time.Duration

private val logger = KotlinLogging.logger {}

object RedisClusterConfig {

    private val host: String get() = System.getenv("REDIS_HOST") ?: "localhost"
    private val port: Int get() = System.getenv("REDIS_PORT")?.toIntOrNull() ?: 6379
    private val password: String? get() = System.getenv("REDIS_PASSWORD")?.takeIf { it.isNotBlank() }

    private var connection: StatefulRedisConnection<String, String>? = null

    suspend fun createConnection(): RedisClientWrapper = withContext(Dispatchers.IO) {
        logger.info { "Creating Redis connection to $host:$port" }
        val uri = RedisURI.Builder.redis(host, port)
            .apply { password?.let { withPassword(it) } }
            .withTimeout(Duration.ofSeconds(5))
            .build()
        val client = RedisClient.create(uri)
        connection = client.connect()
        RedisClientWrapper(connection!!.async())
    }

    fun close() {
        connection?.close()
    }
}

class RedisClientWrapper(private val async: io.lettuce.core.api.async.RedisAsyncCommands<String, String>) {

    suspend fun set(key: String, value: String): String? = withContext(Dispatchers.IO) {
        try { async.set(key, value).get() } catch (e: Exception) { null }
    }

    suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        try { async.get(key).get() } catch (e: Exception) { null }
    }

    suspend fun del(vararg keys: String): Long? = withContext(Dispatchers.IO) {
        try { async.del(*keys).get() } catch (e: Exception) { null }
    }

    suspend fun expire(key: String, seconds: Long): Boolean? = withContext(Dispatchers.IO) {
        try { async.expire(key, seconds).get() } catch (e: Exception) { null }
    }

    suspend fun rpush(key: String, vararg values: String): Long? = withContext(Dispatchers.IO) {
        try { async.rpush(key, *values).get() } catch (e: Exception) { null }
    }

    suspend fun lpop(key: String): String? = withContext(Dispatchers.IO) {
        try { async.lpop(key).get() } catch (e: Exception) { null }
    }

    suspend fun llen(key: String): Long? = withContext(Dispatchers.IO) {
        try { async.llen(key).get() } catch (e: Exception) { null }
    }

    suspend fun ping(): String? = withContext(Dispatchers.IO) {
        try { async.ping().get() } catch (e: Exception) { null }
    }
}
