package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.test.MockRecordingRepository
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для DeleteRecordingUseCase
 */
class DeleteRecordingUseCaseTest {

    @Test
    fun `test delete recording success`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(id = "recording-1")
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        val useCase = DeleteRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isSuccess)
        assertNull(repository.getRecordingById("recording-1"))
    }

    @Test
    fun `test delete recording with blank id fails`() = runTest {
        // Arrange
        val repository = MockRecordingRepository()
        val useCase = DeleteRecordingUseCase(repository)

        // Act
        val result = useCase("")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `test delete recording not found fails`() = runTest {
        // Arrange
        val repository = MockRecordingRepository()
        val useCase = DeleteRecordingUseCase(repository)

        // Act
        val result = useCase("non-existent")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
    }

    @Test
    fun `test delete recording repository failure`() = runTest {
        // Arrange
        val recording = TestDataFactory.createTestRecording(id = "recording-1")
        val repository = MockRecordingRepository()
        repository.addRecordingDirectly(recording)
        repository.shouldFailOnDelete = true
        repository.deleteError = Exception("Delete failed")
        val useCase = DeleteRecordingUseCase(repository)

        // Act
        val result = useCase("recording-1")

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Delete failed", result.exceptionOrNull()?.message)
    }
}


