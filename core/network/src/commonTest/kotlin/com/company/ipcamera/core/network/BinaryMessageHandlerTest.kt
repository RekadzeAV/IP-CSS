package com.company.ipcamera.core.network

import kotlin.test.*

/**
 * Тесты для BinaryMessageHandler
 */
class BinaryMessageHandlerTest {

    private val handler = BinaryMessageHandler()

    @Test
    fun testDetectJPEG() {
        // JPEG magic bytes: FF D8 FF
        val jpegData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val type = handler.detectMessageType(jpegData)
        assertEquals(BinaryMessageType.IMAGE, type)
    }

    @Test
    fun testDetectPNG() {
        // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
        val pngData = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A.toByte(), 0x0A,
            0x00, 0x00, 0x00, 0x0D
        )
        val type = handler.detectMessageType(pngData)
        assertEquals(BinaryMessageType.IMAGE, type)
    }

    @Test
    fun testDetectWebP() {
        // WebP: RIFF...WEBP
        val webpData = byteArrayOf(
            0x52, 0x49, 0x46, 0x46, // RIFF
            0x00, 0x00, 0x00, 0x00, // size
            0x57, 0x45, 0x42, 0x50 // WEBP
        )
        val type = handler.detectMessageType(webpData)
        assertEquals(BinaryMessageType.IMAGE, type)
    }

    @Test
    fun testDetectH264() {
        // H.264 NAL: 00 00 00 01
        val h264Data = byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x67.toByte(), 0x64.toByte())
        val type = handler.detectMessageType(h264Data)
        assertEquals(BinaryMessageType.VIDEO_CHUNK, type)
    }

    @Test
    fun testDetectH264ShortNAL() {
        // H.264 NAL: 00 00 01
        val h264Data = byteArrayOf(0x00, 0x00, 0x01, 0x67.toByte(), 0x64.toByte())
        val type = handler.detectMessageType(h264Data)
        assertEquals(BinaryMessageType.VIDEO_CHUNK, type)
    }

    @Test
    fun testDetectFile() {
        // Неизвестный формат
        val fileData = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // ZIP файл
        val type = handler.detectMessageType(fileData)
        assertEquals(BinaryMessageType.FILE, type)
    }

    @Test
    fun testDetectMimeTypeJPEG() {
        val jpegData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        val mimeType = handler.detectMimeType(jpegData)
        assertEquals(BinaryMimeTypes.JPEG, mimeType)
    }

    @Test
    fun testDetectMimeTypePNG() {
        val pngData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A.toByte(), 0x0A)
        val mimeType = handler.detectMimeType(pngData)
        assertEquals(BinaryMimeTypes.PNG, mimeType)
    }

    @Test
    fun testDetectMimeTypeWebP() {
        val webpData = byteArrayOf(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50)
        val mimeType = handler.detectMimeType(webpData)
        assertEquals(BinaryMimeTypes.WEBP, mimeType)
    }

    @Test
    fun testDetectMimeTypeH264() {
        val h264Data = byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x67.toByte())
        val mimeType = handler.detectMimeType(h264Data)
        assertEquals(BinaryMimeTypes.H264, mimeType)
    }

    @Test
    fun testValidateBinaryDataEmpty() {
        val emptyData = byteArrayOf()
        val isValid = handler.validateBinaryData(emptyData)
        assertFalse(isValid, "Empty data should be invalid")
    }

    @Test
    fun testValidateBinaryDataValid() {
        val jpegData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val isValid = handler.validateBinaryData(jpegData, BinaryMessageType.IMAGE)
        assertTrue(isValid, "Valid JPEG data should pass validation")
    }

    @Test
    fun testValidateBinaryDataTooLarge() {
        // Создаем данные больше 100MB (упрощенный тест)
        val largeData = ByteArray(ChunkingConstants.MAX_MESSAGE_SIZE + 1)
        largeData[0] = 0xFF.toByte()
        largeData[1] = 0xD8.toByte()
        largeData[2] = 0xFF.toByte()

        val isValid = handler.validateBinaryData(largeData, BinaryMessageType.IMAGE)
        assertFalse(isValid, "Data larger than MAX_MESSAGE_SIZE should be invalid")
    }

    @Test
    fun testValidateBinaryDataTypeMismatch() {
        val jpegData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        // Ожидаем VIDEO_CHUNK, но это JPEG
        val isValid = handler.validateBinaryData(jpegData, BinaryMessageType.VIDEO_CHUNK)
        assertFalse(isValid, "Type mismatch should be invalid")
    }

    @Test
    fun testNeedsChunkingSmall() {
        val smallData = ByteArray(ChunkingConstants.CHUNK_SIZE_THRESHOLD - 1)
        val needsChunking = handler.needsChunking(smallData)
        assertFalse(needsChunking, "Small data should not need chunking")
    }

    @Test
    fun testNeedsChunkingLarge() {
        val largeData = ByteArray(ChunkingConstants.CHUNK_SIZE_THRESHOLD + 1)
        val needsChunking = handler.needsChunking(largeData)
        assertTrue(needsChunking, "Large data should need chunking")
    }

    @Test
    fun testSplitIntoChunksSmall() {
        val smallData = ByteArray(1000)
        smallData.fill(0x42)
        val chunks = handler.splitIntoChunks(smallData, "test-message-id")

        assertEquals(1, chunks.size, "Small data should not be split")
        assertTrue(chunks[0].contentEquals(smallData), "Single chunk should equal original data")
    }

    @Test
    fun testSplitIntoChunksLarge() {
        val largeData = ByteArray(ChunkingConstants.CHUNK_SIZE_THRESHOLD * 3)
        largeData.fill(0x42)
        val messageId = "test-message-id"
        val chunks = handler.splitIntoChunks(largeData, messageId)

        assertTrue(chunks.size > 1, "Large data should be split into multiple chunks")

        // Проверяем, что все chunks собраны правильно
        val totalSize = chunks.sumOf { it.size }
        assertEquals(largeData.size, totalSize, "Total size of chunks should equal original size")

        // Проверяем размер каждого chunk
        chunks.forEach { chunk ->
            assertTrue(
                chunk.size <= ChunkingConstants.MAX_CHUNK_SIZE,
                "Each chunk should not exceed MAX_CHUNK_SIZE"
            )
        }
    }

    @Test
    fun testSplitIntoChunksReassembly() {
        val originalData = ByteArray(ChunkingConstants.CHUNK_SIZE_THRESHOLD * 2 + 1000)
        for (i in originalData.indices) {
            originalData[i] = (i % 256).toByte()
        }

        val messageId = "test-reassembly"
        val chunks = handler.splitIntoChunks(originalData, messageId)

        // Собираем обратно
        val reassembled = ByteArray(originalData.size)
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(reassembled, offset)
            offset += chunk.size
        }

        assertTrue(
            reassembled.contentEquals(originalData),
            "Reassembled data should equal original data"
        )
    }
}
