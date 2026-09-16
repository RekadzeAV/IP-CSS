package com.company.ipcamera.core.network.video

import java.awt.image.BufferedImage

/**
 * Desktop-only utility to convert decoded frames to BufferedImage.
 */
fun DecodedVideoFrame.toBufferedImage(): BufferedImage? {
    return when (format) {
        DecodedVideoFrame.PixelFormat.RGB24 -> {
            try {
                val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                val raster = image.raster
                val dataBuffer = raster.dataBuffer as java.awt.image.DataBufferInt
                val pixels = dataBuffer.data

                var pixelIndex = 0
                for (i in data.indices step 3) {
                    if (i + 2 < data.size && pixelIndex < pixels.size) {
                        val r = data[i].toInt() and 0xFF
                        val g = data[i + 1].toInt() and 0xFF
                        val b = data[i + 2].toInt() and 0xFF
                        pixels[pixelIndex++] = (r shl 16) or (g shl 8) or b
                    }
                }
                image
            } catch (e: Exception) {
                null
            }
        }

        DecodedVideoFrame.PixelFormat.YUV420 -> {
            try {
                val ySize = width * height
                val uvWidth = width / 2
                val uvHeight = height / 2
                val uvSize = uvWidth * uvHeight
                val expectedSize = ySize + uvSize * 2
                if (width <= 0 || height <= 0 || width % 2 != 0 || height % 2 != 0 || data.size < expectedSize) {
                    return null
                }

                val yOffset = 0
                val uOffset = ySize
                val vOffset = ySize + uvSize
                val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                val pixels = (image.raster.dataBuffer as java.awt.image.DataBufferInt).data

                for (row in 0 until height) {
                    val uvRow = row / 2
                    for (col in 0 until width) {
                        val yValue = data[yOffset + row * width + col].toInt() and 0xFF
                        val uvIndex = uvRow * uvWidth + (col / 2)
                        val uValue = (data[uOffset + uvIndex].toInt() and 0xFF) - 128
                        val vValue = (data[vOffset + uvIndex].toInt() and 0xFF) - 128

                        val c = (yValue - 16).coerceAtLeast(0)
                        val r = ((298 * c + 409 * vValue + 128) shr 8).coerceIn(0, 255)
                        val g = ((298 * c - 100 * uValue - 208 * vValue + 128) shr 8).coerceIn(0, 255)
                        val b = ((298 * c + 516 * uValue + 128) shr 8).coerceIn(0, 255)
                        pixels[row * width + col] = (r shl 16) or (g shl 8) or b
                    }
                }

                image
            } catch (e: Exception) {
                null
            }
        }
    }
}
