package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.data.repository.CameraRepositoryImplTestHelper
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.test.TestDataFactory
import com.company.ipcamera.shared.test.TestDatabaseFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для CameraRepositoryImpl
 *
 * Примечание: Используется тестовый хелпер для обхода проблемы с expect классом DatabaseFactory
 */
class CameraRepositoryImplTest {

    private lateinit var repository: CameraRepository
    private lateinit var testDriver: app.cash.sqldelight.db.SqlDriver

    @BeforeTest
    fun setup() {
        testDriver = TestDatabaseFactory.createDriver()
        repository = CameraRepositoryImplTestHelper.createRepository(testDriver)
    }

    @AfterTest
    fun tearDown() {
        testDriver.close()
    }

    @Test
    fun `test add camera success`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera(
            id = "camera-1",
            name = "Test Camera"
        )

        // Act
        val result = repository.addCamera(camera)

        // Assert
        assertTrue(result.isSuccess)
        val addedCamera = result.getOrNull()
        assertNotNull(addedCamera)
        assertEquals("camera-1", addedCamera.id)
        assertEquals("Test Camera", addedCamera.name)
    }

    @Test
    fun `test get camera by id success`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera(id = "camera-1")
        repository.addCamera(camera)

        // Act
        val result = repository.getCameraById("camera-1")

        // Assert
        assertNotNull(result)
        assertEquals("camera-1", result?.id)
        assertEquals(camera.name, result?.name)
    }

    @Test
    fun `test get camera by id not found`() = runTest {
        // Act
        val result = repository.getCameraById("non-existent")

        // Assert
        assertNull(result)
    }

    @Test
    fun `test get all cameras`() = runTest {
        // Arrange
        val cameras = TestDataFactory.createTestCameras(3)
        cameras.forEach { repository.addCamera(it) }

        // Act
        val result = repository.getCameras()

        // Assert
        assertEquals(3, result.size)
        assertTrue(result.any { it.id == "test-camera-1" })
        assertTrue(result.any { it.id == "test-camera-2" })
        assertTrue(result.any { it.id == "test-camera-3" })
    }

    @Test
    fun `test get all cameras empty`() = runTest {
        // Act
        val result = repository.getCameras()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test update camera success`() = runTest {
        // Arrange
        val originalCamera = TestDataFactory.createTestCamera(
            id = "camera-1",
            name = "Original Name"
        )
        repository.addCamera(originalCamera)

        val updatedCamera = originalCamera.copy(
            name = "Updated Name",
            status = CameraStatus.OFFLINE
        )

        // Act
        val result = repository.updateCamera(updatedCamera)

        // Assert
        assertTrue(result.isSuccess)
        val updated = result.getOrNull()
        assertNotNull(updated)
        assertEquals("Updated Name", updated.name)
        assertEquals(CameraStatus.OFFLINE, updated.status)
        assertTrue(updated.updatedAt > originalCamera.updatedAt)

        // Verify in database
        val fromDb = repository.getCameraById("camera-1")
        assertNotNull(fromDb)
        assertEquals("Updated Name", fromDb.name)
        assertEquals(CameraStatus.OFFLINE, fromDb.status)
    }

    @Test
    fun `test remove camera success`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera(id = "camera-1")
        repository.addCamera(camera)

        // Act
        val result = repository.removeCamera("camera-1")

        // Assert
        assertTrue(result.isSuccess)

        // Verify removed
        val fromDb = repository.getCameraById("camera-1")
        assertNull(fromDb)
    }

    @Test
    fun `test remove non-existent camera`() = runTest {
        // Act
        val result = repository.removeCamera("non-existent")

        // Assert
        assertTrue(result.isSuccess) // SQLDelight delete doesn't fail if row doesn't exist
    }

    @Test
    fun `test get camera status`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera(
            id = "camera-1",
            status = CameraStatus.ONLINE
        )
        repository.addCamera(camera)

        // Act
        val status = repository.getCameraStatus("camera-1")

        // Assert
        assertEquals(CameraStatus.ONLINE, status)
    }

    @Test
    fun `test get camera status not found`() = runTest {
        // Act
        val status = repository.getCameraStatus("non-existent")

        // Assert
        assertEquals(CameraStatus.UNKNOWN, status)
    }

    @Test
    fun `test discover cameras`() = runTest {
        // Act
        val result = repository.discoverCameras()

        // Assert
        assertTrue(result.isEmpty()) // Currently returns empty list (not implemented)
    }

    @Test
    fun `test test connection`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera()

        // Act
        val result = repository.testConnection(camera)

        // Assert
        assertTrue(result is com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure)
        assertEquals("Not implemented", (result as com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure).error)
    }

    @Test
    fun `test add multiple cameras and retrieve`() = runTest {
        // Arrange
        val cameras = TestDataFactory.createTestCameras(5)

        // Act
        cameras.forEach { repository.addCamera(it) }
        val allCameras = repository.getCameras()

        // Assert
        assertEquals(5, allCameras.size)
        cameras.forEach { camera ->
            val found = repository.getCameraById(camera.id)
            assertNotNull(found)
            assertEquals(camera.name, found?.name)
        }
    }

    @Test
    fun `test camera with all fields`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera(
            id = "full-camera",
            name = "Full Camera",
            url = "rtsp://192.168.1.100:554/stream",
            username = "admin",
            password = "password123",
            model = "Model XYZ",
            status = CameraStatus.ONLINE,
            resolution = Resolution(3840, 2160),
            fps = 30,
            bitrate = 8192,
            codec = "H.265",
            audio = true,
            ptz = TestDataFactory.createTestPTZConfig(),
            streams = listOf(TestDataFactory.createTestStreamConfig()),
            statistics = TestDataFactory.createTestCameraStatistics()
        )

        // Act
        val addResult = repository.addCamera(camera)
        val retrieved = repository.getCameraById("full-camera")

        // Assert
        assertTrue(addResult.isSuccess)
        assertNotNull(retrieved)
        assertEquals("Full Camera", retrieved?.name)
        assertEquals("admin", retrieved?.username)
        assertEquals("password123", retrieved?.password)
        assertEquals("Model XYZ", retrieved?.model)
        assertEquals(CameraStatus.ONLINE, retrieved?.status)
        assertNotNull(retrieved?.resolution)
        assertEquals(3840, retrieved?.resolution?.width)
        assertEquals(2160, retrieved?.resolution?.height)
        assertEquals(30, retrieved?.fps)
        assertEquals(8192, retrieved?.bitrate)
        assertEquals("H.265", retrieved?.codec)
        assertTrue(retrieved?.audio == true)
        assertNotNull(retrieved?.ptz)
        assertTrue(retrieved?.streams?.isNotEmpty() == true)
        assertNotNull(retrieved?.statistics)
    }
}

