package com.company.ipcamera.server.routing

import com.company.ipcamera.server.cloud.CloudObjectInfo
import com.company.ipcamera.server.cloud.CloudStorageService
import com.company.ipcamera.server.config.CloudStorageConfig
import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireAdmin
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
data class CloudObjectInfoDto(val key: String, val size: Long, val lastModified: Long)

/**
 * Маршруты облачного хранилища записей (4.1.2).
 */
fun Route.cloudStorageRoutes() {
    val cloudStorage: CloudStorageService by inject()

    authenticate("jwt-auth") {
        route("/cloud/storage") {
            get {
                requireAdmin()
                if (!cloudStorage.isEnabled()) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<List<CloudObjectInfoDto>>(success = false, data = null, message = "Cloud storage not configured"))
                    return@get
                }
                val result = cloudStorage.listRecordings()
                result.fold(
                    onSuccess = { list ->
                        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = list.map { CloudObjectInfoDto(it.key, it.size, it.lastModified) }, message = "Recordings"))
                    },
                    onFailure = { e ->
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, data = null, message = e.message ?: "Cloud storage list failed"))
                    }
                )
            }

            get("/download/{recordingId}") {
                requireAdmin()
                val recordingId = call.parameters["recordingId"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, data = null, message = "recordingId required"))
                    return@get
                }
                val key = call.request.queryParameters["key"] ?: ""
                val result = cloudStorage.downloadRecording(recordingId, key)
                result.fold(
                    onSuccess = { data ->
                        call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"$recordingId.mp4\"")
                        call.respondBytes(bytes = data, contentType = ContentType.Video.MP4)
                    },
                    onFailure = { e ->
                        call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, data = null, message = e.message ?: "Recording not found in cloud storage"))
                    }
                )
            }

            delete("/{recordingId}") {
                requireAdmin()
                val recordingId = call.parameters["recordingId"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, data = null, message = "recordingId required"))
                    return@delete
                }
                val result = cloudStorage.deleteRecording(recordingId)
                result.fold(
                    onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, data = null, message = "Deleted")) },
                    onFailure = { e -> call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, data = null, message = e.message ?: "Cloud delete failed")) }
                )
            }
        }
    }
}
