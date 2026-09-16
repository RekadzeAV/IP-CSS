package com.company.ipcamera.server.sync

import kotlinx.serialization.Serializable

/**
 * Модели для синхронизации между устройствами (4.1.1).
 */

@Serializable
data class SyncState(
    val lastSyncCamerasAt: Long? = null,
    val lastSyncSettingsAt: Long? = null,
    val lastSyncEventsAt: Long? = null,
    val lastSyncAt: Long? = null,
    val status: SyncStatus = SyncStatus.IDLE,
    val errorMessage: String? = null
)

@Serializable
enum class SyncStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILED,
    CONFLICTS
}

@Serializable
data class SyncConflict(
    val id: String,
    val resourceType: String, // cameras | settings | events
    val resourceId: String?,
    val localVersion: SyncResourceVersion,
    val remoteVersion: SyncResourceVersion,
    val detectedAt: Long
)

@Serializable
data class SyncResourceVersion(
    val updatedAt: Long,
    val payloadHash: String? = null
)

enum class ConflictResolutionStrategy {
    LAST_WRITE_WINS,
    MERGE,
    MANUAL
}
