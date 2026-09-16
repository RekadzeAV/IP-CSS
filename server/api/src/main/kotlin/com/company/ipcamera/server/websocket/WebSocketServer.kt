package com.company.ipcamera.server.websocket

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.repository.ServerUserRepository
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import mu.KotlinLogging
import org.koin.ktor.ext.inject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Типы WebSocket сообщений для сервера
 */
@Serializable
sealed class ServerWebSocketMessage {
    @Serializable
    data class AuthResponse(val success: Boolean, val message: String? = null) : ServerWebSocketMessage()

    @Serializable
    data class SubscribeResponse(val success: Boolean, val channels: List<String>, val message: String? = null) : ServerWebSocketMessage()

    @Serializable
    data class UnsubscribeResponse(val success: Boolean, val channels: List<String>, val message: String? = null) : ServerWebSocketMessage()

    @Serializable
    data class EventMessage(
        val type: String,
        val channel: String,
        val data: JsonObject
    ) : ServerWebSocketMessage()

    @Serializable
    data class ErrorMessage(val error: String, val code: String? = null) : ServerWebSocketMessage()
}

/**
 * Каналы WebSocket для подписки
 */
enum class WebSocketChannel {
    CAMERAS,      // Обновления камер
    EVENTS,       // События системы
    RECORDINGS,   // Обновления записей
    NOTIFICATIONS, // Уведомления
    ANALYTICS     // События аналитики в реальном времени
}

/**
 * Менеджер WebSocket сессий
 */
class WebSocketSessionManager {
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val sessionSubscriptions = ConcurrentHashMap<String, MutableSet<WebSocketChannel>>()
    private val channelSubscriptions = ConcurrentHashMap<WebSocketChannel, MutableSet<String>>()

    /**
     * Добавить сессию
     */
    fun addSession(sessionId: String, session: WebSocketSession) {
        sessions[sessionId] = session
        sessionSubscriptions[sessionId] = mutableSetOf()
        logger.info { "WebSocket session added: $sessionId" }
    }

    /**
     * Удалить сессию
     */
    fun removeSession(sessionId: String) {
        val subscriptions = sessionSubscriptions.remove(sessionId)
        subscriptions?.forEach { channel ->
            channelSubscriptions[channel]?.remove(sessionId)
        }
        sessions.remove(sessionId)
        logger.info { "WebSocket session removed: $sessionId" }
    }

    /**
     * Подписать сессию на канал
     */
    fun subscribe(sessionId: String, channel: WebSocketChannel): Boolean {
        if (!sessions.containsKey(sessionId)) {
            return false
        }

        sessionSubscriptions.getOrPut(sessionId) { mutableSetOf() }.add(channel)
        channelSubscriptions.getOrPut(channel) { mutableSetOf() }.add(sessionId)
        logger.debug { "Session $sessionId subscribed to channel: $channel" }
        return true
    }

    /**
     * Отписать сессию от канала
     */
    fun unsubscribe(sessionId: String, channel: WebSocketChannel): Boolean {
        sessionSubscriptions[sessionId]?.remove(channel)
        channelSubscriptions[channel]?.remove(sessionId)
        logger.debug { "Session $sessionId unsubscribed from channel: $channel" }
        return true
    }

    /**
     * Отправить сообщение всем подписчикам канала
     */
    suspend fun broadcastToChannel(channel: WebSocketChannel, message: String) {
        val subscribers = channelSubscriptions[channel] ?: return

        subscribers.forEach { sessionId ->
            val session = sessions[sessionId]
            if (session != null) {
                try {
                    session.send(Frame.Text(message))
                } catch (e: Exception) {
                    logger.error(e) { "Error sending message to session $sessionId" }
                    removeSession(sessionId)
                }
            } else {
                removeSession(sessionId)
            }
        }
    }

    /**
     * Отправить сообщение конкретной сессии
     */
    suspend fun sendToSession(sessionId: String, message: String): Boolean {
        val session = sessions[sessionId] ?: return false
        return try {
            session.send(Frame.Text(message))
            true
        } catch (e: Exception) {
            logger.error(e) { "Error sending message to session $sessionId" }
            removeSession(sessionId)
            false
        }
    }

    /**
     * Получить количество активных сессий
     */
    fun getActiveSessionsCount(): Int = sessions.size

    /**
     * Количество подписчиков на конкретный канал.
     * Используется в тестах и диагностике.
     */
    fun getSubscribersCount(channel: WebSocketChannel): Int {
        return channelSubscriptions[channel]?.size ?: 0
    }

    /**
     * Полный сброс in-memory состояния менеджера сессий.
     * Нужен для изолированных unit/integration тестов.
     */
    fun clearAll() {
        sessions.clear()
        sessionSubscriptions.clear()
        channelSubscriptions.clear()
    }
}

/**
 * Глобальный менеджер WebSocket сессий
 */
object WebSocketManager {
    val sessionManager = WebSocketSessionManager()

    /**
     * Отправить событие в канал
     */
    suspend fun broadcastEvent(channel: WebSocketChannel, type: String, data: JsonObject) {
        val msg = ServerWebSocketMessage.EventMessage(
            type = type,
            channel = channel.name.lowercase(),
            data = data
        )
        val message = Json.encodeToString(ServerWebSocketMessage.EventMessage.serializer(), msg)
        sessionManager.broadcastToChannel(channel, message)
    }
}

private fun responseWithData(type: String, data: JsonObject): String {
    return buildJsonObject {
        put("type", type)
        put("data", data)
    }.toString()
}

private fun errorResponse(error: String, code: String? = null): String {
    return buildJsonObject {
        put("type", "error")
        put("error", error)
        if (code != null) {
            put("code", code)
        }
    }.toString()
}

/**
 * Настройка WebSocket маршрутов
 */
fun Application.configureWebSocket() {
    val userRepository: ServerUserRepository by inject()

    routing {
        route("/api/v1") {
            webSocket("/ws") {
                val sessionId = UUID.randomUUID().toString()
                val connection = WebSocketConnection(sessionId, session = this)
                
                try {
                    WebSocketManager.sessionManager.addSession(sessionId, this)
                    logger.info { "WebSocket connection established: $sessionId" }

                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> handleTextFrame(frame, connection, userRepository)
                            is Frame.Close -> {
                                logger.info { "WebSocket close frame received: $sessionId" }
                                break
                            }
                            is Frame.Ping, is Frame.Pong -> {} // Обрабатывается автоматически
                            else -> logger.warn { "Unsupported frame type: ${frame::class.simpleName}" }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "WebSocket error: $sessionId" }
                } finally {
                    WebSocketManager.sessionManager.removeSession(sessionId)
                    logger.info { "WebSocket connection closed: $sessionId" }
                }
            }
        }
    }
}

private data class WebSocketConnection(
    val sessionId: String,
    var authenticated: Boolean = false,
    var userId: String? = null,
    var session: WebSocketSession? = null
)

private suspend fun handleTextFrame(frame: Frame.Text, connection: WebSocketConnection, userRepository: ServerUserRepository) {
    try {
        val json = Json.parseToJsonElement(frame.readText()).jsonObject
        val type = json["type"]?.jsonPrimitive?.content
        
        when (type) {
            "auth" -> handleAuthMessage(json, connection, userRepository)
            "subscribe" -> handleSubscribeMessage(json, connection)
            "unsubscribe" -> handleUnsubscribeMessage(json, connection)
            else -> handleUnknownMessageType(connection, type)
        }
    } catch (e: Exception) {
        logger.error(e) { "Error processing WebSocket message" }
        connection.session?.send(Frame.Text(errorResponse("Error processing message: ${e.message}", "PROCESSING_ERROR")))
    }
}

private suspend fun handleAuthMessage(json: JsonObject, connection: WebSocketConnection, userRepository: ServerUserRepository) {
    val token = json["data"]?.jsonObject?.get("token")?.jsonPrimitive?.content
    if (token == null) return
    
    try {
        val verifier = JwtConfig.createVerifier()
        val decodedJWT = verifier.verify(token)
        val userId = decodedJWT.subject
        
        val user = userRepository.getUserById(userId)
        if (user != null && user.isActive) {
            connection.authenticated = true
            connection.userId = userId
            connection.session?.send(Frame.Text(responseWithData(
                type = "auth_response",
                data = buildJsonObject {
                    put("success", true)
                    put("message", "Authentication successful")
                }
            )))
            logger.info { "WebSocket authenticated: ${connection.sessionId} (user: $userId)" }
        } else {
            connection.session?.send(Frame.Text(responseWithData(
                type = "auth_response",
                data = buildJsonObject { put("success", false); put("message", "User not found or inactive") }
            )))
        }
    } catch (e: Exception) {
        logger.warn { "WebSocket authentication failed: ${e.message}" }
            connection.session?.send(Frame.Text(responseWithData(
                type = "auth_response",
                data = buildJsonObject { put("success", false); put("message", "Invalid token") }
            )))
    }
}

private suspend fun handleSubscribeMessage(json: JsonObject, connection: WebSocketConnection) {
    if (!connection.authenticated) {
        connection.session?.send(Frame.Text(errorResponse("Authentication required", "AUTH_REQUIRED")))
        return
    }

    val channels = json["data"]?.jsonObject?.get("channels")?.jsonArray ?: return
    val subscribedChannels = mutableListOf<String>()
    
    channels.forEach { channelElement ->
        val channelName = channelElement.jsonPrimitive.content
        try {
            val channel = WebSocketChannel.valueOf(channelName.uppercase())
            if (WebSocketManager.sessionManager.subscribe(connection.sessionId, channel)) {
                subscribedChannels.add(channelName)
            }
        } catch (e: Exception) {
            logger.warn { "Invalid channel: $channelName" }
        }
    }

    connection.session?.send(Frame.Text(responseWithData(
        type = "subscribe_response",
        data = buildJsonObject {
            put("success", true)
            putJsonArray("channels") { subscribedChannels.forEach { add(it) } }
            put("message", "Subscribed to ${subscribedChannels.size} channel(s)")
        }
    )))
}

private suspend fun handleUnsubscribeMessage(json: JsonObject, connection: WebSocketConnection) {
    if (!connection.authenticated) {
        connection.session?.send(Frame.Text(errorResponse("Authentication required", "AUTH_REQUIRED")))
        return
    }

    val channels = json["data"]?.jsonObject?.get("channels")?.jsonArray ?: return
    val unsubscribedChannels = mutableListOf<String>()
    
    channels.forEach { channelElement ->
        val channelName = channelElement.jsonPrimitive.content
        try {
            val channel = WebSocketChannel.valueOf(channelName.uppercase())
            if (WebSocketManager.sessionManager.unsubscribe(connection.sessionId, channel)) {
                unsubscribedChannels.add(channelName)
            }
        } catch (e: Exception) {
            logger.warn { "Invalid channel: $channelName" }
        }
    }

    connection.session?.send(Frame.Text(responseWithData(
        type = "unsubscribe_response",
        data = buildJsonObject {
            put("success", true)
            putJsonArray("channels") { unsubscribedChannels.forEach { add(it) } }
            put("message", "Unsubscribed from ${unsubscribedChannels.size} channel(s)")
        }
    )))
}

private suspend fun handleUnknownMessageType(connection: WebSocketConnection, type: String?) {
    logger.warn { "Unknown message type: $type" }
    connection.session?.send(Frame.Text(errorResponse("Unknown message type: $type", "UNKNOWN_TYPE")))
}

