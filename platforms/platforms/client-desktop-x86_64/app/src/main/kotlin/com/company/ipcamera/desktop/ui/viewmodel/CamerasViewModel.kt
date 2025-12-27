package com.company.ipcamera.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CamerasUiState {
    object Loading : CamerasUiState()
    data class Success(val cameras: List<Camera>) : CamerasUiState()
    data class Error(val message: String) : CamerasUiState()
}

class CamerasViewModel(
    private val getCamerasUseCase: GetCamerasUseCase,
    private val deleteCameraUseCase: DeleteCameraUseCase,
    private val discoverCamerasUseCase: DiscoverCamerasUseCase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow<CamerasUiState>(CamerasUiState.Loading)
    val uiState: StateFlow<CamerasUiState> = _uiState.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    init {
        loadCameras()
    }

    fun loadCameras() {
        coroutineScope.launch {
            _uiState.value = CamerasUiState.Loading
            try {
                val cameras = getCamerasUseCase.invoke()
                _uiState.value = CamerasUiState.Success(cameras)
            } catch (e: Exception) {
                _uiState.value = CamerasUiState.Error(e.message ?: "Не удалось загрузить камеры")
            }
        }
    }

    fun deleteCamera(cameraId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        coroutineScope.launch {
            try {
                deleteCameraUseCase.invoke(cameraId)
                // Перезагружаем список после удаления
                loadCameras()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось удалить камеру")
            }
        }
    }

    fun discoverCameras() {
        coroutineScope.launch {
            try {
                discoverCamerasUseCase.invoke()
                // Перезагружаем список после обнаружения
                loadCameras()
            } catch (e: Exception) {
                // Ошибка обнаружения
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun getFilteredCameras(): List<Camera> {
        val state = _uiState.value
        if (state !is CamerasUiState.Success) return emptyList()

        return if (searchQuery.isBlank()) {
            state.cameras
        } else {
            state.cameras.filter { camera ->
                camera.name.contains(searchQuery, ignoreCase = true) ||
                camera.url.contains(searchQuery, ignoreCase = true) ||
                camera.model?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }
}

