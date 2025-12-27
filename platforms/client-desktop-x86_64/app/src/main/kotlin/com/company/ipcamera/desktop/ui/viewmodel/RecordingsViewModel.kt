package com.company.ipcamera.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.usecase.DeleteRecordingUseCase
import com.company.ipcamera.shared.domain.usecase.GetRecordingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RecordingsUiState {
    object Loading : RecordingsUiState()
    data class Success(val recordings: List<Recording>, val total: Int, val page: Int, val hasMore: Boolean) : RecordingsUiState()
    data class Error(val message: String) : RecordingsUiState()
}

class RecordingsViewModel(
    private val getRecordingsUseCase: GetRecordingsUseCase,
    private val deleteRecordingUseCase: DeleteRecordingUseCase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow<RecordingsUiState>(RecordingsUiState.Loading)
    val uiState: StateFlow<RecordingsUiState> = _uiState.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set
    
    var selectedCameraId by mutableStateOf<String?>(null)
        private set
    
    var currentPage by mutableStateOf(1)
        private set
    
    val pageSize = 20

    init {
        loadRecordings()
    }

    fun loadRecordings(page: Int = 1) {
        coroutineScope.launch {
            _uiState.value = RecordingsUiState.Loading
            try {
                val cameraId = selectedCameraId?.takeIf { it.isNotBlank() }
                val result = getRecordingsUseCase.invoke(
                    cameraId = cameraId,
                    page = page,
                    limit = pageSize
                )
                currentPage = page
                _uiState.value = RecordingsUiState.Success(
                    recordings = result.items,
                    total = result.total,
                    page = result.page,
                    hasMore = result.hasMore
                )
            } catch (e: Exception) {
                _uiState.value = RecordingsUiState.Error(e.message ?: "Не удалось загрузить записи")
            }
        }
    }

    fun deleteRecording(recordingId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        coroutineScope.launch {
            try {
                deleteRecordingUseCase.invoke(recordingId).fold(
                    onSuccess = {
                        loadRecordings(currentPage)
                        onSuccess()
                    },
                    onFailure = { e ->
                        onError(e.message ?: "Не удалось удалить запись")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось удалить запись")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        loadRecordings(1)
    }

    fun setSelectedCamera(cameraId: String?) {
        selectedCameraId = cameraId
        loadRecordings(1)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state is RecordingsUiState.Success && state.hasMore) {
            loadRecordings(state.page + 1)
        }
    }

    fun loadPreviousPage() {
        val state = _uiState.value
        if (state is RecordingsUiState.Success && state.page > 1) {
            loadRecordings(state.page - 1)
        }
    }

    fun getFilteredRecordings(): List<Recording> {
        val state = _uiState.value
        if (state !is RecordingsUiState.Success) return emptyList()

        return if (searchQuery.isBlank()) {
            state.recordings
        } else {
            state.recordings.filter { recording ->
                recording.cameraName?.contains(searchQuery, ignoreCase = true) == true ||
                recording.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }
}

