package com.company.ipcamera.core.network

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Менеджер для сборки chunked сообщений
 */
class ChunkingManager {
    private val chunks = mutableMapOf<String, ChunkedMessage>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Добавить chunk к сообщению
     */
    fun addChunk(
        messageId: String,
        chunkIndex: Int,
        totalChunks: Int,
        data: ByteArray,
        metadata: BinaryMessageMetadata
    ): ByteArray? {
        val chunkedMessage = chunks.getOrPut(messageId) {
            ChunkedMessage(messageId, totalChunks, metadata)
        }

        chunkedMessage.addChunk(chunkIndex, data)

        // Проверяем, все ли chunks получены
        if (chunkedMessage.isComplete()) {
            val completeData = chunkedMessage.assemble()
            chunks.remove(messageId)
            logger.debug { "Assembled message $messageId from $totalChunks chunks" }
            return completeData
        }

        // Запускаем таймаут для неполных сообщений
        if (chunkedMessage.timeoutJob == null) {
            chunkedMessage.timeoutJob = scope.launch {
                delay(ChunkingConstants.CHUNK_TIMEOUT_MS)
                if (chunks.containsKey(messageId) && !chunks[messageId]!!.isComplete()) {
                    logger.warn { "Chunked message $messageId timed out (${chunkedMessage.receivedChunksCount}/$totalChunks chunks received)" }
                    chunks.remove(messageId)
                }
            }
        }

        return null
    }

    /**
     * Очистить старые chunks
     */
    fun cleanup() {
        val now = Clock.System.now().toEpochMilliseconds()
        val expiredIds = chunks
            .filterValues { message -> now - message.timestamp > ChunkingConstants.CHUNK_TIMEOUT_MS }
            .keys
        expiredIds.forEach { chunks.remove(it) }
    }

    /**
     * Отменить таймаут для сообщения
     */
    fun cancelTimeout(messageId: String) {
        chunks[messageId]?.timeoutJob?.cancel()
    }

    /**
     * Очистить все chunks
     */
    fun clear() {
        chunks.values.forEach { it.timeoutJob?.cancel() }
        chunks.clear()
    }
}

/**
 * Chunked сообщение в процессе сборки
 */
private class ChunkedMessage(
    val messageId: String,
    val totalChunks: Int,
    val metadata: BinaryMessageMetadata
) {
    private val receivedChunks = mutableMapOf<Int, ByteArray>()
    val timestamp = Clock.System.now().toEpochMilliseconds()
    var timeoutJob: Job? = null

    fun addChunk(index: Int, data: ByteArray) {
        receivedChunks[index] = data
    }

    fun isComplete(): Boolean {
        return receivedChunks.size == totalChunks
    }

    fun assemble(): ByteArray {
        if (!isComplete()) {
            throw IllegalStateException(
                "Cannot assemble incomplete message: ${receivedChunks.size}/$totalChunks chunks"
            )
        }

        // Собираем chunks в правильном порядке
        val chunks = (0 until totalChunks).map { receivedChunks[it]!! }
        val totalSize = chunks.sumOf { it.size }
        val result = ByteArray(totalSize)

        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }

        return result
    }

    val receivedChunksCount: Int
        get() = receivedChunks.size
}
