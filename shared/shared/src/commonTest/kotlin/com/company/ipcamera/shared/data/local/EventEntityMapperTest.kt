package com.company.ipcamera.shared.data.local

import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.test.TestDataFactory
import kotlin.test.*

/**
 * Тесты для EventEntityMapper
 */
class EventEntityMapperTest {

    private val mapper = EventEntityMapper()

    @Test
    fun `test toDomain basic event`() {
        // Arrange
        val dbEvent = com.company.ipcamera.shared.database.Event(
            id = "event-1",
            camera_id = "camera-1",
            camera_name = "Test Camera",
            type = "MOTION_DETECTION",
            severity = "INFO",
            timestamp = 1000L,
            description = "Motion detected",
            metadata = null,
            acknowledged = 0L,
            acknowledged_at = null,
            acknowledged_by = null,
            thumbnail_url = null,
            video_url = null
        )

        // Act
        val domainEvent = mapper.toDomain(dbEvent)

        // Assert
        assertEquals("event-1", domainEvent.id)
        assertEquals("camera-1", domainEvent.cameraId)
        assertEquals("Test Camera", domainEvent.cameraName)
        assertEquals(EventType.MOTION_DETECTION, domainEvent.type)
        assertEquals(EventSeverity.INFO, domainEvent.severity)
        assertEquals(1000L, domainEvent.timestamp)
        assertEquals("Motion detected", domainEvent.description)
        assertFalse(domainEvent.acknowledged)
    }

    @Test
    fun `test toDomain event with metadata`() {
        // Arrange
        val metadataJson = """{"key1":"value1","key2":"value2"}"""
        val dbEvent = com.company.ipcamera.shared.database.Event(
            id = "event-2",
            camera_id = "camera-1",
            camera_name = null,
            type = "OBJECT_DETECTION",
            severity = "WARNING",
            timestamp = 2000L,
            description = null,
            metadata = metadataJson,
            acknowledged = 1L,
            acknowledged_at = 3000L,
            acknowledged_by = "user-1",
            thumbnail_url = "/thumb.jpg",
            video_url = "/video.mp4"
        )

        // Act
        val domainEvent = mapper.toDomain(dbEvent)

        // Assert
        assertEquals("event-2", domainEvent.id)
        assertTrue(domainEvent.metadata.isNotEmpty())
        assertEquals("value1", domainEvent.metadata["key1"])
        assertEquals("value2", domainEvent.metadata["key2"])
        assertTrue(domainEvent.acknowledged)
        assertEquals(3000L, domainEvent.acknowledgedAt)
        assertEquals("user-1", domainEvent.acknowledgedBy)
        assertEquals("/thumb.jpg", domainEvent.thumbnailUrl)
        assertEquals("/video.mp4", domainEvent.videoUrl)
    }

    @Test
    fun `test toDatabase basic event`() {
        // Arrange
        val domainEvent = TestDataFactory.createTestEvent(
            id = "event-1",
            cameraId = "camera-1",
            type = EventType.MOTION_DETECTION,
            severity = EventSeverity.INFO
        )

        // Act
        val dbEvent = mapper.toDatabase(domainEvent)

        // Assert
        assertEquals("event-1", dbEvent.id)
        assertEquals("camera-1", dbEvent.camera_id)
        assertEquals("MOTION_DETECTION", dbEvent.type)
        assertEquals("INFO", dbEvent.severity)
        assertEquals(0L, dbEvent.acknowledged)
    }

    @Test
    fun `test toDatabase event with metadata`() {
        // Arrange
        val domainEvent = TestDataFactory.createTestEvent(
            id = "event-2",
            metadata = mapOf("key1" to "value1", "key2" to "value2"),
            acknowledged = true
        )

        // Act
        val dbEvent = mapper.toDatabase(domainEvent)

        // Assert
        assertEquals(1L, dbEvent.acknowledged)
        assertNotNull(dbEvent.metadata)
        assertTrue(dbEvent.metadata!!.contains("key1"))
    }

    @Test
    fun `test round trip conversion`() {
        // Arrange
        val originalEvent = TestDataFactory.createTestEvent(
            id = "event-1",
            cameraId = "camera-1",
            type = EventType.FACE_DETECTION,
            severity = EventSeverity.CRITICAL,
            metadata = mapOf("test" to "value")
        )

        // Act
        val dbEvent = mapper.toDatabase(originalEvent)
        val convertedEvent = mapper.toDomain(dbEvent)

        // Assert
        assertEquals(originalEvent.id, convertedEvent.id)
        assertEquals(originalEvent.cameraId, convertedEvent.cameraId)
        assertEquals(originalEvent.type, convertedEvent.type)
        assertEquals(originalEvent.severity, convertedEvent.severity)
        assertEquals(originalEvent.metadata, convertedEvent.metadata)
    }

    @Test
    fun `test toDomain all event types`() {
        val eventTypes = EventType.values()

        eventTypes.forEach { eventType ->
            val dbEvent = com.company.ipcamera.shared.database.Event(
                id = "event-$eventType",
                camera_id = "camera-1",
                camera_name = null,
                type = eventType.name,
                severity = "INFO",
                timestamp = System.currentTimeMillis(),
                description = null,
                metadata = null,
                acknowledged = 0L,
                acknowledged_at = null,
                acknowledged_by = null,
                thumbnail_url = null,
                video_url = null
            )

            val domainEvent = mapper.toDomain(dbEvent)
            assertEquals(eventType, domainEvent.type)
        }
    }

    @Test
    fun `test toDomain all severity levels`() {
        val severities = EventSeverity.values()

        severities.forEach { severity ->
            val dbEvent = com.company.ipcamera.shared.database.Event(
                id = "event-$severity",
                camera_id = "camera-1",
                camera_name = null,
                type = "MOTION_DETECTION",
                severity = severity.name,
                timestamp = System.currentTimeMillis(),
                description = null,
                metadata = null,
                acknowledged = 0L,
                acknowledged_at = null,
                acknowledged_by = null,
                thumbnail_url = null,
                video_url = null
            )

            val domainEvent = mapper.toDomain(dbEvent)
            assertEquals(severity, domainEvent.severity)
        }
    }
}


