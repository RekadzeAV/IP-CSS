package com.company.ipcamera.server.sync

import com.company.ipcamera.server.config.CloudSyncConfig
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

/**
 * Реализация облачной синхронизации (4.1.1).
 * Состояние и конфликты хранятся в памяти; при наличии удалённого хранилища — обмен через него.
 */
class CloudSyncServiceImpl(
    private val cameraRepository: CameraRepository,
    private val settingsRepository: SettingsRepository,
    private val eventRepository: EventRepository
) : CloudSyncService {

    private val stateRef = AtomicReference(SyncState())
    private val conflictsRef = AtomicReference<List<SyncConflict>>(emptyList())
    private val mutex = Mutex()

    override suspend fun syncCameras(): Result<Unit> = mutex.withLock {
        setState { copy(status = SyncStatus.RUNNING) }
        runCatching {
            // Локальная синхронизация: при наличии удалённого источника здесь будет pull/push
            val cameras = cameraRepository.getCameras()
            logger.info { "Sync cameras: ${cameras.size} items" }
            setState {
                copy(
                    lastSyncCamerasAt = System.currentTimeMillis(),
                    lastSyncAt = System.currentTimeMillis(),
                    status = SyncStatus.SUCCESS,
                    errorMessage = null
                )
            }
        }.onFailure { e ->
            logger.error(e) { "Sync cameras failed" }
            setState {
                copy(status = SyncStatus.FAILED, errorMessage = e.message)
            }
        }
    }

    override suspend fun syncSettings(): Result<Unit> = mutex.withLock {
        setState { copy(status = SyncStatus.RUNNING) }
        runCatching {
            val settings = settingsRepository.getSettings()
            logger.info { "Sync settings: ${settings.size} keys" }
            setState {
                copy(
                    lastSyncSettingsAt = System.currentTimeMillis(),
                    lastSyncAt = System.currentTimeMillis(),
                    status = SyncStatus.SUCCESS,
                    errorMessage = null
                )
            }
        }.onFailure { e ->
            logger.error(e) { "Sync settings failed" }
            setState { copy(status = SyncStatus.FAILED, errorMessage = e.message) }
        }
    }

    override suspend fun syncEvents(): Result<Unit> = mutex.withLock {
        setState { copy(status = SyncStatus.RUNNING) }
        runCatching {
            val result = eventRepository.getEvents(page = 1, limit = 1000)
            logger.info { "Sync events: ${result.items.size} items" }
            setState {
                copy(
                    lastSyncEventsAt = System.currentTimeMillis(),
                    lastSyncAt = System.currentTimeMillis(),
                    status = SyncStatus.SUCCESS,
                    errorMessage = null
                )
            }
        }.onFailure { e ->
            logger.error(e) { "Sync events failed" }
            setState { copy(status = SyncStatus.FAILED, errorMessage = e.message) }
        }
    }

    override suspend fun syncAll(): Result<Unit> = mutex.withLock {
        setState { copy(status = SyncStatus.RUNNING) }
        val r1 = runCatching { syncCameras().getOrThrow() }
        val r2 = runCatching { syncSettings().getOrThrow() }
        val r3 = runCatching { syncEvents().getOrThrow() }
        val ok = r1.isSuccess && r2.isSuccess && r3.isSuccess
        if (!ok) {
            setState {
                copy(
                    status = SyncStatus.FAILED,
                    errorMessage = listOfNotNull(r1.exceptionOrNull()?.message, r2.exceptionOrNull()?.message, r3.exceptionOrNull()?.message).firstOrNull()
                )
            }
            return@withLock Result.failure(r1.exceptionOrNull() ?: r2.exceptionOrNull() ?: r3.exceptionOrNull()!!)
        }
        Result.success(Unit)
    }

    override fun getSyncState(): SyncState = stateRef.get()

    override suspend fun getConflicts(): List<SyncConflict> = conflictsRef.get()

    override suspend fun resolveConflicts(conflicts: List<SyncConflict>, strategy: ConflictResolutionStrategy): Result<Unit> = mutex.withLock {
        when (strategy) {
            ConflictResolutionStrategy.LAST_WRITE_WINS, ConflictResolutionStrategy.MERGE -> {
                conflictsRef.set(conflictsRef.get() - conflicts.toSet())
                setState { copy(status = SyncStatus.SUCCESS, errorMessage = null) }
                Result.success(Unit)
            }
            ConflictResolutionStrategy.MANUAL -> {
                // Ожидаем явного выбора версии через API; здесь только сбрасываем после ручного resolve
                Result.success(Unit)
            }
        }
    }

    private fun setState(block: SyncState.() -> SyncState) {
        stateRef.updateAndGet { block(it) }
    }
}
