package com.company.ipcamera.server.middleware

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HttpsRedirectAndHstsMiddlewareTest {

    @Test
    fun `installHttpsRedirect redirects http to https with query`() = testApplication {
        application {
            installHttpsRedirect(enabled = true, httpsPort = 443)
            routing {
                get("/x") { call.respondText("should-not-run") }
            }
        }
        val client = createClient { followRedirects = false }
        val response = client.get("/x?a=1")
        assertEquals(HttpStatusCode.MovedPermanently, response.status)
        val loc = response.headers[HttpHeaders.Location]
        assertNotNull(loc)
        assertTrue(loc.startsWith("https://"), loc)
        assertTrue(loc.contains("/x"), loc)
        assertTrue(loc.contains("a=1"), loc)
    }

    @Test
    fun `installHttpsRedirect skipped when X-Forwarded-Proto is https`() = testApplication {
        application {
            installHttpsRedirect(enabled = true, httpsPort = 443)
            routing {
                get("/ok") { call.respondText("ok") }
            }
        }
        val response = client.get("/ok") {
            header("X-Forwarded-Proto", "https")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", response.bodyAsText())
    }

    @Test
    fun `installHttpsRedirect disabled passes through`() = testApplication {
        application {
            installHttpsRedirect(enabled = false, httpsPort = 443)
            routing {
                get("/plain") { call.respondText("plain") }
            }
        }
        val response = client.get("/plain")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("plain", response.bodyAsText())
    }

    @Test
    fun `installHsts sets header when X-Forwarded-Proto is https`() = testApplication {
        application {
            installHsts(
                maxAge = 60,
                includeSubDomains = false,
                preload = false,
                enabled = true
            )
            routing {
                get("/h") { call.respondText("h") }
            }
        }
        val response = client.get("/h") {
            header("X-Forwarded-Proto", "https")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val hsts = response.headers[HttpHeaders.StrictTransportSecurity]
        assertNotNull(hsts)
        assertTrue(hsts.contains("max-age=60"), hsts)
    }
}
