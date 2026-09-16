package com.company.ipcamera.server.integration

import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.routing.recordingRoutes
import com.company.ipcamera.server.service.ExportService
import com.company.ipcamera.server.service.FfmpegService
import com.company.ipcamera.server.service.HlsGeneratorService
import com.company.ipcamera.server.service.SignedUrlService
import com.company.ipcamera.server.service.VideoRecordingService
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.unmockkAll
import kotlinx.serialization.json.Json
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * JWT HLS для записей: `/api/v1/recordings/{id}/hls/playlist.m3u8` и сегменты `segment_NNN.ts`.
 */
class HlsRecordingRoutesIntegrationTest {

    private val recordingId = "rec-hls-itest"
    private val recordingsHlsRoot: File
        get() = File(File(System.getProperty("user.dir")), "streams/hls/recordings/$recordingId")

    @AfterTest
    fun cleanup() {
        unmockkAll()
        stopKoin()
        val root = File(File(System.getProperty("user.dir")), "streams/hls/recordings/$recordingId")
        if (root.exists()) {
            root.walkBottomUp().forEach { it.delete() }
        }
    }

    private fun bearer(role: UserRole = UserRole.VIEWER): String {
        val token = JwtConfig.generateAccessToken(
            userId = "rec-hls-user",
            username = "rec-hls",
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

    private fun sampleRecording(filePath: String?, codec: String? = null): Recording = Recording(
        id = recordingId,
        cameraId = "cam-itest",
        cameraName = null,
        startTime = 0L,
        endTime = 1L,
        duration = 1000L,
        filePath = filePath,
        fileSize = 4L,
        codec = codec,
        format = RecordingFormat.MP4,
        status = RecordingStatus.COMPLETED
    )

    private fun Application.installRecordingJwtAndKoin(
        recordingRepository: RecordingRepository,
        hlsGeneratorService: HlsGeneratorService,
        ffmpegService: FfmpegService = mockk(relaxed = true),
        signedUrlService: SignedUrlService = mockk(relaxed = true)
    ) {
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
                    single<RecordingRepository> { recordingRepository }
                    single<VideoRecordingService> { mockk(relaxed = true) }
                    single<CameraRepository> { mockk(relaxed = true) }
                    single<HlsGeneratorService> { hlsGeneratorService }
                    single<FfmpegService> { ffmpegService }
                    single<SignedUrlService> { signedUrlService }
                    single<ExportService> { mockk(relaxed = true) }
                }
            )
        }
        routing {
            route("/api/v1") {
                recordingRoutes()
            }
        }
    }

    @Test
    fun `recording hls playlist without auth returns 401`() = testApplication {
        application {
            installRecordingJwtAndKoin(
                recordingRepository = mockk(relaxed = true),
                hlsGeneratorService = mockk(relaxed = true)
            )
        }

        val response = client.get("/api/v1/recordings/$recordingId/hls/playlist.m3u8")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `recording hls playlist returns 404 when recording missing`() = testApplication {
        val repo = mockk<RecordingRepository>(relaxed = true)
        coEvery { repo.getRecordingById(any()) } returns null

        application {
            installRecordingJwtAndKoin(
                recordingRepository = repo,
                hlsGeneratorService = mockk(relaxed = true)
            )
        }

        val response = client.get("/api/v1/recordings/$recordingId/hls/playlist.m3u8") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `recording hls playlist returns 400 when file path null`() = testApplication {
        val repo = mockk<RecordingRepository>(relaxed = true)
        coEvery { repo.getRecordingById(recordingId) } returns sampleRecording(filePath = null)

        application {
            installRecordingJwtAndKoin(
                recordingRepository = repo,
                hlsGeneratorService = mockk(relaxed = true)
            )
        }

        val response = client.get("/api/v1/recordings/$recordingId/hls/playlist.m3u8") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `recording hls playlist returns 500 when generator returns null`() = testApplication {
        recordingsHlsRoot.mkdirs()
        val videoFile = File(recordingsHlsRoot, "source.mp4").apply { writeBytes(byteArrayOf(0, 1, 2, 3)) }
        val repo = mockk<RecordingRepository>(relaxed = true)
        coEvery { repo.getRecordingById(recordingId) } returns sampleRecording(filePath = videoFile.absolutePath)

        val hls = mockk<HlsGeneratorService>(relaxed = true)
        every { hls.startHlsFromRecording(any(), any(), any()) } returns null

        application {
            installRecordingJwtAndKoin(
                recordingRepository = repo,
                hlsGeneratorService = hls
            )
        }

        val response = client.get("/api/v1/recordings/$recordingId/hls/playlist.m3u8") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `recording hls playlist returns 200 and rewrites segment urls`() = testApplication {
        recordingsHlsRoot.mkdirs()
        val videoFile = File(recordingsHlsRoot, "source.mp4").apply { writeBytes(byteArrayOf(0, 1, 2, 3)) }
        val playlistFile = File(recordingsHlsRoot, "playlist.m3u8").apply { writeText(minimalPlaylist()) }
        File(recordingsHlsRoot, "segment_001.ts").writeBytes(byteArrayOf(0x47, 0x40))

        val repo = mockk<RecordingRepository>(relaxed = true)
        coEvery { repo.getRecordingById(recordingId) } returns sampleRecording(filePath = videoFile.absolutePath)

        val hls = mockk<HlsGeneratorService>(relaxed = true)
        every { hls.startHlsFromRecording(recordingId, videoFile.absolutePath, any()) } returns playlistFile.absolutePath

        application {
            installRecordingJwtAndKoin(
                recordingRepository = repo,
                hlsGeneratorService = hls
            )
        }

        val response = client.get("/api/v1/recordings/$recordingId/hls/playlist.m3u8") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("#EXTM3U"), body)
        assertTrue(body.contains("/api/v1/recordings/$recordingId/hls/"), body)
    }

    @Test
    fun `recording hls segment invalid name returns 400`() = testApplication {
        application {
            installRecordingJwtAndKoin(
                recordingRepository = mockk(relaxed = true),
                hlsGeneratorService = mockk(relaxed = true)
            )
        }

        val response = client.get("/api/v1/recordings/$recordingId/hls/not-a-segment.ts") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `recording hls segment returns 404 when file missing`() = testApplication {
        if (recordingsHlsRoot.exists()) recordingsHlsRoot.deleteRecursively()

        application {
            installRecordingJwtAndKoin(
                recordingRepository = mockk(relaxed = true),
                hlsGeneratorService = mockk(relaxed = true)
            )
        }

        val response = client.get("/api/v1/recordings/$recordingId/hls/segment_001.ts") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `recording hls segment returns 200 when file exists`() = testApplication {
        recordingsHlsRoot.mkdirs()
        File(recordingsHlsRoot, "segment_001.ts").writeBytes(byteArrayOf(0x47, 0x40))

        application {
            installRecordingJwtAndKoin(
                recordingRepository = mockk(relaxed = true),
                hlsGeneratorService = mockk(relaxed = true)
            )
        }

        val response = client.get("/api/v1/recordings/$recordingId/hls/segment_001.ts") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `recording get by id includes codec metadata`() = testApplication {
        val repo = mockk<RecordingRepository>(relaxed = true)
        coEvery { repo.getRecordingById(recordingId) } returns sampleRecording(
            filePath = "/tmp/source.mp4",
            codec = "H.265"
        )

        application {
            installRecordingJwtAndKoin(
                recordingRepository = repo,
                hlsGeneratorService = mockk(relaxed = true)
            )
        }

        val response = client.get("/api/v1/recordings/$recordingId") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"codec\":\"H.265\""), body)
    }

    @Test
    fun `recordings list includes codec metadata`() = testApplication {
        val repo = mockk<RecordingRepository>(relaxed = true)
        coEvery {
            repo.getRecordings(
                cameraId = any(),
                startTime = any(),
                endTime = any(),
                page = any(),
                limit = any()
            )
        } returns PaginatedResult(
            items = listOf(
                sampleRecording(filePath = "/tmp/source.mp4", codec = "H.264")
            ),
            total = 1,
            page = 1,
            limit = Int.MAX_VALUE,
            hasMore = false
        )

        application {
            installRecordingJwtAndKoin(
                recordingRepository = repo,
                hlsGeneratorService = mockk(relaxed = true)
            )
        }

        val response = client.get("/api/v1/recordings") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"codec\":\"H.264\""), body)
    }

    @Test
    fun `recording export query useH265 true passes through ffmpeg and returns signed url`() = testApplication {
        val exportRoot = File(File(System.getProperty("user.dir")), "exports")
        exportRoot.mkdirs()
        val source = File(recordingsHlsRoot, "source-export.mp4").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }

        val repo = mockk<RecordingRepository>(relaxed = true)
        coEvery { repo.getRecordingById(recordingId) } returns sampleRecording(
            filePath = source.absolutePath,
            codec = "H.265"
        )

        val ffmpeg = mockk<FfmpegService>(relaxed = true)
        every {
            ffmpeg.exportVideo(
                inputFile = any(),
                outputFile = any(),
                format = any(),
                quality = any(),
                startTime = any(),
                endTime = any(),
                useH265 = true
            )
        } answers {
            val outputFile = arg<File>(1)
            outputFile.parentFile?.mkdirs()
            outputFile.writeBytes(byteArrayOf(9, 9, 9))
            true
        }

        val signed = mockk<SignedUrlService>(relaxed = true)
        every { signed.generateExportSignedUrl(any(), any(), any()) } returns "/signed/export/url"

        application {
            installRecordingJwtAndKoin(
                recordingRepository = repo,
                hlsGeneratorService = mockk(relaxed = true),
                ffmpegService = ffmpeg,
                signedUrlService = signed
            )
        }

        val response = client.post("/api/v1/recordings/$recordingId/export?format=mp4&quality=high&useH265=true") {
            header(HttpHeaders.Authorization, bearer(UserRole.OPERATOR))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("/signed/export/url"), body)
        verify(atLeast = 1) {
            ffmpeg.exportVideo(
                inputFile = any(),
                outputFile = any(),
                format = any(),
                quality = any(),
                startTime = any(),
                endTime = any(),
                useH265 = true
            )
        }
    }

    @Test
    fun `recording passport returns declared and actual codec metadata`() = testApplication {
        val mediaRoot = File(recordingsHlsRoot, "passport").apply { mkdirs() }
        val source = File(mediaRoot, "source.mp4").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }

        val repo = mockk<RecordingRepository>(relaxed = true)
        coEvery { repo.getRecordingById(recordingId) } returns sampleRecording(
            filePath = source.absolutePath,
            codec = "H.265"
        )

        val ffmpeg = mockk<FfmpegService>(relaxed = true)
        every { ffmpeg.getVideoInfo(any()) } returns mapOf(
            "duration" to "1.234",
            "size" to "1024",
            "width" to 1920,
            "height" to 1080,
            "bitrate" to "8000000",
            "codec" to "hevc",
            "fps" to "25.00"
        )

        application {
            installRecordingJwtAndKoin(
                recordingRepository = repo,
                hlsGeneratorService = mockk(relaxed = true),
                ffmpegService = ffmpeg
            )
        }

        val response = client.get("/api/v1/recordings/$recordingId/passport") {
            header(HttpHeaders.Authorization, bearer())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"declaredCodec\":\"H.265\""), body)
        assertTrue(body.contains("\"actualCodec\":\"hevc\""), body)
        assertTrue(body.contains("\"width\":1920"), body)
        assertTrue(body.contains("\"height\":1080"), body)
    }
}
