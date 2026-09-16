package com.company.ipcamera.server.integration

import com.company.ipcamera.server.cluster.ClusterService
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.routing.healthRoutes
import com.company.ipcamera.server.service.CameraService
import com.company.ipcamera.server.service.FfmpegService
import com.company.ipcamera.server.service.StorageService
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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
import kotlin.test.assertTrue

/**
 * Readiness smoke: [GET /api/v1/health/ready] в не-production окружении без DB_MODE=postgres
 * (ветка database в health считается доступной без реального пула).
 */
class HealthReadyIntegrationTest {

    @Test
    fun `GET api v1 health ready returns structured readiness`() = testApplication {
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
                                    totalBytes = 1_000_000_000L,
                                    usedBytes = 1L,
                                    availableBytes = 999_999_999L,
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
                    }
                )
            }
            routing {
                route("/api/v1") {
                    healthRoutes()
                }
            }
        }

        val response = client.get("/api/v1/health/ready")
        assertTrue(
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.ServiceUnavailable,
            "unexpected status ${response.status}"
        )
        val body = response.bodyAsText()
        assertTrue(body.contains("\"checks\""), body)
        assertTrue(body.contains("\"success\""), body)
        assertTrue(body.contains("READY") || body.contains("NOT_READY"), body)
    }
}
