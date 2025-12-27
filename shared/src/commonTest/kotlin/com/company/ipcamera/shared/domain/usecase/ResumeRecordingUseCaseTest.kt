package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.test.MockRecordingRepository
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для ResumeRecordingUseCase
 */
class ResumeRecordingUseCaseTest {

    @Test
    fun `test resume recording success`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            status = RecordingStatus.PAUSED
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        val useCase = ResumeRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isSuccess)
        val resumedRecording = result.getOrNull()
        assertNotNull(resumedRecording)
        assertEquals(RecordingStatus.ACTIVE, resumedRecording.status)
    }

    @Test
    fun `test resume recording not found fails`() = runTest {
        // Arrange
        val repository = MockRecordingRepository()
        val useCase = ResumeRecordingUseCase(repository)

        // Act
        val result = useCase("non-existent")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
    }

    @Test
    fun `test resume recording not paused fails`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            status = RecordingStatus.ACTIVE
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        val useCase = ResumeRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(result.exceptionOrNull()?.message?.contains("not paused") == true)
    }

    @Test
    fun `test resume recording completed fails`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            status = RecordingStatus.COMPLETED
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        val useCase = ResumeRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `test resume recording repository failure`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(
            id = "recording-1",
            status = RecordingStatus.PAUSED
        )
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        repository.shouldFailOnUpdate = true
        repository.updateError = Exception("Update failed")
        val useCase = ResumeRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Update failed", result.exceptionOrNull()?.message)
    }
}


