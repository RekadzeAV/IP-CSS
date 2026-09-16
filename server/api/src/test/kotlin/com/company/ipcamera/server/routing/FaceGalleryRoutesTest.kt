package com.company.ipcamera.server.routing

import com.auth0.jwt.JWT
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.middleware.configureExceptionHandling
import com.company.ipcamera.shared.domain.model.StoredFace
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.FaceRepository
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
import kotlinx.serialization.json.Json
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FaceGalleryRoutesTest {

    /** Минимальный размер embedding в API — 8 (см. FaceGalleryRoutes). */
    private val emb8 = "[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8]"

    @Test
    fun `viewer can read labels and search faces`() = testApplication {
        application { configureFaceTestApp() }

        val operatorToken = createTestToken("op-1", UserRole.OPERATOR)
        val viewerToken = createTestToken("viewer-1", UserRole.VIEWER)

        val createResponse = client.post("/api/v1/analytics/faces") {
            header(HttpHeaders.Authorization, "Bearer $operatorToken")
            contentType(ContentType.Application.Json)
            setBody("""{"label":"john","embedding":$emb8,"cameraId":"cam-1"}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)

        val labelsResponse = client.get("/api/v1/analytics/faces/labels") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        assertEquals(HttpStatusCode.OK, labelsResponse.status)
        assertTrue(labelsResponse.bodyAsText().contains("john"))

        val searchResponse = client.post("/api/v1/analytics/faces/search") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"embedding":$emb8,"topK":5,"minSimilarity":0.5}""")
        }
        assertEquals(HttpStatusCode.OK, searchResponse.status)
    }

    @Test
    fun `viewer cannot create or delete face`() = testApplication {
        application { configureFaceTestApp() }
        val viewerToken = createTestToken("viewer-1", UserRole.VIEWER)

        val deleteResponse = client.delete("/api/v1/analytics/faces/unknown-id") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        assertEquals(HttpStatusCode.Forbidden, deleteResponse.status)
    }

    @Test
    fun `operator can create and delete face by id`() = testApplication {
        application { configureFaceTestApp() }
        val operatorToken = createTestToken("op-1", UserRole.OPERATOR)

        val createResponse = client.post("/api/v1/analytics/faces") {
            header(HttpHeaders.Authorization, "Bearer $operatorToken")
            contentType(ContentType.Application.Json)
            setBody("""{"id":"face-1","label":"alice","embedding":[1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0]}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)

        val getResponse = client.get("/api/v1/analytics/faces/face-1") {
            header(HttpHeaders.Authorization, "Bearer $operatorToken")
        }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains("alice"))

        val deleteResponse = client.delete("/api/v1/analytics/faces/face-1") {
            header(HttpHeaders.Authorization, "Bearer $operatorToken")
        }
        assertEquals(HttpStatusCode.OK, deleteResponse.status)
    }

    private fun Application.configureFaceTestApp() {
        // Koin is global; ensure previous test app context is cleaned.
        stopKoin()
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        configureExceptionHandling()
        install(Authentication) {
            jwt("jwt-auth") {
                realm = "IP-CSS"
                verifier(JwtConfig.createVerifier())
                validate { credential ->
                    if (credential.payload.subject.isNotBlank()) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }
        install(Koin) {
            modules(
                module {
                    single<FaceRepository> { InMemoryFaceRepository() }
                }
            )
        }

        routing {
            route("/api/v1") {
                faceGalleryRoutes()
            }
        }
    }

    private fun createTestToken(userId: String, role: UserRole): String {
        val algorithm = JwtConfig.algorithm
        return JWT.create()
            .withIssuer("ip-camera-server")
            .withAudience("ip-camera-client")
            .withSubject(userId)
            .withClaim("role", role.name)
            .withClaim("permissions", listOf("*"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3600000))
            .sign(algorithm)
    }
}

private class InMemoryFaceRepository : FaceRepository {
    private val faces = LinkedHashMap<String, StoredFace>()

    override suspend fun insert(face: StoredFace): Result<Unit> {
        faces[face.id] = face
        return Result.success(Unit)
    }

    override suspend fun getById(id: String): StoredFace? = faces[id]

    override suspend fun getByLabel(label: String, limit: Int): List<StoredFace> {
        return faces.values.filter { it.label == label }.take(limit)
    }

    override suspend fun findNearest(
        embedding: FloatArray,
        topK: Int,
        minSimilarity: Float
    ): List<Pair<StoredFace, Float>> {
        return faces.values
            .map { it to cosineSimilarity(embedding, it.embedding) }
            .filter { (_, score) -> score >= minSimilarity }
            .sortedByDescending { it.second }
            .take(topK)
    }

    override suspend fun delete(id: String): Result<Unit> {
        faces.remove(id)
        return Result.success(Unit)
    }

    override suspend fun listLabels(): List<String> = faces.values.map { it.label }.distinct().sorted()

    override suspend fun deleteByLabel(label: String): Result<Int> {
        val ids = faces.values.filter { it.label == label }.map { it.id }
        ids.forEach { faces.remove(it) }
        return Result.success(ids.size)
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            val av = a[i].toDouble()
            val bv = b[i].toDouble()
            dot += av * bv
            normA += av * av
            normB += bv * bv
        }
        val denominator = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
        return if (denominator == 0.0) 0f else (dot / denominator).toFloat()
    }
}

