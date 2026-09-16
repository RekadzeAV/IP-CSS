package com.company.ipcamera.server.routing

import com.auth0.jwt.JWT
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.middleware.configureExceptionHandling
import com.company.ipcamera.server.service.PushTokenService
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.usecase.GetNotificationsUseCase
import com.company.ipcamera.shared.domain.usecase.MarkNotificationAsReadUseCase
import io.ktor.client.request.delete
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
import io.ktor.server.application.Application
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

class NotificationRoutesPushTokensTest {
    @Test
    fun `register list and revoke push token`() = testApplication {
        val pushTokenService = PushTokenService()
        application { configurePushTokenTestApp(pushTokenService) }
        val token = createTestToken("user-push-1", UserRole.OPERATOR)

        val register = client.post("/api/v1/notifications/push-tokens") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"token":"push-token-1","platform":"android"}""")
        }
        assertEquals(HttpStatusCode.OK, register.status)

        val list = client.get("/api/v1/notifications/push-tokens") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, list.status)
        assertTrue(list.bodyAsText().contains("\"token\":\"push-token-1\""))

        val revoke = client.delete("/api/v1/notifications/push-tokens/push-token-1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, revoke.status)
    }

    @Test
    fun `register push token validates blank token`() = testApplication {
        val pushTokenService = PushTokenService()
        application { configurePushTokenTestApp(pushTokenService) }
        val token = createTestToken("user-push-2", UserRole.VIEWER)

        val register = client.post("/api/v1/notifications/push-tokens") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"token":"   ","platform":"ios"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, register.status)
    }

    @Test
    fun `revoke invalid push tokens removes multiple tokens`() = testApplication {
        val pushTokenService = PushTokenService().apply {
            register(userId = "user-push-3", token = "t-a", platform = "android")
            register(userId = "user-push-3", token = "t-b", platform = "ios")
            register(userId = "user-push-3", token = "t-c", platform = "android")
        }
        application { configurePushTokenTestApp(pushTokenService) }
        val token = createTestToken("user-push-3", UserRole.OPERATOR)

        val revoke = client.post("/api/v1/notifications/push-tokens/revoke-invalid") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"tokens":["t-a","t-c"]}""")
        }
        assertEquals(HttpStatusCode.OK, revoke.status)
        assertTrue(revoke.bodyAsText().contains("\"removed\":2"))

        val list = client.get("/api/v1/notifications/push-tokens") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, list.status)
        val body = list.bodyAsText()
        assertTrue(body.contains("\"token\":\"t-b\""))
        assertTrue(!body.contains("\"token\":\"t-a\""))
        assertTrue(!body.contains("\"token\":\"t-c\""))
    }

    private fun Application.configurePushTokenTestApp(pushTokenService: PushTokenService) {
        stopKoin()
        configureExceptionHandling()
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
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
            modules(
                module {
                    single { pushTokenService }
                    single<GetNotificationsUseCase> { mockk(relaxed = true) }
                    single<MarkNotificationAsReadUseCase> { mockk(relaxed = true) }
                }
            )
        }
        routing {
            route("/api/v1") {
                notificationRoutes()
            }
        }
    }

    private fun createTestToken(userId: String, role: UserRole): String {
        return JWT.create()
            .withIssuer("ip-camera-server")
            .withAudience("ip-camera-client")
            .withSubject(userId)
            .withClaim("role", role.name)
            .withClaim("permissions", listOf("*"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3600000))
            .sign(JwtConfig.algorithm)
    }
}
