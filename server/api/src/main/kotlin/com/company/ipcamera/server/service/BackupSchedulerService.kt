package com.company.ipcamera.server.service

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Планировщик автоматического резервного копирования (4.1.3.1).
 * Запускает создание бэка по расписанию (интервал из env).
 */
class BackupSchedulerService(
    private val backupService: DatabaseBackupService?
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val lastRunAt = AtomicLong(0)

    /** Интервал в миллисекундах (0 = отключено). */
    private val intervalMs: Long
        get() {
            val minutes = System.getenv("BACKUP_SCHEDULER_INTERVAL_MIN")?.toLongOrNull()?.coerceIn(1, 10080) ?: 0L
            return minutes * 60 * 1000
        }

    fun start() {
        if (backupService == null || intervalMs <= 0) {
            logger.info { "Backup scheduler disabled (no service or BACKUP_SCHEDULER_INTERVAL_MIN=0)" }
            return
        }
        logger.info { "Backup scheduler started, interval ${intervalMs / 60000} min" }
        scope.launch {
            while (isActive) {
                delay(intervalMs)
                runCatching {
                    lastRunAt.set(System.currentTimeMillis())
                    backupService.createBackup(null).getOrThrow()
                    logger.info { "Scheduled backup completed" }
                }.onFailure { e ->
                    logger.error(e) { "Scheduled backup failed" }
                }
            }
        }
    }

    fun getLastRunAt(): Long = lastRunAt.get()
}
