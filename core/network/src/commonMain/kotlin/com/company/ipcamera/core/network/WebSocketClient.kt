package com.company.ipcamera.core.network

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация WebSocket клиента
 */
data class WebSocketClientConfig(
    val url: String,
    val autoReconnect: Boolean = true,
    val reconnectDelayMillis: Long = 5000,
    val maxReconnectAttempts: Int = Int.MAX_VALUE,
    val pingIntervalMillis: Long = 30000,
    val timeoutMillis: Long = 10000,
    val enableLogging: Boolean = true,
    val enableCompression: Boolean = false,
    val maxMessageBufferSize: Int = 100, // Deprecated: используйте queueMaxSize
    val queueMaxSize: Int = 100,
    val queueOverflowStrategy: QueueOverflowStrategy = QueueOverflowStrategy.DROP_LOWEST,
    val rateLimitConfig: RateLimitConfig = RateLimitConfig()
)

/**
 * Типы WebSocket сообщений
 */
@Serializable
sealed class WebSocketMessage {
    @Serializable
    data class AuthMessage(val token: String) : WebSocketMessage()

    @Serializable
    data class SubscribeMessage(
        val channels: List<String>,
        val filters: JsonObject? = null
    ) : WebSocketMessage()

    @Serializable
    data class UnsubscribeMessage(val channels: List<String>) : WebSocketMessage()

    @Serializable
    data class EventMessage(
        val type: String,
        val channel: String,
        val data: JsonObject
    ) : WebSocketMessage()

    @Serializable
    data class ErrorMessage(val error: String, val code: String? = null) : WebSocketMessage()

    @Serializable
    data class BinaryMessage(val data: ByteArray) : WebSocketMessage() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BinaryMessage) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }

    /**
     * Сообщение с изображением (JPEG, PNG, WebP)
     */
    @Serializable
    data class ImageMessage(
        val data: ByteArray,
        val metadata: BinaryMessageMetadata
    ) : WebSocketMessage() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ImageMessage) return false
            return data.contentEquals(other.data) && metadata == other.metadata
        }

        override fun hashCode(): Int {
            return data.contentHashCode() + metadata.hashCode()
        }
    }

    /**
     * Сообщение с видео фрагментом (H.264, H.265)
     */
    @Serializable
    data class VideoChunkMessage(
        val data: ByteArray,
        val metadata: BinaryMessageMetadata
    ) : WebSocketMessage() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is VideoChunkMessage) return false
            return data.contentEquals(other.data) && metadata == other.metadata
        }

        override fun hashCode(): Int {
            return data.contentHashCode() + metadata.hashCode()
        }
    }

    /**
     * Сообщение с файлом
     */
    @Serializable
    data class FileMessage(
        val data: ByteArray,
        val metadata: BinaryMessageMetadata,
        val fileName: String
    ) : WebSocketMessage() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FileMessage) return false
            return data.contentEquals(other.data) &&
                metadata == other.metadata &&
                fileName == other.fileName
        }

        override fun hashCode(): Int {
            return data.contentHashCode() + metadata.hashCode() + fileName.hashCode()
        }
    }

    /**
     * Пользовательское бинарное сообщение
     */
    @Serializable
    data class CustomBinaryMessage(
        val data: ByteArray,
        val metadata: BinaryMessageMetadata,
        val format: String
    ) : WebSocketMessage() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CustomBinaryMessage) return false
            return data.contentEquals(other.data) &&
                metadata == other.metadata &&
                format == other.format
        }

        override fun hashCode(): Int {
            return data.contentHashCode() + metadata.hashCode() + format.hashCode()
        }
    }
}

/**
 * Состояние подключения WebSocket
 */
enum class WebSocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}

/**
 * Обработчик событий WebSocket
 */
interface WebSocketEventHandler {
    fun onConnected() {}
    fun onDisconnected(cause: Throwable?) {}
    fun onMessage(message: WebSocketMessage) {}
    fun onError(error: Throwable) {}

    /**
     * Вызывается при превышении rate limit
     */
    fun onRateLimitExceeded(operationType: String, limitType: String) {}

    /**
     * Вызывается при переполнении очереди
     */
    fun onQueueOverflow(strategy: QueueOverflowStrategy, droppedCount: Long) {}
}

/**
 * WebSocket клиент с поддержкой переподключения и подписок
 */
class WebSocketClient(
    private val engine: HttpClientEngine,
    private val config: WebSocketClientConfig
) {
    private val binaryHandler = BinaryMessageHandler()
    private val chunkingManager = ChunkingManager()
    private val messageQueue = MessageQueue(
        maxSize = config.queueMaxSize,
        overflowStrategy = config.queueOverflowStrategy
    )
    private val rateLimiter = RateLimiter(config.rateLimitConfig)

    private val client: HttpClient by lazy {
        HttpClient(engine) {
            install(WebSockets) {
                pingInterval = config.pingIntervalMillis
            }
        }
    }

    private var session: DefaultWebSocketSession? = null
    private var reconnectJob: Job? = null
    private val connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    private val eventHandlers = mutableListOf<WebSocketEventHandler>()
    private val subscriptions = mutableSetOf<String>()
    private var isManualClose = false
    private var authToken: String? = null
    private var reconnectAttempts = 0

    @Deprecated("Используйте messageQueue", ReplaceWith("messageQueue"))
    private val messageBuffer = mutableListOf<WebSocketMessage>()

    /**
     * Получить состояние подключения
     */
    fun getConnectionState(): StateFlow<WebSocketConnectionState> = connectionState.asStateFlow()

    /**
     * Подключиться к WebSocket серверу
     */
    suspend fun connect(token: String? = null) {
        if (connectionState.value == WebSocketConnectionState.CONNECTED) {
            logger.warn { "WebSocket already connected" }
            return
        }

        authToken = token
        isManualClose = false
        reconnectAttempts = 0
        connectInternal()
    }

    private suspend fun connectInternal() {
        try {
            connectionState.value = WebSocketConnectionState.CONNECTING
            logger.info { "Connecting to WebSocket: ${config.url}" }

            session = client.webSocketSession(config.url)
            connectionState.value = WebSocketConnectionState.CONNECTED
            reconnectAttempts = 0

            logger.info { "WebSocket connected successfully" }
            eventHandlers.forEach { it.onConnected() }

            // Отправка токена аутентификации, если он предоставлен
            authToken?.let { token ->
                sendMessage(WebSocketMessage.AuthMessage(token))
            }

            // Восстановление подписок
            if (subscriptions.isNotEmpty()) {
                sendMessage(
                    WebSocketMessage.SubscribeMessage(
                        channels = subscriptions.toList()
                    )
                )
            }

            // Отправка сообщений из очереди
            while (!messageQueue.isEmpty()) {
                val queuedMessage = messageQueue.dequeue()
                if (queuedMessage != null) {
                    try {
                        // Определяем приоритет сообщения
                        val priority = when (queuedMessage) {
                            is WebSocketMessage.AuthMessage -> MessagePriority.CRITICAL
                            is WebSocketMessage.ErrorMessage -> MessagePriority.CRITICAL
                            is WebSocketMessage.SubscribeMessage -> MessagePriority.HIGH
                            is WebSocketMessage.UnsubscribeMessage -> MessagePriority.HIGH
                            is WebSocketMessage.EventMessage -> MessagePriority.HIGH
                            else -> MessagePriority.NORMAL
                        }
                        sendMessage(queuedMessage, priority)
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to send queued message" }
                    }
                }
            }

            // Обработка старого messageBuffer для обратной совместимости
            if (messageBuffer.isNotEmpty()) {
                val bufferedMessages = messageBuffer.toList()
                messageBuffer.clear()
                for (msg in bufferedMessages) {
                    try {
                        val priority = when (msg) {
                            is WebSocketMessage.AuthMessage -> MessagePriority.CRITICAL
                            is WebSocketMessage.ErrorMessage -> MessagePriority.CRITICAL
                            else -> MessagePriority.NORMAL
                        }
                        sendMessage(msg, priority)
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to send buffered message" }
                    }
                }
            }

            // Обработка входящих сообщений
            handleIncomingMessages()
        } catch (e: Exception) {
            logger.error(e) { "Failed to connect to WebSocket" }
            connectionState.value = WebSocketConnectionState.FAILED
            eventHandlers.forEach { it.onDisconnected(e) }
            eventHandlers.forEach { it.onError(e) }

            if (config.autoReconnect && !isManualClose) {
                scheduleReconnect()
            }
        }
    }

    /**
     * Обработка входящих сообщений
     */
    private suspend fun handleIncomingMessages() {
        val session = this.session ?: return

        try {
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        try {
                            val text = frame.readText()
                            if (config.enableLogging) {
                                logger.debug { "Received WebSocket message: $text" }
                            }

                            // Парсинг сообщения по типу
                            val parsedMessage = parseMessage(text)
                            parsedMessage?.let { msg ->
                                eventHandlers.forEach { it.onMessage(msg) }
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "Error parsing WebSocket message" }
                            eventHandlers.forEach { it.onError(e) }
                        }
                    }
                    is Frame.Binary -> {
                        try {
                            val binaryData = frame.readBytes()
                            if (config.enableLogging) {
                                logger.debug { "Received binary frame (${binaryData.size} bytes)" }
                            }

                            // Проверяем, является ли это chunked сообщением
                            // Формат: [4 байта - размер JSON метаданных][JSON метаданные][данные chunk]
                            if (binaryData.size >= 4) {
                                // Читаем размер метаданных (big-endian)
                                val metadataSize = ((binaryData[0].toInt() and 0xFF) shl 24) or
                                    ((binaryData[1].toInt() and 0xFF) shl 16) or
                                    ((binaryData[2].toInt() and 0xFF) shl 8) or
                                    (binaryData[3].toInt() and 0xFF)

                                // Если есть метаданные и они помещаются в сообщение
                                if (metadataSize > 0 && metadataSize < binaryData.size - 4) {
                                    try {
                                        // Читаем JSON метаданные
                                        val metadataJson = binaryData.decodeToString(4, 4 + metadataSize)

                                        val metadata = Json.decodeFromString(
                                            BinaryMessageMetadata.serializer(),
                                            metadataJson
                                        )

                                        // Если это chunked сообщение
                                        if (metadata.chunkIndex != null && metadata.totalChunks != null) {
                                            // Читаем данные chunk
                                            val chunkData = binaryData.copyOfRange(
                                                4 + metadataSize,
                                                binaryData.size
                                            )

                                            // Добавляем chunk к менеджеру
                                            val completeData = chunkingManager.addChunk(
                                                metadata.messageId,
                                                metadata.chunkIndex!!,
                                                metadata.totalChunks!!,
                                                chunkData,
                                                metadata
                                            )

                                            if (completeData != null) {
                                                // Все chunks получены, обрабатываем полное сообщение
                                                if (config.enableLogging) {
                                                    logger.debug { "Assembled chunked message ${metadata.messageId} from ${metadata.totalChunks} chunks" }
                                                }
                                                processCompleteBinaryMessage(completeData, metadata)
                                            } else {
                                                // Chunks еще собираются
                                                if (config.enableLogging) {
                                                    logger.debug { "Received chunk ${metadata.chunkIndex!! + 1}/${metadata.totalChunks} of message ${metadata.messageId}" }
                                                }
                                            }
                                            continue
                                        }
                                    } catch (e: Exception) {
                                        logger.warn(e) { "Failed to parse chunk metadata, treating as regular binary message" }
                                        // Продолжаем обработку как обычное сообщение
                                    }
                                }
                            }

                            // Обычное (не chunked) бинарное сообщение
                            processCompleteBinaryMessage(binaryData, null)
                        } catch (e: Exception) {
                            logger.error(e) { "Error processing binary frame" }
                            eventHandlers.forEach { it.onError(e) }
                        }
                    }
                    is Frame.Close -> {
                        logger.info { "Received close frame" }
                        break
                    }
                    is Frame.Ping, is Frame.Pong -> {
                        // Обрабатывается автоматически Ktor
                    }
                    else -> Unit
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error handling incoming messages" }
            if (!isManualClose) {
                scheduleReconnect()
            }
            eventHandlers.forEach { it.onDisconnected(e) }
        } finally {
            session.close()
            this.session = null
            connectionState.value = WebSocketConnectionState.DISCONNECTED
        }
    }

    /**
     * Обработка полного бинарного сообщения
     */
    private fun processCompleteBinaryMessage(binaryData: ByteArray, existingMetadata: BinaryMessageMetadata?) {
        // Определяем тип бинарного сообщения
        val messageType = binaryHandler.detectMessageType(binaryData)
        val mimeType = binaryHandler.detectMimeType(binaryData)

        // Валидация данных
        if (!binaryHandler.validateBinaryData(binaryData, messageType)) {
            logger.warn { "Binary data validation failed" }
            eventHandlers.forEach {
                it.onError(Exception("Binary data validation failed"))
            }
            return
        }

        // Создаем метаданные
        val messageId = existingMetadata?.messageId ?: newRandomId()
        val metadata = existingMetadata ?: BinaryMessageMetadata(
            type = messageType?.name ?: "CUSTOM",
            mimeType = mimeType,
            size = binaryData.size.toLong(),
            messageId = messageId
        )

        // Создаем соответствующее сообщение
        val binaryMessage: WebSocketMessage = when (messageType) {
            BinaryMessageType.IMAGE -> {
                WebSocketMessage.ImageMessage(binaryData, metadata)
            }
            BinaryMessageType.VIDEO_CHUNK -> {
                WebSocketMessage.VideoChunkMessage(binaryData, metadata)
            }
            BinaryMessageType.FILE -> {
                WebSocketMessage.FileMessage(
                    binaryData,
                    metadata,
                    fileName = "file_$messageId"
                )
            }
            BinaryMessageType.CUSTOM, null -> {
                WebSocketMessage.CustomBinaryMessage(
                    binaryData,
                    metadata,
                    format = mimeType
                )
            }
        }

        eventHandlers.forEach { it.onMessage(binaryMessage) }
    }

    /**
     * Парсинг сообщения по типу
     */
    private fun parseMessage(json: String): WebSocketMessage? {
        return try {
            val jsonObject = Json.parseToJsonElement(json).jsonObject
            val type = jsonObject["type"]?.jsonPrimitive?.content

            when (type) {
                "auth" -> {
                    val token = jsonObject["data"]?.jsonObject?.get("token")?.jsonPrimitive?.content
                    token?.let { WebSocketMessage.AuthMessage(it) }
                }
                "subscribe" -> {
                    val channels = jsonObject["data"]?.jsonObject?.get("channels")
                        ?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                    val filters = jsonObject["data"]?.jsonObject?.get("filters")?.jsonObject
                    WebSocketMessage.SubscribeMessage(channels, filters)
                }
                "unsubscribe" -> {
                    val channels = jsonObject["data"]?.jsonObject?.get("channels")
                        ?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                    WebSocketMessage.UnsubscribeMessage(channels)
                }
                "event" -> {
                    val channel = jsonObject["channel"]?.jsonPrimitive?.content ?: ""
                    val data = jsonObject["data"]?.jsonObject ?: buildJsonObject { }
                    WebSocketMessage.EventMessage(
                        type = type,
                        channel = channel,
                        data = data
                    )
                }
                "error" -> {
                    val error = jsonObject["error"]?.jsonPrimitive?.content ?: "Unknown error"
                    val code = jsonObject["code"]?.jsonPrimitive?.content
                    WebSocketMessage.ErrorMessage(error, code)
                }
                else -> {
                    // Логируем неизвестные типы для отладки, но не выбрасываем ошибку
                    if (config.enableLogging) {
                        logger.debug { "Unknown message type: $type, full message: $json" }
                    }
                    // Пытаемся распарсить как общее сообщение, если есть базовые поля
                    val error = jsonObject["error"]?.jsonPrimitive?.content
                    if (error != null) {
                        val code = jsonObject["code"]?.jsonPrimitive?.content
                        WebSocketMessage.ErrorMessage(error, code)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error parsing message: ${e.message ?: json}" }
            null
        }
    }

    /**
     * Отправить бинарное сообщение
     */
    suspend fun sendBinary(data: ByteArray) {
        sendMessage(WebSocketMessage.BinaryMessage(data))
    }

    /**
     * Отправить бинарные данные с поддержкой chunking
     */
    private suspend fun sendBinaryData(
        session: DefaultWebSocketSession,
        data: ByteArray,
        metadata: BinaryMessageMetadata? = null
    ) {
        // Проверяем, нужно ли разбивать на chunks
        if (binaryHandler.needsChunking(data)) {
            val messageId = metadata?.messageId ?: newRandomId()
            val chunks = binaryHandler.splitIntoChunks(data, messageId)
            val totalChunks = chunks.size

            // Отправляем каждый chunk
            chunks.forEachIndexed { index, chunk ->
                val chunkMetadata = metadata?.copy(
                    messageId = messageId,
                    chunkIndex = index,
                    totalChunks = totalChunks,
                    size = data.size.toLong()
                ) ?: BinaryMessageMetadata(
                    type = binaryHandler.detectMessageType(data)?.name ?: "CUSTOM",
                    mimeType = binaryHandler.detectMimeType(data),
                    size = data.size.toLong(),
                    messageId = messageId,
                    chunkIndex = index,
                    totalChunks = totalChunks
                )

                // Отправляем chunk с метаданными в заголовке
                // Формат: [4 байта - размер JSON][JSON метаданные][данные chunk]
                val metadataJson = Json.encodeToString(BinaryMessageMetadata.serializer(), chunkMetadata)
                val metadataBytes = metadataJson.encodeToByteArray()
                val metadataSize = metadataBytes.size

                // Создаем бинарное сообщение: размер метаданных (4 байта) + метаданные + данные
                val chunkWithMetadata = ByteArray(4 + metadataSize + chunk.size)
                // Записываем размер метаданных (big-endian)
                chunkWithMetadata[0] = ((metadataSize shr 24) and 0xFF).toByte()
                chunkWithMetadata[1] = ((metadataSize shr 16) and 0xFF).toByte()
                chunkWithMetadata[2] = ((metadataSize shr 8) and 0xFF).toByte()
                chunkWithMetadata[3] = (metadataSize and 0xFF).toByte()
                // Копируем метаданные
                metadataBytes.copyInto(chunkWithMetadata, 4)
                // Копируем данные chunk
                chunk.copyInto(chunkWithMetadata, 4 + metadataSize)

                session.send(Frame.Binary(index == chunks.size - 1, chunkWithMetadata))

                if (config.enableLogging) {
                    logger.debug { "Sent chunk ${index + 1}/$totalChunks of message $messageId (${chunk.size} bytes)" }
                }
            }

            if (config.enableLogging) {
                logger.debug { "Sent chunked binary message $messageId (${data.size} bytes in $totalChunks chunks)" }
            }
        } else {
            // Отправляем как обычное сообщение
            session.send(Frame.Binary(true, data))
            if (config.enableLogging) {
                logger.debug { "Sent binary message (${data.size} bytes)" }
            }
        }
    }

    /**
     * Отправить сообщение
     */
    suspend fun sendMessage(message: WebSocketMessage, priority: MessagePriority = MessagePriority.NORMAL) {
        val session = this.session ?: run {
            // Добавляем в очередь если не подключены
            val enqueued = messageQueue.enqueue(message, priority)
            if (enqueued) {
                logger.debug { "Message queued (priority: $priority, queue size: ${messageQueue.size()})" }
            } else {
                logger.warn { "Cannot queue message: queue is full" }
            }

            // Обратная совместимость со старым messageBuffer
            if (config.maxMessageBufferSize > 0 && messageBuffer.size < config.maxMessageBufferSize) {
                messageBuffer.add(message)
            }
            return
        }

        try {
            // Проверка rate limit для сообщений (кроме CRITICAL приоритета)
            if (priority != MessagePriority.CRITICAL) {
                if (!rateLimiter.checkMessageLimit()) {
                    // Если лимит превышен, добавляем в очередь
                    val enqueued = messageQueue.enqueue(message, priority)
                    if (enqueued) {
                        logger.debug { "Message rate limited, queued (priority: $priority)" }
                    } else {
                        logger.warn { "Message rate limited and queue is full, message dropped" }
                        eventHandlers.forEach {
                            it.onError(Exception("Message rate limit exceeded and queue is full"))
                        }
                    }
                    return
                }
            }

            // Проверка лимита на размер (для бинарных сообщений)
            val messageSize = when (message) {
                is WebSocketMessage.BinaryMessage -> message.data.size.toLong()
                is WebSocketMessage.ImageMessage -> message.data.size.toLong()
                is WebSocketMessage.VideoChunkMessage -> message.data.size.toLong()
                is WebSocketMessage.FileMessage -> message.data.size.toLong()
                is WebSocketMessage.CustomBinaryMessage -> message.data.size.toLong()
                else -> 0L
            }

            if (messageSize > 0 && !rateLimiter.checkBytesLimit(messageSize)) {
                // Если лимит размера превышен, добавляем в очередь
                val enqueued = messageQueue.enqueue(message, priority)
                if (enqueued) {
                    logger.debug { "Message bytes rate limited, queued (priority: $priority, size: $messageSize)" }
                    eventHandlers.forEach {
                        it.onRateLimitExceeded("bytes", "bytes_per_second")
                    }
                } else {
                    logger.warn { "Message bytes rate limited and queue is full, message dropped" }
                    val metrics = messageQueue.getMetrics()
                    eventHandlers.forEach {
                        it.onError(Exception("Message bytes rate limit exceeded and queue is full"))
                        it.onQueueOverflow(config.queueOverflowStrategy, metrics.droppedMessages)
                    }
                }
                return
            }
            // Оптимизация: минимизация размера JSON сообщений
            val json = when (message) {
                is WebSocketMessage.AuthMessage -> {
                    // Компактный формат для auth
                    val obj = buildJsonObject {
                        put("t", "auth")
                        put("d", buildJsonObject { put("token", message.token) })
                    }
                    Json.encodeToString(JsonObject.serializer(), obj)
                }
                is WebSocketMessage.SubscribeMessage -> {
                    // Компактный формат для subscribe
                    val data = buildJsonObject {
                        put("c", JsonArray(message.channels.map { JsonPrimitive(it) }))
                        message.filters?.let { put("f", it) }
                    }
                    val obj = buildJsonObject {
                        put("t", "sub")
                        put("d", data)
                    }
                    Json.encodeToString(JsonObject.serializer(), obj)
                }
                is WebSocketMessage.UnsubscribeMessage -> {
                    // Компактный формат для unsubscribe
                    val obj = buildJsonObject {
                        put("t", "unsub")
                        put("d", buildJsonObject { put("c", JsonArray(message.channels.map { JsonPrimitive(it) })) })
                    }
                    Json.encodeToString(JsonObject.serializer(), obj)
                }
                is WebSocketMessage.BinaryMessage -> {
                    // Отправка бинарных данных с поддержкой chunking
                    sendBinaryData(session, message.data)
                    return
                }
                is WebSocketMessage.ImageMessage -> {
                    // Отправка изображения с поддержкой chunking
                    sendBinaryData(session, message.data, message.metadata)
                    return
                }
                is WebSocketMessage.VideoChunkMessage -> {
                    // Отправка видео фрагмента с поддержкой chunking
                    sendBinaryData(session, message.data, message.metadata)
                    return
                }
                is WebSocketMessage.FileMessage -> {
                    // Отправка файла с поддержкой chunking
                    sendBinaryData(session, message.data, message.metadata)
                    return
                }
                is WebSocketMessage.CustomBinaryMessage -> {
                    // Отправка пользовательского бинарного сообщения с поддержкой chunking
                    sendBinaryData(session, message.data, message.metadata)
                    return
                }
                else -> {
                    logger.warn { "Unsupported message type: ${message::class.simpleName}" }
                    return
                }
            }

            session.send(Frame.Text(json))

            if (config.enableLogging) {
                logger.debug { "Sent WebSocket message: $json" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error sending message" }
            eventHandlers.forEach { it.onError(e) }
            throw e
        }
    }

    /**
     * Подписаться на каналы событий
     */
    suspend fun subscribe(channels: List<String>, filters: JsonObject? = null) {
        // Проверка rate limit для подписок
        if (!rateLimiter.checkSubscriptionLimit()) {
            logger.warn { "Subscription rate limit exceeded, subscription queued" }
            // Добавляем в очередь с высоким приоритетом
            messageQueue.enqueue(
                WebSocketMessage.SubscribeMessage(channels, filters),
                MessagePriority.HIGH
            )
            return
        }

        subscriptions.addAll(channels)

        if (connectionState.value == WebSocketConnectionState.CONNECTED) {
            sendMessage(WebSocketMessage.SubscribeMessage(channels, filters), MessagePriority.HIGH)
        }
    }

    /**
     * Отписаться от каналов
     */
    suspend fun unsubscribe(channels: List<String>) {
        // Проверка rate limit для отписок
        if (!rateLimiter.checkSubscriptionLimit()) {
            logger.warn { "Unsubscription rate limit exceeded, unsubscription queued" }
            // Добавляем в очередь с высоким приоритетом
            messageQueue.enqueue(
                WebSocketMessage.UnsubscribeMessage(channels),
                MessagePriority.HIGH
            )
            return
        }

        subscriptions.removeAll(channels)

        if (connectionState.value == WebSocketConnectionState.CONNECTED) {
            sendMessage(WebSocketMessage.UnsubscribeMessage(channels), MessagePriority.HIGH)
        }
    }

    /**
     * Запланировать переподключение
     */
    private fun scheduleReconnect() {
        if (!config.autoReconnect || reconnectAttempts >= config.maxReconnectAttempts) {
            logger.warn { "Max reconnect attempts reached or auto-reconnect disabled" }
            connectionState.value = WebSocketConnectionState.FAILED
            return
        }

        reconnectJob?.cancel()
        reconnectJob = CoroutineScope(Dispatchers.Default).launch {
            reconnectAttempts++
            connectionState.value = WebSocketConnectionState.RECONNECTING

            val delay = config.reconnectDelayMillis * reconnectAttempts
            logger.info { "Scheduling reconnect in ${delay}ms (attempt $reconnectAttempts)" }

            delay(delay)

            if (!isManualClose) {
                connectInternal()
            }
        }
    }

    /**
     * Добавить обработчик событий
     */
    fun addEventHandler(handler: WebSocketEventHandler) {
        eventHandlers.add(handler)
    }

    /**
     * Удалить обработчик событий
     */
    fun removeEventHandler(handler: WebSocketEventHandler) {
        eventHandlers.remove(handler)
    }

    /**
     * Отключиться от WebSocket сервера
     */
    suspend fun disconnect() {
        isManualClose = true
        reconnectJob?.cancel()
        reconnectJob = null

        session?.close()
        session = null
        connectionState.value = WebSocketConnectionState.DISCONNECTED

        logger.info { "WebSocket disconnected" }
    }

    /**
     * Закрыть клиент и освободить ресурсы
     */
    fun close() {
        isManualClose = true
        reconnectJob?.cancel()
        reconnectJob = null
        session = null
        client.close()
        connectionState.value = WebSocketConnectionState.DISCONNECTED
        chunkingManager.clear()
        messageQueue.clear()
        rateLimiter.reset()
    }

    /**
     * Получить метрики rate limiting
     */
    fun getRateLimitMetrics(): RateLimitMetrics {
        return rateLimiter.getMetrics()
    }

    /**
     * Получить метрики очереди сообщений
     */
    fun getQueueMetrics(): QueueMetrics {
        return messageQueue.getMetrics()
    }

    private fun newRandomId(): String {
        val chars = "0123456789abcdef"
        return buildString(32) {
            repeat(32) { append(chars[Random.nextInt(chars.length)]) }
        }
    }
}
