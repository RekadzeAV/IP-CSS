package com.company.ipcamera.server.cluster

import com.company.ipcamera.server.config.ClusterConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Сервис health check'ов для кластера (4.2.2).
 *
 * Проверяет доступность узлов кластера через HTTP health endpoint.
 * Интегрируется с NGINX/HAProxy для автоматического исключения unhealthy узлов.
 */
class HealthCheckService(
    private val config: ClusterConfig = ClusterConfig
) {
    private val httpClient = HttpClient(CIO) {
        expectSuccess = false
    }

    private val mutex = Mutex()
    private var checkJob: Job? = null
    private val nodeHealth = mutableMapOf<String, NodeHealth>()

    data class NodeHealth(
        val url: String,
        var isHealthy: Boolean = true,
        var lastCheckTime: Long = 0,
        var lastSuccessTime: Long = 0,
        var consecutiveFailures: Int = 0,
        var responseTimeMs: Long = 0,
        var errorMessage: String? = null
    )

    data class HealthCheckResult(
        val isHealthy: Boolean,
        val nodeCount: Int,
        val unhealthyNodes: List<String>,
        val averageResponseTimeMs: Long
    )

    /**
     * Проверить здоровье одного узла.
     */
    suspend fun checkNode(url: String): NodeHealth = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val healthUrl = url.trimEnd('/') + config.healthCheckUrl

        return@withContext try {
            val response = withTimeout(config.healthCheckTimeoutSec * 1000L) {
                httpClient.get(healthUrl)
            }

            val responseTime = System.currentTimeMillis() - startTime
            val isHealthy = response.status.value in 200..399

            NodeHealth(
                url = url,
                isHealthy = isHealthy,
                lastCheckTime = System.currentTimeMillis(),
                lastSuccessTime = if (isHealthy) System.currentTimeMillis() else 0,
                consecutiveFailures = if (isHealthy) 0 else 1,
                responseTimeMs = responseTime,
                errorMessage = if (isHealthy) null else "HTTP ${response.status.value}"
            ).also {
                logger.debug { "Health check for $url: ${if (it.isHealthy) "OK" else "FAIL"} (${responseTime}ms)" }
            }
        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis() - startTime
            NodeHealth(
                url = url,
                isHealthy = false,
                lastCheckTime = System.currentTimeMillis(),
                consecutiveFailures = 1,
                responseTimeMs = responseTime,
                errorMessage = e.message
            ).also {
                logger.warn { "Health check failed for $url: ${e.message}" }
            }
        }
    }

    /**
     * Проверить все узлы кластера.
     */
    suspend fun checkAllNodes(nodeUrls: List<String>): List<NodeHealth> = coroutineScope {
        nodeUrls.map { url ->
            async { checkNode(url) }
        }.awaitAll()
    }

    /**
     * Получить результат health check.
     */
    suspend fun getHealthResult(nodeUrls: List<String>): HealthCheckResult = mutex.withLock {
        val results = checkAllNodes(nodeUrls)

        // Обновляем состояние
        results.forEach { health ->
            val existing = nodeHealth[health.url]
            if (existing != null) {
                existing.isHealthy = health.isHealthy
                existing.lastCheckTime = health.lastCheckTime
                existing.responseTimeMs = health.responseTimeMs
                existing.errorMessage = health.errorMessage

                if (health.isHealthy) {
                    existing.consecutiveFailures = 0
                    existing.lastSuccessTime = health.lastCheckTime
                } else {
                    existing.consecutiveFailures++
                }
            } else {
                nodeHealth[health.url] = health
            }
        }

        val healthyNodes = results.filter { it.isHealthy }
        val unhealthyNodes = results.filter { !it.isHealthy }

        HealthCheckResult(
            isHealthy = unhealthyNodes.size < nodeUrls.size, // Допускаем частичную недоступность
            nodeCount = nodeUrls.size,
            unhealthyNodes = unhealthyNodes.map { it.url },
            averageResponseTimeMs = if (healthyNodes.isNotEmpty())
                healthyNodes.map { it.responseTimeMs }.average().toLong() else 0
        )
    }

    /**
     * Запустить фоновые health check'и.
     */
    fun startPeriodicChecks(scope: CoroutineScope, nodeUrls: List<String>) {
        checkJob?.cancel()
        checkJob = scope.launch {
            while (isActive) {
                runCatching {
                    val result = getHealthResult(nodeUrls)
                    if (!result.isHealthy) {
                        logger.warn {
                            "Cluster health degraded: ${result.unhealthyNodes.size}/${result.nodeCount} nodes unhealthy. " +
                                "Average response: ${result.averageResponseTimeMs}ms"
                        }
                    }
                }
                delay(config.healthCheckIntervalSec * 1000L)
            }
        }
        logger.info { "Periodic health checks started (interval: ${config.healthCheckIntervalSec}s)" }
    }

    fun stopPeriodicChecks() {
        checkJob?.cancel()
        checkJob = null
    }

    /**
     * Получить устойчивый счётчик неудач для узла.
     */
    private fun getConsecutiveFailures(url: String): Int {
        return nodeHealth[url]?.consecutiveFailures ?: 0
    }

    /**
     * Очистить историю health check'ов.
     */
    fun reset() {
        nodeHealth.clear()
    }

    fun close() {
        stopPeriodicChecks()
        httpClient.close()
    }
}
