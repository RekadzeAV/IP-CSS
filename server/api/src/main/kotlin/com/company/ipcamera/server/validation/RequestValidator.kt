package com.company.ipcamera.server.validation

import com.company.ipcamera.server.dto.*
import java.net.URL
import java.util.regex.Pattern

/**
 * Результат валидации
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String, val field: String? = null) : ValidationResult()
}

/**
 * Валидатор для входных данных запросов
 */
object RequestValidator {

    // Паттерн для email
    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
    )

    // Паттерн для username (буквы, цифры, подчеркивания, 3-30 символов)
    private val USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}\$")

    /**
     * Валидация запроса на вход
     */
    fun validateLoginRequest(request: LoginRequest): ValidationResult {
        if (request.username.isBlank()) {
            return ValidationResult.Error("Username is required", "username")
        }
        if (request.username.length < 3) {
            return ValidationResult.Error("Username must be at least 3 characters", "username")
        }
        if (request.username.length > 30) {
            return ValidationResult.Error("Username must be at most 30 characters", "username")
        }
        if (request.password.isBlank()) {
            return ValidationResult.Error("Password is required", "password")
        }
        if (request.password.length < 6) {
            return ValidationResult.Error("Password must be at least 6 characters", "password")
        }
        return ValidationResult.Success
    }

    /**
     * Валидация запроса на создание пользователя
     */
    fun validateCreateUserRequest(request: CreateUserRequest): ValidationResult {
        // Валидация username
        if (request.username.isBlank()) {
            return ValidationResult.Error("Username is required", "username")
        }
        if (!USERNAME_PATTERN.matcher(request.username).matches()) {
            return ValidationResult.Error(
                "Username must contain only letters, numbers, and underscores (3-30 characters)",
                "username"
            )
        }

        // Валидация email (если указан)
        if (request.email != null && request.email.isNotBlank()) {
            if (!EMAIL_PATTERN.matcher(request.email).matches()) {
                return ValidationResult.Error("Invalid email format", "email")
            }
        }

        // Валидация пароля
        if (request.password.isBlank()) {
            return ValidationResult.Error("Password is required", "password")
        }
        if (request.password.length < 8) {
            return ValidationResult.Error("Password must be at least 8 characters", "password")
        }
        if (request.password.length > 128) {
            return ValidationResult.Error("Password must be at most 128 characters", "password")
        }

        // Валидация роли
        val validRoles = listOf("VIEWER", "OPERATOR", "ADMIN")
        if (!validRoles.contains(request.role.uppercase())) {
            return ValidationResult.Error(
                "Invalid role. Must be one of: ${validRoles.joinToString(", ")}",
                "role"
            )
        }

        return ValidationResult.Success
    }

    /**
     * Валидация запроса на обновление пользователя
     */
    fun validateUpdateUserRequest(request: UpdateUserRequest): ValidationResult {
        // Валидация email (если указан)
        if (request.email != null && request.email.isNotBlank()) {
            if (!EMAIL_PATTERN.matcher(request.email).matches()) {
                return ValidationResult.Error("Invalid email format", "email")
            }
        }

        // Валидация роли (если указана)
        if (request.role != null) {
            val validRoles = listOf("VIEWER", "OPERATOR", "ADMIN")
            if (!validRoles.contains(request.role.uppercase())) {
                return ValidationResult.Error(
                    "Invalid role. Must be one of: ${validRoles.joinToString(", ")}",
                    "role"
                )
            }
        }

        return ValidationResult.Success
    }

    /**
     * Валидация запроса на создание камеры
     */
    fun validateCreateCameraRequest(request: CreateCameraRequest): ValidationResult {
        // Валидация имени
        if (request.name.isBlank()) {
            return ValidationResult.Error("Camera name is required", "name")
        }
        if (request.name.length > 100) {
            return ValidationResult.Error("Camera name must be at most 100 characters", "name")
        }

        // Валидация URL
        if (request.url.isBlank()) {
            return ValidationResult.Error("Camera URL is required", "url")
        }
        val urlValidation = validateUrl(request.url)
        if (urlValidation is ValidationResult.Error) {
            return ValidationResult.Error("Invalid camera URL: ${urlValidation.message}", "url")
        }

        // Валидация username (если указан)
        if (request.username != null && request.username.isNotBlank()) {
            if (request.username.length > 100) {
                return ValidationResult.Error(
                    "Username must be at most 100 characters",
                    "username"
                )
            }
        }

        // Валидация password (если указан)
        if (request.password != null && request.password.length > 128) {
            return ValidationResult.Error(
                "Password must be at most 128 characters",
                "password"
            )
        }

        return ValidationResult.Success
    }

    /**
     * Валидация запроса на обновление камеры
     */
    fun validateUpdateCameraRequest(request: UpdateCameraRequest): ValidationResult {
        // Валидация имени (если указано)
        if (request.name != null) {
            if (request.name.isBlank()) {
                return ValidationResult.Error("Camera name cannot be blank", "name")
            }
            if (request.name.length > 100) {
                return ValidationResult.Error(
                    "Camera name must be at most 100 characters",
                    "name"
                )
            }
        }

        // Валидация URL (если указан)
        if (request.url != null) {
            if (request.url.isBlank()) {
                return ValidationResult.Error("Camera URL cannot be blank", "url")
            }
            val urlValidation = validateUrl(request.url)
            if (urlValidation is ValidationResult.Error) {
                return ValidationResult.Error(
                    "Invalid camera URL: ${urlValidation.message}",
                    "url"
                )
            }
        }

        // Валидация username (если указан)
        if (request.username != null && request.username.isNotBlank()) {
            if (request.username.length > 100) {
                return ValidationResult.Error(
                    "Username must be at most 100 characters",
                    "username"
                )
            }
        }

        // Валидация password (если указан)
        if (request.password != null && request.password.length > 128) {
            return ValidationResult.Error(
                "Password must be at most 128 characters",
                "password"
            )
        }

        return ValidationResult.Success
    }

    /**
     * Валидация URL
     */
    private fun validateUrl(urlString: String): ValidationResult {
        return try {
            val url = URL(urlString)
            val protocol = url.protocol.lowercase()
            if (protocol !in listOf("http", "https", "rtsp", "rtmp")) {
                return ValidationResult.Error(
                    "URL protocol must be one of: http, https, rtsp, rtmp"
                )
            }
            if (url.host.isBlank()) {
                return ValidationResult.Error("URL must contain a host")
            }
            ValidationResult.Success
        } catch (e: Exception) {
            ValidationResult.Error("Invalid URL format: ${e.message}")
        }
    }

    /**
     * Валидация параметров пагинации
     */
    fun validatePagination(page: Int?, limit: Int?): ValidationResult {
        val pageValue = page ?: 1
        val limitValue = limit ?: 20

        if (pageValue < 1) {
            return ValidationResult.Error("Page must be at least 1", "page")
        }
        if (limitValue < 1) {
            return ValidationResult.Error("Limit must be at least 1", "limit")
        }
        if (limitValue > 100) {
            return ValidationResult.Error("Limit must be at most 100", "limit")
        }

        return ValidationResult.Success
    }
}

