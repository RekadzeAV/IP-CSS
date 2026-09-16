package com.company.ipcamera.server.middleware

import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireAnyPermission
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requirePermission
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** Тесты permission-проверок AuthorizationMiddleware (requirePermission / requireAnyPermission). */
class AuthorizationPermissionTest {

    private fun tokenWith(permissions: List<String>): String =
        JwtConfig.generateAccessToken(
            userId = "u-1", username = "tester", role = "VIEWER",
            permissions = permissions
        )

    private fun Application.configureAuth() {
        install(Authentication) {
            jwt("jwt-auth") {
                realm = "IP-CSS"
                verifier(JwtConfig.createVerifier())
                validate { credential -> JWTPrincipal(credential.payload) }
            }
        }
    }

    @Test
    fun `requirePermission passes when exact permission present`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    get("/perm/cameras") {
                        requirePermission("cameras:view")
                        call.respondText("ok")
                    }
                }
            }
        }
        val res = client.get("/perm/cameras") {
            header(HttpHeaders.Authorization, "Bearer " + tokenWith(listOf("cameras:view")))
        }
        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `requirePermission blocks when permission missing`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    get("/perm/users") { requirePermission("users:delete"); call.respondText("ok") }
                }
            }
        }
        val res = client.get("/perm/users") {
            header(HttpHeaders.Authorization, "Bearer " + tokenWith(listOf("cameras:view")))
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)
    }

    @Test
    fun `requirePermission allows wildcard star`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    get("/perm/any") { requirePermission("users:delete"); call.respondText("ok") }
                }
            }
        }
        val res = client.get("/perm/any") {
            header(HttpHeaders.Authorization, "Bearer " + tokenWith(listOf("*")))
        }
        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `requirePermission returns unauthorized without principal`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    get("/perm/noauth") { requirePermission("cameras:view"); call.respondText("ok") }
                }
            }
        }
        val res = client.get("/perm/noauth")
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `requireAnyPermission passes when one of several matches`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    get("/perm/anyof") {
                        requireAnyPermission("users:create", "users:read")
                        call.respondText("ok")
                    }
                }
            }
        }
        val res = client.get("/perm/anyof") {
            header(HttpHeaders.Authorization, "Bearer " + tokenWith(listOf("users:read")))
        }
        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `requireAnyPermission blocks when none match`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    get("/perm/noneof") {
                        requireAnyPermission("users:create", "users:delete")
                        call.respondText("ok")
                    }
                }
            }
        }
        val res = client.get("/perm/noneof") {
            header(HttpHeaders.Authorization, "Bearer " + tokenWith(listOf("cameras:view")))
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)
    }

    @Test
    fun `requireAnyPermission allows wildcard star`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    get("/perm/wildcard") {
                        requireAnyPermission("anything:write")
                        call.respondText("ok")
                    }
                }
            }
        }
        val res = client.get("/perm/wildcard") {
            header(HttpHeaders.Authorization, "Bearer " + tokenWith(listOf("*")))
        }
        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `requireAnyPermission returns unauthorized without principal`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            configureAuth()
            routing {
                authenticate("jwt-auth") {
                    get("/perm/anonymous") { requireAnyPermission("users:read"); call.respondText("ok") }
                }
            }
        }
        val res = client.get("/perm/anonymous")
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }
}