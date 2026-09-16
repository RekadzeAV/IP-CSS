package com.company.ipcamera.shared.test

import com.company.ipcamera.shared.data.datasource.local.SettingsLocalDataSource
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory

/**
 * Mock реализация SettingsLocalDataSource для тестов
 */
class MockSettingsLocalDataSource(
    private var settings: MutableList<Settings> = mutableListOf(),
) : SettingsLocalDataSource {
    var shouldFailOnSave: Boolean = false
    var shouldFailOnUpdate: Boolean = false
    var shouldFailOnDelete: Boolean = false

    override suspend fun getSettings(): List<Settings> = settings.toList()

    override suspend fun getSettingById(id: String): Settings? = settings.find { it.id == id }

    override suspend fun getSettingsByCategory(category: SettingsCategory): List<Settings> {
        return settings.filter { it.category == category }
    }

    override suspend fun getSettingByCategoryAndKey(
        category: SettingsCategory,
        key: String,
    ): Settings? {
        return settings.find { it.category == category && it.key == key }
    }

    override suspend fun getSettingByKey(key: String): Settings? {
        return settings.find { it.key == key }
    }

    override suspend fun saveSetting(setting: Settings): Result<Settings> {
        return if (shouldFailOnSave) {
            Result.failure(Exception("Mock save failure"))
        } else {
            settings.add(setting)
            Result.success(setting)
        }
    }

    override suspend fun saveSettings(settings: List<Settings>): Result<List<Settings>> {
        return if (shouldFailOnSave) {
            Result.failure(Exception("Mock save failure"))
        } else {
            this.settings.addAll(settings)
            Result.success(settings)
        }
    }

    override suspend fun updateSetting(setting: Settings): Result<Settings> {
        return if (shouldFailOnUpdate) {
            Result.failure(Exception("Mock update failure"))
        } else {
            val index = settings.indexOfFirst { it.id == setting.id }
            if (index >= 0) {
                settings[index] = setting
                Result.success(setting)
            } else {
                Result.failure(Exception("Setting not found: ${setting.id}"))
            }
        }
    }

    override suspend fun updateSettings(settings: Map<String, String>): Result<Int> {
        return if (shouldFailOnUpdate) {
            Result.failure(Exception("Mock update failure"))
        } else {
            var count = 0
            for ((key, value) in settings) {
                val existing = this.settings.find { it.key == key } ?: continue
                val index = this.settings.indexOfFirst { it.id == existing.id }
                this.settings[index] = existing.copy(value = value, updatedAt = System.currentTimeMillis())
                count++
            }
            Result.success(count)
        }
    }

    override suspend fun updateSettingValue(
        category: SettingsCategory,
        key: String,
        value: String,
    ): Result<Unit> {
        val setting = settings.find { it.category == category && it.key == key }
        return if (setting != null) {
            val updated = setting.copy(value = value, updatedAt = System.currentTimeMillis())
            val index = settings.indexOfFirst { it.id == setting.id }
            settings[index] = updated
            Result.success(Unit)
        } else {
            Result.failure(Exception("Setting not found: $category.$key"))
        }
    }

    override suspend fun deleteSetting(id: String): Result<Unit> {
        return if (shouldFailOnDelete) {
            Result.failure(Exception("Mock delete failure"))
        } else {
            settings.removeIf { it.id == id }
            Result.success(Unit)
        }
    }

    override suspend fun deleteSettingByCategoryAndKey(
        category: SettingsCategory,
        key: String,
    ): Result<Unit> {
        settings.removeIf { it.category == category && it.key == key }
        return Result.success(Unit)
    }

    override suspend fun deleteSettingsByCategory(category: SettingsCategory): Result<Unit> {
        settings.removeIf { it.category == category }
        return Result.success(Unit)
    }

    override suspend fun deleteAllSettings(): Result<Unit> {
        settings.clear()
        return Result.success(Unit)
    }

    override suspend fun settingExists(id: String): Boolean {
        return settings.any { it.id == id }
    }

    override suspend fun settingExists(
        category: SettingsCategory,
        key: String,
    ): Boolean {
        return settings.any { it.category == category && it.key == key }
    }

    fun clear() {
        settings.clear()
    }

    fun addSettingDirectly(setting: Settings) {
        settings.add(setting)
    }
}
