package com.company.ipcamera.server.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebhookQueryValidatorTest {

    @Test
    fun `parseSuccess accepts true false null`() {
        assertEquals(true, WebhookQueryValidator.parseSuccess("true"))
        assertEquals(false, WebhookQueryValidator.parseSuccess("false"))
        assertNull(WebhookQueryValidator.parseSuccess(null))
    }

    @Test
    fun `parseSuccess rejects invalid value`() {
        val ex = runCatching { WebhookQueryValidator.parseSuccess("yes") }.exceptionOrNull()
        assertTrue(ex is IllegalArgumentException)
    }

    @Test
    fun `parseTimeRange parses valid and rejects invalid`() {
        assertEquals(100L to 200L, WebhookQueryValidator.parseTimeRange("100", "200"))
        assertEquals(null to 200L, WebhookQueryValidator.parseTimeRange(null, "200"))
        assertEquals(100L to null, WebhookQueryValidator.parseTimeRange("100", null))

        assertTrue(runCatching { WebhookQueryValidator.parseTimeRange("a", "200") }.isFailure)
        assertTrue(runCatching { WebhookQueryValidator.parseTimeRange("100", "b") }.isFailure)
        assertTrue(runCatching { WebhookQueryValidator.parseTimeRange("200", "100") }.isFailure)
    }

    @Test
    fun `parsePage validates bounds and default`() {
        assertEquals(1, WebhookQueryValidator.parsePage(null))
        assertEquals(2, WebhookQueryValidator.parsePage("2"))
        assertTrue(runCatching { WebhookQueryValidator.parsePage("0") }.isFailure)
        assertTrue(runCatching { WebhookQueryValidator.parsePage("x") }.isFailure)
    }

    @Test
    fun `parseLimit validates bounds and default`() {
        assertEquals(100, WebhookQueryValidator.parseLimit(null))
        assertEquals(1, WebhookQueryValidator.parseLimit("1"))
        assertEquals(500, WebhookQueryValidator.parseLimit("500"))
        assertTrue(runCatching { WebhookQueryValidator.parseLimit("0") }.isFailure)
        assertTrue(runCatching { WebhookQueryValidator.parseLimit("501") }.isFailure)
        assertTrue(runCatching { WebhookQueryValidator.parseLimit("x") }.isFailure)
    }
}
