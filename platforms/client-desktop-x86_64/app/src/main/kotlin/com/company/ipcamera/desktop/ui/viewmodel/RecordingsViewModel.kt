package com.company.ipcamera.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.repository.RecordingRepository
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
    private val recordingRepository: RecordingRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow<RecordingsUiState>(RecordingsUiState.Loading)
    val uiState: StateFlow<RecordingsUiState> = _uiState.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set
    
    var selectedCameraId by mutableStateOf<String?>(null)
    var selectedStatus by mutableStateOf<RecordingStatus?>(null)
    var selectedFormat by mutableStateOf<RecordingFormat?>(null)
    var selectedQuality by mutableStateOf<Quality?>(null)
    
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

        var filtered = state.recordings

        // Фильтр по поисковому запросу
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { recording ->
                recording.cameraName?.contains(searchQuery, ignoreCase = true) == true ||
                recording.id.contains(searchQuery, ignoreCase = true)
            }
        }

        // Фильтр по статусу
        selectedStatus?.let { status ->
            filtered = filtered.filter { it.status == status }
        }

        // Фильтр по формату
        selectedFormat?.let { format ->
            filtered = filtered.filter { it.format == format }
        }

        // Фильтр по качеству
        selectedQuality?.let { quality ->
            filtered = filtered.filter { it.quality == quality }
        }

        return filtered
    }

    /**
     * Экспорт записи в указанный формат.
     */
    fun exportRecording(
        recordingId: String,
        format: String = "mp4",
        quality: String = "medium",
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        coroutineScope.launch {
            try {
                recordingRepository.exportRecording(recordingId, format, quality).fold(
                    onSuccess = { resultUrl -> onSuccess(resultUrl) },
                    onFailure = { e -> onError(e.message ?: "Не удалось экспортировать запись") }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось экспортировать запись")
            }
        }
    }
}