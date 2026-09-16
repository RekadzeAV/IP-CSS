package com.company.ipcamera.server.integration

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.config.OnvifEventsConfig
import com.company.ipcamera.server.routing.cameraRoutes
import com.company.ipcamera.server.service.ApiMetricsService
import com.company.ipcamera.server.service.ExportService
import com.company.ipcamera.server.service.OnvifEventSubscriptionService
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.ConnectionTestResult
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import com.company.ipcamera.shared.domain.repository.ErrorCode
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.io.File
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraDiscoverFallbackIntegrationTest {

    @Test
    fun `GET discover returns known-host fallback when ONVIF is empty`() = testApplication {
        val serverSocket = ServerSocket(0)
        val openPort = serverSocket.localPort
        val configFile = File.createTempFile("discover-fallback-test", ".json")
        configFile.writeText(
            """
            {
              "defaults": {
                "rtspPort": $openPort,
                "rtspPath": "/stream",
                "httpPorts": [80]
              },
              "cameras": [
                {
                  "id": "cam-local",
                  "name": "Local test camera",
                  "host": "127.0.0.1",
                  "rtspPort": $openPort,
                  "rtspPath": "/stream"
                }
              ]
            }
            """.trimIndent()
        )

        System.setProperty("DISCOVERY_FALLBACK_ENABLED", "true")
        System.setProperty("DISCOVERY_KNOWN_HOSTS_CONFIG", configFile.absolutePath)
        System.setProperty("DISCOVERY_CONNECT_TIMEOUT_MS", "200")

        try {
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
                            single<CameraRepository> { EmptyDiscoveryCameraRepository() }
                        single { ApiMetricsService() }
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

            val token = JwtConfig.generateAccessToken(
                userId = "test-user",
                username = "tester",
                role = "ADMIN",
                permissions = listOf("*")
            )
            val response = client.get("/api/v1/cameras/discover?refresh=true") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("\"success\":true"), body)
            assertTrue(body.contains("Local test camera"), body)
            assertTrue(body.contains("known-host-fallback"), body)
            assertTrue(body.contains("127.0.0.1"), body)
        } finally {
            runCatching { serverSocket.close() }
            runCatching { configFile.delete() }
            System.clearProperty("DISCOVERY_FALLBACK_ENABLED")
            System.clearProperty("DISCOVERY_KNOWN_HOSTS_CONFIG")
            System.clearProperty("DISCOVERY_CONNECT_TIMEOUT_MS")
        }
    }
}

private class EmptyDiscoveryCameraRepository : CameraRepository {
    override suspend fun getCameras(): List<Camera> = emptyList()
    override suspend fun getCameraById(id: String): Camera? = null
    override suspend fun addCamera(camera: Camera): Result<Camera> = Result.success(camera)
    override suspend fun updateCamera(camera: Camera): Result<Camera> = Result.success(camera)
    override suspend fun removeCamera(id: String): Result<Unit> = Result.success(Unit)
    override suspend fun discoverCameras(forceRefresh: Boolean): List<DiscoveredCamera> = emptyList()
    override suspend fun discoverCamerasWithProgress(
        forceRefresh: Boolean,
        config: com.company.ipcamera.shared.domain.repository.DiscoveryConfig
    ): kotlinx.coroutines.flow.Flow<com.company.ipcamera.shared.domain.repository.DiscoveryProgress> = kotlinx.coroutines.flow.emptyFlow()
    override suspend fun testConnection(camera: Camera): ConnectionTestResult =
        ConnectionTestResult.Failure("Not implemented for test", ErrorCode.UNKNOWN)

    override suspend fun getCameraStatus(id: String): CameraStatus = CameraStatus.UNKNOWN
}
