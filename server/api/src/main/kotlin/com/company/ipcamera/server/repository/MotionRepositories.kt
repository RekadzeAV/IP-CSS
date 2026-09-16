package com.company.ipcamera.server.repository

import com.company.ipcamera.shared.domain.model.MotionConfig
import com.company.ipcamera.shared.domain.model.MotionEvent
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для конфигурации детекции движения
 */
interface MotionConfigRepository {
    
    /**
     * Получить конфигурацию по ID
     */
    suspend fun findById(id: String): MotionConfig?
    
    /**
     * Получить конфигурацию по ID камеры
     */
    suspend fun findByCameraId(cameraId: String): MotionConfig?
    
    /**
     * Получить все конфигурации
     */
    suspend fun findAll(): List<MotionConfig>
    
    /**
     * Сохранить конфигурацию
     */
    suspend fun save(config: MotionConfig): Result<MotionConfig>
    
    /**
     * Удалить конфигурацию
     */
    suspend fun delete(id: String): Result<Unit>
    
    /**
     * Обновить статус
     */
    suspend fun updateStatus(cameraId: String, enabled: Boolean): Result<MotionConfig>
}

/**
 * Репозиторий для событий детекции движения
 */
interface MotionEventRepository {
    
    /**
     * Получить событие по ID
     */
    suspend fun findById(id: String): MotionEvent?
    
    /**
     * Получить события по камере
     */
    suspend fun findByCameraId(cameraId: String, limit: Int = 50): List<MotionEvent>
    
    /**
     * Получить события по диапазону времени
     */
    suspend fun findByTimeRange(startTime: Long, endTime: Long, limit: Int = 100): List<MotionEvent>
    
    /**
     * Получить необработанные события
     */
    suspend fun findUnprocessed(): List<MotionEvent>
    
    /**
     * Сохранить событие
     */
    suspend fun save(event: MotionEvent): Result<MotionEvent>
    
    /**
     * Отметить событие как обработанное
     */
    suspend fun markProcessed(id: String): Result<Unit>
    
    /**
     * Удалить старые события
     */
    suspend fun deleteOlderThan(timestamp: Long): Result<Int>
}
