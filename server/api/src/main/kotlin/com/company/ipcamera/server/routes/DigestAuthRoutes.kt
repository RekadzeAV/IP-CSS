package com.company.ipcamera.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Маршруты для ONVIF Digest Authentication.
 * Позволяет клиентам проверить Digest аутентификацию.
 */
fun Route.digestAuthRoutes() {
    route("/digest-auth") {
        get("/test") {
            call.respondText(
                """{
                    "status": "ready",
                    "description": "ONVIF Digest Auth endpoint",
                    "usage": "POST /api/v1/digest-auth/authenticate with WWW-Authenticate header"
                }""".trimIndent(),
                ContentType.Application.Json
            )
        }
    }
}
