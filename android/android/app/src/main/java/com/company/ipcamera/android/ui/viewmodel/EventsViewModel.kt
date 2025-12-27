package com.company.ipcamera.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventsUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterType: EventType? = null,
    val filterSeverity: EventSeverity? = null,
    val filterCameraId: String? = null,
    val showAcknowledged: Boolean? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true
)

class EventsViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val state = _uiState.value
                val result = eventRepository.getEvents(
                    type = state.filterType,
                    cameraId = state.filterCameraId,
                    severity = state.filterSeverity,
                    acknowledged = state.showAcknowledged,
                    page = page,
                    limit = 20
                )
                val newEvents = if (page == 1) {
                    result.items
                } else {
                    state.events + result.items
                }
                _uiState.value = state.copy(
                    events = newEvents,
                    isLoading = false,
                    currentPage = page,
                    hasMore = result.hasMore
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load events"
                )
            }
        }
    }

    fun setFilterType(type: EventType?) {
        _uiState.value = _uiState.value.copy(filterType = type, currentPage = 1)
        loadEvents(1)
    }

    fun setFilterSeverity(severity: EventSeverity?) {
        _uiState.value = _uiState.value.copy(filterSeverity = severity, currentPage = 1)
        loadEvents(1)
    }

    fun setFilterCamera(cameraId: String?) {
        _uiState.value = _uiState.value.copy(filterCameraId = cameraId, currentPage = 1)
        loadEvents(1)
    }

    fun setShowAcknowledged(show: Boolean?) {
        _uiState.value = _uiState.value.copy(showAcknowledged = show, currentPage = 1)
        loadEvents(1)
    }

    fun acknowledgeEvent(eventId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = eventRepository.acknowledgeEvent(eventId, userId)
                result.getOrElse { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to acknowledge event"
                    )
                    return@launch
                }
                val event = result.getOrNull()
                if (event != null) {
                    val updatedEvents = _uiState.value.events.map {
                        if (it.id == eventId) event else it
                    }
                    _uiState.value = _uiState.value.copy(events = updatedEvents)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to acknowledge event"
                )
            }
        }
    }

    fun loadMore() {
        if (!_uiState.value.isLoading && _uiState.value.hasMore) {
            loadEvents(_uiState.value.currentPage + 1)
        }
    }

    fun refresh() {
        loadEvents(1)
    }
}

