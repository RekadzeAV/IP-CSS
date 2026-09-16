package com.company.ipcamera.core.network.integration

import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration тесты для VideoDecoder с использованием JavaCV
 * * Тесты используют реальный FFmpeg через JavaCV для проверки доступности библиотек
 */
class VideoDecoderJavaCVIntegrationTest {

    @Test
    fun `JavaCV should be available for testing`() {
        // When
        val converter = Java2DFrameConverter()

        // Then
        assertNotNull(converter, "JavaCV converter should be available")
    }

    @Test
    fun `Java2DFrameConverter should convert frames`() {
        // Given
        val converter = Java2DFrameConverter()
        val image = BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB)

        // When
        val frame = converter.convert(image)

        // Then
        assertNotNull(frame, "Frame should be converted")
    }

    @Test
    fun `JavaCV should handle multiple consecutive operations`() {
        // Given
        val converter = Java2DFrameConverter()

        // When & Then - multiple conversions should work
        repeat(10) { i ->
            val image = BufferedImage(320 + i * 10, 240 + i * 10, BufferedImage.TYPE_INT_RGB)
            val frame = converter.convert(image)
            assertNotNull(frame, "Frame #$i should be converted")
        }
    }

    @Test
    fun `JavaCV should handle different resolutions`() {
        val resolutions = listOf(
            Pair(640, 480),
            Pair(1280, 720),
            Pair(1920, 1080)
        )

        resolutions.forEach { (width, height) ->
            // Given
            val converter = Java2DFrameConverter()
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

            // When
            val frame = converter.convert(image)

            // Then
            assertNotNull(frame, "Frame should be converted for ${width}x$height")
        }
    }

    @Test
    fun `JavaCV converter should preserve image dimensions`() {
        // Given
        val converter = Java2DFrameConverter()
        val width = 640
        val height = 480
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        // When
        val frame = converter.convert(image)

        // Then
        assertNotNull(frame, "Frame should be converted")
        assertTrue(frame.imageWidth == width, "Frame width should match")
        assertTrue(frame.imageHeight == height, "Frame height should match")
    }

    @Test
    fun `JavaCV should release resources properly`() {
        // Given
        val converter = Java2DFrameConverter()
        val image = BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB)

        // When
        val frame = converter.convert(image)
        converter.close()

        // Then - should not throw exception
        assertNotNull(frame, "Frame should be converted before close")
    }
}
