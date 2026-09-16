package com.company.ipcamera.server.middleware

import com.company.ipcamera.server.config.JwtConfig
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class CookieJwtAuthFlowIntegrationTest {

    @Test
    fun `jwt auth accepts access_token from cookie via middleware`() = testApplication {
        application {
            configureCookieAuth()
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = JwtConfig.realm
                    verifier(JwtConfig.createVerifier())
                    authHeader { call ->
                        val token = call.getJwtToken() ?: return@authHeader null
                        parseAuthorizationHeader("Bearer $token")
                    }
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) {
                            JWTPrincipal(credential.payload)
                        } else {
                            null
                        }
                    }
                }
            }
            routing {
                authenticate("jwt-auth") {
                    get("/protected") {
                        call.respondText("ok")
                    }
                }
            }
        }

        val accessToken = JwtConfig.generateAccessToken(
            userId = "cookie-user",
            username = "cookie-user",
            role = "ADMIN",
            permissions = listOf("*")
        )

        val response = client.get("/protected") {
            cookie("access_token", accessToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", response.bodyAsText())
    }
}
