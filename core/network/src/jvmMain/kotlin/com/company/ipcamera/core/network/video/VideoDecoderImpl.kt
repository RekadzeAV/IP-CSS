package com.company.ipcamera.core.network.video

import com.company.ipcamera.core.network.RtspFrame
import mu.KotlinLogging
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.avcodec.*
import org.bytedeco.ffmpeg.avutil.*
import org.bytedeco.ffmpeg.swscale.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import java.awt.image.BufferedImage

private val logger = KotlinLogging.logger {}

/**
 * Вспомогательный класс для декодирования H.264/H.265 через FFmpeg AVCodecContext
 *
 * Этот класс содержит полную реализацию декодирования через низкоуровневый API FFmpeg.
 * Может быть использован в VideoDecoder.jvm.kt после доработки.
 */
internal class VideoDecoderImpl(
    private val codec: VideoCodec,
    private val width: Int,
    private val height: Int
) {
    private var codecContext: AVCodecContext? = null
    private var swsContext: SwsContext? = null
    private var rgbFrame: AVFrame? = null
    private var rgbBuffer: BytePointer? = null
    private var isInitialized = false

    init {
        try {
            initialize()
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize FFmpeg decoder" }
        }
    }

    private fun initialize() {
        // 1. Найти кодек
        val codecId = when (codec) {
            VideoCodec.H264 -> avcodec.AV_CODEC_ID_H264
            VideoCodec.H265 -> avcodec.AV_CODEC_ID_H265
            else -> {
                logger.error { "Unsupported codec: $codec" }
                return
            }
        }

        val avCodec = avcodec_find_decoder(codecId)
        if (avCodec == null || avCodec.isNull) {
            logger.error { "Codec not found: $codecId" }
            return
        }

        // 2. Создать контекст кодера
        codecContext = avcodec_alloc_context3(avCodec)
        if (codecContext == null || codecContext!!.isNull) {
            logger.error { "Failed to allocate codec context" }
            return
        }

        codecContext!!.width(width)
        codecContext!!.height(height)
        codecContext!!.pix_fmt(avutil.AV_PIX_FMT_YUV420P)

        // 3. Открыть кодек
        val result = avcodec_open2(codecContext, avCodec, null as Pointer?)
        if (result < 0) {
            logger.error { "Failed to open codec: $result" }
            codecContext = null
            return
        }

        // 4. Создать контекст для конвертации YUV в RGB
        swsContext = sws_getContext(
            width, height, avutil.AV_PIX_FMT_YUV420P,
            width, height, avutil.AV_PIX_FMT_RGB24,
            swscale.SWS_BILINEAR, null, null, null
        )

        if (swsContext == null || swsContext!!.isNull) {
            logger.error { "Failed to create sws context" }
            return
        }

        // 5. Выделить память для RGB кадра
        rgbFrame = AVFrame()
        val rgbBufferSize = av_image_get_buffer_size(avutil.AV_PIX_FMT_RGB24, width, height, 1)
        rgbBuffer = BytePointer(avutil.av_malloc(rgbBufferSize.toLong()))

        if (rgbBuffer == null || rgbBuffer!!.isNull) {
            logger.error { "Failed to allocate RGB buffer" }
            return
        }

        av_image_fill_arrays(
            rgbFrame!!.data(),
            rgbFrame!!.linesize(),
            rgbBuffer,
            avutil.AV_PIX_FMT_RGB24,
            width,
            height,
            1
        )

        isInitialized = true
        logger.info { "FFmpeg decoder initialized for $codec" }
    }

    fun decode(frame: RtspFrame): DecodedVideoFrame? {
        if (!isInitialized || codecContext == null) {
            return null
        }

        return try {
            // 1. Создать AVPacket
            val packet = AVPacket()
            avcodec.av_init_packet(packet)
            packet.data(BytePointer(frame.data))
            packet.size(frame.data.size)

            // 2. Отправить пакет в декодер
            val sendResult = avcodec.avcodec_send_packet(codecContext, packet)
            if (sendResult < 0) {
                logger.debug { "Failed to send packet: $sendResult" }
                return null
            }

            // 3. Получить декодированный кадр
            val decodedFrame = AVFrame()
            val receiveResult = avcodec.avcodec_receive_frame(codecContext, decodedFrame)
            if (receiveResult < 0) {
                logger.debug { "Failed to receive frame: $receiveResult" }
                return null
            }

            // 4. Конвертировать YUV в RGB
            sws_scale(
                swsContext,
                decodedFrame.data(),
                decodedFrame.linesize(),
                0,
                height,
                rgbFrame!!.data(),
                rgbFrame!!.linesize()
            )

            // 5. Копировать RGB данные в ByteArray
            val rgbData = ByteArray(width * height * 3)
            rgbBuffer!!.get(rgbData)

            DecodedVideoFrame(
                data = rgbData,
                width = width,
                height = height,
                timestamp = frame.timestamp,
                format = DecodedVideoFrame.PixelFormat.RGB24
            )
        } catch (e: Exception) {
            logger.error(e) { "Error decoding frame" }
            null
        }
    }

    fun release() {
        try {
            swsContext?.let { sws_freeContext(it) }
            rgbBuffer?.let { avutil.av_free(it) }
            codecContext?.let { avcodec.avcodec_free_context(it) }
            isInitialized = false
            logger.info { "FFmpeg decoder released" }
        } catch (e: Exception) {
            logger.error(e) { "Error releasing decoder" }
        }
    }
}

