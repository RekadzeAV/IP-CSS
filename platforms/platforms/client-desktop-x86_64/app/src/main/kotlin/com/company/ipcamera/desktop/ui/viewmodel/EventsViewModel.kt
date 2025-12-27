package com.company.ipcamera.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.usecase.AcknowledgeEventUseCase
import com.company.ipcamera.shared.domain.usecase.DeleteEventUseCase
import com.company.ipcamera.shared.domain.usecase.GetEventsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EventsUiState {
    object Loading : EventsUiState()
    data class Success(val events: List<Event>, val total: Int, val page: Int, val hasMore: Boolean) : EventsUiState()
    data class Error(val message: String) : EventsUiState()
}

class EventsViewModel(
    private val getEventsUseCase: GetEventsUseCase,
    private val acknowledgeEventUseCase: AcknowledgeEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    var selectedType by mutableStateOf<EventType?>(null)
        private set

    var selectedSeverity by mutableStateOf<EventSeverity?>(null)
        private set

    var selectedCameraId by mutableStateOf<String?>(null)
        private set

    var showOnlyUnacknowledged by mutableStateOf(false)
        private set

    var currentPage by mutableStateOf(1)
        private set

    val pageSize = 20

    init {
        loadEvents()
    }

    fun loadEvents(page: Int = 1) {
        coroutineScope.launch {
            _uiState.value = EventsUiState.Loading
            try {
                val result = getEventsUseCase.invoke(
                    type = selectedType,
                    cameraId = selectedCameraId?.takeIf { it.isNotBlank() },
                    severity = selectedSeverity,
                    acknowledged = if (showOnlyUnacknowledged) false else null,
                    page = page,
                    limit = pageSize
                )
                currentPage = page
                _uiState.value = EventsUiState.Success(
                    events = result.items,
                    total = result.total,
                    page = result.page,
                    hasMore = result.hasMore
                )
            } catch (e: Exception) {
                _uiState.value = EventsUiState.Error(e.message ?: "Не удалось загрузить события")
            }
        }
    }

    fun acknowledgeEvent(eventId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        coroutineScope.launch {
            try {
                acknowledgeEventUseCase.invoke(eventId).fold(
                    onSuccess = {
                        loadEvents(currentPage)
                        onSuccess()
                    },
                    onFailure = { e ->
                        onError(e.message ?: "Не удалось подтвердить событие")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось подтвердить событие")
            }
        }
    }

    fun deleteEvent(eventId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        coroutineScope.launch {
            try {
                deleteEventUseCase.invoke(eventId).fold(
                    onSuccess = {
                        loadEvents(currentPage)
                        onSuccess()
                    },
                    onFailure = { e ->
                        onError(e.message ?: "Не удалось удалить событие")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось удалить событие")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun setSelectedType(type: EventType?) {
        selectedType = type
        loadEvents(1)
    }

    fun setSelectedSeverity(severity: EventSeverity?) {
        selectedSeverity = severity
        loadEvents(1)
    }

    fun setSelectedCamera(cameraId: String?) {
        selectedCameraId = cameraId
        loadEvents(1)
    }

    fun setShowOnlyUnacknowledged(value: Boolean) {
        showOnlyUnacknowledged = value
        loadEvents(1)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state is EventsUiState.Success && state.hasMore) {
            loadEvents(state.page + 1)
        }
    }

    fun loadPreviousPage() {
        val state = _uiState.value
        if (state is EventsUiState.Success && state.page > 1) {
            loadEvents(state.page - 1)
        }
    }

    fun getFilteredEvents(): List<Event> {
        val state = _uiState.value
        if (state !is EventsUiState.Success) return emptyList()

        return if (searchQuery.isBlank()) {
            state.events
        } else {
            state.events.filter { event ->
                event.cameraName?.contains(searchQuery, ignoreCase = true) == true ||
                event.description?.contains(searchQuery, ignoreCase = true) == true ||
                event.type.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }
}

