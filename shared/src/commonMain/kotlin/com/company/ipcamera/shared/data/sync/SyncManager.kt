package com.company.ipcamera.shared.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Менеджер синхронизации, объединяющий очередь синхронизации и инкрементальную синхронизацию
 */
class SyncManager(
    private val syncQueue: SyncQueue = SyncQueue(),
    private val incrementalSyncManager: IncrementalSyncManager = IncrementalSyncManager(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /**
     * Установить статус онлайн/офлайн
     */
    fun setOnlineStatus(isOnline: Boolean) {
        _isOnline.value = isOnline
        logger.info { "Network status changed: ${if (isOnline) "ONLINE" else "OFFLINE"}" }

        if (isOnline && !syncQueue.isEmpty()) {
            // При восстановлении сети запускаем синхронизацию очереди
            scope.launch {
                processSyncQueue()
            }
        }
    }

    /**
     * Добавить операцию в очередь синхронизации
     */
    fun enqueueSync(operation: SyncOperation) {
        if (_isOnline.value) {
            // Если онлайн, пытаемся синхронизировать сразу
            scope.launch {
                processSyncOperation(operation)
            }
        } else {
            // Если офлайн, добавляем в очередь
            syncQueue.enqueue(operation)
        }
    }

    /**
     * Обработать операцию синхронизации
     */
    private suspend fun processSyncOperation(operation: SyncOperation) {
        try {
            // Здесь должна быть логика синхронизации с удаленным сервером
            // Для примера просто логируем
            logger.info { "Processing sync operation: ${operation.type} for ${operation.entityType}/${operation.entityId}" }

            // После успешной синхронизации удаляем из очереди
            syncQueue.dequeue(operation)

            // Обновляем timestamp последней синхронизации
            incrementalSyncManager.updateLastSyncTimestamp(operation.entityType)
        } catch (e: Exception) {
            logger.error(
                e,
            ) { "Failed to sync operation: ${operation.type} for ${operation.entityType}/${operation.entityId}" }

            // Если превышен лимит попыток, удаляем из очереди
            if (operation.retryCount >= 3) {
                logger.warn { "Max retries reached for operation: ${operation.entityId}, removing from queue" }
                syncQueue.dequeue(operation)
            } else {
                // Увеличиваем счетчик попыток и оставляем в очереди
                val updatedOperation = operation.incrementRetry()
                syncQueue.dequeue(operation)
                syncQueue.enqueue(updatedOperation)
            }
        }
    }

    /**
     * Обработать всю очередь синхронизации
     */
    private suspend fun processSyncQueue() {
        if (_isSyncing.value) {
            logger.debug { "Sync already in progress, skipping" }
            return
        }

        _isSyncing.value = true
        try {
            val operations = syncQueue.getAll()
            logger.info { "Processing ${operations.size} sync operations from queue" }

            operations.forEach { operation ->
                processSyncOperation(operation)
            }
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Проверить, требуется ли синхронизация для сущности
     */
    fun shouldSync(
        entityType: String,
        syncIntervalMs: Long = 60000,
    ): Boolean {
        return incrementalSyncManager.shouldSync(entityType, syncIntervalMs)
    }

    /**
     * Получить очередь синхронизации
     */
    fun getSyncQueue(): SyncQueue = syncQueue

    /**
     * Получить менеджер инкрементальной синхронизации
     */
    fun getIncrementalSyncManager(): IncrementalSyncManager = incrementalSyncManager
}
