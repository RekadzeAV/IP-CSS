package com.company.ipcamera.server.security

data class AuditIntegrityRecord(
    val eventType: String,
    val severity: String,
    val userId: String?,
    val username: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val detailsJson: String,
    val eventTimestamp: Long,
    val createdAt: Long,
    val previousHash: String?,
    val integrityHash: String?
)

object AuditIntegrityVerifier {

    fun verifyChain(records: List<AuditIntegrityRecord>): Boolean {
        var expectedPrevious: String? = null
        for (record in records) {
            if (record.previousHash != expectedPrevious) {
                return false
            }
            val expectedHash = AuditIntegrityHasher.computeHash(
                eventType = record.eventType,
                severity = record.severity,
                userId = record.userId,
                username = record.username,
                ipAddress = record.ipAddress,
                userAgent = record.userAgent,
                detailsJson = record.detailsJson,
                eventTimestamp = record.eventTimestamp,
                createdAt = record.createdAt,
                previousHash = record.previousHash
            )
            if (record.integrityHash != expectedHash) {
                return false
            }
            expectedPrevious = record.integrityHash
        }
        return true
    }
}
