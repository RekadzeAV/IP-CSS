package com.company.ipcamera.core.network.test

import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamType
import com.company.ipcamera.core.network.video.VideoCodec
import com.company.ipcamera.core.network.video.VideoDecoder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for VideoDecoder Android stub implementation.
 * * These tests verify that the stub implementation behaves correctly
 * by returning default values and not throwing exceptions.
 * * Note: Full integration tests with JavaCV should be in desktopMain.
 */
class VideoDecoderAndroidStubTest {

    @Test
    fun `stub decoder should be created successfully`() {
        // When
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)

        // Then
        assertNotNull(decoder, "Stub decoder should be created")
    }

    @Test
    fun `stub decoder release should not throw exception`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)

        // When
        decoder.release()

        // Then - should not throw
        assertTrue(true, "Release should not throw exception")
    }

    @Test
    fun `stub decoder decode should return false`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)
        val frame = RtspFrame(
            data = ByteArray(100),
            timestamp = System.currentTimeMillis(),
            streamType = RtspStreamType.VIDEO
        )

        // When
        val result = decoder.decode(frame)

        // Then
        assertTrue(!result, "Stub decoder should return false")
    }

    @Test
    fun `stub decoder setCallback should not throw exception`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)

        // When
        decoder.setCallback(null)

        // Then
        assertTrue(true, "setCallback should not throw")
    }

    @Test
    fun `stub decoder getInfo should return null`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)

        // When
        val info = decoder.getInfo()

        // Then
        assertTrue(info == null, "Stub decoder should return null for getInfo")
    }
}
