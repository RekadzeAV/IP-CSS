package com.company.ipcamera.server.sync

/**
 * Сервис облачной синхронизации камер, настроек и событий между устройствами (4.1.1).
 */
interface CloudSyncService {

    suspend fun syncCameras(): Result<Unit>
    suspend fun syncSettings(): Result<Unit>
    suspend fun syncEvents(): Result<Unit>

    /** Полная синхронизация (камеры, настройки, события). */
    suspend fun syncAll(): Result<Unit>

    fun getSyncState(): SyncState
    suspend fun getConflicts(): List<SyncConflict>
    suspend fun resolveConflicts(conflicts: List<SyncConflict>, strategy: ConflictResolutionStrategy): Result<Unit>
}
