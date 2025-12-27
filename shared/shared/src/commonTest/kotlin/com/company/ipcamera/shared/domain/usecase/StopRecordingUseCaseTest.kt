package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.test.MockRecordingRepository
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для StopRecordingUseCase
 */
class StopRecordingUseCaseTest {

    @Test
    fun `test stop recording success`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            status = RecordingStatus.ACTIVE
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        val useCase = StopRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isSuccess)
        val stoppedRecording = result.getOrNull()
        assertNotNull(stoppedRecording)
        assertEquals(RecordingStatus.COMPLETED, stoppedRecording.status)
        assertNotNull(stoppedRecording.endTime)
        assertTrue(stoppedRecording.duration > 0)
        assertTrue(stoppedRecording.endTime!! >= stoppedRecording.startTime)
    }

    @Test
    fun `test stop recording with blank id fails`() = runTest {
        // Arrange
        val repository = MockRecordingRepository()
        val useCase = StopRecordingUseCase(repository)

        // Act
        val result = useCase("")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `test stop recording not found fails`() = runTest {
        // Arrange
        val repository = MockRecordingRepository()
        val useCase = StopRecordingUseCase(repository)

        // Act
        val result = useCase("non-existent")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
    }

    @Test
    fun `test stop recording already completed fails`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            status = RecordingStatus.COMPLETED
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        val useCase = StopRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot be stopped") == true)
    }

    @Test
    fun `test stop recording paused fails`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            status = RecordingStatus.PAUSED
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        val useCase = StopRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `test stop recording repository failure`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            status = RecordingStatus.ACTIVE
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        repository.shouldFailOnUpdate = true
        repository.updateError = Exception("Update failed")
        val useCase = StopRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Update failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `test stop recording calculates duration correctly`() = runTest {
        // Arrange
        val startTime = System.currentTimeMillis() - 3600000 // 1 hour ago
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            startTime = startTime,
            status = RecordingStatus.ACTIVE
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        val useCase = StopRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isSuccess)
        val stoppedRecording = result.getOrNull()
        assertNotNull(stoppedRecording)
        // Duration should be approximately 1 hour (3600000 ms)
        assertTrue(stoppedRecording.duration >= 3590000) // Allow 10 seconds tolerance
        assertTrue(stoppedRecording.duration <= 3610000)
    }
}


