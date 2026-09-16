package com.company.ipcamera.server.routing

import com.company.ipcamera.server.config.RedisConfig
import com.company.ipcamera.server.config.DatabaseConfig
import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireAdmin
import com.company.ipcamera.server.service.CameraService
import com.company.ipcamera.server.service.FfmpegService
import com.company.ipcamera.server.service.StorageService
import com.company.ipcamera.server.service.ApiMetricsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class HealthStatus(
    val status: String,
    val version: String = "1.0.0",
    val timestamp: Long = System.currentTimeMillis(),
    val checks: Map<String, String> = emptyMap(),
    val nodeId: String? = null,
    val clusterEnabled: Boolean = false
)

@Serializable
data class SystemStatistics(
    val cameras: CameraStatistics? = null,
    val storage: StorageStatistics? = null,
    val services: ServiceStatus? = null,
    val uptime: Long = System.currentTimeMillis() - startTime
) {
    companion object {
        val startTime = System.currentTimeMillis()
    }
}

@Serializable
data class CameraStatistics(
    val total: Int,
    val online: Int,
    val offline: Int,
    val error: Int,
    val unknown: Int,
    val monitoringActive: Boolean
)

@Serializable
data class StorageStatistics(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val usagePercentage: Double,
    val warningThresholdExceeded: Boolean
)

@Serializable
data class ServiceStatus(
    val redis: Boolean,
    val ffmpeg: Boolean,
    val database: Boolean = false,
    val databasePool: DatabasePoolStatus? = null
)

@Serializable
data class DatabasePoolStatus(
    val activeConnections: Int,
    val idleConnections: Int,
    val totalConnections: Int,
    val maxPoolSize: Int,
    val utilizationPercent: Double,
    val isHealthy: Boolean
)

/**
 * [DatabaseConfig.getDataSource] существует только после инициализации пула в [DatabaseFactory.createPostgresDriver],
 * т.е. при первом разрешении Koin [com.company.ipcamera.shared.database.CameraDatabase]. Регистрация маршрутов
 * выполняется раньше — нельзя кэшировать [DatabaseMonitoringService] на уровне [healthRoutes()].
 */
private suspend fun databaseMonitoringForHealth(): com.company.ipcamera.server.service.DatabaseMonitoringService? {
    return try {
        val dataSource = DatabaseConfig.getDataSource() ?: return null
        com.company.ipcamera.server.service.DatabaseMonitoringService(dataSource)
    } catch (e: Exception) {
        null
    }
}

fun Route.healthRoutes() {
    val ffmpegService: FfmpegService by inject()
    val storageService: StorageService by inject()
    val cameraService: CameraService by inject()
    val apiMetricsService: ApiMetricsService by inject()
    val clusterService: com.company.ipcamera.server.cluster.ClusterService by inject()

    // GET /api/v1/health - базовая проверка здоровья
    get("/health") {
        val checks = mutableMapOf<String, String>()
        var overallStatus = "OK"

        // Проверка Redis
        val redisAvailable = try {
            runBlocking { RedisConfig.ping() }
        } catch (e: Exception) {
            false
        }
        val redisFallbackInDev = RedisConfig.isFallbackModeEnabled() && !DatabaseConfig.isProductionEnvironment()
        checks["redis"] = when {
            redisAvailable -> "OK"
            redisFallbackInDev -> "FALLBACK"
            else -> "FAIL"
        }
        if (!redisAvailable && !redisFallbackInDev) overallStatus = "DEGRADED"

        // Проверка FFmpeg
        val ffmpegAvailable = ffmpegService.isAvailable()
        checks["ffmpeg"] = if (ffmpegAvailable) "OK" else "FAIL"
        if (!ffmpegAvailable) overallStatus = "DEGRADED"

        // Проверка хранилища
        val storageInfo = storageService.getStorageInfo()
        checks["storage"] = if (storageInfo.availableBytes > 0) "OK" else "FAIL"
        if (storageInfo.availableBytes == 0L) overallStatus = "DEGRADED"

        // Проверка БД:
        // - В postgres режиме/production требуется реальная доступность БД.
        // - В embedded режиме считаем БД доступной.
        val requiresPostgresHealth = DatabaseConfig.isPostgresModeEnabled() || DatabaseConfig.isProductionEnvironment()
        val databaseHealthy = if (requiresPostgresHealth) {
            databaseMonitoringForHealth()?.let { service ->
                runBlocking { service.checkConnection().isHealthy }
            } ?: false
        } else {
            true
        }
        checks["database"] = if (databaseHealthy) "OK" else "FAIL"
        if (!databaseHealthy) overallStatus = "DEGRADED"

        val statusCode = if (overallStatus == "OK") HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable

        call.respond(
            statusCode,
            ApiResponse(
                success = overallStatus == "OK",
                data = HealthStatus(
                    status = overallStatus,
                    version = "1.0.0",
                    timestamp = System.currentTimeMillis(),
                    checks = checks,
                    nodeId = clusterService.getNodeId().takeIf { clusterService.isEnabled() },
                    clusterEnabled = clusterService.isEnabled()
                ),
                message = if (overallStatus == "OK") "Server is healthy" else "Some services are unavailable"
            )
        )
    }

    // GET /api/v1/health/ready - проверка готовности (для Kubernetes readiness probe)
    get("/health/ready") {
        val checks = mutableMapOf<String, String>()
        var isReady = true

        // Проверка Redis
        val redisAvailable = try {
            runBlocking { RedisConfig.ping() }
        } catch (e: Exception) {
            false
        }
        val redisFallbackInDev = RedisConfig.isFallbackModeEnabled() && !DatabaseConfig.isProductionEnvironment()
        checks["redis"] = when {
            redisAvailable -> "OK"
            redisFallbackInDev -> "FALLBACK"
            else -> "FAIL"
        }
        if (!redisAvailable && !redisFallbackInDev) isReady = false

        // Проверка хранилища
        val storageInfo = storageService.getStorageInfo()
        checks["storage"] = if (storageInfo.availableBytes > 0) "OK" else "FAIL"
        if (storageInfo.availableBytes == 0L) isReady = false

        // Readiness обязательно учитывает БД, если сервер ожидает PostgreSQL.
        val requiresPostgresHealth = DatabaseConfig.isPostgresModeEnabled() || DatabaseConfig.isProductionEnvironment()
        val databaseReady = if (requiresPostgresHealth) {
            databaseMonitoringForHealth()?.let { service ->
                runBlocking { service.checkConnection().isHealthy }
            } ?: false
        } else {
            true
        }
        checks["database"] = if (databaseReady) "OK" else "FAIL"
        if (!databaseReady) isReady = false

        val statusCode = if (isReady) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable

        call.respond(
            statusCode,
            ApiResponse(
                success = isReady,
                data = HealthStatus(
                    status = if (isReady) "READY" else "NOT_READY",
                    checks = checks,
                    nodeId = clusterService.getNodeId().takeIf { clusterService.isEnabled() },
                    clusterEnabled = clusterService.isEnabled()
                ),
                message = if (isReady) "Server is ready" else "Server is not ready"
            )
        )
    }

    // GET /api/v1/health/live - проверка живости (для Kubernetes liveness probe)
    get("/health/live") {
        call.respond(
            HttpStatusCode.OK,
            ApiResponse(
                success = true,
                data = HealthStatus(status = "ALIVE"),
                message = "Server is alive"
            )
        )
    }

    // GET /api/v1/statistics - статистика системы (требует аутентификации)
    authenticate("jwt-auth") {
        get("/health/metrics") {
            requireAdmin()
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = apiMetricsService.snapshot(),
                    message = "API metrics retrieved successfully"
                )
            )
        }

        get("/statistics") {
            try {
                // Статистика камер
                val cameraStats = cameraService.getMonitoringStats()
                val cameraStatistics = CameraStatistics(
                    total = cameraStats.totalCameras,
                    online = cameraStats.onlineCameras,
                    offline = cameraStats.offlineCameras,
                    error = cameraStats.errorCameras,
                    unknown = cameraStats.unknownCameras,
                    monitoringActive = cameraStats.isMonitoringActive
                )

                // Статистика хранилища
                val storageInfo = storageService.getStorageInfo()
                val storageStatistics = StorageStatistics(
                    totalBytes = storageInfo.totalBytes,
                    usedBytes = storageInfo.usedBytes,
                    availableBytes = storageInfo.availableBytes,
                    usagePercentage = storageInfo.usagePercentage,
                    warningThresholdExceeded = storageInfo.warningThresholdExceeded
                )

                // Статус сервисов
                val redisAvailable = try {
                    runBlocking { RedisConfig.ping() }
                } catch (e: Exception) {
                    false
                }

                // Статус connection pool
                val poolStatus = databaseMonitoringForHealth()?.let { service ->
                    runBlocking {
                        val stats = service.getPoolStats()
                        DatabasePoolStatus(
                            activeConnections = stats.activeConnections,
                            idleConnections = stats.idleConnections,
                            totalConnections = stats.totalConnections,
                            maxPoolSize = stats.maxPoolSize,
                            utilizationPercent = stats.utilizationPercent,
                            isHealthy = stats.isHealthy
                        )
                    }
                }

                val databaseHealthy = databaseMonitoringForHealth()?.let { service ->
                    runBlocking {
                        service.checkConnection().isHealthy
                    }
                } ?: true

                val serviceStatus = ServiceStatus(
                    redis = redisAvailable,
                    ffmpeg = ffmpegService.isAvailable(),
                    database = databaseHealthy,
                    databasePool = poolStatus
                )

                val statistics = SystemStatistics(
                    cameras = cameraStatistics,
                    storage = storageStatistics,
                    services = serviceStatus
                )

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = statistics,
                        message = "System statistics retrieved successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<SystemStatistics>(
                        success = false,
                        data = null,
                        message = "Error retrieving statistics: ${e.message}"
                    )
                )
            }
        }
    }
}



