package com.company.ipcamera.shared.domain.repository

import com.company.ipcamera.shared.domain.model.StoredLicensePlate

/**
 * Репозиторий для хранения записей распознанных номерных знаков (ANPR).
 */
interface LicensePlateRepository {
    /**
     * Сохранить распознанный номер.
     */
    suspend fun insert(plate: StoredLicensePlate): Result<Unit>

    /**
     * Список записей по камере (последние сначала).
     */
    suspend fun getByCameraId(
        cameraId: String,
        limit: Int = 100,
    ): List<StoredLicensePlate>

    /**
     * Список записей по камере и диапазону времени.
     */
    suspend fun getByCameraIdAndDateRange(
        cameraId: String,
        fromTimestamp: Long,
        toTimestamp: Long,
    ): List<StoredLicensePlate>

    /**
     * Поиск по номеру.
     */
    suspend fun getByPlateNumber(
        plateNumber: String,
        limit: Int = 50,
    ): List<StoredLicensePlate>

    /**
     * Удалить записи старше указанной метки времени.
     */
    suspend fun deleteOlderThan(timestamp: Long): Result<Int>
}
