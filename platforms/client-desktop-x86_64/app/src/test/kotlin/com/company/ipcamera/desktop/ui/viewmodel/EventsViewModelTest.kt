package com.company.ipcamera.desktop.ui.viewmodel

import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.usecase.AcknowledgeEventUseCase
import com.company.ipcamera.shared.domain.usecase.DeleteEventUseCase
import com.company.ipcamera.shared.domain.usecase.GetEventsUseCase
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
class EventsViewModelTest {

    private lateinit var getEventsUseCase: GetEventsUseCase
    private lateinit var acknowledgeEventUseCase: AcknowledgeEventUseCase
    private lateinit var deleteEventUseCase: DeleteEventUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        getEventsUseCase = mock()
        acknowledgeEventUseCase = mock()
        deleteEventUseCase = mock()
    }

    @Test
    fun `loadEvents emits Success with paged result`() = runTest(testDispatcher) {
        val events = listOf(
            Event(
                id = "1",
                cameraId = "camera1",
                type = EventType.MOTION_DETECTION,
                severity = EventSeverity.WARNING,
                timestamp = System.currentTimeMillis()
            )
        )
        whenever(
            getEventsUseCase.invoke(
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                20
            )
        ).thenReturn(
            PaginatedResult(items = events, total = 1, page = 1, limit = 20, hasMore = false)
        )

        val viewModel = EventsViewModel(
            getEventsUseCase = getEventsUseCase,
            acknowledgeEventUseCase = acknowledgeEventUseCase,
            deleteEventUseCase = deleteEventUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.loadEvents()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EventsUiState.Success)
        assertEquals(1, state.events.size)
    }

    @Test
    fun `loadEvents emits Error on exception`() = runTest(testDispatcher) {
        whenever(
            getEventsUseCase.invoke(
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                20
            )
        )
            .thenThrow(RuntimeException("Network error"))

        val viewModel = EventsViewModel(
            getEventsUseCase = getEventsUseCase,
            acknowledgeEventUseCase = acknowledgeEventUseCase,
            deleteEventUseCase = deleteEventUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        // Act
        viewModel.loadEvents()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is EventsUiState.Error)
    }

    @Test
    fun `acknowledgeEvent delegates to use case`() = runTest(testDispatcher) {
        val eventId = "1"
        val event = Event(
            id = eventId,
            cameraId = "camera1",
            type = EventType.MOTION_DETECTION,
            severity = EventSeverity.WARNING,
            timestamp = System.currentTimeMillis()
        )
        whenever(
            getEventsUseCase.invoke(
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                20
            )
        )
            .thenReturn(PaginatedResult(items = listOf(event), total = 1, page = 1, limit = 20, hasMore = false))
        whenever(acknowledgeEventUseCase.invoke(eq(eventId), any())).thenReturn(Result.success(event.copy(acknowledged = true)))

        val viewModel = EventsViewModel(
            getEventsUseCase = getEventsUseCase,
            acknowledgeEventUseCase = acknowledgeEventUseCase,
            deleteEventUseCase = deleteEventUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        // Act
        var onSuccessCalled = false
        viewModel.acknowledgeEvent(eventId) {
            onSuccessCalled = true
        }
        advanceUntilIdle()

        verify(acknowledgeEventUseCase).invoke(eq(eventId), any())
        assertTrue(viewModel.uiState.value is EventsUiState.Success)
    }

    @Test
    fun `deleteEvent delegates to use case`() = runTest(testDispatcher) {
        val eventId = "1"
        whenever(deleteEventUseCase.invoke(eventId))
            .thenReturn(kotlin.Result.success(Unit))
        whenever(
            getEventsUseCase.invoke(
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                20
            )
        )
            .thenReturn(PaginatedResult(items = emptyList(), total = 0, page = 1, limit = 20, hasMore = false))

        val viewModel = EventsViewModel(
            getEventsUseCase = getEventsUseCase,
            acknowledgeEventUseCase = acknowledgeEventUseCase,
            deleteEventUseCase = deleteEventUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        // Act
        var onSuccessCalled = false
        viewModel.deleteEvent(eventId) {
            onSuccessCalled = true
        }
        advanceUntilIdle()

        // Assert
        verify(deleteEventUseCase).invoke(eventId)
        assertTrue(viewModel.uiState.value is EventsUiState.Success)
    }

    @Test
    fun `updateSearchQuery updates query`() = runTest(testDispatcher) {
        whenever(
            getEventsUseCase.invoke(
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                20
            )
        )
            .thenReturn(PaginatedResult(items = emptyList(), total = 0, page = 1, limit = 20, hasMore = false))

        val viewModel = EventsViewModel(
            getEventsUseCase = getEventsUseCase,
            acknowledgeEventUseCase = acknowledgeEventUseCase,
            deleteEventUseCase = deleteEventUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        // Act
        viewModel.updateSearchQuery("test query")
        advanceUntilIdle()

        // Assert
        assertEquals("test query", viewModel.searchQuery)
        verify(getEventsUseCase, atLeastOnce()).invoke(null, null, null, null, null, null, 1, 20)
    }
}
