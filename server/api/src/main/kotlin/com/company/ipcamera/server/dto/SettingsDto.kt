package com.company.ipcamera.server.dto

import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SystemSettings
import kotlinx.serialization.Serializable

/**
 * DTO для ответа с настройкой
 */
@Serializable
data class SettingsDto(
    val id: String,
    val category: String,
    val key: String,
    val value: String,
    val type: String,
    val description: String? = null,
    val updatedAt: Long
)

/**
 * DTO для обновления настроек
 */
@Serializable
data class UpdateSettingsRequest(
    val settings: Map<String, String>
)

/**
 * DTO для обновления одной настройки
 */
@Serializable
data class UpdateSettingRequest(
    val value: String
)

/**
 * Extension функции для конвертации
 */
fun Settings.toDto(): SettingsDto {
    return SettingsDto(
        id = this.id,
        category = this.category.name,
        key = this.key,
        value = this.value,
        type = this.type.name,
        description = this.description,
        updatedAt = this.updatedAt
    )
}

