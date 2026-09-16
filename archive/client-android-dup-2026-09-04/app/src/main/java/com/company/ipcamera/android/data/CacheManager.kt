package com.company.ipcamera.android.data

import android.content.Context
import com.company.ipcamera.android.data.local.*
import kotlinx.coroutines.flow.Flow

/**
 * Менеджер кэширования для offline-режима Android.
 * Использует Room для локального хранения камер, событий и записей.
 */
class CacheManager(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val cameraDao = db.cameraDao()
    private val eventDao = db.eventDao()
    private val recordingDao = db.recordingDao()

    // ========== Cameras ==========

    fun getAllCameras(): Flow<List<CameraEntity>> = cameraDao.getAllCameras()

    suspend fun getCameraById(id: String): CameraEntity? = cameraDao.getCameraById(id)

    suspend fun cacheCameras(cameras: List<CameraEntity>) {
        cameraDao.insertCameras(cameras)
    }

    suspend fun cacheCamera(camera: CameraEntity) {
        cameraDao.insertCamera(camera)
    }

    suspend fun deleteCamera(id: String) {
        cameraDao.deleteCameraById(id)
    }

    fun getCameraCount(): Flow<Int> = cameraDao.getCameraCount()

    // ========== Events ==========

    fun getAllEvents(): Flow<List<EventEntity>> = eventDao.getAllEvents()

    fun getEventsByCamera(cameraId: String): Flow<List<EventEntity>> = eventDao.getEventsByCamera(cameraId)

    suspend fun cacheEvents(events: List<EventEntity>) {
        eventDao.insertEvents(events)
    }

    suspend fun acknowledgeEvent(id: String) {
        eventDao.acknowledgeEvent(id)
    }

    fun getUnacknowledgedCount(): Flow<Int> = eventDao.getUnacknowledgedCount()

    // ========== Recordings ==========

    fun getAllRecordings(): Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    fun getRecordingsByCamera(cameraId: String): Flow<List<RecordingEntity>> = recordingDao.getRecordingsByCamera(cameraId)

    suspend fun cacheRecording(recording: RecordingEntity) {
        recordingDao.insertRecording(recording)
    }

    // ========== Cleanup ==========

    suspend fun cleanupOldData(retentionDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        eventDao.deleteOldEvents(cutoff)
        recordingDao.deleteOldRecordings(cutoff)
    }
}
