package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun Route.cameraRoutes() {
    val repository: CameraRepository by inject()

    route("/cameras") {
        // GET /api/v1/cameras - список камер
        get {
            try {
                val cameras = repository.getCameras()
                val response = ApiResponse(
                    success = true,
                    data = cameras.map { it.toDto() },
                    message = "Cameras retrieved successfully"
                )
                call.respond(response)
            } catch (e: Exception) {
                logger.error(e) { "Error getting cameras" }
                call.respond(
                    status = io.ktor.http.HttpStatusCode.InternalServerError,
                    message = ApiResponse<Unit>(
                        success = false,
                        data = null,
                        message = "Error retrieving cameras: ${e.message}"
                    )
                )
            }
        }

        // GET /api/v1/cameras/{id} - получение камеры
        get("{id}") {
            try {
                val id = call.parameters["id"] ?: return@get call.respond(
                    status = io.ktor.http.HttpStatusCode.BadRequest,
                    message = ApiResponse<Unit>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                val camera = repository.getCameraById(id)
                if (camera != null) {
                    call.respond(
                        ApiResponse(
                            success = true,
                            data = camera.toDto(),
                            message = "Camera retrieved successfully"
                        )
                    )
                } else {
                    call.respond(
                        status = io.ktor.http.HttpStatusCode.NotFound,
                        message = ApiResponse<Unit>(
                            success = false,
                            data = null,
                            message = "Camera not found"
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "Error getting camera" }
                call.respond(
                    status = io.ktor.http.HttpStatusCode.InternalServerError,
                    message = ApiResponse<Unit>(
                        success = false,
                        data = null,
                        message = "Error retrieving camera: ${e.message}"
                    )
                )
            }
        }

        // POST /api/v1/cameras - создание камеры
        post {
            try {
                val request = call.receive<CreateCameraRequest>()
                val camera = request.toDomain()

                val result = repository.addCamera(camera)
                result.fold(
                    onSuccess = { createdCamera ->
                        call.respond(
                            status = io.ktor.http.HttpStatusCode.Created,
                            message = ApiResponse(
                                success = true,
                                data = createdCamera.toDto(),
                                message = "Camera created successfully"
                            )
                        )
                    },
                    onFailure = { error ->
                        logger.error(error) { "Error creating camera" }
                        call.respond(
                            status = io.ktor.http.HttpStatusCode.BadRequest,
                            message = ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Error creating camera: ${error.message}"
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                logger.error(e) { "Error creating camera" }
                call.respond(
                    status = io.ktor.http.HttpStatusCode.InternalServerError,
                    message = ApiResponse<Unit>(
                        success = false,
                        data = null,
                        message = "Error creating camera: ${e.message}"
                    )
                )
            }
        }

        // PUT /api/v1/cameras/{id} - обновление камеры
        put("{id}") {
            try {
                val id = call.parameters["id"] ?: return@put call.respond(
                    status = io.ktor.http.HttpStatusCode.BadRequest,
                    message = ApiResponse<Unit>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                val request = call.receive<UpdateCameraRequest>()
                val existingCamera = repository.getCameraById(id)
                    ?: return@put call.respond(
                        status = io.ktor.http.HttpStatusCode.NotFound,
                        message = ApiResponse<Unit>(
                            success = false,
                            data = null,
                            message = "Camera not found"
                        )
                    )

                val updatedCamera = existingCamera.copy(
                    name = request.name ?: existingCamera.name,
                    url = request.url ?: existingCamera.url,
                    username = request.username ?: existingCamera.username,
                    password = request.password ?: existingCamera.password
                )

                val result = repository.updateCamera(updatedCamera)
                result.fold(
                    onSuccess = { camera ->
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = camera.toDto(),
                                message = "Camera updated successfully"
                            )
                        )
                    },
                    onFailure = { error ->
                        logger.error(error) { "Error updating camera" }
                        call.respond(
                            status = io.ktor.http.HttpStatusCode.BadRequest,
                            message = ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Error updating camera: ${error.message}"
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                logger.error(e) { "Error updating camera" }
                call.respond(
                    status = io.ktor.http.HttpStatusCode.InternalServerError,
                    message = ApiResponse<Unit>(
                        success = false,
                        data = null,
                        message = "Error updating camera: ${e.message}"
                    )
                )
            }
        }

        // DELETE /api/v1/cameras/{id} - удаление камеры
        delete("{id}") {
            try {
                val id = call.parameters["id"] ?: return@delete call.respond(
                    status = io.ktor.http.HttpStatusCode.BadRequest,
                    message = ApiResponse<Unit>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                val result = repository.removeCamera(id)
                result.fold(
                    onSuccess = {
                        call.respond(
                            ApiResponse<Unit>(
                                success = true,
                                data = null,
                                message = "Camera deleted successfully"
                            )
                        )
                    },
                    onFailure = { error ->
                        logger.error(error) { "Error deleting camera" }
                        call.respond(
                            status = io.ktor.http.HttpStatusCode.BadRequest,
                            message = ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Error deleting camera: ${error.message}"
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                logger.error(e) { "Error deleting camera" }
                call.respond(
                    status = io.ktor.http.HttpStatusCode.InternalServerError,
                    message = ApiResponse<Unit>(
                        success = false,
                        data = null,
                        message = "Error deleting camera: ${e.message}"
                    )
                )
            }
        }

        // POST /api/v1/cameras/{id}/test - тест подключения
        post("{id}/test") {
            try {
                val id = call.parameters["id"] ?: return@post call.respond(
                    status = io.ktor.http.HttpStatusCode.BadRequest,
                    message = ApiResponse<Unit>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                val camera = repository.getCameraById(id)
                    ?: return@post call.respond(
                        status = io.ktor.http.HttpStatusCode.NotFound,
                        message = ApiResponse<Unit>(
                            success = false,
                            data = null,
                            message = "Camera not found"
                        )
                    )

                val result = repository.testConnection(camera)
                call.respond(
                    ApiResponse(
                        success = true,
                        data = result.toDto(),
                        message = "Connection test completed"
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Error testing camera connection" }
                call.respond(
                    status = io.ktor.http.HttpStatusCode.InternalServerError,
                    message = ApiResponse<Unit>(
                        success = false,
                        data = null,
                        message = "Error testing connection: ${e.message}"
                    )
                )
            }
        }

        // GET /api/v1/cameras/discover - обнаружение камер
        get("discover") {
            try {
                val discovered = repository.discoverCameras()
                call.respond(
                    ApiResponse(
                        success = true,
                        data = discovered.map { it.toDto() },
                        message = "Camera discovery completed"
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Error discovering cameras" }
                call.respond(
                    status = io.ktor.http.HttpStatusCode.InternalServerError,
                    message = ApiResponse<Unit>(
                        success = false,
                        data = null,
                        message = "Error discovering cameras: ${e.message}"
                    )
                )
            }
        }
    }
}

