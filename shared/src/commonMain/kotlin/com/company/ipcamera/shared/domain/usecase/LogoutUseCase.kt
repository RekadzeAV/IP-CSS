package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.repository.UserRepository
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Use case для выхода из системы
 */
class LogoutUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Выйти из системы
     */
    suspend operator fun invoke(): Result<Unit> {
        return try {
            val result = userRepository.logout()

            if (result.isSuccess) {
                logger.info { "User logged out successfully" }
            } else {
                logger.error { "Logout failed: ${result.exceptionOrNull()?.message}" }
            }

            result
        } catch (e: Exception) {
            logger.error(e) { "Error during logout" }
            Result.failure(e)
        }
    }
}

