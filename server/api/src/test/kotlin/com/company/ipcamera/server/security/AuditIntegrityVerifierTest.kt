package com.company.ipcamera.server.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuditIntegrityVerifierTest {

    @Test
    fun `verifyChain returns true for valid chain`() {
        val first = buildRecord(
            eventType = "LOGIN_SUCCESS",
            severity = "INFO",
            detailsJson = """{"a":"1"}""",
            eventTimestamp = 1L,
            createdAt = 2L,
            previousHash = null
        )
        val second = buildRecord(
            eventType = "LOGIN_FAILURE",
            severity = "WARNING",
            detailsJson = """{"a":"2"}""",
            eventTimestamp = 3L,
            createdAt = 4L,
            previousHash = first.integrityHash
        )

        assertTrue(AuditIntegrityVerifier.verifyChain(listOf(first, second)))
    }

    @Test
    fun `verifyChain returns false for broken previous hash link`() {
        val first = buildRecord(
            eventType = "LOGIN_SUCCESS",
            severity = "INFO",
            detailsJson = """{"a":"1"}""",
            eventTimestamp = 1L,
            createdAt = 2L,
            previousHash = null
        )
        val second = buildRecord(
            eventType = "LOGIN_FAILURE",
            severity = "WARNING",
            detailsJson = """{"a":"2"}""",
            eventTimestamp = 3L,
            createdAt = 4L,
            previousHash = "bad-link"
        )

        assertFalse(AuditIntegrityVerifier.verifyChain(listOf(first, second)))
    }

    @Test
    fun `verifyChain returns false for tampered payload`() {
        val original = buildRecord(
            eventType = "DATA_ACCESS",
            severity = "INFO",
            detailsJson = """{"resource":"cameras"}""",
            eventTimestamp = 10L,
            createdAt = 11L,
            previousHash = null
        )

        val tampered = original.copy(detailsJson = """{"resource":"users"}""")
        assertFalse(AuditIntegrityVerifier.verifyChain(listOf(tampered)))
    }

    private fun buildRecord(
        eventType: String,
        severity: String,
        detailsJson: String,
        eventTimestamp: Long,
        createdAt: Long,
        previousHash: String?
    ): AuditIntegrityRecord {
        val integrityHash = AuditIntegrityHasher.computeHash(
            eventType = eventType,
            severity = severity,
            userId = null,
            username = null,
            ipAddress = null,
            userAgent = null,
            detailsJson = detailsJson,
            eventTimestamp = eventTimestamp,
            createdAt = createdAt,
            previousHash = previousHash
        )
        return AuditIntegrityRecord(
            eventType = eventType,
            severity = severity,
            userId = null,
            username = null,
            ipAddress = null,
            userAgent = null,
            detailsJson = detailsJson,
            eventTimestamp = eventTimestamp,
            createdAt = createdAt,
            previousHash = previousHash,
            integrityHash = integrityHash
        )
    }
}
