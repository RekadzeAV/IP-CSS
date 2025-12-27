package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.test.MockCameraRepository
import com.company.ipcamera.shared.test.MockRecordingRepository
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для StartRecordingUseCase
 */
class StartRecordingUseCaseTest {

    @Test
    fun `test start recording success`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera(
            id = "camera-1",
            status = CameraStatus.ONLINE
        )
        val cameraRepository = MockCameraRepository()
        cameraRepository.addCameraDirectly(camera)
        val recordingRepository = MockRecordingRepository()
        val useCase = StartRecordingUseCase(cameraRepository, recordingRepository)

        // Act
        val result = useCase(
            cameraId = "camera-1",
            format = RecordingFormat.MP4,
            quality = Quality.HIGH
        )

        // Assert
        assertTrue(result.isSuccess)
        val recording = result.getOrNull()
        assertNotNull(recording)
        assertEquals("camera-1", recording.cameraId)
        assertEquals(RecordingFormat.MP4, recording.format)
        assertEquals(Quality.HIGH, recording.quality)
        assertEquals(RecordingStatus.ACTIVE, recording.status)
        assertNotNull(recording.startTime)
        assertNull(recording.endTime)
        assertEquals(0, recording.duration)
    }

    @Test
    fun `test start recording with blank camera id fails`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        val recordingRepository = MockRecordingRepository()
        val useCase = StartRecordingUseCase(cameraRepository, recordingRepository)

        // Act
        val result = useCase(cameraId = "")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `test start recording camera not found fails`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        val recordingRepository = MockRecordingRepository()
        val useCase = StartRecordingUseCase(cameraRepository, recordingRepository)

        // Act
        val result = useCase(cameraId = "non-existent")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
    }

    @Test
    fun `test start recording camera offline fails`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera(
            id = "camera-1",
            status = CameraStatus.OFFLINE
        )
        val cameraRepository = MockCameraRepository()
        cameraRepository.addCameraDirectly(camera)
        val recordingRepository = MockRecordingRepository()
        val useCase = StartRecordingUseCase(cameraRepository, recordingRepository)

        // Act
        val result = useCase(cameraId = "camera-1")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(result.exceptionOrNull()?.message?.contains("not online") == true)
    }

    @Test
    fun `test start recording with negative duration fails`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera(
            id = "camera-1",
            status = CameraStatus.ONLINE
        )
        val cameraRepository = MockCameraRepository()
        cameraRepository.addCameraDirectly(camera)
        val recordingRepository = MockRecordingRepository()
        val useCase = StartRecordingUseCase(cameraRepository, recordingRepository)

        // Act
        val result = useCase(
            cameraId = "camera-1",
            duration = -1000L
        )

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("positive") == true)
    }

    @Test
    fun `test start recording with zero duration fails`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera(
            id = "camera-1",
            status = CameraStatus.ONLINE
        )
        val cameraRepository = MockCameraRepository()
        cameraRepository.addCameraDirectly(camera)
        val recordingRepository = MockRecordingRepository()
        val useCase = StartRecordingUseCase(cameraRepository, recordingRepository)

        // Act
        val result = useCase(
            cameraId = "camera-1",
            duration = 0L
        )

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `test start recording repository failure`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera(
            id = "camera-1",
            status = CameraStatus.ONLINE
        )
        val cameraRepository = MockCameraRepository()
        cameraRepository.addCameraDirectly(camera)
        val recordingRepository = MockRecordingRepository()
        recordingRepository.shouldFailOnAdd = true
        recordingRepository.addError = Exception("Database error")
        val useCase = StartRecordingUseCase(cameraRepository, recordingRepository)

        // Act
        val result = useCase(cameraId = "camera-1")

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `test start recording with different formats`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera(
            id = "camera-1",
            status = CameraStatus.ONLINE
        )
        val cameraRepository = MockCameraRepository()
        cameraRepository.addCameraDirectly(camera)
        val recordingRepository = MockRecordingRepository()
        val useCase = StartRecordingUseCase(cameraRepository, recordingRepository)

        val formats = listOf(RecordingFormat.MP4, RecordingFormat.MKV, RecordingFormat.AVI)

        formats.forEach { format ->
            // Act
            val result = useCase(
                cameraId = "camera-1",
                format = format
            )

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(format, result.getOrNull()?.format)
        }
    }

    @Test
    fun `test start recording with different qualities`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera(
            id = "camera-1",
            status = CameraStatus.ONLINE
        )
        val cameraRepository = MockCameraRepository()
        cameraRepository.addCameraDirectly(camera)
        val recordingRepository = MockRecordingRepository()
        val useCase = StartRecordingUseCase(cameraRepository, recordingRepository)

        val qualities = listOf(Quality.LOW, Quality.MEDIUM, Quality.HIGH, Quality.ULTRA)

        qualities.forEach { quality ->
            // Act
            val result = useCase(
                cameraId = "camera-1",
                quality = quality
            )

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(quality, result.getOrNull()?.quality)
        }
    }
}


