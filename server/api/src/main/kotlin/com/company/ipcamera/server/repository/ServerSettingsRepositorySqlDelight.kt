package com.company.ipcamera.server.repository

import com.company.ipcamera.server.service.StorageService
import com.company.ipcamera.shared.database.CameraDatabase
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SettingsType
import com.company.ipcamera.shared.domain.model.SystemSettings
import com.company.ipcamera.shared.domain.model.RecordingSystemSettings
import com.company.ipcamera.shared.domain.model.StorageSettings
import com.company.ipcamera.shared.domain.model.NotificationSystemSettings
import com.company.ipcamera.shared.domain.model.SecuritySettings
import com.company.ipcamera.shared.domain.model.PasswordPolicy
import com.company.ipcamera.shared.domain.model.NetworkSettings
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Серверная реализация SettingsRepository с использованием SQLDelight / PostgreSQL.
 * Использует общий экземпляр CameraDatabase (один пул соединений).
 *
 * При первом запуске (пустая таблица setting) заполняется дефолтными значениями.
 */
class ServerSettingsRepositorySqlDelight(
    private val database: CameraDatabase,
    private val storageService: StorageService? = null
) : SettingsRepository {

    private fun mapRow(row: com.company.ipcamera.shared.database.Setting): Settings = Settings(
        id = row.id,
        category = runCatching { SettingsCategory.valueOf(row.category) }.getOrDefault(SettingsCategory.OTHER),
        key = row.key,
        value = row.value_,
        type = runCatching { SettingsType.valueOf(row.type) }.getOrDefault(SettingsType.STRING),
        description = row.description,
        updatedAt = row.updated_at
    )

    private val seeded = java.util.concurrent.atomic.AtomicBoolean(false)
    private val seedMutex = Mutex()

    /** Ленивая инициализация дефолтов (первый вызов любого метода). */
    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedMutex.withLock {
            if (seeded.get()) return@withLock
            try {
                seedDefaultSettingsIfEmpty()
            } catch (e: Exception) {
                logger.error(e) { "Failed to seed default settings (c=${'$'}{defaultSettings().size})" }
                throw e
            }
            seeded.set(true)
        }
    }

    private fun defaultSettings(): List<Settings> {
        val now = System.currentTimeMillis()
        return listOf(
            Settings(UUID.randomUUID().toString(), SettingsCategory.RECORDING, "default_quality", "HIGH", SettingsType.STRING, "Default recording quality", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.RECORDING, "default_format", "MP4", SettingsType.STRING, "Default recording format", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.RECORDING, "max_duration", "3600", SettingsType.INTEGER, "Maximum recording duration in seconds", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.RECORDING, "auto_delete", "true", SettingsType.BOOLEAN, "Automatically delete old recordings", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.RECORDING, "retention_days", "30", SettingsType.INTEGER, "Number of days to retain recordings", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.STORAGE, "max_storage_size", "107374182400", SettingsType.INTEGER, "Maximum storage size in bytes", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.STORAGE, "storage_path", "recordings", SettingsType.STRING, "Path to storage directory", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.STORAGE, "auto_cleanup", "true", SettingsType.BOOLEAN, "Automatically cleanup old files", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.SECURITY, "require_auth", "true", SettingsType.BOOLEAN, "Require authentication", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.SECURITY, "session_timeout", "3600", SettingsType.INTEGER, "Session timeout in seconds", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.NOTIFICATIONS, "email_enabled", "false", SettingsType.BOOLEAN, "Enable email notifications", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.NOTIFICATIONS, "sms_enabled", "false", SettingsType.BOOLEAN, "Enable SMS notifications", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.NOTIFICATIONS, "push_enabled", "true", SettingsType.BOOLEAN, "Enable push notifications", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.NETWORK, "api_port", "8080", SettingsType.INTEGER, "API server port", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.NETWORK, "websocket_port", "8081", SettingsType.INTEGER, "WebSocket server port", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.NETWORK, "allow_remote_access", "false", SettingsType.BOOLEAN, "Allow remote access", now),
            Settings(UUID.randomUUID().toString(), SettingsCategory.NETWORK, "ssl_enabled", "false", SettingsType.BOOLEAN, "Enable SSL/TLS", now)
        )
    }

    private suspend fun insertSettingRow(s: Settings) {
        database.cameraDatabaseQueries.insertSetting(
            id = s.id,
            category = s.category.name,
            key = s.key,
            value_ = s.value,
            type = s.type.name,
            description = s.description,
            updated_at = s.updatedAt
        )
    }

    private suspend fun seedDefaultSettingsIfEmpty() {
        val existing = database.cameraDatabaseQueries.selectAllSettings().executeAsList()
        if (existing.isNotEmpty()) return
        val defaults = defaultSettings()
        defaults.forEach { insertSettingRow(it) }
        logger.info { "Default settings seeded: ${defaults.size} entries" }
    }

    override suspend fun getSettings(category: SettingsCategory?): List<Settings> = withContext(Dispatchers.IO) {
        try {
            ensureSeeded()
            val rows = if (category != null) {
                database.cameraDatabaseQueries.selectSettingsByCategory(category.name).executeAsList()
            } else {
                database.cameraDatabaseQueries.selectAllSettings().executeAsList()
            }
            rows.map { mapRow(it) }.sortedBy { it.key }
        } catch (e: Exception) {
            logger.error(e) { "Error getting settings (category=$category)" }
            emptyList()
        }
    }

    override suspend fun getSetting(key: String): Settings? = withContext(Dispatchers.IO) {
        try {
            ensureSeeded()
            database.cameraDatabaseQueries.selectSettingByKey(key)
                .executeAsOneOrNull()
                ?.let { mapRow(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting setting by key: $key" }
            null
        }
    }

    override suspend fun updateSettings(settingsMap: Map<String, String>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            ensureSeeded()
            var updatedCount = 0
            for ((key, value) in settingsMap) {
                upsertSetting(key, value)
                updatedCount++
            }
            logger.info { "Settings updated: $updatedCount settings" }
            Result.success(updatedCount)
        } catch (e: Exception) {
            logger.error(e) { "Error updating settings" }
            Result.failure(e)
        }
    }

    override suspend fun updateSetting(key: String, value: String): Result<Settings> = withContext(Dispatchers.IO) {
        try {
            ensureSeeded()
            val result = upsertSetting(key, value)
            logger.info { "Setting updated: $key = $value" }
            Result.success(result)
        } catch (e: Exception) {
            logger.error(e) { "Error updating setting: $key" }
            Result.failure(e)
        }
    }

    private suspend fun upsertSetting(key: String, value: String): Settings {
        val now = System.currentTimeMillis()
        val existing = database.cameraDatabaseQueries.selectSettingByKey(key).executeAsOneOrNull()
        return if (existing != null) {
            database.cameraDatabaseQueries.updateSettingValue(
                value_ = value,
                updated_at = now,
                category = existing.category,
                key = existing.key
            )
            mapRow(existing.copy(value_ = value, updated_at = now))
        } else {
            val newSetting = Settings(
                id = UUID.randomUUID().toString(),
                category = SettingsCategory.OTHER,
                key = key,
                value = value,
                updatedAt = now
            )
            insertSettingRow(newSetting)
            newSetting
        }
    }

    override suspend fun deleteSetting(key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureSeeded()
            val existing = database.cameraDatabaseQueries.selectSettingByKey(key).executeAsOneOrNull()
            if (existing == null) {
                Result.failure(IllegalArgumentException("Setting not found: $key"))
            } else {
                database.cameraDatabaseQueries.deleteSetting(existing.id)
                logger.info { "Setting deleted: $key" }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting setting: $key" }
            Result.failure(e)
        }
    }

    // region — SystemSettings (агрегированное представление)

    private suspend fun value(key: String): String? =
        database.cameraDatabaseQueries.selectSettingByKey(key).executeAsOneOrNull()?.value_

    private suspend inline fun <reified T : Any> parsed(key: String, parser: (String) -> T?): T? =
        value(key)?.let(parser)

    override suspend fun getSystemSettings(): SystemSettings? = withContext(Dispatchers.IO) {
        try {
            ensureSeeded()
            SystemSettings(
                recording = RecordingSystemSettings(
                    defaultQuality = parsed("default_quality", Quality::valueOf) ?: Quality.HIGH,
                    defaultFormat = parsed("default_format", RecordingFormat::valueOf) ?: RecordingFormat.MP4,
                    maxDuration = parsed("max_duration", String::toLongOrNull) ?: 3600L,
                    autoDelete = parsed("auto_delete", String::toBoolean) ?: true,
                    retentionDays = parsed("retention_days", String::toIntOrNull) ?: 30
                ),
                storage = StorageSettings(
                    maxStorageSize = parsed("max_storage_size", String::toLongOrNull) ?: 0L,
                    storagePath = value("storage_path") ?: "recordings",
                    currentStorageUsed = storageService?.getUsedSpace() ?: 0L,
                    autoCleanup = parsed("auto_cleanup", String::toBoolean) ?: true
                ),
                notifications = NotificationSystemSettings(
                    emailEnabled = parsed("email_enabled", String::toBoolean) ?: false,
                    smsEnabled = parsed("sms_enabled", String::toBoolean) ?: false,
                    pushEnabled = parsed("push_enabled", String::toBoolean) ?: true,
                    webhookUrl = value("webhook_url")
                ),
                security = SecuritySettings(
                    requireAuth = parsed("require_auth", String::toBoolean) ?: true,
                    sessionTimeout = parsed("session_timeout", String::toLongOrNull) ?: 3600L,
                    passwordPolicy = PasswordPolicy(
                        minLength = parsed("password_min_length", String::toIntOrNull) ?: 8,
                        requireUppercase = parsed("password_require_uppercase", String::toBoolean) ?: true,
                        requireLowercase = parsed("password_require_lowercase", String::toBoolean) ?: true,
                        requireNumbers = parsed("password_require_numbers", String::toBoolean) ?: true,
                        requireSpecialChars = parsed("password_require_special_chars", String::toBoolean) ?: false
                    )
                ),
                network = NetworkSettings(
                    apiPort = parsed("api_port", String::toIntOrNull) ?: 8080,
                    websocketPort = parsed("websocket_port", String::toIntOrNull) ?: 8081,
                    allowRemoteAccess = parsed("allow_remote_access", String::toBoolean) ?: false,
                    sslEnabled = parsed("ssl_enabled", String::toBoolean) ?: false
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting system settings" }
            null
        }
    }

    override suspend fun updateSystemSettings(systemSettings: SystemSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureSeeded()
            systemSettings.recording?.let { recording ->
                upsertSetting("default_quality", recording.defaultQuality.name)
                upsertSetting("default_format", recording.defaultFormat.name)
                upsertSetting("max_duration", recording.maxDuration.toString())
                upsertSetting("auto_delete", recording.autoDelete.toString())
                upsertSetting("retention_days", recording.retentionDays.toString())
            }

            systemSettings.storage?.let { storage ->
                upsertSetting("max_storage_size", storage.maxStorageSize.toString())
                upsertSetting("storage_path", storage.storagePath)
                upsertSetting("auto_cleanup", storage.autoCleanup.toString())
            }

            systemSettings.notifications?.let { notifications ->
                upsertSetting("email_enabled", notifications.emailEnabled.toString())
                upsertSetting("sms_enabled", notifications.smsEnabled.toString())
                upsertSetting("push_enabled", notifications.pushEnabled.toString())
                notifications.webhookUrl?.let { upsertSetting("webhook_url", it) }
            }

            systemSettings.security?.let { security ->
                upsertSetting("require_auth", security.requireAuth.toString())
                upsertSetting("session_timeout", security.sessionTimeout.toString())
                security.passwordPolicy?.let { policy ->
                    upsertSetting("password_min_length", policy.minLength.toString())
                    upsertSetting("password_require_uppercase", policy.requireUppercase.toString())
                    upsertSetting("password_require_lowercase", policy.requireLowercase.toString())
                    upsertSetting("password_require_numbers", policy.requireNumbers.toString())
                    upsertSetting("password_require_special_chars", policy.requireSpecialChars.toString())
                }
            }

            systemSettings.network?.let { network ->
                upsertSetting("api_port", network.apiPort.toString())
                upsertSetting("websocket_port", network.websocketPort.toString())
                upsertSetting("allow_remote_access", network.allowRemoteAccess.toString())
                upsertSetting("ssl_enabled", network.sslEnabled.toString())
            }

            logger.info { "System settings updated successfully" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error updating system settings" }
            Result.failure(e)
        }
    }


    override suspend fun resetSettings(category: SettingsCategory?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureSeeded()
            if (category != null) {
                database.cameraDatabaseQueries.deleteSettingsByCategory(category.name)
                defaultSettings().filter { it.category == category }.forEach { insertSettingRow(it) }
            } else {
                database.cameraDatabaseQueries.deleteAllSettings()
                defaultSettings().forEach { insertSettingRow(it) }
            }
            logger.info { "Settings reset for category: ${category ?: "all"}" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error resetting settings" }
            Result.failure(e)
        }
    }

    override suspend fun exportSettings(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            ensureSeeded()
            val exported = database.cameraDatabaseQueries.selectAllSettings()
                .executeAsList()
                .associate { it.key to it.value_ }
            logger.info { "Settings exported: ${exported.size} settings" }
            Result.success(exported)
        } catch (e: Exception) {
            logger.error(e) { "Error exporting settings" }
            Result.failure(e)
        }
    }

    override suspend fun importSettings(settingsMap: Map<String, String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureSeeded()
            for ((key, value) in settingsMap) {
                upsertSetting(key, value)
            }
            logger.info { "Settings imported: ${settingsMap.size} settings" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error importing settings" }
            Result.failure(e)
        }
    }
}
