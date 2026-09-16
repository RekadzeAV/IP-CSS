package com.company.ipcamera.shared.data.datasource.local.impl

import com.company.ipcamera.shared.data.datasource.local.SettingsLocalDataSource
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.SettingsEntityMapper
import com.company.ipcamera.shared.data.local.createDatabaseSync
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SettingsType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация SettingsLocalDataSource с использованием SQLDelight
 */
class SettingsLocalDataSourceImpl(
    private val databaseFactory: DatabaseFactory,
) : SettingsLocalDataSource {
    private val database = createDatabaseSync(databaseFactory.createDriver())
    private val mapper = SettingsEntityMapper()

    override suspend fun getSettings(): List<Settings> =
        withContext(Dispatchers.Default) {
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

    override suspend fun getSettingById(id: String): Settings? =
        withContext(Dispatchers.Default) {
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

    override suspend fun getSettingsByCategory(category: SettingsCategory): List<Settings> =
        withContext(
            Dispatchers.Default,
        ) {
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

    override suspend fun getSettingByCategoryAndKey(
        category: SettingsCategory,
        key: String,
    ): Settings? =
        withContext(
            Dispatchers.Default,
        ) {
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

    override suspend fun getSettingByKey(key: String): Settings? =
        withContext(Dispatchers.Default) {
            try {
                // Ищем настройку по ключу во всех категориях
                val allSettings =
                    database.cameraDatabaseQueries
                        .selectAllSettings()
                        .executeAsList()
                allSettings.find { it.key == key }?.let { mapper.toDomain(it) }
            } catch (e: Exception) {
                logger.error(e) { "Error getting setting by key from local database: $key" }
                null
            }
        }

    override suspend fun saveSetting(setting: Settings): Result<Settings> =
        withContext(Dispatchers.Default) {
            upsertSetting(setting)
        }

    override suspend fun saveSettings(settings: List<Settings>): Result<List<Settings>> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                database.cameraDatabaseQueries.transaction {
                    settings.forEach { setting ->
                        val dbSetting = mapper.toDatabase(setting)
                        database.cameraDatabaseQueries.insertSetting(
                            id = dbSetting.id,
                            category = dbSetting.category,
                            key = dbSetting.key,
                            value_ = dbSetting.value_,
                            type = dbSetting.type,
                            description = dbSetting.description,
                            updated_at = dbSetting.updated_at,
                        )
                    }
                }
                Result.success(settings)
            } catch (e: Exception) {
                logger.error(e) { "Error saving settings to local database" }
                Result.failure(e)
            }
        }

    override suspend fun updateSetting(setting: Settings): Result<Settings> =
        withContext(Dispatchers.Default) {
            // INSERT OR REPLACE (см. insertSetting в .sq): чистый UPDATE не вставляет строку, если её ещё нет.
            upsertSetting(setting)
        }

    private suspend fun upsertSetting(setting: Settings): Result<Settings> {
        return try {
            val dbSetting = mapper.toDatabase(setting)
            database.cameraDatabaseQueries.insertSetting(
                id = dbSetting.id,
                category = dbSetting.category,
                key = dbSetting.key,
                value_ = dbSetting.value_,
                type = dbSetting.type,
                description = dbSetting.description,
                updated_at = dbSetting.updated_at,
            )
            Result.success(setting)
        } catch (e: Exception) {
            logger.error(e) { "Error upserting setting in local database: ${setting.id}" }
            Result.failure(e)
        }
    }

    override suspend fun updateSettingValue(
        category: SettingsCategory,
        key: String,
        value: String,
    ): Result<Unit> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val existing =
                    database.cameraDatabaseQueries
                        .selectSettingByCategoryAndKey(category.name, key)
                        .executeAsOneOrNull()
                if (existing != null) {
                    database.cameraDatabaseQueries.updateSettingValue(
                        value_ = value,
                        updated_at = now,
                        category = category.name,
                        key = key,
                    )
                } else {
                    val setting =
                        Settings(
                            id = stableSettingId(category, key),
                            category = category,
                            key = key,
                            value = value,
                            type = SettingsType.STRING,
                            description = null,
                            updatedAt = now,
                        )
                    upsertSetting(setting).getOrElse { return@withContext Result.failure(it) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error updating setting value in local database: $category/$key" }
                Result.failure(e)
            }
        }

    override suspend fun updateSettings(settings: Map<String, String>): Result<Int> =
        withContext(Dispatchers.Default) {
            try {
                var updatedCount = 0
                val now = Clock.System.now().toEpochMilliseconds()
                for ((key, value) in settings) {
                    val existing = getSettingByKey(key)
                    if (existing != null) {
                        upsertSetting(existing.copy(value = value, updatedAt = now)).getOrElse {
                            return@withContext Result.failure(
                                it,
                            )
                        }
                    } else {
                        val category = categoryFromDottedKey(key)
                        val newSetting =
                            Settings(
                                id = stableSettingId(category, key),
                                category = category,
                                key = key,
                                value = value,
                                type = SettingsType.STRING,
                                description = null,
                                updatedAt = now,
                            )
                        upsertSetting(newSetting).getOrElse { return@withContext Result.failure(it) }
                    }
                    updatedCount++
                }
                Result.success(updatedCount)
            } catch (e: Exception) {
                logger.error(e) { "Error updating settings in local database" }
                Result.failure(e)
            }
        }

    /**
     * Категория по префиксу ключа (`recording.*` → RECORDING). Если префикс неизвестен — OTHER.
     */
    private fun categoryFromDottedKey(key: String): SettingsCategory {
        val prefix = key.substringBefore('.').lowercase()
        return when (prefix) {
            "recording" -> SettingsCategory.RECORDING
            "storage" -> SettingsCategory.STORAGE
            "notifications" -> SettingsCategory.NOTIFICATIONS
            "security" -> SettingsCategory.SECURITY
            "network" -> SettingsCategory.NETWORK
            "system" -> SettingsCategory.SYSTEM
            "analytics" -> SettingsCategory.ANALYTICS
            else -> SettingsCategory.OTHER
        }
    }

    private fun stableSettingId(
        category: SettingsCategory,
        key: String,
    ): String {
        return "${category.name}_${key.replace('.', '_')}"
    }

    override suspend fun deleteSetting(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.deleteSetting(id)
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error deleting setting from local database: $id" }
                Result.failure(e)
            }
        }

    override suspend fun deleteSettingByCategoryAndKey(
        category: SettingsCategory,
        key: String,
    ): Result<Unit> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                database.cameraDatabaseQueries.deleteSettingByCategoryAndKey(category.name, key)
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error deleting setting by category and key from local database: $category/$key" }
                Result.failure(e)
            }
        }

    override suspend fun deleteSettingsByCategory(category: SettingsCategory): Result<Unit> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                database.cameraDatabaseQueries.deleteSettingsByCategory(category.name)
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error deleting settings by category from local database: $category" }
                Result.failure(e)
            }
        }

    override suspend fun deleteAllSettings(): Result<Unit> =
        withContext(Dispatchers.Default) {
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

    override suspend fun settingExists(id: String): Boolean =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.selectSettingById(id).executeAsOneOrNull() != null
            } catch (e: Exception) {
                logger.error(e) { "Error checking setting existence in local database: $id" }
                false
            }
        }

    override suspend fun settingExists(
        category: SettingsCategory,
        key: String,
    ): Boolean =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                database.cameraDatabaseQueries.selectSettingByCategoryAndKey(category.name, key).executeAsOneOrNull() != null
            } catch (e: Exception) {
                logger.error(e) { "Error checking setting existence in local database: $category/$key" }
                false
            }
        }
}
