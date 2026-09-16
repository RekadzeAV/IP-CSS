package com.company.ipcamera.server.notification

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebPushSenderIntegrationTest {

    /** Генерирует валидные VAPID-ключи и подписку с настоящими P-256 ключами. */
    private fun validConfigAndSubscription(subject: String = "mailto:test@example.com"):
        Triple<WebPushConfig, WebPushSubscription, String> {
        val vapid = WebPushConfig.generateVapidKeys() // real P-256 keys

        // Подписка: браузерная P-256 пара
        val browserPair = WebPushCrypto.generateVapidKeyPair()
        val p256dh = WebPushCrypto.b64UrlEncode(WebPushCrypto.rawPublicKey(browserPair.public))
        val auth = WebPushCrypto.b64UrlEncode(ByteArray(16) { (it * 13).toByte() })

        val config = WebPushConfig(
            enabled = true,
            publicKey = vapid.publicKey,
            privateKey = vapid.privateKey,
            subject = subject
        )
        val subscription = WebPushSubscription(
            endpoint = "https://fcm.googleapis.com/fcm/send/test",
            p256dh = p256dh,
            auth = auth
        )
        return Triple(config, subscription, vapid.privateKey)
    }

    @Test
    fun `send delivers encrypted aes128gcm payload with vapid auth`() = runTest {
        val (config, subscription, _) = validConfigAndSubscription()
        var authHeader = ""
        var encHeader = ""

        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    assertEquals(HttpMethod.Post, request.method)
                    assertTrue { request.url.toString().contains("fcm.googleapis.com") }
                    authHeader = request.headers["Authorization"] ?: ""
                    encHeader = request.headers["Content-Encoding"] ?: ""
                    respond("", HttpStatusCode.OK)
                }
            }
        }

        val sender = WebPushSender(config, mockClient)
        val payload = WebPushPayload(title = "Test", body = "Message", data = mapOf("type" to "TEST"))
        val result = sender.send(subscription, payload)

        assertTrue(result.isSuccess, "send should succeed: ${result.exceptionOrNull()}")
        assertTrue(result.getOrThrow().success)
        assertTrue(authHeader.startsWith("vapid t="), "Authorization должен быть vapid формат: $authHeader")
        assertTrue(authHeader.contains(", k="), "должен содержать VAPID public key")
        assertEquals("aes128gcm", encHeader, "Content-Encoding должен быть aes128gcm")
    }

    @Test
    fun `send retries on transient failure then succeeds`() = runTest {
        val (config, subscription, _) = validConfigAndSubscription()
        var attemptCount = 0
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    attemptCount++
                    if (attemptCount < 3) {
                        respond("Service Unavailable", HttpStatusCode.ServiceUnavailable)
                    } else {
                        respond("", HttpStatusCode.OK)
                    }
                }
            }
        }
        val sender = WebPushSender(config, mockClient)
        val payload = WebPushPayload(title = "T", body = "B")
        val result = sender.send(subscription, payload)

        assertTrue(result.isSuccess, "should succeed after retries: ${result.exceptionOrNull()}")
        assertEquals(3, attemptCount, "должно быть 3 попытки")
    }

    @Test
    fun `send succeeds when disabled without network`() = runTest {
        val config = WebPushConfig(enabled = false)
        val sender = WebPushSender(config)
        val subscription = WebPushSubscription("https://example.com", "x", "y")
        val payload = WebPushPayload(title = "Test", body = "Test")

        val result = sender.send(subscription, payload)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().success)
    }

    @Test
    fun `bulk send processes each subscription`() = runTest {
        val (config, subscription, _) = validConfigAndSubscription()
        var calls = 0
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    calls++
                    respond("", HttpStatusCode.OK)
                }
            }
        }
        val sender = WebPushSender(config, mockClient)

        val subscriptions = if (true) {
            // Для bulk каждый должен иметь собственные ключи; используем 3 разные подписки
            val subs = mutableListOf<WebPushSubscription>()
            repeat(3) {
                val pair = WebPushCrypto.generateVapidKeyPair()
                subs += WebPushSubscription(
                    endpoint = "https://endpoint$it.example.com",
                    p256dh = WebPushCrypto.b64UrlEncode(WebPushCrypto.rawPublicKey(pair.public)),
                    auth = WebPushCrypto.b64UrlEncode(ByteArray(16) { (it).toByte() })
                )
            }
            subs
        } else emptyList()

        val result = sender.sendBulk(subscriptions, WebPushPayload(title = "Bulk", body = "x"))

        assertTrue(result.isSuccess, "bulk should succeed")
        val results = result.getOrThrow()
        assertEquals(3, results.size)
        assertTrue(results.all { it.value.success })
        assertEquals(3, calls)
        assertNotNull(subscription)
    }
}