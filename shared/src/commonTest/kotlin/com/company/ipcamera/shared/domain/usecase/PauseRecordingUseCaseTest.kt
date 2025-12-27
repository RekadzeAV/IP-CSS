package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.test.MockRecordingRepository
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для PauseRecordingUseCase
 */
class PauseRecordingUseCaseTest {

    @Test
    fun `test pause recording success`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            status = RecordingStatus.ACTIVE
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        val useCase = PauseRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isSuccess)
        val pausedRecording = result.getOrNull()
        assertNotNull(pausedRecording)
        assertEquals(RecordingStatus.PAUSED, pausedRecording.status)
    }

    @Test
    fun `test pause recording not found fails`() = runTest {
        // Arrange
        val repository = MockRecordingRepository()
        val useCase = PauseRecordingUseCase(repository)

        // Act
        val result = useCase("non-existent")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
    }

    @Test
    fun `test pause recording already paused fails`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            status = RecordingStatus.PAUSED
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        val useCase = PauseRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(result.exceptionOrNull()?.message?.contains("not active") == true)
    }

    @Test
    fun `test pause recording completed fails`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            status = RecordingStatus.COMPLETED
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        val useCase = PauseRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `test pause recording repository failure`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            status = RecordingStatus.ACTIVE
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        repository.shouldFailOnUpdate = true
        repository.updateError = Exception("Update failed")
        val useCase = PauseRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Update failed", result.exceptionOrNull()?.message)
    }
}


