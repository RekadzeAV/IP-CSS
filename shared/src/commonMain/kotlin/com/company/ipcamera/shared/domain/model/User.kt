package com.company.ipcamera.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Модель пользователя
 */
@Serializable
data class User(
    val id: String,
    val username: String,
    val email: String? = null,
    val fullName: String? = null,
    val role: UserRole,
    val permissions: List<String> = emptyList(),
    val createdAt: Long,
    val lastLoginAt: Long? = null,
    val isActive: Boolean = true
) {
    /**
     * Проверка, является ли пользователь администратором
     */
    fun isAdmin(): Boolean = role == UserRole.ADMIN
    
    /**
     * Проверка, имеет ли пользователь разрешение
     */
    fun hasPermission(permission: String): Boolean = permissions.contains(permission)
}

@Serializable
enum class UserRole {
    ADMIN,
    OPERATOR,
    VIEWER,
    GUEST
}
