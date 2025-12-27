package com.company.ipcamera.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SystemSettings
import com.company.ipcamera.shared.domain.usecase.GetSettingsUseCase
import com.company.ipcamera.shared.domain.usecase.UpdateSettingUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    data class Success(val settings: List<Settings>, val systemSettings: SystemSettings?) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingUseCase: UpdateSettingUseCase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    var selectedCategory by mutableStateOf<SettingsCategory?>(null)
        private set

    init {
        loadSettings()
    }

    fun loadSettings() {
        coroutineScope.launch {
            _uiState.value = SettingsUiState.Loading
            try {
                val settings = getSettingsUseCase.invoke(selectedCategory)
                val systemSettings = getSettingsUseCase.getSystemSettings()
                _uiState.value = SettingsUiState.Success(settings, systemSettings)
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Не удалось загрузить настройки")
            }
        }
    }

    fun updateSetting(key: String, value: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        coroutineScope.launch {
            try {
                updateSettingUseCase.invoke(key, value).fold(
                    onSuccess = {
                        loadSettings()
                        onSuccess()
                    },
                    onFailure = { e ->
                        onError(e.message ?: "Не удалось обновить настройку")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось обновить настройку")
            }
        }
    }

    fun updateSystemSettings(settings: SystemSettings, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        coroutineScope.launch {
            try {
                updateSettingUseCase.updateSystemSettings(settings).fold(
                    onSuccess = {
                        loadSettings()
                        onSuccess()
                    },
                    onFailure = { e ->
                        onError(e.message ?: "Не удалось обновить системные настройки")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось обновить системные настройки")
            }
        }
    }

    fun setSelectedCategory(category: SettingsCategory?) {
        selectedCategory = category
        loadSettings()
    }

    fun getSettingsByCategory(): Map<SettingsCategory, List<Settings>> {
        val state = _uiState.value
        if (state !is SettingsUiState.Success) return emptyMap()

        return state.settings.groupBy { it.category }
    }
}

