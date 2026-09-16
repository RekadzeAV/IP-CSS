package com.company.ipcamera.server.config

/**
 * Конфигурация облачной синхронизации (4.1.1).
 */
object CloudSyncConfig {

    val enabled: Boolean
        get() = System.getenv("CLOUD_SYNC_ENABLED")?.lowercase() == "true"

    /** Стратегия разрешения конфликтов по умолчанию: last_write_wins | merge | manual */
    val defaultConflictStrategy: String
        get() = System.getenv("CLOUD_SYNC_CONFLICT_STRATEGY")?.uppercase() ?: "LAST_WRITE_WINS"

    /** Интервал фоновой синхронизации (секунды), 0 = отключено */
    val syncIntervalSec: Long
        get() = System.getenv("CLOUD_SYNC_INTERVAL_SEC")?.toLongOrNull()?.coerceIn(0, 86400) ?: 0L
}
