package com.company.ipcamera.server.integration

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.config.OnvifEventsConfig
import com.company.ipcamera.server.routing.cameraRoutes
import com.company.ipcamera.server.service.ExportService
import com.company.ipcamera.server.service.OnvifEventSubscriptionService
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.CameraRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraRoutesAuthIntegrationTest {

    @Test
    fun `GET cameras rejects request without JWT`() = testApplication {
        application {
            stopKoin()
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) {
                            JWTPrincipal(credential.payload)
                        } else {
                            null
                        }
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<CameraRepository> { mockk(relaxed = true) }
                        single { ExportService() }
                        single<OnvifEventSubscriptionService> { mockk(relaxed = true) }
                        single { OnvifEventsConfig(enabled = false) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    cameraRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET cameras returns list for viewer JWT`() = testApplication {
        val cameraRepository = mockk<CameraRepository>()
        coEvery { cameraRepository.getCameras() } returns listOf(
            Camera(
                id = "cam-1",
                name = "Front door",
                url = "rtsp://example.local/stream",
                status = CameraStatus.ONLINE
            )
        )

        application {
            stopKoin()
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) {
                            JWTPrincipal(credential.payload)
                        } else {
                            null
                        }
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<CameraRepository> { cameraRepository }
                        single { ExportService() }
                        single<OnvifEventSubscriptionService> { mockk(relaxed = true) }
                        single { OnvifEventsConfig(enabled = false) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    cameraRoutes()
                }
            }
        }

        val viewerToken = JwtConfig.generateAccessToken(
            userId = "viewer-user",
            username = "viewer",
            role = UserRole.VIEWER.name,
            permissions = listOf("*")
        )
        val response = client.get("/api/v1/cameras") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"success\":true"), body)
        assertTrue(body.contains("Front door"), body)
    }

    @Test
    fun `POST cameras accepts rtsp url for operator JWT`() = testApplication {
        val cameraRepository = mockk<CameraRepository>()
        coEvery { cameraRepository.addCamera(any()) } answers {
            val camera = firstArg<Camera>()
            Result.success(camera.copy(status = CameraStatus.UNKNOWN))
        }

        application {
            stopKoin()
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) {
                            JWTPrincipal(credential.payload)
                        } else {
                            null
                        }
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<CameraRepository> { cameraRepository }
                        single { ExportService() }
                        single<OnvifEventSubscriptionService> { mockk(relaxed = true) }
                        single { OnvifEventsConfig(enabled = false) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    cameraRoutes()
                }
            }
        }

        val operatorToken = JwtConfig.generateAccessToken(
            userId = "operator-user",
            username = "operator",
            role = UserRole.OPERATOR.name,
            permissions = listOf("*")
        )
        val response = client.post("/api/v1/cameras") {
            header(HttpHeaders.Authorization, "Bearer $operatorToken")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "RTSP Cam",
                  "url": "rtsp://example.com:554/stream",
                  "username": "admin",
                  "password": "secret"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"success\":true"), body)
        assertTrue(body.contains("rtsp://example.com:554/stream"), body)
    }
}
