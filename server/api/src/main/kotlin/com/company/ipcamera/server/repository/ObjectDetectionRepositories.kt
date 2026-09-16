package com.company.ipcamera.server.repository

import com.company.ipcamera.shared.domain.model.ObjectDetectionConfig
import com.company.ipcamera.shared.domain.model.ObjectDetectionEvent
import com.company.ipcamera.shared.domain.model.ObjectStats

/**
 * Репозиторий для конфигурации детекции объектов
 */
interface ObjectDetectionConfigRepository {
    suspend fun findById(id: String): ObjectDetectionConfig?
    suspend fun findByCameraId(cameraId: String): ObjectDetectionConfig?
    suspend fun findAll(): List<ObjectDetectionConfig>
    suspend fun save(config: ObjectDetectionConfig): Result<ObjectDetectionConfig>
    suspend fun delete(id: String): Result<Unit>
    suspend fun updateStatus(cameraId: String, enabled: Boolean): Result<ObjectDetectionConfig>
}

/**
 * Репозиторий для событий детекции объектов
 */
interface ObjectDetectionEventRepository {
    suspend fun findById(id: String): ObjectDetectionEvent?
    suspend fun findByCameraId(cameraId: String, limit: Int = 50): List<ObjectDetectionEvent>
    suspend fun findByTimeRange(startTime: Long, endTime: Long, limit: Int = 100): List<ObjectDetectionEvent>
    suspend fun findByClass(className: String, limit: Int = 100): List<ObjectDetectionEvent>
    suspend fun findUnprocessed(): List<ObjectDetectionEvent>
    suspend fun save(event: ObjectDetectionEvent): Result<ObjectDetectionEvent>
    suspend fun markProcessed(id: String): Result<Unit>
    suspend fun getStats(cameraId: String, startTime: Long, endTime: Long): ObjectStats?
    suspend fun deleteOlderThan(timestamp: Long): Result<Int>
}
