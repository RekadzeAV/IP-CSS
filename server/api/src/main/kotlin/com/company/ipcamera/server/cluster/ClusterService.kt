package com.company.ipcamera.server.cluster

import com.company.ipcamera.server.config.ClusterConfig
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.json.JSONObject

private val logger = KotlinLogging.logger {}

/**
 * Сервис координации узлов кластера (4.2.1.2).
 * Регистрирует текущий узел в Redis и получает список остальных узлов.
 */
class ClusterService(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val config: ClusterConfig = ClusterConfig
) {
    private val keyPrefix = "cluster:nodes:"
    private val setKey = "cluster:node_ids"
    private val mutex = Mutex()
    private var heartbeatJob: Job? = null

    fun isEnabled(): Boolean = config.enabled

    fun getNodeId(): String = config.nodeId

    /** Текущий узел в формате для health/ready. */
    fun getCurrentNode(): ClusterNodeInfo = ClusterNodeInfo(
        nodeId = config.nodeId,
        url = config.nodeUrl,
        lastSeen = System.currentTimeMillis()
    )

    /**
     * Зарегистрировать узел в Redis (heartbeat). Вызывать периодически из startHeartbeat().
     */
    suspend fun register(): Result<Unit> = mutex.withLock {
        if (!config.enabled) return Result.success(Unit)
        return try {
            val key = keyPrefix + config.nodeId
            val value = JSONObject().apply {
                put("nodeId", config.nodeId)
                put("url", config.nodeUrl ?: "")
                put("lastSeen", System.currentTimeMillis())
            }.toString()
            redis.set(key, value)
            redis.expire(key, config.nodeTtlSec)
            redis.sadd(setKey, config.nodeId)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.warn(e) { "Cluster register failed" }
            Result.failure(e)
        }
    }

    /**
     * Список всех зарегистрированных узлов (по ключам cluster:nodes:*).
     */
    suspend fun getPeers(): List<ClusterNodeInfo> = mutex.withLock {
        if (!config.enabled) return emptyList()
        return try {
            val nodeIds: List<String> = redis.smembers(setKey).toList()
            val result = mutableListOf<ClusterNodeInfo>()
            for (nodeId in nodeIds) {
                val value = redis.get(keyPrefix + nodeId) ?: continue
                try {
                    val obj = JSONObject(value)
                    result.add(
                        ClusterNodeInfo(
                            nodeId = obj.optString("nodeId", nodeId),
                            url = obj.optString("url").takeIf { it.isNotBlank() },
                            lastSeen = obj.optLong("lastSeen", 0)
                        )
                    )
                } catch (_: Exception) {
                    // skip malformed entry
                }
            }
            result
        } catch (e: Exception) {
            logger.warn(e) { "Cluster getPeers failed" }
            emptyList()
        }
    }

    /**
     * Запустить фоновый heartbeat (регистрация раз в heartbeatIntervalSec).
     */
    fun startHeartbeat(scope: CoroutineScope) {
        if (!config.enabled) return
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                runCatching { register() }
                delay(config.heartbeatIntervalSec * 1000L)
            }
        }
        logger.info { "Cluster heartbeat started for node ${config.nodeId}" }
    }

    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}

data class ClusterNodeInfo(
    val nodeId: String,
    val url: String?,
    val lastSeen: Long
)
