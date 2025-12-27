package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.repository.UserRepository
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Use case для регистрации нового пользователя
 */
class RegisterUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Зарегистрировать нового пользователя
     *
     * @param username Имя пользователя (должно быть уникальным)
     * @param email Email пользователя (должен быть валидным и уникальным)
     * @param password Пароль (минимум 4 символа)
     * @param fullName Полное имя пользователя (опционально)
     */
    suspend operator fun invoke(
        username: String,
        email: String,
        password: String,
        fullName: String? = null
    ): Result<User> {
        return try {
            // Валидация входных данных
            require(username.isNotBlank()) { "Username cannot be empty" }
            require(username.length >= 3) { "Username must be at least 3 characters long" }
            require(username.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                "Username can only contain letters, numbers, underscores and hyphens"
            }

            require(email.isNotBlank()) { "Email cannot be empty" }
            require(isValidEmail(email)) { "Invalid email format" }

            require(password.isNotBlank()) { "Password cannot be empty" }
            require(password.length >= 4) { "Password must be at least 4 characters long" }
            require(password.length <= 128) { "Password must be at most 128 characters long" }

            val result = userRepository.register(
                username = username.trim(),
                email = email.trim().lowercase(),
                password = password,
                fullName = fullName?.trim()?.takeIf { it.isNotBlank() }
            )

            if (result.isSuccess) {
                val user = result.getOrThrow()
                logger.info { "User registered successfully: ${user.username} (${user.id})" }
            } else {
                logger.error { "Registration failed: ${result.exceptionOrNull()?.message}" }
            }

            result
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Invalid registration parameters" }
            Result.failure(e)
        } catch (e: Exception) {
            logger.error(e) { "Error during registration" }
            Result.failure(e)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        ))
    }
}

