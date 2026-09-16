package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.test.MockRecordingLocalDataSource
import com.company.ipcamera.shared.test.MockRecordingRemoteDataSource
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для RecordingRepositoryImpl
 */
class RecordingRepositoryImplTest {
    private lateinit var localDataSource: MockRecordingLocalDataSource
    private lateinit var remoteDataSource: MockRecordingRemoteDataSource
    private lateinit var repository: RecordingRepositoryImpl

    @BeforeTest
    fun setup() {
        localDataSource = MockRecordingLocalDataSource()
        remoteDataSource = MockRecordingRemoteDataSource()
        repository = RecordingRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun `test getRecordings with pagination`() =
        runTest {
            val recordings = TestDataFactory.createTestRecordings(10, "camera-1")
            recordings.forEach { localDataSource.addRecordingDirectly(it) }

            val result =
                repository.getRecordings(
                    cameraId = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 5,
                )

            assertEquals(5, result.items.size)
        }

    @Test
    fun `test getRecordings with filtering by cameraId`() =
        runTest {
            val camera1Recordings = TestDataFactory.createTestRecordings(3, "camera-1")
            val camera2Recordings = TestDataFactory.createTestRecordings(2, "camera-2")
            camera1Recordings.forEach { localDataSource.addRecordingDirectly(it) }
            camera2Recordings.forEach { localDataSource.addRecordingDirectly(it) }

            val result =
                repository.getRecordings(
                    cameraId = "camera-1",
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 20,
                )

            assertEquals(3, result.items.size)
            assertTrue(result.items.all { it.cameraId == "camera-1" })
        }

    @Test
    fun `test getRecordings uses local-first strategy`() =
        runTest {
            val localRecording = TestDataFactory.createTestRecording(id = "local-1", cameraId = "camera-1")
            val remoteRecording = TestDataFactory.createTestRecording(id = "remote-1", cameraId = "camera-1")

            localDataSource.addRecordingDirectly(localRecording)
            remoteDataSource.addRecordingDirectly(remoteRecording)

            val result =
                repository.getRecordings(
                    cameraId = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 20,
                )

            assertEquals(1, result.items.size)
            assertEquals("local-1", result.items[0].id)
        }

    @Test
    fun `test getRecordings with remote fallback when local is empty`() =
        runTest {
            val remoteRecording = TestDataFactory.createTestRecording(id = "remote-1", cameraId = "camera-1")
            remoteDataSource.addRecordingDirectly(remoteRecording)

            val result =
                repository.getRecordings(
                    cameraId = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 20,
                )

            assertEquals(1, result.items.size)
            assertEquals("remote-1", result.items[0].id)
            val localRecordings = localDataSource.getRecordings()
            assertEquals(1, localRecordings.size)
        }

    @Test
    fun `test deleteRecording syncs with remote`() =
        runTest {
            val recording = TestDataFactory.createTestRecording(id = "recording-1", cameraId = "camera-1")
            localDataSource.addRecordingDirectly(recording)
            remoteDataSource.addRecordingDirectly(recording)

            val result = repository.deleteRecording("recording-1")

            assertTrue(result.isSuccess)
            val localRecordings = localDataSource.getRecordings()
            assertEquals(0, localRecordings.size)
        }

    @Test
    fun `test deleteRecording handles network errors gracefully`() =
        runTest {
            val recording = TestDataFactory.createTestRecording(id = "recording-1", cameraId = "camera-1")
            localDataSource.addRecordingDirectly(recording)
            remoteDataSource.shouldFailOnDelete = true
            remoteDataSource.networkError = Exception("Network error")

            val result = repository.deleteRecording("recording-1")

            assertTrue(result.isSuccess)
            val localRecordings = localDataSource.getRecordings()
            assertEquals(0, localRecordings.size)
        }

    @Test
    fun `test getRecordings handles network errors gracefully`() =
        runTest {
            remoteDataSource.shouldFailOnGet = true
            remoteDataSource.networkError = Exception("Network error")

            val result =
                repository.getRecordings(
                    cameraId = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 20,
                )

            assertTrue(result.items.isEmpty())
        }

    @Test
    fun `test repository works with local-only strategy when remote is null`() =
        runTest {
            val repositoryLocalOnly = RecordingRepositoryImpl(localDataSource, null)
            val recording = TestDataFactory.createTestRecording(id = "recording-1", cameraId = "camera-1")
            localDataSource.addRecordingDirectly(recording)

            val result =
                repositoryLocalOnly.getRecordings(
                    cameraId = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 20,
                )

            assertEquals(1, result.items.size)
            assertEquals("recording-1", result.items[0].id)
        }
}
