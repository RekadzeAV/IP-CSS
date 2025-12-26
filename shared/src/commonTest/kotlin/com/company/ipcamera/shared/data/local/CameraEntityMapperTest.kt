package com.company.ipcamera.shared.data.local

import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.domain.model.*
import com.company.ipcamera.shared.test.TestDataFactory
import kotlin.test.*

/**
 * Тесты для CameraEntityMapper
 */
class CameraEntityMapperTest {

    private val mapper = CameraEntityMapper()

    @Test
    fun `test toDomain basic camera`() {
        // Arrange
        val dbCamera = com.company.ipcamera.shared.database.Camera(
            id = "camera-1",
            name = "Test Camera",
            url = "rtsp://192.168.1.100:554/stream",
            username = "admin",
            password = "password",
            model = "Test Model",
            status = "ONLINE",
            resolution_width = 1920L,
            resolution_height = 1080L,
            fps = 25L,
            bitrate = 4096L,
            codec = "H.264",
            audio = 1L,
            ptz_config = null,
            streams = null,
            settings = "{}",
            statistics = null,
            created_at = 1000L,
            updated_at = 2000L,
            last_seen = 3000L
        )

        // Act
        val domainCamera = mapper.toDomain(dbCamera)

        // Assert
        assertEquals("camera-1", domainCamera.id)
        assertEquals("Test Camera", domainCamera.name)
        assertEquals("rtsp://192.168.1.100:554/stream", domainCamera.url)
        assertEquals("admin", domainCamera.username)
        assertEquals("password", domainCamera.password)
        assertEquals("Test Model", domainCamera.model)
        assertEquals(CameraStatus.ONLINE, domainCamera.status)
        assertNotNull(domainCamera.resolution)
        assertEquals(1920, domainCamera.resolution?.width)
        assertEquals(1080, domainCamera.resolution?.height)
        assertEquals(25, domainCamera.fps)
        assertEquals(4096, domainCamera.bitrate)
        assertEquals("H.264", domainCamera.codec)
        assertTrue(domainCamera.audio)
        assertEquals(1000L, domainCamera.createdAt)
        assertEquals(2000L, domainCamera.updatedAt)
        assertEquals(3000L, domainCamera.lastSeen)
    }

    @Test
    fun `test toDomain camera without optional fields`() {
        // Arrange
        val dbCamera = com.company.ipcamera.shared.database.Camera(
            id = "camera-2",
            name = "Minimal Camera",
            url = "rtsp://192.168.1.101:554/stream",
            username = null,
            password = null,
            model = null,
            status = "OFFLINE",
            resolution_width = null,
            resolution_height = null,
            fps = 30L,
            bitrate = 2048L,
            codec = "H.265",
            audio = 0L,
            ptz_config = null,
            streams = null,
            settings = "{}",
            statistics = null,
            created_at = 1000L,
            updated_at = 1000L,
            last_seen = null
        )

        // Act
        val domainCamera = mapper.toDomain(dbCamera)

        // Assert
        assertEquals("camera-2", domainCamera.id)
        assertNull(domainCamera.username)
        assertNull(domainCamera.password)
        assertNull(domainCamera.model)
        assertEquals(CameraStatus.OFFLINE, domainCamera.status)
        assertNull(domainCamera.resolution)
        assertFalse(domainCamera.audio)
        assertNull(domainCamera.lastSeen)
    }

    @Test
    fun `test toDatabase basic camera`() {
        // Arrange
        val domainCamera = TestDataFactory.createTestCamera(
            id = "camera-1",
            name = "Test Camera",
            resolution = Resolution(1920, 1080),
            audio = true
        )

        // Act
        val dbCamera = mapper.toDatabase(domainCamera)

        // Assert
        assertEquals("camera-1", dbCamera.id)
        assertEquals("Test Camera", dbCamera.name)
        assertEquals(1920L, dbCamera.resolution_width)
        assertEquals(1080L, dbCamera.resolution_height)
        assertEquals(1L, dbCamera.audio)
        assertEquals(domainCamera.status.name, dbCamera.status)
    }

    @Test
    fun `test toDatabase camera without audio`() {
        // Arrange
        val domainCamera = TestDataFactory.createTestCamera(
            id = "camera-2",
            audio = false
        )

        // Act
        val dbCamera = mapper.toDatabase(domainCamera)

        // Assert
        assertEquals(0L, dbCamera.audio)
    }

    @Test
    fun `test round trip conversion`() {
        // Arrange
        val originalCamera = TestDataFactory.createTestCamera(
            id = "camera-1",
            name = "Round Trip Camera",
            ptz = TestDataFactory.createTestPTZConfig(),
            streams = listOf(TestDataFactory.createTestStreamConfig()),
            statistics = TestDataFactory.createTestCameraStatistics()
        )

        // Act
        val dbCamera = mapper.toDatabase(originalCamera)
        val convertedCamera = mapper.toDomain(dbCamera)

        // Assert
        assertEquals(originalCamera.id, convertedCamera.id)
        assertEquals(originalCamera.name, convertedCamera.name)
        assertEquals(originalCamera.url, convertedCamera.url)
        assertEquals(originalCamera.status, convertedCamera.status)
        assertEquals(originalCamera.resolution, convertedCamera.resolution)
        assertEquals(originalCamera.fps, convertedCamera.fps)
        assertEquals(originalCamera.bitrate, convertedCamera.bitrate)
        assertEquals(originalCamera.codec, convertedCamera.codec)
        assertEquals(originalCamera.audio, convertedCamera.audio)
    }

    @Test
    fun `test toDomain with PTZ config`() {
        // Arrange
        val ptzConfig = TestDataFactory.createTestPTZConfig()
        val ptzJson = kotlinx.serialization.json.Json.encodeToString(PTZConfig.serializer(), ptzConfig)

        val dbCamera = com.company.ipcamera.shared.database.Camera(
            id = "camera-ptz",
            name = "PTZ Camera",
            url = "rtsp://192.168.1.100:554/stream",
            username = null,
            password = null,
            model = null,
            status = "ONLINE",
            resolution_width = null,
            resolution_height = null,
            fps = 25L,
            bitrate = 4096L,
            codec = "H.264",
            audio = 0L,
            ptz_config = ptzJson,
            streams = null,
            settings = "{}",
            statistics = null,
            created_at = 1000L,
            updated_at = 1000L,
            last_seen = null
        )

        // Act
        val domainCamera = mapper.toDomain(dbCamera)

        // Assert
        assertNotNull(domainCamera.ptz)
        assertEquals(ptzConfig.enabled, domainCamera.ptz?.enabled)
        assertEquals(ptzConfig.type, domainCamera.ptz?.type)
    }
}


