package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.shared.domain.repository.CameraRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Маршруты для управления камерами
 * Все маршруты требуют JWT аутентификации
 */
fun Route.cameraRoutes() {
    val cameraRepository: CameraRepository by inject()
    
    authenticate("jwt-auth") {
        route("/cameras") {
            // GET /api/v1/cameras - список всех камер
            get {
                try {
                    val cameras = cameraRepository.getCameras()
                    val camerasDto = cameras.map { it.toDto() }
                    
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = camerasDto,
                            message = "Cameras retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<List<CameraDto>>(
                            success = false,
                            data = null,
                            message = "Error retrieving cameras: ${e.message}"
                        )
                    )
                }
            }
            
            // POST /api/v1/cameras - создание новой камеры
            post {
                try {
                    val request = call.receive<CreateCameraRequest>()
                    val camera = request.toDomain()
                    
                    val result = cameraRepository.addCamera(camera)
                    result.fold(
                        onSuccess = { createdCamera ->
                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    data = createdCamera.toDto(),
                                    message = "Camera created successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<CameraDto>(
                                    success = false,
                                    data = null,
                                    message = "Error creating camera: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<CameraDto>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }
            
            // GET /api/v1/cameras/discover - обнаружение камер в сети
            get("/discover") {
                try {
                    val discoveredCameras = cameraRepository.discoverCameras()
                    val camerasDto = discoveredCameras.map { it.toDto() }
                    
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = camerasDto,
                            message = "Cameras discovered successfully"
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<List<DiscoveredCameraDto>>(
                            success = false,
                            data = null,
                            message = "Error discovering cameras: ${e.message}"
                        )
                    )
                }
            }
            
            route("/{id}") {
                // GET /api/v1/cameras/{id} - получение камеры по ID
                get {
                    try {
                        val id = call.parameters["id"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<CameraDto>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )
                        
                        val camera = cameraRepository.getCameraById(id)
                        if (camera != null) {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = camera.toDto(),
                                    message = "Camera retrieved successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<CameraDto>(
                                    success = false,
                                    data = null,
                                    message = "Camera not found"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<CameraDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }
                
                // PUT /api/v1/cameras/{id} - обновление камеры
                put {
                    try {
                        val id = call.parameters["id"] ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<CameraDto>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )
                        
                        val request = call.receive<UpdateCameraRequest>()
                        val existingCamera = cameraRepository.getCameraById(id)
                        
                        if (existingCamera == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<CameraDto>(
                                    success = false,
                                    data = null,
                                    message = "Camera not found"
                                )
                            )
                            return@put
                        }
                        
                        // Обновляем только переданные поля
                        val updatedCamera = existingCamera.copy(
                            name = request.name ?: existingCamera.name,
                            url = request.url ?: existingCamera.url,
                            username = request.username ?: existingCamera.username,
                            password = request.password ?: existingCamera.password
                        )
                        
                        val result = cameraRepository.updateCamera(updatedCamera)
                        result.fold(
                            onSuccess = { camera ->
                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse(
                                        success = true,
                                        data = camera.toDto(),
                                        message = "Camera updated successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<CameraDto>(
                                        success = false,
                                        data = null,
                                        message = "Error updating camera: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<CameraDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }
                
                // DELETE /api/v1/cameras/{id} - удаление камеры
                delete {
                    try {
                        val id = call.parameters["id"] ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )
                        
                        val result = cameraRepository.removeCamera(id)
                        result.fold(
                            onSuccess = {
                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse<Unit>(
                                        success = true,
                                        data = null,
                                        message = "Camera deleted successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<Unit>(
                                        success = false,
                                        data = null,
                                        message = "Error deleting camera: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }
                
                // POST /api/v1/cameras/{id}/test - тест подключения к камере
                post("/test") {
                    try {
                        val id = call.parameters["id"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<ConnectionTestResultDto>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )
                        
                        val camera = cameraRepository.getCameraById(id)
                        if (camera == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<ConnectionTestResultDto>(
                                    success = false,
                                    data = null,
                                    message = "Camera not found"
                                )
                            )
                            return@post
                        }
                        
                        val testResult = cameraRepository.testConnection(camera)
                        val resultDto = testResult.toDto()
                        
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = resultDto,
                                message = "Connection test completed"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<ConnectionTestResultDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }
            }
        }
    }
}

