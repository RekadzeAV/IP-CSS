package com.company.ipcamera.core.network.video

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoFrameConversionTest {

    @Test
    fun `converts RGB24 frame to BufferedImage`() {
        val frame = DecodedVideoFrame(
            data = byteArrayOf(
                0x00, 0x00, 0x00, // black
                0x7F, 0x7F, 0x7F, // gray
                0xFF.toByte(), 0x00, 0x00, // red
                0x00, 0xFF.toByte(), 0x00 // green
            ),
            width = 2,
            height = 2,
            timestamp = 1L,
            format = DecodedVideoFrame.PixelFormat.RGB24
        )

        val image = frame.toBufferedImage()

        assertNotNull(image)
        assertEquals(2, image!!.width)
        assertEquals(2, image.height)
        assertEquals(0x000000, image.getRGB(0, 0) and 0xFFFFFF)
        assertEquals(0x7F7F7F, image.getRGB(1, 0) and 0xFFFFFF)
        assertEquals(0xFF0000, image.getRGB(0, 1) and 0xFFFFFF)
        assertEquals(0x00FF00, image.getRGB(1, 1) and 0xFFFFFF)
    }

    @Test
    fun `converts YUV420 frame to BufferedImage`() {
        val width = 2
        val height = 2
        val yPlane = byteArrayOf(16, 16, 16, 16) // black in limited range
        val uPlane = byteArrayOf(128.toByte()) // neutral chroma
        val vPlane = byteArrayOf(128.toByte()) // neutral chroma

        val frame = DecodedVideoFrame(
            data = yPlane + uPlane + vPlane,
            width = width,
            height = height,
            timestamp = 2L,
            format = DecodedVideoFrame.PixelFormat.YUV420
        )

        val image = frame.toBufferedImage()

        assertNotNull(image)
        repeat(height) { row ->
            repeat(width) { col ->
                val rgb = image!!.getRGB(col, row) and 0xFFFFFF
                assertEquals(0x000000, rgb)
            }
        }
    }

    @Test
    fun `returns null for invalid YUV420 frame size`() {
        val frame = DecodedVideoFrame(
            data = ByteArray(5),
            width = 4,
            height = 4,
            timestamp = 3L,
            format = DecodedVideoFrame.PixelFormat.YUV420
        )

        assertNull(frame.toBufferedImage())
    }

    @Test
    fun `YUV420 conversion keeps near-equal RGB channels for neutral chroma`() {
        val width = 2
        val height = 2
        val yPlane = byteArrayOf(126, 126, 126, 126)
        val uPlane = byteArrayOf(128.toByte())
        val vPlane = byteArrayOf(128.toByte())

        val frame = DecodedVideoFrame(
            data = yPlane + uPlane + vPlane,
            width = width,
            height = height,
            timestamp = 4L,
            format = DecodedVideoFrame.PixelFormat.YUV420
        )

        val image = frame.toBufferedImage()

        assertNotNull(image)
        val rgb = image!!.getRGB(0, 0)
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        assertTrue(kotlin.math.abs(r - g) <= 2)
        assertTrue(kotlin.math.abs(g - b) <= 2)
    }
}
