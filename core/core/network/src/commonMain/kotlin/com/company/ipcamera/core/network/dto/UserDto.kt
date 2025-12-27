package com.company.ipcamera.core.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO для пользователей
 */
@Serializable
data class UserResponse(
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

@Serializable
data class UpdateUserRequest(
    val email: String? = null,
    val fullName: String? = null,
    val password: String? = null
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String = "", // Токены теперь в httpOnly cookies, не отправляются в теле ответа
    val refreshToken: String? = null, // Токены теперь в httpOnly cookies
    val expiresIn: Long,
    val user: UserResponse
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String? = null
)

@Serializable
data class RegisterResponse(
    val success: Boolean,
    val user: UserResponse? = null,
    val message: String? = null
)



