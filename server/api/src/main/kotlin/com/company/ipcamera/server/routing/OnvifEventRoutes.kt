package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.service.OnvifEventSubscriptionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

/**
 * Маршруты для ONVIF Event Service
 *
 * Обрабатывает подписки на события камер и прием уведомлений от камер.
 */
fun Route.onvifEventRoutes() {
    val subscriptionService: OnvifEventSubscriptionService by inject()

    route("/onvif") {
        route("/events") {
            /**
             * POST /api/v1/onvif/events/notification/{subscriptionId?}
             *
             * Endpoint для приема событий от камер (WS-Notification NotificationMessage)
             *
             * Камеры отправляют события на этот endpoint после подписки.
             * Endpoint должен быть доступен извне (публичный URL).
             */
            post("/notification/{subscriptionId?}") {
                try {
                    val subscriptionId = call.parameters["subscriptionId"]
                    val notificationMessage = call.receiveText()

                    logger.debug {
                        "Received ONVIF notification, subscriptionId: $subscriptionId, " +
                        "message length: ${notificationMessage.length}"
                    }

                    // Обработать входящее событие
                    val result = subscriptionService.handleIncomingEvent(
                        notificationMessage = notificationMessage,
                        subscriptionId = subscriptionId
                    )

                    result.fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = null,
                                    message = "Notification processed successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Failed to process ONVIF notification" }
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<Unit>(
                                    success = false,
                                    data = null,
                                    message = "Failed to process notification: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error processing ONVIF notification" }
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(
                            success = false,
                            data = null,
                            message = "Invalid notification format: ${e.message}"
                        )
                    )
                }
            }

            /**
             * POST /api/v1/onvif/events/subscribe/{cameraId}
             *
             * Подписаться на события камеры
             */
            post("/subscribe/{cameraId}") {
                try {
                    val cameraId = call.parameters["cameraId"]
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )

                    val usePullPoint = call.request.queryParameters["pullPoint"]?.toBoolean() ?: false

                    val result = subscriptionService.subscribeToCameraEvents(
                        cameraId = cameraId,
                        usePullPoint = usePullPoint
                    )

                    result.fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = null,
                                    message = "Subscribed to camera events successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Failed to subscribe to camera events: $cameraId" }
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<Unit>(
                                    success = false,
                                    data = null,
                                    message = "Failed to subscribe: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error subscribing to camera events" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Unit>(
                            success = false,
                            data = null,
                            message = "Error: ${e.message}"
                        )
                    )
                }
            }

            /**
             * POST /api/v1/onvif/events/unsubscribe/{cameraId}
             *
             * Отписаться от событий камеры
             */
            post("/unsubscribe/{cameraId}") {
                try {
                    val cameraId = call.parameters["cameraId"]
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )

                    val result = subscriptionService.unsubscribeFromCameraEvents(cameraId)

                    result.fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = null,
                                    message = "Unsubscribed from camera events successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Failed to unsubscribe from camera events: $cameraId" }
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<Unit>(
                                    success = false,
                                    data = null,
                                    message = "Failed to unsubscribe: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error unsubscribing from camera events" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Unit>(
                            success = false,
                            data = null,
                            message = "Error: ${e.message}"
                        )
                    )
                }
            }
        }
    }
}
