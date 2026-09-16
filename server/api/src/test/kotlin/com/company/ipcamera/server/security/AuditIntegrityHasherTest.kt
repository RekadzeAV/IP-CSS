package com.company.ipcamera.server.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AuditIntegrityHasherTest {

    @Test
    fun `computeHash is deterministic for same payload`() {
        val hash1 = AuditIntegrityHasher.computeHash(
            eventType = "LOGIN_SUCCESS",
            severity = "INFO",
            userId = "u-1",
            username = "operator",
            ipAddress = "127.0.0.1",
            userAgent = "ktor-test",
            detailsJson = """{"k":"v"}""",
            eventTimestamp = 1710000000000,
            createdAt = 1710000000001,
            previousHash = "abc"
        )

        val hash2 = AuditIntegrityHasher.computeHash(
            eventType = "LOGIN_SUCCESS",
            severity = "INFO",
            userId = "u-1",
            username = "operator",
            ipAddress = "127.0.0.1",
            userAgent = "ktor-test",
            detailsJson = """{"k":"v"}""",
            eventTimestamp = 1710000000000,
            createdAt = 1710000000001,
            previousHash = "abc"
        )

        assertEquals(hash1, hash2)
    }

    @Test
    fun `computeHash changes when previous hash changes`() {
        val base = AuditIntegrityHasher.computeHash(
            eventType = "LOGIN_FAILURE",
            severity = "WARNING",
            userId = null,
            username = "operator",
            ipAddress = "127.0.0.1",
            userAgent = null,
            detailsJson = """{"reason":"invalid"}""",
            eventTimestamp = 1710000000000,
            createdAt = 1710000000002,
            previousHash = "chain-1"
        )

        val changedPrevious = AuditIntegrityHasher.computeHash(
            eventType = "LOGIN_FAILURE",
            severity = "WARNING",
            userId = null,
            username = "operator",
            ipAddress = "127.0.0.1",
            userAgent = null,
            detailsJson = """{"reason":"invalid"}""",
            eventTimestamp = 1710000000000,
            createdAt = 1710000000002,
            previousHash = "chain-2"
        )

        assertNotEquals(base, changedPrevious)
    }
}
