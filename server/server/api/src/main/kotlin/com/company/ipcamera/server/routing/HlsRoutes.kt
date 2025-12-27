package com.company.ipcamera.server.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import java.io.File
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * Маршруты для HLS потоков (без аутентификации для доступа к сегментам)
 * Эти маршруты используются для доступа к HLS плейлистам и сегментам
 */
fun Route.hlsRoutes() {
    // GET /api/v1/cameras/streams/{streamId}/hls/playlist.m3u8 - HLS плейлист по streamId
    route("/streams/{streamId}/hls") {
        get("/playlist.m3u8") {
            try {
                val streamId = call.parameters["streamId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Stream ID is required"
                )
                
                val playlistPath = Paths.get("streams/hls", streamId, "playlist.m3u8")
                val playlistFile = playlistPath.toFile()
                
                if (playlistFile.exists()) {
                    var playlistContent = playlistFile.readText()
                    
                    // Заменяем пути к сегментам на правильные URL
                    playlistContent = playlistContent.replace(
                        Regex("segment_\\d+\\.ts"),
                        "/api/v1/cameras/streams/$streamId/hls/\\$0"
                    )
                    
                    call.respondText(
                        playlistContent,
                        ContentType("application", "vnd.apple.mpegurl"),
                        HttpStatusCode.OK
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound, "HLS playlist not found")
                }
            } catch (e: Exception) {
                logger.error(e) { "Error serving HLS playlist by streamId" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error serving playlist: ${e.message}"
                )
            }
        }
        
        // GET /api/v1/cameras/streams/{streamId}/hls/segment_XXX.ts - HLS сегмент
        get("/{segmentName}") {
            try {
                val streamId = call.parameters["streamId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Stream ID is required"
                )
                
                val segmentName = call.parameters["segmentName"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Segment name is required"
                )
                
                // Проверяем, что это сегмент (защита от path traversal)
                if (!segmentName.matches(Regex("segment_\\d+\\.ts"))) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid segment name")
                    return@get
                }
                
                val segmentPath = Paths.get("streams/hls", streamId, segmentName)
                val segmentFile = segmentPath.toFile()
                
                if (segmentFile.exists()) {
                    call.respondFile(segmentFile)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Segment not found")
                }
            } catch (e: Exception) {
                logger.error(e) { "Error serving HLS segment" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error serving segment: ${e.message}"
                )
            }
        }
    }
}

