package com.company.ipcamera.server.controller

import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.onvif.OnvifEventSubscriptionService
import com.company.ipcamera.shared.domain.repository.CameraRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Контроллер для управления ONVIF подписками камер
 */
fun Route.onvifRoutes() {
    val cameraRepository: CameraRepository by inject()

    route("/onvif") {
        authenticate("jwt-auth") {
            // Проверка поддержки ONVIF для камеры
            route("/check") {
                post("/{cameraId}") {
                    val cameraId = call.parameters["cameraId"]
                    if (cameraId == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, null, "Camera ID required"))
                        return@post
                    }

                    val camera = cameraRepository.getCameraById(cameraId)
                    if (camera == null) {
                        call.respond(HttpStatusCode.NotFound, ApiResponse(false, null, "Camera not found"))
                        return@post
                    }

                    val supported = OnvifEventSubscriptionService.checkOnvifSupport(camera.url)

                    call.respond(ApiResponse(true, mapOf(
                        "cameraId" to cameraId,
                        "onvifSupported" to supported,
                        "cameraIp" to camera.url
                    ), "success"))
                }
            }

            // Подписка на события камеры
            route("/subscribe") {
                post("/") {
                    val request = call.receive<OnvifSubscribeRequest>()
                    val camera = cameraRepository.getCameraById(request.cameraId)
                    if (camera == null) {
                        call.respond(HttpStatusCode.NotFound, ApiResponse(false, null, "Camera not found"))
                        return@post
                    }

                    val success = OnvifEventSubscriptionService.subscribeToCamera(
                        cameraId = request.cameraId,
                        cameraIp = camera.url,
                        username = camera.username ?: "admin",
                        password = camera.password ?: ""
                    )

                    if (success) {
                        call.respond(ApiResponse(true, mapOf(
                            "message" to "Successfully subscribed to camera events",
                            "cameraId" to request.cameraId
                        ), "success"))
                    } else {
                        call.respond(ApiResponse(false, null, "Failed to subscribe to camera events"))
                    }
                }
            }

            // Отписка от событий камеры
            route("/unsubscribe") {
                post("/{cameraId}") {
                    val cameraId = call.parameters["cameraId"]
                    if (cameraId == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, null, "Camera ID required"))
                        return@post
                    }

                    val camera = cameraRepository.getCameraById(cameraId)
                    if (camera == null) {
                        call.respond(HttpStatusCode.NotFound, ApiResponse(false, null, "Camera not found"))
                        return@post
                    }

                    val success = OnvifEventSubscriptionService.unsubscribeFromCamera(cameraId, "")

                    if (success) {
                        call.respond(ApiResponse(true, mapOf(
                            "message" to "Successfully unsubscribed from camera events",
                            "cameraId" to cameraId
                        ), "success"))
                    } else {
                        call.respond(ApiResponse(false, null, "Failed to unsubscribe from camera events"))
                    }
                }
            }

            // Получить информацию об устройстве
            route("/device") {
                get("/{cameraId}") {
                    val cameraId = call.parameters["cameraId"]
                    if (cameraId == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, null, "Camera ID required"))
                        return@get
                    }

                    val camera = cameraRepository.getCameraById(cameraId)
                    if (camera == null) {
                        call.respond(HttpStatusCode.NotFound, ApiResponse(false, null, "Camera not found"))
                        return@get
                    }

                    val deviceInfo = OnvifEventSubscriptionService.getDeviceInfo(
                        camera.url,
                        camera.username ?: "admin",
                        camera.password ?: ""
                    )

                    if (deviceInfo != null) {
                        call.respond(ApiResponse(true, mapOf(
                            "cameraId" to cameraId,
                            "manufacturer" to deviceInfo.manufacturer,
                            "model" to deviceInfo.model,
                            "firmwareVersion" to deviceInfo.firmwareVersion,
                            "serialNumber" to deviceInfo.serialNumber,
                            "hardwareId" to deviceInfo.hardwareId
                        ), "success"))
                    } else {
                        call.respond(ApiResponse(false, null, "Failed to retrieve device information"))
                    }
                }
            }
        }
    }
}

data class OnvifSubscribeRequest(
    val cameraId: String,
    val cameraIp: String,
    val username: String,
    val password: String
)
