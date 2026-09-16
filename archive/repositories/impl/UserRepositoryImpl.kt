package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.network.api.UserApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.LoginResult
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация UserRepository с использованием API сервиса
 */
class UserRepositoryImpl(
    private val userApiService: UserApiService,
) : UserRepository {
    override suspend fun login(
        username: String,
        password: String,
    ): Result<LoginResult> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                val request = LoginRequest(username = username, password = password)
                val response = userApiService.login(request)
                Result.success(
                    LoginResult(
                        user = response.user.toDomain(),
                        token = "",
                        refreshToken = "",
                        expiresIn = response.expiresIn,
                    ),
                )
            } catch (e: Exception) {
                logger.error(e) { "Error logging in user: $username" }
                Result.failure(e)
            }
        }

    override suspend fun logout(): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                userApiService.logout()
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error logging out" }
                Result.failure(e)
            }
        }

    override suspend fun register(
        username: String,
        email: String,
        password: String,
        fullName: String?,
    ): Result<User> =
        withContext(Dispatchers.Default) {
            try {
                val request =
                    RegisterRequest(username = username, email = email, password = password, fullName = fullName)
                val response = userApiService.register(request)
                response.user?.let { Result.success(it.toDomain()) }
                    ?: Result.failure(Exception(response.message ?: "Registration failed"))
            } catch (e: Exception) {
                logger.error(e) { "Error registering user: $username" }
                Result.failure(e)
            }
        }

    override suspend fun getCurrentUser(): User? =
        withContext(Dispatchers.Default) {
            try {
                userApiService.getCurrentUser().toDomain()
            } catch (e: Exception) {
                logger.error(e) { "Error getting current user" }
                null
            }
        }

    override suspend fun updateCurrentUser(user: User): Result<User> =
        withContext(Dispatchers.Default) {
            try {
                val request =
                    UpdateUserRequest(
                        email = user.email,
                        fullName = user.fullName,
                        password = null, // Password update should be separate endpoint
                    )
                Result.success(userApiService.updateCurrentUser(request).toDomain())
            } catch (e: Exception) {
                logger.error(e) { "Error updating current user: ${user.id}" }
                Result.failure(e)
            }
        }

    override suspend fun getUsers(
        page: Int,
        limit: Int,
        role: String?,
    ): PaginatedResult<User> =
        withContext(Dispatchers.Default) {
            try {
                val result = userApiService.getUsers(page, limit, role)
                PaginatedResult(
                    items = result.items.map { it.toDomain() },
                    total = result.total,
                    page = result.page,
                    limit = result.limit,
                    hasMore = result.hasMore,
                )
            } catch (e: Exception) {
                logger.error(e) { "Error getting users" }
                PaginatedResult(emptyList(), 0, page, limit, false)
            }
        }

    override suspend fun getUserById(id: String): User? =
        withContext(Dispatchers.Default) {
            try {
                userApiService.getUserById(id).toDomain()
            } catch (e: Exception) {
                logger.error(e) { "Error getting user by id: $id" }
                null
            }
        }

    override suspend fun updateUser(user: User): Result<User> =
        withContext(Dispatchers.Default) {
            try {
                val request =
                    UpdateUserRequest(
                        email = user.email,
                        fullName = user.fullName,
                        password = null, // Password update should be separate endpoint
                    )
                Result.success(userApiService.updateUser(user.id, request).toDomain())
            } catch (e: Exception) {
                logger.error(e) { "Error updating user: ${user.id}" }
                Result.failure(e)
            }
        }

    override suspend fun deleteUser(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                userApiService.deleteUser(id)
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error deleting user: $id" }
                Result.failure(e)
            }
        }

    override suspend fun refreshToken(refreshToken: String): Result<LoginResult> =
        withContext(Dispatchers.Default) {
            try {
                // Refresh token теперь берется из httpOnly cookie автоматически
                // Передаем пустую строку, сервер прочитает token из cookie
                val response = userApiService.refreshToken("")
                Result.success(
                    LoginResult(
                        user = response.user.toDomain(),
                        token = "",
                        refreshToken = "",
                        expiresIn = response.expiresIn,
                    ),
                )
            } catch (e: Exception) {
                logger.error(e) { "Error refreshing token" }
                Result.failure(e)
            }
        }

    private fun UserResponse.toDomain(): User {
        return User(
            id = id,
            username = username,
            email = email,
            fullName = fullName,
            role = UserRole.valueOf(role.uppercase()),
            permissions = permissions,
            createdAt = createdAt,
            lastLoginAt = lastLoginAt,
            isActive = isActive,
        )
    }
}
