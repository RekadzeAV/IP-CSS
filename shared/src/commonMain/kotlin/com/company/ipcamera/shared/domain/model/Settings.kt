package com.company.ipcamera.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Модель настройки приложения
 */
@Serializable
data class Settings(
    val id: String,
    val category: SettingsCategory,
    val key: String,
    val value: String,
    val type: SettingsType = SettingsType.STRING,
    val description: String? = null,
    val updatedAt: Long
)

@Serializable
enum class SettingsCategory {
    RECORDING,
    STORAGE,
    NOTIFICATIONS,
    SECURITY,
    NETWORK,
    SYSTEM,
    ANALYTICS,
    OTHER
}

@Serializable
enum class SettingsType {
    STRING,
    INTEGER,
    BOOLEAN,
    FLOAT,
    JSON
}

/**
 * Системные настройки приложения
 */
@Serializable
data class SystemSettings(
    val recording: RecordingSystemSettings? = null,
    val storage: StorageSettings? = null,
    val notifications: NotificationSystemSettings? = null,
    val security: SecuritySettings? = null,
    val network: NetworkSettings? = null
)

@Serializable
data class RecordingSystemSettings(
    val defaultQuality: Quality = Quality.HIGH,
    val defaultFormat: RecordingFormat = RecordingFormat.MP4,
    val maxDuration: Long = 3600, // секунды
    val autoDelete: Boolean = true,
    val retentionDays: Int = 30
)

@Serializable
data class StorageSettings(
    val maxStorageSize: Long,
    val currentStorageUsed: Long,
    val storagePath: String,
    val autoCleanup: Boolean = true
)

@Serializable
data class NotificationSystemSettings(
    val emailEnabled: Boolean = false,
    val smsEnabled: Boolean = false,
    val pushEnabled: Boolean = true,
    val webhookUrl: String? = null
)

@Serializable
data class SecuritySettings(
    val requireAuth: Boolean = true,
    val sessionTimeout: Long = 3600,
    val passwordPolicy: PasswordPolicy? = null
)

@Serializable
data class PasswordPolicy(
    val minLength: Int = 8,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireNumbers: Boolean = true,
    val requireSpecialChars: Boolean = false
)

@Serializable
data class NetworkSettings(
    val apiPort: Int = 8080,
    val websocketPort: Int = 8081,
    val allowRemoteAccess: Boolean = false,
    val sslEnabled: Boolean = false
)
