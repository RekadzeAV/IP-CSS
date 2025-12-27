package com.company.ipcamera.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecordingsUiState(
    val recordings: List<Recording> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val cameraId: String? = null,
    val startTime: Long? = null,
    val endTime: Long? = null
)

class RecordingsViewModel(
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingsUiState())
    val uiState: StateFlow<RecordingsUiState> = _uiState.asStateFlow()

    init {
        loadRecordings()
    }

    fun loadRecordings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val state = _uiState.value
                val result = recordingRepository.getRecordings(
                    cameraId = state.cameraId,
                    startTime = state.startTime,
                    endTime = state.endTime
                )
                _uiState.value = state.copy(
                    recordings = result.items,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load recordings"
                )
            }
        }
    }

    fun setCameraFilter(cameraId: String?) {
        _uiState.value = _uiState.value.copy(cameraId = cameraId)
        loadRecordings()
    }

    fun setTimeRange(startTime: Long?, endTime: Long?) {
        _uiState.value = _uiState.value.copy(
            startTime = startTime,
            endTime = endTime
        )
        loadRecordings()
    }

    fun deleteRecording(recordingId: String) {
        viewModelScope.launch {
            try {
                val result = recordingRepository.deleteRecording(recordingId)
                result.fold(
                    onSuccess = { loadRecordings() },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Failed to delete recording"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete recording"
                )
            }
        }
    }

    fun refresh() {
        loadRecordings()
    }
}

