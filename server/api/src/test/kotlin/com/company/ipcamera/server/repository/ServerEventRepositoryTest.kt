package com.company.ipcamera.server.repository

import com.company.ipcamera.shared.domain.model.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerEventRepositoryTest {

    private val repo = ServerEventRepository()

    private fun ev(
        id: String, cameraId: String = "cam-1", type: EventType = EventType.MOTION_DETECTION,
        severity: EventSeverity = EventSeverity.WARNING, acknowledged: Boolean = false, ts: Long = 1000L
    ) = Event(
        id = id, cameraId = cameraId, type = type, severity = severity,
        timestamp = ts, acknowledged = acknowledged
    )

    @Test
    fun `add and get by id`() = runBlocking {
        assertTrue(repo.addEvent(ev("e1")).isSuccess)
        assertEquals("e1", repo.getEventById("e1")!!.id)
        assertNull(repo.getEventById("missing"))
    }

    @Test
    fun `update event fields`() = runBlocking {
        repo.addEvent(ev("e1"))
        val updated = ev("e1").copy(acknowledged = true, acknowledgedBy = "admin")
        assertTrue(repo.updateEvent(updated).isSuccess)
        assertTrue(repo.getEventById("e1")!!.acknowledged)
        assertEquals("admin", repo.getEventById("e1")!!.acknowledgedBy)
    }

    @Test
    fun `acknowledge single event`() = runBlocking {
        repo.addEvent(ev("e1"))
        val res = repo.acknowledgeEvent("e1", "admin")
        assertTrue(res.isSuccess)
        assertTrue(res.getOrThrow().acknowledged)
    }

    @Test
    fun `acknowledge events list`() = runBlocking {
        repo.addEvent(ev("e1")); repo.addEvent(ev("e2")); repo.addEvent(ev("e3"))
        val res = repo.acknowledgeEvents(listOf("e1", "e2"), "operator")
        assertTrue(res.isSuccess)
        assertEquals(2, res.getOrThrow().size)
        assertTrue(res.getOrThrow().all { it.acknowledged })
    }

    @Test
    fun `acknowledge missing event fails`() = runBlocking {
        assertTrue(repo.acknowledgeEvent("ghost", "admin").isFailure)
    }

    @Test
    fun `delete event`() = runBlocking {
        repo.addEvent(ev("e1"))
        assertTrue(repo.deleteEvent("e1").isSuccess)
        assertNull(repo.getEventById("e1"))
    }

    @Test
    fun `getEvents filters combine and sort`() = runBlocking {
        repo.addEvent(ev("e1", cameraId = "cam-1", type = EventType.MOTION_DETECTION, severity = EventSeverity.WARNING, ts = 100L))
        repo.addEvent(ev("e2", cameraId = "cam-2", type = EventType.MOTION_DETECTION, severity = EventSeverity.ERROR, ts = 200L))
        repo.addEvent(ev("e3", cameraId = "cam-1", type = EventType.CAMERA_OFFLINE, severity = EventSeverity.ERROR, ts = 300L))
        repo.addEvent(ev("e4", cameraId = "cam-1", type = EventType.MOTION_DETECTION, severity = EventSeverity.ERROR, acknowledged = true, ts = 400L))

        // фильтр: cam-1 + MOTION_DETECTION + ERROR + не подтверждённое -> e3 не подходит (type), e4 подходит по type но acknowledged
        val r1 = repo.getEvents(
            type = EventType.MOTION_DETECTION, cameraId = "cam-1",
            severity = EventSeverity.ERROR, acknowledged = false,
            startTime = null, endTime = null, page = 1, limit = 20
        )
        assertEquals(0, r1.total) // e4 acknowledged=true -> исключено

        val r2 = repo.getEvents(null, null, EventSeverity.ERROR, null, null, null, 1, 20)
        assertEquals(3, r2.total) // e2, e3, e4
        // сортировка по ts убыв.
        assertEquals("e4", r2.items[0].id)
    }

    @Test
    fun `getEvents time range`() = runBlocking {
        repo.addEvent(ev("e1", ts = 100L)); repo.addEvent(ev("e2", ts = 200L)); repo.addEvent(ev("e3", ts = 300L))
        val r = repo.getEvents(null, null, null, null, startTime = 150L, endTime = 250L, page = 1, limit = 20)
        assertEquals(1, r.total)
        assertEquals("e2", r.items[0].id)
    }

    @Test
    fun `getEvents pagination`() = runBlocking {
        repeat(5) { repo.addEvent(ev("e$it", ts = it.toLong())) }
        val page1 = repo.getEvents(null, null, null, null, null, null, page = 1, limit = 2)
        assertEquals(2, page1.items.size)
        assertTrue(page1.hasMore)
        val page3 = repo.getEvents(null, null, null, null, null, null, page = 3, limit = 2)
        assertEquals(1, page3.items.size)
    }

    @Test
    fun `getEventStatistics returns counts`() = runBlocking {
        repo.addEvent(ev("e1")); repo.addEvent(ev("e2")); repo.addEvent(ev("e3"))
        val stats = repo.getEventStatistics(cameraId = null, startTime = null, endTime = null)
        assertTrue(stats.isSuccess)
        assertTrue(stats.getOrThrow().isNotEmpty())
    }
}