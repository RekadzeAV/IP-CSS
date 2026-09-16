package com.company.ipcamera.server.routing

import com.auth0.jwt.JWT
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.service.AnalyticsWebhookService
import com.company.ipcamera.server.service.WebhookDeliveryLogEntry
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.LicensePlateRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsRoutesWebhookSummaryTest {

    @Test
    fun `summary route returns grouped webhook stats`() = testApplication {
        val webhookService = AnalyticsWebhookService(logRetentionMs = 60_000L)
        val now = System.currentTimeMillis()
        listOf(
            WebhookDeliveryLogEntry(now - 5_000, "rule-a", "cam-1", "u", 1, 3, true, 200, null),
            WebhookDeliveryLogEntry(now - 4_000, "rule-a", "cam-1", "u", 2, 3, false, 500, "fail"),
            WebhookDeliveryLogEntry(now - 3_000, "rule-b", "cam-2", "u", 1, 3, true, 200, null)
        ).forEach { webhookService.appendLogForTests(it) }

        application { configureAnalyticsSummaryTestApp(webhookService) }
        val token = createTestToken("viewer-1", UserRole.VIEWER)

        val response = client.get("/api/v1/analytics/rules/webhook-deliveries/summary?limit=10") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(true, payload["success"]?.jsonPrimitive?.content?.toBooleanStrictOrNull())

        val data = payload["data"]!!.jsonArray
        assertEquals(2, data.size)

        val first = data[0].jsonObject
        assertEquals("rule-a", first["ruleId"]?.jsonPrimitive?.content)
        assertEquals("cam-1", first["cameraId"]?.jsonPrimitive?.content)
        assertEquals(2, first["totalAttempts"]?.jsonPrimitive?.content?.toInt())
        assertEquals(1, first["successfulAttempts"]?.jsonPrimitive?.content?.toInt())
        assertEquals(1, first["failedAttempts"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `summary route returns bad request for invalid limit`() = testApplication {
        val webhookService = AnalyticsWebhookService(logRetentionMs = 60_000L)
        application { configureAnalyticsSummaryTestApp(webhookService) }
        val token = createTestToken("viewer-1", UserRole.VIEWER)

        val response = client.get("/api/v1/analytics/rules/webhook-deliveries/summary?limit=0") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("limit"))
    }

    private fun Application.configureAnalyticsSummaryTestApp(
        webhookService: AnalyticsWebhookService
    ) {
        stopKoin()
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
                    single { webhookService }
                    single<CameraRepository> { mockk(relaxed = true) }
                    single<LicensePlateRepository> { mockk(relaxed = true) }
                }
            )
        }
        routing {
            route("/api/v1") {
                analyticsRoutes()
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
