package com.company.ipcamera.server.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OfferSdpAnswerFactoryTest {

    private val offer = listOf(
        "v=0",
        "o=- 4611731400430051336 2 IN IP4 127.0.0.1",
        "s=-",
        "t=0 0",
        "a=group:BUNDLE 0",
        "a=msid-semantic: WMS",
        "m=video 9 UDP/TLS/RTP/SAVPF 96 98 102",
        "c=IN IP4 0.0.0.0",
        "a=ice-ufrag:4ZcD",
        "a=ice-pwd:2/1muCWoOi3uLifh0NuRHlFJ",
        "a=fingerprint:sha-256 00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF",
        "a=setup:actpass",
        "a=mid:0",
        "a=sendonly",
        "a=rtcp-mux",
        "a=rtpmap:96 H264/90000",
        "a=rtcp-fb:96 nack pli",
        "a=fmtp:96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f",
        "a=rtpmap:98 VP8/90000",
        "a=rtpmap:102 H265/90000"
    ).joinToString("\r\n") + "\r\n"

    @Test
    fun `answer inherits payload types and codecs from offer`() {
        val answer = OfferSdpAnswerFactory.buildAnswer(offer)

        val mLine = answer.lineSequence().first { it.startsWith("m=video") }
        assertEquals(
            "m=video 9 UDP/TLS/RTP/SAVPF 96 98 102",
            mLine,
            "answer должен наследовать proto и все payload-type из offer"
        )

        assertTrue(answer.contains("a=rtpmap:96 H264/90000"))
        assertTrue(answer.contains("a=fmtp:96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f"))
        assertTrue(answer.contains("a=rtpmap:98 VP8/90000"))
        // rtpmap:102 из offer тоже копируется (H265)
        assertTrue(answer.contains("a=rtpmap:102 H265/90000"))
        // а несуществующий rtpmap не появляется
        assertTrue(!answer.contains("a=rtpmap:103"))
    }

    @Test
    fun `answer mirrors ice and dtls parameters from offer`() {
        val answer = OfferSdpAnswerFactory.buildAnswer(offer)

        assertTrue(answer.contains("a=ice-ufrag:4ZcD"))
        assertTrue(answer.contains("a=ice-pwd:2/1muCWoOi3uLifh0NuRHlFJ"))
        assertTrue(
            answer.contains("a=fingerprint:sha-256 00:11:22:33:44:55:66:77"),
            "fingerprint должен быть унаследован"
        )
        assertEquals("a=setup:active", answer.lineSequence().first { it.startsWith("a=setup:") })
        assertTrue(answer.contains("a=mid:0"))
        assertTrue(answer.contains("a=rtcp-mux"))
    }

    @Test
    fun `direction mirrors offer sendonly to recvonly`() {
        val answer = OfferSdpAnswerFactory.buildAnswer(offer)
        assertEquals("a=recvonly", answer.lineSequence().first { it.startsWith("a=send") || it.startsWith("a=recv") || it.startsWith("a=inactive") })
    }

    @Test
    fun `session id reused and version incremented`() {
        val answer = OfferSdpAnswerFactory.buildAnswer(offer)
        val oLine = answer.lineSequence().first { it.startsWith("o=") }
        val parts = oLine.split(" ")
        assertEquals("4611731400430051336", parts[1], "session-id должен наследоваться")
        assertEquals("3", parts[2], "session-version должен быть увеличен на 1")
    }

    @Test
    fun `offer without video section falls back to first media section`() {
        val audioOffer = offer
            .replace("m=video 9 UDP/TLS/RTP/SAVPF 96 98 102", "m=audio 9 UDP/TLS/RTP/SAVPF 111")
            .replace("a=rtpmap:96 H264/90000", "a=rtpmap:111 opus/48000/2")
        val answer = OfferSdpAnswerFactory.buildAnswer(audioOffer)

        val mLine = answer.lineSequence().first { it.startsWith("m=") }
        assertTrue(
            mLine.startsWith("m=audio"),
            "должна использоваться первая доступная media-секция, answer=$answer"
        )
        assertTrue(answer.contains("a=rtpmap:111 opus/48000/2"))
    }

    @Test
    fun `offer without any media lines produces minimal valid answer`() {
        val bare = "v=0\r\no=- 123 1 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n"
        val answer = OfferSdpAnswerFactory.buildAnswer(bare)

        assertTrue(answer.startsWith("v=0"))
        assertTrue(answer.lineSequence().any { it.startsWith("m=") })
    }

    @Test
    fun `offer without ice attributes gets generated ones`() {
        val minimal = listOf(
            "v=0",
            "o=- 999 1 IN IP4 127.0.0.1",
            "s=-",
            "t=0 0",
            "m=video 9 UDP/TLS/RTP/SAVPF 96",
            "c=IN IP4 0.0.0.0",
            "a=sendonly",
            "a=rtpmap:96 H264/90000"
        ).joinToString("\r\n") + "\r\n"

        val answer = OfferSdpAnswerFactory.buildAnswer(minimal)

        val ufrag = answer.lineSequence().first { it.startsWith("a=ice-ufrag:") }
        val pwd = answer.lineSequence().first { it.startsWith("a=ice-pwd:") }
        assertTrue(ufrag.removePrefix("a=ice-ufrag:").length == 8, "ufrag должен быть сгенерирован")
        assertTrue(pwd.removePrefix("a=ice-pwd:").length == 24, "pwd должен быть сгенерирован")
        assertNotEquals(ufrag, pwd)
    }

    @Test
    fun `answers for different offers differ in generated ice params`() {
        val a1 = OfferSdpAnswerFactory.buildAnswer(offer)
        val minimal = offer.replace("a=ice-ufrag:4ZcD", "a=ice-ufrag:ZZZZ")
        val a2 = OfferSdpAnswerFactory.buildAnswer(minimal)

        val u1 = a1.lineSequence().first { it.startsWith("a=ice-ufrag:") }
        val u2 = a2.lineSequence().first { it.startsWith("a=ice-ufrag:") }
        assertNotEquals(u1, u2)
    }
}
