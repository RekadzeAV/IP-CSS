package com.company.ipcamera.shared.data.sync

import com.company.ipcamera.shared.common.nowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Очередь синхронизации для операций, которые должны быть выполнены при восстановлении сети
 */
class SyncQueue {
    private val _queue = MutableStateFlow<List<SyncOperation>>(emptyList())
    val queue: StateFlow<List<SyncOperation>> = _queue.asStateFlow()

    /**
     * Добавить операцию в очередь
     */
    fun enqueue(operation: SyncOperation) {
        _queue.value = _queue.value + operation
        logger.debug { "Added sync operation to queue: ${operation.type} for ${operation.entityId}" }
    }

    /**
     * Удалить операцию из очереди
     */
    fun dequeue(operation: SyncOperation) {
        _queue.value = _queue.value.filter { it != operation }
        logger.debug { "Removed sync operation from queue: ${operation.type} for ${operation.entityId}" }
    }

    /**
     * Получить все операции из очереди
     */
    fun getAll(): List<SyncOperation> = _queue.value

    /**
     * Очистить очередь
     */
    fun clear() {
        _queue.value = emptyList()
        logger.debug { "Sync queue cleared" }
    }

    /**
     * Проверить, есть ли операции в очереди
     */
    fun isEmpty(): Boolean = _queue.value.isEmpty()
}

/**
 * Операция синхронизации
 */
data class SyncOperation(
    val type: SyncOperationType,
    val entityType: String, // "camera", "recording", "event", etc.
    val entityId: String,
    val data: Map<String, Any> = emptyMap(),
    val timestamp: Long = nowMillis(),
    val retryCount: Int = 0,
) {
    /**
     * Увеличить счетчик попыток
     */
    fun incrementRetry(): SyncOperation {
        return copy(retryCount = retryCount + 1)
    }
}

enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE,
}
