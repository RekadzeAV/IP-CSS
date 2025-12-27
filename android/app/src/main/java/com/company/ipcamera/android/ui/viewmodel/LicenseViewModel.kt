package com.company.ipcamera.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ipcamera.shared.domain.model.License
import com.company.ipcamera.shared.domain.repository.LicenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LicenseUiState(
    val license: License? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isActivating: Boolean = false,
    val activationKey: String = ""
)

class LicenseViewModel(
    private val licenseRepository: LicenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LicenseUiState())
    val uiState: StateFlow<LicenseUiState> = _uiState.asStateFlow()

    init {
        loadLicense()
    }

    fun loadLicense() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val license = licenseRepository.getLicense()
                _uiState.value = _uiState.value.copy(
                    license = license,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load license"
                )
            }
        }
    }

    fun updateActivationKey(key: String) {
        _uiState.value = _uiState.value.copy(activationKey = key)
    }

    fun activateLicense() {
        val key = _uiState.value.activationKey
        if (key.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Activation key is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActivating = true, error = null)
            try {
                val result = licenseRepository.activateLicense(key)
                result.fold(
                    onSuccess = {
                        loadLicense()
                        _uiState.value = _uiState.value.copy(
                            isActivating = false,
                            activationKey = ""
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isActivating = false,
                            error = error.message ?: "Failed to activate license"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isActivating = false,
                    error = e.message ?: "Failed to activate license"
                )
            }
        }
    }

    fun refresh() {
        loadLicense()
    }
}

