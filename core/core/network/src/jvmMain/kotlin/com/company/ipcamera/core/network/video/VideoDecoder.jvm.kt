package com.company.ipcamera.core.network.video

import com.company.ipcamera.core.network.RtspFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

/**
 * JVM реализация видео декодера
 *
 * Использует JavaCV/FFmpeg для декодирования H.264/H.265
 * Для MJPEG используется ImageIO (более эффективно)
 */
actual class VideoDecoder actual constructor(
    private val codec: VideoCodec,
    private val width: Int,
    private val height: Int
) {
    private var callback: DecodedFrameCallback? = null
    private var isReleased = false

    // JavaCV компоненты для H.264/H.265
    private var frameGrabber: FFmpegFrameGrabber? = null
    private var frameConverter: Java2DFrameConverter? = null
    private var decoderImpl: VideoDecoderImpl? = null

    init {
        logger.info { "Creating VideoDecoder for codec: $codec, size: ${width}x${height}" }

            when (codec) {
                VideoCodec.H264, VideoCodec.H265 -> {
                    try {
                        // Инициализация FFmpeg для H.264/H.265 через низкоуровневый API
                        decoderImpl = VideoDecoderImpl(codec, width, height)
                        frameConverter = Java2DFrameConverter()
                        logger.info { "FFmpeg decoder initialized for $codec" }
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to initialize FFmpeg decoder: ${e.message}" }
                    }
                }
            VideoCodec.MJPEG -> {
                // MJPEG не требует специальной инициализации
            }
            VideoCodec.UNKNOWN -> {
                logger.warn { "Unknown codec, decoder may not work properly" }
            }
        }
    }

    actual fun decode(frame: RtspFrame): Boolean {
        if (isReleased) {
            logger.warn { "Attempted to decode with released decoder" }
            return false
        }

        return when (codec) {
            VideoCodec.MJPEG -> {
                decodeMJPEG(frame)
            }
            VideoCodec.H264, VideoCodec.H265 -> {
                decodeH264H265(frame)
            }
            VideoCodec.UNKNOWN -> {
                logger.warn { "Unknown codec, cannot decode" }
                false
            }
        }
    }

    private fun decodeH264H265(frame: RtspFrame): Boolean {
        return try {
            val impl = decoderImpl
            if (impl == null) {
                logger.warn { "Decoder implementation not initialized" }
                return false
            }

            // Декодирование через низкоуровневый AVCodecContext
            val decodedFrame = impl.decode(frame)

            if (decodedFrame != null) {
                callback?.invoke(decodedFrame)
                true
            } else {
                logger.debug { "Failed to decode frame (codec: $codec, size: ${frame.data.size})" }
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error decoding H.264/H.265 frame: ${e.message}" }
            false
        }
    }

    private fun decodeMJPEG(frame: RtspFrame): Boolean {
        return try {
            val image = ImageIO.read(ByteArrayInputStream(frame.data))
            if (image != null) {
                // Конвертация BufferedImage в RGB24 ByteArray
                val rgbImage = if (image.type != BufferedImage.TYPE_INT_RGB) {
                    val converted = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
                    val g = converted.createGraphics()
                    g.drawImage(image, 0, 0, null)
                    g.dispose()
                    converted
                } else {
                    image
                }

                val rgbData = ByteArray(rgbImage.width * rgbImage.height * 3)
                val raster = rgbImage.raster
                val dataBuffer = raster.dataBuffer as java.awt.image.DataBufferInt
                val pixels = dataBuffer.data

                var index = 0
                for (pixel in pixels) {
                    rgbData[index++] = ((pixel shr 16) and 0xFF).toByte() // R
                    rgbData[index++] = ((pixel shr 8) and 0xFF).toByte()  // G
                    rgbData[index++] = (pixel and 0xFF).toByte()           // B
                }

                val decodedFrame = DecodedVideoFrame(
                    data = rgbData,
                    width = rgbImage.width,
                    height = rgbImage.height,
                    timestamp = frame.timestamp,
                    format = DecodedVideoFrame.PixelFormat.RGB24
                )

                callback?.invoke(decodedFrame)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error decoding MJPEG frame" }
            false
        }
    }

    actual fun setCallback(callback: DecodedFrameCallback?) {
        this.callback = callback
    }

    actual fun getInfo(): DecoderInfo? {
        if (isReleased) return null
        return DecoderInfo(width, height, codec)
    }

    actual fun release() {
        if (!isReleased) {
            isReleased = true
            callback = null

            // Освобождение FFmpeg ресурсов
            try {
                frameGrabber?.stop()
                frameGrabber?.release()
                frameGrabber = null
                frameConverter = null
                decoderImpl?.release()
                decoderImpl = null
            } catch (e: Exception) {
                logger.error(e) { "Error releasing FFmpeg resources" }
            }
            logger.info { "VideoDecoder released" }
        }
    }
}

