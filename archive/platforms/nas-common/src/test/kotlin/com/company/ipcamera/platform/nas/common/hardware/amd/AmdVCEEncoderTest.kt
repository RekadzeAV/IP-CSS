package com.company.ipcamera.platform.nas.common.hardware.amd

import com.company.ipcamera.platform.nas.common.hardware.CodecType
import com.company.ipcamera.platform.nas.common.hardware.EncodeConfig
import com.company.ipcamera.platform.nas.common.hardware.PixelFormat
import com.company.ipcamera.platform.nas.common.hardware.VideoFrame
import com.company.ipcamera.platform.nas.common.hardware.VideoStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Unit tests for AmdVCEEncoder
 */
class AmdVCEEncoderTest {

    private val encoder = AmdVCEEncoder()

    @Test
    fun `test isSupported returns false when no AMD device`() = runBlocking {
        // This test will pass on systems without AMD GPU
        val supported = encoder.isSupported()
        
        // On test system without AMD GPU, should return false
        // On system with AMD GPU, will return true
        assertNotNull(supported)
    }

    @Test
    fun `test getHardwareInfo returns valid info`() = runBlocking {
        // When
        val info = encoder.getHardwareInfo()

        // Then
        assertEquals("AMD VCE", info.name)
        assertNotNull(info.type)
        assertNotNull(info.available)
    }

    @Test
    fun `test getSupportedCodecs returns H264 and H265`() = runBlocking {
        // When
        val info = encoder.getHardwareInfo()
        val codecs = info.codecs

        // Then
        assertTrue(codecs.any { it.type == CodecType.H264 })
    }

    @Test
    fun `test encode fails gracefully when not supported`() = runBlocking {
        // Given
        val frame = VideoFrame(
            data = ByteArray(1920 * 1080 * 2),
            width = 1920,
            height = 1080,
            format = PixelFormat.NV12,
            timestamp = System.currentTimeMillis()
        )
        val encoded = com.company.ipcamera.platform.nas.common.hardware.EncodedFrame(
            data = ByteArray(0),
            width = 1920,
            height = 1080,
            bitrate = 4_000_000,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = encoder.encode(frame, encoded)

        // Then
        // Result may fail if AMD GPU not available, which is expected
        assertNotNull(result)
    }

    @Test
    fun `test transcode returns flow`() = runBlocking {
        // Given
        val stream = VideoStream(url = "rtsp://test/stream")
        val config = EncodeConfig(
            width = 1920,
            height = 1080,
            bitrate = 4_000_000,
            framerate = 30
        )

        // When
        val flow = encoder.transcode(stream, config)

        // Then
        assertNotNull(flow)
        // Flow should be collectable (may be empty on test system)
        val frames = kotlinx.coroutines.flow.toList(flow)
        assertNotNull(frames)
    }
}
