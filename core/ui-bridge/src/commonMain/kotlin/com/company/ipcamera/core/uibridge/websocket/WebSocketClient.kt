package com.company.ipcamera.core.uibridge.websocket

/**
 * Интерфейс WebSocket клиента для UI Bridge
 * Обеспечивает real-time коммуникацию между сервером и UI
 */
interface WebSocketClient {
    /**
     * Текущее состояние подключения
     */
    val state: kotlinx.coroutines.flow.StateFlow<WebSocketState>

    /**
     * Поток входящих сообщений
     */
    val messages: kotlinx.coroutines.flow.Flow<WebSocketMessage>

    /**
     * Подключение к WebSocket серверу
     */
    suspend fun connect()

    /**
     * Отключение от WebSocket сервера
     */
    suspend fun disconnect()

    /**
     * Отправка сообщения
     */
    suspend fun send(message: WebSocketMessage)

    /**
     * Проверка подключения
     */
    fun isConnected(): Boolean

    /**
     * Повторное подключение
     */
    suspend fun reconnect()

    /**
     * Авторизация на сервере
     */
    suspend fun authenticate(token: String)
}
