package com.company.ipcamera.server.dto

import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.serialization.Serializable

/**
 * DTO для ответа с пользователем
 */
@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val email: String? = null,
    val fullName: String? = null,
    val role: String,
    val permissions: List<String> = emptyList(),
    val createdAt: Long,
    val lastLoginAt: Long? = null,
    val isActive: Boolean = true
)

/**
 * DTO для пагинированного ответа с пользователями
 */
@Serializable
data class PaginatedUserResponse(
    val items: List<UserDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

/**
 * DTO для создания пользователя
 */
@Serializable
data class CreateUserRequest(
    val username: String,
    val email: String? = null,
    val password: String,
    val fullName: String? = null,
    val role: String = "VIEWER"
)

/**
 * DTO для обновления пользователя
 */
@Serializable
data class UpdateUserRequest(
    val email: String? = null,
    val fullName: String? = null,
    val role: String? = null,
    val isActive: Boolean? = null
)

/**
 * Extension функции для конвертации
 */
fun User.toDto(): UserDto {
    return UserDto(
        id = this.id,
        username = this.username,
        email = this.email,
        fullName = this.fullName,
        role = this.role.name,
        permissions = this.permissions,
        createdAt = this.createdAt,
        lastLoginAt = this.lastLoginAt,
        isActive = this.isActive
    )
}

fun PaginatedResult<User>.toDto(): PaginatedUserResponse {
    return PaginatedUserResponse(
        items = this.items.map { it.toDto() },
        total = this.total,
        page = this.page,
        limit = this.limit,
        hasMore = this.hasMore
    )
}

