package com.company.ipcamera.server.integration

import com.company.ipcamera.server.routing.hlsRoutes
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Публичные HLS-маршруты [hlsRoutes]: `/api/v1/cameras/streams/{streamId}/hls/…` без JWT.
 */
class HlsPublicRoutesIntegrationTest {

    private val streamId = "pub-hls-stream"
    private val baseDir: File
        get() = File(File(System.getProperty("user.dir")), "streams/hls/$streamId")

    @AfterTest
    fun cleanup() {
        val root = File(File(System.getProperty("user.dir")), "streams/hls/$streamId")
        if (root.exists()) {
            root.walkBottomUp().forEach { it.delete() }
        }
    }

    private fun minimalPlaylist(): String = """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXTINF:2.0,
        segment_001.ts
        #EXT-X-ENDLIST
    """.trimIndent()

    @Test
    fun `public hls playlist returns 404 when missing`() = testApplication {
        if (baseDir.exists()) baseDir.deleteRecursively()
        application {
            routing {
                route("/api/v1") {
                    route("/cameras") {
                        hlsRoutes()
                    }
                }
            }
        }

        val response = client.get("/api/v1/cameras/streams/$streamId/hls/playlist.m3u8")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `public hls playlist returns 200 and rewrites segment urls`() = testApplication {
        baseDir.mkdirs()
        File(baseDir, "playlist.m3u8").writeText(minimalPlaylist())
        File(baseDir, "segment_001.ts").writeBytes(byteArrayOf(0x47, 0x40))

        application {
            routing {
                route("/api/v1") {
                    route("/cameras") {
                        hlsRoutes()
                    }
                }
            }
        }

        val response = client.get("/api/v1/cameras/streams/$streamId/hls/playlist.m3u8")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("#EXTM3U"), body)
        assertTrue(body.contains("/api/v1/cameras/streams/$streamId/hls/"), body)
    }

    @Test
    fun `public hls segment wrong pattern returns 400`() = testApplication {
        application {
            routing {
                route("/api/v1") {
                    route("/cameras") {
                        hlsRoutes()
                    }
                }
            }
        }

        val response = client.get("/api/v1/cameras/streams/$streamId/hls/notsegment.ts")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `public hls segment returns 404 when file missing`() = testApplication {
        if (baseDir.exists()) baseDir.deleteRecursively()
        application {
            routing {
                route("/api/v1") {
                    route("/cameras") {
                        hlsRoutes()
                    }
                }
            }
        }

        val response = client.get("/api/v1/cameras/streams/$streamId/hls/segment_001.ts")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `public hls segment returns 200 when file exists`() = testApplication {
        baseDir.mkdirs()
        File(baseDir, "segment_001.ts").writeBytes(byteArrayOf(0x47, 0x40))

        application {
            routing {
                route("/api/v1") {
                    route("/cameras") {
                        hlsRoutes()
                    }
                }
            }
        }

        val response = client.get("/api/v1/cameras/streams/$streamId/hls/segment_001.ts")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
