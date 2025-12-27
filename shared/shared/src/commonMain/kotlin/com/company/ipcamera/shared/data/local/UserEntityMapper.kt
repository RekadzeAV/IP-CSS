package com.company.ipcamera.shared.data.local

import com.company.ipcamera.shared.database.User as DbUser
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Маппер между сущностью базы данных и доменной моделью User
 */
internal class UserEntityMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toDomain(dbUser: DbUser): User {
        return User(
            id = dbUser.id,
            username = dbUser.username,
            email = dbUser.email,
            fullName = dbUser.full_name,
            role = UserRole.valueOf(dbUser.role),
            permissions = dbUser.permissions?.let {
                json.decodeFromString<List<String>>(it)
            } ?: emptyList(),
            createdAt = dbUser.created_at,
            lastLoginAt = dbUser.last_login_at,
            isActive = dbUser.is_active == 1L
        )
    }

    fun toDatabase(user: User): DbUser {
        return DbUser(
            id = user.id,
            username = user.username,
            email = user.email,
            full_name = user.fullName,
            role = user.role.name,
            permissions = if (user.permissions.isNotEmpty()) {
                json.encodeToString(user.permissions)
            } else null,
            created_at = user.createdAt,
            last_login_at = user.lastLoginAt,
            is_active = if (user.isActive) 1L else 0L
        )
    }
}

