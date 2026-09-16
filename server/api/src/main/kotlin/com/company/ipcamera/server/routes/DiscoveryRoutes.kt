package com.company.ipcamera.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Маршруты для обнаружения камер (WS-Discovery + UPnP).
 */
fun Route.discoveryRoutes() {
    route("/discovery") {
        get("/status") {
            call.respondText(
                """{
                    "wsDiscovery": "enabled",
                    "upnp": "enabled",
                    "multicastGroups": [
                        {"address": "239.255.255.250", "port": 3702, "protocol": "WS-Discovery"},
                        {"address": "239.255.255.250", "port": 1900, "protocol": "UPnP/SSDP"}
                    ]
                }""".trimIndent(),
                ContentType.Application.Json
            )
        }
    }
}
