package com.company.ipcamera.server.validation

import com.company.ipcamera.server.dto.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestValidatorBulkAndSettingsTest {

    // ---- validateUpdateSettingsRequest ----
    @Test
    fun `update settings accepts non-empty map`() {
        val res = RequestValidator.validateUpdateSettingsRequest(UpdateSettingsRequest(mapOf("k" to "v")))
        assertTrue(res is ValidationResult.Success)
    }

    @Test
    fun `update settings rejects empty map`() {
        val res = RequestValidator.validateUpdateSettingsRequest(UpdateSettingsRequest(emptyMap()))
        assertTrue(res is ValidationResult.Error)
        assertEquals("settings", (res as ValidationResult.Error).field)
    }

    @Test
    fun `update settings rejects blank key`() {
        val res = RequestValidator.validateUpdateSettingsRequest(UpdateSettingsRequest(mapOf("  " to "v")))
        assertTrue(res is ValidationResult.Error)
        assertEquals("settings", (res as ValidationResult.Error).field)
    }

    @Test
    fun `update settings rejects very long value`() {
        val res = RequestValidator.validateUpdateSettingsRequest(UpdateSettingsRequest(mapOf("k" to "v".repeat(10001))))
        assertTrue(res is ValidationResult.Error)
        assertEquals("settings", (res as ValidationResult.Error).field)
    }

    // ---- validateAcknowledgeEventsRequest ----
    @Test
    fun `acknowledge events accepts valid ids`() {
        val res = RequestValidator.validateAcknowledgeEventsRequest(AcknowledgeEventsRequest(listOf("e1", "e2")))
        assertTrue(res is ValidationResult.Success)
    }

    @Test
    fun `acknowledge events rejects empty ids`() {
        val res = RequestValidator.validateAcknowledgeEventsRequest(AcknowledgeEventsRequest(emptyList()))
        assertTrue(res is ValidationResult.Error)
        assertEquals("ids", (res as ValidationResult.Error).field)
    }

    @Test
    fun `acknowledge events rejects blank id`() {
        val res = RequestValidator.validateAcknowledgeEventsRequest(AcknowledgeEventsRequest(listOf("e1", " ")))
        assertTrue(res is ValidationResult.Error)
        assertEquals("ids", (res as ValidationResult.Error).field)
    }

    // ---- validateBulkDeleteRecordingsRequest ----
    @Test
    fun `bulk delete recordings accepts unique ids`() {
        val res = RequestValidator.validateBulkDeleteRecordingsRequest(BulkDeleteRecordingsRequest(listOf("r1", "r2")))
        assertTrue(res is ValidationResult.Success)
    }

    @Test
    fun `bulk delete recordings rejects empty`() {
        val res = RequestValidator.validateBulkDeleteRecordingsRequest(BulkDeleteRecordingsRequest(emptyList()))
        assertTrue(res is ValidationResult.Error)
        assertEquals("ids", (res as ValidationResult.Error).field)
    }

    @Test
    fun `bulk delete recordings rejects duplicates`() {
        val res = RequestValidator.validateBulkDeleteRecordingsRequest(BulkDeleteRecordingsRequest(listOf("r1", "r1")))
        assertTrue(res is ValidationResult.Error)
        assertEquals("ids", (res as ValidationResult.Error).field)
    }

    // ---- validateBulkExportRecordingsRequest ----
    @Test
    fun `bulk export accepts valid format and quality`() {
        val res = RequestValidator.validateBulkExportRecordingsRequest(
            BulkExportRecordingsRequest(listOf("r1"), format = "mp4", quality = "high")
        )
        assertTrue(res is ValidationResult.Success)
    }

    @Test
    fun `bulk export rejects invalid format`() {
        val res = RequestValidator.validateBulkExportRecordingsRequest(
            BulkExportRecordingsRequest(listOf("r1"), format = "exe", quality = "medium")
        )
        assertTrue(res is ValidationResult.Error)
        assertEquals("format", (res as ValidationResult.Error).field)
    }

    @Test
    fun `bulk export rejects invalid quality`() {
        val res = RequestValidator.validateBulkExportRecordingsRequest(
            BulkExportRecordingsRequest(listOf("r1"), format = "mp4", quality = "ultra-hd")
        )
        assertTrue(res is ValidationResult.Error)
        assertEquals("quality", (res as ValidationResult.Error).field)
    }

    @Test
    fun `bulk export rejects reversed time range`() {
        val res = RequestValidator.validateBulkExportRecordingsRequest(
            BulkExportRecordingsRequest(listOf("r1"), startTime = 2000L, endTime = 1000L)
        )
        assertTrue(res is ValidationResult.Error)
        assertEquals("startTime", (res as ValidationResult.Error).field)
    }

    @Test
    fun `bulk export rejects duration over 24h`() {
        val res = RequestValidator.validateBulkExportRecordingsRequest(
            BulkExportRecordingsRequest(listOf("r1"), startTime = 0L, endTime = 86400001L)
        )
        assertTrue(res is ValidationResult.Error)
        assertEquals("endTime", (res as ValidationResult.Error).field)
    }

    // ---- validateBulkDeleteCamerasRequest ----
    @Test
    fun `bulk delete cameras accepts unique ids`() {
        val res = RequestValidator.validateBulkDeleteCamerasRequest(BulkDeleteCamerasRequest(listOf("c1", "c2")))
        assertTrue(res is ValidationResult.Success)
    }

    @Test
    fun `bulk delete cameras rejects blank id`() {
        val res = RequestValidator.validateBulkDeleteCamerasRequest(BulkDeleteCamerasRequest(listOf("c1", " ")))
        assertTrue(res is ValidationResult.Error)
        assertEquals("ids", (res as ValidationResult.Error).field)
    }

    @Test
    fun `bulk delete cameras rejects duplicates`() {
        val res = RequestValidator.validateBulkDeleteCamerasRequest(BulkDeleteCamerasRequest(listOf("c1", "c1")))
        assertTrue(res is ValidationResult.Error)
        assertEquals("ids", (res as ValidationResult.Error).field)
    }

    // ---- validatePagination ----
    @Test
    fun `pagination accepts valid values`() {
        val res = RequestValidator.validatePagination(page = 1, limit = 20)
        assertTrue(res is ValidationResult.Success)
    }

    @Test
    fun `pagination accepts null defaults`() {
        val res = RequestValidator.validatePagination(page = null, limit = null)
        assertTrue(res is ValidationResult.Success)
    }

    @Test
    fun `pagination rejects page below 1`() {
        val res = RequestValidator.validatePagination(page = 0, limit = 20)
        assertTrue(res is ValidationResult.Error)
        assertEquals("page", (res as ValidationResult.Error).field)
    }

    @Test
    fun `pagination rejects limit above 100`() {
        val res = RequestValidator.validatePagination(page = 1, limit = 101)
        assertTrue(res is ValidationResult.Error)
        assertEquals("limit", (res as ValidationResult.Error).field)
    }
}