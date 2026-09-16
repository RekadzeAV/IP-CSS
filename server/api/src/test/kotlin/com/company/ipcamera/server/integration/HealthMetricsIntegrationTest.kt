package com.company.ipcamera.server.integration

import com.company.ipcamera.server.cluster.ClusterService
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.routing.healthRoutes
import com.company.ipcamera.server.service.ApiMetricsService
import com.company.ipcamera.server.service.CameraService
import com.company.ipcamera.server.service.FfmpegService
import com.company.ipcamera.server.service.StorageService
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
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthMetricsIntegrationTest {
    @Test
    fun `GET health metrics returns discover and auth counters for admin`() = testApplication {
        val metricsService = ApiMetricsService().apply {
            markDiscoverRequest(usedFallback = true)
            markLoginSuccess()
            markRefreshSuccess()
            markWsTokenSuccess()
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
                        single<FfmpegService> { mockk(relaxed = true) }
                        single<StorageService> {
                            mockk {
                                every { getStorageInfo() } returns StorageService.StorageInfo(
                                    totalBytes = 1L,
                                    usedBytes = 1L,
                                    availableBytes = 1L,
                                    usagePercentage = 0.0,
                                    warningThresholdExceeded = false,
                                    quotaBytes = null,
                                    quotaUsedBytes = null,
                                    quotaRemainingBytes = null
                                )
                            }
                        }
                        single<CameraService> { mockk(relaxed = true) }
                        single<ClusterService> { mockk(relaxed = true) }
                        single { metricsService }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    healthRoutes()
                }
            }
        }

        val adminToken = JwtConfig.generateAccessToken(
            userId = "admin-user",
            username = "admin",
            role = "ADMIN",
            permissions = listOf("*")
        )
        val response = client.get("/api/v1/health/metrics") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"discoverRequests\":1"), body)
        assertTrue(body.contains("\"discoverFallbackUsed\":1"), body)
        assertTrue(body.contains("\"authLoginSuccess\":1"), body)
        assertTrue(body.contains("\"authRefreshSuccess\":1"), body)
        assertTrue(body.contains("\"authWsTokenSuccess\":1"), body)
    }
}
