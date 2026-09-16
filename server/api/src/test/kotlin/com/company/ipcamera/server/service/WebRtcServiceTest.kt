package com.company.ipcamera.server.service

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebRtcServiceTest {

    /** Сервис с Janus = null — ветка placeholder (getRtspUrl не вызывается) */
    private fun serviceNoJanus() = WebRtcService(
        videoStreamService = VideoStreamService(),
        janusGatewayService = null
    )

    @Test
    fun `handleOffer without janus returns fallback answer built from offer`() = runBlocking {
        val service = serviceNoJanus()
        val offer = listOf(
            "v=0",
            "o=- 42 1 IN IP4 127.0.0.1",
            "s=-",
            "t=0 0",
            "m=video 9 UDP/TLS/RTP/SAVPF 96",
            "c=IN IP4 0.0.0.0",
            "a=sendonly",
            "a=rtpmap:96 H264/90000"
        ).joinToString("\r\n")

        val result = service.handleOffer("cam-1", offer)

        assertTrue(result.isSuccess)
        val answer = result.getOrThrow()
        assertTrue(answer.answer.contains("v=0"))
        assertTrue(answer.answer.contains("m=video"))
        assertTrue(answer.answer.contains("a=rtpmap:96 H264/90000"), "кодек должен наследоваться из offer")
        assertTrue(answer.answer.contains("a=recvonly"), "направление зеркалится: sendonly → recvonly")
        assertTrue(!answer.answer.contains("placeholder"), "фиктивные значения не допускаются")
        assertTrue(answer.iceCandidates.isEmpty())
    }

    @Test
    fun `closeConnection on unknown id is a no-op`() = runBlocking {
        val service = serviceNoJanus()
        service.closeConnection("missing-id")
    }

    @Test
    fun `closeConnectionsForCamera on unknown camera is a no-op`() = runBlocking {
        val service = serviceNoJanus()
        service.closeConnectionsForCamera("unknown-camera")
    }

    @Test
    fun `cleanupOldConnections never throws`() = runBlocking {
        val service = serviceNoJanus()
        service.handleOffer("cam-1", "offer")
        service.cleanupOldConnections()
    }

    @Test
    fun `fallback handleOffer registers then closes connections`() = runBlocking {
        val service = serviceNoJanus()
        val r1 = service.handleOffer("cam-1", "o1")
        val r2 = service.handleOffer("cam-1", "o2")
        assertTrue(r1.isSuccess)
        assertTrue(r2.isSuccess)
        // Все созданные fallback-соединения компактны и закрываются без ошибок
        service.cleanupOldConnections()
        service.closeConnectionsForCamera("cam-1")
    }

    // -------------------------------------------------------------------------
    // Fake gateway: путь через медиа-шлюз (B1)
    // -------------------------------------------------------------------------

    /** Фейк медиа-шлюза, записывающий последовательность вызовов. */
    private class FakeGateway(
        private val available: Boolean = true,
        private val failAt: String? = null
    ) : WebRtcMediaGateway {
        val calls = mutableListOf<String>()
        var stopped = false
        var destroyed = false
        var detached = false

        private fun step(name: String, vararg args: Any?): Result<Long> {
            calls += name
            return if (failAt == name) {
                Result.failure(Exception("fake failure at $name"))
            } else {
                Result.success(1L)
            }
        }

        override suspend fun isAvailable(): Boolean {
            calls += "isAvailable"
            return available
        }

        // Имитация контракта реального шлюза: сессия кэшируется по cameraId.
        private val sessionByCamera = mutableMapOf<String, Long>()

        override suspend fun getOrCreateSession(cameraId: String): Result<Long> {
            sessionByCamera[cameraId]?.let { return Result.success(it) }
            val created = step("session", cameraId).map { 101L }
            created.getOrThrow()
            sessionByCamera[cameraId] = 101L
            return created
        }

        override suspend fun getOrCreateHandle(cameraId: String, sessionId: Long): Result<Long> =
            step("handle", cameraId, sessionId).map { 202L }

        override suspend fun getOrCreateRtspStream(sessionId: Long, handleId: Long, rtspUrl: String): Result<Long> =
            step("stream", sessionId, handleId, rtspUrl).map { 303L }

        override suspend fun startStream(sessionId: Long, handleId: Long, streamId: Long): Result<Unit> {
            calls += "start"
            return if (failAt == "start") Result.failure(Exception("fake start failure")) else Result.success(Unit)
        }

        override suspend fun handleOffer(sessionId: Long, handleId: Long, offer: String): Result<JanusWebRtcAnswer> {
            calls += "offer"
            return if (failAt == "offer") {
                Result.failure(Exception("fake offer failure"))
            } else {
                Result.success(
                    JanusWebRtcAnswer(
                        answer = "v=0\r\no=- 1 1 IN IP4 0.0.0.0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\n",
                        iceCandidates = emptyList()
                    )
                )
            }
        }

        override suspend fun stopStream(sessionId: Long, handleId: Long, streamId: Long): Result<Unit> {
            calls += "stop"
            stopped = true
            return Result.success(Unit)
        }

        override suspend fun destroyStream(sessionId: Long, handleId: Long, streamId: Long): Result<Unit> {
            calls += "destroy"
            destroyed = true
            return Result.success(Unit)
        }

        override suspend fun detachHandle(sessionId: Long, handleId: Long): Result<Unit> {
            calls += "detach"
            detached = true
            return Result.success(Unit)
        }
    }

    private fun rtspOffer(): String = listOf(
        "v=0",
        "o=- 7 1 IN IP4 127.0.0.1",
        "s=-",
        "t=0 0",
        "m=video 9 UDP/TLS/RTP/SAVPF 96",
        "c=IN IP4 0.0.0.0",
        "a=sendonly",
        "a=rtpmap:96 H264/90000"
    ).joinToString("\r\n")
    @Test
    fun `handleOffer via gateway processes full happy path`() = runBlocking {
        val gateway = FakeGateway()
        val service = FixedUrlService(VideoStreamService(), gateway)

        val result = service.handleOffer("cam-1", rtspOffer())

        assertTrue(result.isSuccess, "happy path должен проходить: ${result.exceptionOrNull()}")
        assertTrue(gateway.calls.contains("session"))
        assertTrue(gateway.calls.contains("handle"))
        assertTrue(gateway.calls.contains("stream"))
        assertTrue(gateway.calls.contains("start"))
        assertTrue(gateway.calls.contains("offer"))
        assertTrue(!gateway.stopped && !gateway.destroyed && !gateway.detached)
    }

    @Test
    fun `handleOffer via gateway returns error when session fails`() = runBlocking {
        val gateway = FakeGateway(failAt = "session")
        val service = FixedUrlService(VideoStreamService(), gateway)

        val result = service.handleOffer("cam-1", rtspOffer())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("session", ignoreCase = true))
        assertTrue(!gateway.calls.contains("start"), "после отказа сессии старт потока не выполняется")
    }

    @Test
    fun `handleOffer via gateway returns error when stream creation fails`() = runBlocking {
        val gateway = FakeGateway(failAt = "stream")
        val service = FixedUrlService(VideoStreamService(), gateway)

        val result = service.handleOffer("cam-1", rtspOffer())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("stream", ignoreCase = true))
        assertTrue(!gateway.calls.contains("start"))
    }

    @Test
    fun `handleOffer via gateway returns error when rtsp url is missing`() = runBlocking {
        val gateway = FakeGateway()
        val service = WebRtcService(VideoStreamService(), gateway) // resolver вернёт null

        val result = service.handleOffer("cam-1", rtspOffer())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("RTSP URL not available"))
        assertTrue(gateway.calls.none { it == "session" }, "без RTSP URL шлюз не должен вызываться")
    }

    @Test
    fun `closeConnectionsForCamera stops and destroys gateway resources`() = runBlocking {
        val gateway = FakeGateway()
        val service = FixedUrlService(VideoStreamService(), gateway)
        service.handleOffer("cam-1", rtspOffer())

        service.closeConnectionsForCamera("cam-1")

        assertTrue(gateway.stopped, "поток должен быть остановлен")
        assertTrue(gateway.detached, "handle должен быть отсоединён")
        assertTrue(gateway.destroyed, "поток должен быть удалён")
    }

    @Test
    fun `gateway session is reused for repeated offers on same camera`() = runBlocking {
        val gateway = FakeGateway()
        val service = FixedUrlService(VideoStreamService(), gateway)
        val r1 = service.handleOffer("cam-1", rtspOffer())
        val r2 = service.handleOffer("cam-1", rtspOffer())
        assertTrue(r1.isSuccess && r2.isSuccess)

        val sessionCount = gateway.calls.count { it == "session" }
        assertEquals(1, sessionCount, "сессия камеры должна переиспользоваться между offer'ами")
    }

    /** Сервис с детерминированным RTSP URL для gateway-тестов. */
    private class FixedUrlService(
        videoStreamService: VideoStreamService,
        janusGatewayService: WebRtcMediaGateway?
    ) : WebRtcService(videoStreamService, janusGatewayService) {
        override fun resolveRtspUrl(cameraId: String): String? = "rtsp://cam.local/stream1"
    }
}