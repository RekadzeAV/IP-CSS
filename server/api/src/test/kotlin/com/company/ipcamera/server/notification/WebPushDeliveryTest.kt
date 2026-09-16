package com.company.ipcamera.server.notification

import com.company.ipcamera.server.service.PushTokenPurger
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebPushDeliveryTest {

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
        message = "Motion detected on cam-1",
        type = NotificationType.EVENT,
        priority = NotificationPriority.HIGH,
        extras = mapOf("cameraId" to "cam-1")
    )

    /** Валидная конфигурация + токен-подписка (base64url JSON) с настоящей P-256 парой. */
    private fun configAndToken(): Pair<WebPushConfig, String> {
        val vapid = WebPushConfig.generateVapidKeys()
        val config = WebPushConfig(
            enabled = true,
            publicKey = vapid.publicKey,
            privateKey = vapid.privateKey,
            subject = "mailto:test@example.com"
        )
        // Подписка браузера: настоящая P-256 пара (иначе ECDH-шифрование упадёт до HTTP).
        val browserPair = WebPushCrypto.generateVapidKeyPair()
        val subscriptionJson = Json.encodeToString(
            SubscriptionJson.serializer(),
            SubscriptionJson(
                endpoint = "https://push.example.com/send/abc",
                keys = KeysJson(
                    p256dh = WebPushCrypto.b64UrlEncode(WebPushCrypto.rawPublicKey(browserPair.public)),
                    auth = WebPushCrypto.b64UrlEncode(ByteArray(16) { (it * 13).toByte() })
                )
            )
        )
        val token = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(subscriptionJson.toByteArray(Charsets.UTF_8))
        return config to token
    }

    /** Сериализуемые формы подписки (браузерная `keys: {p256dh, auth}` и плоская). */
    @kotlinx.serialization.Serializable
    private data class SubscriptionJson(
        val endpoint: String,
        val keys: KeysJson
    )

    @kotlinx.serialization.Serializable
    private data class KeysJson(val p256dh: String, val auth: String)

    // -------------------------------------------------------------------------
    // parseSubscription
    // -------------------------------------------------------------------------

    @Test
    fun `parseSubscription accepts base64url subscription JSON`() {
        val (_, token) = configAndToken()
        val delivery = WebPushDelivery(WebPushSender(WebPushConfig(enabled = false)))
        val sub = delivery.parseSubscription(token)
        assertNotNull(sub)
        assertTrue(sub.endpoint.startsWith("https://"))
        assertTrue(sub.p256dh.isNotBlank())
        assertTrue(sub.auth.isNotBlank())
    }

    @Test
    fun `parseSubscription accepts plain flat JSON`() {
        val delivery = WebPushDelivery(WebPushSender(WebPushConfig(enabled = false)))
        val plain = """{"endpoint":"https://push.example.com/x","p256dh":"K_key","auth":"A_sec"}"""
        val sub = delivery.parseSubscription(plain)
        assertNotNull(sub)
        assertEquals("https://push.example.com/x", sub.endpoint)
        assertEquals("K_key", sub.p256dh)
        assertEquals("A_sec", sub.auth)
    }

    @Test
    fun `parseSubscription accepts nested keys form`() {
        val delivery = WebPushDelivery(WebPushSender(WebPushConfig(enabled = false)))
        val plain = """{"endpoint":"https://push.example.com/x","keys":{"p256dh":"K","auth":"A"}}"""
        val sub = delivery.parseSubscription(plain)
        assertNotNull(sub)
        assertEquals("K", sub.p256dh)
        assertEquals("A", sub.auth)
    }

    @Test
    fun `parseSubscription rejects garbage`() {
        val delivery = WebPushDelivery(WebPushSender(WebPushConfig(enabled = false)))
        assertNull(delivery.parseSubscription("not-a-subscription"))
        assertNull(delivery.parseSubscription(""))
        assertNull(delivery.parseSubscription("""{"foo":1}"""))
    }

    // -------------------------------------------------------------------------
    // send: автоочистка при 404/410
    // -------------------------------------------------------------------------

    @Test
    fun `send purges token when push service returns 410`() = runTest {
        val (config, token) = configAndToken()
        val purger = RecordingPurger()
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { respond("Gone", HttpStatusCode.Gone) }
            }
        }
        val delivery = WebPushDelivery(WebPushSender(config, mockClient), purger)

        val result = delivery.send(listOf(token), notification())

        assertEquals(listOf(token), purger.purged, "мертвая подписка должна быть удалена")
        assertTrue(result.isFailure, "все токены невалидны -> failure")
    }

    @Test
    fun `send purges token when push service returns 404`() = runTest {
        val (config, token) = configAndToken()
        val purger = RecordingPurger()
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { respond("Not Found", HttpStatusCode.NotFound) }
            }
        }
        val delivery = WebPushDelivery(WebPushSender(config, mockClient), purger)

        delivery.send(listOf(token), notification())

        assertEquals(listOf(token), purger.purged)
    }

    @Test
    fun `send does not purge on transient 500`() = runTest {
        val (config, token) = configAndToken()
        val purger = RecordingPurger()
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { respond("Server Error", HttpStatusCode.InternalServerError) }
            }
        }
        val delivery = WebPushDelivery(WebPushSender(config, mockClient), purger)

        val result = delivery.send(listOf(token), notification())

        assertTrue(purger.purged.isEmpty(), "временная ошибка не должна удалять токен")
        assertTrue(result.isFailure)
    }

    @Test
    fun `send succeeds on 200 without purge`() = runTest {
        val (config, token) = configAndToken()
        val purger = RecordingPurger()
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { respond("", HttpStatusCode.OK) }
            }
        }
        val delivery = WebPushDelivery(WebPushSender(config, mockClient), purger)

        val result = delivery.send(listOf(token), notification())

        assertTrue(result.isSuccess)
        assertTrue(purger.purged.isEmpty())
    }

    @Test
    fun `send purges unparseable token and keeps valid one`() = runTest {
        val (config, token) = configAndToken()
        val purger = RecordingPurger()
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { respond("", HttpStatusCode.OK) }
            }
        }
        val delivery = WebPushDelivery(WebPushSender(config, mockClient), purger)

        val result = delivery.send(listOf("garbage-token", token), notification())

        // Невалидный токен удалён, валидный доставлен => общий результат success.
        assertEquals(listOf("garbage-token"), purger.purged)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `send works without purger (no NPE)`() = runTest {
        val (config, token) = configAndToken()
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { respond("Gone", HttpStatusCode.Gone) }
            }
        }
        val delivery = WebPushDelivery(WebPushSender(config, mockClient), tokenPurger = null)

        val result = delivery.send(listOf(token), notification())

        assertTrue(result.isFailure, "все токены невалидны -> failure даже без purger")
    }
}
