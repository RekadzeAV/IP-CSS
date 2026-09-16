package com.company.ipcamera.server.repository

import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Тесты ServerRecordingRepositorySqlDelight на in-memory SQLite.
 * Покрывают CRUD, фильтры (камера/время), пагинацию и URL-методы.
 */
class ServerRecordingRepositorySqlDelightTest {

    private fun createRepository(): ServerRecordingRepositorySqlDelight {
        // Используем createDatabaseSync (из shared, commonMain): корректно применяет схему/миграции.
        // Прямой CameraDatabase.Schema.create(driver) — async и создаёт гонку (схема не успевала примениться).
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val database = com.company.ipcamera.shared.data.local.createDatabaseSync(driver)
        return ServerRecordingRepositorySqlDelight(database)
    }

    private fun recording(
        id: String,
        cameraId: String = "cam-1",
        startTime: Long = 1000L,
        endTime: Long? = null,
        status: RecordingStatus = RecordingStatus.ACTIVE
    ) = Recording(
        id = id,
        cameraId = cameraId,
        cameraName = "Camera $cameraId",
        startTime = startTime,
        endTime = endTime,
        duration = (endTime ?: startTime + 60_000) - startTime,
        filePath = "/recordings/$id.mp4",
        fileSize = 1024L,
        codec = "H.264",
        format = RecordingFormat.MP4,
        quality = Quality.HIGH,
        status = status,
        createdAt = startTime
    )

    @Test
    fun `addRecording and getRecordingById roundtrip`() = runBlocking {
        val repo = createRepository()
        val rec = recording("rec-1")

        val added = repo.addRecording(rec)
        assertTrue(added.isSuccess)

        val loaded = repo.getRecordingById("rec-1")
        assertNotNull(loaded)
        assertEquals("rec-1", loaded.id)
        assertEquals("cam-1", loaded.cameraId)
        assertEquals(RecordingFormat.MP4, loaded.format)
        assertEquals(Quality.HIGH, loaded.quality)
        assertEquals(RecordingStatus.ACTIVE, loaded.status)
    }

    @Test
    fun `addRecording fails on duplicate id`() = runBlocking {
        val repo = createRepository()
        repo.addRecording(recording("dup"))
        val second = repo.addRecording(recording("dup", startTime = 9999L))
        assertTrue(second.isFailure)
        assertIs<IllegalArgumentException>(second.exceptionOrNull())
    }

    @Test
    fun `getRecordings paginates sorted by startTime desc`() = runBlocking {
        val repo = createRepository()
        repo.addRecording(recording("r1", startTime = 1000L))
        repo.addRecording(recording("r2", startTime = 3000L))
        repo.addRecording(recording("r3", startTime = 2000L))

        val page1 = repo.getRecordings(page = 1, limit = 2)
        assertEquals(3, page1.total)
        assertEquals(listOf("r2", "r3"), page1.items.map { it.id })
        assertTrue(page1.hasMore)

        val page2 = repo.getRecordings(page = 2, limit = 2)
        assertEquals(listOf("r1"), page2.items.map { it.id })
        assertFalse(page2.hasMore)
    }

    @Test
    fun `getRecordings filters by cameraId`() = runBlocking {
        val repo = createRepository()
        repo.addRecording(recording("c1", cameraId = "cam-1"))
        repo.addRecording(recording("c2", cameraId = "cam-2"))

        val result = repo.getRecordings(cameraId = "cam-2")
        assertEquals(1, result.total)
        assertEquals("c2", result.items.single().id)
    }

    @Test
    fun `getRecordings filters by time range`() = runBlocking {
        val repo = createRepository()
        repo.addRecording(recording("t1", startTime = 1000L, endTime = 2000L))
        repo.addRecording(recording("t2", startTime = 5000L, endTime = 6000L))

        val result = repo.getRecordings(startTime = 0L, endTime = 3000L)
        assertEquals(1, result.total)
        assertEquals("t1", result.items.single().id)
    }

    @Test
    fun `updateRecording persists changes`() = runBlocking {
        val repo = createRepository()
        repo.addRecording(recording("u1"))

        val original = repo.getRecordingById("u1")!!
        val updated = original.copy(status = RecordingStatus.COMPLETED, endTime = 9000L, fileSize = 2048L)
        val result = repo.updateRecording(updated)
        assertTrue(result.isSuccess)
        assertEquals(updated, result.getOrThrow())

        val loaded = repo.getRecordingById("u1")
        assertEquals(RecordingStatus.COMPLETED, loaded?.status)
        assertEquals(9000L, loaded?.endTime)
    }

    @Test
    fun `updateRecording fails for missing id`() = runBlocking {
        val repo = createRepository()
        val result = repo.updateRecording(recording("ghost"))
        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteRecording removes and fails second time`() = runBlocking {
        val repo = createRepository()
        repo.addRecording(recording("d1"))

        assertTrue(repo.deleteRecording("d1").isSuccess)
        assertNull(repo.getRecordingById("d1"))
        assertTrue(repo.deleteRecording("d1").isFailure)
    }

    @Test
    fun `getDownloadUrl requires existing recording with file`() = runBlocking {
        val repo = createRepository()
        repo.addRecording(recording("dl"))

        val ok = repo.getDownloadUrl("dl")
        assertTrue(ok.isSuccess)
        assertEquals("/api/v1/recordings/dl/download/file", ok.getOrThrow())

        assertTrue(repo.getDownloadUrl("missing").isFailure)
    }

    @Test
    fun `exportRecording returns export url`() = runBlocking {
        val repo = createRepository()
        repo.addRecording(recording("ex"))

        val result = repo.exportRecording("ex", "MP4", "HIGH")
        assertTrue(result.isSuccess)
        assertEquals("/api/v1/recordings/ex/export?format=MP4&quality=HIGH", result.getOrThrow())

        assertTrue(repo.exportRecording("missing", "MP4", "HIGH").isFailure)
    }
}
