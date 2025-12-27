package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SystemSettings
import com.company.ipcamera.shared.domain.repository.SettingsRepository

/**
 * Use case для обновления настроек
 */
class UpdateSettingUseCase(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Обновить одну настройку
     */
    suspend operator fun invoke(key: String, value: String): Result<Settings> {
        if (key.isBlank()) {
            return Result.failure(IllegalArgumentException("Setting key cannot be blank"))
        }

        return settingsRepository.updateSetting(key, value)
    }

    /**
     * Обновить несколько настроек
     */
    suspend fun updateSettings(settings: Map<String, String>): Result<Int> {
        return settingsRepository.updateSettings(settings)
    }

    /**
     * Обновить системные настройки
     */
    suspend fun updateSystemSettings(settings: SystemSettings): Result<Unit> {
        return settingsRepository.updateSystemSettings(settings)
    }
}

