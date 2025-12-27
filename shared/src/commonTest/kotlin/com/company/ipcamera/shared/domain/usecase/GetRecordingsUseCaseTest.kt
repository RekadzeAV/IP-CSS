package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.test.MockRecordingRepository
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для GetRecordingsUseCase
 */
class GetRecordingsUseCaseTest {

    @Test
    fun `test get recordings success`() = runTest {
        // Arrange
        val recordings = TestDataFactory.createTestRecordings(5, "camera-1")
        val repository = MockRecordingRepository()
        recordings.forEach { repository.addRecordingDirectly(it) }
        val useCase = GetRecordingsUseCase(repository)

        // Act
        val result = useCase()

        // Assert
        assertEquals(5, result.items.size)
        assertEquals(5, result.total)
        assertEquals(1, result.page)
        assertFalse(result.hasMore)
    }

    @Test
    fun `test get recordings empty list`() = runTest {
        // Arrange
        val repository = MockRecordingRepository()
        val useCase = GetRecordingsUseCase(repository)

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.items.isEmpty())
        assertEquals(0, result.total)
        assertFalse(result.hasMore)
    }

    @Test
    fun `test get recordings filter by camera id`() = runTest {
        // Arrange
        val camera1Recordings = TestDataFactory.createTestRecordings(3, "camera-1")
        val camera2Recordings = TestDataFactory.createTestRecordings(2, "camera-2")
        val repository = MockRecordingRepository()
        camera1Recordings.forEach { repository.addRecordingDirectly(it) }
        camera2Recordings.forEach { repository.addRecordingDirectly(it) }
        val useCase = GetRecordingsUseCase(repository)

        // Act
        val result = useCase(cameraId = "camera-1")

        // Assert
        assertEquals(3, result.items.size)
        assertEquals(3, result.total)
        assertTrue(result.items.all { it.cameraId == "camera-1" })
    }

    @Test
    fun `test get recordings pagination`() = runTest {
        // Arrange
        val recordings = TestDataFactory.createTestRecordings(25, "camera-1")
        val repository = MockRecordingRepository()
        recordings.forEach { repository.addRecordingDirectly(it) }
        val useCase = GetRecordingsUseCase(repository)

        // Act
        val page1 = useCase(page = 1, limit = 10)
        val page2 = useCase(page = 2, limit = 10)
        val page3 = useCase(page = 3, limit = 10)

        // Assert
        assertEquals(10, page1.items.size)
        assertEquals(25, page1.total)
        assertTrue(page1.hasMore)

        assertEquals(10, page2.items.size)
        assertEquals(25, page2.total)
        assertTrue(page2.hasMore)

        assertEquals(5, page3.items.size)
        assertEquals(25, page3.total)
        assertFalse(page3.hasMore)
    }

    @Test
    fun `test get recordings filter by time range`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val hourAgo = now - 3600000
        val twoHoursAgo = now - 7200000
        val threeHoursAgo = now - 10800000

        val recording1 = TestDataFactory.createTestRecording(
            id = "rec-1",
            cameraId = "camera-1",
            startTime = threeHoursAgo,
            endTime = twoHoursAgo
        )
        val recording2 = TestDataFactory.createTestRecording(
            id = "rec-2",
            cameraId = "camera-1",
            startTime = hourAgo,
            endTime = now
        )
        val recording3 = TestDataFactory.createTestRecording(
            id = "rec-3",
            cameraId = "camera-1",
            startTime = now - 1000,
            endTime = null // активная запись
        )

        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording1)
        repository.addRecordingDirectly(recording2)
        repository.addRecordingDirectly(recording3)
        val useCase = GetRecordingsUseCase(repository)

        // Act - фильтр за последние 2 часа
        val result = useCase(startTime = twoHoursAgo, endTime = now)

        // Assert
        assertEquals(2, result.items.size)
        assertTrue(result.items.all { it.id == "rec-2" || it.id == "rec-3" })
    }

    @Test
    fun `test get recordings invalid page defaults to 1`() = runTest {
        // Arrange
        val recordings = TestDataFactory.createTestRecordings(5, "camera-1")
        val repository = MockRecordingRepository()
        recordings.forEach { repository.addRecordingDirectly(it) }
        val useCase = GetRecordingsUseCase(repository)

        // Act
        val result = useCase(page = 0)

        // Assert
        assertEquals(1, result.page) // Should default to page 1
    }

    @Test
    fun `test get recordings invalid limit defaults to 20`() = runTest {
        // Arrange
        val recordings = TestDataFactory.createTestRecordings(25, "camera-1")
        val repository = MockRecordingRepository()
        recordings.forEach { repository.addRecordingDirectly(it) }
        val useCase = GetRecordingsUseCase(repository)

        // Act
        val result = useCase(limit = 0)

        // Assert
        assertEquals(20, result.limit)
        assertEquals(20, result.items.size)
    }

    @Test
    fun `test get recordings limit exceeds max`() = runTest {
        // Arrange
        val recordings = TestDataFactory.createTestRecordings(150, "camera-1")
        val repository = MockRecordingRepository()
        recordings.forEach { repository.addRecordingDirectly(it) }
        val useCase = GetRecordingsUseCase(repository)

        // Act
        val result = useCase(limit = 200) // Max is 100

        // Assert
        assertEquals(100, result.limit)
        assertEquals(100, result.items.size)
    }

    @Test
    fun `test get recordings start time greater than end time throws exception`() = runTest {
        // Arrange
        val repository = MockRecordingRepository()
        val useCase = GetRecordingsUseCase(repository)
        val now = System.currentTimeMillis()
        val hourAgo = now - 3600000

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            useCase(startTime = now, endTime = hourAgo)
        }
    }

    @Test
    fun `test get recordings blank camera id ignored`() = runTest {
        // Arrange
        val recordings = TestDataFactory.createTestRecordings(3, "camera-1")
        val repository = MockRecordingRepository()
        recordings.forEach { repository.addRecordingDirectly(it) }
        val useCase = GetRecordingsUseCase(repository)

        // Act
        val result = useCase(cameraId = "")

        // Assert
        assertEquals(3, result.items.size) // Blank cameraId should be ignored
    }
}

