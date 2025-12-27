package com.company.ipcamera.shared.data.datasource.local

import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory

/**
 * Локальный источник данных для настроек.
 * Отвечает за работу с локальной базой данных SQLite.
 */
interface SettingsLocalDataSource {
    /**
     * Получить все настройки из локальной БД
     */
    suspend fun getSettings(): List<Settings>

    /**
     * Получить настройку по ID из локальной БД
     */
    suspend fun getSettingById(id: String): Settings?

    /**
     * Получить настройки по категории
     */
    suspend fun getSettingsByCategory(category: SettingsCategory): List<Settings>

    /**
     * Получить настройку по категории и ключу
     */
    suspend fun getSettingByCategoryAndKey(category: SettingsCategory, key: String): Settings?

    /**
     * Получить настройку по ключу (поиск во всех категориях)
     */
    suspend fun getSettingByKey(key: String): Settings?

    /**
     * Сохранить настройку в локальную БД
     */
    suspend fun saveSetting(setting: Settings): Result<Settings>

    /**
     * Сохранить список настроек в локальную БД (batch операция)
     */
    suspend fun saveSettings(settings: List<Settings>): Result<List<Settings>>

    /**
     * Обновить настройку в локальной БД
     */
    suspend fun updateSetting(setting: Settings): Result<Settings>

    /**
     * Обновить значение настройки
     */
    suspend fun updateSettingValue(category: SettingsCategory, key: String, value: String): Result<Unit>

    /**
     * Удалить настройку из локальной БД
     */
    suspend fun deleteSetting(id: String): Result<Unit>

    /**
     * Удалить настройку по категории и ключу
     */
    suspend fun deleteSettingByCategoryAndKey(category: SettingsCategory, key: String): Result<Unit>

    /**
     * Удалить все настройки по категории
     */
    suspend fun deleteSettingsByCategory(category: SettingsCategory): Result<Unit>

    /**
     * Удалить все настройки из локальной БД
     */
    suspend fun deleteAllSettings(): Result<Unit>

    /**
     * Проверить существование настройки
     */
    suspend fun settingExists(id: String): Boolean

    /**
     * Проверить существование настройки по категории и ключу
     */
    suspend fun settingExists(category: SettingsCategory, key: String): Boolean
}

