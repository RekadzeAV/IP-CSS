package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabaseSync
import com.company.ipcamera.shared.domain.model.StoredLicensePlate
import com.company.ipcamera.shared.domain.repository.LicensePlateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Реализация LicensePlateRepository через SQLDelight.
 */
class LicensePlateRepositoryImplSqlDelight(
    private val databaseFactory: DatabaseFactory,
) : LicensePlateRepository {
    private val database = createDatabaseSync(databaseFactory.createDriver())

    override suspend fun insert(plate: StoredLicensePlate): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.insertLicensePlate(
                    id = plate.id,
                    camera_id = plate.cameraId,
                    timestamp = plate.timestamp,
                    plate_number = plate.plateNumber,
                    confidence = plate.confidence.toDouble(),
                    country = plate.country,
                    bbox_x = plate.bboxX.toLong(),
                    bbox_y = plate.bboxY.toLong(),
                    bbox_width = plate.bboxWidth.toLong(),
                    bbox_height = plate.bboxHeight.toLong(),
                    created_at = plate.createdAt,
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getByCameraId(
        cameraId: String,
        limit: Int,
    ): List<StoredLicensePlate> =
        withContext(Dispatchers.Default) {
            database.cameraDatabaseQueries
                .selectLicensePlatesByCameraId(cameraId)
                .executeAsList()
                .take(limit)
                .map { row ->
                    StoredLicensePlate(
                        id = row.id,
                        cameraId = row.camera_id,
                        timestamp = row.timestamp,
                        plateNumber = row.plate_number,
                        confidence = row.confidence.toFloat(),
                        country = row.country,
                        bboxX = row.bbox_x.toInt(),
                        bboxY = row.bbox_y.toInt(),
                        bboxWidth = row.bbox_width.toInt(),
                        bboxHeight = row.bbox_height.toInt(),
                        createdAt = row.created_at,
                    )
                }
        }

    override suspend fun getByCameraIdAndDateRange(
        cameraId: String,
        fromTimestamp: Long,
        toTimestamp: Long,
    ): List<StoredLicensePlate> =
        withContext(Dispatchers.Default) {
            database.cameraDatabaseQueries
                .selectLicensePlatesByDateRange(cameraId, fromTimestamp, toTimestamp)
                .executeAsList()
                .map { row ->
                    StoredLicensePlate(
                        id = row.id,
                        cameraId = row.camera_id,
                        timestamp = row.timestamp,
                        plateNumber = row.plate_number,
                        confidence = row.confidence.toFloat(),
                        country = row.country,
                        bboxX = row.bbox_x.toInt(),
                        bboxY = row.bbox_y.toInt(),
                        bboxWidth = row.bbox_width.toInt(),
                        bboxHeight = row.bbox_height.toInt(),
                        createdAt = row.created_at,
                    )
                }
        }

    override suspend fun getByPlateNumber(
        plateNumber: String,
        limit: Int,
    ): List<StoredLicensePlate> =
        withContext(Dispatchers.Default) {
            database.cameraDatabaseQueries
                .selectLicensePlatesByPlateNumber(plateNumber)
                .executeAsList()
                .take(limit)
                .map { row ->
                    StoredLicensePlate(
                        id = row.id,
                        cameraId = row.camera_id,
                        timestamp = row.timestamp,
                        plateNumber = row.plate_number,
                        confidence = row.confidence.toFloat(),
                        country = row.country,
                        bboxX = row.bbox_x.toInt(),
                        bboxY = row.bbox_y.toInt(),
                        bboxWidth = row.bbox_width.toInt(),
                        bboxHeight = row.bbox_height.toInt(),
                        createdAt = row.created_at,
                    )
                }
        }

    override suspend fun deleteOlderThan(timestamp: Long): Result<Int> =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.deleteOldLicensePlates(timestamp)
                Result.success(0)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
