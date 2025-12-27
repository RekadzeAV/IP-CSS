package com.company.ipcamera.server

import com.auth0.jwt.JWT
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.di.appModule
import com.company.ipcamera.server.middleware.configureCookieAuth
import com.company.ipcamera.server.middleware.configureSecurityHeaders
import com.company.ipcamera.server.service.CameraService
import com.company.ipcamera.server.websocket.configureWebSocket
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Koin DI
    install(Koin) {
        modules(appModule)
    }

    // Content Negotiation
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // CORS - ИСПРАВЛЕНО: убран anyHost(), добавлен whitelist доменов
    install(CORS) {
        // Получаем разрешенные домены из переменной окружения или используем дефолтные
        val allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS")
            ?.split(",")
            ?.map { it.trim() }
            ?: listOf("http://localhost:3000", "http://localhost:8080")

        allowedOrigins.forEach { origin ->
            allowHost(origin, schemes = listOf("http", "https"))
        }

        allowHeader("Content-Type")
        allowHeader("Authorization")
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowCredentials = true
    }

    // Security Headers
    configureSecurityHeaders()

    // Cookie Auth Middleware - читает токен из cookie и устанавливает в заголовок Authorization
    configureCookieAuth()

    // JWT Authentication
    install(Authentication) {
        jwt("jwt-auth") {
            realm = JwtConfig.realm
            verifier(JwtConfig.createVerifier())
            // Токен теперь может быть в заголовке Authorization (установлен middleware из cookie)
            challenge { defaultScheme, realm ->
                call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Token is invalid or expired")
            }
            validate { credential ->
                if (credential.payload.issuer == "ip-camera-server" &&
                    credential.payload.audience.contains("ip-camera-client")) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    // Call Logging
    install(CallLogging) {
        level = Level.INFO
    }

    // Routing
    configureRouting()

    // WebSocket
    configureWebSocket()

    // Start Camera Service monitoring
    val cameraService: CameraService by inject()
    cameraService.startMonitoring()
}

