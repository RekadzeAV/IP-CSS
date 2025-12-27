package com.company.ipcamera.shared.data.local

import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.test.TestDataFactory
import kotlin.test.*

/**
 * Тесты для RecordingEntityMapper
 */
class RecordingEntityMapperTest {

    private val mapper = RecordingEntityMapper()

    @Test
    fun `test toDomain basic recording`() {
        // Arrange
        val dbRecording = com.company.ipcamera.shared.database.Recording(
            id = "recording-1",
            camera_id = "camera-1",
            camera_name = "Test Camera",
            start_time = 1000L,
            end_time = 2000L,
            duration = 1000L,
            file_path = "/recordings/recording-1.mp4",
            file_size = 1024000L,
            format = "MP4",
            quality = "HIGH",
            status = "COMPLETED",
            thumbnail_url = "/thumb.jpg",
            created_at = 1000L
        )

        // Act
        val domainRecording = mapper.toDomain(dbRecording)

        // Assert
        assertEquals("recording-1", domainRecording.id)
        assertEquals("camera-1", domainRecording.cameraId)
        assertEquals("Test Camera", domainRecording.cameraName)
        assertEquals(1000L, domainRecording.startTime)
        assertEquals(2000L, domainRecording.endTime)
        assertEquals(1000L, domainRecording.duration)
        assertEquals("/recordings/recording-1.mp4", domainRecording.filePath)
        assertEquals(1024000L, domainRecording.fileSize)
        assertEquals(RecordingFormat.MP4, domainRecording.format)
        assertEquals(Quality.HIGH, domainRecording.quality)
        assertEquals(RecordingStatus.COMPLETED, domainRecording.status)
    }

    @Test
    fun `test toDomain recording without end time`() {
        // Arrange
        val dbRecording = com.company.ipcamera.shared.database.Recording(
            id = "recording-2",
            camera_id = "camera-1",
            camera_name = null,
            start_time = 1000L,
            end_time = null,
            duration = 0L,
            file_path = null,
            file_size = null,
            format = "MP4",
            quality = "HIGH",
            status = "ACTIVE",
            thumbnail_url = null,
            created_at = 1000L
        )

        // Act
        val domainRecording = mapper.toDomain(dbRecording)

        // Assert
        assertNull(domainRecording.endTime)
        assertNull(domainRecording.filePath)
        assertNull(domainRecording.fileSize)
        assertEquals(RecordingStatus.ACTIVE, domainRecording.status)
    }

    @Test
    fun `test toDatabase basic recording`() {
        // Arrange
        val domainRecording = TestDataFactory.createTestRecording(
            id = "recording-1",
            cameraId = "camera-1"
        )

        // Act
        val dbRecording = mapper.toDatabase(domainRecording)

        // Assert
        assertEquals("recording-1", dbRecording.id)
        assertEquals("camera-1", dbRecording.camera_id)
        assertEquals("MP4", dbRecording.format)
        assertEquals("HIGH", dbRecording.quality)
    }

    @Test
    fun `test round trip conversion`() {
        // Arrange
        val originalRecording = TestDataFactory.createTestRecording(
            id = "recording-1",
            cameraId = "camera-1"
        )

        // Act
        val dbRecording = mapper.toDatabase(originalRecording)
        val convertedRecording = mapper.toDomain(dbRecording)

        // Assert
        assertEquals(originalRecording.id, convertedRecording.id)
        assertEquals(originalRecording.cameraId, convertedRecording.cameraId)
        assertEquals(originalRecording.format, convertedRecording.format)
        assertEquals(originalRecording.quality, convertedRecording.quality)
        assertEquals(originalRecording.status, convertedRecording.status)
    }

    @Test
    fun `test toDomain all recording formats`() {
        val formats = RecordingFormat.values()

        formats.forEach { format ->
            val dbRecording = com.company.ipcamera.shared.database.Recording(
                id = "rec-$format",
                camera_id = "camera-1",
                camera_name = null,
                start_time = 1000L,
                end_time = 2000L,
                duration = 1000L,
                file_path = null,
                file_size = null,
                format = format.name,
                quality = "HIGH",
                status = "COMPLETED",
                thumbnail_url = null,
                created_at = 1000L
            )

            val domainRecording = mapper.toDomain(dbRecording)
            assertEquals(format, domainRecording.format)
        }
    }

    @Test
    fun `test toDomain all recording statuses`() {
        val statuses = RecordingStatus.values()

        statuses.forEach { status ->
            val dbRecording = com.company.ipcamera.shared.database.Recording(
                id = "rec-$status",
                camera_id = "camera-1",
                camera_name = null,
                start_time = 1000L,
                end_time = if (status == RecordingStatus.COMPLETED) 2000L else null,
                duration = 1000L,
                file_path = null,
                file_size = null,
                format = "MP4",
                quality = "HIGH",
                status = status.name,
                thumbnail_url = null,
                created_at = 1000L
            )

            val domainRecording = mapper.toDomain(dbRecording)
            assertEquals(status, domainRecording.status)
        }
    }
}

