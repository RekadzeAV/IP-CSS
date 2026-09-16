package com.company.ipcamera.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CameraListUiState(
    val cameras: List<Camera> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CameraListViewModel(
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraListUiState())
    val uiState: StateFlow<CameraListUiState> = _uiState.asStateFlow()

    init {
        loadCameras()
    }

    fun loadCameras() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val cameras = cameraRepository.getCameras()
                _uiState.value = _uiState.value.copy(
                    cameras = cameras,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load cameras"
                )
            }
        }
    }

    fun refresh() {
        loadCameras()
    }
}

