package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.SettingsEntityMapper
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SystemSettings
import com.company.ipcamera.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация SettingsRepository с использованием SQLDelight
 */
class SettingsRepositoryImplSqlDelight(
    private val databaseFactory: DatabaseFactory
) : SettingsRepository {

    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = SettingsEntityMapper()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun getSettings(category: SettingsCategory?): List<Settings> = withContext(Dispatchers.Default) {
        try {
            val dbSettings = if (category != null) {
                database.cameraDatabaseQueries
                    .selectSettingsByCategory(category.name)
                    .executeAsList()
            } else {
                database.cameraDatabaseQueries
                    .selectAllSettings()
                    .executeAsList()
            }
            dbSettings.map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting settings" }
            emptyList()
        }
    }

    override suspend fun getSetting(key: String): Settings? = withContext(Dispatchers.Default) {
        try {
            // Ищем настройку по ключу во всех категориях
            val allSettings = database.cameraDatabaseQueries
                .selectAllSettings()
                .executeAsList()
            allSettings.find { it.key == key }?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting setting: $key" }
            null
        }
    }

    override suspend fun updateSettings(settings: Map<String, String>): Result<Int> = withContext(Dispatchers.Default) {
        try {
            var updatedCount = 0
            val now = System.currentTimeMillis()

            settings.forEach { (key, value) ->
                // Ищем существующую настройку
                val allSettings = database.cameraDatabaseQueries
                    .selectAllSettings()
                    .executeAsList()

                val existingSetting = allSettings.find { it.key == key }
                if (existingSetting != null) {
                    // Обновляем существующую настройку
                    database.cameraDatabaseQueries.updateSettingValue(
                        value = value,
                        updated_at = now,
                        category = existingSetting.category,
                        key = key
                    )
                    updatedCount++
                } else {
                    // Настройка не найдена - возвращаем ошибку
                    logger.warn { "Setting not found: $key" }
                }
            }

            Result.success(updatedCount)
        } catch (e: Exception) {
            logger.error(e) { "Error updating settings" }
            Result.failure(e)
        }
    }

    override suspend fun updateSetting(key: String, value: String): Result<Settings> = withContext(Dispatchers.Default) {
        try {
            val now = System.currentTimeMillis()

            // Ищем существующую настройку
            val allSettings = database.cameraDatabaseQueries
                .selectAllSettings()
                .executeAsList()

            val existingSetting = allSettings.find { it.key == key }
            if (existingSetting != null) {
                database.cameraDatabaseQueries.updateSettingValue(
                    value = value,
                    updated_at = now,
                    category = existingSetting.category,
                    key = key
                )
                getSetting(key)?.let { Result.success(it) }
                    ?: Result.failure(IllegalArgumentException("Setting not found after update: $key"))
            } else {
                Result.failure(IllegalArgumentException("Setting not found: $key"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating setting: $key" }
            Result.failure(e)
        }
    }

    override suspend fun deleteSetting(key: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // Ищем настройку по ключу
            val allSettings = database.cameraDatabaseQueries
                .selectAllSettings()
                .executeAsList()

            val existingSetting = allSettings.find { it.key == key }
            if (existingSetting != null) {
                database.cameraDatabaseQueries.deleteSettingByCategoryAndKey(
                    category = existingSetting.category,
                    key = key
                )
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Setting not found: $key"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting setting: $key" }
            Result.failure(e)
        }
    }

    override suspend fun getSystemSettings(): SystemSettings? = withContext(Dispatchers.Default) {
        try {
            val settingsMap = getSettings().associateBy { it.key }

            // Создаем SystemSettings из отдельных настроек
            // Это упрощенная версия - для полноценной реализации нужно правильно парсить значения
            SystemSettings(
                recording = settingsMap["recording.defaultQuality"]?.let {
                    com.company.ipcamera.shared.domain.model.RecordingSystemSettings(
                        defaultQuality = try {
                            com.company.ipcamera.shared.domain.model.Quality.valueOf(it.value)
                        } catch (e: Exception) {
                            com.company.ipcamera.shared.domain.model.Quality.HIGH
                        },
                        defaultFormat = settingsMap["recording.defaultFormat"]?.let { formatSetting ->
                            try {
                                com.company.ipcamera.shared.domain.model.RecordingFormat.valueOf(formatSetting.value)
                            } catch (e: Exception) {
                                com.company.ipcamera.shared.domain.model.RecordingFormat.MP4
                            }
                        } ?: com.company.ipcamera.shared.domain.model.RecordingFormat.MP4,
                        maxDuration = settingsMap["recording.maxDuration"]?.value?.toLongOrNull() ?: 3600,
                        autoDelete = settingsMap["recording.autoDelete"]?.value?.toBoolean() ?: true,
                        retentionDays = settingsMap["recording.retentionDays"]?.value?.toIntOrNull() ?: 30
                    )
                },
                storage = settingsMap["storage.maxStorageSize"]?.let {
                    com.company.ipcamera.shared.domain.model.StorageSettings(
                        maxStorageSize = it.value.toLongOrNull() ?: 0L,
                        currentStorageUsed = settingsMap["storage.currentStorageUsed"]?.value?.toLongOrNull() ?: 0L,
                        storagePath = settingsMap["storage.storagePath"]?.value ?: "",
                        autoCleanup = settingsMap["storage.autoCleanup"]?.value?.toBoolean() ?: true
                    )
                },
                notifications = settingsMap["notifications.emailEnabled"]?.let {
                    com.company.ipcamera.shared.domain.model.NotificationSystemSettings(
                        emailEnabled = it.value.toBoolean(),
                        smsEnabled = settingsMap["notifications.smsEnabled"]?.value?.toBoolean() ?: false,
                        pushEnabled = settingsMap["notifications.pushEnabled"]?.value?.toBoolean() ?: true,
                        webhookUrl = settingsMap["notifications.webhookUrl"]?.value
                    )
                },
                security = settingsMap["security.requireAuth"]?.let {
                    com.company.ipcamera.shared.domain.model.SecuritySettings(
                        requireAuth = it.value.toBoolean(),
                        sessionTimeout = settingsMap["security.sessionTimeout"]?.value?.toLongOrNull() ?: 3600,
                        passwordPolicy = settingsMap["security.passwordPolicy"]?.value?.let { policyJson ->
                            try {
                                json.decodeFromString<com.company.ipcamera.shared.domain.model.PasswordPolicy>(policyJson)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    )
                },
                network = settingsMap["network.apiPort"]?.let {
                    com.company.ipcamera.shared.domain.model.NetworkSettings(
                        apiPort = it.value.toIntOrNull() ?: 8080,
                        websocketPort = settingsMap["network.websocketPort"]?.value?.toIntOrNull() ?: 8081,
                        allowRemoteAccess = settingsMap["network.allowRemoteAccess"]?.value?.toBoolean() ?: false,
                        sslEnabled = settingsMap["network.sslEnabled"]?.value?.toBoolean() ?: false
                    )
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting system settings" }
            null
        }
    }

    override suspend fun updateSystemSettings(settings: SystemSettings): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val now = System.currentTimeMillis()

            // Сохраняем системные настройки как отдельные записи
            // Это упрощенная версия - для полноценной реализации нужно правильно сериализовать значения

            settings.recording?.let { recording ->
                updateSetting("recording.defaultQuality", recording.defaultQuality.name)
                updateSetting("recording.defaultFormat", recording.defaultFormat.name)
                updateSetting("recording.maxDuration", recording.maxDuration.toString())
                updateSetting("recording.autoDelete", recording.autoDelete.toString())
                updateSetting("recording.retentionDays", recording.retentionDays.toString())
            }

            settings.storage?.let { storage ->
                updateSetting("storage.maxStorageSize", storage.maxStorageSize.toString())
                updateSetting("storage.currentStorageUsed", storage.currentStorageUsed.toString())
                updateSetting("storage.storagePath", storage.storagePath)
                updateSetting("storage.autoCleanup", storage.autoCleanup.toString())
            }

            settings.notifications?.let { notifications ->
                updateSetting("notifications.emailEnabled", notifications.emailEnabled.toString())
                updateSetting("notifications.smsEnabled", notifications.smsEnabled.toString())
                updateSetting("notifications.pushEnabled", notifications.pushEnabled.toString())
                notifications.webhookUrl?.let {
                    updateSetting("notifications.webhookUrl", it)
                }
            }

            settings.security?.let { security ->
                updateSetting("security.requireAuth", security.requireAuth.toString())
                updateSetting("security.sessionTimeout", security.sessionTimeout.toString())
                security.passwordPolicy?.let { policy ->
                    updateSetting("security.passwordPolicy", json.encodeToString(policy))
                }
            }

            settings.network?.let { network ->
                updateSetting("network.apiPort", network.apiPort.toString())
                updateSetting("network.websocketPort", network.websocketPort.toString())
                updateSetting("network.allowRemoteAccess", network.allowRemoteAccess.toString())
                updateSetting("network.sslEnabled", network.sslEnabled.toString())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error updating system settings" }
            Result.failure(e)
        }
    }

    override suspend fun resetSettings(category: SettingsCategory?): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            if (category != null) {
                database.cameraDatabaseQueries.deleteSettingsByCategory(category.name)
            } else {
                // Удаляем все настройки
                val allSettings = database.cameraDatabaseQueries
                    .selectAllSettings()
                    .executeAsList()
                allSettings.forEach { setting ->
                    database.cameraDatabaseQueries.deleteSettingByCategoryAndKey(
                        category = setting.category,
                        key = setting.key
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error resetting settings" }
            Result.failure(e)
        }
    }

    override suspend fun exportSettings(): Result<Map<String, String>> = withContext(Dispatchers.Default) {
        try {
            val settings = getSettings()
            val settingsMap = settings.associate { it.key to it.value }
            Result.success(settingsMap)
        } catch (e: Exception) {
            logger.error(e) { "Error exporting settings" }
            Result.failure(e)
        }
    }

    override suspend fun importSettings(settings: Map<String, String>): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            updateSettings(settings)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error importing settings" }
            Result.failure(e)
        }
    }
}


