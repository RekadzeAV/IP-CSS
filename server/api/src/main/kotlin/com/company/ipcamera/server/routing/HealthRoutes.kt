package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.ApiResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthStatus(
    val status: String,
    val version: String = "1.0.0"
)

fun Route.healthRoutes() {
    get("/health") {
        call.respond(
            ApiResponse(
                success = true,
                data = HealthStatus(status = "OK"),
                message = "Server is healthy"
            )
        )
    }
}



