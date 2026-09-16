package com.company.ipcamera.server.notification

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import kotlinx.coroutines.runBlocking
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FcmNotificationSenderTest {

    /** Генерирует RSA-пару и экспортирует приватный ключ как PKCS#8 PEM. */
    private fun generatePem(): Pair<String, java.security.PublicKey> {
        val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val pkcs8 = Base64.getEncoder().encodeToString(kp.private.encoded)
        val pem = "-----BEGIN PRIVATE KEY-----\n$pkcs8\n-----END PRIVATE KEY-----\n"
        return pem to kp.public
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

    @Test
    fun `service account json parses into credentials`() {
        val (pem, _) = generatePem()
        val creds = FcmServiceAccountCredentials.fromJson(serviceAccountJson(pem))

        assertNotNull(creds)
        assertEquals("ipcss-test", creds!!.projectId)
        assertEquals("push@ipcss-test.iam.gserviceaccount.com", creds.clientEmail)
    }

    @Test
    fun `assertion jwt is signed and verifiable with public key`() {
        val (pem, publicKey) = generatePem()
        val creds = assertNotNull(FcmServiceAccountCredentials.fromJson(serviceAccountJson(pem)))

        val now = 1_700_000_000_000L
        val jwt = creds.buildAssertionJwt(now)

        val parts = jwt.split(".")
        assertEquals(3, parts.size)

        fun b64(s: String) = String(Base64.getUrlDecoder().decode(s))
        val header = b64(parts[0])
        val claims = b64(parts[1])
        assertTrue(header.contains("RS256"))
        assertTrue(claims.contains("push@ipcss-test.iam.gserviceaccount.com")) // iss
        assertTrue(claims.contains("firebase.messaging")) // scope

        // Верификация подписи публичным ключом
        val verifier = Signature.getInstance("SHA256withRSA").apply {
            initVerify(publicKey)
            update((parts[0] + "." + parts[1]).toByteArray(Charsets.UTF_8))
        }
        val signatureOk = verifier.verify(Base64.getUrlDecoder().decode(parts[2]))
        assertTrue(signatureOk)
    }

    @Test
    fun `invalid service account json returns null`() {
        assertEquals(null, FcmServiceAccountCredentials.fromJson("{ not-json "))
        assertEquals(null, FcmServiceAccountCredentials.fromJson("""{"type":"service_account"}"""))
    }

    @Test
    fun `send skips silently when disabled`() = runBlocking {
        var networkCalls = 0
        val engine = MockEngine { networkCalls++; respond("{}", HttpStatusCode.OK) }
        val sender = FcmNotificationSender(
            config = FcmConfig(enabled = false, projectId = "p"),
            client = HttpClient(engine)
        )
        val result = sender.send(listOf("token-1"), title = "T", body = "B")
        assertTrue(result.isSuccess)
        assertEquals(0, networkCalls)
    }

    @Test
    fun `send delivers via fcm http v1 and caches oauth token`() = runBlocking {
        val (pem, _) = generatePem()
        var oauthCalls = 0
        var fcmCalls = 0
        var lastFcmBody = ""

        val engine = MockEngine { request ->
            when {
                request.url.host == "oauth2.googleapis.com" -> {
                    oauthCalls++
                    respond(
                        """{"access_token":"ya29.test-token","expires_in":3600,"token_type":"Bearer"}""",
                        HttpStatusCode.OK
                    )
                }
                request.url.host == "fcm.googleapis.com" -> {
                    fcmCalls++
                    lastFcmBody = (request.body as io.ktor.http.content.TextContent).text
                    respond("""{"name":"projects/ipcss-test/messages/1"}""", HttpStatusCode.OK)
                }
                else -> respond("unexpected", HttpStatusCode.NotFound)
            }
        }

        val sender = FcmNotificationSender(
            config = FcmConfig(enabled = true, projectId = "ipcss-test", serviceAccountJson = serviceAccountJson(pem)),
            retryDelayMs = 1L,
            client = HttpClient(engine)
        )

        val r1 = sender.send(listOf("device-token-1"), title = "Motion", body = "Movement detected", priority = FcmPriority.HIGH)
        assertTrue(r1.isSuccess, "r1 failed: ${r1.exceptionOrNull()}")

        // Повторная отправка: OAuth-токен должен быть закэширован
        val r2 = sender.send(listOf("device-token-2"), title = "T2", body = "B2", priority = FcmPriority.HIGH)
        assertTrue(r2.isSuccess, "r2 failed: ${r2.exceptionOrNull()}")

        assertEquals(1, oauthCalls, "OAuth должен быть вызван 1 раз (кэш), был: $oauthCalls")
        assertEquals(2, fcmCalls, "FCM должен быть вызван по разу на токен, был: $fcmCalls")
        val tokenInBody = lastFcmBody.contains("\"token\":\"device-token-2\"")
        assertTrue(tokenInBody, "Тело последнего FCM-запроса должно содержать device-token-2: $lastFcmBody")
        assertTrue(lastFcmBody.contains("\"title\":\"T2\""), "Должен содержать title T2: $lastFcmBody")
        assertTrue(lastFcmBody.contains("\"priority\":\"HIGH\""), "Должен содержать priority HIGH: $lastFcmBody")
    }

    @Test
    fun `send fails after retries on persistent fcm error`() = runBlocking {
        val (pem, _) = generatePem()
        var fcmCalls = 0

        val engine = MockEngine { request ->
            when (request.url.host) {
                "oauth2.googleapis.com" ->
                    respond("""{"access_token":"t","expires_in":3600}""", HttpStatusCode.OK)
                else -> { fcmCalls++; respond("""{"error":"unavailable"}""", HttpStatusCode.InternalServerError) }
            }
        }

        val sender = FcmNotificationSender(
            config = FcmConfig(enabled = true, projectId = "p", serviceAccountJson = serviceAccountJson(pem)),
            maxRetries = 3,
            retryDelayMs = 1L,
            client = HttpClient(engine)
        )

        val result = sender.send(listOf("tok"), title = "T", body = "B")
        assertTrue(result.isFailure)
        assertEquals(3, fcmCalls)
    }

    @Test
    fun `send fails immediately when credentials missing`() = runBlocking {
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val sender = FcmNotificationSender(
            config = FcmConfig(enabled = true, projectId = "p"), // без service account
            client = HttpClient(engine)
        )
        val result = sender.send(listOf("tok"), title = "T", body = "B")
        assertTrue(result.isFailure)
    }
}