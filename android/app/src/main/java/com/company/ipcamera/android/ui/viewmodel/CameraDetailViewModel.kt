package com.company.ipcamera.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.ConnectionTestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CameraDetailUiState(
    val camera: Camera? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val connectionTestResult: ConnectionTestResult? = null,
    val isTestingConnection: Boolean = false
)

class CameraDetailViewModel(
    private val cameraRepository: CameraRepository,
    private val cameraId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraDetailUiState())
    val uiState: StateFlow<CameraDetailUiState> = _uiState.asStateFlow()

    init {
        loadCamera()
    }

    fun loadCamera() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val camera = cameraRepository.getCameraById(cameraId)
                _uiState.value = _uiState.value.copy(
                    camera = camera,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load camera"
                )
            }
        }
    }

    fun testConnection() {
        val camera = _uiState.value.camera ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true, error = null)
            try {
                val result = cameraRepository.testConnection(camera)
                _uiState.value = _uiState.value.copy(
                    connectionTestResult = result,
                    isTestingConnection = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTestingConnection = false,
                    error = e.message ?: "Failed to test connection"
                )
            }
        }
    }

    fun refresh() {
        loadCamera()
    }
}

