package com.company.ipcamera.server.dto

import kotlinx.serialization.Serializable

/**
 * DTO для запроса входа в систему
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * DTO для ответа при входе в систему
 */
@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 900, // 15 минут в секундах
    val user: UserInfoDto
)

/**
 * DTO для информации о пользователе в ответе
 */
@Serializable
data class UserInfoDto(
    val id: String,
    val username: String,
    val email: String? = null,
    val fullName: String? = null,
    val role: String,
    val permissions: List<String> = emptyList()
)

/**
 * DTO для запроса обновления токена
 */
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

/**
 * DTO для ответа при обновлении токена
 */
@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 900
)

