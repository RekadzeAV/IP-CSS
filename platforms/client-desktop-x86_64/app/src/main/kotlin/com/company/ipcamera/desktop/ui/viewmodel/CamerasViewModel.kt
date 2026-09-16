package com.company.ipcamera.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.company.ipcamera.desktop.data.CacheKeys
import com.company.ipcamera.desktop.data.CacheManager
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

sealed class CamerasUiState {
    object Loading : CamerasUiState()
    data class Success(val cameras: List<Camera>) : CamerasUiState()
    data class Error(val message: String) : CamerasUiState()
    data class Empty(val message: String = "Нет камер") : CamerasUiState()
}

class CamerasViewModel(
    private val getCamerasUseCase: GetCamerasUseCase,
    private val deleteCameraUseCase: DeleteCameraUseCase,
    private val discoverCamerasUseCase: DiscoverCamerasUseCase,
    private val cacheManager: CacheManager,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow<CamerasUiState>(CamerasUiState.Loading)
    val uiState: StateFlow<CamerasUiState> = _uiState.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    init {
        loadCameras(useCache = true)
    }

    fun loadCameras(useCache: Boolean = true) {
        coroutineScope.launch {
            // Проверяем кэш
            if (useCache) {
                val cachedCameras: List<Camera>? = cacheManager.get(CacheKeys.CAMERAS)
                if (cachedCameras != null && cachedCameras.isNotEmpty()) {
                    _uiState.value = CamerasUiState.Success(cachedCameras)
                    // Загружаем свежие данные в фоне
                    loadFromNetwork()
                    return@launch
                }
            }

            _uiState.value = CamerasUiState.Loading
            try {
                val cameras = getCamerasUseCase.invoke()

                // Сохраняем в кэш
                cacheManager.put(CacheKeys.CAMERAS, cameras, ttl = 5.minutes)

                if (cameras.isEmpty()) {
                    _uiState.value = CamerasUiState.Empty()
                } else {
                    _uiState.value = CamerasUiState.Success(cameras)
                }
            } catch (e: Exception) {
                _uiState.value = CamerasUiState.Error(e.message ?: "Не удалось загрузить камеры")
            }
        }
    }

    private suspend fun loadFromNetwork() {
        try {
            val cameras = getCamerasUseCase.invoke()
            cacheManager.put(CacheKeys.CAMERAS, cameras, ttl = 5.minutes)
            if (cameras.isNotEmpty()) {
                _uiState.value = CamerasUiState.Success(cameras)
            }
        } catch (e: Exception) {
            // Ошибка загрузки в фоне - не обновляем состояние, оставляем кэш
        }
    }

    fun deleteCamera(cameraId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        coroutineScope.launch {
            try {
                deleteCameraUseCase.invoke(cameraId)
                // Инвалидируем кэш
                cacheManager.invalidate(CacheKeys.CAMERAS)
                cacheManager.invalidate(CacheKeys.camera(cameraId))
                // Перезагружаем список после удаления
                loadCameras(useCache = false)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось удалить камеру")
            }
        }
    }

    fun discoverCameras() {
        coroutineScope.launch {
            try {
                discoverCamerasUseCase.invoke(forceRefresh = true)
                // Инвалидируем кэш
                cacheManager.invalidate(CacheKeys.CAMERAS)
                // Перезагружаем список после обнаружения
                loadCameras(useCache = false)
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

