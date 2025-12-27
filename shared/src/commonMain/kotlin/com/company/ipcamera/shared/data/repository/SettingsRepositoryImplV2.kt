package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.data.datasource.local.SettingsLocalDataSource
import com.company.ipcamera.shared.data.datasource.remote.SettingsRemoteDataSource
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SystemSettings
import com.company.ipcamera.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация SettingsRepository с использованием Data Sources (новая архитектура)
 *
 * Использует стратегию local-first: сначала проверяет локальную БД,
 * затем синхронизирует с удаленным API при необходимости
 */
class SettingsRepositoryImplV2(
    private val localDataSource: SettingsLocalDataSource,
    private val remoteDataSource: SettingsRemoteDataSource? = null
) : SettingsRepository {

    override suspend fun getSettings(category: SettingsCategory?): List<Settings> = withContext(Dispatchers.Default) {
        try {
            // Получаем настройки локально
            val localSettings = if (category != null) {
                localDataSource.getSettingsByCategory(category)
            } else {
                localDataSource.getSettings()
            }

            // Если локально пусто или есть удаленный источник, синхронизируем
            if ((localSettings.isEmpty() || remoteDataSource != null) && remoteDataSource != null) {
                remoteDataSource.getSettings(category).fold(
                    onSuccess = { remoteSettings ->
                        // Сохраняем в локальную БД для кэширования
                        if (remoteSettings.isNotEmpty()) {
                            localDataSource.saveSettings(remoteSettings).getOrNull()
                        }
                        remoteSettings
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to get settings from remote, using local" }
                        localSettings
                    }
                )
            } else {
                localSettings
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting settings" }
            emptyList()
        }
    }

    override suspend fun getSetting(key: String): Settings? = withContext(Dispatchers.Default) {
        try {
            // Сначала проверяем локально
            localDataSource.getSettingByKey(key) ?: run {
                // Если не найдено локально, пытаемся получить с сервера
                remoteDataSource?.getSetting(key)?.fold(
                    onSuccess = { setting ->
                        // Сохраняем в локальную БД
                        localDataSource.saveSetting(setting).getOrNull()
                        setting
                    },
                    onError = {
                        logger.warn(it) { "Failed to get setting from remote: $key" }
                        null
                    }
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting setting: $key" }
            null
        }
    }

    override suspend fun updateSettings(settings: Map<String, String>): Result<Int> = withContext(Dispatchers.Default) {
        try {
            // Обновляем локально
            val localResult = localDataSource.updateSettings(settings)

            // Если есть удаленный источник, синхронизируем
            if (remoteDataSource != null && localResult.isSuccess) {
                remoteDataSource.updateSettings(settings).fold(
                    onSuccess = { remoteCount ->
                        Result.success(remoteCount)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to sync settings update to remote, but updated locally" }
                        localResult
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating settings" }
            Result.failure(e)
        }
    }

    override suspend fun updateSetting(key: String, value: String): Result<Settings> = withContext(Dispatchers.Default) {
        try {
            // Получаем существующую настройку
            val existingSetting = localDataSource.getSettingByKey(key)
            if (existingSetting == null) {
                return@withContext Result.failure(Exception("Setting not found: $key"))
            }

            // Обновляем локально
            val updatedSetting = existingSetting.copy(value = value)
            val localResult = localDataSource.updateSetting(updatedSetting)

            // Если есть удаленный источник, синхронизируем
            if (remoteDataSource != null && localResult.isSuccess) {
                remoteDataSource.updateSetting(key, value).fold(
                    onSuccess = { remoteSetting ->
                        // Обновляем локальную версию данными с сервера
                        localDataSource.saveSetting(remoteSetting).getOrNull()
                        Result.success(remoteSetting)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to sync setting update to remote, but updated locally: $key" }
                        localResult
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating setting: $key" }
            Result.failure(e)
        }
    }

    override suspend fun deleteSetting(key: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // Находим настройку по ключу
            val setting = localDataSource.getSettingByKey(key)
            if (setting == null) {
                return@withContext Result.failure(Exception("Setting not found: $key"))
            }

            // Удаляем локально
            val localResult = localDataSource.deleteSetting(setting.id)

            // Если есть удаленный источник, синхронизируем
            if (remoteDataSource != null && localResult.isSuccess) {
                remoteDataSource.deleteSetting(key).fold(
                    onSuccess = {
                        Result.success(Unit)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to delete setting from remote, but deleted locally: $key" }
                        localResult
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting setting: $key" }
            Result.failure(e)
        }
    }

    override suspend fun getSystemSettings(): SystemSettings? = withContext(Dispatchers.Default) {
        try {
            // SystemSettings обычно хранятся как специальные настройки, для упрощения получаем с сервера
            // В реальной реализации можно парсить из локальных настроек
            remoteDataSource?.getSystemSettings()?.fold(
                onSuccess = { systemSettings ->
                    // TODO: Сохранить системные настройки локально если нужно
                    systemSettings
                },
                onError = {
                    logger.warn(it) { "Failed to get system settings from remote" }
                    null
                }
            ) ?: run {
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting system settings" }
            null
        }
    }

    override suspend fun updateSystemSettings(settings: SystemSettings): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // SystemSettings обновляются только через удаленный источник

            if (remoteDataSource != null) {
                remoteDataSource.updateSystemSettings(settings).fold(
                    onSuccess = {
                        Result.success(Unit)
                    },
                    onError = { error ->
                        logger.error(error) { "Error updating system settings" }
                        Result.failure(Exception(error.message))
                    }
                )
            } else {
                Result.failure(Exception("Remote data source not available for system settings"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating system settings" }
            Result.failure(e)
        }
    }

    override suspend fun resetSettings(category: SettingsCategory?): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // Сбрасываем настройки через удаленный источник (если доступен)
            if (remoteDataSource != null) {
                remoteDataSource.resetSettings(category).fold(
                    onSuccess = {
                        // После успешного сброса на сервере, обновляем локально
                        // Для этого получаем настройки с сервера
                        remoteDataSource.getSettings(category).fold(
                            onSuccess = { settings ->
                                localDataSource.saveSettings(settings).getOrNull()
                                Result.success(Unit)
                            },
                            onError = { error ->
                                logger.warn(error) { "Failed to sync settings after reset" }
                                Result.success(Unit)
                            }
                        )
                    },
                    onError = { error ->
                        logger.error(error) { "Error resetting settings" }
                        Result.failure(Exception(error.message))
                    }
                )
            } else {
                Result.failure(Exception("Remote data source not available for reset"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error resetting settings" }
            Result.failure(e)
        }
    }

    override suspend fun exportSettings(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            // Экспорт через удаленный источник
            if (remoteDataSource != null) {
                remoteDataSource.exportSettings().fold(
                    onSuccess = { settings ->
                        Result.success(settings)
                    },
                    onError = { error ->
                        logger.error(error) { "Error exporting settings" }
                        Result.failure(Exception(error.message))
                    }
                )
            } else {
                // Если удаленный источник недоступен, экспортируем локальные настройки
                val localSettings = localDataSource.getSettings()
                val settingsMap = localSettings.associate { it.key to it.value }
                Result.success(settingsMap)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error exporting settings" }
            Result.failure(e)
        }
    }

    override suspend fun importSettings(settings: Map<String, String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Обновляем локально
            val localResult = localDataSource.updateSettings(settings)

            // Если есть удаленный источник, синхронизируем
            if (remoteDataSource != null && localResult.isSuccess) {
                remoteDataSource.importSettings(settings).fold(
                    onSuccess = {
                        Result.success(Unit)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to import settings to remote, but imported locally" }
                        localResult.map { Unit }
                    }
                )
            } else {
                localResult.map { Unit }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error importing settings" }
            Result.failure(e)
        }
    }
}

