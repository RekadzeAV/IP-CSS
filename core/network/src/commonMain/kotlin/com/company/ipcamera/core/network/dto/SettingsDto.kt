package com.company.ipcamera.core.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO для настроек
 */
@Serializable
data class SettingsResponse(
    val id: String,
    val category: String,
    val key: String,
    val value: String,
    val type: String = "string",
    val description: String? = null,
    val updatedAt: Long
)

@Serializable
data class UpdateSettingsRequest(
    val settings: Map<String, String>
)

@Serializable
data class UpdateSettingsResponse(
    val success: Boolean,
    val updated: Int,
    val message: String? = null
)

@Serializable
data class SystemSettingsResponse(
    val recording: RecordingSystemSettings? = null,
    val storage: StorageSettings? = null,
    val notifications: NotificationSystemSettings? = null,
    val security: SecuritySettings? = null,
    val network: NetworkSettings? = null
)

@Serializable
data class RecordingSystemSettings(
    val defaultQuality: String = "HIGH",
    val defaultFormat: String = "mp4",
    val maxDuration: Long = 3600,
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



