package com.company.ipcamera.core.decoder

import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для VideoDecoder
 */
class VideoDecoderTest {
    
    @Test
    fun testDecoderConfigCreation() {
        val config = DecoderConfig(
            format = VideoFormat.H264,
            width = 1920,
            height = 1080,
            fps = 30,
            bitrate = 4096,
            useHardwareAcceleration = true,
            maxBufferFrames = 10,
            outputFormat = PixelFormat.RGBA
        )
        
        assertEquals(VideoFormat.H264, config.format)
        assertEquals(1920, config.width)
        assertEquals(1080, config.height)
        assertEquals(30, config.fps)
        assertEquals(4096, config.bitrate)
        assertTrue(config.useHardwareAcceleration)
        assertEquals(10, config.maxBufferFrames)
        assertEquals(PixelFormat.RGBA, config.outputFormat)
    }
    
    @Test
    fun testVideoFormatEnum() {
        assertEquals(0, VideoFormat.UNKNOWN.ordinal)
        assertEquals(1, VideoFormat.H264.ordinal)
        assertEquals(2, VideoFormat.H265.ordinal)
        assertEquals(3, VideoFormat.MPEG4.ordinal)
        assertEquals(4, VideoFormat.VP8.ordinal)
        assertEquals(5, VideoFormat.VP9.ordinal)
    }
    
    @Test
    fun testPixelFormatEnum() {
        assertEquals(0, PixelFormat.UNKNOWN.ordinal)
        assertEquals(1, PixelFormat.RGBA.ordinal)
        assertEquals(2, PixelFormat.RGB24.ordinal)
        assertEquals(3, PixelFormat.NV12.ordinal)
        assertEquals(4, PixelFormat.I420.ordinal)
        assertEquals(5, PixelFormat.YUYV.ordinal)
    }
    
    @Test
    fun testDecoderStatusEnum() {
        assertEquals(0, DecoderStatus.UNINITIALIZED.ordinal)
        assertEquals(1, DecoderStatus.INITIALIZING.ordinal)
        assertEquals(2, DecoderStatus.READY.ordinal)
        assertEquals(3, DecoderStatus.DECODING.ordinal)
        assertEquals(4, DecoderStatus.PAUSED.ordinal)
        assertEquals(5, DecoderStatus.ERROR.ordinal)
        assertEquals(6, DecoderStatus.CLOSED.ordinal)
    }
    
    @Test
    fun testDecoderTypeEnum() {
        assertEquals(0, DecoderType.UNKNOWN.ordinal)
        assertEquals(1, DecoderType.SOFTWARE.ordinal)
        assertEquals(2, DecoderType.DXVA2.ordinal)
        assertEquals(3, DecoderType.VAAPI.ordinal)
        assertEquals(4, DecoderType.VIDEOTOOLBOX.ordinal)
        assertEquals(5, DecoderType.QSV.ordinal)
        assertEquals(6, DecoderType.NVDEC.ordinal)
    }
    
    @Test
    fun testDecodedFrameCreation() {
        val frame = DecodedFrame(
            data = ByteArray(1024),
            width = 1920,
            height = 1080,
            timestamp = 1000,
            isKeyFrame = true,
            format = PixelFormat.RGBA,
            stride = 1920 * 4,
            pts = 1000,
            dts = 1000
        )
        
        assertEquals(1920, frame.width)
        assertEquals(1080, frame.height)
        assertEquals(1000, frame.timestamp)
        assertTrue(frame.isKeyFrame)
        assertEquals(PixelFormat.RGBA, frame.format)
        assertEquals(7680, frame.stride)
    }
    
    @Test
    fun testDecoderStatsCreation() {
        val stats = DecoderStats(
            framesDecoded = 100,
            framesDropped = 2,
            decodingTimeMs = 500,
            averageFrameTimeMs = 5.0,
            minFrameTimeMs = 3,
            maxFrameTimeMs = 10,
            cpuUsagePercent = 15.5,
            memoryUsageBytes = 50_000_000
        )
        
        assertEquals(100, stats.framesDecoded)
        assertEquals(2, stats.framesDropped)
        assertEquals(500, stats.decodingTimeMs)
        assertEquals(5.0, stats.averageFrameTimeMs)
        assertEquals(15.5, stats.cpuUsagePercent)
        assertEquals(50_000_000, stats.memoryUsageBytes)
    }
    
    @Test
    fun testHardwareDecoderInfoCreation() {
        val info = HardwareDecoderInfo(
            supported = true,
            decoderType = DecoderType.DXVA2,
            supportedFormats = listOf(VideoFormat.H264, VideoFormat.H265),
            maxResolution = IntWidthHeight(3840, 2160),
            capabilities = DecoderCapabilities(
                maxConcurrentStreams = 4,
                supportedResolutions = listOf(
                    IntWidthHeight(1920, 1080),
                    IntWidthHeight(3840, 2160)
                ),
                supportsBFrames = true,
                supportsRefFrames = true,
                maxReferenceFrames = 16
            )
        )
        
        assertTrue(info.supported)
        assertEquals(DecoderType.DXVA2, info.decoderType)
        assertEquals(2, info.supportedFormats.size)
        assertEquals(3840, info.maxResolution.width)
        assertEquals(2160, info.maxResolution.height)
    }
    
    @Test
    fun testDecoderCapabilitiesCreation() {
        val caps = DecoderCapabilities(
            maxConcurrentStreams = 4,
            supportedResolutions = listOf(
                IntWidthHeight(1920, 1080),
                IntWidthHeight(3840, 2160)
            ),
            supportsBFrames = true,
            supportsRefFrames = true,
            maxReferenceFrames = 16
        )
        
        assertEquals(4, caps.maxConcurrentStreams)
        assertEquals(2, caps.supportedResolutions.size)
        assertTrue(caps.supportsBFrames)
        assertEquals(16, caps.maxReferenceFrames)
    }
    
    @Test
    fun testIntWidthHeightCreation() {
        val size = IntWidthHeight(1920, 1080)
        
        assertEquals(1920, size.width)
        assertEquals(1080, size.height)
    }
    
    @Test
    fun testDecoderFactory() = runTest {
        val factory = DecoderFactory()
        
        // Создаём декодер
        val config = DecoderConfig(
            format = VideoFormat.H264,
            width = 1920,
            height = 1080,
            fps = 30,
            bitrate = 4096,
            useHardwareAcceleration = false
        )
        
        val decoder = factory.getDecoder(DecoderType.SOFTWARE, config)
        
        assertNotNull(decoder)
        
        // Закрываем декодер
        factory.closeAll()
        
        assertTrue(true) // Если дошли до сюда - тест прошёл
    }
    
    @Test
    fun testDecoderDisplayName() {
        assertEquals("Software", DecoderType.SOFTWARE.displayName)
        assertEquals("DXVA2 (DirectX)", DecoderType.DXVA2.displayName)
        assertEquals("VAAPI (Linux)", DecoderType.VAAPI.displayName)
        assertEquals("VideoToolbox (Apple)", DecoderType.VIDEOTOOLBOX.displayName)
        assertEquals("Quick Sync (Intel)", DecoderType.QSV.displayName)
        assertEquals("NVIDIA Decoder", DecoderType.NVDEC.displayName)
    }
    
    @Test
    fun testVideoFormatDisplayName() {
        assertEquals("H.264 / AVC", VideoFormat.H264.displayName)
        assertEquals("H.265 / HEVC", VideoFormat.H265.displayName)
        assertEquals("MPEG-4", VideoFormat.MPEG4.displayName)
        assertEquals("VP8", VideoFormat.VP8.displayName)
        assertEquals("VP9", VideoFormat.VP9.displayName)
    }
    
    @Test
    fun testPixelFormatDisplayName() {
        assertEquals("RGBA (32-bit)", PixelFormat.RGBA.displayName)
        assertEquals("RGB24 (24-bit)", PixelFormat.RGB24.displayName)
        assertEquals("NV12 (YUV)", PixelFormat.NV12.displayName)
        assertEquals("I420 (YUV)", PixelFormat.I420.displayName)
        assertEquals("YUYV (YUV)", PixelFormat.YUYV.displayName)
    }
    
    @Test
    fun testVideoBufferCreation() {
        val buffer = VideoBuffer(
            data = ByteArray(1024),
            size = 1024,
            timestamp = 1000,
            isKeyFrame = true
        )
        
        assertEquals(1024, buffer.size)
        assertEquals(1000, buffer.timestamp)
        assertTrue(buffer.isKeyFrame)
    }
}
