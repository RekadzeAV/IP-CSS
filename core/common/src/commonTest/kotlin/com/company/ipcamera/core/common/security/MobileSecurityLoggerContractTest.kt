package com.company.ipcamera.core.common.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MobileSecurityLoggerContractTest {

    @Test
    fun helperMethodsDoNotInjectSecretKeys() {
        val logger = CollectingLogger()

        logger.logEncryptionFailure(dataType = "password", error = "operation failed")
        logger.logDecryptionFailure(dataType = "token", error = "invalid payload")

        val events = logger.events
        assertEquals(2, events.size)

        events.forEach { event ->
            assertFalse(event.details.containsKey("password"))
            assertFalse(event.details.containsKey("token"))
            assertFalse(event.details.containsKey("secret"))
            assertFalse(event.details.containsKey("private_key"))
        }
    }

    @Test
    fun suspiciousActivityKeepsProvidedDetailsAndDescription() {
        val logger = CollectingLogger()
        val extra = mapOf("camera_id" to "cam-01", "ip" to "192.168.1.10")

        logger.logSuspiciousActivity(description = "unexpected auth pattern", details = extra)

        val event = logger.events.single()
        assertEquals(MobileSecurityEventType.SUSPICIOUS_ACTIVITY, event.type)
        assertEquals("unexpected auth pattern", event.details["description"])
        assertEquals("cam-01", event.details["camera_id"])
        assertEquals("192.168.1.10", event.details["ip"])
        assertTrue(event.details.keys.containsAll(listOf("description", "camera_id", "ip")))
    }

    private class CollectingLogger : MobileSecurityLogger {
        val events = mutableListOf<MobileSecurityEvent>()

        override fun log(event: MobileSecurityEvent) {
            events += event
        }
    }
}
