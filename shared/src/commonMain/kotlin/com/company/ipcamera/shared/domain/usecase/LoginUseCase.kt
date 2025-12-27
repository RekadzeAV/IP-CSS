package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.repository.LoginResult
import com.company.ipcamera.shared.domain.repository.UserRepository
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Use case для входа в систему
 */
class LoginUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Войти в систему
     *
     * @param username Имя пользователя
     * @param password Пароль
     */
    suspend operator fun invoke(
        username: String,
        password: String
    ): Result<LoginResult> {
        return try {
            // Валидация входных данных
            require(username.isNotBlank()) { "Username cannot be empty" }
            require(password.isNotBlank()) { "Password cannot be empty" }
            require(password.length >= 4) { "Password must be at least 4 characters long" }

            val result = userRepository.login(
                username = username.trim(),
                password = password
            )

            if (result.isSuccess) {
                val loginResult = result.getOrThrow()
                logger.info { "User logged in successfully: ${loginResult.user.username} (${loginResult.user.id})" }
            } else {
                logger.warn { "Login failed for username: $username" }
            }

            result
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Invalid login parameters" }
            Result.failure(e)
        } catch (e: Exception) {
            logger.error(e) { "Error during login" }
            Result.failure(e)
        }
    }
}

