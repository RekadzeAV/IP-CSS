package com.company.ipcamera.server.sync

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Разрешение конфликтов при синхронизации (4.1.1.2).
 */
object ConflictResolver {

    /**
     * Выбрать версию по стратегии last-write-wins (больше updatedAt побеждает).
     */
    fun resolveLastWriteWins(local: SyncResourceVersion, remote: SyncResourceVersion): SyncResourceVersion =
        if (remote.updatedAt >= local.updatedAt) remote else local

    /**
     * Для merge возвращаем «новейшую» версию по времени; при равенстве — remote.
     * Специфичная логика слияния (например для настроек — объединение ключей) выносится в вызывающий код.
     */
    fun resolveMerge(local: SyncResourceVersion, remote: SyncResourceVersion): SyncResourceVersion =
        if (remote.updatedAt >= local.updatedAt) remote else local
}
