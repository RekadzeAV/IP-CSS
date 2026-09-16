package com.company.ipcamera.server.integration

import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.routing.streamRoutes
import com.company.ipcamera.server.service.ScreenshotService
import com.company.ipcamera.server.service.VideoStreamService
import com.company.ipcamera.server.service.WebRtcService
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.CameraRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.coEvery
import kotlinx.serialization.json.Json
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Интеграционные проверки HLS-маршрутов под [authenticate]: playlist / variant / сегмент / 401 / 404 / 503.
 * Файлы создаются под `streams/hls/<streamId>/` относительно [user.dir] (как в [com.company.ipcamera.server.routing.StreamRoutes]).
 */
class HlsStreamRoutesIntegrationTest {

    private val cameraId = "cam-hls-itest"
    private val streamId = "itest-hls-stream"
    private val baseDir: File
        get() = File(File(System.getProperty("user.dir")), "streams/hls/$streamId")

    @AfterTest
    fun cleanupHlsFiles() {
        val root = File(File(System.getProperty("user.dir")), "streams/hls/$streamId")
        if (root.exists()) {
            root.walkBottomUp().forEach { it.delete() }
        }
    }

    private fun bearer(role: UserRole = UserRole.VIEWER): String {
        val token = JwtConfig.generateAccessToken(
            userId = "hls-itest-user",
            username = "hls-itest",
            role = role.name,
            permissions = listOf("*")
        )
        return "Bearer $token"
    }

    private fun minimalPlaylist(): String = """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXTINF:2.0,
        segment_001.ts
        #EXT-X-ENDLIST
    """.trimIndent()

    private fun preparePlaylistAndSegment() {
        baseDir.mkdirs()
        File(baseDir, "playlist.m3u8").writeText(minimalPlaylist())
        File(baseDir, "segment_001.ts").writeBytes(byteArrayOf(0x47, 0x40))
    }

    private fun prepareVariantMedium() {
        val dir = File(baseDir, "medium")
        dir.mkdirs()
        File(dir, "playlist.m3u8").writeText(minimalPlaylist())
        File(dir, "segment_001.ts").writeBytes(byteArrayOf(0x47, 0x40))
    }

    private fun prepareVariantPlaylist(quality: String) {
        val dir = File(baseDir, quality)
        dir.mkdirs()
        File(dir, "playlist.m3u8").writeText(minimalPlaylist())
        File(dir, "segment_001.ts").writeBytes(byteArrayOf(0x47, 0x40))
    }

    @Test
    fun `hls playlist without auth returns 401`() = testApplication {
        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { mockk(relaxed = true) }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/hls/playlist.m3u8")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `hls playlist returns 200 when file exists`() = testApplication {
        preparePlaylistAndSegment()
        val playlistPath = File(baseDir, "playlist.m3u8").absolutePath
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.isStreamActive(any()) } returns true
        every { videoSvc.getHlsPlaylistPath(cameraId) } returns playlistPath

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/hls/playlist.m3u8") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("#EXTM3U"), body)
        assertTrue(body.contains("/api/v1/cameras/$cameraId/stream/hls/segment_"), body)
        val ctype = response.headers[HttpHeaders.ContentType]
        assertTrue(
            ctype?.contains("mpegurl") == true || ctype?.contains("application") == true,
            ctype
        )
    }

    @Test
    fun `hls playlist returns 404 when path set but file missing`() = testApplication {
        val missing = File(baseDir, "playlist.m3u8").absolutePath
        if (baseDir.exists()) baseDir.deleteRecursively()
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.isStreamActive(any()) } returns true
        every { videoSvc.getHlsPlaylistPath(cameraId) } returns missing

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/hls/playlist.m3u8") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `hls playlist returns 503 when generation path unavailable`() = testApplication {
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.isStreamActive(any()) } returns true
        every { videoSvc.getHlsPlaylistPath(cameraId) } returns null

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/hls/playlist.m3u8") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(response.bodyAsText().contains("HLS generation not available", ignoreCase = true))
    }

    @Test
    fun `hls master returns 503 when adaptive not available in test koin`() = testApplication {
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.isStreamActive(any()) } returns true
        every { videoSvc.getStreamId(cameraId) } returns streamId

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/hls/master.m3u8") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }

    @Test
    fun `hls variant quality invalid returns 400`() = testApplication {
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.getStreamId(cameraId) } returns streamId

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/hls/notaquality/playlist.m3u8") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `hls variant medium playlist returns 200 when file exists`() = testApplication {
        prepareVariantMedium()
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.getStreamId(cameraId) } returns streamId

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/hls/medium/playlist.m3u8") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("segment_"))
    }

    @Test
    fun `hls variant segment returns 404 when file missing`() = testApplication {
        if (baseDir.exists()) baseDir.deleteRecursively()
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.getStreamId(cameraId) } returns streamId

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/hls/medium/segment_001.ts") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `hls variant segment returns 200 when file exists`() = testApplication {
        prepareVariantMedium()
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.getStreamId(cameraId) } returns streamId

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/hls/medium/segment_001.ts") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `hls variant qhd1440 h264 playlist returns 200 when file exists`() = testApplication {
        prepareVariantPlaylist("qhd1440_h264")
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.getStreamId(cameraId) } returns streamId

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/hls/qhd1440_h264/playlist.m3u8") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("segment_"))
    }

    @Test
    fun `hls variant uhd4k h264 playlist returns 404 when missing file but quality valid`() = testApplication {
        if (baseDir.exists()) baseDir.deleteRecursively()
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.getStreamId(cameraId) } returns streamId

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/hls/uhd4k_h264/playlist.m3u8") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `stream status returns rtsp diagnostics payload when stream active`() = testApplication {
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.isStreamActive(cameraId) } returns true
        every { videoSvc.getStreamId(cameraId) } returns streamId
        every { videoSvc.getHlsUrl(cameraId) } returns "/api/v1/cameras/$cameraId/stream/hls/playlist.m3u8"
        coEvery { videoSvc.getRtspUrl(cameraId) } returns "rtsp://admin:pass@10.0.0.2/stream1"
        every { videoSvc.getRtspDiagnostics(cameraId) } returns VideoStreamService.RtspDiagnostics(
            connectAttempts = 3,
            connectSuccesses = 1,
            connectFailures = 2,
            reconnectAttempts = 2,
            reconnectSuccesses = 1,
            reconnectFailures = 1,
            consecutiveFailures = 0,
            lastError = "timeout",
            lastConnectedAt = 1234L,
            lastFrameAt = 5678L
        )

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/status") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"active\":true"), body)
        assertTrue(body.contains("\"connectAttempts\":3"), body)
        assertTrue(body.contains("\"lastError\":\"timeout\""), body)
        assertTrue(body.contains("\"rtspUrl\":\"rtsp://admin:pass@10.0.0.2/stream1\""), body)
    }

    @Test
    fun `stream status returns null diagnostics when stream inactive`() = testApplication {
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.isStreamActive(cameraId) } returns false
        every { videoSvc.getStreamId(cameraId) } returns null
        coEvery { videoSvc.getRtspUrl(cameraId) } returns null
        every { videoSvc.getRtspDiagnostics(cameraId) } returns null

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/status") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertNotNull(body)
        assertTrue(body.contains("\"active\":false"), body)
        assertTrue(
            body.contains("\"rtspDiagnostics\":null") || !body.contains("\"rtspDiagnostics\":{"),
            body
        )
    }

    @Test
    fun `hls legacy segment rejects non ts extension`() = testApplication {
        val videoSvc = mockk<VideoStreamService>(relaxed = true)
        every { videoSvc.getHlsPlaylistPath(cameraId) } returns File(baseDir, "playlist.m3u8").absolutePath

        application {
            stopKoin()
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Authentication) {
                jwt("jwt-auth") {
                    realm = "IP-CSS"
                    verifier(JwtConfig.createVerifier())
                    validate { credential ->
                        if (credential.payload.subject.isNotBlank()) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            install(Koin) {
                modules(
                    module {
                        single<VideoStreamService> { videoSvc }
                        single<ScreenshotService> { mockk(relaxed = true) }
                        single<WebRtcService> { mockk(relaxed = true) }
                        single<CameraRepository> { mockk(relaxed = true) }
                    }
                )
            }
            routing {
                route("/api/v1") {
                    streamRoutes()
                }
            }
        }

        val response = client.get("/api/v1/cameras/$cameraId/stream/hls/segment_001.mp4") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
