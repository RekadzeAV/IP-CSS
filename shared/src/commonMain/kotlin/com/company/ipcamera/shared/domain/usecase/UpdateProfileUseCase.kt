package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.repository.UserRepository
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Use case для обновления профиля пользователя
 */
class UpdateProfileUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Обновить профиль текущего пользователя
     *
     * @param fullName Новое полное имя (опционально)
     * @param email Новый email (опционально, должен быть валидным если указан)
     */
    suspend operator fun invoke(
        fullName: String? = null,
        email: String? = null
    ): Result<User> {
        return try {
            // Получаем текущего пользователя
            val currentUser = userRepository.getCurrentUser()
                ?: return Result.failure(IllegalStateException("No user is currently logged in"))

            // Валидация email, если он указан
            if (email != null && email.isNotBlank()) {
                require(isValidEmail(email)) { "Invalid email format" }
            }

            // Создаем обновленного пользователя
            val updatedUser = currentUser.copy(
                fullName = fullName?.trim()?.takeIf { it.isNotBlank() } ?: currentUser.fullName,
                email = email?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: currentUser.email
            )

            val result = userRepository.updateCurrentUser(updatedUser)

            if (result.isSuccess) {
                val user = result.getOrThrow()
                logger.info { "Profile updated successfully for user: ${user.username} (${user.id})" }
            } else {
                logger.error { "Failed to update profile: ${result.exceptionOrNull()?.message}" }
            }

            result
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Invalid profile update parameters" }
            Result.failure(e)
        } catch (e: IllegalStateException) {
            logger.error(e) { "Cannot update profile: no user logged in" }
            Result.failure(e)
        } catch (e: Exception) {
            logger.error(e) { "Error updating profile" }
            Result.failure(e)
        }
    }

    /**
     * Обновить пользователя (для администраторов)
     *
     * @param user Обновленный объект пользователя
     */
    suspend operator fun invoke(user: User): Result<User> {
        return try {
            // Валидация
            require(user.username.isNotBlank()) { "Username cannot be empty" }
            if (user.email != null) {
                require(isValidEmail(user.email)) { "Invalid email format" }
            }

            val result = userRepository.updateUser(user)

            if (result.isSuccess) {
                val updatedUser = result.getOrThrow()
                logger.info { "User updated successfully: ${updatedUser.username} (${updatedUser.id})" }
            } else {
                logger.error { "Failed to update user: ${result.exceptionOrNull()?.message}" }
            }

            result
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Invalid user update parameters" }
            Result.failure(e)
        } catch (e: Exception) {
            logger.error(e) { "Error updating user" }
            Result.failure(e)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        ))
    }
}

