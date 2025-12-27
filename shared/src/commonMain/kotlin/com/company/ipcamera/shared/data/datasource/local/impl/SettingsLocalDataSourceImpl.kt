package com.company.ipcamera.shared.data.datasource.local.impl

import com.company.ipcamera.shared.data.datasource.local.SettingsLocalDataSource
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.SettingsEntityMapper
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация SettingsLocalDataSource с использованием SQLDelight
 */
class SettingsLocalDataSourceImpl(
    private val databaseFactory: DatabaseFactory
) : SettingsLocalDataSource {

    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = SettingsEntityMapper()

    override suspend fun getSettings(): List<Settings> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectAllSettings()
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting settings from local database" }
            emptyList()
        }
    }

    override suspend fun getSettingById(id: String): Settings? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectSettingById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting setting by id from local database: $id" }
            null
        }
    }

    override suspend fun getSettingsByCategory(category: SettingsCategory): List<Settings> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectSettingsByCategory(category.name)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting settings by category from local database: $category" }
            emptyList()
        }
    }

    override suspend fun getSettingByCategoryAndKey(category: SettingsCategory, key: String): Settings? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectSettingByCategoryAndKey(category.name, key)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting setting by category and key from local database: $category/$key" }
            null
        }
    }

    override suspend fun getSettingByKey(key: String): Settings? = withContext(Dispatchers.Default) {
        try {
            // Ищем настройку по ключу во всех категориях
            val allSettings = database.cameraDatabaseQueries
                .selectAllSettings()
                .executeAsList()
            allSettings.find { it.key == key }?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting setting by key from local database: $key" }
            null
        }
    }

    override suspend fun saveSetting(setting: Settings): Result<Settings> = withContext(Dispatchers.Default) {
        try {
            val dbSetting = mapper.toDatabase(setting)
            database.cameraDatabaseQueries.insertSetting(
                id = dbSetting.id,
                category = dbSetting.category,
                key = dbSetting.key,
                value = dbSetting.value,
                type = dbSetting.type,
                description = dbSetting.description,
                updated_at = dbSetting.updated_at
            )
            Result.success(setting)
        } catch (e: Exception) {
            logger.error(e) { "Error saving setting to local database: ${setting.id}" }
            Result.failure(e)
        }
    }

    override suspend fun saveSettings(settings: List<Settings>): Result<List<Settings>> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.transaction {
                settings.forEach { setting ->
                    val dbSetting = mapper.toDatabase(setting)
                    database.cameraDatabaseQueries.insertSetting(
                        id = dbSetting.id,
                        category = dbSetting.category,
                        key = dbSetting.key,
                        value = dbSetting.value,
                        type = dbSetting.type,
                        description = dbSetting.description,
                        updated_at = dbSetting.updated_at
                    )
                }
            }
            Result.success(settings)
        } catch (e: Exception) {
            logger.error(e) { "Error saving settings to local database" }
            Result.failure(e)
        }
    }

    override suspend fun updateSetting(setting: Settings): Result<Settings> = withContext(Dispatchers.Default) {
        try {
            val dbSetting = mapper.toDatabase(setting)
            database.cameraDatabaseQueries.updateSetting(
                category = dbSetting.category,
                key = dbSetting.key,
                value = dbSetting.value,
                type = dbSetting.type,
                description = dbSetting.description,
                updated_at = dbSetting.updated_at,
                id = dbSetting.id
            )
            Result.success(setting)
        } catch (e: Exception) {
            logger.error(e) { "Error updating setting in local database: ${setting.id}" }
            Result.failure(e)
        }
    }

    override suspend fun updateSettingValue(category: SettingsCategory, key: String, value: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.updateSettingValue(
                value = value,
                updated_at = System.currentTimeMillis(),
                category = category.name,
                key = key
            )
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error updating setting value in local database: $category/$key" }
            Result.failure(e)
        }
    }

    override suspend fun deleteSetting(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteSetting(id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting setting from local database: $id" }
            Result.failure(e)
        }
    }

    override suspend fun deleteSettingByCategoryAndKey(category: SettingsCategory, key: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteSettingByCategoryAndKey(category.name, key)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting setting by category and key from local database: $category/$key" }
            Result.failure(e)
        }
    }

    override suspend fun deleteSettingsByCategory(category: SettingsCategory): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteSettingsByCategory(category.name)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting settings by category from local database: $category" }
            Result.failure(e)
        }
    }

    override suspend fun deleteAllSettings(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val allSettings = database.cameraDatabaseQueries.selectAllSettings().executeAsList()
            database.cameraDatabaseQueries.transaction {
                allSettings.forEach { setting ->
                    database.cameraDatabaseQueries.deleteSettingByCategoryAndKey(setting.category, setting.key)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting all settings from local database" }
            Result.failure(e)
        }
    }

    override suspend fun settingExists(id: String): Boolean = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectSettingById(id).executeAsOneOrNull() != null
        } catch (e: Exception) {
            logger.error(e) { "Error checking setting existence in local database: $id" }
            false
        }
    }

    override suspend fun settingExists(category: SettingsCategory, key: String): Boolean = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectSettingByCategoryAndKey(category.name, key).executeAsOneOrNull() != null
        } catch (e: Exception) {
            logger.error(e) { "Error checking setting existence in local database: $category/$key" }
            false
        }
    }
}

