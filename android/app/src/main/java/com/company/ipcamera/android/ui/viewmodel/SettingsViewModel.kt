package com.company.ipcamera.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: List<Settings> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: SettingsCategory? = null,
    val isSaving: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings(category: SettingsCategory? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val settings = settingsRepository.getSettings(category)
                _uiState.value = _uiState.value.copy(
                    settings = settings,
                    isLoading = false,
                    selectedCategory = category
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load settings"
                )
            }
        }
    }

    fun updateSetting(key: String, value: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val result = settingsRepository.updateSetting(key, value)
                result.fold(
                    onSuccess = { updatedSetting ->
                        val updatedSettings = _uiState.value.settings.map {
                            if (it.key == key) updatedSetting else it
                        }
                        _uiState.value = _uiState.value.copy(
                            settings = updatedSettings,
                            isSaving = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to update setting"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to update setting"
                )
            }
        }
    }

    fun resetSettings(category: SettingsCategory? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val result = settingsRepository.resetSettings(category)
                result.fold(
                    onSuccess = {
                        loadSettings(category)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to reset settings"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to reset settings"
                )
            }
        }
    }

    fun refresh() {
        loadSettings(_uiState.value.selectedCategory)
    }
}

