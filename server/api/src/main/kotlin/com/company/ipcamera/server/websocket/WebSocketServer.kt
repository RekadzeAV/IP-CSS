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
    NOTIFICATIONS // Уведомления
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
        val message = Json.encodeToString(
            ServerWebSocketMessage.EventMessage(
                type = type,
                channel = channel.name.lowercase(),
                data = data
            )
        )
        sessionManager.broadcastToChannel(channel, message)
    }
}

/**
 * Настройка WebSocket маршрутов
 */
fun Application.configureWebSocket() {
    val userRepository: ServerUserRepository by inject()
    
    routing {
        webSocket("/ws") {
            val sessionId = UUID.randomUUID().toString()
            var authenticated = false
            var userId: String? = null
            
            try {
                WebSocketManager.sessionManager.addSession(sessionId, this)
                
                logger.info { "WebSocket connection established: $sessionId" }
                
                // Обработка входящих сообщений
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            try {
                                val text = frame.readText()
                                val json = Json.parseToJsonElement(text).jsonObject
                                val type = json["type"]?.jsonPrimitive?.content
                                
                                when (type) {
                                    "auth" -> {
                                        val token = json["data"]?.jsonObject?.get("token")?.jsonPrimitive?.content
                                        if (token != null) {
                                            try {
                                                val verifier = JwtConfig.createVerifier()
                                                val decodedJWT: DecodedJWT = verifier.verify(token)
                                                userId = decodedJWT.subject
                                                
                                                // Проверяем, что пользователь существует и активен
                                                val user = userRepository.getUserById(userId)
                                                if (user != null && user.isActive) {
                                                    authenticated = true
                                                    val response = Json.encodeToString(
                                                        ServerWebSocketMessage.AuthResponse(
                                                            success = true,
                                                            message = "Authentication successful"
                                                        )
                                                    )
                                                    send(Frame.Text(response))
                                                    logger.info { "WebSocket authenticated: $sessionId (user: $userId)" }
                                                } else {
                                                    val response = Json.encodeToString(
                                                        ServerWebSocketMessage.AuthResponse(
                                                            success = false,
                                                            message = "User not found or inactive"
                                                        )
                                                    )
                                                    send(Frame.Text(response))
                                                }
                                            } catch (e: Exception) {
                                                logger.warn { "WebSocket authentication failed: ${e.message}" }
                                                val response = Json.encodeToString(
                                                    ServerWebSocketMessage.AuthResponse(
                                                        success = false,
                                                        message = "Invalid token"
                                                    )
                                                )
                                                send(Frame.Text(response))
                                            }
                                        }
                                    }
                                    
                                    "subscribe" -> {
                                        if (!authenticated) {
                                            val response = Json.encodeToString(
                                                ServerWebSocketMessage.ErrorMessage(
                                                    error = "Authentication required",
                                                    code = "AUTH_REQUIRED"
                                                )
                                            )
                                            send(Frame.Text(response))
                                            continue
                                        }
                                        
                                        val channels = json["data"]?.jsonObject?.get("channels")?.jsonArray
                                        if (channels != null) {
                                            val subscribedChannels = mutableListOf<String>()
                                            channels.forEach { channelElement ->
                                                val channelName = channelElement.jsonPrimitive.content
                                                try {
                                                    val channel = WebSocketChannel.valueOf(channelName.uppercase())
                                                    if (WebSocketManager.sessionManager.subscribe(sessionId, channel)) {
                                                        subscribedChannels.add(channelName)
                                                    }
                                                } catch (e: Exception) {
                                                    logger.warn { "Invalid channel: $channelName" }
                                                }
                                            }
                                            
                                            val response = Json.encodeToString(
                                                ServerWebSocketMessage.SubscribeResponse(
                                                    success = true,
                                                    channels = subscribedChannels,
                                                    message = "Subscribed to ${subscribedChannels.size} channel(s)"
                                                )
                                            )
                                            send(Frame.Text(response))
                                        }
                                    }
                                    
                                    "unsubscribe" -> {
                                        if (!authenticated) {
                                            val response = Json.encodeToString(
                                                ServerWebSocketMessage.ErrorMessage(
                                                    error = "Authentication required",
                                                    code = "AUTH_REQUIRED"
                                                )
                                            )
                                            send(Frame.Text(response))
                                            continue
                                        }
                                        
                                        val channels = json["data"]?.jsonObject?.get("channels")?.jsonArray
                                        if (channels != null) {
                                            val unsubscribedChannels = mutableListOf<String>()
                                            channels.forEach { channelElement ->
                                                val channelName = channelElement.jsonPrimitive.content
                                                try {
                                                    val channel = WebSocketChannel.valueOf(channelName.uppercase())
                                                    if (WebSocketManager.sessionManager.unsubscribe(sessionId, channel)) {
                                                        unsubscribedChannels.add(channelName)
                                                    }
                                                } catch (e: Exception) {
                                                    logger.warn { "Invalid channel: $channelName" }
                                                }
                                            }
                                            
                                            val response = Json.encodeToString(
                                                ServerWebSocketMessage.UnsubscribeResponse(
                                                    success = true,
                                                    channels = unsubscribedChannels,
                                                    message = "Unsubscribed from ${unsubscribedChannels.size} channel(s)"
                                                )
                                            )
                                            send(Frame.Text(response))
                                        }
                                    }
                                    
                                    else -> {
                                        logger.warn { "Unknown message type: $type" }
                                        val response = Json.encodeToString(
                                            ServerWebSocketMessage.ErrorMessage(
                                                error = "Unknown message type: $type",
                                                code = "UNKNOWN_TYPE"
                                            )
                                        )
                                        send(Frame.Text(response))
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error(e) { "Error processing WebSocket message" }
                                val response = Json.encodeToString(
                                    ServerWebSocketMessage.ErrorMessage(
                                        error = "Error processing message: ${e.message}",
                                        code = "PROCESSING_ERROR"
                                    )
                                )
                                try {
                                    send(Frame.Text(response))
                                } catch (sendError: Exception) {
                                    logger.error(sendError) { "Error sending error message" }
                                }
                            }
                        }
                        
                        is Frame.Close -> {
                            logger.info { "WebSocket close frame received: $sessionId" }
                            break
                        }
                        
                        is Frame.Ping, is Frame.Pong -> {
                            // Обрабатывается автоматически Ktor
                        }
                        
                        else -> {
                            logger.warn { "Unsupported frame type: ${frame::class.simpleName}" }
                        }
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

