package com.company.ipcamera.shared.data.datasource.local

import com.company.ipcamera.shared.domain.model.Camera

/**
 * Локальный источник данных для камер.
 * Отвечает за работу с локальной базой данных SQLite.
 */
interface CameraLocalDataSource {
    /**
     * Получить все камеры из локальной БД
     */
    suspend fun getCameras(): List<Camera>

    /**
     * Получить камеру по ID из локальной БД
     */
    suspend fun getCameraById(id: String): Camera?

    /**
     * Сохранить камеру в локальную БД
     */
    suspend fun saveCamera(camera: Camera): Result<Camera>

    /**
     * Сохранить список камер в локальную БД (batch операция)
     */
    suspend fun saveCameras(cameras: List<Camera>): Result<List<Camera>>

    /**
     * Обновить камеру в локальной БД
     */
    suspend fun updateCamera(camera: Camera): Result<Camera>

    /**
     * Удалить камеру из локальной БД
     */
    suspend fun deleteCamera(id: String): Result<Unit>

    /**
     * Удалить все камеры из локальной БД
     */
    suspend fun deleteAllCameras(): Result<Unit>

    /**
     * Обновить статус камеры
     */
    suspend fun updateCameraStatus(id: String, status: String, lastSeen: Long?): Result<Unit>

    /**
     * Проверить существование камеры
     */
    suspend fun cameraExists(id: String): Boolean
}

