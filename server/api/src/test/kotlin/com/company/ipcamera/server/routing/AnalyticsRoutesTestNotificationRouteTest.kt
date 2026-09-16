package com.company.ipcamera.server.routing

import com.auth0.jwt.JWT
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.middleware.configureExceptionHandling
import com.company.ipcamera.server.service.AnalyticsRuleService
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.LicensePlateRepository
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.put
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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsRoutesTestNotificationRouteTest {

    @Test
    fun `test notification route returns success payload`() = testApplication {
        val ruleService = mockk<AnalyticsRuleService>()
        coEvery {
            ruleService.sendTestNotification(
                ruleId = "rule-1",
                userId = null,
                cameraIdOverride = null,
                notifyInApp = true,
                notifyEmail = false,
                notifyTelegram = true
            )
        } returns Result.success(
            AnalyticsRuleService.TestNotificationResult(
                ruleId = "rule-1",
                notificationId = "notification-1",
                channels = listOf("in-app", "telegram")
            )
        )

        application { configureTestApp(ruleService) }
        val token = createTestToken("op-1", UserRole.OPERATOR)
        val response = client.post("/api/v1/analytics/rules/rule-1/test-notification") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"notifyInApp":true,"notifyEmail":false,"notifyTelegram":true}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"ruleId\":\"rule-1\""))
        assertTrue(body.contains("\"notificationId\":\"notification-1\""))
        assertTrue(body.contains("\"channels\":[\"in-app\",\"telegram\"]"))
    }

    @Test
    fun `test notification route returns bad request when no channel selected`() = testApplication {
        val ruleService = mockk<AnalyticsRuleService>()
        coEvery {
            ruleService.sendTestNotification(
                ruleId = "rule-2",
                userId = null,
                cameraIdOverride = null,
                notifyInApp = false,
                notifyEmail = false,
                notifyTelegram = false
            )
        } returns Result.failure(
            IllegalArgumentException("At least one notification channel must be enabled for test delivery")
        )

        application { configureTestApp(ruleService) }
        val token = createTestToken("op-1", UserRole.OPERATOR)
        val response = client.post("/api/v1/analytics/rules/rule-2/test-notification") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"notifyInApp":false,"notifyEmail":false,"notifyTelegram":false}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("At least one notification channel must be enabled"))
    }

    @Test
    fun `viewer cannot call test notification route`() = testApplication {
        val ruleService = mockk<AnalyticsRuleService>(relaxed = true)
        application { configureTestApp(ruleService) }
        val token = createTestToken("viewer-1", UserRole.VIEWER)

        val response = client.post("/api/v1/analytics/rules/rule-1/test-notification") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"notifyInApp":true}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        coVerify(exactly = 0) { ruleService.sendTestNotification(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `operator can enable and disable rule`() = testApplication {
        val ruleService = mockk<AnalyticsRuleService>()
        coEvery { ruleService.setRuleEnabled("rule-3", true) } returns Result.success(Unit)
        coEvery { ruleService.setRuleEnabled("rule-3", false) } returns Result.success(Unit)

        application { configureTestApp(ruleService) }
        val token = createTestToken("op-1", UserRole.OPERATOR)

        val enableResponse = client.post("/api/v1/analytics/rules/rule-3/enable") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val disableResponse = client.post("/api/v1/analytics/rules/rule-3/disable") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, enableResponse.status)
        assertEquals(HttpStatusCode.OK, disableResponse.status)
        coVerify(exactly = 1) { ruleService.setRuleEnabled("rule-3", true) }
        coVerify(exactly = 1) { ruleService.setRuleEnabled("rule-3", false) }
    }

    @Test
    fun `viewer cannot enable or disable rule`() = testApplication {
        val ruleService = mockk<AnalyticsRuleService>(relaxed = true)
        application { configureTestApp(ruleService) }
        val token = createTestToken("viewer-1", UserRole.VIEWER)

        val enableResponse = client.post("/api/v1/analytics/rules/rule-4/enable") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val disableResponse = client.post("/api/v1/analytics/rules/rule-4/disable") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.Forbidden, enableResponse.status)
        assertEquals(HttpStatusCode.Forbidden, disableResponse.status)
        coVerify(exactly = 0) { ruleService.setRuleEnabled(any(), any()) }
    }

    @Test
    fun `viewer can read notification policy`() = testApplication {
        val ruleService = mockk<AnalyticsRuleService>()
        coEvery { ruleService.getNotificationPolicy("rule-5") } returns Result.success(
            AnalyticsRuleService.RuleNotificationPolicy(
                ruleId = "rule-5",
                sendNotification = true,
                notifyInApp = true,
                notifyEmail = false,
                notifyTelegram = true,
                notificationType = com.company.ipcamera.shared.domain.model.NotificationType.INFO
            )
        )
        application { configureTestApp(ruleService) }
        val token = createTestToken("viewer-1", UserRole.VIEWER)

        val response = client.get("/api/v1/analytics/rules/rule-5/notification-policy") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"ruleId\":\"rule-5\""))
        assertTrue(response.bodyAsText().contains("\"notifyTelegram\":true"))
    }

    @Test
    fun `operator can update notification policy`() = testApplication {
        val ruleService = mockk<AnalyticsRuleService>()
        coEvery {
            ruleService.updateNotificationPolicy(
                ruleId = "rule-6",
                sendNotification = true,
                notifyInApp = false,
                notifyEmail = true,
                notifyTelegram = false,
                notificationType = com.company.ipcamera.shared.domain.model.NotificationType.WARNING
            )
        } returns Result.success(
            AnalyticsRuleService.RuleNotificationPolicy(
                ruleId = "rule-6",
                sendNotification = true,
                notifyInApp = false,
                notifyEmail = true,
                notifyTelegram = false,
                notificationType = com.company.ipcamera.shared.domain.model.NotificationType.WARNING
            )
        )
        application { configureTestApp(ruleService) }
        val token = createTestToken("op-1", UserRole.OPERATOR)

        val response = client.put("/api/v1/analytics/rules/rule-6/notification-policy") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"sendNotification":true,"notifyInApp":false,"notifyEmail":true,"notifyTelegram":false,"notificationType":"WARNING"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"notifyEmail\":true"))
        coVerify(exactly = 1) {
            ruleService.updateNotificationPolicy(
                ruleId = "rule-6",
                sendNotification = true,
                notifyInApp = false,
                notifyEmail = true,
                notifyTelegram = false,
                notificationType = com.company.ipcamera.shared.domain.model.NotificationType.WARNING
            )
        }
    }

    @Test
    fun `viewer cannot update notification policy`() = testApplication {
        val ruleService = mockk<AnalyticsRuleService>(relaxed = true)
        application { configureTestApp(ruleService) }
        val token = createTestToken("viewer-1", UserRole.VIEWER)

        val response = client.put("/api/v1/analytics/rules/rule-7/notification-policy") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"sendNotification":true}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        coVerify(exactly = 0) { ruleService.updateNotificationPolicy(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `operator gets bad request for invalid notification type in policy update`() = testApplication {
        val ruleService = mockk<AnalyticsRuleService>(relaxed = true)
        application { configureTestApp(ruleService) }
        val token = createTestToken("op-1", UserRole.OPERATOR)

        val response = client.put("/api/v1/analytics/rules/rule-8/notification-policy") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"sendNotification":true,"notifyEmail":true,"notificationType":"NOT_A_TYPE"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        coVerify(exactly = 0) { ruleService.updateNotificationPolicy(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `get notification policy returns not found when rule is absent`() = testApplication {
        val ruleService = mockk<AnalyticsRuleService>()
        coEvery { ruleService.getNotificationPolicy("missing-rule") } returns
            Result.failure(IllegalArgumentException("Rule not found: missing-rule"))
        application { configureTestApp(ruleService) }
        val token = createTestToken("viewer-2", UserRole.VIEWER)

        val response = client.get("/api/v1/analytics/rules/missing-rule/notification-policy") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Rule not found"))
    }

    @Test
    fun `update notification policy returns bad request when service validation fails`() = testApplication {
        val ruleService = mockk<AnalyticsRuleService>()
        coEvery {
            ruleService.updateNotificationPolicy(
                ruleId = "rule-9",
                sendNotification = false,
                notifyInApp = true,
                notifyEmail = null,
                notifyTelegram = null,
                notificationType = null
            )
        } returns Result.failure(
            IllegalArgumentException("sendNotification must be true when any notification channel is enabled")
        )
        application { configureTestApp(ruleService) }
        val token = createTestToken("op-2", UserRole.OPERATOR)

        val response = client.put("/api/v1/analytics/rules/rule-9/notification-policy") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"sendNotification":false,"notifyInApp":true}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("sendNotification must be true"))
    }

    private fun Application.configureTestApp(ruleService: AnalyticsRuleService) {
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
                    single { ruleService }
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
