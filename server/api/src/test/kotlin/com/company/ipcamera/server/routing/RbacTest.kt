package com.company.ipcamera.server.routing

import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.middleware.AuthorizationMiddleware
import com.company.ipcamera.shared.domain.model.UserRole
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.testing.*
import io.ktor.server.routing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Тесты для проверки RBAC (Role-Based Access Control) в маршрутах
 */
class RbacTest {

    /**
     * Создает JWT токен для тестирования
     */
    private fun createTestToken(userId: String, role: UserRole): String {
        val algorithm = Algorithm.HMAC256(JwtConfig.secret)
        return JWT.create()
            .withSubject(userId)
            .withClaim("role", role.name)
            .withClaim("permissions", listOf("*"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3600000)) // 1 час
            .sign(algorithm)
    }

    @Test
    fun `test GET cameras requires VIEWER role`() = testApplication {
        application {
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    route("/api/v1") {
                        cameraRoutes()
                    }
                }
            }
        }

        // Тест с VIEWER ролью - должен пройти
        val viewerToken = createTestToken("viewer-user", UserRole.VIEWER)
        val response = client.get("/api/v1/cameras") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)

        // Тест с GUEST ролью - должен быть запрещен
        val guestToken = createTestToken("guest-user", UserRole.GUEST)
        val guestResponse = client.get("/api/v1/cameras") {
            header(HttpHeaders.Authorization, "Bearer $guestToken")
        }
        assertEquals(HttpStatusCode.Forbidden, guestResponse.status)
    }

    @Test
    fun `test POST cameras requires OPERATOR role`() = testApplication {
        application {
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    route("/api/v1") {
                        cameraRoutes()
                    }
                }
            }
        }

        // Тест с OPERATOR ролью - должен пройти
        val operatorToken = createTestToken("operator-user", UserRole.OPERATOR)
        val response = client.post("/api/v1/cameras") {
            header(HttpHeaders.Authorization, "Bearer $operatorToken")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Test Camera","url":"rtsp://test.com"}""")
        }
        // Может быть BadRequest из-за валидации, но не Forbidden
        assert(response.status != HttpStatusCode.Forbidden) {
            "OPERATOR should have access to POST /cameras"
        }

        // Тест с VIEWER ролью - должен быть запрещен
        val viewerToken = createTestToken("viewer-user", UserRole.VIEWER)
        val viewerResponse = client.post("/api/v1/cameras") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Test Camera","url":"rtsp://test.com"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, viewerResponse.status)
    }

    @Test
    fun `test GET events requires VIEWER role`() = testApplication {
        application {
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    route("/api/v1") {
                        eventRoutes()
                    }
                }
            }
        }

        // Тест с VIEWER ролью - должен пройти
        val viewerToken = createTestToken("viewer-user", UserRole.VIEWER)
        val response = client.get("/api/v1/events") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)

        // Тест без токена - должен быть Unauthorized
        val noAuthResponse = client.get("/api/v1/events")
        assertEquals(HttpStatusCode.Unauthorized, noAuthResponse.status)
    }

    @Test
    fun `test DELETE events requires OPERATOR role`() = testApplication {
        application {
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    route("/api/v1") {
                        eventRoutes()
                    }
                }
            }
        }

        // Тест с OPERATOR ролью - должен пройти (или NotFound если события нет)
        val operatorToken = createTestToken("operator-user", UserRole.OPERATOR)
        val response = client.delete("/api/v1/events/test-id") {
            header(HttpHeaders.Authorization, "Bearer $operatorToken")
        }
        // Может быть NotFound, но не Forbidden
        assert(response.status != HttpStatusCode.Forbidden) {
            "OPERATOR should have access to DELETE /events/{id}"
        }

        // Тест с VIEWER ролью - должен быть запрещен
        val viewerToken = createTestToken("viewer-user", UserRole.VIEWER)
        val viewerResponse = client.delete("/api/v1/events/test-id") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        assertEquals(HttpStatusCode.Forbidden, viewerResponse.status)
    }

    @Test
    fun `test GET recordings requires VIEWER role`() = testApplication {
        application {
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    route("/api/v1") {
                        recordingRoutes()
                    }
                }
            }
        }

        // Тест с VIEWER ролью - должен пройти
        val viewerToken = createTestToken("viewer-user", UserRole.VIEWER)
        val response = client.get("/api/v1/recordings") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test POST recordings start requires OPERATOR role`() = testApplication {
        application {
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    route("/api/v1") {
                        recordingRoutes()
                    }
                }
            }
        }

        // Тест с OPERATOR ролью - должен пройти (или BadRequest из-за валидации)
        val operatorToken = createTestToken("operator-user", UserRole.OPERATOR)
        val response = client.post("/api/v1/recordings/start") {
            header(HttpHeaders.Authorization, "Bearer $operatorToken")
            contentType(ContentType.Application.Json)
            setBody("""{"cameraId":"test-camera"}""")
        }
        // Может быть BadRequest, но не Forbidden
        assert(response.status != HttpStatusCode.Forbidden) {
            "OPERATOR should have access to POST /recordings/start"
        }

        // Тест с VIEWER ролью - должен быть запрещен
        val viewerToken = createTestToken("viewer-user", UserRole.VIEWER)
        val viewerResponse = client.post("/api/v1/recordings/start") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"cameraId":"test-camera"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, viewerResponse.status)
    }

    @Test
    fun `test role hierarchy - ADMIN has access to all operations`() = testApplication {
        application {
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    route("/api/v1") {
                        cameraRoutes()
                        eventRoutes()
                        recordingRoutes()
                    }
                }
            }
        }

        val adminToken = createTestToken("admin-user", UserRole.ADMIN)

        // ADMIN должен иметь доступ ко всем операциям
        val camerasResponse = client.get("/api/v1/cameras") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, camerasResponse.status)

        val eventsResponse = client.get("/api/v1/events") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, eventsResponse.status)

        val recordingsResponse = client.get("/api/v1/recordings") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, recordingsResponse.status)
    }

    /**
     * Настройка аутентификации для тестов
     */
    private fun Application.configureAuth() {
        install(Authentication) {
            jwt("jwt-auth") {
                realm = "IP-CSS"
                verifier(
                    JwtConfig.createVerifier()
                )
                validate { credential ->
                    if (credential.payload.subject.isNotBlank()) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }
    }
}


