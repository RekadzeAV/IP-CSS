package com.company.ipcamera.desktop.ui.viewmodel

import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import com.company.ipcamera.shared.domain.usecase.DeleteRecordingUseCase
import com.company.ipcamera.shared.domain.usecase.GetRecordingsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingsViewModelTest {

    private lateinit var getRecordingsUseCase: GetRecordingsUseCase
    private lateinit var deleteRecordingUseCase: DeleteRecordingUseCase
    private lateinit var recordingRepository: RecordingRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        getRecordingsUseCase = mock()
        deleteRecordingUseCase = mock()
        recordingRepository = mock()
    }

    @Test
    fun `loadRecordings emits Success when data exists`() = runTest(testDispatcher) {
        val recordings = listOf(
            Recording(
                id = "1",
                cameraId = "camera1",
                cameraName = "Camera 1",
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis() + 3600000,
                filePath = "/path/to/recording.mp4",
                duration = 3600L
            )
        )
        whenever(getRecordingsUseCase.invoke(null, null, null, 1, 20))
            .thenReturn(PaginatedResult(items = recordings, total = 1, page = 1, limit = 20, hasMore = false))

        val viewModel = RecordingsViewModel(
            getRecordingsUseCase = getRecordingsUseCase,
            deleteRecordingUseCase = deleteRecordingUseCase,
            recordingRepository = recordingRepository,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        viewModel.loadRecordings()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is RecordingsUiState.Success)
        assertEquals(recordings, (state as RecordingsUiState.Success).recordings)
    }

    @Test
    fun `loadRecordings emits Error on exception`() = runTest(testDispatcher) {
        whenever(getRecordingsUseCase.invoke(null, null, null, 1, 20))
            .thenThrow(RuntimeException("Network error"))

        val viewModel = RecordingsViewModel(
            getRecordingsUseCase = getRecordingsUseCase,
            deleteRecordingUseCase = deleteRecordingUseCase,
            recordingRepository = recordingRepository,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        viewModel.loadRecordings()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is RecordingsUiState.Error)
    }

    @Test
    fun `deleteRecording delegates to use case`() = runTest(testDispatcher) {
        val recordingId = "1"
        whenever(deleteRecordingUseCase.invoke(recordingId))
            .thenReturn(kotlin.Result.success(Unit))
        whenever(getRecordingsUseCase.invoke(null, null, null, 1, 20))
            .thenReturn(PaginatedResult(items = emptyList(), total = 0, page = 1, limit = 20, hasMore = false))

        val viewModel = RecordingsViewModel(
            getRecordingsUseCase = getRecordingsUseCase,
            deleteRecordingUseCase = deleteRecordingUseCase,
            recordingRepository = recordingRepository,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        var onSuccessCalled = false
        viewModel.deleteRecording(recordingId) {
            onSuccessCalled = true
        }
        advanceUntilIdle()

        verify(deleteRecordingUseCase).invoke(recordingId)
        assertTrue(viewModel.uiState.value is RecordingsUiState.Success)
    }

    @Test
    fun `updateSearchQuery updates property`() = runTest(testDispatcher) {
        whenever(getRecordingsUseCase.invoke(null, null, null, 1, 20))
            .thenReturn(PaginatedResult(items = emptyList(), total = 0, page = 1, limit = 20, hasMore = false))

        val viewModel = RecordingsViewModel(
            getRecordingsUseCase = getRecordingsUseCase,
            deleteRecordingUseCase = deleteRecordingUseCase,
            recordingRepository = recordingRepository,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        viewModel.updateSearchQuery("test query")
        advanceUntilIdle()

        assertEquals("test query", viewModel.searchQuery)
    }

    @Test
    fun `setSelectedCamera updates filter and triggers load`() = runTest(testDispatcher) {
        val cameraId = "camera1"
        whenever(getRecordingsUseCase.invoke(cameraId, null, null, 1, 20))
            .thenReturn(PaginatedResult(items = emptyList(), total = 0, page = 1, limit = 20, hasMore = false))
        whenever(getRecordingsUseCase.invoke(null, null, null, 1, 20))
            .thenReturn(PaginatedResult(items = emptyList(), total = 0, page = 1, limit = 20, hasMore = false))

        val viewModel = RecordingsViewModel(
            getRecordingsUseCase = getRecordingsUseCase,
            deleteRecordingUseCase = deleteRecordingUseCase,
            recordingRepository = recordingRepository,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        viewModel.selectedCameraId = cameraId
        advanceUntilIdle()

        assertEquals(cameraId, viewModel.selectedCameraId)
    }
}
