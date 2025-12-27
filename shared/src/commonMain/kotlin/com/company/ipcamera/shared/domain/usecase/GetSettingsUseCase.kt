package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SystemSettings
import com.company.ipcamera.shared.domain.repository.SettingsRepository

/**
 * Use case для получения настроек
 */
class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Получить все настройки или настройки категории
     */
    suspend operator fun invoke(category: SettingsCategory? = null): List<Settings> {
        return settingsRepository.getSettings(category)
    }

    /**
     * Получить системные настройки
     */
    suspend fun getSystemSettings(): SystemSettings? {
        return settingsRepository.getSystemSettings()
    }
}

