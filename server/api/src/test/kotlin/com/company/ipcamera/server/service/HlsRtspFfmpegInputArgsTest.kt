package com.company.ipcamera.server.service

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HlsRtspFfmpegInputArgsTest {

    @Test
    fun `non-rtsp url yields only dash i and url`() {
        assertContentEquals(
            listOf("-i", "http://example/stream"),
            rtspFfmpegInputArgs("http://example/stream")
        )
    }

    @Test
    fun `rtsp url includes tcp transport and input`() {
        val args = rtspFfmpegInputArgs("rtsp://192.168.1.10/stream")
        assertEquals(listOf("-rtsp_transport", "tcp"), args.take(2))
        assertEquals("-i", args[args.size - 2])
        assertEquals("rtsp://192.168.1.10/stream", args.last())
        // -rw_timeout и -stimeout больше не используются (не поддерживаются в FFmpeg 8.x)
        assertTrue(!args.contains("-rw_timeout"), args.toString())
        assertTrue(!args.contains("-stimeout"), args.toString())
    }

    @Test
    fun `recording to hls genpts enabled by default`() {
        if (System.getenv("FFMPEG_RECORDING_HLS_GENPTS")?.trim()?.lowercase() in setOf("0", "false")) {
            return
        }
        val flags = ffmpegRecordingToHlsInputFlags()
        assertContentEquals(listOf("-fflags", "+genpts"), flags)
    }
}
