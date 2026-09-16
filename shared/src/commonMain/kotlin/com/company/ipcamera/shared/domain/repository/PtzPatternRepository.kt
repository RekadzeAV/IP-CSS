package com.company.ipcamera.shared.domain.repository

import com.company.ipcamera.shared.domain.model.PtzGeozone
import com.company.ipcamera.shared.domain.model.PtzPattern
import com.company.ipcamera.shared.domain.model.PtzTour

/**
 * Репозиторий расширенного PTZ (блок 9.2): паттерны, туры, геозоны.
 */
interface PtzPatternRepository {
    suspend fun getPatternById(id: String): PtzPattern?

    suspend fun getPatternsByCameraId(cameraId: String): List<PtzPattern>

    suspend fun insertPattern(pattern: PtzPattern): Result<Unit>

    suspend fun updatePattern(pattern: PtzPattern): Result<Unit>

    suspend fun deletePattern(id: String): Result<Unit>

    suspend fun getTourById(id: String): PtzTour?

    suspend fun getToursByCameraId(cameraId: String): List<PtzTour>

    suspend fun insertTour(tour: PtzTour): Result<Unit>

    suspend fun updateTour(tour: PtzTour): Result<Unit>

    suspend fun deleteTour(id: String): Result<Unit>

    suspend fun getGeozoneById(id: String): PtzGeozone?

    suspend fun getGeozonesByCameraId(cameraId: String): List<PtzGeozone>

    suspend fun insertGeozone(geozone: PtzGeozone): Result<Unit>

    suspend fun updateGeozone(geozone: PtzGeozone): Result<Unit>

    suspend fun deleteGeozone(id: String): Result<Unit>
}
