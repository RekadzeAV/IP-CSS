package com.company.ipcamera.server.notification

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelegramNotificationSenderTest {

    @Test
    fun `send delivers message to all configured chats`() = runBlocking {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(
                content = """{"ok":true}""",
                status = HttpStatusCode.OK
            )
        }
        val sender = TelegramNotificationSender(
            config = TelegramConfig(
                botToken = "token",
                chatIds = listOf("1", "2")
            ),
            client = HttpClient(engine) {
                install(ContentNegotiation) { json() }
            }
        )

        val result = sender.send("hello")
        assertTrue(result.isSuccess)
        assertEquals(2, callCount)
    }

    @Test
    fun `send retries and fails after max attempts`() = runBlocking {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(
                content = """{"ok":false}""",
                status = HttpStatusCode.InternalServerError
            )
        }
        val sender = TelegramNotificationSender(
            config = TelegramConfig(
                botToken = "token",
                chatIds = listOf("1")
            ),
            maxRetries = 3,
            retryDelayMs = 1L,
            client = HttpClient(engine) {
                install(ContentNegotiation) { json() }
            }
        )

        val result = sender.send("hello")
        assertTrue(result.isFailure)
        assertEquals(3, callCount)
    }
}
