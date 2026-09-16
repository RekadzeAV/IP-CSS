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
 * Маршруты для доступа к снимкам (без аутентификации для простоты доступа)
 */
fun Route.screenshotRoutes() {
    // GET /api/v1/screenshots/{fileName} - получить снимок
    get("/screenshots/{fileName}") {
        try {
            val fileName = call.parameters["fileName"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "File name is required"
            )
            
            // Проверяем, что это изображение (защита от path traversal)
            if (!fileName.matches(Regex(".*\\.(jpg|jpeg|png)$"))) {
                call.respond(HttpStatusCode.BadRequest, "Invalid file name")
                return@get
            }
            
            val filePath = Paths.get("screenshots", fileName)
            val file = filePath.toFile()
            
            if (file.exists()) {
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound, "Screenshot not found")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error serving screenshot" }
            call.respond(
                HttpStatusCode.InternalServerError,
                "Error serving screenshot: ${e.message}"
            )
        }
    }
}

