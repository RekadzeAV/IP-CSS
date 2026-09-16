package com.company.ipcamera.server.integration

import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.middleware.configureExceptionHandling
import com.company.ipcamera.server.routing.auditRoutes
import com.company.ipcamera.server.security.AuditLogRepository
import com.company.ipcamera.server.security.InMemoryAuditLogRepository
import com.company.ipcamera.server.security.SecurityEvent
import com.company.ipcamera.server.security.SecurityEventSeverity
import com.company.ipcamera.server.security.SecurityEventType
import com.company.ipcamera.shared.domain.model.UserRole
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
import kotlinx.serialization.json.Json
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuditRoutesIntegrationTest {

    @Test
    fun `GET audit rejects non-admin token`() = testApplication {
        val repository = InMemoryAuditLogRepository()
        application {
            stopKoin()
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            configureExceptionHandling()
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(module { single<AuditLogRepository> { repository } })
            }
            routing {
                route("/api/v1") {
                    auditRoutes()
                }
            }
        }

        val viewerToken = JwtConfig.generateAccessToken(
            userId = "viewer-user",
            username = "viewer",
            role = UserRole.VIEWER.name,
            permissions = listOf("*")
        )
        val response = client.get("/api/v1/audit") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `GET audit returns filtered events for admin`() = testApplication {
        val repository = InMemoryAuditLogRepository()
        val now = System.currentTimeMillis()
        repository.append(
            SecurityEvent(
                type = SecurityEventType.LOGIN_SUCCESS,
                severity = SecurityEventSeverity.INFO,
                userId = "admin-user",
                username = "admin",
                ipAddress = "127.0.0.1",
                details = mapOf("reason" to "ok"),
                timestamp = now
            )
        )
        repository.append(
            SecurityEvent(
                type = SecurityEventType.LOGIN_FAILURE,
                severity = SecurityEventSeverity.WARNING,
                userId = "operator-user",
                username = "operator",
                ipAddress = "127.0.0.2",
                details = mapOf("reason" to "invalid_password"),
                timestamp = now + 1
            )
        )

        application {
            stopKoin()
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            configureExceptionHandling()
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(module { single<AuditLogRepository> { repository } })
            }
            routing {
                route("/api/v1") {
                    auditRoutes()
                }
            }
        }

        val adminToken = JwtConfig.generateAccessToken(
            userId = "admin-user",
            username = "admin",
            role = UserRole.ADMIN.name,
            permissions = listOf("*")
        )
        val response = client.get("/api/v1/audit?type=LOGIN_FAILURE&limit=10&offset=0") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"success\":true"), body)
        assertTrue(body.contains("LOGIN_FAILURE"), body)
        assertTrue(!body.contains("LOGIN_SUCCESS"), body)
    }

    @Test
    fun `GET audit integrity returns true for in-memory repository`() = testApplication {
        val repository = InMemoryAuditLogRepository()
        application {
            stopKoin()
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            configureExceptionHandling()
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(module { single<AuditLogRepository> { repository } })
            }
            routing {
                route("/api/v1") {
                    auditRoutes()
                }
            }
        }

        val adminToken = JwtConfig.generateAccessToken(
            userId = "admin-user",
            username = "admin",
            role = UserRole.ADMIN.name,
            permissions = listOf("*")
        )
        val response = client.get("/api/v1/audit/integrity") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"valid\":true"), body)
    }
}
