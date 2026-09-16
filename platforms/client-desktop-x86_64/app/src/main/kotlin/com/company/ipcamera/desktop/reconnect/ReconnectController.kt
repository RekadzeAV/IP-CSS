package com.company.ipcamera.desktop.reconnect

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory

/**
 * Состояние управления reconnect для одной камеры.
 */
data class ReconnectState(
    val cameraId: String,
    val isConnected: Boolean = false,
    val isReconnecting: Boolean = false,
    val currentAttempt: Int = 0,
    val maxAttempts: Int = 0,
    val lastError: String? = null,
    val lastErrorAtMs: Long? = null,
    val reconnectPolicy: ReconnectPolicy = ReconnectPolicy.BALANCED,
    val nextRetryDelayMs: Long? = null,
    val totalReconnectAttempts: Int = 0,
    val successfulReconnects: Int = 0,
    val failedReconnects: Int = 0
) {
    val canRetry: Boolean
        get() = reconnectPolicy.enabled && currentAttempt < maxAttempts && shouldRetryError(lastError, reconnectPolicy)

    val retryInMs: Long?
        get() = if (canRetry && nextRetryDelayMs != null) {
            nextRetryDelayMs
        } else {
            null
        }
}

/**
 * События reconnect для публикации в UI/телеметрию.
 */
sealed class ReconnectEvent {
    data class ReconnectStarted(
        val cameraId: String,
        val attempt: Int,
        val maxAttempts: Int,
        val reason: String? = null
    ) : ReconnectEvent()

    data class ReconnectSuccess(
        val cameraId: String,
        val attempt: Int,
        val totalAttempts: Int
    ) : ReconnectEvent()

    data class ReconnectFailed(
        val cameraId: String,
        val attempt: Int,
        val maxAttempts: Int,
        val error: String,
        val willRetry: Boolean
    ) : ReconnectEvent()

    data class ReconnectExhausted(
        val cameraId: String,
        val totalAttempts: Int,
        val lastError: String
    ) : ReconnectEvent()

    data class ReconnectCancelled(
        val cameraId: String,
        val reason: String? = null
    ) : ReconnectEvent()

    data class ErrorOccurred(
        val cameraId: String,
        val error: String,
        val timestampMs: Long = System.currentTimeMillis()
    ) : ReconnectEvent()
}

/**
 * Контроллер управления reconnect для RTSP потоков.
 * Обеспечивает экспоненциальный backoff, jitter и фильтрацию ошибок.
 */
class ReconnectController(
    private val reconnectCallback: suspend (cameraId: String) -> Boolean,
    private val eventCallback: (ReconnectEvent) -> Unit = {},
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val logger = LoggerFactory.getLogger(ReconnectController::class.java)

    private val reconnectJobs: MutableMap<String, Job> = mutableMapOf()
    private val reconnectStates: MutableMap<String, MutableStateFlow<ReconnectState>> = mutableMapOf()

    private val _globalEvents = MutableSharedFlow<ReconnectEvent>(extraBufferCapacity = 64)
    val globalEvents = _globalEvents.asSharedFlow()

    init {
        // Подписываемся на локальные события и ретранслируем их глобально
        coroutineScope.launch {
            while (true) {
                // Ждем событий от камер
                // Реализация через callback
                yield()
            }
        }
    }

    /**
     * Запустить или обновить политику reconnect для камеры.
     */
    fun startReconnectMonitoring(
        cameraId: String,
        policy: ReconnectPolicy = ReconnectPolicy.BALANCED
    ) {
        synchronized(reconnectStates) {
            val existingState = reconnectStates[cameraId]
            if (existingState == null) {
                reconnectStates[cameraId] = MutableStateFlow(
                    ReconnectState(
                        cameraId = cameraId,
                        reconnectPolicy = policy
                    )
                )
            } else {
                // Обновляем политику
                val currentState = existingState.value
                reconnectStates[cameraId] = MutableStateFlow(
                    currentState.copy(reconnectPolicy = policy)
                )
            }
        }

        // Запускаем мониторинг
        stopReconnect(cameraId) // Останавливаем существующий job если есть
        startReconnectJob(cameraId)
    }

    /**
     * Остановить reconnect для камеры.
     */
    fun stopReconnect(cameraId: String, reason: String? = null) {
        val job = reconnectJobs.remove(cameraId)
        job?.cancel()
        
        val state = reconnectStates[cameraId]?.value
        if (state != null) {
            reconnectStates[cameraId] = MutableStateFlow(
                state.copy(
                    isReconnecting = false,
                    nextRetryDelayMs = null
                )
            )
        }

        if (reason != null) {
            val event = ReconnectEvent.ReconnectCancelled(cameraId, reason)
            eventCallback(event)
            runCatching { _globalEvents.tryEmit(event) }
            logger.debug("[ $cameraId ] Reconnect cancelled: $reason")
        }
    }

    /**
     * Очистить состояние камеры.
     */
    fun dispose(cameraId: String) {
        stopReconnect(cameraId, "Camera disposed")
        reconnectStates.remove(cameraId)
        reconnectJobs.remove(cameraId)
    }

    /**
     * Получить текущее состояние reconnect для камеры.
     */
    fun getReconnectState(cameraId: String): StateFlow<ReconnectState>? {
        return reconnectStates[cameraId]?.asStateFlow()
    }

    /**
     * Сообщить контроллеру об ошибке потока.
     */
    fun onStreamError(cameraId: String, error: String) {
        val stateFlow = reconnectStates[cameraId] ?: return
        val state = stateFlow.value
        
        val event = ReconnectEvent.ErrorOccurred(cameraId, error)
        eventCallback(event)
        runCatching { _globalEvents.tryEmit(event) }

        if (!state.reconnectPolicy.enabled) {
            logger.debug("[ $cameraId ] Reconnect disabled, ignoring error: $error")
            return
        }

        if (!shouldRetryError(error, state.reconnectPolicy)) {
            logger.debug("[ $cameraId ] Error not retryable: $error")
            return
        }

        // Обновляем состояние
        val newAttempt = state.currentAttempt + 1
        val newState = state.copy(
            currentAttempt = newAttempt,
            lastError = error,
            lastErrorAtMs = System.currentTimeMillis(),
            totalReconnectAttempts = state.totalReconnectAttempts + 1
        )
        stateFlow.value = newState

        // Запускаем reconnect если можно
        if (newState.canRetry) {
            logger.info("[ $cameraId ] Stream error, scheduling reconnect attempt $newAttempt/${state.reconnectPolicy.maxAttempts}: $error")
            scheduleReconnect(cameraId, newAttempt)
        } else {
            logger.warn("[ $cameraId ] Reconnect exhausted or not allowed after attempt $newAttempt: $error")
            val event = ReconnectEvent.ReconnectExhausted(
                cameraId,
                newAttempt,
                error
            )
            eventCallback(event)
            runCatching { _globalEvents.tryEmit(event) }
        }
    }

    /**
     * Сообщить контроллеру об успешном подключении/переподключении.
     */
    fun onConnected(cameraId: String) {
        val stateFlow = reconnectStates[cameraId] ?: return
        val state = stateFlow.value

        val newState = state.copy(
            isConnected = true,
            isReconnecting = false,
            currentAttempt = 0,
            nextRetryDelayMs = null,
            successfulReconnects = state.successfulReconnects + (if (state.currentAttempt > 0) 1 else 0)
        )
        stateFlow.value = newState

        // Останавливаем любой запланированный reconnect
        reconnectJobs[cameraId]?.cancel()
    }

    /**
     * Запланировать reconnect для камеры.
     */
    private fun scheduleReconnect(cameraId: String, attempt: Int) {
        // Отменяем существующий job если есть
        reconnectJobs[cameraId]?.cancel()

        val stateFlow = reconnectStates[cameraId] ?: return
        val state = stateFlow.value
        val policy = state.reconnectPolicy
        val delayMs = calculateReconnectDelay(attempt, policy)

        logger.info("[ $cameraId ] Scheduling reconnect attempt $attempt in ${delayMs}ms (policy: $policy)")

        val job = coroutineScope.launch {
            val event = ReconnectEvent.ReconnectStarted(cameraId, attempt, policy.maxAttempts, state.lastError)
            eventCallback(event)
            runCatching { _globalEvents.tryEmit(event) }

            // Обновляем состояние
            stateFlow.value = stateFlow.value.copy(
                isReconnecting = true,
                currentAttempt = attempt,
                nextRetryDelayMs = delayMs
            )

            // Ждем перед попыткой
            delay(delayMs)

            // Проверяем, не был ли отменен
            if (isActive) {
                performReconnect(cameraId, attempt)
            }
        }

        reconnectJobs[cameraId] = job
    }

    /**
     * Выполнить reconnect для камеры.
     */
    private suspend fun performReconnect(cameraId: String, attempt: Int) {
        val stateFlow = reconnectStates[cameraId] ?: return
        val state = stateFlow.value

        logger.info("[ $cameraId ] Attempting reconnect attempt $attempt/${state.reconnectPolicy.maxAttempts}")

        try {
            val success = reconnectCallback(cameraId)

            if (success) {
                logger.info("[ $cameraId ] Reconnect attempt $attempt succeeded")
                onConnected(cameraId)

                val event = ReconnectEvent.ReconnectSuccess(
                    cameraId,
                    attempt,
                    state.totalReconnectAttempts
                )
                eventCallback(event)
                runCatching { _globalEvents.tryEmit(event) }
            } else {
                handleReconnectFailure(cameraId, attempt, "Reconnect callback returned false")
            }
        } catch (e: Exception) {
            logger.error("[ $cameraId ] Reconnect attempt $attempt failed with exception: ${e.message}", e)
            handleReconnectFailure(cameraId, attempt, e.message ?: "Unknown error")
        }
    }

    /**
     * Обработать неудачу reconnect.
     */
    private fun handleReconnectFailure(cameraId: String, attempt: Int, error: String) {
        val stateFlow = reconnectStates[cameraId] ?: return
        val state = stateFlow.value
        val policy = state.reconnectPolicy

        val newState = state.copy(
            isReconnecting = false,
            lastError = error,
            lastErrorAtMs = System.currentTimeMillis(),
            failedReconnects = state.failedReconnects + 1
        )
        stateFlow.value = newState

        val event = ReconnectEvent.ReconnectFailed(
            cameraId,
            attempt,
            policy.maxAttempts,
            error,
            willRetry = newState.canRetry
        )
        eventCallback(event)
        runCatching { _globalEvents.tryEmit(event) }

        if (newState.canRetry) {
            // Планируем следующую попытку
            scheduleReconnect(cameraId, attempt + 1)
        } else {
            // Все попытки исчерпаны
            logger.error("[ $cameraId ] All reconnect attempts exhausted. Last error: $error")
            val exhaustedEvent = ReconnectEvent.ReconnectExhausted(
                cameraId,
                attempt,
                error
            )
            eventCallback(exhaustedEvent)
            runCatching { _globalEvents.tryEmit(exhaustedEvent) }
        }
    }

    /**
     * Получить статистику reconnect для всех камер.
     */
    fun getReconnectStats(): Map<String, ReconnectState> {
        return reconnectStates.mapValues { it.value.value }
    }

    /**
     * Очистить все состояния.
     */
    fun disposeAll() {
        synchronized(reconnectStates) {
            reconnectStates.keys.toList().forEach { cameraId ->
                dispose(cameraId)
            }
        }
    }

    private fun startReconnectJob(cameraId: String) {
        // Фоновая задача мониторинга, может быть расширена для heartbeat/checkalive
    }
}
