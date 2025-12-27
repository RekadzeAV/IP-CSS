package com.company.ipcamera.shared.data.local

import com.company.ipcamera.shared.database.Setting as DbSetting
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SettingsType

/**
 * Маппер между сущностью базы данных и доменной моделью Settings
 */
internal class SettingsEntityMapper {

    fun toDomain(dbSetting: DbSetting): Settings {
        return Settings(
            id = dbSetting.id,
            category = SettingsCategory.valueOf(dbSetting.category),
            key = dbSetting.key,
            value = dbSetting.value,
            type = SettingsType.valueOf(dbSetting.type),
            description = dbSetting.description,
            updatedAt = dbSetting.updated_at
        )
    }

    fun toDatabase(settings: Settings): DbSetting {
        return DbSetting(
            id = settings.id,
            category = settings.category.name,
            key = settings.key,
            value = settings.value,
            type = settings.type.name,
            description = settings.description,
            updated_at = settings.updatedAt
        )
    }
}

