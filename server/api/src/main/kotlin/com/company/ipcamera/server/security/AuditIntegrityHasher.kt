package com.company.ipcamera.server.security

import java.security.MessageDigest

/**
 * Builds tamper-evident hash chain records for audit_log persistence.
 */
object AuditIntegrityHasher {

    fun computeHash(
        eventType: String,
        severity: String,
        userId: String?,
        username: String?,
        ipAddress: String?,
        userAgent: String?,
        detailsJson: String,
        eventTimestamp: Long,
        createdAt: Long,
        previousHash: String?
    ): String {
        val canonicalPayload = buildString {
            append(eventType).append('|')
            append(severity).append('|')
            append(userId.orEmpty()).append('|')
            append(username.orEmpty()).append('|')
            append(ipAddress.orEmpty()).append('|')
            append(userAgent.orEmpty()).append('|')
            append(detailsJson).append('|')
            append(eventTimestamp).append('|')
            append(createdAt).append('|')
            append(previousHash.orEmpty())
        }
        return sha256Hex(canonicalPayload)
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { b -> "%02x".format(b) }
    }
}
