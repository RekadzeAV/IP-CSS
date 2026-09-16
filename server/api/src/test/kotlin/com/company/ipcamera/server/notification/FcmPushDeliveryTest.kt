package com.company.ipcamera.server.notification

import com.company.ipcamera.server.service.PushTokenPurger
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import java.security.KeyPairGenerator
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FcmPushDeliveryTest {

    private class RecordingPurger : PushTokenPurger {
        val purged = mutableListOf<String>()
        override fun purgeTokens(tokens: Collection<String>): Int {
            purged.addAll(tokens)
            return tokens.size
        }
    }

    private fun notification() = Notification(
        id = "n-1",
        title = "Motion",
        message = "Motion detected",
        type = NotificationType.EVENT,
        priority = NotificationPriority.HIGH,
        extras = mapOf("cameraId" to "cam-1")
    )

    /** RSA-пара → PKCS#8 PEM (как в FcmNotificationSenderTest). */
    private fun generatePem(): String {
        val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val pkcs8 = Base64.getEncoder().encodeToString(kp.private.encoded)
        return "-----BEGIN PRIVATE KEY-----\n$pkcs8\n-----END PRIVATE KEY-----\n"
    }

    private fun serviceAccountJson(pem: String): String = """
        {
          "type": "service_account",
          "project_id": "ipcss-test",
          "private_key_id": "key123",
          "private_key": "${pem.replace("\n", "\\n")}",
          "client_email": "push@ipcss-test.iam.gserviceaccount.com",
          "token_uri": "https://oauth2.googleapis.com/token"
        }
    """.trimIndent()

    private fun mockSender(
        tokenResponses: Map<String, HttpStatusCode> = emptyMap(),
        defaultStatus: HttpStatusCode = HttpStatusCode.OK,
        fcmCalls: MutableList<String> = mutableListOf()
    ): FcmNotificationSender {
        val pem = generatePem()
        val engine = MockEngine { request ->
            when (request.url.host) {
                "oauth2.googleapis.com" ->
                    respond("""{"access_token":"ya29.test","expires_in":3600}""", HttpStatusCode.OK)
                "fcm.googleapis.com" -> {
                    val body = (request.body as io.ktor.http.content.TextContent).text
                    val token = Regex("""\"token\":\"([^\"]+)\"""").find(body)?.groupValues?.get(1).orEmpty()
                    fcmCalls += token
                    val status = tokenResponses[token] ?: defaultStatus
                    respond("""{"name":"projects/p/messages/1"}""", status)
                }
                else -> respond("unexpected", HttpStatusCode.NotFound)
            }
        }
        return FcmNotificationSender(
            config = FcmConfig(enabled = true, projectId = "ipcss-test", serviceAccountJson = serviceAccountJson(pem)),
            retryDelayMs = 1L,
            client = HttpClient(engine)
        )
    }

    // -------------------------------------------------------------------------
    // Тесты
    // -------------------------------------------------------------------------

    @Test
    fun `send delivers to all tokens on success`() = runBlocking {
        val calls = mutableListOf<String>()
        val sender = mockSender(fcmCalls = calls)
        val delivery = FcmPushDelivery(sender)

        val result = delivery.send(listOf("tok-1", "tok-2"), notification())

        assertTrue(result.isSuccess)
        assertEquals(listOf("tok-1", "tok-2"), calls)
    }

    @Test
    fun `send purges token invalidated with 410`() = runBlocking {
        val purger = RecordingPurger()
        val sender = mockSender(tokenResponses = mapOf("dead" to HttpStatusCode.Gone))
        val delivery = FcmPushDelivery(sender, purger)

        val result = delivery.send(listOf("dead"), notification())

        assertEquals(listOf("dead"), purger.purged, "мёртвый токен должен быть удалён")
        assertTrue(result.isFailure, "единственный токен невалиден -> failure")
    }

    @Test
    fun `send purges dead token and delivers to alive ones`() = runBlocking {
        val purger = RecordingPurger()
        val calls = mutableListOf<String>()
        val sender = mockSender(tokenResponses = mapOf("dead" to HttpStatusCode.Gone), fcmCalls = calls)
        val delivery = FcmPushDelivery(sender, purger)

        val result = delivery.send(listOf("dead", "alive"), notification())

        assertEquals(listOf("dead"), purger.purged)
        assertEquals(listOf("dead", "alive"), calls, "после мёртвого токена остальные должны доставляться")
        assertTrue(result.isSuccess, "есть доставленные токены -> success")
    }

    @Test
    fun `send does not purge on transient 500`() = runBlocking {
        val purger = RecordingPurger()
        val sender = mockSender(defaultStatus = HttpStatusCode.InternalServerError)
        val delivery = FcmPushDelivery(sender, purger)

        val result = delivery.send(listOf("tok-1"), notification())

        assertTrue(purger.purged.isEmpty(), "временная ошибка не удаляет токен")
        assertTrue(result.isFailure)
    }

    @Test
    fun `send works without purger (no NPE)`() = runBlocking {
        val sender = mockSender(tokenResponses = mapOf("dead" to HttpStatusCode.NotFound))
        val delivery = FcmPushDelivery(sender, tokenPurger = null)

        val result = delivery.send(listOf("dead"), notification())

        assertTrue(result.isFailure)
    }

    @Test
    fun `empty token list is a no-op success`() = runBlocking {
        val delivery = FcmPushDelivery(mockSender())
        val result = delivery.send(emptyList(), notification())
        assertTrue(result.isSuccess)
    }
}
