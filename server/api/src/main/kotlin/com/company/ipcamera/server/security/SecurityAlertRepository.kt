package com.company.ipcamera.server.security

/**
 * Репозиторий алертов безопасности (4.3.3.2).
 */
interface SecurityAlertRepository {

    suspend fun save(alert: SecurityAlert)

    suspend fun getById(id: String): SecurityAlert?

    suspend fun getActive(limit: Int = 50): List<SecurityAlert>

    suspend fun list(
        limit: Int = 50,
        offset: Int = 0,
        resolved: Boolean? = null,
        type: SecurityAlertType? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null
    ): List<SecurityAlert>

    suspend fun resolve(id: String): Boolean
}
