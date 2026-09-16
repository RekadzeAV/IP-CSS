package com.company.ipcamera.server.dto

import kotlinx.serialization.Serializable

/**
 * DTO для запроса входа в систему
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val captchaToken: String? = null // CAPTCHA токен (требуется после нескольких неудачных попыток)
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
    val user: UserInfoDto,
    /** Требуется второй фактор; в теле вернётся tempToken для POST /auth/2fa/verify */
    val needs2fa: Boolean = false,
    val tempToken: String? = null
)

/** Запрос проверки кода 2FA после логина */
@Serializable
data class Verify2FaRequest(
    val tempToken: String,
    val code: String
)

/** Запрос подтверждения 2FA после сканирования QR */
@Serializable
data class Confirm2FaRequest(
    val code: String
)

/** Ответ успешной проверки 2FA — выдать токены как при логине */
@Serializable
data class TwoFaSetupResponse(
    val secret: String,
    val qrData: String,
    val qrPngBase64: String? = null
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

/**
 * DTO для запроса ручной ротации токена
 */
@Serializable
data class RotateTokenRequest(
    val refreshToken: String
)

/**
 * DTO для ответа при ручной ротации токена
 */
@Serializable
data class RotateTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long,
    val refreshTokenExpiresIn: Long
)

