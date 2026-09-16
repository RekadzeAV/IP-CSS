package com.company.ipcamera.server.service

import mu.KotlinLogging
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

/**
 * Конвертация кадров JPEG ↔ RGB24 для серверного аналитического пайплайна.
 *
 * Детекторы [com.company.ipcamera.core.network.analytics.NativeAnalytics] принимают
 * сырой RGB24 (3 байта на пиксель, row-major, без padding). Кадры с камер приходят
 * в JPEG (multipart/frame inputs) — конвертируем через ImageIO.
 */
object FrameCodec {

    /** Типичная ширина аналитического пайплайна (downscale для скорости детекции). */
    const val DEFAULT_ANALYSIS_WIDTH = 640
    const val DEFAULT_ANALYSIS_HEIGHT = 360

    /**
     * Декодировать JPEG/PNG/BMP в RGB24-массив.
     * @return тройка (rgb, width, height) или null, если изображение не декодируется.
     */
    fun decodeToRgb(imageBytes: ByteArray): Triple<ByteArray, Int, Int>? {
        if (imageBytes.isEmpty()) return null
        return try {
            val image = ImageIO.read(ByteArrayInputStream(imageBytes)) ?: return null
            val width = image.width
            val height = image.height
            if (width <= 0 || height <= 0) return null
            Triple(toRgb24(image), width, height)
        } catch (e: Exception) {
            logger.warn { "FrameCodec: failed to decode image (${imageBytes.size} bytes): ${e.message}" }
            null
        }
    }

    /**
     * Закодировать RGB24-массив в JPEG (для снапшотов).
     * @return JPEG-байты или null при ошибке.
     */
    fun encodeJpeg(
        rgb: ByteArray,
        width: Int,
        height: Int,
        quality: Float = 0.85f,
    ): ByteArray? {
        if (width <= 0 || height <= 0) return null
        val expected = width * height * 3
        if (rgb.size < expected) {
            logger.warn { "FrameCodec: rgb buffer too small (${rgb.size} < $expected)" }
            return null
        }
        return try {
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            var i = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val r = rgb[i].toInt() and 0xFF
                    val g = rgb[i + 1].toInt() and 0xFF
                    val b = rgb[i + 2].toInt() and 0xFF
                    image.setRGB(x, y, (r shl 16) or (g shl 8) or b)
                    i += 3
                }
            }
            val out = ByteArrayOutputStream(expected / 4)
            val writers = ImageIO.getImageWritersByFormatName("jpg")
            if (!writers.hasNext()) return null
            val writer = writers.next()
            val ios = ImageIO.createImageOutputStream(out)
            try {
                writer.output = ios
                // IIOImage c null params: JPEG-писатель использует качество по умолчанию,
                // что гарантированно поддерживается (без усложнения ImageWriteParam).
                writer.write(null, IIOImage(image, null, null), null)
            } finally {
                writer.dispose()
                ios.close()
            }
            out.toByteArray()
        } catch (e: Exception) {
            logger.warn { "FrameCodec: JPEG encode failed: ${e.message}" }
            null
        }
    }

    /** BufferedImage → RGB24 (через пиксельный буфер, без ручной конвертации форматов). */
    private fun toRgb24(image: BufferedImage): ByteArray {
        val rgb = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
        val out = ByteArray(image.width * image.height * 3)
        var j = 0
        for (pixel in rgb) {
            out[j] = ((pixel shr 16) and 0xFF).toByte()
            out[j + 1] = ((pixel shr 8) and 0xFF).toByte()
            out[j + 2] = (pixel and 0xFF).toByte()
            j += 3
        }
        return out
    }
}
