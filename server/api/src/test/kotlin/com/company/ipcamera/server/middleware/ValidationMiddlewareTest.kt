package com.company.ipcamera.server.middleware

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.validation.*
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Тесты для ValidationMiddleware
 */
class ValidationMiddlewareTest {
    private fun Application.configureJsonTestRouting(build: Route.() -> Unit) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        routing(build)
    }

    @Test
    fun testValidateRequest_success() = testApplication {
        application {
            configureJsonTestRouting {
                post("/test") {
                    val request = call.receive<LoginRequest>()
                    if (validateRequest(request) { RequestValidator.validateLoginRequest(it) }) {
                        call.respond(HttpStatusCode.OK, "Success")
                    }
                }
            }
        }

        val response = client.post("/test") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"username":"testuser","password":"password123"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testValidateRequest_validationError() = testApplication {
        application {
            configureJsonTestRouting {
                post("/test") {
                    val request = call.receive<LoginRequest>()
                    val ok = validateRequest(request) { RequestValidator.validateLoginRequest(it) }
                    if (!ok) return@post
                    call.respond(HttpStatusCode.OK, "Should not reach here")
                }
            }
        }

        val response = client.post("/test") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"username":"","password":"password123"}""") // invalid username
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testValidatePagination() = testApplication {
        application {
            configureJsonTestRouting {
                get("/test") {
                    val (page, limit) = validatePagination() ?: return@get
                    call.respond(HttpStatusCode.OK, mapOf("page" to page, "limit" to limit))
                }
            }
        }

        assertEquals(HttpStatusCode.OK, client.get("/test?page=2&limit=50").status)
        assertEquals(HttpStatusCode.OK, client.get("/test").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/test?page=0").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/test?limit=1000").status)
    }

    @Test
    fun testValidateFiltersDateRangeAndSorting() = testApplication {
        application {
            configureJsonTestRouting {
                get("/test") {
                    val filters = validateFilters(
                        validTypes = listOf("MOTION", "OBJECT"),
                        validPriorities = listOf("LOW", "MEDIUM", "HIGH"),
                        validStatuses = listOf("ACTIVE", "RESOLVED")
                    ) ?: return@get
                    val dateRange = validateDateRange() ?: return@get
                    val sorting = validateSorting(
                        validFields = listOf("createdat", "name", "updatedat"),
                        defaultField = "createdat",
                        defaultOrder = "desc"
                    ) ?: return@get
                    call.respondText("ok:${filters.first}:${dateRange.first}:${sorting.first}", status = HttpStatusCode.OK)
                }
            }
        }

        assertEquals(
            HttpStatusCode.OK,
            client.get("/test?type=MOTION&priority=HIGH&status=ACTIVE&startDate=1&endDate=2&sortBy=name&sortOrder=asc").status
        )
        assertEquals(HttpStatusCode.BadRequest, client.get("/test?type=INVALID").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/test?priority=INVALID").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/test?startDate=abc").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/test?startDate=2&endDate=1").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/test?sortBy=invalid").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/test?sortOrder=invalid").status)
    }
}
