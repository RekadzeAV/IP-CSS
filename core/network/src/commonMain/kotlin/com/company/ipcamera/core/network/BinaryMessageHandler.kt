package com.company.ipcamera.core.network

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Тип бинарного сообщения
 */
enum class BinaryMessageType {
    IMAGE, // JPEG, PNG, WebP
    VIDEO_CHUNK, // H.264, H.265 фрагменты
    FILE, // Общие файлы
    CUSTOM // Пользовательские форматы
}

/**
 * MIME типы для бинарных сообщений
 */
object BinaryMimeTypes {
    const val JPEG = "image/jpeg"
    const val PNG = "image/png"
    const val WEBP = "image/webp"
    const val H264 = "video/h264"
    const val H265 = "video/h265"
    const val OCTET_STREAM = "application/octet-stream"
}

/**
 * Метаданные для бинарных сообщений
 */
@Serializable
data class BinaryMessageMetadata(
    val type: String, // BinaryMessageType как строка
    val mimeType: String,
    val size: Long,
    val messageId: String,
    val chunkIndex: Int? = null, // Для chunking
    val totalChunks: Int? = null, // Для chunking
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    fun getBinaryMessageType(): BinaryMessageType {
        return when (type.uppercase()) {
            "IMAGE" -> BinaryMessageType.IMAGE
            "VIDEO_CHUNK" -> BinaryMessageType.VIDEO_CHUNK
            "FILE" -> BinaryMessageType.FILE
            "CUSTOM" -> BinaryMessageType.CUSTOM
            else -> BinaryMessageType.CUSTOM
        }
    }
}

/**
 * Magic bytes для определения типа бинарных данных
 */
object BinaryMagicBytes {
    // JPEG: FF D8 FF
    val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    // PNG: 89 50 4E 47 0D 0A 1A 0A
    val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A.toByte(), 0x0A)

    // WebP: RIFF...WEBP
    val WEBP_RIFF = byteArrayOf(0x52, 0x49, 0x46, 0x46) // "RIFF"
    val WEBP_WEBP = byteArrayOf(0x57, 0x45, 0x42, 0x50) // "WEBP"

    // H.264 NAL: 00 00 00 01 или 00 00 01
    val H264_NAL_1 = byteArrayOf(0x00, 0x00, 0x00, 0x01)
    val H264_NAL_2 = byteArrayOf(0x00, 0x00, 0x01)

    // H.265 NAL: 00 00 00 01 или 00 00 01
    val H265_NAL_1 = byteArrayOf(0x00, 0x00, 0x00, 0x01)
    val H265_NAL_2 = byteArrayOf(0x00, 0x00, 0x01)
}

/**
 * Константы для chunking
 */
object ChunkingConstants {
    const val CHUNK_SIZE_THRESHOLD = 64 * 1024 // 64KB
    const val MAX_CHUNK_SIZE = 64 * 1024 // 64KB
    const val MAX_MESSAGE_SIZE = 100 * 1024 * 1024 // 100MB
    const val CHUNK_TIMEOUT_MS = 30_000L // 30 секунд
}

/**
 * Обработчик бинарных сообщений
 */
class BinaryMessageHandler {

    /**
     * Определить тип бинарного сообщения по magic bytes
     */
    fun detectMessageType(data: ByteArray): BinaryMessageType? {
        if (data.size < 3) return null

        return when {
            startsWith(data, BinaryMagicBytes.JPEG) -> BinaryMessageType.IMAGE
            startsWith(data, BinaryMagicBytes.PNG) -> BinaryMessageType.IMAGE
            isWebP(data) -> BinaryMessageType.IMAGE
            isH264(data) -> BinaryMessageType.VIDEO_CHUNK
            isH265(data) -> BinaryMessageType.VIDEO_CHUNK
            else -> BinaryMessageType.FILE
        }
    }

    /**
     * Определить MIME тип по данным
     */
    fun detectMimeType(data: ByteArray): String {
        return when (detectMessageType(data)) {
            BinaryMessageType.IMAGE -> {
                when {
                    startsWith(data, BinaryMagicBytes.JPEG) -> BinaryMimeTypes.JPEG
                    startsWith(data, BinaryMagicBytes.PNG) -> BinaryMimeTypes.PNG
                    isWebP(data) -> BinaryMimeTypes.WEBP
                    else -> BinaryMimeTypes.JPEG
                }
            }
            BinaryMessageType.VIDEO_CHUNK -> {
                when {
                    isH264(data) -> BinaryMimeTypes.H264
                    isH265(data) -> BinaryMimeTypes.H265
                    else -> BinaryMimeTypes.H264
                }
            }
            BinaryMessageType.FILE -> BinaryMimeTypes.OCTET_STREAM
            BinaryMessageType.CUSTOM -> BinaryMimeTypes.OCTET_STREAM
            null -> BinaryMimeTypes.OCTET_STREAM
        }
    }

    /**
     * Валидация бинарных данных
     */
    fun validateBinaryData(data: ByteArray, expectedType: BinaryMessageType? = null): Boolean {
        // Проверка размера
        if (data.isEmpty()) {
            logger.warn { "Binary data is empty" }
            return false
        }

        if (data.size > ChunkingConstants.MAX_MESSAGE_SIZE) {
            logger.warn { "Binary data size (${data.size}) exceeds maximum (${ChunkingConstants.MAX_MESSAGE_SIZE})" }
            return false
        }

        // Проверка типа, если указан
        if (expectedType != null) {
            val detectedType = detectMessageType(data)
            if (detectedType != expectedType && expectedType != BinaryMessageType.CUSTOM) {
                logger.warn { "Binary data type mismatch: expected $expectedType, detected $detectedType" }
                return false
            }
        }

        return true
    }

    /**
     * Проверка, нужно ли разбивать сообщение на chunks
     */
    fun needsChunking(data: ByteArray): Boolean {
        return data.size > ChunkingConstants.CHUNK_SIZE_THRESHOLD
    }

    /**
     * Разбить данные на chunks
     */
    fun splitIntoChunks(data: ByteArray, messageId: String): List<ByteArray> {
        if (!needsChunking(data)) {
            return listOf(data)
        }

        val chunks = mutableListOf<ByteArray>()
        var offset = 0

        while (offset < data.size) {
            val chunkSize = minOf(ChunkingConstants.MAX_CHUNK_SIZE, data.size - offset)
            val chunk = data.copyOfRange(offset, offset + chunkSize)
            chunks.add(chunk)
            offset += chunkSize
        }

        logger.debug { "Split message $messageId into ${chunks.size} chunks" }
        return chunks
    }

    // Вспомогательные функции

    private fun startsWith(data: ByteArray, prefix: ByteArray): Boolean {
        if (data.size < prefix.size) return false
        return prefix.indices.all { data[it] == prefix[it] }
    }

    private fun isWebP(data: ByteArray): Boolean {
        if (data.size < 12) return false
        return startsWith(data, BinaryMagicBytes.WEBP_RIFF) &&
            data.size >= 8 &&
            data[8] == BinaryMagicBytes.WEBP_WEBP[0] &&
            data[9] == BinaryMagicBytes.WEBP_WEBP[1] &&
            data[10] == BinaryMagicBytes.WEBP_WEBP[2] &&
            data[11] == BinaryMagicBytes.WEBP_WEBP[3]
    }

    private fun isH264(data: ByteArray): Boolean {
        if (data.size < 4) return false
        return startsWith(data, BinaryMagicBytes.H264_NAL_1) ||
            startsWith(data, BinaryMagicBytes.H264_NAL_2)
    }

    private fun isH265(data: ByteArray): Boolean {
        if (data.size < 4) return false
        // H.265 имеет похожие NAL единицы, но с другими типами
        // Для упрощения проверяем наличие NAL заголовка
        return startsWith(data, BinaryMagicBytes.H265_NAL_1) ||
            startsWith(data, BinaryMagicBytes.H265_NAL_2)
    }
}
