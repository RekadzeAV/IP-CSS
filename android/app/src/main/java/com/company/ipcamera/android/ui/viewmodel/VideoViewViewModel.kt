package com.company.ipcamera.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ipcamera.core.network.api.StreamApiService
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
    val isRecording: Boolean = false,
    val rtspUrl: String? = null,
    val streamActive: Boolean = false
)

class VideoViewViewModel(
    private val cameraRepository: CameraRepository,
    private val streamApiService: StreamApiService,
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
                // Получаем RTSP URL для трансляции
                loadRtspUrl()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load camera"
                )
            }
        }
    }

    private fun loadRtspUrl() {
        viewModelScope.launch {
            try {
                val result = streamApiService.getRtspUrl(cameraId)
                result.fold(
                    onSuccess = { url ->
                        _uiState.value = _uiState.value.copy(rtspUrl = url)
                    },
                    onError = { error ->
                        // Не критично, если не удалось получить URL
                    }
                )
            } catch (e: Exception) {
                // Игнорируем ошибку
            }
        }
    }

    fun startStream() {
        viewModelScope.launch {
            try {
                val result = streamApiService.startStream(cameraId)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(streamActive = true)
                        loadRtspUrl() // Обновляем RTSP URL
                    },
                    onError = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Failed to start stream"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to start stream"
                )
            }
        }
    }

    fun stopStream() {
        viewModelScope.launch {
            try {
                val result = streamApiService.stopStream(cameraId)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            streamActive = false,
                            isPlaying = false
                        )
                    },
                    onError = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Failed to stop stream"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to stop stream"
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
        // Останавливаем стрим при очистке ViewModel
        if (_uiState.value.streamActive) {
            stopStream()
        }
        _uiState.value = _uiState.value.copy(isPlaying = false, isRecording = false)
    }
}

