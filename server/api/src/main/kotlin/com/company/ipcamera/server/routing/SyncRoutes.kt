package com.company.ipcamera.server.routing

import com.company.ipcamera.server.config.CloudSyncConfig
import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireAdmin
import com.company.ipcamera.server.sync.CloudSyncService
import com.company.ipcamera.server.sync.ConflictResolutionStrategy
import com.company.ipcamera.server.sync.SyncConflict
import com.company.ipcamera.server.sync.SyncState
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

@Serializable
data class ResolveConflictsRequest(
    val conflictIds: List<String>,
    val strategy: String // LAST_WRITE_WINS | MERGE | MANUAL
)

/**
 * Маршруты облачной синхронизации (4.1.1).
 */
fun Route.syncRoutes() {
    val cloudSyncService: CloudSyncService by inject()

    authenticate("jwt-auth") {
        route("/sync") {
            get("/status") {
                requireAdmin()
                if (!CloudSyncConfig.enabled) {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = cloudSyncService.getSyncState(), message = "Cloud sync is disabled"))
                    return@get
                }
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = cloudSyncService.getSyncState(), message = "Sync status"))
            }

            post("/start") {
                requireAdmin()
                if (!CloudSyncConfig.enabled) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, data = null, message = "Cloud sync is disabled"))
                    return@post
                }
                try {
                    val result = cloudSyncService.syncAll()
                    result.fold(
                        onSuccess = {
                            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = Unit, message = "Sync completed"))
                        },
                        onFailure = { e ->
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, data = null, message = e.message ?: "Sync failed"))
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Sync start failed" }
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, data = null, message = e.message ?: "Sync start failed"))
                }
            }

            get("/conflicts") {
                requireAdmin()
                val conflicts = cloudSyncService.getConflicts()
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = conflicts, message = "Conflicts"))
            }

            post("/resolve") {
                requireAdmin()
                try {
                    val request = call.receive<ResolveConflictsRequest>()
                    val strategy = runCatching { ConflictResolutionStrategy.valueOf(request.strategy) }.getOrNull()
                        ?: ConflictResolutionStrategy.LAST_WRITE_WINS
                    val conflicts = cloudSyncService.getConflicts().filter { it.id in request.conflictIds }
                    val result = cloudSyncService.resolveConflicts(conflicts, strategy)
                    result.fold(
                        onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = Unit, message = "Conflicts resolved")) },
                        onFailure = { e -> call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, data = null, message = e.message ?: "Conflict resolution failed")) }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Resolve conflicts failed" }
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, data = null, message = e.message ?: "Resolve conflicts failed"))
                }
            }
        }
    }
}
