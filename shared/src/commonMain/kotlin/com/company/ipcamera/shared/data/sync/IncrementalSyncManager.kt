package com.company.ipcamera.shared.data.sync

import com.company.ipcamera.shared.common.nowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Менеджер инкрементальной синхронизации
 * Отслеживает изменения и синхронизирует только измененные данные
 */
class IncrementalSyncManager {
    private val _lastSyncTimestamps = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lastSyncTimestamps: StateFlow<Map<String, Long>> = _lastSyncTimestamps.asStateFlow()

    /**
     * Обновить timestamp последней синхронизации для сущности
     */
    fun updateLastSyncTimestamp(
        entityType: String,
        timestamp: Long = nowMillis(),
    ) {
        _lastSyncTimestamps.value = _lastSyncTimestamps.value + (entityType to timestamp)
        logger.debug { "Updated last sync timestamp for $entityType: $timestamp" }
    }

    /**
     * Получить timestamp последней синхронизации для сущности
     */
    fun getLastSyncTimestamp(entityType: String): Long? {
        return _lastSyncTimestamps.value[entityType]
    }

    /**
     * Проверить, требуется ли синхронизация (прошло ли достаточно времени с последней синхронизации)
     */
    fun shouldSync(
        entityType: String,
        syncIntervalMs: Long = 60000,
    ): Boolean {
        val lastSync = getLastSyncTimestamp(entityType) ?: return true
        val timeSinceLastSync = nowMillis() - lastSync
        return timeSinceLastSync >= syncIntervalMs
    }

    /**
     * Сбросить timestamp синхронизации для сущности (принудительная синхронизация)
     */
    fun resetSyncTimestamp(entityType: String) {
        _lastSyncTimestamps.value = _lastSyncTimestamps.value - entityType
        logger.debug { "Reset sync timestamp for $entityType" }
    }

    /**
     * Очистить все timestamps
     */
    fun clear() {
        _lastSyncTimestamps.value = emptyMap()
        logger.debug { "Cleared all sync timestamps" }
    }
}
