package com.company.ipcamera.server.routing

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        route("/api/v1") {
            // Публичные маршруты (не требуют аутентификации)
            authRoutes()
            healthRoutes()

            // HLS маршруты (без аутентификации для доступа к сегментам)
            route("/cameras") {
                hlsRoutes()
            }

            // Маршруты для снимков (без аутентификации для простоты доступа)
            screenshotRoutes()

            // Защищенные маршруты (требуют аутентификации)
            cameraRoutes()
            recordingRoutes()
            eventRoutes()
            userRoutes()
            settingsRoutes()
            streamRoutes()
            notificationRoutes()
        }
    }
}



