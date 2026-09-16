package com.company.ipcamera.server.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/**
 * PostgreSQL реализация AuditLogRepository.
 * Использует таблицу audit_log (миграция V4__Add_audit_log_table.sql).
 */
class PostgresAuditLogRepository(
    private val dataSource: DataSource
) : AuditLogRepository {

    override suspend fun append(event: SecurityEvent): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val originalAutoCommit = conn.autoCommit
            try {
                conn.autoCommit = false

                conn.createStatement().use { statement ->
                    // Serialize append operations to keep hash chain order deterministic.
                    statement.execute("SELECT pg_advisory_xact_lock(23042026)")
                }

                val previousHash = conn.prepareStatement(
                    "SELECT integrity_hash FROM audit_log ORDER BY id DESC LIMIT 1"
                ).use { ps ->
                    ps.executeQuery().use { rs ->
                        if (rs.next()) rs.getString("integrity_hash") else null
                    }
                }

                val detailsJson = json.encodeToString(event.details.mapValues { (_, value) -> value?.toString() })
                val createdAt = System.currentTimeMillis()
                val integrityHash = AuditIntegrityHasher.computeHash(
                    eventType = event.type.name,
                    severity = event.severity.name,
                    userId = event.userId,
                    username = event.username,
                    ipAddress = event.ipAddress,
                    userAgent = event.userAgent,
                    detailsJson = detailsJson,
                    eventTimestamp = event.timestamp,
                    createdAt = createdAt,
                    previousHash = previousHash
                )

                conn.prepareStatement(
                    """
                    INSERT INTO audit_log(
                        event_type, severity, user_id, username, ip_address, user_agent,
                        details_json, event_timestamp, created_at, previous_hash, integrity_hash
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { ps ->
                    ps.setString(1, event.type.name)
                    ps.setString(2, event.severity.name)
                    ps.setString(3, event.userId)
                    ps.setString(4, event.username)
                    ps.setString(5, event.ipAddress)
                    ps.setString(6, event.userAgent)
                    ps.setString(7, detailsJson)
                    ps.setLong(8, event.timestamp)
                    ps.setLong(9, createdAt)
                    ps.setString(10, previousHash)
                    ps.setString(11, integrityHash)
                    ps.executeUpdate()
                }

                conn.commit()
            } catch (e: Exception) {
                runCatching { conn.rollback() }
                throw e
            } finally {
                conn.autoCommit = originalAutoCommit
            }
        }
    }

    override suspend fun getRecent(
        limit: Int,
        offset: Int,
        type: SecurityEventType?,
        userId: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ): List<SecurityEvent> = withContext(Dispatchers.IO) {
        val (whereSql, params) = buildWhereClause(type, userId, fromTimestamp, toTimestamp)
        val sql = buildSelectSql(whereSql)

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                bindParameters(ps, params, limit, offset)
                parseAuditEvents(ps.executeQuery())
            }
        }
    }

    private fun buildWhereClause(
        type: SecurityEventType?,
        userId: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ): Pair<String, List<Any>> {
        val where = mutableListOf<String>()
        val params = mutableListOf<Any>()

        type?.let { where += "event_type = ?"; params += it.name }
        if (!userId.isNullOrBlank()) { where += "user_id = ?"; params += userId }
        fromTimestamp?.let { where += "event_timestamp >= ?"; params += it }
        toTimestamp?.let { where += "event_timestamp <= ?"; params += it }

        val whereSql = if (where.isNotEmpty()) where.joinToString(prefix = "WHERE ", separator = " AND ") else ""
        return whereSql to params
    }

    private fun buildSelectSql(whereSql: String): String = """
        SELECT event_type, severity, user_id, username, ip_address, user_agent, details_json, event_timestamp
        FROM audit_log
        $whereSql
        ORDER BY event_timestamp DESC
        LIMIT ? OFFSET ?
    """.trimIndent()

    private fun bindParameters(ps: java.sql.PreparedStatement, params: List<Any>, limit: Int, offset: Int) {
        var idx = 1
        params.forEach { value ->
            when (value) {
                is String -> ps.setString(idx++, value)
                is Long -> ps.setLong(idx++, value)
                else -> ps.setObject(idx++, value)
            }
        }
        ps.setInt(idx++, limit.coerceIn(1, 500))
        ps.setInt(idx, offset.coerceAtLeast(0))
    }

    private fun parseAuditEvents(rs: java.sql.ResultSet): List<SecurityEvent> {
        val events = mutableListOf<SecurityEvent>()
        while (rs.next()) {
            val parsedType = parseEventType(rs.getString("event_type")) ?: continue
            val parsedSeverity = parseSeverity(rs.getString("severity"))
            val details = parseDetails(rs.getString("details_json"))

            events += SecurityEvent(
                type = parsedType,
                severity = parsedSeverity,
                userId = rs.getString("user_id"),
                username = rs.getString("username"),
                ipAddress = rs.getString("ip_address"),
                userAgent = rs.getString("user_agent"),
                details = details,
                timestamp = rs.getLong("event_timestamp")
            )
        }
        return events
    }

    private fun parseEventType(typeName: String?): SecurityEventType? {
        return typeName?.let {
            runCatching { SecurityEventType.valueOf(it) }.getOrElse {
                logger.warn { "Unknown audit event type '$it', skipping row" }
                null
            }
        }
    }

    private fun parseSeverity(severityName: String?): SecurityEventSeverity {
        return severityName?.let {
            runCatching { SecurityEventSeverity.valueOf(it) }.getOrElse {
                logger.warn { "Unknown audit severity '$it', fallback to INFO" }
                SecurityEventSeverity.INFO
            }
        } ?: SecurityEventSeverity.INFO
    }

    private fun parseDetails(detailsRaw: String?): Map<String, Any?> {
        return (detailsRaw ?: "{}").let { raw ->
            runCatching {
                json.decodeFromString<Map<String, String>>(raw).mapValues { (_, v) -> v }
            }.getOrElse {
                logger.warn(it) { "Failed to parse audit details_json, using empty details" }
                emptyMap()
            }
        }
    }

    override suspend fun verifyIntegrityChain(): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                SELECT
                    event_type, severity, user_id, username, ip_address, user_agent,
                    details_json, event_timestamp, created_at, previous_hash, integrity_hash
                FROM audit_log
                WHERE integrity_hash IS NOT NULL
                ORDER BY id ASC
                """.trimIndent()
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    val records = mutableListOf<AuditIntegrityRecord>()
                    while (rs.next()) {
                        records += AuditIntegrityRecord(
                            eventType = rs.getString("event_type"),
                            severity = rs.getString("severity"),
                            userId = rs.getString("user_id"),
                            username = rs.getString("username"),
                            ipAddress = rs.getString("ip_address"),
                            userAgent = rs.getString("user_agent"),
                            detailsJson = rs.getString("details_json") ?: "{}",
                            eventTimestamp = rs.getLong("event_timestamp"),
                            createdAt = rs.getLong("created_at"),
                            previousHash = rs.getString("previous_hash"),
                            integrityHash = rs.getString("integrity_hash")
                        )
                    }
                    AuditIntegrityVerifier.verifyChain(records)
                }
            }
        }
    }
}

