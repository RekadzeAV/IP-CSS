package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamInfo
import com.company.ipcamera.core.network.RtspStreamType
import mu.KotlinLogging
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

/**
 * Конвертирует JavaCV [Frame] в [RtspFrame] для передачи через pipeline.
 * Поддерживает как видео, так и аудио-кадры (AAC, PCMU, PCMA/G.711).
 */
object JvmRtspFrameConverter {

    private val java2dConverter = Java2DFrameConverter()

    /**
     * Конвертировать кадр [Frame] → [RtspFrame].
     * Поддерживает видео (JPEG) и аудио (PCM/AAC/G.711).
     */
    fun convert(frame: Frame, timestamp: Long, streamIndex: Int): RtspFrame? {
        return when {
            // Видеокадр
            frame.image != null && frame.imageWidth > 0 && frame.imageHeight > 0 ->
                convertVideoFrame(frame, timestamp)
            // Аудиокадр
            frame.samples != null ->
                convertAudioFrame(frame, timestamp, streamIndex)
            else -> null
        }
    }

    private fun convertVideoFrame(frame: Frame, timestamp: Long): RtspFrame? {
        return try {
            val width = frame.imageWidth
            val height = frame.imageHeight

            val bufferedImage = java2dConverter.getBufferedImage(frame, 1.0)
            val dataBytes = if (bufferedImage != null) {
                val baos = ByteArrayOutputStream()
                ImageIO.write(bufferedImage, "jpg", baos)
                baos.toByteArray()
            } else {
                ByteArray(0)
            }

            RtspFrame(
                data = dataBytes,
                timestamp = timestamp,
                streamType = RtspStreamType.VIDEO,
                width = width,
                height = height
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to convert video frame" }
            null
        }
    }

    private fun convertAudioFrame(frame: Frame, timestamp: Long, streamIndex: Int): RtspFrame? {
        return try {
            val samples = frame.samples
            if (samples == null || samples.isEmpty()) {
                logger.debug { "Empty audio samples at timestamp $timestamp" }
                return null
            }

            // Конвертируем samples (Array<Buffer?>) в ByteArray
            val dataBytes = convertSamplesToBytes(samples)
            if (dataBytes.isEmpty()) {
                logger.debug { "No audio data converted at timestamp $timestamp" }
                return null
            }

            RtspFrame(
                data = dataBytes,
                timestamp = timestamp,
                streamType = RtspStreamType.AUDIO,
                width = 0,
                height = 0
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to convert audio frame at timestamp $timestamp" }
            null
        }
    }

    /**
     * Конвертирует массив Buffer в ByteArray.
     * Поддерживает PCM16 (наиболее распространённый формат из FFmpeg).
     */
    @Suppress("UNCHECKED_CAST")
    private fun convertSamplesToBytes(samples: Array<java.nio.Buffer?>): ByteArray {
        val baos = ByteArrayOutputStream()
        try {
            for (sampleBuffer in samples) {
                if (sampleBuffer == null || !sampleBuffer.hasRemaining()) continue

                // Пытаемся интерпретировать как ShortBuffer
                val shortBuffer = sampleBuffer as? java.nio.ShortBuffer
                if (shortBuffer != null) {
                    // Создаём копию буфера чтобы не портить позицию
                    val buffer = shortBuffer.duplicate()
                    val shortArray = ShortArray(buffer.remaining())
                    buffer.get(shortArray)

                    // Конвертируем short[] → byte[] (little-endian, как в FFmpeg)
                    for (sample in shortArray) {
                        baos.write(sample.toInt() and 0xFF) // Low byte
                        baos.write((sample.toInt() shr 8) and 0xFF) // High byte
                    }
                } else {
                    // Fallback: читаем как ByteBuffer
                    val byteBuffer = sampleBuffer as? java.nio.ByteBuffer
                    if (byteBuffer != null) {
                        val buffer = byteBuffer.duplicate()
                        val byteArray = ByteArray(buffer.remaining())
                        buffer.get(byteArray)
                        baos.write(byteArray)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error converting audio samples to bytes" }
        }
        return baos.toByteArray()
    }

    fun getVideoStreamInfo(grabber: FFmpegFrameGrabber): RtspStreamInfo? {
        return try {
            val codecName = grabber.videoCodecName ?: "H.264"
            val fps = (grabber.frameRate + 0.5).toInt().coerceAtLeast(1)
            RtspStreamInfo(
                index = 0,
                type = RtspStreamType.VIDEO,
                resolution = null,
                fps = fps,
                codec = codecName
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to extract video stream info" }
            null
        }
    }

    fun getAudioStreamInfo(grabber: FFmpegFrameGrabber): RtspStreamInfo? {
        return try {
            val audioChannels = grabber.audioChannels
            if (audioChannels <= 0) return null

            val codecName = grabber.audioCodecName ?: "AAC"
            val sampleRate = grabber.sampleRate

            RtspStreamInfo(
                index = if (grabber.hasVideo()) 1 else 0,
                type = RtspStreamType.AUDIO,
                resolution = null,
                fps = 0,
                codec = codecName,
                audioCodec = codecName,
                sampleRate = sampleRate,
                channels = audioChannels
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to extract audio stream info" }
            null
        }
    }
}
