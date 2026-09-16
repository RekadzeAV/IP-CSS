package com.company.ipcamera.core.network.test

import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamType
import com.company.ipcamera.core.network.video.VideoCodec
import com.company.ipcamera.core.network.video.VideoDecoder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for VideoDecoder Desktop (JVM) implementation.
 * * These tests verify the stub implementation in jvmMain source set.
 */
class VideoDecoderDesktopTest {

    @Test
    fun `decoder should be created successfully`() {
        // When
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)

        // Then
        assertNotNull(decoder, "VideoDecoder should be created")
    }

    @Test
    fun `decoder decode should return false for stub`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)
        val frame = RtspFrame(
            data = ByteArray(100),
            timestamp = System.currentTimeMillis(),
            streamType = RtspStreamType.VIDEO,
            width = 1920,
            height = 1080
        )

        // When
        val result = decoder.decode(frame)

        // Then - stub returns false
        assertTrue(!result, "Stub decoder should return false")
    }

    @Test
    fun `decoder release should not throw exception`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)

        // When
        decoder.release()

        // Then - should not throw
        assertTrue(true, "Release should not throw exception")
    }

    @Test
    fun `decoder setCallback should not throw exception`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)

        // When
        decoder.setCallback(null)

        // Then - should not throw
        assertTrue(true, "setCallback should not throw exception")
    }

    @Test
    fun `decoder getInfo should return null for stub`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)

        // When
        val info = decoder.getInfo()

        // Then - stub returns null
        assertTrue(info == null, "Stub decoder should return null for getInfo")
    }

    @Test
    fun `decoder should handle multiple release calls`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)

        // When
        decoder.release()
        decoder.release()

        // Then - should not throw
        assertTrue(true, "Multiple releases should not throw")
    }
}
