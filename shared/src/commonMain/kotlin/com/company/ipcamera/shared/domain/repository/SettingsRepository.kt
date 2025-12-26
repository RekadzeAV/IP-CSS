package com.company.ipcamera.shared.domain.repository

import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SystemSettings

/**
 * Репозиторий для работы с настройками
 */
interface SettingsRepository {
    /**
     * Получить все настройки
     */
    suspend fun getSettings(category: SettingsCategory? = null): List<Settings>
    
    /**
     * Получить настройку по ключу
     */
    suspend fun getSetting(key: String): Settings?
    
    /**
     * Обновить настройки
     */
    suspend fun updateSettings(settings: Map<String, String>): Result<Int>
    
    /**
     * Обновить одну настройку
     */
    suspend fun updateSetting(key: String, value: String): Result<Settings>
    
    /**
     * Удалить настройку
     */
    suspend fun deleteSetting(key: String): Result<Unit>
    
    /**
     * Получить системные настройки
     */
    suspend fun getSystemSettings(): SystemSettings?
    
    /**
     * Обновить системные настройки
     */
    suspend fun updateSystemSettings(settings: SystemSettings): Result<Unit>
    
    /**
     * Сбросить настройки к значениям по умолчанию
     */
    suspend fun resetSettings(category: SettingsCategory? = null): Result<Unit>
    
    /**
     * Экспортировать настройки
     */
    suspend fun exportSettings(): Result<Map<String, String>>
    
    /**
     * Импортировать настройки
     */
    suspend fun importSettings(settings: Map<String, String>): Result<Unit>
}

