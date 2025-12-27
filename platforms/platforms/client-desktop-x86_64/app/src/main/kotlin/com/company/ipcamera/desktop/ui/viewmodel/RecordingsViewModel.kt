package com.company.ipcamera.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.PaginatedResult
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
    data class Success(val recordings: PaginatedResult<Recording>) : RecordingsUiState()
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
        private set

    var startDate by mutableStateOf<Long?>(null)
        private set

    var endDate by mutableStateOf<Long?>(null)
        private set

    private var currentPage = 1
    private val pageSize = 20

    init {
        loadRecordings()
    }

    fun loadRecordings(page: Int = 1) {
        coroutineScope.launch {
            currentPage = page
            _uiState.value = RecordingsUiState.Loading
            try {
                val result = getRecordingsUseCase.invoke(
                    cameraId = selectedCameraId,
                    startTime = startDate,
                    endTime = endDate,
                    page = page,
                    limit = pageSize
                )
                _uiState.value = RecordingsUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = RecordingsUiState.Error(e.message ?: "Не удалось загрузить записи")
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state is RecordingsUiState.Success && state.recordings.hasMore) {
            loadRecordings(currentPage + 1)
        }
    }

    fun deleteRecording(recordingId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        coroutineScope.launch {
            try {
                deleteRecordingUseCase.invoke(recordingId).fold(
                    onSuccess = {
                        loadRecordings(currentPage) // Перезагружаем текущую страницу
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

    fun exportRecording(
        recordingId: String,
        format: String = "MP4",
        quality: String = "HIGH",
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        coroutineScope.launch {
            try {
                val result = recordingRepository.exportRecording(recordingId, format, quality)
                result.fold(
                    onSuccess = { url -> onSuccess(url) },
                    onFailure = { error -> onError(error.message ?: "Ошибка экспорта") }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось экспортировать запись")
            }
        }
    }

    fun getDownloadUrl(
        recordingId: String,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        coroutineScope.launch {
            try {
                val result = recordingRepository.getDownloadUrl(recordingId)
                result.fold(
                    onSuccess = { url -> onSuccess(url) },
                    onFailure = { error -> onError(error.message ?: "Ошибка получения URL") }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось получить URL для скачивания")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun setCameraFilter(cameraId: String?) {
        selectedCameraId = cameraId
        loadRecordings(1)
    }

    fun setDateRange(start: Long?, end: Long?) {
        startDate = start
        endDate = end
        loadRecordings(1)
    }

    fun clearFilters() {
        searchQuery = ""
        selectedCameraId = null
        startDate = null
        endDate = null
        loadRecordings(1)
    }

    fun getFilteredRecordings(): List<Recording> {
        val state = _uiState.value
        if (state !is RecordingsUiState.Success) return emptyList()

        val recordings = state.recordings.items

        return if (searchQuery.isBlank()) {
            recordings
        } else {
            recordings.filter { recording ->
                recording.cameraName?.contains(searchQuery, ignoreCase = true) == true ||
                recording.cameraId.contains(searchQuery, ignoreCase = true) ||
                recording.format.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }
}
