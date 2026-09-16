package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireRole
import com.company.ipcamera.server.service.DatabaseBackupService
import com.company.ipcamera.server.service.DatabaseMonitoringService
import com.company.ipcamera.shared.domain.model.UserRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.nio.file.Paths

@Serializable
data class DatabasePoolStatsDto(
    val activeConnections: Int,
    val idleConnections: Int,
    val totalConnections: Int,
    val threadsAwaitingConnection: Int,
    val maxPoolSize: Int,
    val minIdle: Int,
    val utilizationPercent: Double,
    val isHealthy: Boolean
)

@Serializable
data class DatabaseHealthDto(
    val isHealthy: Boolean,
    val databaseName: String? = null,
    val error: String? = null,
    val timestamp: Long
)

@Serializable
data class MigrationInfoDto(
    val pending: Int,
    val applied: Int,
    val total: Int,
    val currentVersion: String? = null,
    val isUpToDate: Boolean,
    val error: String? = null
)

@Serializable
data class BackupInfoDto(
    val fileName: String,
    val size: Long,
    val createdAt: Long
)

@Serializable
data class CreateBackupRequest(
    val customName: String? = null
)

@Serializable
data class RestoreBackupRequest(
    val backupPath: String
)

/**
 * Маршруты для управления и мониторинга базы данных
 * Все маршруты требуют JWT аутентификации и роли ADMIN
 */
fun Route.databaseRoutes() {
    authenticate("jwt-auth") {
        val monitoringService: DatabaseMonitoringService? by inject()
        val backupService: DatabaseBackupService? by inject()

        route("/database/pool") {
            get("/stats") { handlePoolStats(monitoringService, call) }
        }

        route("/database/health") {
            get { handleHealthCheck(monitoringService, call) }
        }

        route("/database/migrations") {
            get { handleMigrations(monitoringService, call) }
        }

        route("/database/backup") {
            post { handleCreateBackup(backupService, call) }
            get { handleListBackups(backupService, call) }
            post("/restore") { handleRestoreBackup(backupService, call) }
            post("/cleanup") { handleCleanupBackups(backupService, call) }
        }
    }
}

private suspend fun handlePoolStats(monitoringService: DatabaseMonitoringService?, call: io.ktor.server.application.ApplicationCall) {
    handleServiceAvailability(monitoringService, "Database connection pool is not available", call) {
        val stats = monitoringService!!.getPoolStats()
        call.respond(
            HttpStatusCode.OK,
            ApiResponse(
                success = true,
                data = DatabasePoolStatsDto(
                    activeConnections = stats.activeConnections,
                    idleConnections = stats.idleConnections,
                    totalConnections = stats.totalConnections,
                    threadsAwaitingConnection = stats.threadsAwaitingConnection,
                    maxPoolSize = stats.maxPoolSize,
                    minIdle = stats.minIdle,
                    utilizationPercent = stats.utilizationPercent,
                    isHealthy = stats.isHealthy
                ),
                message = "Pool statistics retrieved successfully"
            )
        )
    }
}

private suspend fun handleHealthCheck(monitoringService: DatabaseMonitoringService?, call: io.ktor.server.application.ApplicationCall) {
    handleServiceAvailability(monitoringService, "Database is not available", call) {
        val health = monitoringService!!.checkConnection()
        call.respond(
            if (health.isHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
            ApiResponse(
                success = health.isHealthy,
                data = DatabaseHealthDto(
                    isHealthy = health.isHealthy,
                    databaseName = health.databaseName,
                    error = health.error,
                    timestamp = health.timestamp
                ),
                message = if (health.isHealthy) "Database is healthy" else "Database health check failed"
            )
        )
    }
}

private suspend fun handleMigrations(monitoringService: DatabaseMonitoringService?, call: io.ktor.server.application.ApplicationCall) {
    handleServiceAvailability(monitoringService, "Database is not available", call) {
        val info = monitoringService!!.getMigrationInfo()
        call.respond(
            HttpStatusCode.OK,
            ApiResponse(
                success = true,
                data = MigrationInfoDto(
                    pending = info.pending,
                    applied = info.applied,
                    total = info.total,
                    currentVersion = info.currentVersion,
                    isUpToDate = info.isUpToDate,
                    error = info.error
                ),
                message = "Migration info retrieved successfully"
            )
        )
    }
}

private suspend fun handleCreateBackup(backupService: DatabaseBackupService?, call: io.ktor.server.application.ApplicationCall) {
    handleServiceAvailability(backupService, "Backup service is not available (PostgreSQL required)", call) {
        val request = parseBackupRequest(call)
        val result = backupService!!.createBackup(request.customName)
        respondResult(call, result) { backupPath ->
            mapOf(
                "backupPath" to backupPath.toString(),
                "fileName" to backupPath.fileName.toString()
            ) to "Database backup created successfully"
        }
    }
}

private suspend fun handleListBackups(backupService: DatabaseBackupService?, call: io.ktor.server.application.ApplicationCall) {
    handleServiceAvailability(backupService, "Backup service is not available (PostgreSQL required)", call) {
        val backups = backupService!!.listBackups()
        call.respond(
            HttpStatusCode.OK,
            ApiResponse(
                success = true,
                data = backups.map { BackupInfoDto(it.fileName, it.size, it.createdAt) },
                message = "Backup list retrieved successfully"
            )
        )
    }
}

private suspend fun handleRestoreBackup(backupService: DatabaseBackupService?, call: io.ktor.server.application.ApplicationCall) {
    handleServiceAvailability(backupService, "Backup service is not available (PostgreSQL required)", call) {
        val request = call.receive<RestoreBackupRequest>()
        val backupPath = validateBackupPath(request.backupPath, call) ?: return@handleServiceAvailability
        val result = backupService!!.restoreBackup(backupPath)
        respondResult(call, result) {
            mapOf("backupPath" to backupPath.toString()) to "Database restored successfully from backup"
        }
    }
}

private suspend fun handleCleanupBackups(backupService: DatabaseBackupService?, call: io.ktor.server.application.ApplicationCall) {
    handleServiceAvailability(backupService, "Backup service is not available (PostgreSQL required)", call) {
        val keepDays = call.request.queryParameters["keepDays"]?.toIntOrNull() ?: 30
        val deletedCount = backupService!!.cleanupOldBackups(keepDays)
        call.respond(
            HttpStatusCode.OK,
            ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount, "keepDays" to keepDays),
                message = "Old backups cleaned up successfully"
            )
        )
    }
}

private suspend fun parseBackupRequest(call: io.ktor.server.application.ApplicationCall): CreateBackupRequest {
    return try {
        call.receive<CreateBackupRequest>()
    } catch (e: Exception) {
        CreateBackupRequest(customName = null)
    }
}

private suspend fun validateBackupPath(path: String, call: io.ktor.server.application.ApplicationCall): java.nio.file.Path? {
    if (path.isBlank()) {
        call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, data = null, message = "Backup path is required"))
        return null
    }
    return try {
        java.nio.file.Paths.get(path)
    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, data = null, message = "Invalid backup path: ${e.message}"))
        null
    }
}

private suspend fun <T : Any> handleServiceAvailability(
    service: T?,
    errorMessage: String,
    call: io.ktor.server.application.ApplicationCall,
    block: suspend () -> Unit
) {
    if (service == null) {
        call.respond(HttpStatusCode.ServiceUnavailable, ApiResponse<String>(success = false, data = null, message = errorMessage))
        return
    }
    try {
        block()
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, ApiResponse<String>(success = false, data = null, message = "Error: ${e.message}"))
    }
}

private suspend fun <T : Any> respondResult(
    call: io.ktor.server.application.ApplicationCall,
    result: Result<T>,
    toResponseData: (T) -> Pair<Map<String, Any?>, String>
) {
    result.fold(
        onSuccess = { value ->
            val (data, message) = toResponseData(value)
            call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = data, message = message))
        },
        onFailure = { error ->
            call.respond(HttpStatusCode.InternalServerError, ApiResponse<String>(success = false, data = null, message = "Error: ${error.message}"))
        }
    )
}
