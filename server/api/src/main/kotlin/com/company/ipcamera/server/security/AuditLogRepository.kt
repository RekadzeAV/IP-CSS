package com.company.ipcamera.server.security

/**
 * Репозиторий для сохранения событий аудита (4.3.3.1).
 * При AUDIT_PERSIST_ENABLED=true SecurityLogger может дублировать события сюда.
 */
interface AuditLogRepository {

    /**
     * Сохранить событие безопасности в постоянное хранилище (БД, файл).
     */
    suspend fun append(event: SecurityEvent)

    /**
     * Получить последние события с пагинацией (для админки/мониторинга).
     */
    suspend fun getRecent(
        limit: Int = 100,
        offset: Int = 0,
        type: SecurityEventType? = null,
        userId: String? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null
    ): List<SecurityEvent>

    /**
     * Проверить целостность цепочки tamper-evident хешей.
     * Для реализаций без хеш-цепочки допускается возвращать true.
     */
    suspend fun verifyIntegrityChain(): Boolean
}
