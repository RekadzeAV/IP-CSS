package com.company.ipcamera.core.network.native

import kotlinx.cinterop.*
import com.company.ipcamera.core.network.native.videoprocessing.*

/**
 * Kotlin обертка для нативного VideoDecoder
 */
class VideoDecoderWrapper private constructor(
    private val decoder: CPointer<VideoDecoderVar>
) {
    private var callbackRef: StableRef<(DecodedFrame) -> Unit>? = null

    companion object {
        /**
         * Создает новый декодер видео
         * @param codec Тип кодека (H264, H265, MJPEG)
         * @param width Ширина видео
         * @param height Высота видео
         */
        fun create(codec: VideoCodec, width: Int, height: Int): VideoDecoderWrapper? {
            val decoder = video_decoder_create(codec, width, height)
            return decoder?.let { VideoDecoderWrapper(it) }
        }
    }

    /**
     * Декодирует кадр
     * @param data Данные закодированного кадра
     * @param timestamp Временная метка кадра
     * @return true если декодирование успешно
     */
    fun decode(data: ByteArray, timestamp: Long): Boolean {
        return memScoped {
            val dataPtr = allocArray<UByteVar>(data.size)
            data.forEachIndexed { index, byte ->
                dataPtr[index] = byte.toUByte()
            }
            video_decoder_decode(decoder, dataPtr, data.size.toULong(), timestamp)
        }
    }

    /**
     * Устанавливает callback для получения декодированных кадров
     */
    fun setCallback(callback: (DecodedFrame) -> Unit) {
        // Освобождаем предыдущий callback
        callbackRef?.dispose()

        callbackRef = StableRef.create(callback)

        memScoped {
            val callbackWrapper: CFunction<CFunction<(CPointer<DecodedFrameVar>?, COpaquePointer?) -> Unit>> =
                staticCFunction { frame: CPointer<DecodedFrameVar>?, userData: COpaquePointer? ->
                    userData?.asStableRef<(DecodedFrame) -> Unit>()?.get()?.let { callback ->
                        frame?.let { framePtr ->
                            val decodedFrame = DecodedFrame(
                                data = framePtr.pointed.data?.readBytes(framePtr.pointed.dataSize.toInt()) ?: ByteArray(0),
                                width = framePtr.pointed.width,
                                height = framePtr.pointed.height,
                                timestamp = framePtr.pointed.timestamp,
                                format = framePtr.pointed.format,
                                dataSize = framePtr.pointed.dataSize.toInt()
                            )
                            callback(decodedFrame)
                        }
                    }
                }

            video_decoder_set_callback(
                decoder,
                callbackWrapper.ptr,
                callbackRef?.asCPointer()
            )
        }
    }

    /**
     * Получает информацию о декодере
     */
    fun getInfo(): DecoderInfo? {
        return memScoped {
            val width = alloc<IntVar>()
            val height = alloc<IntVar>()
            val codec = alloc<VideoCodecVar>()

            if (video_decoder_get_info(decoder, width.ptr, height.ptr, codec.ptr)) {
                DecoderInfo(
                    width = width.value,
                    height = height.value,
                    codec = codec.value
                )
            } else {
                null
            }
        }
    }

    /**
     * Освобождает ресурсы декодера
     */
    fun release() {
        callbackRef?.dispose()
        callbackRef = null
        video_decoder_destroy(decoder)
    }
}

/**
 * Информация о декодере
 */
data class DecoderInfo(
    val width: Int,
    val height: Int,
    val codec: VideoCodec
)

/**
 * Декодированный кадр
 */
data class DecodedFrame(
    val data: ByteArray,
    val width: Int,
    val height: Int,
    val timestamp: Long,
    val format: Int,
    val dataSize: Int
)

