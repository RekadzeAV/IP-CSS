package com.company.ipcamera.server.routing

import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.middleware.configureExceptionHandling
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireRole
import com.company.ipcamera.shared.domain.model.UserRole
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Тесты RBAC на минимальных защищенных маршрутах. */
class RbacTest {

    /**
     * Создает JWT токен для тестирования
     */
    private fun createTestToken(userId: String, role: UserRole): String {
        return JwtConfig.generateAccessToken(
            userId = userId,
            username = "test-$userId",
            role = role.name,
            permissions = listOf("*")
        )
    }

    @Test
    fun `viewer route allows viewer and blocks guest`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            configureExceptionHandling()
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    get("/secure/viewer") {
                        requireRole(UserRole.VIEWER)
                        call.respondText("ok")
                    }
                }
            }
        }

        val viewerToken = createTestToken("viewer-user", UserRole.VIEWER)
        val viewerResponse = client.get("/secure/viewer") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        assertEquals(HttpStatusCode.OK, viewerResponse.status)

        val guestToken = createTestToken("guest-user", UserRole.GUEST)
        val guestResponse = client.get("/secure/viewer") {
            header(HttpHeaders.Authorization, "Bearer $guestToken")
        }
        assertEquals(HttpStatusCode.Forbidden, guestResponse.status)
    }

    @Test
    fun `operator route allows operator and blocks viewer`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            configureExceptionHandling()
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    post("/secure/operator") {
                        requireRole(UserRole.OPERATOR)
                        call.respondText("ok")
                    }
                }
            }
        }

        val operatorToken = createTestToken("operator-user", UserRole.OPERATOR)
        val operatorResponse = client.post("/secure/operator") {
            header(HttpHeaders.Authorization, "Bearer $operatorToken")
            setBody("{}")
        }
        assertTrue(
            operatorResponse.status != HttpStatusCode.Forbidden,
            "OPERATOR should pass OPERATOR gate"
        )

        val viewerToken = createTestToken("viewer-user", UserRole.VIEWER)
        val viewerResponse = client.post("/secure/operator") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            setBody("{}")
        }
        assertEquals(HttpStatusCode.Forbidden, viewerResponse.status)
    }

    @Test
    fun `admin can access all protected routes`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            configureExceptionHandling()
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    get("/secure/viewer") {
                        requireRole(UserRole.VIEWER)
                        call.respondText("ok")
                    }
                    delete("/secure/operator") {
                        requireRole(UserRole.OPERATOR)
                        call.respondText("ok")
                    }
                }
            }
        }

        val adminToken = createTestToken("admin-user", UserRole.ADMIN)
        val viewerRouteResponse = client.get("/secure/viewer") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, viewerRouteResponse.status)

        val operatorRouteResponse = client.delete("/secure/operator") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, operatorRouteResponse.status)
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


