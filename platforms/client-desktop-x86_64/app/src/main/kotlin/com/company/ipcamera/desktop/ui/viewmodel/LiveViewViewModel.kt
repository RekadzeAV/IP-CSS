package com.company.ipcamera.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.usecase.GetCamerasUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class GridLayout {
    SINGLE,    // 1 камера
    GRID_4,    // 2x2 (4 камеры)
    GRID_9,    // 3x3 (9 камер)
    GRID_16    // 4x4 (16 камер)
}

data class LiveViewState(
    val cameras: List<Camera> = emptyList(),
    val selectedCameras: List<String> = emptyList(),
    val gridLayout: GridLayout = GridLayout.SINGLE,
    val isLoading: Boolean = false,
    val error: String? = null
)

class LiveViewViewModel(
    private val getCamerasUseCase: GetCamerasUseCase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _state = MutableStateFlow(LiveViewState())
    val state: StateFlow<LiveViewState> = _state.asStateFlow()
    
    init {
        loadCameras()
    }
    
    fun loadCameras() {
        coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val cameras = getCamerasUseCase.invoke()
                _state.value = _state.value.copy(
                    cameras = cameras,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Не удалось загрузить камеры"
                )
            }
        }
    }
    
    fun selectCamera(cameraId: String) {
        val current = _state.value.selectedCameras.toMutableList()
        if (current.contains(cameraId)) {
            current.remove(cameraId)
        } else {
            val maxCameras = when (_state.value.gridLayout) {
                GridLayout.SINGLE -> 1
                GridLayout.GRID_4 -> 4
                GridLayout.GRID_9 -> 9
                GridLayout.GRID_16 -> 16
            }
            if (current.size < maxCameras) {
                current.add(cameraId)
            }
        }
        _state.value = _state.value.copy(selectedCameras = current)
    }
    
    fun removeCamera(cameraId: String) {
        val current = _state.value.selectedCameras.toMutableList()
        current.remove(cameraId)
        _state.value = _state.value.copy(selectedCameras = current)
    }
    
    fun setGridLayout(layout: GridLayout) {
        val current = _state.value.selectedCameras.toMutableList()
        val maxCameras = when (layout) {
            GridLayout.SINGLE -> 1
            GridLayout.GRID_4 -> 4
            GridLayout.GRID_9 -> 9
            GridLayout.GRID_16 -> 16
        }
        // Удаляем лишние камеры, если новый layout меньше
        while (current.size > maxCameras) {
            current.removeLast()
        }
        _state.value = _state.value.copy(
            gridLayout = layout,
            selectedCameras = current
        )
    }
    
    fun getSelectedCameras(): List<Camera> {
        val selectedIds = _state.value.selectedCameras
        return _state.value.cameras.filter { it.id in selectedIds }
    }
}

