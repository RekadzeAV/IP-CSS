package com.company.ipcamera.server.routing

import com.company.ipcamera.server.di.appModule
import com.company.ipcamera.server.routes.analyticsMetricsRoutes
import com.company.ipcamera.server.routes.digestAuthRoutes
import com.company.ipcamera.server.routes.discoveryRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.getKoin

fun Application.configureRouting() {
    routing {
        route("/api/v1") {
            // Публичные маршруты (не требуют аутентификации)
            authRoutes()
            healthRoutes()
            clusterRoutes()
            securityRoutes() // CSP reports endpoint

            // ONVIF Event Service маршруты (публичный endpoint для приема событий от камер)
            onvifEventRoutes()

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
            analyticsRoutes()
            analyticsMetricsRoutes(
                videoAnalyticsService = getKoin().get(),
                productionMonitor = getKoin().get()
            )
            faceGalleryRoutes()
            reportRoutes()
            databaseRoutes()
            auditRoutes()
            securityMonitoringRoutes()
            syncRoutes()
            cloudStorageRoutes()
            
            // Motion Detection routes
            motionRoutes(
                motionDetectorService = getKoin().get(),
                motionConfigRepository = getKoin().get(),
                motionEventRepository = getKoin().get()
            )
            
            // RTSP Benchmark routes
            rtspBenchmarkRoutes(getKoin().get())

            // Swagger UI / OpenAPI
            swaggerRoutes()

            // Digest Authentication
            digestAuthRoutes()

            // WS-Discovery / UPnP Discovery
            discoveryRoutes()
        }
    }
}



