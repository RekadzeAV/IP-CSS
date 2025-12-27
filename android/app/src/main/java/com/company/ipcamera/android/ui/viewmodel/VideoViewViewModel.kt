package com.company.ipcamera.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VideoViewUiState(
    val camera: Camera? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPlaying: Boolean = false,
    val isRecording: Boolean = false
)

class VideoViewViewModel(
    private val cameraRepository: CameraRepository,
    private val cameraId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoViewUiState())
    val uiState: StateFlow<VideoViewUiState> = _uiState.asStateFlow()

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

    fun togglePlayback() {
        _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
    }

    fun toggleRecording() {
        _uiState.value = _uiState.value.copy(isRecording = !_uiState.value.isRecording)
    }

    override fun onCleared() {
        super.onCleared()
        // Stop playback and recording when ViewModel is cleared
        _uiState.value = _uiState.value.copy(isPlaying = false, isRecording = false)
    }
}

