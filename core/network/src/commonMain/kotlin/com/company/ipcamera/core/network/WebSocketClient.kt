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
    val maxMessageBufferSize: Int = 100
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
        val filters: Map<String, Any>? = null
    ) : WebSocketMessage()

    @Serializable
    data class UnsubscribeMessage(val channels: List<String>) : WebSocketMessage()

    @Serializable
    data class EventMessage(
        val type: String,
        val channel: String,
        val data: Map<String, Any>
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
}

/**
 * WebSocket клиент с поддержкой переподключения и подписок
 */
class WebSocketClient(
    private val engine: HttpClientEngine,
    private val config: WebSocketClientConfig
) {
    private val client: HttpClient by lazy {
        HttpClient(engine) {
            install(WebSockets) {
                pingInterval = config.pingIntervalMillis
                contentConverter = KotlinxWebsocketSerializationConverter(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
                // Поддержка сжатия если включено
                if (config.enableCompression) {
                    extensions {
                        install(io.ktor.websocket.WebSocketDeflateExtension)
                    }
                }
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

            // Отправка буферизованных сообщений
            if (messageBuffer.isNotEmpty()) {
                val bufferedMessages = messageBuffer.toList()
                messageBuffer.clear()
                for (msg in bufferedMessages) {
                    try {
                        sendMessage(msg)
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

                            val binaryMessage = WebSocketMessage.BinaryMessage(binaryData)
                            eventHandlers.forEach { it.onMessage(binaryMessage) }
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
                "event" -> {
                    val channel = jsonObject["channel"]?.jsonPrimitive?.content ?: ""
                    val data = jsonObject["data"]?.jsonObject?.toMap()
                        ?.mapValues { it.value.toString() } ?: emptyMap()
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
                else -> null
            }
        } catch (e: Exception) {
            logger.error(e) { "Error parsing message: $json" }
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
     * Отправить сообщение
     */
    suspend fun sendMessage(message: WebSocketMessage) {
        val session = this.session ?: run {
            // Буферизация сообщений если не подключены
            if (config.maxMessageBufferSize > 0 && messageBuffer.size < config.maxMessageBufferSize) {
                messageBuffer.add(message)
                logger.debug { "Message buffered (${messageBuffer.size}/${config.maxMessageBufferSize})" }
            } else {
                logger.warn { "Cannot send message: not connected and buffer is full" }
            }
            return
        }

        try {
            val json = when (message) {
                is WebSocketMessage.AuthMessage -> {
                    Json.encodeToString(
                        mapOf(
                            "type" to "auth",
                            "data" to mapOf("token" to message.token)
                        )
                    )
                }
                is WebSocketMessage.SubscribeMessage -> {
                    Json.encodeToString(
                        mapOf(
                            "type" to "subscribe",
                            "data" to mapOf(
                                "channels" to message.channels,
                                "filters" to (message.filters ?: emptyMap())
                            )
                        )
                    )
                }
                is WebSocketMessage.UnsubscribeMessage -> {
                    Json.encodeToString(
                        mapOf(
                            "type" to "unsubscribe",
                            "data" to mapOf("channels" to message.channels)
                        )
                    )
                }
                is WebSocketMessage.BinaryMessage -> {
                    // Отправка бинарных данных напрямую
                    session.send(Frame.Binary(true, message.data))
                    if (config.enableLogging) {
                        logger.debug { "Sent binary message (${message.data.size} bytes)" }
                    }
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
    suspend fun subscribe(channels: List<String>, filters: Map<String, Any>? = null) {
        subscriptions.addAll(channels)

        if (connectionState.value == WebSocketConnectionState.CONNECTED) {
            sendMessage(WebSocketMessage.SubscribeMessage(channels, filters))
        }
    }

    /**
     * Отписаться от каналов
     */
    suspend fun unsubscribe(channels: List<String>) {
        subscriptions.removeAll(channels)

        if (connectionState.value == WebSocketConnectionState.CONNECTED) {
            sendMessage(WebSocketMessage.UnsubscribeMessage(channels))
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
    }
}
