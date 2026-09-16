package com.company.ipcamera.server.routing

import com.company.ipcamera.server.repository.MotionConfigRepository
import com.company.ipcamera.server.repository.MotionEventRepository
import com.company.ipcamera.server.service.MotionDetectorService
import com.company.ipcamera.shared.domain.model.MotionConfig
import com.company.ipcamera.shared.domain.model.MotionZone
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
// kotlinx.datetime.Clock removed - using System.currentTimeMillis()
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * API Routes для управления детекцией движения
 */
fun Route.motionRoutes(
    motionDetectorService: MotionDetectorService,
    motionConfigRepository: MotionConfigRepository,
    motionEventRepository: MotionEventRepository
) {
    route("/api/v1/motion") {
        
        // Получить конфигурацию для камеры
        get("/config/{cameraId}") {
            val cameraId = call.parameters["cameraId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            
            val config = motionConfigRepository.findByCameraId(cameraId)
            if (config != null) {
                call.respond(HttpStatusCode.OK, config)
            } else {
                call.respond(HttpStatusCode.NotFound, "Configuration not found")
            }
        }
        
        // Создать/обновить конфигурацию
        post("/config") {
            val config = call.receive<MotionConfig>()
            
            val result = motionConfigRepository.save(config)
            result.fold(
                onSuccess = { savedConfig ->
                    call.respond(HttpStatusCode.OK, savedConfig)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to save motion config" }
                    call.respond(HttpStatusCode.InternalServerError, error.message ?: "Unknown error")
                }
            )
        }
        
        // Обновить статус
        patch("/config/{cameraId}/status") {
            val cameraId = call.parameters["cameraId"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
            val data = call.receive<Map<String, Any>>()
            val enabled = data["enabled"] as? Boolean ?: return@patch call.respond(HttpStatusCode.BadRequest)
            
            val result = motionConfigRepository.updateStatus(cameraId, enabled)
            result.fold(
                onSuccess = { config ->
                    call.respond(HttpStatusCode.OK, config)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to update motion status" }
                    call.respond(HttpStatusCode.InternalServerError, error.message ?: "Unknown error")
                }
            )
        }
        
        // Удалить конфигурацию
        delete("/config/{cameraId}") {
            val cameraId = call.parameters["cameraId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            
            val config = motionConfigRepository.findByCameraId(cameraId)
            if (config != null) {
                motionConfigRepository.delete(config.id)
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "Configuration not found")
            }
        }
        
        // Запустить детекцию
        post("/start/{cameraId}") {
            val cameraId = call.parameters["cameraId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val data = call.receive<Map<String, String>>()
            val rtspUrl = data["rtspUrl"] ?: return@post call.respond(HttpStatusCode.BadRequest, "RTSP URL required")
            
            val config = com.company.ipcamera.shared.domain.model.MotionConfig(id = java.util.UUID.randomUUID().toString(), cameraId = cameraId, enabled = true, sensitivity = 0.5, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()); val result = motionDetectorService.startDetection(cameraId, config)
            result.fold(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, mapOf("status" to "started"))
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to start motion detection" }
                    call.respond(HttpStatusCode.InternalServerError, error.message ?: "Unknown error")
                }
            )
        }
        
        // Остановить детекцию
        post("/stop/{cameraId}") {
            val cameraId = call.parameters["cameraId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            
            val result = motionDetectorService.stopDetection(cameraId)
            result.fold(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, mapOf("status" to "stopped"))
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to stop motion detection" }
                    call.respond(HttpStatusCode.InternalServerError, error.message ?: "Unknown error")
                }
            )
        }
        
        // Получить статус детектора
        get("/status/{cameraId}") {
            val cameraId = call.parameters["cameraId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            
            val status = motionDetectorService.getDetectorStatus(cameraId)
            if (status != null) {
                call.respond(HttpStatusCode.OK, status)
            } else {
                call.respond(HttpStatusCode.NotFound, "Detector not found")
            }
        }
        
        // Получить все статусы
        get("/status") {
            val statuses = motionDetectorService.getAllDetectorStatuses()
            call.respond(HttpStatusCode.OK, statuses)
        }
        
        // Получить события
        get("/events") {
            val cameraId = call.request.queryParameters["cameraId"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            
            val events = if (cameraId != null) {
                motionEventRepository.findByCameraId(cameraId, limit)
            } else {
                val endTime = System.currentTimeMillis()
                val startTime = endTime - (24 * 60 * 60 * 1000) // Последние 24 часа
                motionEventRepository.findByTimeRange(startTime, endTime, limit)
            }
            
            call.respond(HttpStatusCode.OK, events)
        }
        
        // Получить событие по ID
        get("/events/{eventId}") {
            val eventId = call.parameters["eventId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            
            val event = motionEventRepository.findById(eventId)
            if (event != null) {
                call.respond(HttpStatusCode.OK, event)
            } else {
                call.respond(HttpStatusCode.NotFound, "Event not found")
            }
        }
        
        // Отметить событие как обработанное
        post("/events/{eventId}/process") {
            val eventId = call.parameters["eventId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            
            val result = motionEventRepository.markProcessed(eventId)
            result.fold(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, mapOf("status" to "processed"))
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to mark event as processed" }
                    call.respond(HttpStatusCode.InternalServerError, error.message ?: "Unknown error")
                }
            )
        }
    }
}
