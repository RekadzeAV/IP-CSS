package com.company.ipcamera.server.routing

import com.company.ipcamera.server.repository.ObjectDetectionConfigRepository
import com.company.ipcamera.server.repository.ObjectDetectionEventRepository
import com.company.ipcamera.server.service.ObjectDetectionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.objectDetectionRoutes(
    objectDetectionService: ObjectDetectionService,
    configRepository: ObjectDetectionConfigRepository,
    eventRepository: ObjectDetectionEventRepository
) {
    route("/api/v1/object-detection") {
        
        get("/config/{cameraId}") {
            val cameraId = call.parameters["cameraId"] ?: run { call.respond(HttpStatusCode.BadRequest); return@get };
            val config = configRepository.findByCameraId(cameraId)
            if (config != null) call.respond(HttpStatusCode.OK, config)
            else call.respond(HttpStatusCode.NotFound, "Config not found")
        }
        
        post("/config") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "config endpoint disabled"))
        }
        
        post("/start/{cameraId}") {
            val cameraId = call.parameters["cameraId"] ?: run { call.respond(HttpStatusCode.BadRequest); return@post };
            val data = call.receive<Map<String, String>>()
            val rtspUrl = data["rtspUrl"] ?: run { call.respond(HttpStatusCode.BadRequest, "RTSP URL required"); return@post }
            
            val result = objectDetectionService.startDetection(cameraId, rtspUrl)
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.OK, mapOf("status" to "started")) },
                onFailure = { call.respond(HttpStatusCode.InternalServerError, it.message ?: "Error") }
            )
        }
        
        post("/stop/{cameraId}") {
            val cameraId = call.parameters["cameraId"] ?: run { call.respond(HttpStatusCode.BadRequest); return@post };
            val result = objectDetectionService.stopDetection(cameraId)
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.OK, mapOf("status" to "stopped")) },
                onFailure = { call.respond(HttpStatusCode.InternalServerError, it.message ?: "Error") }
            )
        }
        
        get("/status/{cameraId}") {
            val cameraId = call.parameters["cameraId"] ?: run { call.respond(HttpStatusCode.BadRequest); return@get };
            val status = objectDetectionService.getDetectorStatus(cameraId)
            if (status != null) call.respond(HttpStatusCode.OK, status)
            else call.respond(HttpStatusCode.NotFound, "Detector not found")
        }
        
        get("/events") {
            val cameraId = call.request.queryParameters["cameraId"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val events = if (cameraId != null) {
                eventRepository.findByCameraId(cameraId, limit)
            } else {
                eventRepository.findByTimeRange(
                    System.currentTimeMillis() - 86400000,
                    System.currentTimeMillis(),
                    limit
                )
            }
            call.respond(HttpStatusCode.OK, events)
        }
        
        get("/stats/{cameraId}") {
            val cameraId = call.parameters["cameraId"] ?: run { call.respond(HttpStatusCode.BadRequest); return@get };
            val startTime = call.request.queryParameters["startTime"]?.toLongOrNull()
                ?: (System.currentTimeMillis() - 86400000)
            val endTime = call.request.queryParameters["endTime"]?.toLongOrNull()
                ?: System.currentTimeMillis()
            
            val stats = eventRepository.getStats(cameraId, startTime, endTime)
            if (stats != null) call.respond(HttpStatusCode.OK, stats)
            else call.respond(HttpStatusCode.NotFound, "Stats not found")
        }
    }
}