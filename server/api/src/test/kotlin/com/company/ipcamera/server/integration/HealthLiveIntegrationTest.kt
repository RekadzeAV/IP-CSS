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
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Минимальный интеграционный smoke: публичный liveness без Redis/PostgreSQL.
 * Koin предоставляет заглушки для сервисов, которые [healthRoutes] резолвит через inject()
 * (даже если конкретный маршрут их не вызывает — безопаснее явно зарегистрировать).
 */
class HealthLiveIntegrationTest {

    @Test
    fun `GET api v1 health live returns OK and ALIVE`() = testApplication {
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
                        single<StorageService> { mockk(relaxed = true) }
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

        val response = client.get("/api/v1/health/live")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("ALIVE"), body)
        assertTrue(body.contains("\"success\""), body)
    }
}
